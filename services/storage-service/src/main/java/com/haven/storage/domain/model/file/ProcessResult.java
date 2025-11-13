package com.haven.storage.domain.model.file;

import com.haven.base.common.exception.BusinessException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ProcessResult {
    private boolean isSuccess;
    private String message;
    private BusinessException businessException;

    public ProcessResult(boolean isSuccess, String message) {
        this(isSuccess, message, isSuccess ? null : new BusinessException(message));
    }

    public ProcessResult(BusinessException businessException) {
        this(false, businessException.getMessage());
        this.businessException = businessException;
    }


    public ProcessResult(boolean isSuccess, String message, BusinessException businessException) {
        this.isSuccess = isSuccess;
        this.message = message;
        this.businessException = businessException;
    }


    public static ProcessResult fail(String message) {
        return new ProcessResult(false, message);
    }

    public static ProcessResult success(String message) {
        return new ProcessResult(true, message);
    }

    public static ProcessResult failure(String message) {
        return new ProcessResult(false, message);
    }
}

