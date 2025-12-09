package net.ellie.bolt.config;

public class RTLSDRConfig {
    private int rtlSdrDeviceIndex = 0;

    private int rtlSdrSampleRate = 2_400_000;
    private int[] rtlSdrSampleRates = {
        250_000,
        500_000,
        1_024_000,
        1_200_000,
        1_800_000,
        1_920_000,
        2_048_000,
        2_400_000,
        2_560_000,
        2_880_000,
        3_200_000
    };

    private int rtlSdrCenterFrequency = 101_500_000;

    public int getRtlSdrDeviceIndex() {
        return rtlSdrDeviceIndex;
    }

    public void setRtlSdrDeviceIndex(int newIndex) {
        rtlSdrDeviceIndex = newIndex;
        Configuration.saver.save(Configuration.INSTANCE);
    }

    public int getRtlSdrSampleRate() {
        return rtlSdrSampleRate;
    }

    public void setRtlSdrSampleRate(int newSampleRate) {
        rtlSdrSampleRate = newSampleRate;
        Configuration.saver.save(Configuration.INSTANCE);
    }

    public int[] getRtlSdrSampleRates() {
        return rtlSdrSampleRates;
    }

    public int getRtlSdrCenterFrequency() {
        return rtlSdrCenterFrequency;
    }

    public void setRtlSdrCenterFrequency(int newCenterFrequency) {
        rtlSdrCenterFrequency = newCenterFrequency;
        Configuration.saver.save(Configuration.INSTANCE);
    }
}
