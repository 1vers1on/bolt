package net.ellie.bolt.decoder;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ellie.bolt.dsp.IPipelineStep;
import net.ellie.bolt.dsp.buffers.CircularFloatBuffer;

public class DecoderThread implements Runnable {
    private Logger logger = LoggerFactory.getLogger(DecoderThread.class);
    private volatile boolean running = true;
    private Thread thread;

    public DecoderPipeline<?, ? extends DecoderPipelineData> pipeline;

    private final CircularFloatBuffer audioInputBuffer;
    private final int sampleRate;

    public DecoderThread(CircularFloatBuffer audioInputBuffer, int sampleRate) {
        this.audioInputBuffer = audioInputBuffer;
        this.sampleRate = sampleRate;
    }

    public void start() {
        thread = new Thread(this, "DecoderThread");
        thread.start();
        logger.info("Decoder thread started");
    }

    public void run() {
        while (running) {
            // Decoding logic goes here
        }
    }

    public void stop() {
        running = false;
        try {
            if (thread != null) {
                thread.join();
            }
        } catch (InterruptedException e) {
            logger.error("Decoder thread interrupted during stop", e);
        }
        logger.info("Decoder thread stopped");
    }
}
