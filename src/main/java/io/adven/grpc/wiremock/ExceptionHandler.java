package io.adven.grpc.wiremock;

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;

import static io.grpc.Status.*;
import static org.apache.http.HttpStatus.*;

public class ExceptionHandler implements ServerInterceptor {
    private final static Map<Integer, Status> CODE_EQUIVALENCES = new HashMap<>() {
        {
            put(SC_BAD_REQUEST, INTERNAL);
            put(SC_UNAUTHORIZED, UNAUTHENTICATED);
            put(SC_FORBIDDEN, PERMISSION_DENIED);
            put(SC_NOT_FOUND, UNIMPLEMENTED);
            put(SC_BAD_GATEWAY, UNAVAILABLE);
            put(SC_SERVICE_UNAVAILABLE, UNAVAILABLE);
            put(SC_GATEWAY_TIMEOUT, UNAVAILABLE);
        }
    };

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler
    ) {
        ServerCall.Listener<ReqT> listener = serverCallHandler.startCall(serverCall, metadata);
        return new ExceptionHandlingServerCallListener<>(listener, serverCall, metadata);
    }

    private static class ExceptionHandlingServerCallListener<ReqT, RespT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
        private final ServerCall<ReqT, RespT> serverCall;
        private final Metadata metadata;

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
            } catch (Exception ex) {
                handleException(ex, serverCall, metadata);
                throw ex;
            }
        }

        @Override
        public void onReady() {
            try {
                super.onReady();
            } catch (Exception ex) {
                handleException(ex, serverCall, metadata);
                throw ex;
            }
        }

        private void handleException(Exception ex, ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
            serverCall.close(
                    ((UndeclaredThrowableException) ex).getUndeclaredThrowable() instanceof HttpResponseException ?
                    CODE_EQUIVALENCES.getOrDefault(((HttpResponseException)((UndeclaredThrowableException) ex).getUndeclaredThrowable()).getStatusCode(), UNKNOWN).withDescription(ex.getMessage())
                            : UNKNOWN,
                    metadata);
        }
    }
}