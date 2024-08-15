
package com.ABC.pioneer.sensor.datatype;

import com.ABC.pioneer.sensor.ble.Configurations;

import java.util.UUID;

/// 从目标接收到的旧有效负载数据
public class LegacyPayload extends PayloadData {
    public final UUID service;
    public enum ProtocolName {
        UNKNOWN, NOT_AVAILABLE, PIONEER
    }

    public LegacyPayload(final UUID service, final byte[] value) {
        super(value);
        this.service = service;
    }

    public ProtocolName protocolName() {
        if (service == null) {
            return ProtocolName.NOT_AVAILABLE;
        } else if (service == Configurations.serviceUUID) {
            return ProtocolName.PIONEER;
        } else {
            return ProtocolName.UNKNOWN;
        }
    }

    @Override
    public String shortName() {
        return super.shortName();
    }
}
