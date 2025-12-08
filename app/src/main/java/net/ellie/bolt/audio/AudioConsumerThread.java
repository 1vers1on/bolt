package net.ellie.bolt.audio;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ellie.bolt.config.Configuration;
import net.ellie.bolt.contexts.OpenALAudioContext;
import net.ellie.bolt.dsp.buffers.CircularFloatBuffer;

public class AudioConsumerThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AudioConsumerThread.class);

    private final CircularFloatBuffer inputBuffer;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final OpenALAudioContext audioContext;

    public AudioConsumerThread(CircularFloatBuffer inputBuffer) {
        this.inputBuffer = inputBuffer;
        this.audioContext = new OpenALAudioContext(Configuration.getSampleRate(), false, 4,
                Configuration.getAudioBufferSize() * 2);
    }

    public void start() {
        running.set(true);
        new Thread(this, "AudioConsumerThread").start();
    }

    public void stop() {
        running.set(false);
    }

    @Override
public void run() {
    if (!audioContext.openDevice(Configuration.getAudioOutputDevice())) {
        audioContext.openDefaultDevice();
    }

    audioContext.init();

    float[] audioData = new float[Configuration.getAudioBufferSize()];

    while (running.get()) {
        int bytesRead = inputBuffer.readNonBlocking(audioData, 0, audioData.length);
        if (bytesRead == -1) {
            break;
        }
        
        if (bytesRead > 0) {
            float[] actualData = new float[bytesRead];
            System.arraycopy(audioData, 0, actualData, 0, bytesRead);
            audioContext.writeFloats(actualData);
        } else {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    audioContext.destroy();
}
}
