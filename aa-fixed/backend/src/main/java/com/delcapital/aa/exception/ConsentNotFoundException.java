package com.delcapital.aa.exception;

import java.util.UUID;

public class ConsentNotFoundException extends RuntimeException {
    public ConsentNotFoundException(UUID id) {
        super("Consent not found: " + id);
    }
    public ConsentNotFoundException(String key) {
        super("Consent not found: " + key);
    }
}
