package net.ellie.bolt.jni.portaudio;

import java.util.List;

public class PortAudioJNI {
    static {
        System.loadLibrary("native-jni");
    }

    public record DeviceInfo(
        int index,
        String name,
        String hostApi,
        int maxInputChannels,
        int maxOutputChannels,
        double defaultSampleRate
    ) {}

    public native int initialize();
    
    public native void terminate();

    public native List<DeviceInfo> enumerateDevices();

    public native boolean isFormatSupported(int deviceIndex, int channels, double sampleRate);

    static native long nativeOpenInputStream(int deviceIndex, int channels, double sampleRate, long framesPerBuffer);

    static native void nativeStartStream(long streamPtr);

    static native void nativeStopStream(long streamPtr);

    static native void nativeCloseStream(long streamPtr);

    static native long nativeReadStream(long streamPtr, byte[] buffer, long framesToRead, int channels);

    static native long nativeReadStreamOffset(long streamPtr, byte[] buffer, int offset, long framesToRead, int channels, int inputDeviceIndex);

    public AudioInputStream openInputStream(int deviceIndex, int channels, double sampleRate, long framesPerBuffer) {
        long streamPtr = nativeOpenInputStream(deviceIndex, channels, sampleRate, framesPerBuffer);
        if (streamPtr == 0) {
            throw new IllegalStateException("Failed to open input stream");
        }
        return new AudioInputStream(streamPtr, channels, deviceIndex);
    }

    // -------- OUTPUT --------
    static native long nativeOpenOutputStream(int deviceIndex, int channels, double sampleRate, long framesPerBuffer);

    static native long nativeWriteStream(long streamPtr, byte[] buffer, long framesToWrite);

    static native long nativeWriteStreamOffset(long streamPtr, byte[] buffer, int offset, long framesToWrite);

    public AudioOutputStream openOutputStream(int deviceIndex, int channels, double sampleRate, long framesPerBuffer) {
        long streamPtr = nativeOpenOutputStream(deviceIndex, channels, sampleRate, framesPerBuffer);
        if (streamPtr == 0) {
            throw new IllegalStateException("failed to open output stream");
        }
        return new AudioOutputStream(streamPtr, channels);
    }
}
