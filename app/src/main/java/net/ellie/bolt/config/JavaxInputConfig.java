package net.ellie.bolt.config;

public class JavaxInputConfig {
    private String deviceName;
    private int sampleRate;
    private int channelCount;
    private int bufferSize;

    public JavaxInputConfig(String deviceName, int sampleRate, int channelCount, int bufferSize) {
        this.deviceName = deviceName;
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        this.bufferSize = bufferSize;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
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

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        Configuration.saver.save(Configuration.INSTANCE);
    }
}
