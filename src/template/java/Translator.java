package io.adven.grpc.wiremock;

import io.grpc.stub.ServerCallStreamObserver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
@import_services@

@Configuration
public class Translator{
    @services@

    @Aspect @Component
    class WireMockTranslator {
        private final HttpMock httpMock;

        public WireMockTranslator(HttpMock httpMock) {
            this.httpMock = httpMock;
        }

        private void redirect(ProceedingJoinPoint jp, String service, Map<String, Class> respTypes) throws Throwable {
            Object[] args = jp.getArgs();
            String method = jp.getStaticPart().getSignature().getName();
            ServerCallStreamObserver observer = (ServerCallStreamObserver) args[1];
            observer.onNext(httpMock.send(args[0], service + method, respTypes.get(method)));
            observer.onCompleted();
        }
        @decorate_services@
    }
}