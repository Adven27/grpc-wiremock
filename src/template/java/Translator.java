package io.adven.grpc.wiremock;

import io.grpc.stub.ServerCallStreamObserver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.Arrays;
@import_services@

@Configuration
public class Translator{
    @services@

    @Aspect
    @Component
    class WireMockTranslator {
        private final HttpMock httpMock;

        public WireMockTranslator(HttpMock httpMock) {
            this.httpMock = httpMock;
        }

        @decorate_services@

        private Class<?> respType(String name, String byClass) throws ClassNotFoundException {
            return Arrays.stream(Class.forName(byClass).getMethods())
                .filter(method -> method.getName().equals(name))
                .findFirst().orElseThrow().getReturnType();
        }
    }
}