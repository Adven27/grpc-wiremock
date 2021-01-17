package io.adven.grpc.wiremock;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties("grpc.server")
@ConstructorBinding
public class ServerProperties {
    private final DataSize maxHeaderListSize;
    private final DataSize maxMessageSize;
    private final DataSize maxInboundMetadataSize;
    private final DataSize maxInboundMessageSize;

    public ServerProperties(DataSize maxHeaderListSize, DataSize maxMessageSize, DataSize maxInboundMetadataSize, DataSize maxInboundMessageSize) {
        this.maxHeaderListSize = maxHeaderListSize;
        this.maxMessageSize = maxMessageSize;
        this.maxInboundMetadataSize = maxInboundMetadataSize;
        this.maxInboundMessageSize = maxInboundMessageSize;
    }

    public DataSize getMaxHeaderListSize() {
        return maxHeaderListSize;
    }

    public DataSize getMaxMessageSize() {
        return maxMessageSize;
    }

    public DataSize getMaxInboundMetadataSize() {
        return maxInboundMetadataSize;
    }

    public DataSize getMaxInboundMessageSize() {
        return maxInboundMessageSize;
    }
}
