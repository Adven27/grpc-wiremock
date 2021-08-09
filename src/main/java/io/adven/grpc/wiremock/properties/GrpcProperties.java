package io.adven.grpc.wiremock.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.util.unit.DataSize;

import java.util.Map;

@ConfigurationProperties("grpc")
@ConstructorBinding
public class GrpcProperties {
    private final ServerProperties server;
    private final ErrorCodeMapping errorCodeBy;

    public GrpcProperties(ServerProperties server, ErrorCodeMapping errorCodeBy) {
        this.server = server;
        this.errorCodeBy = errorCodeBy;
    }

    public ServerProperties getServer() {
        return server;
    }

    public ErrorCodeMapping getErrorCodeBy() {
        return errorCodeBy;
    }

    public static class ServerProperties {
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

    public static class ErrorCodeMapping {
        private final Http http;

        public ErrorCodeMapping(Http http) {
            this.http = http;
        }

        public Http getHttp() {
            return http;
        }

        public static class Http {
            private final Map<Integer, String> statusCode;

            public Http(Map<Integer, String> statusCode) {
                this.statusCode = statusCode;
            }

            public Map<Integer, String> getStatusCode() {
                return statusCode;
            }
        }
    }
}
