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

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Aspect @Component
    static class WireMockTranslator {
        private final HttpMock httpMock;

        public WireMockTranslator(HttpMock httpMock) {
            this.httpMock = httpMock;
        }

        private void redirect(ProceedingJoinPoint jp, String service, Map<String, Class> respTypes, Map<String, String> methodTypes) throws Throwable {
            final Object[] args = jp.getArgs();
            final String method = jp.getStaticPart().getSignature().getName();
            final String path = service + "/" + method;
            final String methodType = methodTypes.get(method.toLowerCase());
            final Class respType = respTypes.get(method.toLowerCase());
            final ServerCallStreamObserver observer = (ServerCallStreamObserver) args[1];

            if (methodType.equals("SERVER_STREAMING")) {
                int streamCursor = 1;
                HttpMock.Response response;
                do {
                    response = httpMock.request(path, args[0], new HashMap(Map.of("streamCursor", "" + streamCursor++)));
                    observer.onNext(response.getMessage(respType));
                } while (streamCursor <= response.streamSize() && !observer.isCancelled());
            } else {
                observer.onNext(httpMock.request(path, args[0]).getMessage(respType));
            }
            observer.onCompleted();
        }
        @decorate_services@
    }
}
