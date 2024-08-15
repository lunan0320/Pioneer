//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.ABC.pioneer.sensor;

import com.ABC.pioneer.sensor.datatype.ImmediateSendData;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.Proximity;
import com.ABC.pioneer.sensor.datatype.SensorType;
import com.ABC.pioneer.sensor.datatype.TargetIdentifier;

import java.util.List;


// 用来接受传感事件的传感器代理
public interface SensorDelegate {
    // 用临时身份标识符探测周边设备
    void sensor(SensorType sensor, TargetIdentifier didDetect);

    // 从目标处读取payload
    void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget);

    // 从目标处写下立即传送的数据
    void sensor(SensorType sensor, ImmediateSendData didReceive, TargetIdentifier fromTarget);

    // 从一个目标处读取该目标最近从其它设备接收到的payload
    void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget);

    // 测量和一个目标的接近度
    void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget);

}
