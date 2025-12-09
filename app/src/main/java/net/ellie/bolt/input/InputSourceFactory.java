package net.ellie.bolt.input;

import net.ellie.bolt.config.Configuration;
import net.ellie.bolt.input.sources.complex.DummyInputSource;
import net.ellie.bolt.input.sources.complex.RTLSDRInputSource;
import net.ellie.bolt.input.sources.real.PortAudioInputSource;

public class InputSourceFactory {
    public static CloseableInputSource createInputSource() {
        switch (Configuration.getInputDevice()) {
            case "RTL-SDR":
                return new RTLSDRInputSource(Configuration.getRtlSdrConfig().getRtlSdrDeviceIndex(),
                        Configuration.getRtlSdrConfig().getRtlSdrSampleRate(),
                        Configuration.getRtlSdrConfig().getRtlSdrCenterFrequency());
            case "Dummy":
                return new DummyInputSource(Configuration.getDummyConfig().getSampleRate());
            case "PortAudio":
                return new PortAudioInputSource(Configuration.getPortAudioConfig().getDeviceIndex(),
                        Configuration.getPortAudioConfig().getChannelCount(),
                        Configuration.getPortAudioConfig().getSampleRate(),
                        Configuration.getPortAudioConfig().getFramesPerBuffer());
            case "File":
                throw new UnsupportedOperationException("File input source not implemented yet");
            default:
                throw new IllegalArgumentException("Unknown input source type");
        }
    }
}
