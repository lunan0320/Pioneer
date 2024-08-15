
package com.ABC.pioneer.sensor.datatype;

import java.util.Objects;

/// 有符号整型 (16 位)
public class Int16 {
    public final static int bitWidth = 16;
    public final static Int16 min = new Int16(Short.MIN_VALUE);
    public final static Int16 max = new Int16(Short.MAX_VALUE);
    public final int value;

    public Int16(int value) {
        this.value = (value < Short.MIN_VALUE ? Short.MIN_VALUE : (value > Short.MAX_VALUE ? Short.MAX_VALUE : value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Int16 uInt16 = (Int16) o;
        return value == uInt16.value;
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
