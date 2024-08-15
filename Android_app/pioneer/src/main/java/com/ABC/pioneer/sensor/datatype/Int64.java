
package com.ABC.pioneer.sensor.datatype;

import java.util.Objects;

/// 有符号整型 (64位)
public class Int64 {
    public final static int bitWidth = 64;
    public final static Int64 min = new Int64(Long.MIN_VALUE);
    public final static Int64 max = new Int64(Long.MAX_VALUE);
    public final long value;

    public Int64(long value) {
        this.value = (value < Long.MIN_VALUE ? Long.MIN_VALUE : (value > Long.MAX_VALUE ? Long.MAX_VALUE : value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Int64 uInt64 = (Int64) o;
        return value == uInt64.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
