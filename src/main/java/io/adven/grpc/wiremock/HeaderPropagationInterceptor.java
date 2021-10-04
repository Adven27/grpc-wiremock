package io.adven.grpc.wiremock;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

import java.util.Map;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static io.grpc.Metadata.Key.of;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class HeaderPropagationInterceptor implements ServerInterceptor {
    public static final Context.Key<Map<String, String>> HEADERS = Context.key("GRPC_WIREMOCK_HEADERS");

    @Override
    public <Req, Resp> ServerCall.Listener<Req> interceptCall(ServerCall<Req, Resp> call, final Metadata headers, ServerCallHandler<Req, Resp> next) {
        return Contexts.interceptCall(
            Context.current().withValue(HEADERS, asMap(headers)),
            call,
            headers,
            next
        );
    }

    private Map<String, String> asMap(Metadata headers) {
        return headers.keys().stream().collect(toMap(identity(), k -> headers.get(of(k, ASCII_STRING_MARSHALLER))));
    }
}
