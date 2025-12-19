package net.ellie.bolt.decoder.decoders;

import net.ellie.bolt.decoder.DecoderOutputTypes;
import net.ellie.bolt.decoder.IDecoderStep;
import net.ellie.bolt.decoder.TextData;

public class CWDecoder implements IDecoderStep<float[], TextData> {
    @Override
    public TextData decode(float[] inputData) {
        String decodedText = ""; // TODO: implement CW decoding logic here
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
