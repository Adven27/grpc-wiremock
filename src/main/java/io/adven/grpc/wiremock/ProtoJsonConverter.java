package io.adven.grpc.wiremock;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@Component
public class ProtoJsonConverter {
    private static final Logger LOG = LoggerFactory.getLogger(HttpMock.class);
    private final JsonFormat.Printer printer;

    public ProtoJsonConverter(@Value("${json.preserving.proto.field.names:false}") boolean preservingProtoFieldNames) {
        printer = preservingProtoFieldNames ? JsonFormat.printer().preservingProtoFieldNames() : JsonFormat.printer();
    }

    public String toJson(MessageOrBuilder messageOrBuilder) throws IOException {
        return printer.print(messageOrBuilder);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Message> T fromJson(String json, Class<?> clazz) {
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
