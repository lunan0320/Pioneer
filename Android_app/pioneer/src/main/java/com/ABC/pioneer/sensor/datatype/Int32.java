
package com.ABC.pioneer.sensor.datatype;

import java.util.Objects;

/// 有符号整型 (32位)
public class Int32 {
    public final static int bitWidth = 32;
    public final static Int32 min = new Int32(Integer.MIN_VALUE);
    public final static Int32 max = new Int32(Integer.MAX_VALUE);
    public final int value;

    public Int32(long value) {
        this.value = (int) (value < Integer.MIN_VALUE ? Integer.MIN_VALUE : (value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Int32 uInt32 = (Int32) o;
        return value == uInt32.value;
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
