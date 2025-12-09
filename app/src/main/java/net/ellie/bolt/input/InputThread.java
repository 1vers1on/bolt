package net.ellie.bolt.input;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import net.ellie.bolt.config.Configuration;
import net.ellie.bolt.dsp.buffers.CircularFloatBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(InputThread.class);

    private final CloseableInputSource inputSource;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final CircularFloatBuffer buffer;

    private final float[] readBuffer;

    private Thread localInputThread = null;

    public InputThread(CloseableInputSource inputSource) {
        this.inputSource = inputSource;

        int complexFactor = inputSource.isComplex() ? 2 : 1;

        int bufferSize = Configuration.getFftSize() * 32 * complexFactor; // TODO: figure out the correct size
        this.buffer = new CircularFloatBuffer(bufferSize);

        this.readBuffer = new float[4 * 16384 * complexFactor]; // TODO: maybe increase the size
    }

    public void start() {
        running.set(true);
        localInputThread = new Thread(this, "InputThread-" + inputSource.getName());
        localInputThread.start();
    }

    @Override
    public void run() {
        logger.info("InputThread for {} started", inputSource.getName());
        try {
            while (running.get()) {
                int samplesRead = inputSource.read(readBuffer, 0, readBuffer.length);
                if (samplesRead > 0) {
                    buffer.write(readBuffer, 0, samplesRead);
                } else {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { inputSource.close(); } catch (Exception ignored) {}
        }
    }

    public void stop() {
        running.set(false);
        if (localInputThread != null) {
            try {
                localInputThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        inputSource.stop();
        logger.info("InputThread for {} stopped", inputSource.getName());
    }

    public CircularFloatBuffer getBuffer() {
        return buffer;
    }
}
