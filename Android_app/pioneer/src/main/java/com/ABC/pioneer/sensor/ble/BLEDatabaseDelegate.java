
package com.ABC.pioneer.sensor.ble;

/// 代表接收注册表创建/更新/删除事件
public interface BLEDatabaseDelegate {
    void bleDatabaseDidCreate(BLEDevice device);

    void bleDatabaseDidUpdate(BLEDevice device, DeviceAttribute attribute);

    void bleDatabaseDidDelete(BLEDevice device);
}
