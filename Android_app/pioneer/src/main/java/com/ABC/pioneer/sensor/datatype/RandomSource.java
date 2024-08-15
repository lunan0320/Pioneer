

package com.ABC.pioneer.sensor.datatype;


import java.security.SecureRandom;
import java.util.Random;

/// 随机数据来源
public class RandomSource {
    public final Method method;
    private Random random = null;
    private short externalEntropy = 0;
    public enum Method {
        // 每次调用可重用单例随机源
        Random, SecureRandomSingleton, SecureRandom, SecureRandomNIST
    }

    public RandomSource(final Method method) {
        this.method = method;
        externalEntropy = (short) (Math.random() * Short.MAX_VALUE);
    }

    /// 从外部来源贡献熵，例如 不可预测的时间间隔
    public synchronized void addEntropy(final long value) {
        final short contribution = (short) (value % Short.MAX_VALUE);
        externalEntropy += contribution;
    }

    // MARK: 随机数据

    public void nextBytes(byte[] bytes) {
        init();
        random.nextBytes(bytes);
    }

    public int nextInt() {
        init();
        return random.nextInt();
    }

    public long nextLong() {
        init();
        return random.nextLong();
    }

    public double nextDouble() {
        init();
        return random.nextDouble();
    }



    /// 根据方法初始化随机
    protected synchronized void init() {
        switch (method) {
            case Random: {
                random = getRandom();
                break;
            }
            case SecureRandomSingleton: {
                random = getSecureRandomSingleton();
                break;
            }
            case SecureRandom: {
                random = getSecureRandom();
                break;
            }
            case SecureRandomNIST: {
                random = getSecureRandomNIST();
                break;
            }
        }
        // 使用外部熵调整PRNG序列位置0-128位
        final int skipPositions = (Math.abs(externalEntropy) % 128);
        for (int i=skipPositions; i-->0;) {
            random.nextBoolean();
        }
        externalEntropy = 0;
    }

    /// 具有可靠熵源的无阻塞随机数生成器。
    private static long getRandomLongLastCalledAt = System.nanoTime();
    private synchronized final static Random getRandom() {
        //在调用之间使用不可预测的时间来增加熵
        final long timestamp = System.nanoTime();
        final long entropy = (timestamp - getRandomLongLastCalledAt);
        final int skipRandomSequence = (int) Math.abs(entropy % 128);
        for (int i=skipRandomSequence; i-->0;) {
            Math.random();
        }
        // 使用Math.random（）中的种子创建一个Random新实例，以增加搜索空间
        // 从新的Random实例获得的值到Math.random（）的种子。
        final Random random = new Random(Math.round(Math.random() * Long.MAX_VALUE));
        // 在新的Random实例上跳过256-1280位，以将搜索空间从新的Random实例值增加到其种子。
        // 使用Math.random（）选择跳过距离以增加搜索空间。
        final int skipInitialBits = (int) Math.abs(256 + Math.round(Math.random() * 1024));
        for (int i=skipInitialBits; i-->0;) {
            random.nextBoolean();
        }
        // 更新时间戳以在下一个呼叫中使用
        getRandomLongLastCalledAt = timestamp;
        // 获取下一个long
        return random;
    }

    /// 安全的随机数生成器，由于缺少熵，在闲置设备上运行约7.5小时后会阻塞。
    private static SecureRandom secureRandomSingleton = null;
    private synchronized final static Random getSecureRandomSingleton() {
        if (secureRandomSingleton == null) {
            secureRandomSingleton = new SecureRandom();
        }
        return secureRandomSingleton;
    }


    /// 安全的随机数生成器，由于缺少熵，在空闲设备上约4.5小时后会阻塞。
    private final static Random getSecureRandom() {
        return new SecureRandom();
    }

    private final static Random getSecureRandomNIST() {
        try {
            final SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            final SecureRandom secureRandomForSeed = new SecureRandom();
           final byte[] seed = secureRandomForSeed.generateSeed(55);
            secureRandom.setSeed(seed); // seed with random number
            secureRandom.nextBytes(new byte[256 + secureRandom.nextInt(1024)]);
            return secureRandom;
        } catch (Throwable e) {
            return getSecureRandom();
        }
    }

}
