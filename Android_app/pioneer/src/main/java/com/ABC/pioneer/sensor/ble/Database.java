
package com.ABC.pioneer.sensor.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.PayloadSharingData;
import com.ABC.pioneer.sensor.datatype.TargetIdentifier;

import java.util.List;

// 用于整理来自异步BLE操作的信息片段的注册表。
public interface Database {
    // 添加用于处理数据库事件的委托
    void add(BLEDatabaseDelegate delegate);

    // 获取或创建用于整理来自异步BLE操作的信息的设备。
    BLEDevice device(ScanResult scanResult);

    // 获取或创建用于整理来自异步BLE操作的信息的设备。
    BLEDevice device(BluetoothDevice bluetoothDevice);

    /// 获取或创建用于整理来自异步BLE操作的信息的设备。
    BLEDevice device(PayloadData payloadData);

    /// 从TargetIdentifier获取设备
    BLEDevice device(TargetIdentifier targetIdentifier);

    /// 获取所有设备
    List<BLEDevice> devices();

    /// 删除设备
    void delete(BLEDevice device);

    /// 获取对等方的有效负载共享数据
    PayloadSharingData payloadSharingData(BLEDevice peer);
}
