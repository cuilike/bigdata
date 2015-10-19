package net.redborder.cep.rest.exceptions;

public class RestException extends Exception {
    public RestException(String message) {
        super(message);
    }

    public RestException(String message, Throwable cause) {
        super(message, cause);
    }
}
