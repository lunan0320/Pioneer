
package com.ABC.pioneer.sensor.ble.filter;

import com.ABC.pioneer.sensor.datatype.Data;

public class BLESeg {
    public final BLESegType type;
    public final int dataLength;
    public final byte[] data;
    // 根据网络顺序，在此处我们采取大端的方式存储
    public final Data raw;

    public BLESeg(BLESegType type, int dataLength, byte[] data, Data raw) {
        this.type = type;
        this.dataLength = dataLength;
        this.data = data;
        this.raw = raw;
    }

    @Override
    public String toString() {
        return "BLEAdvertSegment{" +
                "type=" + type +
                ", dataLength=" + dataLength +
                ", data=" + new Data(data).hexEncodedString() +
                ", raw=" + raw.hexEncodedString() +
                '}';
    }
}
