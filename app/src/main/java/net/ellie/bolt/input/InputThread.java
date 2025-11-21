package net.ellie.bolt.input;

import java.util.concurrent.atomic.AtomicBoolean;

import net.ellie.bolt.Configuration;
import net.ellie.bolt.dsp.buffers.CircularFloatBuffer;

public class InputThread implements Runnable {
    private final IInputSource inputSource;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final CircularFloatBuffer buffer;

    public InputThread(IInputSource inputSource) {
        this.inputSource = inputSource;
        this.buffer = new CircularFloatBuffer(Configuration.getFftSize() * 4 * (inputSource.isComplex() ? 2 : 1));
    }

    @Override
    public void run() {
        
    }

    public void stop() {
        running.set(false);
        inputSource.stop();
    }

    public boolean isRunning() {
        return running.get();
    }

    public IInputSource getInputSource() {
        return inputSource;
    }
}
