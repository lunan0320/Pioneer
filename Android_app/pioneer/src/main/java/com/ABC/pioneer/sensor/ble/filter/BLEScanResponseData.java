
package com.ABC.pioneer.sensor.ble.filter;

import java.util.List;

public class BLEScanResponseData {
    public int dataLength;
    public List<BLESeg> segments;

    public BLEScanResponseData(int dataLength, List<BLESeg> segments) {
        this.dataLength = dataLength;
        this.segments = segments;
    }

    @Override
    public String toString() {
        return segments.toString();
    }
}
