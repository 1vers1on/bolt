package net.ellie.bolt.decoder.decoders;

import net.ellie.bolt.decoder.DecoderOutputTypes;
import net.ellie.bolt.decoder.IDecoderStep;
import net.ellie.bolt.decoder.TextData;

public class CWDecoder implements IDecoderStep<float[], TextData> {
    @Override
    public TextData decode(float[] inputData) {
        // Placeholder implementation for CW decoding
        String decodedText = ""; // Replace with actual decoding logic
        return new TextData(decodedText);
    }

    @Override
    public void reset() {
        // Reset any internal state if necessary
    }

    @Override
    public DecoderOutputTypes getOutputType() {
        return DecoderOutputTypes.TEXT;
    }
}
