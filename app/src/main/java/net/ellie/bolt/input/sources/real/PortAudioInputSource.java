package net.ellie.bolt.input.sources.real;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ellie.bolt.contexts.PortAudioContext;
import net.ellie.bolt.input.CloseableInputSource;
import net.ellie.bolt.jni.portaudio.AudioInputStream;
import net.ellie.bolt.jni.portaudio.PortAudioJNI;

public class PortAudioInputSource implements CloseableInputSource {
    private final Logger logger = LoggerFactory.getLogger(PortAudioInputSource.class);
    private volatile boolean running = true;
    private AudioInputStream audioInputStream;
    private final int sampleRate;
    private final int channelCount;
    private final int framesPerBuffer;
    private byte[] byteBuffer; // re-used between reads to avoid allocations
    
    public PortAudioInputSource(int deviceIndex, int channels, double sampleRate, long framesPerBuffer) {
        PortAudioJNI pa = PortAudioContext.getInstance().getPortAudioJNI();
        
        PortAudioJNI.DeviceInfo device = null;
        List<PortAudioJNI.DeviceInfo> devices = pa.enumerateDevices();
        for (PortAudioJNI.DeviceInfo d : devices) {
            if (d.index() == deviceIndex) {
                device = d;
                break;
            }
        }
        
        if (device == null) {
            throw new RuntimeException("Device not found: " + deviceIndex);
        }
        
        int maxInputChannels = device.maxInputChannels();
        
        int actualChannels = channels;
        if (channels == 1 && maxInputChannels < 1) {
            actualChannels = Math.max(1, maxInputChannels);
            logger.warn("Device {} doesn't support {} channels, using {} channels instead", 
                    device.name(), channels, actualChannels);
        }
        
        this.sampleRate = (int) sampleRate;
        this.channelCount = Math.max(1, actualChannels);
        this.framesPerBuffer = (int) Math.max(1, framesPerBuffer);
        
        logger.info("Opening PortAudio input stream with deviceIndex={}, channels={} (requested: {}), sampleRate={}, framesPerBuffer={}",
                deviceIndex, this.channelCount, channels, sampleRate, framesPerBuffer);
        
        audioInputStream = PortAudioContext.getInstance().getPortAudioJNI()
            .openInputStream(deviceIndex, this.channelCount, sampleRate, framesPerBuffer);

        this.byteBuffer = new byte[this.framesPerBuffer * this.channelCount * 8];
        logger.info("PortAudio input stream opened successfully");

        audioInputStream.start();
    }

    @Override
    public int read(float[] buffer, int offset, int length) throws InterruptedException, IOException {
        if (!running || audioInputStream == null) {
            return 0;
        }

        int bytesToRead = Math.min(byteBuffer.length, length * 2 * channelCount);
        int bytesRead = audioInputStream.read(byteBuffer, 0, bytesToRead);
        if (bytesRead <= 0) {
            return 0;
        }

        int framesCaptured = bytesRead / (2 * channelCount);
        int framesToCopy = Math.min(framesCaptured, length);

        int bi = 0;
        for (int i = 0; i < framesToCopy; i++) {
            float sum = 0;
            int channelsRead = 0;
            for (int j = 0; j < channelCount; j++) {
                if (bi + 1 >= byteBuffer.length) break;
                
                int lo = byteBuffer[bi++] & 0xFF;
                int hi = byteBuffer[bi++];
                int sample = (hi << 8) | lo;
                float f = sample >= 0 ? (sample / 32767.0f) : (sample / 32768.0f);
                sum += f;
                channelsRead++;
            }

            if (channelsRead > 0) {
                buffer[offset + i] = sum / channelsRead;
            } else {
                buffer[offset + i] = 0;
            }
        }

        return framesToCopy;
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public void stop() {
        running = false;
        if (audioInputStream != null) {
            audioInputStream.stop();
            audioInputStream.close();
            audioInputStream = null;
        }
    }

    @Override
    public String getName() {
        return "PortAudio Input Source";
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isComplex() {
        return false;
    }

    @Override
    public void close() {
        stop();
    }
}