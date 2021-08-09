package io.adven.grpc.wiremock.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties("wiremock.server")
@ConstructorBinding
public class WiremockProperties {
    private final boolean disableRequestJournal;
    private final boolean asynchronousResponseEnabled;
    private final Integer asynchronousResponseThreads;
    private final boolean stubRequestLoggingDisabled;
    private final boolean verbose;

    public WiremockProperties(boolean disableRequestJournal, boolean asynchronousResponseEnabled, Integer asynchronousResponseThreads, boolean stubRequestLoggingDisabled, boolean verbose) {
        this.disableRequestJournal = disableRequestJournal;
        this.asynchronousResponseEnabled = asynchronousResponseEnabled;
        this.asynchronousResponseThreads = asynchronousResponseThreads;
        this.stubRequestLoggingDisabled = stubRequestLoggingDisabled;
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isDisableRequestJournal() {
        return disableRequestJournal;
    }

    public Integer getAsynchronousResponseThreads() {
        return asynchronousResponseThreads;
    }

    public boolean isAsynchronousResponseEnabled() {
        return asynchronousResponseEnabled;
    }

    public boolean isStubRequestLoggingDisabled() {
        return stubRequestLoggingDisabled;
    }
}
