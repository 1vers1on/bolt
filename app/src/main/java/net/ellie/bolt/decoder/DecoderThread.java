package net.ellie.bolt.decoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ellie.bolt.dsp.buffers.CircularFloatBuffer;
import net.ellie.bolt.decoder.buffer.CircularByteBuffer;
import net.ellie.bolt.decoder.buffer.CircularCharBuffer;

public class DecoderThread implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(DecoderThread.class);
    private volatile boolean running = true;
    private Thread thread;

    public DecoderPipeline<float[], ? extends DecoderPipelineData> pipeline;

    private final CircularFloatBuffer audioInputBuffer;
    private final int sampleRate;

    private CircularByteBuffer rawByteOutputBuffer;
    private CircularCharBuffer textOutputBuffer;

    public DecoderThread(CircularFloatBuffer audioInputBuffer, int sampleRate) {
        this.audioInputBuffer = audioInputBuffer;
        this.sampleRate = sampleRate;
        this.rawByteOutputBuffer = new CircularByteBuffer(1024);
        this.textOutputBuffer = new CircularCharBuffer(1024);
    }

    public void start() {
        thread = new Thread(this, "DecoderThread");
        thread.start();
        logger.info("Decoder thread started");
    }


    public void run() {
        final int bufferSize = 1024;
        float[] buffer = new float[bufferSize];
        while (running) {
            int read = audioInputBuffer.readNonBlocking(buffer, 0, bufferSize);
            if (read > 0 && pipeline != null) {
                float[] input = buffer;
                if (read < bufferSize) {
                    input = new float[read];
                    System.arraycopy(buffer, 0, input, 0, read);
                }
                DecoderPipelineData output = pipeline.execute(input);
                if (output instanceof TextData textData) {
                    String text = textData.get();
                    textOutputBuffer.writeNonBlocking(text.toCharArray(), 0, text.length());
                } else if (output instanceof BytesData bytesData) {
                    byte[] bytes = bytesData.get();
                    rawByteOutputBuffer.writeNonBlocking(bytes, 0, bytes.length);
                }
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public CircularByteBuffer getRawByteOutputBuffer() {
        return rawByteOutputBuffer;
    }

    public CircularCharBuffer getTextOutputBuffer() {
        return textOutputBuffer;
    }

    public DecoderOutputTypes getOutputType() {
        if (pipeline == null) {
            return null;
        }
        Class<? extends DecoderPipelineData> outputClass = pipeline.execute(new float[0]).getClass();
        if (outputClass == TextData.class) {
            return DecoderOutputTypes.TEXT;
        } else if (outputClass == BytesData.class) {
            return DecoderOutputTypes.RAW_BYTES;
        } else {
            return null;
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
