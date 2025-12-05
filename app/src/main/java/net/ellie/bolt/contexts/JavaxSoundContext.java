package net.ellie.bolt.contexts;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public class JavaxSoundContext {
    private Mixer mixer;
    private SourceDataLine line;
    private AudioFormat format;

    public JavaxSoundContext(float sampleRate, int sampleSizeBits, int channels) {
        this.format = new AudioFormat(
                sampleRate,
                sampleSizeBits,
                channels,
                true,
                false
        );
    }

    public static List<String> listOutputDevices() {
        List<String> devices = new ArrayList<>();
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(info);
            if (mixer.getSourceLineInfo().length > 0) {
                devices.add(info.getName());
            }
        }
        return devices;
    }

    public boolean selectOutputDevice(String namePart) {
        Mixer.Info chosen = null;

        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().toLowerCase().contains(namePart.toLowerCase())) {
                Mixer mix = AudioSystem.getMixer(info);
                if (mix.getSourceLineInfo().length > 0) {
                    chosen = info;
                    break;
                }
            }
        }

        if (chosen == null) {
            return false;
        }

        this.mixer = AudioSystem.getMixer(chosen);
        return true;
    }

    public void open() throws LineUnavailableException {
        if (mixer == null) {
            throw new IllegalStateException("no mixer selected!");
        }

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        this.line = (SourceDataLine) mixer.getLine(info);
        line.open(format);
        line.start();
    }

    public void write(byte[] data) {
        if (line != null) {
            line.write(data, 0, data.length);
        }
    }

    public void writeFloats(float[] samples) {
        byte[] buf = new byte[samples.length * 2];
        int bi = 0;

        for (float s : samples) {
            s = Math.max(-1f, Math.min(1f, s));
            short v = (short) (s * 32767f);

            buf[bi++] = (byte) (v & 0xff);
            buf[bi++] = (byte) ((v >> 8) & 0xff);
        }

        write(buf);
    }

    public void close() {
        if (line != null) {
            line.drain();
            line.stop();
            line.close();
        }
    }
}
