package net.ellie.bolt.decoder;

final class TextData implements DecoderPipelineData {
    String value;
    public TextData(String value) { this.value = value; }
    public String get() { return value; }
}

