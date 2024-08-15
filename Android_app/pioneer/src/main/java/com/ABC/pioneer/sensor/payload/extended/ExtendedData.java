package com.ABC.pioneer.sensor.payload.extended;

import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.datatype.Float16;
import com.ABC.pioneer.sensor.datatype.Int16;
import com.ABC.pioneer.sensor.datatype.Int32;
import com.ABC.pioneer.sensor.datatype.Int64;
import com.ABC.pioneer.sensor.datatype.Int8;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.UInt16;
import com.ABC.pioneer.sensor.datatype.UInt32;
import com.ABC.pioneer.sensor.datatype.UInt64;
import com.ABC.pioneer.sensor.datatype.UInt8;

public interface ExtendedData {
    boolean hasData();
    void addSection(UInt8 code, UInt8 value);
    void addSection(UInt8 code, UInt16 value);
    void addSection(UInt8 code, UInt32 value);
    void addSection(UInt8 code, UInt64 value);
    void addSection(UInt8 code, Int8 value);
    void addSection(UInt8 code, Int16 value);
    void addSection(UInt8 code, Int32 value);
    void addSection(UInt8 code, Int64 value);
    void addSection(UInt8 code, Float16 value);
    void addSection(UInt8 code, String value);
    void addSection(UInt8 code, Data value);
    PayloadData payload();
}