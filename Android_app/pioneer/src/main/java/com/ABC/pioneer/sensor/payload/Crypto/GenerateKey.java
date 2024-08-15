
package com.ABC.pioneer.sensor.payload.Crypto;

import com.ABC.pioneer.sensor.datatype.TimeInterval;

import java.text.SimpleDateFormat;
import java.util.Date;

/// 生成相应的keys，链式生成
public class GenerateKey {
    /// Secret key的长度设定为2048
    private final static int secretKeyLength = 2048;
    /// 派生keys所能维持到的最大天数为2000天
    private final static int days = 2000;
    /// 生成matching keys的时间间隔为6min
    private final static int periods = 240;
    /// 获取1970年以来的时间间隔
    private final static TimeInterval epoch = GenerateKey.getEpoch();

    ///转换时间显示为相应的格式
    protected static Date date(String fromString) {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        try {
            return format.parse(fromString);
        } catch (Throwable e) {
            return null;
        }
    }

    /// 计算用到的是具体的那天或哪个间隔的key
    protected static TimeInterval getEpoch() {
        final Date date = date("2020-09-01T00:00:00+0000");
        return new TimeInterval(date.getTime() / 1000);
    }


    /// 选择正确的天数来获取相应的matching key
    public static int day(Date onDate) {
        return (int) ((new TimeInterval(onDate).value - epoch.value) / 86400);
    }

    /// 获取每天相应正确的时间间隔来获取相应的contact key
    protected static int period(Date atTime) {
        final int second = (int) ((new TimeInterval(atTime).value - epoch.value) % 86400);
        return second / (86400 / periods);
    }

    /// 生成secret key, K_s
    public static SecretKey secretKey() {
        //用SM3生成随机数
        PioneerPRG prg = new PioneerPRG();
        final byte[] bytes = new byte[secretKeyLength];
        prg.generateRandomNumber(bytes);
        return new SecretKey(bytes);

    }






    /// 生成 matching keys
    public static MatchingKey[] matchingKeys(SecretKey secretKey) {
        final int n = days;
        //这里的密钥是由hash链式生成的，以确保密钥不能反向生成前向密钥
        final MatchingKeySeed[] matchingKeySeed = new MatchingKeySeed[n + 1];
        //当最后一个Matching key耗尽的时候，生成新的密钥
        matchingKeySeed[n] = new MatchingKeySeed(BasicFunc.h(secretKey));
        for (int i=n; i-->0;) {
            matchingKeySeed[i] = new MatchingKeySeed(BasicFunc.h(BasicFunc.t(matchingKeySeed[i + 1])));
        }
        //第i天的匹配密钥是第i天或第i-1天的匹配密钥种子的哈希
        final MatchingKey[] matchingKey = new MatchingKey[n + 1];
        for (int i=1; i<=n; i++) {
            matchingKey[i] = new MatchingKey(BasicFunc.h(BasicFunc.xor(matchingKeySeed[i], matchingKeySeed[i - 1])));
        }
        // 第0天的匹配密钥来自第0天和第-1天的匹配密钥种子。
        //  在上面的代码中为清楚起见我们将其列为特殊情况。
        final MatchingKeySeed matchingKeySeedMinusOne = new MatchingKeySeed(BasicFunc.h(BasicFunc.t(matchingKeySeed[0])));
        matchingKey[0] = new MatchingKey(BasicFunc.h(BasicFunc.xor(matchingKeySeed[0], matchingKeySeedMinusOne)));
        return matchingKey;
    }

    /// 生成 contact keys
    public static ContactKey[] contactKeys(MatchingKey matchingKey) {
        final int n = periods;

        final ContactKeySeed[] contactKeySeed = new ContactKeySeed[n + 1];
        //在第240个间隔（一天的最后6分钟），第i天的最后一个联系人密钥种子是第i天的匹配密钥的哈希。
        contactKeySeed[n] = new ContactKeySeed(BasicFunc.h(matchingKey));
        for (int j=n; j-->0;) {
            contactKeySeed[j] = new ContactKeySeed(BasicFunc.h(BasicFunc.t(contactKeySeed[j + 1])));
        }
        final ContactKey[] contactKey = new ContactKey[n + 1];
        for (int j=1; j<=n; j++) {
            contactKey[j] = new ContactKey(BasicFunc.h(BasicFunc.xor(contactKeySeed[j], contactKeySeed[j - 1])));
        }
        final ContactKeySeed contactKeySeedMinusOne = new ContactKeySeed(BasicFunc.h(BasicFunc.t(contactKeySeed[0])));
        contactKey[0] = new ContactKey(BasicFunc.h(BasicFunc.xor(contactKeySeed[0], contactKeySeedMinusOne)));
        return contactKey;
    }

    /// 生成 contact identifer，截取contact keys的前16字节
    public static ContactIdentifier contactIdentifier(ContactKey contactKey) {
        return new ContactIdentifier(BasicFunc.t(contactKey, 16));
    }
}
