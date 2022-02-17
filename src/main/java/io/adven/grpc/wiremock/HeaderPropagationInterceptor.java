package io.adven.grpc.wiremock;

import com.google.common.io.BaseEncoding;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

import java.util.Map;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static io.grpc.Metadata.BINARY_BYTE_MARSHALLER;
import static io.grpc.Metadata.BINARY_HEADER_SUFFIX;
import static io.grpc.Metadata.Key.of;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class HeaderPropagationInterceptor implements ServerInterceptor {
    public static final String X_GRPC_FULL_METHOD_NAME = "x-grpc-full-method-name";
    public static final Context.Key<Map<String, String>> HEADERS = Context.key("GRPC_WIREMOCK_HEADERS");
    private static final BaseEncoding BASE64_ENCODING_OMIT_PADDING = BaseEncoding.base64().omitPadding();

    @Override
    public <Req, Resp> ServerCall.Listener<Req> interceptCall(ServerCall<Req, Resp> call, final Metadata headers, ServerCallHandler<Req, Resp> next) {
        return Contexts.interceptCall(
            Context.current().withValue(HEADERS, enriched(asMap(headers), call)),
            call,
            headers,
            next
        );
    }

    private Map<String, String> enriched(Map<String, String> map, ServerCall call) {
        map.put(X_GRPC_FULL_METHOD_NAME, call.getMethodDescriptor().getFullMethodName());
        return map;
    }

    private Map<String, String> asMap(Metadata headers) {
        return headers.keys().stream().collect(
            toMap(
                identity(),
                k -> k.endsWith(BINARY_HEADER_SUFFIX)
                    ? BASE64_ENCODING_OMIT_PADDING.encode(headers.get(of(k, BINARY_BYTE_MARSHALLER)))
                    : headers.get(of(k, ASCII_STRING_MARSHALLER))
            )
        );
    }
}
