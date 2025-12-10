package net.ellie.bolt;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.List;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import net.ellie.bolt.jni.portaudio.AudioInputStream;
import net.ellie.bolt.jni.portaudio.PortAudioJNI;

public class Main {
    public static void main(String[] args) {
        // try {
        // Bolt.run();
        // } catch (Exception e) {
        // e.printStackTrace();
        // }

        final int durationSeconds = 5;
        final int channelsPreferred = 2;
        final double sampleRatePreferred = 48000.0;

        PortAudioJNI pa = new PortAudioJNI();
        int init = pa.initialize();
        if (init != 0) {
            System.err.println("Failed to initialize PortAudio (code=" + init + ")");
            return;
        }

        try {
            List<PortAudioJNI.DeviceInfo> devices = pa.enumerateDevices();
            if (devices == null || devices.isEmpty()) {
                System.err.println("No audio devices found");
                return;
            }

            for (PortAudioJNI.DeviceInfo d : devices) {
                System.out.printf("Device %d: %s (in: %d ch, out: %d ch, default SR: %.1f)\n",
                        d.index(), d.name(), d.maxInputChannels(), d.maxOutputChannels(), d.defaultSampleRate());
            }

            PortAudioJNI.DeviceInfo device = devices.get(27);

            int channels = Math.min(device.maxInputChannels(), channelsPreferred);
            double sampleRate = sampleRatePreferred;

            if (!pa.isFormatSupported(device.index(), channels, sampleRate)) {
                channels = Math.max(1, Math.min(device.maxInputChannels(), 1));
                sampleRate = device.defaultSampleRate() > 0 ? device.defaultSampleRate() : sampleRatePreferred;
            }

            long framesPerBuffer = 256;
            AudioInputStream inputStream = pa.openInputStream(device.index(), channels, sampleRate, framesPerBuffer);

            try (inputStream) {
                inputStream.start();

                int totalFrames = (int) (sampleRate * durationSeconds);
                int bytesPerSample = 2;
                int bytesPerFrame = bytesPerSample * channels;
                byte[] data = new byte[totalFrames * bytesPerFrame];

                int bytesCaptured = 0;
                while (bytesCaptured < data.length) {
                    int remaining = data.length - bytesCaptured;
                    int toRead = Math.max(bytesPerFrame, Math.min(remaining, bytesPerFrame * 1024));
                    int read = inputStream.read(data, bytesCaptured, toRead);
                    if (read <= 0)
                        break;
                    bytesCaptured += read;
                }

                int framesCaptured = bytesCaptured / bytesPerFrame;
                float[] floatInterleaved = pcm16LeBytesToFloats(data, framesCaptured, channels);

                float gain = 0.8f;
                for (int i = 0; i < floatInterleaved.length; i++) {
                    floatInterleaved[i] *= gain;
                }

                byte[] outBytes = floatsToPcm16LeBytes(floatInterleaved, framesCaptured, channels);

                AudioFormat format = new AudioFormat(
                        (float) sampleRate,
                        16,
                        channels,
                        true,
                        false
                );

                ByteArrayInputStream bais = new ByteArrayInputStream(outBytes);
                javax.sound.sampled.AudioInputStream ais = new javax.sound.sampled.AudioInputStream(bais, format,
                        framesCaptured);

                File out = new File("output.wav");
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out);
                System.out.printf("Saved %d frames to %s at %.0f Hz, %d ch (processed)\n", framesCaptured,
                        out.getAbsolutePath(), sampleRate, channels);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pa.terminate();
        }
    }

    private static float[] pcm16LeBytesToFloats(byte[] bytes, int frames, int channels) {
        int samples = frames * channels;
        float[] out = new float[samples];
        int bi = 0;
        for (int i = 0; i < samples; i++) {
            int lo = bytes[bi++] & 0xFF;
            int hi = bytes[bi++];
            int sample = (hi << 8) | lo;
            float f = sample >= 0 ? (sample / 32767.0f) : (sample / 32768.0f);
            out[i] = f;
        }
        return out;
    }

    private static byte[] floatsToPcm16LeBytes(float[] floats, int frames, int channels) {
        int samples = frames * channels;
        byte[] out = new byte[samples * 2];
        int bi = 0;
        for (int i = 0; i < samples; i++) {
            float f = floats[i];
            if (f > 1.0f)
                f = 1.0f;
            if (f < -1.0f)
                f = -1.0f;
            int s;
            if (f >= 0) {
                s = (int) Math.round(f * 32767.0);
            } else {
                s = (int) Math.round(f * 32768.0);
            }
            out[bi++] = (byte) (s & 0xFF);
            out[bi++] = (byte) ((s >>> 8) & 0xFF);
        }
        return out;
    }
}
