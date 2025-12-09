package net.ellie.bolt.config;

public class PortAudioInputConfig {
    private int deviceIndex;
    private int sampleRate;
    private int channelCount;
    private int framesPerBuffer;

    public PortAudioInputConfig(int deviceIndex, int sampleRate, int channelCount, int framesPerBuffer) {
        this.deviceIndex = deviceIndex;
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        this.framesPerBuffer = framesPerBuffer;
    }

    public int getDeviceIndex() {
        return deviceIndex;
    }

    public void setDeviceIndex(int deviceIndex) {
        this.deviceIndex = deviceIndex;
        Configuration.saver.save(Configuration.INSTANCE);
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        Configuration.saver.save(Configuration.INSTANCE);
    }

    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
        Configuration.saver.save(Configuration.INSTANCE);
    }

    public int getFramesPerBuffer() {
        return framesPerBuffer;
    }

    public void setFramesPerBuffer(int framesPerBuffer) {
        this.framesPerBuffer = framesPerBuffer;
        Configuration.saver.save(Configuration.INSTANCE);
    }
}
