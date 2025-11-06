package com.haven.storage.validator;

import com.haven.storage.domain.model.file.FileUploadRequest;
import com.haven.storage.model.bean.FileBasicMetadata;
import com.haven.storage.model.bean.FileProcessingResult;
import okio.BufferedSource;

import java.util.Set;

/**
 * 图片文件校验策略
 */
class ImageValidationStrategy implements FileProcessingStrategy {
    @Override
    public FileProcessingResult validate(FileBasicMetadata fileBasicMetadata, BufferedSource bufferedSource) {
        // 图片特定校验:格式、尺寸、分辨率等
        Set<String> imageTypes = Set.of("jpg", "jpeg", "png", "gif");
        if (!imageTypes.contains(request.getContentType().toLowerCase())) {
            return FileProcessingResult.fail("图片格式必须是: jpg, jpeg, png, gif");
        }
        if (request.getFileSize() > 5 * 1024 * 1024) {
            return FileProcessingResult.fail("图片大小不能超过5MB");
        }
        return FileProcessingResult.success();
    }
}
