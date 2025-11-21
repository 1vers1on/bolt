package net.ellie.bolt.input;

import java.io.IOException;

public interface IComplexInputSource {
    /**
     * Read samples from the source into the buffer.
     * Reads len/2 interleaved complex samples. 
     * @param buffer Destination float array.
     * @param offset Index to start writing at.
     * @param length Number of floats to read.
     * @return Number of floats read.
     * @throws InterruptedException
     */
    int read(float[] buffer, int offset, int length) throws InterruptedException, IOException;

    int getSampleRate();

    void stop();

    String getName();

    boolean isRunning();
}
