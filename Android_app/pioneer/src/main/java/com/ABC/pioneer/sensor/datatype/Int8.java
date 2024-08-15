
package com.ABC.pioneer.sensor.datatype;

import java.util.Objects;

/// 有符号整型（8位）
public class Int8 {
    public final static int bitWidth = 8;
    public final static Int8 min = new Int8(Byte.MIN_VALUE);
    public final static Int8 max = new Int8(Byte.MAX_VALUE);
    public final int value;

    public Int8(int value) {
        this.value = (value < Byte.MIN_VALUE ? Byte.MIN_VALUE : (value > Byte.MAX_VALUE ? Byte.MAX_VALUE : value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Int8 uInt8 = (Int8) o;
        return value == uInt8.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
