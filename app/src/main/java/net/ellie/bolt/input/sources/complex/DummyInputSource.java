package net.ellie.bolt.input.sources.complex;

import net.ellie.bolt.input.CloseableInputSource;

public class DummyInputSource implements CloseableInputSource {
    private volatile boolean running = true;
    private final int sampleRate;

    // Carrier and FM parameters
    private final double carrierFreq = 1000.0;   // Carrier frequency in Hz
    private final double modFreq = 100.0;        // FM modulation frequency in Hz
    private final double freqDeviation = 400.0;  // Frequency deviation for FM in Hz

    private double phase = 0.0;      // Carrier phase
    private double modPhase = 0.0;   // Modulator phase

    public DummyInputSource(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    @Override
    public int read(float[] buffer, int offset, int length) throws InterruptedException {
        if (!running)
            return 0;

        // Precompute increments
        final double twoPi = 2.0 * Math.PI;
        final double modPhaseInc = twoPi * modFreq / sampleRate;

        for (int i = 0; i < length; i += 2) {
            // Advance modulator phase (100 Hz sine)
            modPhase += modPhaseInc;
            if (modPhase >= twoPi)
                modPhase -= twoPi;

            // Instantaneous frequency = carrier + deviation * sin(modPhase)
            double instFreq = carrierFreq + freqDeviation * Math.sin(modPhase);
            double phaseInc = twoPi * instFreq / sampleRate;

            // Advance carrier phase using instantaneous frequency
            phase += phaseInc;
            if (phase >= twoPi)
                phase -= twoPi;

            // Output complex IQ: I = cos(phase), Q = sin(phase)
            buffer[offset + i] = (float) Math.cos(phase);
            buffer[offset + i + 1] = (float) Math.sin(phase);
        }

        return length;
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public String getName() {
    return "Dummy Sine Input Source";
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    @Override
    public void close() {
        stop();
    }
}
