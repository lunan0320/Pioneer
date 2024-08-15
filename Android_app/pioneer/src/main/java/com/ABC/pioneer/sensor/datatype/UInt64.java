

package com.ABC.pioneer.sensor.datatype;

import java.util.Objects;

/// 无符号数整数(8位)
public class UInt64 {
    public final static int bitWidth = 64;
    public final static UInt64 min = new UInt64(0);
    // 将max设置为有符号的long max，而不是无符号的long max，
    // 因为Java无符号的long算术函数相对不成熟，因此很可能引起混乱。
    public final static UInt64 max = new UInt64(Long.MAX_VALUE);
    public final long value;

    public UInt64(long value) {
        this.value = (value < 0 ? 0 : (value > Long.MAX_VALUE ? Long.MAX_VALUE : value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UInt64 uInt64 = (UInt64) o;
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
