

package com.ABC.pioneer.sensor.datatype;

import android.bluetooth.BluetoothDevice;

import java.util.Objects;
import java.util.UUID;

/// 检测到的目标（例如智能手机，信标，地点）的临时标识符。
//  这可能是一个UUID，但使用String表示变量标识符长度。
public class TargetIdentifier {
    public final String value;

    protected TargetIdentifier(final String value) {
        this.value = value;
    }

    /// 创建随机目标标识符
    public TargetIdentifier() {
        this(UUID.randomUUID().toString());
    }

    /// 根据蓝牙设备地址创建目标标识符
    public TargetIdentifier(BluetoothDevice bluetoothDevice) {
        this(bluetoothDevice.getAddress());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TargetIdentifier that = (TargetIdentifier) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
