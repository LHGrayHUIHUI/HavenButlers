package com.haven.storage.exception;

/**
 * 文件分享异常
 *
 * @author Haven
 */
public class ShareException extends RuntimeException {

    private final String errorCode;
    private final String shareId;

    public ShareException(String message) {
        super(message);
        this.errorCode = "SHARE_ERROR";
        this.shareId = null;
    }

    public ShareException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SHARE_ERROR";
        this.shareId = null;
    }

    public ShareException(String errorCode, String shareId, String message) {
        super(message);
        this.errorCode = errorCode;
        this.shareId = shareId;
    }

    public ShareException(String errorCode, String shareId, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.shareId = shareId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getShareId() {
        return shareId;
    }
}