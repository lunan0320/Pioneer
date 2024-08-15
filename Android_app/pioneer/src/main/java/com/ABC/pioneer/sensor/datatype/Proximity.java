
package com.ABC.pioneer.sensor.datatype;

import androidx.annotation.NonNull;

import java.util.Objects;

/// 用于估算传感器与目标之间接近程度的原始数据，例如 BLE的RSSI。
public class Proximity {
    /// 度量单位，例如：RSSI
    public final ProximityMeasurementUnit unit;
    /// 测量值，例如：原始RSSI值。
    public final Double value;
    /// 校准数据（可选），例如：发射功率
    public final Calibration calibration;

    public Proximity(ProximityMeasurementUnit unit, Double value) {
        this(unit, value, null);
    }

    public Proximity(ProximityMeasurementUnit unit, Double value, Calibration calibration) {
        this.unit = unit;
        this.value = value;
        this.calibration = calibration;
    }

    /// 获取邻近数据的纯文本描述
    public String description() {
        if (calibration == null) {
            return unit + ":" + value;
        }
        return unit + ":" + value + "[" + calibration.description() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Proximity proximity = (Proximity) o;
        return unit == proximity.unit &&
                Objects.equals(value, proximity.value) &&
                Objects.equals(calibration, proximity.calibration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unit, value, calibration);
    }

    @NonNull
    @Override
    public String toString() {
        return description();
    }
}
