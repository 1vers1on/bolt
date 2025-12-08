package net.ellie.bolt.dsp;

public class IIRFilterDesigns {
	public static double[][] butterworthLowpass1(double sampleRateHz, double cutoffHz) {
		validateFsFc(sampleRateHz, cutoffHz);

		final double T = 1.0 / sampleRateHz;
		final double omegaA = 2.0 * Math.PI * cutoffHz;
		final double omegaW = (2.0 / T) * Math.tan(omegaA * T / 2.0);

		final double K = omegaW * T / 2.0;
		final double norm = 1.0 / (1.0 + K);
		final double b0 = K * norm;
		final double b1 = K * norm;
		final double a1 = (1.0 - K) * norm;

		return new double[][] { new double[] { b0, b1 }, new double[] { 1.0, a1 } };
	}

	public static double[][] butterworthHighpass1(double sampleRateHz, double cutoffHz) {
		validateFsFc(sampleRateHz, cutoffHz);

		final double T = 1.0 / sampleRateHz;
		final double omegaA = 2.0 * Math.PI * cutoffHz;
		final double omegaW = (2.0 / T) * Math.tan(omegaA * T / 2.0);

		final double K = omegaW * T / 2.0;
		final double norm = 1.0 / (1.0 + K);
		final double b0 = 1.0 * norm;
		final double b1 = -1.0 * norm;
		final double a1 = (1.0 - K) * norm;

		return new double[][] { new double[] { b0, b1 }, new double[] { 1.0, a1 } };
	}

	public static double[][] butterworthLowpass2(double sampleRateHz, double cutoffHz) {
		validateFsFc(sampleRateHz, cutoffHz);

		final double T = 1.0 / sampleRateHz;
		final double omegaA = 2.0 * Math.PI * cutoffHz;
		final double omegaW = (2.0 / T) * Math.tan(omegaA * T / 2.0);

		final double K = omegaW * T / 2.0;
		final double K2 = K * K;
		final double norm = 1.0 / (1.0 + Math.sqrt(2.0) * K + K2);

		final double b0 = K2 * norm;
		final double b1 = 2.0 * K2 * norm;
		final double b2 = K2 * norm;
		final double a1 = 2.0 * (K2 - 1.0) * norm;
		final double a2 = (1.0 - Math.sqrt(2.0) * K + K2) * norm;

		return new double[][] { new double[] { b0, b1, b2 }, new double[] { 1.0, a1, a2 } };
	}

	public static double[][] butterworthHighpass2(double sampleRateHz, double cutoffHz) {
		validateFsFc(sampleRateHz, cutoffHz);

		final double T = 1.0 / sampleRateHz;
		final double omegaA = 2.0 * Math.PI * cutoffHz;
		final double omegaW = (2.0 / T) * Math.tan(omegaA * T / 2.0);

		final double K = omegaW * T / 2.0;
		final double K2 = K * K;
		final double norm = 1.0 / (1.0 + Math.sqrt(2.0) * K + K2);

		final double b0 = 1.0 * norm;
		final double b1 = -2.0 * norm;
		final double b2 = 1.0 * norm;
		final double a1 = 2.0 * (K2 - 1.0) * norm;
		final double a2 = (1.0 - Math.sqrt(2.0) * K + K2) * norm;

		return new double[][] { new double[] { b0, b1, b2 }, new double[] { 1.0, a1, a2 } };
	}

	private static void validateFsFc(double sampleRateHz, double cutoffHz) throws IllegalArgumentException {
		if (sampleRateHz <= 0.0) {
			throw new IllegalArgumentException("sampleRateHz must be > 0");
		}
		if (cutoffHz <= 0.0 || cutoffHz >= sampleRateHz / 2.0) {
			throw new IllegalArgumentException("cutoffHz must be in (0, sampleRateHz/2)");
		}
	}
}
