package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;
import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.DspPipeline;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;
import net.ellie.bolt.dsp.attributes.PipelineAttribute;

public class Average extends AbstractPipelineStep {
    private PipelineAttribute<Integer> windowSizeAttr;
    private double[] windowBuffer;
    private int windowSize;
    private int currentIndex;
    private boolean isWindowFull;
    private double sum;

    public Average(PipelineAttribute<Integer> windowSizeAttr, DspPipeline pipeline) {
        this.windowSizeAttr = windowSizeAttr;
        this.windowSize = windowSizeAttr.resolve(pipeline);
        if (this.windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be positive");
        }
        this.windowBuffer = new double[this.windowSize];
        this.currentIndex = 0;
        this.isWindowFull = false;
        this.sum = 0;

        this.windowSizeAttr.addListener(newValue -> {
            this.windowSize = newValue;
            this.windowBuffer = new double[this.windowSize];
            this.currentIndex = 0;
            this.isWindowFull = false;
            this.sum = 0;
        });
    }

    @Override
    public int process(double[] buffer, int length, DspPipeline pipeline) {
        if (buffer == null || length <= 0 || length > buffer.length) {
            return 0;
        }

        int resolvedWindowSize = windowSizeAttr.resolve(pipeline);
        if (resolvedWindowSize != windowSize) {
            windowSize = resolvedWindowSize;
            windowBuffer = new double[windowSize];
            currentIndex = 0;
            isWindowFull = false;
            sum = 0;
        }

        for (int i = 0; i < length; i++) {
            if (isWindowFull) {
                sum -= windowBuffer[currentIndex];
            }

            windowBuffer[currentIndex] = buffer[i];
            sum += buffer[i];

            currentIndex = (currentIndex + 1) % windowSize;

            if (!isWindowFull && currentIndex == 0) {
                isWindowFull = true;
            }

            int count = isWindowFull ? windowSize : currentIndex;
            double average = sum / count;

            buffer[i] = average;
        }

        return length;
    }

    @Override
    public void reset() {
        currentIndex = 0;
        isWindowFull = false;
        sum = 0;
        for (int i = 0; i < windowSize; i++) {
            windowBuffer[i] = 0;
        }
    }

    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return new Pair<>(NumberType.REAL, NumberType.REAL);
    }

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.FILTER;
    }
}