package com.haven.storage.validator;

import java.util.Set;

/**
 * 校验器建造者
 */
class ValidatorBuilder {
    private FileValidator head;
    private FileValidator current;

    public ValidatorBuilder addRequiredFieldValidator() {
        return addValidator(new RequiredFieldValidator());
    }

    public ValidatorBuilder addFileSizeValidator(long maxSize) {
        return addValidator(new FileSizeValidator(maxSize));
    }

    public ValidatorBuilder addFileTypeValidator(Set<String> allowedTypes) {
        return addValidator(new FileTypeValidator(allowedTypes));
    }

    public ValidatorBuilder addFileNameValidator() {
        return addValidator(new FileNameValidator());
    }

    private ValidatorBuilder addValidator(FileValidator validator) {
        if (head == null) {
            head = validator;
            current = validator;
        } else {
            current.setNext(validator);
            current = validator;
        }
        return this;
    }

    public FileValidator build() {
        return head;
    }
}
