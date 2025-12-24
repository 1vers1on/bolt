package net.ellie.bolt.dsp.pipelineSteps.demodulators;

import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.DspPipeline;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;
import net.ellie.bolt.dsp.attributes.ConstantAttribute;
import net.ellie.bolt.dsp.attributes.PipelineAttribute;

import org.apache.commons.math3.util.Pair;

public class SSBDemodulator extends AbstractPipelineStep {

    // Attributes
    private final PipelineAttribute<Boolean> isUSB;
    private final PipelineAttribute<Double> sampleRate;
    // Note: 'carrierFrequency' here acts as the Tuning Offset (how far to shift the spectrum)
    private final PipelineAttribute<Double> carrierFrequency; 
    
    // Configuration for Filtering and Decimation
    private final PipelineAttribute<Double> bandwidth;     // Filter cutoff (e.g., 3000 Hz)
    private final PipelineAttribute<Integer> decimation;   // Downsampling factor (e.g., 5 or 10)

    // Oscillator State
    private double phase;
    private double phaseIncrement;

    // Filter State (FIR Low Pass)
    private double[] filterTaps;
    private double[] historyI;
    private double[] historyQ;
    private int historyIndex = 0;
    
    // Flag to check if filter is initialized
    private boolean filterInitialized = false;

    public SSBDemodulator(
            PipelineAttribute<Boolean> isUSB, 
            PipelineAttribute<Double> sampleRate, 
            PipelineAttribute<Double> carrierFrequency,
            PipelineAttribute<Double> bandwidth,
            PipelineAttribute<Integer> decimation,
            DspPipeline pipeline) {
        
        this.isUSB = isUSB;
        this.sampleRate = sampleRate;
        this.carrierFrequency = carrierFrequency;
        this.bandwidth = (bandwidth != null) ? bandwidth : new ConstantAttribute<Double>(3000.0);
        this.decimation = (decimation != null) ? decimation : new ConstantAttribute<Integer>(1);

        double rate = this.sampleRate.resolve(pipeline);
        double offset = this.carrierFrequency.resolve(pipeline);
        this.phaseIncrement = 2.0 * Math.PI * offset / rate;
        this.phase = 0.0;

        initFilter(pipeline);

        this.carrierFrequency.addListener(newValue -> {
            this.phaseIncrement = 2.0 * Math.PI * newValue / this.sampleRate.resolve(pipeline);
        });

        this.sampleRate.addListener(newValue -> {
            this.phaseIncrement = 2.0 * Math.PI * this.carrierFrequency.resolve(pipeline) / newValue;
            initFilter(pipeline);
        });
        
        if (bandwidth != null) {
            this.bandwidth.addListener(v -> initFilter(pipeline));
        }
    }
    
    private void initFilter(DspPipeline pipeline) {
        int rate = this.sampleRate.resolve(pipeline).intValue();
        int bw = this.bandwidth.resolve(pipeline).intValue();
        
        int numTaps = 31;
        if (numTaps % 2 == 0) numTaps++;

        this.filterTaps = new double[numTaps];
        this.historyI = new double[numTaps];
        this.historyQ = new double[numTaps];
        this.historyIndex = 0;
        
        double M = (numTaps - 1) / 2.0;
        double normalizedCutoff = (double) bw / rate;

        for (int i = 0; i < numTaps; i++) {
            int n = i - (int) M;
            
            double tapVal;
            if (n == 0) {
                tapVal = 2.0 * normalizedCutoff;
            } else {
                tapVal = Math.sin(2.0 * Math.PI * normalizedCutoff * n) / (Math.PI * n);
            }
            
            double window = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * i / (numTaps - 1));
            
            this.filterTaps[i] = tapVal * window;
        }
        
        filterInitialized = true;
    }

    @Override
    public int process(double[] buffer, int length, DspPipeline pipeline) {
        if (!filterInitialized) {
            return 0;
        }

        boolean usb = this.isUSB.resolve(pipeline);
        int dec = this.decimation.resolve(pipeline);
        if (dec < 1) dec = 1;

        int outIndex = 0;
        int decimatorCounter = 0;

        // Iterate through complex pairs (I, Q)
        for (int i = 0; i + 1 < length; i += 2) {
            // 1. Read IQ
            double I = buffer[i];
            double Q = buffer[i + 1];

            // 2. Frequency Mix (Shift Down)
            // Using cos/sin. 
            // Multiplies by (cos - j*sin) -> Shifts DOWN.
            double cosPhase = Math.cos(this.phase);
            double sinPhase = Math.sin(this.phase);

            double mixed_i = I * cosPhase + Q * sinPhase;
            double mixed_q = Q * cosPhase - I * sinPhase;

            this.phase += this.phaseIncrement;
            if (this.phase >= 2.0 * Math.PI) {
                this.phase -= 2.0 * Math.PI;
            }

            historyI[historyIndex] = mixed_i;
            historyQ[historyIndex] = mixed_q;

            if (decimatorCounter == 0) {
                double sumI = 0;
                double sumQ = 0;
                
                for (int j = 0; j < filterTaps.length; j++) {
                    int idx = (historyIndex - j + filterTaps.length) % filterTaps.length;
                    sumI += historyI[idx] * filterTaps[j];
                    sumQ += historyQ[idx] * filterTaps[j];
                }

                double audioVal = usb ? sumI : sumQ;

                buffer[outIndex++] = audioVal;
            }

            historyIndex = (historyIndex + 1) % filterTaps.length;
            decimatorCounter++;
            if (decimatorCounter >= dec) {
                decimatorCounter = 0;
            }
        }
        
        return outIndex;
    }
    
    @Override
    public void reset() {
        this.phase = 0.0;
        if (historyI != null) {
            java.util.Arrays.fill(historyI, 0);
            java.util.Arrays.fill(historyQ, 0);
        }
        historyIndex = 0;
    }
    
    @Override
    public Pair<NumberType, NumberType> getInputOutputType() {
        return new Pair<>(NumberType.COMPLEX, NumberType.REAL);
    }
    
    @Override
    public PipelineStepType getType() {
        return PipelineStepType.DEMODULATOR;
    }
}