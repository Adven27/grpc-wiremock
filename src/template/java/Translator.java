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

@Configuration
public class Translator{
    @services@

    @Aspect @Component
    class WireMockTranslator {
        private final HttpMock httpMock;

        public WireMockTranslator(HttpMock httpMock) {
            this.httpMock = httpMock;
        }

        private void redirect(ProceedingJoinPoint jp, String service, Map<String, Class> respTypes, Map<String, String> methodTypes) throws Throwable {
            Object[] args = jp.getArgs();
            String method = jp.getStaticPart().getSignature().getName();
            String methodType = methodTypes.get(method.toLowerCase());
            ServerCallStreamObserver observer = (ServerCallStreamObserver) args[1];
            if (methodType.equals("SERVER_STREAMING")) {
                int streamCursor = 0;
                HttpMock.Response response;
                do {
                    response = httpMock.request(service + "/" + method + "/" + Integer.toString(++streamCursor), args[0]);
                    observer.onNext(response.getMessage(respTypes.get(method.toLowerCase())));
                } while(response.isContinueStreaming() && !observer.isCancelled());
            } else {
                observer.onNext(httpMock.request(service + "/" + method, args[0]).getMessage(respTypes.get(method.toLowerCase())));
            }
            observer.onCompleted();
        }
        @decorate_services@
    }
}
