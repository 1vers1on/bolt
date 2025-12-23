package net.ellie.bolt.dsp.pipelineSteps;


import org.apache.commons.math3.util.Pair;
import net.ellie.bolt.dsp.AbstractPipelineStep;
import net.ellie.bolt.dsp.DspPipeline;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;
import net.ellie.bolt.dsp.attributes.PipelineAttribute;

public class IIRFilter extends AbstractPipelineStep {
	private PipelineAttribute<double[]> bAttr;
	private PipelineAttribute<double[]> aAttr;
	private double[] dRe;
	private double[] dIm;

	public IIRFilter(PipelineAttribute<double[]> bAttr, PipelineAttribute<double[]> aAttr, DspPipeline pipeline) {
		this.bAttr = bAttr;
		this.aAttr = aAttr;
		updateDelayArrays(pipeline);

		this.bAttr.addListener(newValue -> updateDelayArrays(pipeline));
		this.aAttr.addListener(newValue -> updateDelayArrays(pipeline));
	}

	private void updateDelayArrays(DspPipeline pipeline) {
		double[] b = bAttr.resolve(pipeline);
		double[] a = aAttr.resolve(pipeline);
		int M = Math.max(b.length, a.length) - 1;
		if (M > 0) {
			dRe = new double[M];
			dIm = new double[M];
		} else {
			dRe = null;
			dIm = null;
		}
	}

	@Override
	public int process(double[] buffer, int length, DspPipeline pipeline) {
		if (buffer == null) return 0;
		if ((length & 1) != 0) {
			throw new IllegalArgumentException("Complex buffer length must be even (interleaved re, im)");
		}

		double[] b = bAttr.resolve(pipeline);
		double[] a = aAttr.resolve(pipeline);
		final int complexSamples = length / 2;
		final int Nb = b.length;
		final int Na = a.length;
		final int M = Math.max(Nb, Na) - 1;

		if (M == 0) {
			final double b0 = b[0];
			for (int n = 0; n < complexSamples; n++) {
				int idx = n * 2;
				buffer[idx] *= b0;
				buffer[idx + 1] *= b0;
			}
			return length;
		}

		if (dRe == null || dRe.length != M) {
			dRe = new double[M];
			dIm = new double[M];
		}

		double[] bp = new double[M + 1];
		double[] ap = new double[M + 1];

		for (int i = 0; i < bp.length; i++) {
			bp[i] = (i < Nb) ? b[i] : 0.0;
			ap[i] = (i < Na) ? a[i] : 0.0;
		}
		ap[0] = 1.0;

		double[] newRe = new double[M];
		double[] newIm = new double[M];

		for (int n = 0; n < complexSamples; n++) {
			int idx = n * 2;
			double xRe = buffer[idx];
			double xIm = buffer[idx + 1];

			double yRe = bp[0] * xRe + dRe[0];
			double yIm = bp[0] * xIm + dIm[0];

			int last = M - 1;
			for (int i = 0; i < last; i++) {
				newRe[i] = dRe[i + 1] + bp[i + 1] * xRe - ap[i + 1] * yRe;
				newIm[i] = dIm[i + 1] + bp[i + 1] * xIm - ap[i + 1] * yIm;
			}
			newRe[last] = bp[M] * xRe - ap[M] * yRe;
			newIm[last] = bp[M] * xIm - ap[M] * yIm;

			buffer[idx] = yRe;
			buffer[idx + 1] = yIm;

			System.arraycopy(newRe, 0, dRe, 0, M);
			System.arraycopy(newIm, 0, dIm, 0, M);
		}
		return length;
	}

	@Override
	public void reset() {
		if (dRe != null) {
			for (int i = 0; i < dRe.length; i++) dRe[i] = 0.0;
		}
		if (dIm != null) {
			for (int i = 0; i < dIm.length; i++) dIm[i] = 0.0;
		}
	}

	@Override
	public Pair<NumberType, NumberType> getInputOutputType() {
		return new Pair<>(NumberType.COMPLEX, NumberType.COMPLEX);
	}

	@Override
	public PipelineStepType getType() {
		return PipelineStepType.FILTER;
	}

	public PipelineAttribute<double[]> getBAttribute() {
		return bAttr;
	}

	public PipelineAttribute<double[]> getAAttribute() {
		return aAttr;
	}
}
