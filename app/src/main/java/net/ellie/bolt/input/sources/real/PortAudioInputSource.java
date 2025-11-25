package net.ellie.bolt.input.sources.real;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import net.ellie.bolt.contexts.PortAudioContext;
import net.ellie.bolt.input.CloseableInputSource;
import net.ellie.bolt.jni.portaudio.AudioInputStream;

public class PortAudioInputSource implements CloseableInputSource {
    private volatile boolean running = true;
    private AudioInputStream audioInputStream;
    private final int sampleRate;
    
    public PortAudioInputSource(int deviceIndex, int channels, double sampleRate, long framesPerBuffer) {
        this.sampleRate = (int) sampleRate;
        audioInputStream = PortAudioContext.getInstance().getPortAudioJNI()
            .openInputStream(deviceIndex, channels, sampleRate, framesPerBuffer);
        audioInputStream.start();
    }

    @Override
    public int read(float[] buffer, int offset, int length) throws InterruptedException, IOException {
        if (!running || audioInputStream == null) {
            return 0;
        }
        byte[] byteBuffer = new byte[length * 2];
        int bytesRead = audioInputStream.read(byteBuffer, 0, byteBuffer.length);
        if (bytesRead <= 0) {
            return 0;
        }

        int samplesRead = bytesRead / 2;
        ByteBuffer.wrap(byteBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(new short[samplesRead]);
        for (int i = 0; i < samplesRead; i++) {
            short sample = (short) ((byteBuffer[i * 2 + 1] << 8) | (byteBuffer[i * 2] & 0xFF));
            buffer[offset + i] = sample / 32768.0f;
        }

        return samplesRead;
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public void stop() {
        running = false;
        if (audioInputStream != null) {
            audioInputStream.stop();
            audioInputStream.close();
            audioInputStream = null;
        }
    }

    @Override
    public String getName() {
        return "PortAudio Input Source";
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isComplex() {
        return false;
    }

    @Override
    public void close() {
        stop();
    }
}
