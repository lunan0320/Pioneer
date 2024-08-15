
package com.ABC.pioneer.sensor.ble.filter;

import com.ABC.pioneer.sensor.datatype.Data;

public class BLEServiceData {
    public final byte[] service;
    public final byte[] data;
    // 根据网络顺序，在此处我们采取大端的方式存储
    public final Data raw;

    public BLEServiceData(final byte[] service, final byte[] dataBigEndian, final Data raw) {
        this.service = service;
        this.data = dataBigEndian;
        this.raw = raw;
    }

    @Override
    public String toString() {
        return "BLEAdvertServiceData{" +
                "service=" + new Data(service).hexEncodedString() +
                ", data=" + new Data(data).hexEncodedString() +
                ", raw=" + raw.hexEncodedString() +
                '}';
    }
}
