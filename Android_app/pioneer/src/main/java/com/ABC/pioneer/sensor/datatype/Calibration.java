
package com.ABC.pioneer.sensor.datatype;

import androidx.annotation.NonNull;

import java.util.Objects;

/// 用于解释传感器与目标之间接近值的校准数据，例如发射BLE的功率。
public class Calibration {
    /// 度量单位，例如：发射功率
    public final CalibrationMeasurementUnit unit;
    /// 测量值 BLE广告中的发射功率
    public final Double value;

    public Calibration(CalibrationMeasurementUnit unit, Double value) {
        this.unit = unit;
        this.value = value;
    }

    /// 获取校准数据的纯文本描述
    public String description() {
        return unit + ":" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Calibration that = (Calibration) o;
        return unit == that.unit &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unit, value);
    }

    @NonNull
    @Override
    public String toString() {
        return description();
    }
}
