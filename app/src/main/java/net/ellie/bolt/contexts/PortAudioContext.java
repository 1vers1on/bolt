package net.ellie.bolt.contexts;

import net.ellie.bolt.jni.portaudio.PortAudioJNI;
import net.ellie.bolt.jni.portaudio.PortAudioJNI.DeviceInfo;

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

    public DeviceInfo getDeviceInfoByName(String name) {
        for (DeviceInfo info : pa.enumerateDevices()) {
            System.out.println("Found device: " + info.name());
            if (info.name().toLowerCase().contains(name.toLowerCase())) {
                return info;
            }
        }
        return null;
    }

    public DeviceInfo getDeviceInfoByIndex(int index) {
        for (DeviceInfo info : pa.enumerateDevices()) {
            if (info.index() == index) {
                return info;
            }
        }
        return null;
    }

    public void terminate() {
        pa.terminate();
        instance = null;
    }
}
