package net.ellie.bolt.decoder.decoders;

import net.ellie.bolt.decoder.DecoderOutputTypes;
import net.ellie.bolt.decoder.IDecoderStep;
import net.ellie.bolt.decoder.TextData;

public class RandomTextOutputDecoder implements IDecoderStep<float[], TextData> {
    @Override
    public TextData decode(float[] inputData) {
        String decodedText = "abcd";
        return new TextData(decodedText);
    }

    @Override
    public void reset() {
    }

    @Override
    public DecoderOutputTypes getOutputType() {
        return DecoderOutputTypes.TEXT;
    }
}
