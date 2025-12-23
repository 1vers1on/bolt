
package net.ellie.bolt.dsp.pipelineSteps;

import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.DspPipeline;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;
import net.ellie.bolt.dsp.attributes.PipelineAttribute;
import org.apache.commons.math3.util.Pair;

public class FIRFilter extends AbstractPipelineStep {
    private PipelineAttribute<double[]> taps;
    private double[] delay;
    private int delayIdx;

    public FIRFilter(PipelineAttribute<double[]> tapsAttr, DspPipeline pipeline) {
        this.taps = tapsAttr;
        double[] resolvedTaps = tapsAttr.resolve(pipeline);
        if (resolvedTaps == null || resolvedTaps.length == 0) {
            throw new IllegalArgumentException("FIR taps must be non-empty");
        }
        this.delay = new double[resolvedTaps.length * 2];
        this.delayIdx = 0;
        // Listen for taps changes to resize delay buffer
        this.taps.addListener(newTaps -> {
            if (newTaps == null || newTaps.length == 0) {
                throw new IllegalArgumentException("FIR taps must be non-empty");
            }
            this.delay = new double[newTaps.length * 2];
            this.delayIdx = 0;
        });
    }

    @Override
    public int process(double[] buffer, int length, DspPipeline pipeline) {
        if (buffer == null) return 0;
        if ((length & 1) != 0) {
            throw new IllegalArgumentException("Complex buffer length must be even (interleaved re, im)");
        }
        double[] currentTaps = taps.resolve(pipeline);
        int complexSamples = length / 2;
        int L = currentTaps.length;

        for (int n = 0; n < complexSamples; n++) {
            int writePos = delayIdx * 2;
            delay[writePos] = buffer[n * 2];
            delay[writePos + 1] = buffer[n * 2 + 1];

            double accRe = 0.0;
            double accIm = 0.0;

            int di = delayIdx;
            for (int k = 0; k < L; k++) {
                int pos = di * 2;
                double tk = currentTaps[k];
                accRe += delay[pos] * tk;
                accIm += delay[pos + 1] * tk;

                di--;
                if (di < 0) di = L - 1;
            }

            buffer[n * 2] = accRe;
            buffer[n * 2 + 1] = accIm;

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
        return new Pair<>(NumberType.COMPLEX, NumberType.COMPLEX);
    }

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.FILTER;
    }

    public void setTaps(PipelineAttribute<double[]> tapsAttr, DspPipeline pipeline) {
        double[] resolvedTaps = tapsAttr.resolve(pipeline);
        if (resolvedTaps == null || resolvedTaps.length == 0) {
            throw new IllegalArgumentException("FIR taps must be non-empty");
        }
        this.taps = tapsAttr;
        this.delay = new double[resolvedTaps.length * 2];
        this.delayIdx = 0;
    }
}