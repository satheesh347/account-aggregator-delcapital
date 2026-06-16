package com.delcapital.aa.exception;

public class ConsentAlreadyExistsException extends RuntimeException {
    public ConsentAlreadyExistsException(String key) {
        super("Consent already exists for key: " + key);
    }
}
