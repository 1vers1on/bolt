package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;

import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;

public class RealFIRFilter extends AbstractPipelineStep {
    private double[] taps;
    private double[] delay;
    private int delayIdx;

    public RealFIRFilter(double[] taps) {
        if (taps == null || taps.length == 0) {
            throw new IllegalArgumentException("FIR taps must be non-empty");
        }
        this.taps = taps.clone();
        this.delay = new double[taps.length];
        this.delayIdx = 0;
    }

    public RealFIRFilter() {
        this(new double[] { 1.0 });
    }

    @Override
    public int process(double[] buffer, int length) {
        if (buffer == null) return 0;
        int L = taps.length;

        for (int n = 0; n < length; n++) {
            delay[delayIdx] = buffer[n];

            double acc = 0.0;
            int di = delayIdx;

            for (int k = 0; k < L; k++) {
                acc += delay[di] * taps[k];
                di--;
                if (di < 0) di = L - 1;
            }

            buffer[n] = acc;

            delayIdx++;
            if (delayIdx >= L) delayIdx = 0;
        }

        return length;
    }

    @Override
    public void reset() {
        for (int i = 0; i < delay.length; i++) delay[i] = 0.0;
        delayIdx = 0;
    }

    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return new Pair<>(NumberType.REAL, NumberType.REAL);
    }

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.FILTER;
    }

    public void setTaps(double[] taps) {
        if (taps == null || taps.length == 0) {
            throw new IllegalArgumentException("FIR taps must be non-empty");
        }
        this.taps = taps.clone();
        this.delay = new double[taps.length];
        this.delayIdx = 0;
    }
}