package net.ellie.bolt.dsp.pipelineSteps;

import org.apache.commons.math3.util.Pair;

import net.ellie.bolt.dsp.IPipelineStep;
import net.ellie.bolt.dsp.NumberType;
import net.ellie.bolt.dsp.PipelineStepType;

public class IIRFilter implements IPipelineStep {
	private double[] b;
	private double[] a;

	private double[] dRe;
	private double[] dIm;

	public IIRFilter(double[] b, double[] a) {
		setCoefficients(b, a);
	}

	public IIRFilter() {
		this(new double[] { 1.0 }, new double[] { 1.0 });
	}

	@Override
	public int process(double[] buffer, int length) {
		if (buffer == null) return 0;
		if ((length & 1) != 0) {
			throw new IllegalArgumentException("Complex buffer length must be even (interleaved re, im)");
		}

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

	public void setCoefficients(double[] b, double[] a) {
		if (b == null || b.length == 0) {
			throw new IllegalArgumentException("IIR numerator (b) must be non-empty");
		}
		if (a == null || a.length == 0) {
			throw new IllegalArgumentException("IIR denominator (a) must be non-empty and include a[0]=1");
		}
		if (Math.abs(a[0] - 1.0) > 1e-12) {
			throw new IllegalArgumentException("IIR requires a[0] == 1. Normalize coefficients before setting.");
		}

		this.b = b.clone();
		this.a = a.clone();

		int M = Math.max(this.b.length, this.a.length) - 1;
		if (M > 0) {
			this.dRe = new double[M];
			this.dIm = new double[M];
		} else {
			this.dRe = null;
			this.dIm = null;
		}
	}
}
