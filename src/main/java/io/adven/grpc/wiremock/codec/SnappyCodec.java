package io.adven.grpc.wiremock.codec;

import io.grpc.Codec;
import org.springframework.stereotype.Component;
import org.xerial.snappy.SnappyFramedInputStream;
import org.xerial.snappy.SnappyFramedOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Component("snappy")
public class SnappyCodec implements Codec {
    @Override
    public String getMessageEncoding() {
        return "snappy";
    }

    @Override
    public InputStream decompress(InputStream is) throws IOException {
        return new SnappyFramedInputStream(is);
    }

    @Override
    public OutputStream compress(OutputStream os) throws IOException {
        return new SnappyFramedOutputStream(os);
    }
}
