package net.ellie.bolt.dsp;

import java.util.concurrent.atomic.AtomicBoolean;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import net.ellie.bolt.config.Configuration;
import net.ellie.bolt.dsp.buffers.CircularFloatBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DspThread implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DspThread.class);

    private final CircularFloatBuffer inputBuffer;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private DoubleFFT_1D fft;
    private IWindow windowFunction;
    private CircularFloatBuffer waterfallOutputBuffer;

    public DspThread(CircularFloatBuffer inputBuffer) {
        this.inputBuffer = inputBuffer;
        this.fft = new DoubleFFT_1D(Configuration.getFftSize());
        this.waterfallOutputBuffer = new CircularFloatBuffer(Configuration.getFftSize());
    }

    public void start() {
        running.set(true);
        new Thread(this, "DspThread").start();
    }

    @Override
    public void run() {
        logger.info("DspThread started");
        running.set(true);

        int n = Configuration.getFftSize();
        double[] fftBuf = new double[n * 2];


        while (running.get()) {
            try {
                inputBuffer.read(fftBuf, 0, n * 2);
            } catch (InterruptedException e) {
                running.set(false);
                logger.error("DspThread interrupted during read", e);
                break;
            }

            windowFunction.apply(fftBuf, n);

            fft.complexForward(fftBuf);

            double[] magnitude = new double[n];
            double gain = windowFunction.getGainCompensation(n);
            for (int i = 0; i < n; i++) {
                double real = fftBuf[2 * i];
                double imag = fftBuf[2 * i + 1];
                magnitude[i] = 10.0 * Math.log10(real * real + imag * imag) + 20.0 * Math.log10(gain);
            }

            try {
                waterfallOutputBuffer.write(magnitude, 0, n);
            } catch (InterruptedException e) {
                running.set(false);
                logger.error("DspThread interrupted during write", e);
                break;
            }
        }
    }

    public void stop() {
        running.set(false);
    }

    public CircularFloatBuffer getWaterfallOutputBuffer() {
        return waterfallOutputBuffer;
    }
}
