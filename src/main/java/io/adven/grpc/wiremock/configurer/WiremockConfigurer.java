package io.adven.grpc.wiremock.configurer;

public interface WiremockConfigurer {
    String[] configure(String... args);
}
