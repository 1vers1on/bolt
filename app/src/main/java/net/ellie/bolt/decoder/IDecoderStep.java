package net.ellie.bolt.decoder;

public interface IDecoderStep<I, O extends DecoderPipelineData> {
    O decode(I inputData);

    void reset();
    DecoderOutputTypes getOutputType();
}
