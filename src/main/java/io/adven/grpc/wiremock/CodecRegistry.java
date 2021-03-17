package io.adven.grpc.wiremock;

import io.grpc.Codec;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@PropertySource("classpath:application.properties")
public class CodecRegistry {

    @Value("#{'${external.codecs}'.split(',')}")
    private List<String> externalCodecs;
    private Map<String, Codec> codecs;

    public CodecRegistry(Map<String, Codec> codecs) {
        this.codecs = codecs;
    }

    public CompressorRegistry compressorRegistry() {
        CompressorRegistry compressorRegistry = CompressorRegistry.getDefaultInstance();
        for (String codec : externalCodecs) {
            Codec codecImpl = codecs.get(codec);
            if (codecImpl != null) {
                compressorRegistry.register(codecs.get(codec));
            }
        }
        return compressorRegistry;
    }

    public DecompressorRegistry decompressorRegistry() {
        DecompressorRegistry decompressorRegistry = DecompressorRegistry.getDefaultInstance();
        for (String codec : externalCodecs) {
            Codec codecImpl = codecs.get(codec);
            if (codecImpl != null) {
                decompressorRegistry = decompressorRegistry.with(codecs.get(codec), true);
            }
        }
        return decompressorRegistry;
    }
}
