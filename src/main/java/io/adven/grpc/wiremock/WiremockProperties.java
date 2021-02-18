package io.adven.grpc.wiremock;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConfigurationProperties("wiremock.server")
@ConstructorBinding
public class WiremockProperties {
    private final boolean disableRequestJournal;
    private final boolean asynchronousResponseEnabled;
    private final Integer asynchronousResponseThreads;
    private final Integer jettyAcceptors;
    private final Integer containerThreads;
    private final boolean stubRequestLoggingDisabled;
    private final boolean verbose;

    public WiremockProperties(boolean disableRequestJournal, boolean asynchronousResponseEnabled, Integer asynchronousResponseThreads, Integer jettyAcceptors, Integer containerThreads, boolean stubRequestLoggingDisabled, boolean verbose) {
        this.disableRequestJournal = disableRequestJournal;
        this.asynchronousResponseEnabled = asynchronousResponseEnabled;
        this.asynchronousResponseThreads = asynchronousResponseThreads;
        this.jettyAcceptors = jettyAcceptors;
        this.containerThreads = containerThreads;
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

    public Integer getJettyAcceptors() {
        return jettyAcceptors;
    }

    public Integer getContainerThreads() {
        return containerThreads;
    }
}
