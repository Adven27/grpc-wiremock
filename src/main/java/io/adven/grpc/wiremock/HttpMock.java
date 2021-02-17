package io.adven.grpc.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOG = LoggerFactory.getLogger(HttpMock.class);
    private final WireMockServer server;

    public HttpMock(WiremockProperties properties) {
        WireMockConfiguration config = wireMockConfig()
            .usingFilesUnderDirectory("/wiremock")
            .extensions(new ResponseTemplateTransformer(true))
            .port(8888)
            .notifier(new Slf4jNotifier(properties.isVerbose()))
            .stubRequestLoggingDisabled(properties.isStubRequestLoggingDisabled())
            .asynchronousResponseEnabled(properties.isAsynchronousResponseEnabled())
            .asynchronousResponseThreads(properties.getAsynchronousResponseThreads());

        if (properties.isDisableRequestJournal()) {
            config.disableRequestJournal();
        }
        server = new WireMockServer(config);
    }

    @PostConstruct
    public void init() {
        server.start();
    }

    @PreDestroy
    public void destroy() {
        server.stop();
    }

    public Message send(Object message, String path, Class<?> aClass) throws IOException, InterruptedException {
        return ProtoJsonUtil.fromJson(request(path, message).body(), aClass);
    }

    private HttpResponse<String> request(String path, Object message) throws IOException, InterruptedException {
        LOG.info("Grpc request {}:\n{}", path, message);
        final HttpResponse<String> response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder().uri(URI.create(server.baseUrl() + "/" + path)).POST(asJson(message)).build(),
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
                LOG.info("Converting to {} json:\n{}", clazz, json);
                AbstractMessage.Builder builder = (AbstractMessage.Builder) clazz.getMethod("newBuilder").invoke(null);
                JsonFormat.parser().merge(json, builder);
                T result = (T) builder.build();
                LOG.info("Grpc response:\n{}", result);
                return result;
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InvalidProtocolBufferException e) {
                throw new IllegalArgumentException("Failed to convert " + json + " to " + clazz, e);
            }
        }
    }
}