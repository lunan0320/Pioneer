
package com.ABC.pioneer.sensor.datatype;

public class PayloadSharingData {
    public final RSSI rssi;
    public final Data data;

    public PayloadSharingData(final RSSI rssi, final Data data) {
        this.rssi = rssi;
        this.data = data;
    }
}
