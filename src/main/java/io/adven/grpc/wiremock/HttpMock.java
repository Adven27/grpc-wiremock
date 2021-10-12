package io.adven.grpc.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.standalone.CommandLineOptions;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.adven.grpc.wiremock.configurer.WiremockConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.InputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static io.adven.grpc.wiremock.HeaderPropagationInterceptor.HEADERS;

@Component
public class HttpMock {
    private static final Logger LOG = LoggerFactory.getLogger(HttpMock.class);
    private static final String PREFIX = "wiremock_";
    private final WiremockConfigurer configurer;
    private WireMockServer server;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public HttpMock(WiremockConfigurer configurer) {
        this.configurer = configurer;
    }

    public void start() {
        String[] args = configurer.configure(envOptions());
        LOG.info("Starting WireMock server with options:\n{}", String.join("\n", args));
        CommandLineOptions options = new CommandLineOptions(args);
        server = new WireMockServer(options);
        server.start();
        LOG.info("WireMock server is started:\n{}", setActualPort(options));
    }

    private CommandLineOptions setActualPort(CommandLineOptions options) {
        if (!options.getHttpDisabled()) {
            options.setActualHttpPort(server.port());
        }
        if (options.httpsSettings().enabled()) {
            options.setActualHttpsPort(server.httpsPort());
        }
        return options;
    }

    private String[] envOptions() {
        return System.getenv().entrySet().stream()
            .filter(it -> it.getKey().toLowerCase().startsWith(PREFIX))
            .map(this::toWiremockOption)
            .toArray(String[]::new);
    }

    private String toWiremockOption(Map.Entry<String, String> it) {
        return "--" + it.getKey().toLowerCase().substring(PREFIX.length()) + (nullOrEmpty(it.getValue()) ? "" : "=" + it.getValue());
    }

    private boolean nullOrEmpty(String value) {
        return value == null || value.equals("");
    }

    @PreDestroy
    public void destroy() {
        server.stop();
    }

    public Response request(String path, Object message, Map<String, String> headers) throws IOException, InterruptedException {
        headers.putAll(HEADERS.get());
        headers.remove("accept-encoding");
        LOG.info("Grpc request {}:\nHeaders: {}\nMessage:\n{}", path, headers, message);
        return new Response(
            httpClient.send(
                HttpRequest.newBuilder()
                    .uri(URI.create(server.baseUrl() + "/" + path))
                    .POST(asJson(message))
                    .headers(headers.entrySet().stream().flatMap(e -> Stream.of(e.getKey(), e.getValue())).toArray(String[]::new))
                    .build(),
                HttpResponse.BodyHandlers.ofInputStream()
            )
        );
    }

    public Response request(String path, Object message) throws IOException, InterruptedException {
        return request(path, message, new HashMap<>());
    }

    public static final class Response {
        private final HttpResponse<InputStream> httpResponse;

        public Response(HttpResponse<InputStream> httpResponse) {
            this.httpResponse = httpResponse;
        }

        public Message getMessage(Class<?> aClass) {
            if (httpResponse.statusCode() == 200) {
                return ProtoJsonUtil.fromJson(getBody(), aClass);
            }
            throw new BadHttpResponseException(httpResponse.statusCode(), getBody());
        }

        public int streamSize() {
            return httpResponse.headers().firstValue("streamSize").map(Integer::valueOf).orElse(1);
        }

        private String getBody() {
            try {
                InputStream bodyStream = httpResponse.body();
                if (httpResponse.headers().firstValue("Content-Encoding").orElse("").equals("gzip")) {
                    bodyStream = new GZIPInputStream(bodyStream);
                }
                return new String(bodyStream.readAllBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
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
