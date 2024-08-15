

package com.ABC.pioneer.sensor.payload;

import com.ABC.pioneer.sensor.Device;
import com.ABC.pioneer.sensor.PayloadSupplier;
import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.datatype.LegacyPayload;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.PayloadTimestamp;

import java.util.ArrayList;
import java.util.List;

// 默认的有效载荷数据提供者，实现了固定长度的有效载荷拆分方法。
public abstract class DefaultPayloadSupplier implements PayloadSupplier {

    @Override
    public LegacyPayload legacyPayload(PayloadTimestamp timestamp, Device device) {
        return null;
    }

    @Override
    public  List<PayloadData> payload(Data data) {
        // 获取固定长度的有效载荷数据
        final PayloadData fixedLengthPayloadData = payload(new PayloadTimestamp(), null);
        final int payloadDataLength = fixedLengthPayloadData.value.length;
        // 将包含串联有效负载的原始数据拆分为单独的有效负载
        final List<PayloadData> payloads = new ArrayList<>();
        final byte[] bytes = data.value;
        for (int index = 0; (index + payloadDataLength) <= bytes.length; index += payloadDataLength) {
            final byte[] payloadBytes = new byte[payloadDataLength];
            System.arraycopy(bytes, index, payloadBytes, 0, payloadDataLength);
            payloads.add(new PayloadData(payloadBytes));
        }
        return payloads;
    }
}
