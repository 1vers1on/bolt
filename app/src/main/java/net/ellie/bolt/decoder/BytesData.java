package net.ellie.bolt.decoder;

public final class BytesData implements DecoderPipelineData {
    byte[] value;
    public BytesData(byte[] value) { this.value = value; }
    public byte[] get() { return value; }
}

