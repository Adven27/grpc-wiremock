package io.adven.grpc.wiremock;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

public class ExceptionHandler implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler
    ) {
        ServerCall.Listener<ReqT> listener = serverCallHandler.startCall(serverCall, metadata);
        return new ExceptionHandlingServerCallListener<>(listener, serverCall, metadata);
    }

    private class ExceptionHandlingServerCallListener<ReqT, RespT>
        extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
        private ServerCall<ReqT, RespT> serverCall;
        private Metadata metadata;

        ExceptionHandlingServerCallListener(
            ServerCall.Listener<ReqT> listener, ServerCall<ReqT, RespT> serverCall, Metadata metadata
        ) {
            super(listener);
            this.serverCall = serverCall;
            this.metadata = metadata;
        }

        @Override
        public void onHalfClose() {
            try {
                super.onHalfClose();
            } catch (RuntimeException ex) {
                handleException(ex, serverCall, metadata);
                throw ex;
            }
        }

        @Override
        public void onReady() {
            try {
                super.onReady();
            } catch (RuntimeException ex) {
                handleException(ex, serverCall, metadata);
                throw ex;
            }
        }

        private void handleException(RuntimeException ex, ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
            Status status = Status.UNKNOWN;
            if (ex instanceof BadHttpResponseException) {
                status = mapStatusCode(((BadHttpResponseException) ex).getStatusCode()).withDescription(ex.getMessage());
            } else if (ex instanceof IllegalArgumentException) {
                status = Status.INVALID_ARGUMENT.withDescription(ex.getMessage());
            }
            serverCall.close(status, metadata);
        }

        // Source: https://github.com/googleapis/googleapis/blob/master/google/rpc/code.proto
        private Status mapStatusCode(int statusCode)
        {
            switch(statusCode) {
                case 400: // Bad Request
                    return Status.INVALID_ARGUMENT;
                case 401: // Unauthorized
                    return Status.UNAUTHENTICATED;
                case 403: // Forbidden
                    return Status.PERMISSION_DENIED;
                case 404: // Not Found
                    return Status.NOT_FOUND;
                case 409: // Conflict
                    return Status.ALREADY_EXISTS;
                case 429: // Too Many Requests
                    return Status.RESOURCE_EXHAUSTED;
                case 500: // Internal Server Error
                default:
                    return Status.INTERNAL;
            }
        }
    }
}