package net.ellie.bolt.contexts;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.ALC11.*;

public class OpenALAudioContext {
    private long device = 0;
    private long context = 0;
    private int source = 0;

    private final int sampleRate;
    private final int format;

    private final int[] buffers;
    private final int bufferSize;

    // Capture (audio input) fields
    private long captureDevice = 0;
    private int captureChannels = 0; // 1 or 2

    public OpenALAudioContext(int sampleRate, boolean stereo, int bufferCount, int bufferSizeBytes) {
        this.sampleRate = sampleRate;
        this.format = stereo ? AL_FORMAT_STEREO16 : AL_FORMAT_MONO16;
        this.buffers = new int[bufferCount];
        this.bufferSize = bufferSizeBytes;
    }

    public static List<String> listDevices() {
        List<String> devs = new ArrayList<>();

        String raw = alcGetString(0, ALC_DEVICE_SPECIFIER);
        if (raw == null) return devs;

        if (!raw.contains("\0")) {
            devs.add(raw);
            return devs;
        }

        int idx = 0;
        while (idx < raw.length()) {
            int end = raw.indexOf('\0', idx);
            if (end < 0) break;
            String d = raw.substring(idx, end);
            if (!d.isEmpty()) {
                devs.add(d);
            }
            idx = end + 1;
        }

        return devs;
    }

    public static List<String> listCaptureDevices() {
        List<String> devs = new ArrayList<>();

        String raw = alcGetString(0, ALC_CAPTURE_DEVICE_SPECIFIER);
        if (raw == null) return devs;

        if (!raw.contains("\0")) {
            devs.add(raw);
            return devs;
        }

        int idx = 0;
        while (idx < raw.length()) {
            int end = raw.indexOf('\0', idx);
            if (end < 0) break;
            String d = raw.substring(idx, end);
            if (!d.isEmpty()) {
                devs.add(d);
            }
            idx = end + 1;
        }

        return devs;
    }


    public boolean openDevice(String namePart) {
        List<String> devs = listDevices();
        String chosen = null;

        for (String d : devs) {
            if (d.toLowerCase().contains(namePart.toLowerCase())) {
                chosen = d;
                break;
            }
        }

        if (chosen == null) return false;

        device = alcOpenDevice(chosen);
        return device != MemoryUtil.NULL;
    }

    public void openDefaultDevice() {
        device = alcOpenDevice((ByteBuffer) null);
    }

    public void init() {
        if (device == MemoryUtil.NULL) {
            throw new IllegalStateException("device not opened");
        }

        context = alcCreateContext(device, (IntBuffer) null);
        alcMakeContextCurrent(context);
        AL.createCapabilities(ALC.createCapabilities(device));

        source = alGenSources();

        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = alGenBuffers();
        }

        byte[] silent = new byte[bufferSize];
        ByteBuffer silentBuf = MemoryUtil.memAlloc(bufferSize);
        silentBuf.put(silent).flip();
        for (int b : buffers) {
            silentBuf.rewind();
            alBufferData(b, format, silentBuf, sampleRate);
        }
        MemoryUtil.memFree(silentBuf);

        alSourceQueueBuffers(source, buffers);
        alSourcePlay(source);
    }

    public boolean openCaptureDevice(String namePart, int sampleRate, boolean stereo, int bufferSizeBytes) {
        List<String> devs = listCaptureDevices();
        String chosen = null;

        for (String d : devs) {
            if (d.toLowerCase().contains(namePart.toLowerCase())) {
                chosen = d;
                break;
            }
        }

        if (chosen == null) return false;

        int fmt = stereo ? AL_FORMAT_STEREO16 : AL_FORMAT_MONO16;
        int channels = stereo ? 2 : 1;

        captureDevice = alcCaptureOpenDevice(chosen, sampleRate, fmt, bufferSizeBytes / (2 * channels));
        if (captureDevice == MemoryUtil.NULL) return false;

    captureChannels = channels;
        return true;
    }

    public boolean openDefaultCaptureDevice(int sampleRate, boolean stereo, int bufferSizeBytes) {
        int fmt = stereo ? AL_FORMAT_STEREO16 : AL_FORMAT_MONO16;
        int channels = stereo ? 2 : 1;
        captureDevice = alcCaptureOpenDevice((ByteBuffer) null, sampleRate, fmt, bufferSizeBytes / (2 * channels));
    if (captureDevice == MemoryUtil.NULL) return false;
    captureChannels = channels;
        return true;
    }

    public void startCapture() {
        if (captureDevice == MemoryUtil.NULL) throw new IllegalStateException("capture device not opened");
        alcCaptureStart(captureDevice);
    }

    public void stopCapture() {
        if (captureDevice == MemoryUtil.NULL) return;
        alcCaptureStop(captureDevice);
    }

    public void closeCaptureDevice() {
        if (captureDevice == MemoryUtil.NULL) return;
        alcCaptureStop(captureDevice);
        alcCaptureCloseDevice(captureDevice);
        captureDevice = 0;
    }

    public byte[] readCapturedBytes(int samples) {
        if (captureDevice == MemoryUtil.NULL) throw new IllegalStateException("capture device not opened");
        if (samples <= 0) return new byte[0];

        int available = alcGetInteger(captureDevice, ALC_CAPTURE_SAMPLES);
        while (available < samples) {
            try { Thread.sleep(1); } catch (InterruptedException ignored) {}
            available = alcGetInteger(captureDevice, ALC_CAPTURE_SAMPLES);
        }

        int bytesPerSample = 2 * captureChannels;
        int totalBytes = samples * bytesPerSample;
        ByteBuffer bb = MemoryUtil.memAlloc(totalBytes);
        try {
            alcCaptureSamples(captureDevice, bb, samples);
            bb.flip();
            byte[] out = new byte[bb.remaining()];
            bb.get(out);
            return out;
        } finally {
            MemoryUtil.memFree(bb);
        }
    }

    public float[] readCapturedFloats(int samples) {
        byte[] raw = readCapturedBytes(samples);
        int frames = raw.length / (2 * captureChannels);
        float[] out = new float[frames * captureChannels];
        int idx = 0;
        for (int i = 0; i < frames; i++) {
            for (int ch = 0; ch < captureChannels; ch++) {
                int lo = raw[idx++] & 0xff;
                int hi = raw[idx++] & 0xff;
                short s = (short) ((hi << 8) | lo);
                out[i * captureChannels + ch] = s / 32768f;
            }
        }
        return out;
    }

    public void write(byte[] data) {
        int processed = alGetSourcei(source, AL_BUFFERS_PROCESSED);
        while (processed <= 0) {
            int state = alGetSourcei(source, AL_SOURCE_STATE);
            if (state != AL_PLAYING) {
                alSourcePlay(source);
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {}
            processed = alGetSourcei(source, AL_BUFFERS_PROCESSED);
        }

        int buf = alSourceUnqueueBuffers(source);

        ByteBuffer dbuf = MemoryUtil.memAlloc(data.length);
        try {
            dbuf.put(data).flip();
            alBufferData(buf, format, dbuf, sampleRate);
        } finally {
            MemoryUtil.memFree(dbuf);
        }

        alSourceQueueBuffers(source, buf);

        int state = alGetSourcei(source, AL_SOURCE_STATE);
        if (state != AL_PLAYING) {
            alSourcePlay(source);
        }
    }

    public void writeFloats(float[] samples) {
        ByteBuffer bb = MemoryUtil.memAlloc(bufferSize);

        for (float v : samples) {
            v = Math.max(-1f, Math.min(1f, v));
            short s = (short) (v * 32767f);
            bb.put((byte) (s & 0xff));
            bb.put((byte) ((s >> 8) & 0xff));
        }

        bb.flip();
        byte[] raw = new byte[bb.remaining()];
        bb.get(raw);
        MemoryUtil.memFree(bb);

        write(raw);
    }

    public void destroy() {
        alSourceStop(source);

        for (int b : buffers) {
            alDeleteBuffers(b);
        }

        alDeleteSources(source);

        if (captureDevice != MemoryUtil.NULL) {
            try { alcCaptureStop(captureDevice); } catch (Throwable ignored) {}
            try { alcCaptureCloseDevice(captureDevice); } catch (Throwable ignored) {}
            captureDevice = 0;
        }

        alcMakeContextCurrent(0);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }
}
