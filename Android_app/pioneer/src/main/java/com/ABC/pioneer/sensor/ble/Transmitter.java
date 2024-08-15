
package com.ABC.pioneer.sensor.ble;

import com.ABC.pioneer.sensor.Sensor;
import com.ABC.pioneer.sensor.SensorDelegate;
import com.ABC.pioneer.sensor.datatype.PayloadData;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

// Beacon transmitter广播固定服务UUID，以启用iOS的后台扫描。
// 当iOS进入后台模式时，UUID将从广播中消失
// 因此Android设备需要搜索Apple设备，然后连接并发现服务以读取UUID。
public interface Transmitter extends Sensor {
    // 接收Beacon检测事件的delegate。
    // 这是必要的，因为某些Android设备（三星J6）不支持BLE传输
    // 因此要使Beacon Characteristic可写的话，此类设备提供了一种机制，
    // 可通过发送其自身的beacon code和RSSI作为数据来检测Beacon transmitter并知道其自身的存在到transmitter。

    Queue<SensorDelegate> delegates = new ConcurrentLinkedQueue<>();
    // 获取当前Payload。
    PayloadData payloadData();

    //是否支持Transmitter功能
    // 如果BLE广播功能正常，则返回True

    boolean isSupported();
}
