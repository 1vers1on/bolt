package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;

import net.ellie.bolt.config.Configuration;
import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;

public class AutomaticGainControl extends AbstractPipelineStep {
    private final double targetRmsDb;
    private final double minGainDb;
    private final double maxGainDb;

    private final int sampleRate;
    private final double attackCoeff;
    private final double releaseCoeff;

    private double envelope;
    private double currentGainDb;

    public AutomaticGainControl(double targetRmsDb, double minGainDb, double maxGainDb,
            double attackTimeSec, double releaseTimeSec, int sampleRate) {
        this.targetRmsDb = targetRmsDb;
        this.minGainDb = minGainDb;
        this.maxGainDb = maxGainDb;
        this.sampleRate = sampleRate > 0 ? sampleRate : Configuration.getSampleRate();

        this.attackCoeff = computeCoeff(attackTimeSec, this.sampleRate);
        this.releaseCoeff = computeCoeff(releaseTimeSec, this.sampleRate);

        this.envelope = 1e-12;
        this.currentGainDb = 0.0;
    }

    public AutomaticGainControl() {
        this(-20.0, -24.0, 24.0, 0.010, 0.200, Configuration.getSampleRate());
    }

    private static double computeCoeff(double timeSec, int fs) {
        double tau = Math.max(1e-6, timeSec);
        return Math.exp(-1.0 / (tau * Math.max(1, fs)));
    }

    @Override
    public int process(double[] buffer, int length) {
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

        double coeff = (rms > envelope) ? attackCoeff : releaseCoeff;
        envelope = coeff * envelope + (1.0 - coeff) * rms;
        if (envelope < 1e-12)
            envelope = 1e-12;

        double envDb = 20.0 * Math.log10(envelope);

        double desiredGainDb = targetRmsDb - envDb;

        double maxStepDb = 3.0;
        double deltaDb = desiredGainDb - currentGainDb;
        if (deltaDb > maxStepDb)
            deltaDb = maxStepDb;
        else if (deltaDb < -maxStepDb)
            deltaDb = -maxStepDb;
        currentGainDb += deltaDb;

        if (currentGainDb < minGainDb)
            currentGainDb = minGainDb;
        if (currentGainDb > maxGainDb)
            currentGainDb = maxGainDb;

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
