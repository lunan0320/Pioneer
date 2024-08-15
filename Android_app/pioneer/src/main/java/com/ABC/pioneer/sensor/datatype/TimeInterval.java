

package com.ABC.pioneer.sensor.datatype;

import java.util.Date;
import java.util.Objects;

/// 时间间隔（以秒为单位）。
public class TimeInterval {
    public final long value;
    public static final TimeInterval minute = new TimeInterval(60);
    public static final TimeInterval zero = new TimeInterval(0);
    public static final TimeInterval never = new TimeInterval(Long.MAX_VALUE);

    public TimeInterval(long seconds) {
        this.value = seconds;
    }

    public TimeInterval(Date date) {
        this.value = date.getTime() / 1000;
    }

    public TimeInterval(Date from, Date to) {
        this.value = (to.getTime() - from.getTime()) / 1000;
    }

    public static TimeInterval minutes(long minutes) {
        return new TimeInterval(minute.value * minutes);
    }

    public static TimeInterval seconds(long seconds) {
        return new TimeInterval(seconds);
    }

    public long millis() {
        return value * 1000;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeInterval that = (TimeInterval) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        if (value == never.value) {
            return "never";
        }
        return Long.toString(value);
    }
}
