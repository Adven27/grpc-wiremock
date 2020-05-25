package io.adven.grpc.wiremock;

import io.grpc.stub.ServerCallStreamObserver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
public class WireMockTranslator {
    private final HttpMock httpMock;

    public WireMockTranslator(HttpMock httpMock) {
        this.httpMock = httpMock;
    }

    @Around("execution(* @package@.@service@Grpc.@service@ImplBase.*(..))")
    public void redirectAllToHttpMock(ProceedingJoinPoint jp) throws Throwable {
        Object[] args = jp.getArgs();
        ServerCallStreamObserver observer = (ServerCallStreamObserver) args[1];
        observer.onNext(httpMock.send(args[0], respType(((MethodSignature) jp.getStaticPart().getSignature()).getMethod())));
        observer.onCompleted();
    }

    private Class<?> respType(Method name) throws ClassNotFoundException {
        return Arrays.stream(Class.forName("@package@.@service@Grpc$@service@BlockingStub").getMethods())
            .filter(method -> method.getName().equals(name.getName()))
            .findFirst().orElseThrow().getReturnType();
    }
}