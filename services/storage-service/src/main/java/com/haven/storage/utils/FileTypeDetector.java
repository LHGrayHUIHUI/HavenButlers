package com.haven.storage.utils;

import com.haven.storage.model.enums.SupportedFileType;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件类型检测器 (精简版)
 * <p>
 * 职责分离：
 * 1. `SupportedFileType` (enum): 负责定义应用支持的类型数据。
 * 2. `FileTypeDetector` (class): 负责执行检测逻辑。
 * <p>
 * 检测策略：
 * 1. `detectByContent`:   使用 Apache Tika (魔数检测)，最可靠。
 * 2. `detectByMime`:      使用请求头 Content-Type。
 * 3. `detectByExtension`: 使用文件名。
 */
@Slf4j
@Component
public class FileTypeDetector {

    // Tika 实例是线程安全的，可以复用
    private final Tika tika = new Tika();
    private static final int MAX_MARK_LIMIT = 10 * 1024 * 1024; // Tika 可能需要读取几MB来确定ZIP内容

    /**
     * 1. 基于文件内容 (魔数) 进行检测 (最可靠)
     * <p>
     * 使用 Apache Tika 库，它能正确处理 ZIP/OLE 冲突，并安全地处理流。
     *
     * @param inputStream 文件输入流
     * @return 检测到的文件类型 (永远不会返回 null)
     */
    public SupportedFileType detectByContent(InputStream inputStream) {
        // Tika 内部会处理 BufferedInputStream，但我们最好自己包装
        // 以确保 mark/reset 功能是可用的，防止流被消耗
        if (!inputStream.markSupported()) {
            inputStream = new BufferedInputStream(inputStream);
        }

        try {
            // Tika 需要标记流，以便在检测后其他服务仍能从头读取
            inputStream.mark(MAX_MARK_LIMIT);

            // Tika.detect() 会读取流的前几个字节，并返回真实的 MIME 类型
            // 它能正确区分 docx (返回 vnd.openxmlformats-...) 和 zip (返回 application/zip)
            String realMimeType = tika.detect(inputStream);

            // 重置流，以便后续操作（如S3上传）能从头开始
            inputStream.reset();

            log.debug("Tika (魔数) 检测到 MIME: {}", realMimeType);
            return SupportedFileType.findByMimeType(realMimeType);

        } catch (IOException e) {
            log.error("通过 Tika 检测文件内容失败", e);
            // 发生 I/O 异常，无法检测
            return SupportedFileType.UNKNOWN;
        }
    }

    /**
     * 2. 基于请求头 (Content-Type) 进行检测
     *
     * @param mimeType 来自 HTTP 请求头的 Content-Type
     * @return 检测到的文件类型
     */
    public SupportedFileType detectByMime(String mimeType) {
        return SupportedFileType.findByMimeType(mimeType);
    }

    /**
     * 3. 基于文件扩展名进行检测 (最不可靠)
     *
     * @param fileName 文件名
     * @return 检测到的文件类型
     */
    public SupportedFileType detectByExtension(String fileName) {
        String extension = extractExtension(fileName);
        return SupportedFileType.findByExtension(extension);
    }

    /**
     * 辅助方法：提取文件扩展名
     */
    private String extractExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }
}