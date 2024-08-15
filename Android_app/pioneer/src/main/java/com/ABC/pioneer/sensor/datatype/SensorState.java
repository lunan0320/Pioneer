
package com.ABC.pioneer.sensor.datatype;

/// 传感器状态
public enum SensorState {
    /// 传感器开启，处于活动状态且可运行
    on,
    ///传感器已关闭，处于不活动状态并且无法运行
    off,
    /// 传感器不可用
    unavailable
}
