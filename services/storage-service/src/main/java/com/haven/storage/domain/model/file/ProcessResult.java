package com.haven.storage.domain.model.file;

import com.haven.base.common.exception.BusinessException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ProcessResult {
    private boolean valid;
    private String message;
    private BusinessException businessException;

    public ProcessResult(boolean valid, String message) {
        this(valid, message, new BusinessException(message));
    }
    public ProcessResult(BusinessException businessException) {
        this(false, businessException.getMessage());
        this.businessException = businessException;
    }

    public ProcessResult(boolean valid, String message, BusinessException businessException) {
        this.valid = valid;
        this.message = message;
        this.businessException = businessException;
    }


    public static ProcessResult fail(String message) {
        return new ProcessResult(false, message);
    }
}

