package com.haven.storage.domain.model.file;

import org.jetbrains.annotations.NotNull;

/**
 * 校验结果
 */
public record FileProcessingResult(boolean valid, String message) {

    public static FileProcessingResult success() {
        return new FileProcessingResult(true, "处理完成");
    }

    public static FileProcessingResult fail(String message) {
        return new FileProcessingResult(false, message);
    }

    @NotNull
    @Override
    public String toString() {
        return valid ? "ValidationResult{valid=true}" :
                "ValidationResult{valid=false, errorMessage='" + message + "'}";
    }
}
