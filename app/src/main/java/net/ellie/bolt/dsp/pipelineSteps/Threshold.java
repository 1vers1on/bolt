package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;
import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;

public class Threshold extends AbstractPipelineStep {
	private double threshold;

	public Threshold(double threshold) {
		this.threshold = threshold;
	}

	@Override
	public int process(double[] buffer, int length) {
		for (int i = 0; i < length; i++) {
			buffer[i] = buffer[i] >= threshold ? 1.0 : 0.0;
		}
		return length;
	}

	@Override
	public void reset() {
	}

	@Override
	public Pair<NumberType, NumberType> getInputOutputType() {
		return new Pair<>(NumberType.REAL, NumberType.REAL);
	}

	@Override
	public PipelineStepType getType() {
		return PipelineStepType.FILTER;
	}

	public void setThreshold(double value) {
		this.threshold = value;
	}
}
