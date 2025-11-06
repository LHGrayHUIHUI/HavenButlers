package com.haven.storage.validator;

/**
 * 校验结果
 */
public record ValidationResult(boolean valid, String message) {

    public static ValidationResult success() {
        return new ValidationResult(true, "验证通过");
    }

    public static ValidationResult fail(String message) {
        return new ValidationResult(false, message);
    }

}
