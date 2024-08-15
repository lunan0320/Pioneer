
package com.ABC.pioneer.sensor.payload.Crypto;


import com.ABC.pioneer.sensor.Device;

import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.PayloadTimestamp;
import com.ABC.pioneer.sensor.payload.DefaultPayloadSupplier;
import com.ABC.pioneer.sensor.datatype.UInt64;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;





/// 简单的有效载荷标识
public class SpecificUsePayloadSupplier extends DefaultPayloadSupplier implements UsePayloadSupplier {
    public final static int payloadLength = 61;
    public static  MatchingKey[] matchingKeys;
    //定义固定的5字节首部
    private static final Data commonPayload = new Data((byte)0,5);
    // 缓存当天的联系人标识符
    private static Integer day = null;
    private static ContactIdentifier[] contactIdentifiers = null;

    public SpecificUsePayloadSupplier(SecretKey secretKey) {
        // 所有数据均为大端
        // 从秘密密钥生成匹配密钥
        matchingKeys = GenerateKey.matchingKeys(secretKey);
    }
    public MatchingKey matchingkey()
    {
        return matchingKeys[day];
    }

    /// 生成一个新的密钥
    public static SecretKey generateSecretKey() {
        return GenerateKey.secretKey();
    }

    // 由matching key生成contactidentifiers
    public static ContactIdentifier[] contactIdentifiers(MatchingKey matchingKey) {
        final ContactKey[] contactKeys = GenerateKey.contactKeys(matchingKey);
        final ContactIdentifier[] contactIdentifiers = new ContactIdentifier[contactKeys.length];
        for (int i=contactKeys.length; i-->0;) {
            contactIdentifiers[i] = GenerateKey.contactIdentifier(contactKeys[i]);
        }
        return contactIdentifiers;
    }

    public static Data parseRawData(PayloadData payloadData) {
        return payloadData.subdata(5,24);
    }

    // 根据时间time生成当前时间的contactidentifier
    private ContactIdentifier contactIdentifier(Date time) {
        final int day = GenerateKey.day(time);
        final int period = GenerateKey.period(time);

        if (!(day >= 0 && day < matchingKeys.length)) {
            return null;
        }

        // Generate and cache contact keys for specific day on-demand
        if (this.day == null || this.day != day) {
            contactIdentifiers = contactIdentifiers(matchingKeys[day]);
            this.day = day;
        }

        if (contactIdentifiers == null) {
            return null;
        }

        if (!(period >= 0 && period < contactIdentifiers.length)) {
            return null;
        }

        // 安全性检验
        if (contactIdentifiers[period].value.length != 16) {
            return null;
        }

        return contactIdentifiers[period];
    }

    // SimplePayloadDataSupplier

    //用来更新UI界面Payload的函数
    public static PayloadData updatePayload(Date time){
        final int day = GenerateKey.day(time);
        final int period = GenerateKey.period(time);
        final PayloadData payloadData = new PayloadData();
        payloadData.append(commonPayload);

        // 添加contactIdentifier
        final ContactIdentifier contactIdentifier = contactIdentifiers(matchingKeys[day])[period];
        if (contactIdentifier != null) {
            payloadData.append(contactIdentifier);
        } else {
            payloadData.append(new ContactIdentifier((byte) 0, 16));
        }
        return payloadData;
    }

    @Override
    public PayloadData payload(PayloadTimestamp timestamp, Device device) {
        final PayloadData payloadData = new PayloadData();
        payloadData.append(commonPayload);
        // 添加contactIdentifier
        final ContactIdentifier contactIdentifier = contactIdentifier(timestamp.value);
        if (contactIdentifier != null) {
            payloadData.append(contactIdentifier);
        } else {
            payloadData.append(new ContactIdentifier((byte) 0, 16));
        }

        // 添加开始时间和结束时间以及MAC
        appendTimeandMac(timestamp.value, payloadData);
        return payloadData;
    }

    private static void appendTimeandMac(Date start_time, PayloadData payloadData) {
        final long start;
        final long Start;
        start = start_time.getTime()/1000;
        Start = start*1000;
        // 有效开始时间和结束时间
        try {
            payloadData.append(new UInt64(Start));
        }
        catch(Exception e)
        {
            payloadData.append(new Data((byte)0,8));
        }


        // 添加 MAC
        if (day == null || matchingKeys == null)
            payloadData.append(new Data((byte)0,32));
        if (!(day >= 0 && day < matchingKeys.length)) {
            payloadData.append(new Data((byte)0,32));
        }

        Data Rowdata = SpecificUsePayloadSupplier.parseRawData(payloadData);
        try {
            Data mac = new Data(HMACSHA256.GenerateMAC(Rowdata.value, matchingKeys[day].value));
            payloadData.append(mac);
        } catch (Exception e) {
            payloadData.append(new Data((byte)0,32));
        }


    }



    @Override
    public List<PayloadData> payload(Data data) {
        // 将包含级联有效载荷的原始数据拆分为单个有效载荷
        final List<PayloadData> payloads = new ArrayList<>();
        final byte[] bytes = data.value;
        for (int index = 0; (index + payloadLength) <= bytes.length; index += payloadLength) {
            final byte[] payloadBytes = new byte[payloadLength];
            System.arraycopy(bytes, index, payloadBytes, 0, payloadLength);
            payloads.add(new PayloadData(payloadBytes));
        }
        return payloads;
    }


    // 检查payload时间
    public static boolean checkPayloadtime(PayloadData payloadData) {

        try {
            final long r = new Date().getTime();
            Data timeData = parseStartTime(payloadData);
            final long s = timeData.uint64(0).value;
            final long e = s + 360000;
            if (r > s && r < e){
                return true;
            }

            else{
                return false;
            }
        }
        catch(Exception e)
        {
            return false;
        }
    }

    //解析Mac
    public static Data parseMac(PayloadData payloadData) {
        return payloadData.subdata(29,32);
    }

    //解析ontactIdentifier
    public static Data parseContactIdentifier(PayloadData payloadData) {
        return payloadData.subdata(5,16);
    }

    //解析StartTime
    public static Data parseStartTime(PayloadData payloadData) {
        return payloadData.subdata(21,8);
    }


    //解析ContactIdentifier
    static public String parseContactIdentifierToStr(PayloadData payloadData){
        final byte[] parsedata = new byte[16];
        System.arraycopy(payloadData.value,5,parsedata,0,16);
        StringBuffer result = new StringBuffer(parsedata.length);
        String Step;
        for(int j=0;j<parsedata.length;j++){
            Step = Integer.toHexString(0XFF & parsedata[j]);
            if(Step.length() < 2)
                result.append(0);
            result.append(Step.toUpperCase());
        }
        return result.toString();
    }

    //解析StartTime
    static public long parseStartTimeToLong(PayloadData payloadData) {
        Data timeData = parseStartTime(payloadData);
        final long s = timeData.uint64(0).value;
        return s;
    }
}
