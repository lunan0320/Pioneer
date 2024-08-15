
package com.ABC.pioneer.sensor.datatype;

/// 从目标接收到的加密有效负载数据。 这很可能是目标的实际永久标识符的加密数据报。
public class PayloadData extends Data {

    public PayloadData(byte[] value) {
        super(value);
    }

    public PayloadData(String base64EncodedString) {
        super(base64EncodedString);
    }

    public PayloadData(byte repeating, int count) {
        super(repeating, count);
    }

    public PayloadData() {
        this(new byte[0]);
    }

    public String shortName() {
        if (value.length == 0) {
            return "";
        }
        if (!(value.length > 3)) {
            return Base64.encode(value);
        }
        final Data subdata = subdata(3, value.length - 3);
        final byte[] suffix = (subdata == null || subdata.value == null ? new byte[0] : subdata.value);
        final String base64EncodedString = Base64.encode(suffix);
        return base64EncodedString.substring(0, Math.min(6, base64EncodedString.length()));
    }

    public String toString() {
        return shortName();
    }
}
