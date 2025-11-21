package net.ellie.bolt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

public class Resources {
    public static ByteBuffer readResourceToByteBuffer(String resourcePath) throws IOException {
        try (InputStream in = Resources.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            
            byte[] data = in.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(data.length);
            buffer.put(data);
            buffer.flip();
            return buffer;
        }
    }
}
