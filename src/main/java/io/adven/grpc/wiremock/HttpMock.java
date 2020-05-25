package io.adven.grpc.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@Component
public class HttpMock {
    private final WireMockServer server = mockServer();

    public HttpMock() {
        server.start();
    }

    @PreDestroy
    public void destroy() {
        server.stop();
    }

    public Message send(Object message, Class<?> aClass) throws IOException, InterruptedException {
        return ProtoJsonUtil.fromJson(request(message).body(), aClass);
    }

    private HttpResponse<String> request(Object message) throws IOException, InterruptedException {
        final HttpResponse<String> response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder().uri(URI.create(server.baseUrl())).POST(asJson(message)).build(),
            HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() != 200) {
            throw new IllegalArgumentException(response.body());
        }
        return response;
    }

    private HttpRequest.BodyPublisher asJson(Object arg) throws IOException {
        return HttpRequest.BodyPublishers.ofString(ProtoJsonUtil.toJson((MessageOrBuilder) arg));
    }

    private static WireMockServer mockServer() {
        return new WireMockServer(wireMockConfig().extensions(new ResponseTemplateTransformer(true)).port(8888));
    }

    private static final class ProtoJsonUtil {
        static String toJson(MessageOrBuilder messageOrBuilder) throws IOException {
            return JsonFormat.printer().print(messageOrBuilder);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        static <T extends Message> T fromJson(String json, Class<?> clazz) {
            try {
                AbstractMessage.Builder builder = (AbstractMessage.Builder) clazz.getMethod("newBuilder").invoke(null);
                JsonFormat.parser().merge(json, builder);
                return (T) builder.build();
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InvalidProtocolBufferException e) {
                throw new IllegalArgumentException("Failed to convert " + json + " to " + clazz, e);
            }
        }
    }
}