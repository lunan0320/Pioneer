
package com.ABC.pioneer.sensor.datatype;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// IEEE 754二进制16格式16位浮点数
public class Float16 {
    public final float value;
    public final Data bigEndian;

    public Float16(float value) {
        this.value = value;
        final ByteBuffer byteBuffer = ByteBuffer.allocate(2);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort((short) float16(value));
        this.bigEndian = new Data(byteBuffer.array());
    }

    public Float16(Data bigEndian) {
        this.bigEndian = bigEndian;
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bigEndian.value);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        final int value = byteBuffer.getShort(0);
        this.value = valueOf(value);
    }

    /// IEEE 754二进制16格式16位浮点数
    private static int float16(float values) {
        final int bits = Float.floatToIntBits(values);
        final int sign = bits >>> 16 & 0x8000;
        int val = (bits & 0x7fffffff) + 0x1000;
        if (val >= 0x47800000) {
            if ((bits & 0x7fffffff) >= 0x47800000) {
                if (val < 0x7f800000) {
                    return sign | 0x7c00;
                }
                return sign | 0x7c00 | (bits & 0x007fffff) >>> 13;
            }
            return sign | 0x7bff;
        }
        if (val >= 0x38800000) {
            return sign | val - 0x38000000 >>> 13;
        }
        if (val < 0x33000000) {
            return sign;
        }
        val = (bits & 0x7fffffff) >>> 23;
        return sign | ((bits & 0x7fffff | 0x800000) + (0x800000 >>> val - 102) >>> 126 - val);
    }

    private static float valueOf(int hbits) {
        int mant = hbits & 0x03ff;
        int exp = hbits & 0x7c00;
        if (exp == 0x7c00) {
            exp = 0x3fc00;
        } else if (exp != 0) {
            exp += 0x1c000;
            if (mant == 0 && exp > 0x1c400) {
                return Float.intBitsToFloat((hbits & 0x8000) << 16 | exp << 13 | 0x3ff);
            }
        } else if (mant != 0) {
            exp = 0x1c400;
            do {
                mant <<= 1;
                exp -= 0x400;
            } while ((mant & 0x400) == 0);
            mant &= 0x3ff;
        }
        return Float.intBitsToFloat((hbits & 0x8000) << 16 | (exp | mant) << 13);
    }
}
