package net.ellie.bolt.jni.portaudio;

import java.io.IOException;

public class AudioInputStream implements AutoCloseable {
    private final long streamPtr;
    private final int channels;
    private final int bytesPerSample = 2; // paInt16
    private final int inputDeviceIndex;
    private boolean started = false;

    public AudioInputStream(long streamPtr, int channels, int inputDeviceIndex) {
        this.streamPtr = streamPtr;
        this.channels = channels;
        this.inputDeviceIndex = inputDeviceIndex;
    }

    public boolean isStarted() {
        return started;
    }

    public void start() {
        if (!started) {
            PortAudioJNI.nativeStartStream(streamPtr);
            started = true;
        }
    }

    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    public int read(byte[] buffer, int offset, int byteCount) throws IOException {
        if (byteCount <= 0) return 0;
        int bytesPerFrame = bytesPerSample * channels;

        int framesRequested = byteCount / bytesPerFrame;
        if (framesRequested == 0) return 0;

        long framesRead = PortAudioJNI.nativeReadStreamOffset(streamPtr, buffer, offset, framesRequested, channels, inputDeviceIndex);
        if (framesRead < 0) {
            throw new IOException("PortAudio read error (code=" + framesRead + ")");
        }
        return (int) framesRead * bytesPerFrame;
    }

    public void stop() {
        if (started) {
            PortAudioJNI.nativeStopStream(streamPtr);
            started = false;
        }
    }

    @Override
    public void close() {
        try {
            stop();
        } finally {
            PortAudioJNI.nativeCloseStream(streamPtr);
        }
    }
}
