package io.adven.grpc.wiremock;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import static java.util.stream.Collectors.joining;

@SpringBootApplication
@EnableConfigurationProperties({ServerProperties.class, WiremockProperties.class})
public class GrpcWiremock implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcWiremock.class);
    private final GrpcServer server;

    public GrpcWiremock(GrpcServer server) {
        this.server = server;
    }

    @Override
    public void run(String... args) throws Exception {
        server.start(50000);
    }

    @Service
    public static class GrpcServer {
        private final ServerProperties serverProperties;
        private final List<BindableService> services;
        private Server server;
        private final CountDownLatch latch;

        public GrpcServer(ServerProperties serverProperties, List<BindableService> services) {
            this.serverProperties = serverProperties;
            this.services = services;
            this.latch = new CountDownLatch(1);
        }

        public void start(int port) throws IOException {
            NettyServerBuilder builder = NettyServerBuilder.forPort(port)
                .intercept(new ExceptionHandler())
                .addService(ProtoReflectionService.newInstance());

            setProperties(builder);

            services.forEach(builder::addService);
            server = builder.build().start();
            LOG.info(summary(server));
            startDaemonAwaitThread();
        }

        private void setProperties(NettyServerBuilder builder) {
            if (serverProperties.getMaxHeaderListSize() != null) {
                int val = Math.toIntExact(serverProperties.getMaxHeaderListSize().toBytes());
                LOG.info("Set maxHeaderListSize = {}" , val);
                builder.maxHeaderListSize(val);
            }
            if (serverProperties.getMaxMessageSize() != null) {
                int val = Math.toIntExact(serverProperties.getMaxMessageSize().toBytes());
                LOG.info("Set maxMessageSize = {}" , val);
                builder.maxMessageSize(val);
            }
            if (serverProperties.getMaxInboundMetadataSize() != null) {
                int val = Math.toIntExact(serverProperties.getMaxInboundMetadataSize().toBytes());
                LOG.info("Set maxInboundMetadataSize = {}" , val);
                builder.maxInboundMetadataSize(val);
            }
            if (serverProperties.getMaxInboundMessageSize() != null) {
                int val = Math.toIntExact(serverProperties.getMaxInboundMessageSize().toBytes());
                LOG.info("Set maxInboundMessageSize = {}" , val);
                builder.maxInboundMessageSize(val);
            }
        }

        private String summary(Server server) {
            return "Started " + server + "\nRegistered services:\n" +
                server.getServices().stream().map(s -> " * " + s.getServiceDescriptor().getName()).collect(joining("\n"));
        }

        private void startDaemonAwaitThread() {
            Thread awaitThread = new Thread(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    LOG.error("gRPC server awaiter interrupted.", e);
                }
            });
            awaitThread.setName("grpc-server-awaiter");
            awaitThread.setDaemon(false);
            awaitThread.start();
        }

        @PreDestroy
        public void destroy() {
            Optional.ofNullable(server).ifPresent(s -> {
                LOG.info("Shutting down gRPC server ...");
                s.shutdown();
                try {
                    s.awaitTermination();
                } catch (InterruptedException e) {
                    LOG.error("gRPC server interrupted during destroy.", e);
                } finally {
                    latch.countDown();
                }
                LOG.info("gRPC server stopped.");
            });

        }
    }

    public static void main(String[] args) {
        SpringApplication.run(GrpcWiremock.class, args);
    }
}