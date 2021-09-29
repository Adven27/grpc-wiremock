package io.adven.grpc.wiremock;

import io.adven.grpc.wiremock.properties.GrpcProperties;
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
@EnableConfigurationProperties({GrpcProperties.class})
public class GrpcWiremock implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcWiremock.class);
    private final GrpcServer server;
    private final HttpMock httpMock;

    public GrpcWiremock(GrpcServer server, HttpMock httpMock) {
        this.server = server;
        this.httpMock = httpMock;
    }

    @Override
    public void run(String... args) throws Exception {
        httpMock.start();
        server.start();
    }

    @Service
    public static class GrpcServer {
        private final GrpcProperties grpcProperties;
        private final List<BindableService> services;
        private Server server;
        private final CodecRegistry codecRegistry;
        private final CountDownLatch latch;

        public GrpcServer(GrpcProperties grpcProperties, CodecRegistry codecRegistry, List<BindableService> services) {
            this.grpcProperties = grpcProperties;
            this.codecRegistry = codecRegistry;
            this.services = services;
            this.latch = new CountDownLatch(1);
        }

        public void start() throws IOException {
            NettyServerBuilder builder = NettyServerBuilder.forPort(grpcProperties.getServer().getPort())
                .intercept(new ExceptionHandler(grpcProperties.getErrorCodeBy()))
                .compressorRegistry(codecRegistry.compressorRegistry())
                .decompressorRegistry(codecRegistry.decompressorRegistry())
                .addService(ProtoReflectionService.newInstance());

            setProperties(builder);
            services.forEach(builder::addService);
            server = builder.build().start();
            LOG.info(summary(server));
            startDaemonAwaitThread();
        }

        private void setProperties(NettyServerBuilder builder) {
            GrpcProperties.ServerProperties server = grpcProperties.getServer();
            if (server.getMaxHeaderListSize() != null) {
                int val = Math.toIntExact(server.getMaxHeaderListSize().toBytes());
                LOG.info("Set maxHeaderListSize = {}", val);
                builder.maxHeaderListSize(val);
            }
            if (server.getMaxMessageSize() != null) {
                int val = Math.toIntExact(server.getMaxMessageSize().toBytes());
                LOG.info("Set maxMessageSize = {}", val);
                builder.maxMessageSize(val);
            }
            if (server.getMaxInboundMetadataSize() != null) {
                int val = Math.toIntExact(server.getMaxInboundMetadataSize().toBytes());
                LOG.info("Set maxInboundMetadataSize = {}", val);
                builder.maxInboundMetadataSize(val);
            }
            if (server.getMaxInboundMessageSize() != null) {
                int val = Math.toIntExact(server.getMaxInboundMessageSize().toBytes());
                LOG.info("Set maxInboundMessageSize = {}", val);
                builder.maxInboundMessageSize(val);
            }
        }

        private String summary(Server server) {
            return "gRPC server is started: " + server + "\nRegistered services:\n" +
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