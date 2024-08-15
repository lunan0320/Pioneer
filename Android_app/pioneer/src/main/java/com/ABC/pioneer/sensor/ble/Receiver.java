

package com.ABC.pioneer.sensor.ble;

import com.ABC.pioneer.sensor.Sensor;
import com.ABC.pioneer.sensor.SensorDelegate;
import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.datatype.TargetIdentifier;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

//receiver扫描具有固定Service UUID 的外围设备
public interface Receiver extends Sensor {
    Queue<SensorDelegate> delegates = new ConcurrentLinkedQueue<>();

    /// 立即发送数据。
    boolean immediateSend(Data data, TargetIdentifier targetIdentifier);

    // 立即发送给所有人（已连接/最近/附近）
    boolean immediateSendAll(Data data);
}
