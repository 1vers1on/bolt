
package net.ellie.bolt.dsp.pipelineSteps;

import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.DspPipeline;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;
import net.ellie.bolt.dsp.attributes.PipelineAttribute;
import org.apache.commons.math3.util.Pair;

public class AutomaticGainControl extends AbstractPipelineStep {
    private PipelineAttribute<Double> targetRmsDb;
    private PipelineAttribute<Double> minGainDb;
    private PipelineAttribute<Double> maxGainDb;
    private PipelineAttribute<Double> attackTimeSec;
    private PipelineAttribute<Double> releaseTimeSec;
    private PipelineAttribute<Double> sampleRate;

    private double attackCoeff;
    private double releaseCoeff;
    private double envelope;
    private double currentGainDb;

    public AutomaticGainControl(
            PipelineAttribute<Double> targetRmsDb,
            PipelineAttribute<Double> minGainDb,
            PipelineAttribute<Double> maxGainDb,
            PipelineAttribute<Double> attackTimeSec,
            PipelineAttribute<Double> releaseTimeSec,
            PipelineAttribute<Double> sampleRate,
            DspPipeline pipeline) {
        this.targetRmsDb = targetRmsDb;
        this.minGainDb = minGainDb;
        this.maxGainDb = maxGainDb;
        this.attackTimeSec = attackTimeSec;
        this.releaseTimeSec = releaseTimeSec;
        this.sampleRate = sampleRate;

        double fs = this.sampleRate.resolve(pipeline);
        this.attackCoeff = computeCoeff(this.attackTimeSec.resolve(pipeline), fs);
        this.releaseCoeff = computeCoeff(this.releaseTimeSec.resolve(pipeline), fs);

        this.attackTimeSec.addListener(newValue -> {
            this.attackCoeff = computeCoeff(newValue, this.sampleRate.resolve(pipeline));
        });
        this.releaseTimeSec.addListener(newValue -> {
            this.releaseCoeff = computeCoeff(newValue, this.sampleRate.resolve(pipeline));
        });
        this.sampleRate.addListener(newValue -> {
            this.attackCoeff = computeCoeff(this.attackTimeSec.resolve(pipeline), newValue);
            this.releaseCoeff = computeCoeff(this.releaseTimeSec.resolve(pipeline), newValue);
        });

        this.envelope = 1e-12;
        this.currentGainDb = 0.0;
    }

    private static double computeCoeff(double timeSec, double fs) {
        double tau = Math.max(1e-6, timeSec);
        return Math.exp(-1.0 / (tau * Math.max(1.0, fs)));
    }

    @Override
    public int process(double[] buffer, int length, DspPipeline pipeline) {
        if (buffer == null || length <= 0) {
            return 0;
        }

        int n = Math.min(length, buffer.length);

        double sumSq = 0.0;
        for (int i = 0; i < n; i++) {
            double x = buffer[i];
            sumSq += x * x;
        }
        double rms = Math.sqrt(sumSq / Math.max(1, n));
        if (Double.isNaN(rms) || Double.isInfinite(rms))
            rms = 1e-12;

        double targetRmsDbVal = targetRmsDb.resolve(pipeline);
        double minGainDbVal = minGainDb.resolve(pipeline);
        double maxGainDbVal = maxGainDb.resolve(pipeline);

        double coeff = (rms > envelope) ? attackCoeff : releaseCoeff;
        envelope = coeff * envelope + (1.0 - coeff) * rms;
        if (envelope < 1e-12)
            envelope = 1e-12;

        double envDb = 20.0 * Math.log10(envelope);
        double desiredGainDb = targetRmsDbVal - envDb;

        double maxStepDb = 3.0;
        double deltaDb = desiredGainDb - currentGainDb;
        if (deltaDb > maxStepDb)
            deltaDb = maxStepDb;
        else if (deltaDb < -maxStepDb)
            deltaDb = -maxStepDb;
        currentGainDb += deltaDb;

        if (currentGainDb < minGainDbVal)
            currentGainDb = minGainDbVal;
        if (currentGainDb > maxGainDbVal)
            currentGainDb = maxGainDbVal;

        double gainLin = Math.pow(10.0, currentGainDb / 20.0);
        for (int i = 0; i < n; i++) {
            buffer[i] *= gainLin;
        }

        return n;
    }

    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return new Pair<>(NumberType.REAL, NumberType.REAL);
    }

    @Override
    public void reset() {
        envelope = 1e-12;
        currentGainDb = 0.0;
    }

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.FILTER;
    }
}
