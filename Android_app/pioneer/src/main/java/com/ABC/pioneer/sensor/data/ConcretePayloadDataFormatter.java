
package com.ABC.pioneer.sensor.data;

import com.ABC.pioneer.sensor.datatype.PayloadData;

public class ConcretePayloadDataFormatter implements PayloadDataFormatter {
    @Override
    public String shortFormat(PayloadData payloadData) {
        return payloadData.shortName();
    }
}
