package net.ellie.bolt.dsp;

import org.apache.commons.math3.special.BesselJ;

public class FIRFilterDesigns {
    private static double[] kaiserWindow(int M, double beta) {
        double[] w = new double[M];
        double denom = BesselJ.value(0, beta);
        for (int n = 0; n < M; n++) {
            double x = 2.0 * n / (M - 1) - 1.0;
            w[n] = BesselJ.value(0, beta * Math.sqrt(1.0 - x * x)) / denom;
        }
        return w;
    }

    public static double[] designHammingLowPass(int M, double fs, double fc) {
        if (M <= 0) throw new IllegalArgumentException("M must be > 0");
        if (fs <= 0) throw new IllegalArgumentException("fs must be > 0");
        if (fc <= 0 || fc >= fs / 2.0) throw new IllegalArgumentException("fc must be in (0, fs/2)");
        double[] h = new double[M];
        int mid = M / 2;
        double normCut = fc / fs;
        for (int n = 0; n < M; n++) {
            int k = n - mid;
            double sinc = (k == 0) ? 2.0 * normCut
                    : Math.sin(2.0 * Math.PI * normCut * k) / (Math.PI * k);
            double w = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * n / (M - 1));
            h[n] = sinc * w;
        }
        double sum = 0.0;
        for (double v : h) sum += v;
        for (int n = 0; n < M; n++) h[n] /= sum;
        return h;
    }

    public static double[] designBlackmanLowPass(int M, double fs, double fc) {
        if (M <= 0) throw new IllegalArgumentException("M must be > 0");
        if (fs <= 0) throw new IllegalArgumentException("fs must be > 0");
        if (fc <= 0 || fc >= fs / 2.0) throw new IllegalArgumentException("fc must be in (0, fs/2)");
        double[] h = new double[M];
        int mid = M / 2;
        double normCut = fc / fs;
        for (int n = 0; n < M; n++) {
            int k = n - mid;
            double sinc = (k == 0) ? 2.0 * normCut
                    : Math.sin(2.0 * Math.PI * normCut * k) / (Math.PI * k);
            double w = 0.42 - 0.5 * Math.cos(2.0 * Math.PI * n / (M - 1))
                    + 0.08 * Math.cos(4.0 * Math.PI * n / (M - 1));
            h[n] = sinc * w;
        }
        double sum = 0.0;
        for (double v : h) sum += v;
        for (int n = 0; n < M; n++) h[n] /= sum;
        return h;
    }

    public static double[] designKaiserLowPass(int M, double fs, double fc, double beta) {
        if (M <= 0) throw new IllegalArgumentException("M must be > 0");
        if (fs <= 0) throw new IllegalArgumentException("fs must be > 0");
        if (fc <= 0 || fc >= fs / 2.0) throw new IllegalArgumentException("fc must be in (0, fs/2)");
        double[] h = new double[M];
        double[] w = kaiserWindow(M, beta);
        int mid = M / 2;
        double normCut = fc / fs;
        for (int n = 0; n < M; n++) {
            int k = n - mid;
            double sinc = (k == 0) ? 2.0 * normCut
                    : Math.sin(2.0 * Math.PI * normCut * k) / (Math.PI * k);
            h[n] = sinc * w[n];
        }
        double sum = 0.0;
        for (double v : h) sum += v;
        for (int n = 0; n < M; n++) h[n] /= sum;
        return h;
    }

    public static double[] designHammingHighPass(int M, double fs, double fc) {
        if (M <= 0) throw new IllegalArgumentException("M must be > 0");
        if (fs <= 0) throw new IllegalArgumentException("fs must be > 0");
        if (fc <= 0 || fc >= fs / 2.0) throw new IllegalArgumentException("fc must be in (0, fs/2)");
        double[] h = new double[M];
        int mid = M / 2;
        double normCut = fc / fs;
        for (int n = 0; n < M; n++) {
            int k = n - mid;
            double sinc = (k == 0) ? 1.0 - 2.0 * normCut
                    : -Math.sin(2.0 * Math.PI * normCut * k) / (Math.PI * k);
            double w = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * n / (M - 1));
            h[n] = sinc * w;
        }
        return h;
    }

    public static double[] designBlackmanHighPass(int M, double fs, double fc) {
        if (M <= 0) throw new IllegalArgumentException("M must be > 0");
        if (fs <= 0) throw new IllegalArgumentException("fs must be > 0");
        if (fc <= 0 || fc >= fs / 2.0) throw new IllegalArgumentException("fc must be in (0, fs/2)");
        double[] h = new double[M];
        int mid = M / 2;
        double normCut = fc / fs;
        for (int n = 0; n < M; n++) {
            int k = n - mid;
            double sinc = (k == 0) ? 1.0 - 2.0 * normCut
                    : -Math.sin(2.0 * Math.PI * normCut * k) / (Math.PI * k);
            double w = 0.42 - 0.5 * Math.cos(2.0 * Math.PI * n / (M - 1))
                    + 0.08 * Math.cos(4.0 * Math.PI * n / (M - 1));
            h[n] = sinc * w;
        }
        return h;
    }

    public static double[] designHammingBandPass(int M, double fs, double fcLow, double fcHigh) {
        if (M <= 0) throw new IllegalArgumentException("M must be > 0");
        if (fs <= 0) throw new IllegalArgumentException("fs must be > 0");
        if (fcLow <= 0 || fcLow >= fs / 2.0) throw new IllegalArgumentException("fcLow must be in (0, fs/2)");
        if (fcHigh <= fcLow || fcHigh >= fs / 2.0) throw new IllegalArgumentException("fcHigh must be in (fcLow, fs/2)");
        double[] h = new double[M];
        int mid = M / 2;
        double normLow = fcLow / fs;
        double normHigh = fcHigh / fs;
        for (int n = 0; n < M; n++) {
            int k = n - mid;
            double sincHigh = (k == 0) ? 2.0 * normHigh
                    : Math.sin(2.0 * Math.PI * normHigh * k) / (Math.PI * k);
            double sincLow = (k == 0) ? 2.0 * normLow
                    : Math.sin(2.0 * Math.PI * normLow * k) / (Math.PI * k);
            double w = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * n / (M - 1));
            h[n] = (sincHigh - sincLow) * w;
        }
        return h;
    }

    public static double[] designBlackmanBandPass(int M, double fs, double fcLow, double fcHigh) {
        if (M <= 0) throw new IllegalArgumentException("M must be > 0");
        if (fs <= 0) throw new IllegalArgumentException("fs must be > 0");
        if (fcLow <= 0 || fcLow >= fs / 2.0) throw new IllegalArgumentException("fcLow must be in (0, fs/2)");
        if (fcHigh <= fcLow || fcHigh >= fs / 2.0) throw new IllegalArgumentException("fcHigh must be in (fcLow, fs/2)");
        double[] h = new double[M];
        int mid = M / 2;
        double normLow = fcLow / fs;
        double normHigh = fcHigh / fs;
        for (int n = 0; n < M; n++) {
            int k = n - mid;
            double sincHigh = (k == 0) ? 2.0 * normHigh
                    : Math.sin(2.0 * Math.PI * normHigh * k) / (Math.PI * k);
            double sincLow = (k == 0) ? 2.0 * normLow
                    : Math.sin(2.0 * Math.PI * normLow * k) / (Math.PI * k);
            double w = 0.42 - 0.5 * Math.cos(2.0 * Math.PI * n / (M - 1))
                    + 0.08 * Math.cos(4.0 * Math.PI * n / (M - 1));
            h[n] = (sincHigh - sincLow) * w;
        }
        return h;
    }

    public static double[] designHammingBandStop(int M, double fs, double fcLow, double fcHigh) {
        if (M <= 0) throw new IllegalArgumentException("M must be > 0");
        if (fs <= 0) throw new IllegalArgumentException("fs must be > 0");
        if (fcLow <= 0 || fcLow >= fs / 2.0) throw new IllegalArgumentException("fcLow must be in (0, fs/2)");
        if (fcHigh <= fcLow || fcHigh >= fs / 2.0) throw new IllegalArgumentException("fcHigh must be in (fcLow, fs/2)");
        double[] h = new double[M];
        int mid = M / 2;
        double normLow = fcLow / fs;
        double normHigh = fcHigh / fs;
        for (int n = 0; n < M; n++) {
            int k = n - mid;
            double sincHigh = (k == 0) ? 2.0 * normHigh
                    : Math.sin(2.0 * Math.PI * normHigh * k) / (Math.PI * k);
            double sincLow = (k == 0) ? 2.0 * normLow
                    : Math.sin(2.0 * Math.PI * normLow * k) / (Math.PI * k);
            double delta = (k == 0) ? 1.0 : 0.0;
            double w = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * n / (M - 1));
            h[n] = (delta - (sincHigh - sincLow)) * w;
        }
        return h;
    }

    public static double[] designBlackmanBandStop(int M, double fs, double fcLow, double fcHigh) {
        if (M <= 0) throw new IllegalArgumentException("M must be > 0");
        if (fs <= 0) throw new IllegalArgumentException("fs must be > 0");
        if (fcLow <= 0 || fcLow >= fs / 2.0) throw new IllegalArgumentException("fcLow must be in (0, fs/2)");
        if (fcHigh <= fcLow || fcHigh >= fs / 2.0) throw new IllegalArgumentException("fcHigh must be in (fcLow, fs/2)");
        double[] h = new double[M];
        int mid = M / 2;
        double normLow = fcLow / fs;
        double normHigh = fcHigh / fs;
        for (int n = 0; n < M; n++) {
            int k = n - mid;
            double sincHigh = (k == 0) ? 2.0 * normHigh
                    : Math.sin(2.0 * Math.PI * normHigh * k) / (Math.PI * k);
            double sincLow = (k == 0) ? 2.0 * normLow
                    : Math.sin(2.0 * Math.PI * normLow * k) / (Math.PI * k);
            double delta = (k == 0) ? 1.0 : 0.0;
            double w = 0.42 - 0.5 * Math.cos(2.0 * Math.PI * n / (M - 1))
                    + 0.08 * Math.cos(4.0 * Math.PI * n / (M - 1));
            h[n] = (delta - (sincHigh - sincLow)) * w;
        }
        return h;
    }

    public static double[] designDifferentiator(int M, double fs) {
        if (M <= 0) throw new IllegalArgumentException("M must be > 0");
        if (fs <= 0) throw new IllegalArgumentException("fs must be > 0");
        double[] h = new double[M];
        int mid = M / 2;
        for (int n = 0; n < M; n++) {
            int k = n - mid;
            if (k == 0) {
                h[n] = 0.0;
            } else {
                h[n] = Math.cos(Math.PI * k) / k;
            }
            double w = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * n / (M - 1));
            h[n] *= w;
        }
        return h;
    }

    public static double[] designHilbert(int M) {
        if (M <= 0) throw new IllegalArgumentException("M must be > 0");
        double[] h = new double[M];
        int mid = M / 2;
        for (int n = 0; n < M; n++) {
            int k = n - mid;
            if (k == 0) {
                h[n] = 0.0;
            } else if (k % 2 != 0) {
                h[n] = 2.0 / (Math.PI * k);
            } else {
                h[n] = 0.0;
            }
            double w = 0.54 - 0.46 * Math.cos(2.0 * Math.PI * n / (M - 1));
            h[n] *= w;
        }
        return h;
    }

    public static double[] designMovingAverage(int M) {
        if (M <= 0) throw new IllegalArgumentException("M must be > 0");
        double[] h = new double[M];
        double val = 1.0 / M;
        for (int n = 0; n < M; n++) {
            h[n] = val;
        }
        return h;
    }
}
