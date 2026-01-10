package com.vuongnguyen.fintech_project.exception;

public class PriceNotAvailableException extends RuntimeException {

    public PriceNotAvailableException(String message) {
        super(message);
    }

    public PriceNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
