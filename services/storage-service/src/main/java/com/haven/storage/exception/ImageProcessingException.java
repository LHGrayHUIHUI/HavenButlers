package com.haven.storage.exception;

/**
 * 图片处理异常
 *
 * @author Haven
 */
public class ImageProcessingException extends RuntimeException {

    private final String errorCode;
    private final String fileId;
    private final String operation;

    public ImageProcessingException(String message) {
        super(message);
        this.errorCode = "IMAGE_PROCESSING_ERROR";
        this.fileId = null;
        this.operation = "UNKNOWN";
    }

    public ImageProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "IMAGE_PROCESSING_ERROR";
        this.fileId = null;
        this.operation = "UNKNOWN";
    }

    public ImageProcessingException(String errorCode, String fileId, String operation, String message) {
        super(message);
        this.errorCode = errorCode;
        this.fileId = fileId;
        this.operation = operation;
    }

    public ImageProcessingException(String errorCode, String fileId, String operation, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.fileId = fileId;
        this.operation = operation;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getFileId() {
        return fileId;
    }

    public String getOperation() {
        return operation;
    }
}