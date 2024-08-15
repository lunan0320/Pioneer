
package com.ABC.pioneer.sensor.ble.filter;

import java.util.HashMap;
import java.util.Map;

/// BLE广告类型-注意：出于某些原因，我们仅列出在Pioneer中使用的广告类型
public enum BLESegType {
    unknown("unknown", 0x00),
    serviceUUID16IncompleteList("serviceUUID16IncompleteList", 0x02),
    serviceUUID16CompleteList("serviceUUID16CompleteList", 0x03),
    serviceUUID32IncompleteList("serviceUUID32IncompleteList", 0x04),
    serviceUUID32CompleteList("serviceUUID32CompleteList", 0x05),
    serviceUUID128IncompleteList("serviceUUID128IncompleteList", 0x06),
    serviceUUID128CompleteList("serviceUUID128CompleteList", 0x07),
    deviceNameShortened("deviceNameShortened", 0x08),
    deviceNameComplete("deviceNameComplete", 0x09),
    txPowerLevel("txPower",0x0A),
    deviceClass("deviceClass",0x0D),
    simplePairingHash("simplePairingHash",0x0E),
    simplePairingRandomiser("simplePairingRandomiser",0x0F),
    deviceID("deviceID",0x10),
    serviceUUID16Data("serviceUUID16Data", 0x16),
    meshMessage("meshMessage",0x2A),
    meshBeacon("meshBeacon",0x2B),
    bigInfo("bigInfo",0x2C),
    broadcastCode("broadcastCode",0x2D),
    manufacturerData("manufacturerData", 0xFF)
    ;

    private static final Map<String, BLESegType> BY_LABEL = new HashMap<>();
    private static final Map<Integer, BLESegType> BY_CODE = new HashMap<>();
    static {
        for (BLESegType e : values()) {
            BY_LABEL.put(e.label, e);
            BY_CODE.put(e.code, e);
        }
    }

    public final String label;
    public final int code;

    private BLESegType(String label, int code) {
        this.label = label;
        this.code = code;
    }

    public static BLESegType typeFor(int code) {
        BLESegType type = BY_CODE.get(code);
        if (null == type) {
            return BY_LABEL.get("unknown");
        }
        return type;
    }

    public static BLESegType typeFor(String commonName) {
        BLESegType type = BY_LABEL.get(commonName);
        if (null == type) {
            return BY_LABEL.get("unknown");
        }
        return type;
    }
}
