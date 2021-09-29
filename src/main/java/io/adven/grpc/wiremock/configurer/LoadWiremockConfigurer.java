package io.adven.grpc.wiremock.configurer;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

@Profile("load")
@Component
public class LoadWiremockConfigurer extends DefaultWiremockConfigurer {
    private static final String DISABLE_REQUEST_LOGGING = "--disable-request-logging";
    private static final String DISABLE_REQUEST_JOURNAL = "--no-request-journal";
    private static final String ASYNCHRONOUS_RESPONSE_ENABLED = "--async-response-enabled";
    private static final String ASYNCHRONOUS_RESPONSE_THREADS = "--async-response-threads";

    @Override
    public String[] configure(String... args) {
        List<String> options = new ArrayList<>(asList(super.configure(args)));
        options.add(DISABLE_REQUEST_LOGGING);
        options.add(DISABLE_REQUEST_JOURNAL);
        options.add(ASYNCHRONOUS_RESPONSE_ENABLED);
        options.add(ASYNCHRONOUS_RESPONSE_THREADS + "=10");
        return options.toArray(new String[0]);
    }
}
