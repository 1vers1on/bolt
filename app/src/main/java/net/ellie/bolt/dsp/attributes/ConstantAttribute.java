package net.ellie.bolt.dsp.attributes;

import net.ellie.bolt.dsp.DspPipeline;

public class ConstantAttribute<T> extends PipelineAttribute<T> {
    private T value;

    public ConstantAttribute(T value) {
        this.value = value;
    }

    public void setValue(T value) {
        this.value = value;
        notifyChange(value);
    }

    @Override
    public T resolve(DspPipeline pipeline) {
        return value;
    }
}
