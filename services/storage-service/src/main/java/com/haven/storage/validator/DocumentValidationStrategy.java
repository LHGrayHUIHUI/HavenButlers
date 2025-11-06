package com.haven.storage.validator;

import com.haven.storage.domain.model.file.FileUploadRequest;
import com.haven.storage.model.bean.FileBasicMetadata;
import com.haven.storage.model.bean.FileProcessingResult;
import okio.BufferedSource;

import java.util.Set;

/**
 * 文档文件校验策略
 */
class DocumentValidationStrategy implements FileProcessingStrategy {
    @Override
    public FileProcessingResult validate(FileBasicMetadata fileBasicMetadata, BufferedSource bufferedSource) {
        Set<String> docTypes = Set.of("pdf", "doc", "docx", "xls", "xlsx");
        if (!docTypes.contains(request.getContentType().toLowerCase())) {
            return FileProcessingResult.fail("文档格式必须是: pdf, doc, docx, xls, xlsx");
        }
        if (request.getFileSize() > 10 * 1024 * 1024) {
            return FileProcessingResult.fail("文档大小不能超过10MB");
        }
        return FileProcessingResult.success();
    }
}
