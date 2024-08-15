
package com.ABC.pioneer.sensor.analysis;

public class Sample {
    protected long n = 0;
    protected double m1 = 0, m2 = 0, m3 = 0, m4 = 0;
    private double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;

    public Sample() {
    }

    public Sample(final double x, final long f) {
        n = f;
        m1 = x;
        min = x;
        max = x;
    }

    public synchronized void add(final double x) {
        final long n1 = n;
        n++;
        final double delta = x - m1;
        final double delta_n = delta / n;
        final double delta_n2 = delta_n * delta_n;
        final double term1 = delta * delta_n * n1;
        m1 += delta_n;
        m4 += term1 * delta_n2 * (n * n - 3 * n + 3) + 6 * delta_n2 * m2 - 4 * delta_n * m3;
        m3 += term1 * delta_n * (n - 2) - 3 * delta_n * m2;
        m2 += term1;
        if (x < min) {
            min = x;
        }
        if (x > max) {
            max = x;
        }
    }

    public void add(final double x, final long f) {
        add(new Sample(x, f));
    }

    public void add(final Sample distribution) {
        if (n == 0) {
            n = distribution.n;
            m1 = distribution.m1;
            m2 = distribution.m2;
            m3 = distribution.m3;
            m4 = distribution.m4;
            min = distribution.min;
            max = distribution.max;
        } else {
            final Sample combined = new Sample();
            combined.n = n + distribution.n;

            final double delta = distribution.m1 - m1;
            final double delta2 = delta * delta;
            final double delta3 = delta * delta2;
            final double delta4 = delta2 * delta2;

            combined.m1 = (n * m1 + distribution.n * distribution.m1) / combined.n;
            combined.m2 = m2 + distribution.m2 + delta2 * n * distribution.n / combined.n;
            combined.m3 = m3 + distribution.m3
                    + delta3 * n * distribution.n * (n - distribution.n) / (combined.n * combined.n);
            combined.m3 += 3.0 * delta * (n * distribution.m2 - distribution.n * m2) / combined.n;
            combined.m4 = m4 + distribution.m4 + delta4 * n * distribution.n
                    * (n * n - n * distribution.n + distribution.n * distribution.n) / (combined.n * combined.n * combined.n);
            combined.m4 += 6.0 * delta2 * (n * n * distribution.m2 + distribution.n * distribution.n * m2)
                    / (combined.n * combined.n) + 4.0 * delta * (n * distribution.m3 - distribution.n * m3) / combined.n;
            combined.min = (min < distribution.min ? min : distribution.min);
            combined.max = (max > distribution.max ? max : distribution.max);

            n = combined.n;
            m1 = combined.m1;
            m2 = combined.m2;
            m3 = combined.m3;
            m4 = combined.m4;
            min = combined.min;
            max = combined.max;
        }
    }

    public long count() {
        return n;
    }

    //平均数
    public Double mean() {
        if (n > 0) {
            return m1;
        } else {
            return null;
        }
    }

    //方差
    public Double variance() {
        if (n > 1) {
            return m2 / (n - 1d);
        } else {
            return null;
        }
    }

    //默认偏差
    public Double standardDeviation() {
        if (n > 1) {
            return StrictMath.sqrt(m2 / (n - 1d));
        } else {
            return null;
        }
    }

    public Double min() {
        if (n > 0) {
            return min;
        } else {
            return null;
        }
    }

    public Double max() {
        if (n > 0) {
            return max;
        } else {
            return null;
        }
    }

    /// Estimate distance between this sample's distribution and another sample's distribution, 1 means identical and 0 means completely different.
    public Double distance(final Sample sample) {
        return bhattacharyyaDistance(this, sample);
    }

    /// Bhattacharyya distance between two distributions estimate  the likelihood that the two distributions are the same.
    /// bhattacharyyaDistance = 1 means the two distributions are identical; value = 0 means they are different.
    private final static Double bhattacharyyaDistance(final Sample d1, final Sample d2) {
        final Double v1 = d1.variance();
        final Double v2 = d2.variance();
        final Double m1 = d1.mean();
        final Double m2 = d2.mean();
        if (v1 == null || v2 == null || m1 == null || m2 == null) {
            return null;
        }

        if (v1 == 0 && v2 == 0) {
            if (m1 == m2) {
                return 1.0;
            } else {
                return 0.0;
            }
        }

        final Double sd1 = Math.sqrt(v1);
        final Double sd2 = Math.sqrt(v2);
        if (sd1 == null || sd2 == null) {
            return null;
        }

        final double Dbc = Math.sqrt((2.0 * sd1 * sd2) / (v1 + v2))
                * Math.exp(-1.0 / 4.0 * (Math.pow((m1 - m2), 2) / (v1 + v2)));
        return Dbc;
    }

    @Override
    public String toString() {
        return "[count=" + count() + ",mean=" + mean() + ",sd=" + standardDeviation() + ",min=" + min() + ",max=" + max() + "]";
    }
}
