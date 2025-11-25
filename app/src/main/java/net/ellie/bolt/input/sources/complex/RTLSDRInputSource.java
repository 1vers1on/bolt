package net.ellie.bolt.input.sources.complex;

import net.ellie.bolt.input.CloseableInputSource;
import net.ellie.bolt.jni.rtlsdr.RTLSDR;

public class RTLSDRInputSource implements CloseableInputSource {
    private volatile boolean running = true;
    private final RTLSDR rtlSdr;
    private final int deviceIndex;

    public RTLSDRInputSource(int deviceIndex, int sampleRate, int centerFreq) {
        rtlSdr = new RTLSDR();
        this.deviceIndex = deviceIndex;
        if (rtlSdr.open(deviceIndex) != 0) {
            throw new RuntimeException("Failed to open RTL-SDR device at index " + deviceIndex);
        }
        rtlSdr.setSampleRate(sampleRate);
        rtlSdr.setCenterFreq(centerFreq);
        rtlSdr.resetBuffer();
    }

    @Override
    public int read(float[] buffer, int offset, int length) throws InterruptedException {
        if (!running) {
            return 0;
        }
        byte[] byteBuffer = new byte[length]; // TODO: see if this size is correct or not
        int bytesRead = rtlSdr.readSync(byteBuffer, length);
        if (bytesRead <= 0) {
            return 0;
        }
        for (int i = 0; i < bytesRead; i++) {
            // Convert unsigned byte to float in range [-1.0, 1.0]
            int sample = (byteBuffer[i] & 0xFF) - 128;
            buffer[offset + i] = sample / 128.0f;
        }
        return bytesRead;
    }

    @Override
    public int getSampleRate() {
        return rtlSdr.getSampleRate();
    }

    @Override
    public void stop() {
        rtlSdr.close();
        running = false;
    }

    @Override
    public String getName() {
        return RTLSDR.getDeviceName(deviceIndex);
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
