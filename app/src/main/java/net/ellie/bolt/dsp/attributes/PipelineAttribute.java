package net.ellie.bolt.dsp.attributes;

import java.util.ArrayList;
import java.util.List;

import net.ellie.bolt.dsp.DspPipeline;

public abstract class PipelineAttribute<T> {
    private final List<AttributeChangeListener<T>> listeners = new ArrayList<>();
    private T lastValue = null;

    public abstract T resolve(DspPipeline pipeline);

    public void addListener(AttributeChangeListener<T> listener) {
        listeners.add(listener);
    }

    protected void notifyChange(T newValue) {
        for (AttributeChangeListener<T> listener : listeners) {
            listener.onChange(newValue);
        }
    }

    public void checkChange(DspPipeline pipeline) {
        T value = resolve(pipeline);
        if (lastValue == null || !lastValue.equals(value)) {
            lastValue = value;
            notifyChange(value);
        }
    }
}
