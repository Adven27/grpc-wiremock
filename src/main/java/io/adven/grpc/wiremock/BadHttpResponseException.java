package io.adven.grpc.wiremock;

public class BadHttpResponseException extends RuntimeException {
    private final int statusCode;

    public BadHttpResponseException(int statusCode, String errorMessage) {
        super(errorMessage);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
