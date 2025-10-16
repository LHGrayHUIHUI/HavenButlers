package com.haven.storage.exception;

import com.haven.base.common.exception.BaseException;
import com.haven.base.common.response.ErrorCode;
import lombok.Getter;

/**Ø
 * 自定义文件上传异常类
 * <p>ø
 * 用于携带文件上传相关的上下文信息，便于异常处理和日志记录
 */
@Getter
public class FileUploadException extends BaseException {
    private final String familyId;
    private final String userId;
    private final String fileName;

    public FileUploadException(String message, String familyId, String userId, String fileName) {
        super(ErrorCode.FILE_TYPE_ERROR,message);
        this.familyId = familyId;
        this.userId = userId;
        this.fileName = fileName;
    }

    public FileUploadException(ErrorCode errorCode,String message, Throwable cause, String familyId, String userId, String fileName) {
        super(errorCode,message, cause);
        this.familyId = familyId;
        this.userId = userId;
        this.fileName = fileName;
    }

}
