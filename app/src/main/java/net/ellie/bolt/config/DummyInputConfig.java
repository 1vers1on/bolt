package net.ellie.bolt.config;

public class DummyInputConfig {
    private int sampleRate;

    public DummyInputConfig(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }
}
