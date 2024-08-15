

package com.ABC.pioneer.sensor.datatype;

import com.ABC.pioneer.sensor.ble.Configurations;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/// 信号特征数据包的编解码器
public class SignalCharacteristicData {

    /// 编码写入RSSI数据包
    // writeRSSI数据格式
    // 0-0 : 动作码
    // 1-2 : rssi值(Int16)
    public static Data encodeWriteRssi(final RSSI rssi) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(3);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(0, Configurations.signalCharacteristicActionWriteRSSI);
        byteBuffer.putShort(1, (short) rssi.value);
        return new Data(byteBuffer.array());
    }

    /// 解码写入RSSI数据包
    public static RSSI decodeWriteRSSI(final Data data) {
        if (data == null || data.value == null) {
            return null;
        }
        if (signalDataActionCode(data.value) != Configurations.signalCharacteristicActionWriteRSSI) {
            return null;
        }
        if (data.value.length != 3) {
            return null;
        }
        final Short rssiValue = int16(data.value, 1);
        if (rssiValue == null) {
            return null;
        }
        return new RSSI(rssiValue.intValue());
    }

    /// 编码写入payload数据包
    // writePayload数据格式
    // 0-0 : 动作码
    // 1-2 : payload数据计数（以字节为单位）(Int16)
    // 3.. : payload数据
    public static Data encodeWritePayload(final PayloadData payloadData) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(3 + payloadData.value.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(0, Configurations.signalCharacteristicActionWritePayload);
        byteBuffer.putShort(1, (short) payloadData.value.length);
        byteBuffer.position(3);
        byteBuffer.put(payloadData.value);
        return new Data(byteBuffer.array());
    }

    /// 解码写入有效负载数据包
    public static PayloadData decodeWritePayload(final Data data) {
        if (data == null || data.value == null) {
            return null;
        }
        if (signalDataActionCode(data.value) != Configurations.signalCharacteristicActionWritePayload) {
            return null;
        }
        if (data.value.length < 3) {
            return null;
        }
        final Short payloadDataCount = int16(data.value, 1);
        if (payloadDataCount == null) {
            return null;
        }
        if (payloadDataCount == 0) {
            return new PayloadData();
        }
        if (data.value.length != (3 + payloadDataCount.intValue())) {
            return null;
        }
        final Data payloadDataBytes = new Data(data.value).subdata(3);
        if (payloadDataBytes == null) {
            return null;
        }
        return new PayloadData(payloadDataBytes.value);
    }

    /// 编码写入有效负载共享数据包
    // writePayloadSharing数据格式
    // 0-0 : 动作码
    // 1-2 : rssi值(Int16)
    // 3-4 : payload共享数据 (Int16)
    // 5.. : payload sharing 数据值
    public static Data encodeWritePayloadSharing(final PayloadSharingData payloadSharingData) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(5 + payloadSharingData.data.value.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(0, Configurations.signalCharacteristicActionWritePayloadSharing);
        byteBuffer.putShort(1, (short) payloadSharingData.rssi.value);
        byteBuffer.putShort(3, (short) payloadSharingData.data.value.length);
        byteBuffer.position(5);
        byteBuffer.put(payloadSharingData.data.value);
        return new Data(byteBuffer.array());
    }

    /// 解码写入有效负载数据包
    public static PayloadSharingData decodeWritePayloadSharing(final Data data) {
        if (data == null || data.value == null) {
            return null;
        }
        if (signalDataActionCode(data.value) != Configurations.signalCharacteristicActionWritePayloadSharing) {
            return null;
        }
        if (data.value.length < 5) {
            return null;
        }
        final Short rssiValue = int16(data.value, 1);
        if (rssiValue == null) {
            return null;
        }
        final Short payloadSharingDataCount = int16(data.value, 3);
        if (payloadSharingDataCount == null) {
            return null;
        }
        if (payloadSharingDataCount == 0) {
            return new PayloadSharingData(new RSSI(rssiValue.intValue()), new Data());
        }
        if (data.value.length != (5 + payloadSharingDataCount.intValue())) {
            return null;
        }
        final Data payloadSharingDataBytes = new Data(data.value).subdata(5);
        if (payloadSharingDataBytes == null) {
            return null;
        }
        return new PayloadSharingData(new RSSI(rssiValue.intValue()), payloadSharingDataBytes);
    }

    /// 编码立即发送数据包
    //  立即发送数据格式
    // 0-0 : 动作码
    // 1-2 : payload数据计数(Int16)
    // 3.. : payload data值
    public static Data encodeImmediateSend(final ImmediateSendData immediateSendData) {
        final ByteBuffer byteBuffer = ByteBuffer.allocate(3 + immediateSendData.data.value.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(0, Configurations.signalCharacteristicActionWriteImmediate);
        byteBuffer.putShort(1, (short) immediateSendData.data.value.length);
        byteBuffer.position(3);
        byteBuffer.put(immediateSendData.data.value);
        return new Data(byteBuffer.array());
    }

    /// 解码立即发送数据包
    public static ImmediateSendData decodeImmediateSend(final Data data) {
        if (data == null || data.value == null) {
            return null;
        }
        if (signalDataActionCode(data.value) != Configurations.signalCharacteristicActionWriteImmediate) {
            return null;
        }
        if (data.value.length < 3) {
            return null;
        }
        final Short immediateSendDataCount = int16(data.value, 1);
        if (immediateSendDataCount == null) {
            return null;
        }
        if (immediateSendDataCount == 0) {
            return new ImmediateSendData(new Data());
        }
        if (data.value.length != (3 + immediateSendDataCount.intValue())) {
            return null;
        }
        final Data immediateSendDataBytes = new Data(data.value).subdata(3);
        if (immediateSendDataBytes == null) {
            return null;
        }
        return new ImmediateSendData(immediateSendDataBytes);
    }

    /// 检测信号特征数据包类型
    public static SignalCharacteristicDataType detect(Data data) {
        switch (signalDataActionCode(data.value)) {
            case Configurations.signalCharacteristicActionWriteRSSI:
                return SignalCharacteristicDataType.rssi;
            case Configurations.signalCharacteristicActionWritePayload:
                return SignalCharacteristicDataType.payload;
            case Configurations.signalCharacteristicActionWritePayloadSharing:
                return SignalCharacteristicDataType.payloadSharing;
            case Configurations.signalCharacteristicActionWriteImmediate:
                return SignalCharacteristicDataType.immediateSend;
            default:
                return SignalCharacteristicDataType.unknown;
        }
    }

    private static byte signalDataActionCode(byte[] signalData) {
        if (signalData == null || signalData.length == 0) {
            return 0;
        }
        return signalData[0];
    }

    private static Short int16(byte[] data, int index) {
        if (index < data.length - 1) {
            final ByteBuffer byteBuffer = ByteBuffer.wrap(data);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            return byteBuffer.getShort(index);
        } else {
            return null;
        }
    }
}
