package com.haven.storage.exception;

/**
 * 文件存储异常
 *
 * @author Haven
 */
public class FileStorageException extends RuntimeException {

    private final String errorCode;
    private final String operation;

    public FileStorageException(String message) {
        super(message);
        this.errorCode = "FILE_STORAGE_ERROR";
        this.operation = "UNKNOWN";
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "FILE_STORAGE_ERROR";
        this.operation = "UNKNOWN";
    }

    public FileStorageException(String errorCode, String operation, String message) {
        super(message);
        this.errorCode = errorCode;
        this.operation = operation;
    }

    public FileStorageException(String errorCode, String operation, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.operation = operation;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getOperation() {
        return operation;
    }
}