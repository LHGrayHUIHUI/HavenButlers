package com.haven.storage.validator;

import com.haven.storage.domain.model.file.FileUploadRequest;
import com.haven.storage.model.bean.FileBasicMetadata;
import com.haven.storage.model.bean.FileProcessingResult;

/**
 * 必填字段校验器
 */
class RequiredFieldValidator extends FileValidator {
    @Override
    protected FileProcessingResult proceed(FileBasicMetadata fileBasicMetadata) {
        if (fileBasicMetadata.getOwnerId() == null || fileBasicMetadata.getOwnerId().isEmpty()) {
            return FileProcessingResult.fail("用户ID不能为空");
        }
//        if (fileBasicMetadata.getFile() == null) {
//            return FileProcessingResult.fail("文件不能为空");
//        }
        return FileProcessingResult.success();
    }
}
