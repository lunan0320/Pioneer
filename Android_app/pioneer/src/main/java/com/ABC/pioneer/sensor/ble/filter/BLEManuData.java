

package com.ABC.pioneer.sensor.ble.filter;

import com.ABC.pioneer.sensor.datatype.Data;

public class BLEManuData {
    public final int manufacturer;
    public final byte[] data; // BIG ENDIAN (network order) AT THIS POINT
    public final Data raw;
    // 根据网络顺序，在此处我们采取大端的方式存储
    public BLEManuData(final int manufacturer, final byte[] dataBigEndian, final Data raw) {
        this.manufacturer = manufacturer;
        this.data = dataBigEndian;
        this.raw = raw;
    }

    @Override
    public String toString() {
        return "BLEAdvertManufacturerData{" +
                "manufacturer=" + manufacturer +
                ", data=" + new Data(data).hexEncodedString() +
                ", raw=" + raw.hexEncodedString() +
                '}';
    }
}
