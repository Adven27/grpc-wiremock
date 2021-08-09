package io.adven.grpc.wiremock;

import io.grpc.Codec;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CodecRegistry {
    private final List<String> externalCodecs;
    private final Map<String, Codec> codecs;

    public CodecRegistry(
        @Value("${external.codecs:}") List<String> externalCodecs,
        Map<String, Codec> codecs
    ) {
        this.codecs = codecs;
        this.externalCodecs = externalCodecs;
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
