package com.ABC.pioneer.sensor;

import com.ABC.pioneer.sensor.datatype.TargetIdentifier;

import java.util.Date;

public class Device {
    ///设备注册时间戳
    public final Date createdAt;
    ///上次任何更改，例如attribute更新
    public Date lastUpdatedAt = null;
    //临时设备标识符，例如 外设标识符UUID
    public final TargetIdentifier identifier;

    public Device(TargetIdentifier identifier) {
        this.createdAt = new Date();
        this.lastUpdatedAt = this.createdAt;
        this.identifier = identifier;
    }

    public Device(Device device, TargetIdentifier identifier) {
        this.createdAt = device.createdAt;
        this.lastUpdatedAt = new Date();
        this.identifier = identifier;
    }
}
