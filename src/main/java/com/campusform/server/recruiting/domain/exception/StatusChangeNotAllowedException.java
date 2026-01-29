package com.campusform.server.recruiting.domain.exception;

public class StatusChangeNotAllowedException extends RuntimeException {
    public StatusChangeNotAllowedException(String message) {
        super(message);
    }
}
