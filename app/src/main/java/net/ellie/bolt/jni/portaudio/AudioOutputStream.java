package net.ellie.bolt.jni.portaudio;

import java.io.IOException;

public class AudioOutputStream implements AutoCloseable {
    private final long streamPtr;
    private final int channels;
    private final int bytesPerSample = 2; // paInt16
    private boolean started = false;

    public AudioOutputStream(long streamPtr, int channels) {
        this.streamPtr = streamPtr;
        this.channels = channels;
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

    public int write(byte[] buffer) throws IOException {
        return write(buffer, 0, buffer.length);
    }

    public int write(byte[] buffer, int offset, int byteCount) throws IOException {
        if (byteCount <= 0) return 0;
        int bytesPerFrame = bytesPerSample * channels;

        int framesToWrite = byteCount / bytesPerFrame;
        if (framesToWrite == 0) return 0;

        long framesWritten =
                PortAudioJNI.nativeWriteStreamOffset(streamPtr, buffer, offset, framesToWrite);

        if (framesWritten < 0) {
            throw new IOException("PortAudio write error (code=" + framesWritten + ")");
        }

        return (int) framesWritten * bytesPerFrame;
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
