
package com.ABC.pioneer.sensor.datatype;

/// 用于解释邻近数据值的测量单元。
public enum ProximityMeasurementUnit {
    /// 接收信号强度指示，例如 BLE信号强度作为接近度估算器。
    RSSI,
    /// 往返时间，例如 音频信号回声持续时间作为接近度估算器。
    RTT
}
