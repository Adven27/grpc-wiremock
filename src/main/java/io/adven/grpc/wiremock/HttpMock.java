package io.adven.grpc.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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
    private final static WireMockServer SERVER = new WireMockServer(
        wireMockConfig()
            .usingFilesUnderDirectory("/wiremock")
            .extensions(new ResponseTemplateTransformer(true))
            .port(8888)
    );

    @PostConstruct
    public void init() {
        SERVER.start();
    }

    @PreDestroy
    public void destroy() {
        SERVER.stop();
    }

    public Message send(Object message, String path, Class<?> aClass) throws IOException, InterruptedException {
        return ProtoJsonUtil.fromJson(request(path, message).body(), aClass);
    }

    private HttpResponse<String> request(String path, Object message) throws IOException, InterruptedException {
        final HttpResponse<String> response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder().uri(URI.create(SERVER.baseUrl() + "/" + path)).POST(asJson(message)).build(),
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