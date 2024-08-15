package com.ABC.pioneer.sensor.payload;

import com.ABC.pioneer.sensor.datatype.Data;

public class DigitalSignature extends Data {
    private static  Data message = new Data();
    public DigitalSignature(Data message){this.message = message;}
    public DigitalSignature(){this.message = message;}
    public static Data genMAC(){return message;}
}
