package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;

import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.DspPipeline;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;
import net.ellie.bolt.dsp.attributes.PipelineAttribute;

public class RealFIRFilter extends AbstractPipelineStep {
    private PipelineAttribute<double[]> tapsAttr;
    private double[] taps;
    private double[] delay;
    private int delayIdx;

    public RealFIRFilter(PipelineAttribute<double[]> tapsAttr, DspPipeline pipeline) {
        this.tapsAttr = tapsAttr;
        this.taps = tapsAttr.resolve(pipeline).clone();
        this.delay = new double[this.taps.length];
        this.delayIdx = 0;
        this.tapsAttr.addListener(newTaps -> {
            if (newTaps == null || newTaps.length == 0) {
                throw new IllegalArgumentException("FIR taps must be non-empty");
            }
            this.taps = newTaps.clone();
            this.delay = new double[newTaps.length];
            this.delayIdx = 0;
        });
    }
    
    @Override
    public int process(double[] buffer, int length, DspPipeline pipeline) {
        if (buffer == null) return 0;
        double[] currentTaps = tapsAttr.resolve(pipeline);
        int L = currentTaps.length;

        for (int n = 0; n < length; n++) {
            delay[delayIdx] = buffer[n];

            double acc = 0.0;
            int di = delayIdx;

            for (int k = 0; k < L; k++) {
                acc += delay[di] * currentTaps[k];
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

    public void setTaps(PipelineAttribute<double[]> tapsAttr, DspPipeline pipeline) {
        if (tapsAttr == null || tapsAttr.resolve(pipeline).length == 0) {
            throw new IllegalArgumentException("FIR taps must be non-empty");
        }
        this.tapsAttr = tapsAttr;
        this.taps = tapsAttr.resolve(pipeline).clone();
        this.delay = new double[this.taps.length];
        this.delayIdx = 0;
    }
}