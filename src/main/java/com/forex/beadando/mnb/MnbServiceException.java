package com.forex.beadando.mnb;

public class MnbServiceException extends RuntimeException {
    public MnbServiceException(String message) {
        super(message);
    }

    public MnbServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
