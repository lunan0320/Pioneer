
package com.ABC.pioneer.sensor.payload.Crypto;

import com.ABC.pioneer.sensor.datatype.Data;

/// 临时身份标识
public class ContactIdentifier extends Data {

    public ContactIdentifier(Data value) {
        super(value);
    }

    public ContactIdentifier(byte repeating, int count) {
        super(repeating, count);
    }
}
