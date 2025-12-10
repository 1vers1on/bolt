package net.ellie.bolt.decoder;

import java.util.ArrayList;
import java.util.List;

public class DecoderPipeline<I, O extends DecoderPipelineData> {
    private final List<IDecoderStep<?, ?>> steps = new ArrayList<>();

    public <T extends DecoderPipelineData> DecoderPipeline<I, T> addStep(IDecoderStep<O, T> step) {
        steps.add(step);
        return (DecoderPipeline<I, T>) this;
    }

    public O execute(I input) {
        Object current = input;
        for (IDecoderStep<?, ?> step : steps) {
            current = ((IDecoderStep<Object, ? extends DecoderPipelineData>) step).decode(current);
        }
        return (O) current;
    }

    public void reset() {
        for (IDecoderStep<?, ?> step : steps) {
            step.reset();
        }
    }
}
