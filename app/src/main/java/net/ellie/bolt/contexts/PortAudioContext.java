package net.ellie.bolt.contexts;

import net.ellie.bolt.jni.portaudio.PortAudioJNI;

public class PortAudioContext {
    private static volatile PortAudioContext instance;
    private final PortAudioJNI pa;

    private PortAudioContext() {
        pa = new PortAudioJNI();
    }

    public static PortAudioContext getInstance() {
        if (instance == null) {
            synchronized (PortAudioContext.class) {
                if (instance == null) {
                    instance = new PortAudioContext();
                }
            }
        }
        return instance;
    }

    public PortAudioJNI getPortAudioJNI() {
        return pa;
    }

    public void terminate() {
        pa.terminate();
        instance = null;
    }
}
