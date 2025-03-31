
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

    /**
     * 将另一个样本分布合并到当前分布中
     * 用于累积统计量计算，支持在线算法更新统计量
     * 
     * @param distribution 要合并的样本分布
     */
    public void add(final Sample distribution) {
        // 如果当前样本为空，直接复制另一个样本的所有统计量
        if (n == 0) {
            copyStatisticsFrom(distribution);
            return;
        }
    
        // 计算合并后的样本统计量
        Sample combined = combineStatistics(distribution);
        
        // 更新当前样本的统计量
        updateCurrentStatistics(combined);
    }
    
    /**
     * 从另一个样本复制所有统计量到当前样本
     */
    private void copyStatisticsFrom(Sample source) {
        n = source.n;
        m1 = source.m1;
        m2 = source.m2;
        m3 = source.m3;
        m4 = source.m4;
        min = source.min;
        max = source.max;
    }
    
    /**
     * 计算合并后的统计量
     */
    private Sample combineStatistics(Sample other) {
        Sample combined = new Sample();
        combined.n = n + other.n;
        
        // 计算均值差异及其幂次
        final double delta = other.m1 - m1;
        final double delta2 = delta * delta;
        final double delta3 = delta * delta2;
        final double delta4 = delta2 * delta2;
        
        // 计算合并后的各阶中心矩
        combined.m1 = calculateCombinedMean(other, combined.n);
        combined.m2 = calculateCombinedSecondMoment(other, delta2, combined.n);
        combined.m3 = calculateCombinedThirdMoment(other, delta, delta3, combined.n);
        combined.m4 = calculateCombinedFourthMoment(other, delta, delta2, delta4, combined.n);
        
        // 更新最小最大值
        combined.min = Math.min(min, other.min);
        combined.max = Math.max(max, other.max);
        
        return combined;
    }
    
    /**
     * 计算合并后的均值
     */
    private double calculateCombinedMean(Sample other, double combinedN) {
        return (n * m1 + other.n * other.m1) / combinedN;
    }
    
    /**
     * 计算合并后的二阶中心矩
     */
    private double calculateCombinedSecondMoment(Sample other, double delta2, double combinedN) {
        return m2 + other.m2 + delta2 * n * other.n / combinedN;
    }
    
    /**
     * 计算合并后的三阶中心矩
     */
    private double calculateCombinedThirdMoment(Sample other, double delta, double delta3, double combinedN) {
        double term1 = m3 + other.m3;
        double term2 = delta3 * n * other.n * (n - other.n) / (combinedN * combinedN);
        double term3 = 3.0 * delta * (n * other.m2 - other.n * m2) / combinedN;
        return term1 + term2 + term3;
    }
    
    /**
     * 计算合并后的四阶中心矩
     */
    private double calculateCombinedFourthMoment(Sample other, double delta, double delta2, double delta4, double combinedN) {
        double term1 = m4 + other.m4;
        double term2 = delta4 * n * other.n * (n * n - n * other.n + other.n * other.n) 
                      / (combinedN * combinedN * combinedN);
        double term3 = 6.0 * delta2 * (n * n * other.m2 + other.n * other.n * m2) 
                      / (combinedN * combinedN);
        double term4 = 4.0 * delta * (n * other.m3 - other.n * m3) / combinedN;
        return term1 + term2 + term3 + term4;
    }
    
    /**
     * 用合并后的统计量更新当前样本
     */
    private void updateCurrentStatistics(Sample combined) {
        n = combined.n;
        m1 = combined.m1;
        m2 = combined.m2;
        m3 = combined.m3;
        m4 = combined.m4;
        min = combined.min;
        max = combined.max;
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

    /**
     * 计算并返回样本方差
     * 方差是衡量数据离散程度的指标
     * 
     * @return 样本方差值，如果样本数量不足则返回null
     */

    public Double variance() {
        if (n > 1) {
            return m2 / (n - 1d);
        } else {
            return null;
        }
    }

    
    /**
     * 计算并返回样本标准差
     * 标准差是方差的平方根，表示数据分布的离散程度
     * 
     * @return 样本标准差，如果样本数量不足则返回null
     */
    public Double standardDeviation() {
        if (n > 1) {
            return StrictMath.sqrt(m2 / (n - 1d));
        } else {
            return null;
        }
    }

    /**
     * 获取样本中的最小值
     * 
     * @return 样本最小值，如果样本为空则返回null
     */
    public Double min() {
        if (n > 0) {
            return min;
        } else {
            return null;
        }
    }

    /**
     * 获取样本中的最大值
     * 
     * @return 样本最大值，如果样本为空则返回null
     */
    public Double max() {
        if (n > 0) {
            return max;
        } else {
            return null;
        }
    }

    /**
     * 估计当前样本分布与另一个样本分布之间的距离
     * 值为1表示分布相同，0表示完全不同
     * 
     * @param otherSample 要比较的另一个样本
     * @return 分布距离值，如果无法计算则返回null
     */
    public Double distance(final Sample sample) {
        return bhattacharyyaDistance(this, sample);
    }

    /**
     * 计算两个分布之间的Bhattacharyya距离
     * 用于估计两个分布相同的可能性
     * 值为1表示两个分布完全相同，0表示完全不同
     * 
     * @param firstSample 第一个样本
     * @param secondSample 第二个样本
     * @return Bhattacharyya距离值，如果无法计算则返回null
     */
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

    /**
     * 返回样本的字符串表示形式
     * 
     * @return 包含样本统计信息的字符串
     */
    @Override
    public String toString() {
        return String.format(
            "[count=%d, mean=%.2f, sd=%.2f, min=%.2f, max=%.2f]",
            count(), 
            mean(), 
            standardDeviation(), 
            min(), 
            max()
        );
    }
}
