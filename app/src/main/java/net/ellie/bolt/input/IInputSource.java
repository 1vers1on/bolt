package net.ellie.bolt.input;

import java.io.IOException;

public interface IInputSource {
    /**
     * Read samples from the source into the buffer.
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

    boolean isComplex();
}
