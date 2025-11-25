package net.ellie.bolt.jni.rtlsdr;

public class RTLSDR {
    static {
        System.loadLibrary("native-jni");
    }

    private long nativeHandle = 0;

    public static native int getDeviceCount();
    public static native String getDeviceName(int index);
    
    private native long nOpen(int deviceIndex);
    private native int nClose(long device);

    private native int nSetXtalFreq(long device, int rtlFreq, int tunerFreq);

    private native int nWriteEeprom(long device, byte[] eeprom, int offset, int length);
    private native int nReadEeprom(long device, byte[] eeprom, int offset, int length);

    private native int nSetCenterFreq(long device, int freq);
    private native int nGetCenterFreq(long device);
    
    private native int nSetFreqCorrection(long device, int ppm);
    private native int nGetFreqCorrection(long device);

    private native int nGetTunerType(long device);
    
    private native int nGetTunerGains(long device, int[] gains);
    private native int nSetTunerGain(long device, int gain);

    private native int nSetTunerBandwidth(long device, int bandwidth);

    private native int nGetTunerGain(long device);
    private native int nSetTunerIFGain(long device, int stage, int gain);
    private native int nSetTunerGainMode(long device, boolean manual);

    private native int nSetSampleRate(long device, int rate);
    private native int nGetSampleRate(long device);

    private native int nSetTestmode(long device, boolean on);
    private native int nSetAgcMode(long device, boolean on);
    private native int nSetDirectSampling(long device, int mode);
    private native int nGetDirectSampling(long device);

    private native int nSetOffsetTuning(long device, boolean on);
    private native int nGetOffsetTuning(long device);

    private native int nSetBiasTee(long device, boolean on);
    private native int nSetBiasTeeGpio(long device, int gpio, boolean on);

    private native int nResetBuffer(long device);
    private native int nReadSync(long device, byte[] buffer, int length, int[] n_read);

    public int open(int deviceIndex) {
        nativeHandle = nOpen(deviceIndex);
        return nativeHandle == 0 ? -1 : 0;
    }

    public int close() {
        int result = nClose(nativeHandle);
        nativeHandle = 0;
        return result;
    }

    public void setXtalFreq(int rtlFreq, int tunerFreq) {
        if (nSetXtalFreq(nativeHandle, rtlFreq, tunerFreq) != 0) {
            throw new RuntimeException("Failed to set XTAL frequencies");
        }
    }

    public void writeEeprom(byte[] eeprom, int offset, int length) {
        int result = nWriteEeprom(nativeHandle, eeprom, offset, length);
        if (result == -1) {
            throw new RuntimeException("Invalid device handle");
        } else if (result == -2) {
            throw new RuntimeException("EEPROM size exceeded");
        } else if (result == -3) {
            throw new RuntimeException("No EEPROM found");
        }
    }

    public void readEeprom(byte[] eeprom, int offset, int length) {
        int result = nReadEeprom(nativeHandle, eeprom, offset, length);
        if (result == -1) {
            throw new RuntimeException("Invalid device handle");
        } else if (result == -2) {
            throw new RuntimeException("EEPROM size exceeded");
        } else if (result == -3) {
            throw new RuntimeException("No EEPROM found");
        }
    }

    public int setCenterFreq(int freq) {
        return nSetCenterFreq(nativeHandle, freq);
    }

    public int getCenterFreq() {
        int freq = nGetCenterFreq(nativeHandle);
        if (freq <= 0) {
            throw new RuntimeException("Failed to get center frequency");
        }
        return freq;
    }

    public void setFreqCorrection(int ppm) {
        if (nSetFreqCorrection(nativeHandle, ppm) != 0) {
            throw new RuntimeException("Failed to set frequency correction");
        }
    }

    public int getFreqCorrection() {
        return nGetFreqCorrection(nativeHandle);
    }

    public RTLTuner getTunerType() {
        int tunerType = nGetTunerType(nativeHandle);
        if (tunerType < 0 || tunerType >= RTLTuner.values().length) {
            return RTLTuner.RTLSDR_TUNER_UNKNOWN;
        }
        return RTLTuner.values()[tunerType];
    }

    public int getTunerGains(int[] gains) {
        int result = nGetTunerGains(nativeHandle, gains);
        if (result == 0) {
            throw new RuntimeException("Failed to get tuner gains");
        }
        return result;
    }

    public void setTunerGain(int gain) {
        if (nSetTunerGain(nativeHandle, gain) != 0) {
            throw new RuntimeException("Failed to set tuner gain");
        }
    }

    public void setTunerBandwidth(int bandwidth) {
        if (nSetTunerBandwidth(nativeHandle, bandwidth) != 0) {
            throw new RuntimeException("Failed to set tuner bandwidth");
        }
    }

    public int getTunerGain() {
        int gain = nGetTunerGain(nativeHandle);
        if (gain == 0) {
            throw new RuntimeException("Failed to get tuner gain");
        }
        return gain;
    }

    public void setTunerIFGain(int stage, int gain) {
        if (nSetTunerIFGain(nativeHandle, stage, gain) != 0) {
            throw new RuntimeException("Failed to set tuner IF gain");
        }
    }

    public void setTunerGainMode(boolean manual) {
        if (nSetTunerGainMode(nativeHandle, manual) != 0) {
            throw new RuntimeException("Failed to set tuner gain mode");
        }
    }

    public void setSampleRate(int rate) {
        if (nSetSampleRate(nativeHandle, rate) != 0) {
            throw new RuntimeException("Failed to set sample rate");
        }
    }

    public int getSampleRate() {
        int rate = nGetSampleRate(nativeHandle);
        if (rate <= 0) {
            throw new RuntimeException("Failed to get sample rate");
        }
        return rate;
    }

    public void setTestmode(boolean on) {
        if (nSetTestmode(nativeHandle, on) != 0) {
            throw new RuntimeException("Failed to set test mode");
        }
    }

    public void setAgcMode(boolean on) {
        if (nSetAgcMode(nativeHandle, on) != 0) {
            throw new RuntimeException("Failed to set AGC mode");
        }
    }

    public void setDirectSampling(int mode) {
        if (nSetDirectSampling(nativeHandle, mode) != 0) {
            throw new RuntimeException("Failed to set direct sampling mode");
        }
    }

    public int getDirectSampling() {
        int mode = nGetDirectSampling(nativeHandle);
        if (mode < 0) {
            throw new RuntimeException("Failed to get direct sampling mode");
        }
        return mode;
    }

    public void setOffsetTuning(boolean on) {
        if (nSetOffsetTuning(nativeHandle, on) != 0) {
            throw new RuntimeException("Failed to set offset tuning");
        }
    }

    public boolean getOffsetTuning() {
        int result = nGetOffsetTuning(nativeHandle);
        if (result < 0) {
            throw new RuntimeException("Failed to get offset tuning");
        }
        return result != 0;
    }

    public void setBiasTee(boolean on) {
        if (nSetBiasTee(nativeHandle, on) != 0) {
            throw new RuntimeException("Failed to set bias tee");
        }
    }

    public void setBiasTeeGpio(int gpio, boolean on) {
        if (nSetBiasTeeGpio(nativeHandle, gpio, on) != 0) {
            throw new RuntimeException("Failed to set bias tee GPIO");
        }
    }

    public int resetBuffer() {
        return nResetBuffer(nativeHandle);
    }

    public int readSync(byte[] buffer, int length) {
        int[] nRead = new int[1];
        int result = nReadSync(nativeHandle, buffer, length, nRead);
        if (result < 0) {
            throw new RuntimeException("Failed to read from RTL-SDR - " + result);
        }
        return nRead[0];
    }

    public int readSync(byte[] buffer, int length, int[] nRead) {
        int result = nReadSync(nativeHandle, buffer, length, nRead);
        if (result < 0) {
            throw new RuntimeException("Failed to read from RTL-SDR - " + result);
        }
        return nRead[0];
    }

    public static enum RTLTuner {
        RTLSDR_TUNER_UNKNOWN,
        RTLSDR_TUNER_E4000,
        RTLSDR_TUNER_FC0012,
        RTLSDR_TUNER_FC0013,
        RTLSDR_TUNER_FC2580,
        RTLSDR_TUNER_R820T,
        RTLSDR_TUNER_R828D;
    };
}
