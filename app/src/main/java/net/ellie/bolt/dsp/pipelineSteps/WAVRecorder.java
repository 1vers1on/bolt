package net.ellie.bolt.dsp.pipelineSteps;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.commons.math3.util.Pair;

import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;

public class WAVRecorder extends AbstractPipelineStep {
    private final List<Float> recordingBuffer;
    private final int sampleRate;
    private volatile boolean isRecording = false;

    public WAVRecorder(int sampleRate) {
        this.sampleRate = sampleRate;
        this.recordingBuffer = new ArrayList<>();
    }

    @Override
    public int process(double[] buffer, int length) {
        if (isRecording) {
            for (int i = 0; i < length; i++) {
                recordingBuffer.add((float)buffer[i]);
            }
        }
        return length;
    }

    @Override
    public void reset() {
        recordingBuffer.clear();
        isRecording = false;
    }

    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return Pair.create(NumberType.REAL, NumberType.REAL);
    }

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.RECORDING;
    }

    public void startRecording() {
        recordingBuffer.clear();
        isRecording = true;
    }

    public void stopRecordingAndSave(String folderPath) throws IOException {
        isRecording = false;

        float[] samples = new float[recordingBuffer.size()];
        for (int i = 0; i < recordingBuffer.size(); i++) {
            samples[i] = recordingBuffer.get(i);
        }

        byte[] pcmData = floatToPCM16(samples);

        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);

        AudioInputStream audioInputStream = new AudioInputStream(
                new ByteArrayInputStream(pcmData),
                format,
                samples.length
        );

        Path filePath = Path.of(folderPath, "recording_" + System.currentTimeMillis() + ".wav");
        File folder = new File(folderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        AudioSystem.write(audioInputStream, javax.sound.sampled.AudioFileFormat.Type.WAVE, filePath.toFile());
    }

    private static byte[] floatToPCM16(float[] samples) {
        byte[] out = new byte[samples.length * 2];

        for (int i = 0; i < samples.length; i++) {
            float f = Math.max(-1f, Math.min(1f, samples[i]));
            short s = (short)(f * 32767f);

            out[i * 2] = (byte)(s & 0xff);
            out[i * 2 + 1] = (byte)((s >> 8) & 0xff);
        }

        return out;
    }
}
