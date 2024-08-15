
package com.ABC.pioneer.sensor.datatype;

import java.util.Objects;

/// 无符号数整数(16位)
public class UInt16 {
    public final static int bitWidth = 16;
    public final static UInt16 min = new UInt16(0);
    public final static UInt16 max = new UInt16(65535);
    public final int value;

    public UInt16(int value) {
        this.value = (value < 0 ? 0 : (value > 65535 ? 65535 : value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UInt16 uInt16 = (UInt16) o;
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
