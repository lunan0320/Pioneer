package com.ABC.pioneer.sensor.payload.extended;

import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.datatype.UInt8;

public class ConcreteExtendedDataSectionV1 {
    public final UInt8 code;
    public final UInt8 length;
    public final Data data;

    public ConcreteExtendedDataSectionV1(UInt8 code, UInt8 length, Data data) {
        this.code = code;
        this.length = length;
        this.data = data;
    }
}