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

    // Throttling state
    private final int complexFactor;
    private final int sampleRate; // samples per second for one channel; effective rate accounts for complexFactor
    private long nextReadDeadlineNanos = 0L;

    private final float[] readBuffer;

    private Thread localInputThread = null;

    public InputThread(CloseableInputSource inputSource, int sampleRate) {
        this.inputSource = inputSource;

        this.complexFactor = inputSource.isComplex() ? 2 : 1;
        this.sampleRate = sampleRate;

        int bufferSize = Configuration.getFftSize() * 32 * complexFactor; // TODO: figure out the correct size
        logger.info("Creating InputThread buffer of size {} samples for source {}", bufferSize, inputSource.getName());
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
            nextReadDeadlineNanos = System.nanoTime();
            while (running.get()) {
                long now = System.nanoTime();
                long waitNanos = nextReadDeadlineNanos - now;
                if (waitNanos > 0) {
                    long waitMillis = waitNanos / 1_000_000L;
                    int waitExtraNanos = (int)(waitNanos % 1_000_000L);
                    if (waitMillis > 0 || waitExtraNanos > 0) {
                        try {
                            Thread.sleep(waitMillis, waitExtraNanos);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                
                int samplesRead = inputSource.read(readBuffer, 0, readBuffer.length);
                if (samplesRead > 0) {
                    logger.debug("Read {} samples from {}", samplesRead, inputSource.getName());
                    buffer.write(readBuffer, 0, samplesRead);
                    double secondsForChunk = (double) samplesRead / (double) (sampleRate * complexFactor);
                    long nanosForChunk = (long) (secondsForChunk * 1_000_000_000L);
                    nextReadDeadlineNanos = System.nanoTime() + nanosForChunk;
                } else {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    nextReadDeadlineNanos = System.nanoTime() + 1_000_000L;
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
