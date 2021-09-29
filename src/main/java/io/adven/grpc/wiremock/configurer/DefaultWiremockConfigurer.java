package io.adven.grpc.wiremock.configurer;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

@Profile("!load")
@Component
public class DefaultWiremockConfigurer implements WiremockConfigurer {
    private static final String GLOBAL_RESPONSE_TEMPLATING = "--global-response-templating";
    private static final String LOCAL_RESPONSE_TEMPLATING = "--local-response-templating";
    private static final String ROOT_DIR = "--root-dir";
    private static final String PORT = "--port";

    @Override
    public String[] configure(String... args) {
        List<String> options = new ArrayList<>(asList(args));
        if (options.stream().noneMatch(it -> it.equals(GLOBAL_RESPONSE_TEMPLATING) || it.equals(LOCAL_RESPONSE_TEMPLATING))) {
            options.add(GLOBAL_RESPONSE_TEMPLATING);
        }
        if (options.stream().noneMatch(it -> it.startsWith(ROOT_DIR))) {
            options.add(ROOT_DIR + "=/wiremock");
        }
        if (options.stream().noneMatch(it -> it.startsWith(PORT))) {
            options.add(PORT + "=8888");
        }
        return options.toArray(new String[0]);
    }
}
