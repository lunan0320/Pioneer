
package com.ABC.pioneer.sensor.ble.filter;

import com.ABC.pioneer.sensor.datatype.Data;

public class BLEAppleManuSeg {
    public final int type;
    public final int reportedLength;
    public final byte[] data;
    // 根据网络顺序，在此处我们采取大端的方式存储
    public final Data raw;

    public BLEAppleManuSeg(int type, int reportedLength, byte[] dataBigEndian, Data raw) {
        this.type = type;
        this.reportedLength = reportedLength;
        this.data = dataBigEndian;
        this.raw = raw;
    }

    @Override
    public String toString() {
        return raw.hexEncodedString();
    }
}
