
package com.ABC.pioneer.sensor.payload.Crypto;


import com.ABC.pioneer.sensor.datatype.Data;


/// 基本功能
public class BasicFunc {

    /// hash函数SHA256
    public static Data h(Data data) {
        try {
            byte[] hash = new byte[32];
            PioneerHash ph = new PioneerHash();
            ph.generateHash(data.value,hash);
            return new Data(hash);
        } catch (Throwable e) {
            return null;
        }
    }

    /// 截取函数 : 删除数据的后半部分
    public static Data t(Data data) {
        return t(data, data.value.length / 2);
    }

    /// 截取函数：保留数据的前n个字节
    public static Data t(Data data, int n) {
        return data.subdata(0, n);
    }

    /// 异或函数 : 计算左半部分异或右半部分，假设左和右的长度相同
    public static Data xor(Data left, Data right) {
        final byte[] leftByteArray = left.value;
        final byte[] rightByteArray = right.value;
        final byte[] resultByteArray = new byte[left.value.length];
        for (int i=0; i<leftByteArray.length; i++) {
            resultByteArray[i] = (byte) (leftByteArray[i] ^ rightByteArray[i]);
        }
        return new Data(resultByteArray);
    }
}
