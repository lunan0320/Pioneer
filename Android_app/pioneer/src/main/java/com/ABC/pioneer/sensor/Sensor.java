//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.ABC.pioneer.sensor;

///用于检测和跟踪各种疾病传播媒介的传感器
public interface Sensor {
    /// 添加delegate以响应传感器事件。
    void add(SensorDelegate delegate);

    /// 开始检测。
    void start();

    /// 停止检测。
    void stop();
}
