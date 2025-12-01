package net.ellie.bolt.input.sources.complex;

import net.ellie.bolt.input.CloseableInputSource;

public class DummyInputSource implements CloseableInputSource {
    private volatile boolean running = true;
    private final int sampleRate;

    private final double baseFreq = 100000.0;
    private final int harmonics = 2;

    private final double[] phases;

    public DummyInputSource(int sampleRate) {
        this.sampleRate = sampleRate;
        this.phases = new double[harmonics];
    }

    @Override
    public int read(float[] buffer, int offset, int length) throws InterruptedException {
        if (!running)
            return 0;

        for (int i = 0; i < length; i += 2) {
            double iAcc = 0.0;
            double qAcc = 0.0;

            for (int h = 1; h <= harmonics; h++) {
                double freq = h * baseFreq;
                double phaseInc = 2.0 * Math.PI * freq / sampleRate;

                phases[h - 1] += phaseInc;
                if (phases[h - 1] > Math.PI * 2)
                    phases[h - 1] -= Math.PI * 2;

                double cos = Math.cos(phases[h - 1]);
                double sin = Math.sin(phases[h - 1]);

                iAcc += cos;
                qAcc += sin;
            }

            buffer[offset + i] = (float) iAcc;
            buffer[offset + i + 1] = (float) qAcc;
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
        return "Dummy Comb Input Source";
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
