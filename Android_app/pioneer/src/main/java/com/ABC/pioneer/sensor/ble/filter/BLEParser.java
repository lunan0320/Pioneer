
package com.ABC.pioneer.sensor.ble.filter;

import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.datatype.UInt8;

import java.util.ArrayList;
import java.util.List;

public class BLEParser {
    public static BLEScanResponseData parseScanResponse(byte[] raw, int offset) {
        // 将其分隔成多个数据段，直至二进制数据段结束
        return new BLEScanResponseData(raw.length - offset, extractSegments(raw, offset));
    }

    public static List<BLESeg> extractSegments(byte[] raw, int offset) {
        int position = offset;
        ArrayList<BLESeg> segments = new ArrayList<BLESeg>();
        int segmentLength;
        int segmentType;
        byte[] segmentData;
        Data rawData;
        int c;

        while (position < raw.length) {
            if ((position + 2) <= raw.length) {
                segmentLength = (byte)raw[position++] & 0xff;
                segmentType = (byte)raw[position++] & 0xff;
                // Note: 不知道的数据类型将记作 “unknown”
                // 检查报告后的长度与实际剩余数据长度
                if ((position + segmentLength - 1) <= raw.length) {
                    segmentData = subDataBigEndian(raw, position, segmentLength - 1); // Note: type IS INCLUDED in length
                    rawData = new Data(subDataBigEndian(raw, position - 2, segmentLength + 1));
                    position += segmentLength - 1;
                    segments.add(new BLESeg(BLESegType.typeFor(segmentType), segmentLength - 1, segmentData, rawData));
                } else {
                    // 数据的长度出现错误，直接跳转到数据结尾
                    position = raw.length;
                }
            } else {
                // 无效的数据段，直接跳转到数据结尾
                position = raw.length;
            }
        }

        return segments;
    }
    // 十六进制
    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    // 二进制转化为字符串
    public static String binaryString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
            result.append(" ");
        }
        return result.toString();
    }
    // 大端处理subData
    public static byte[] subDataBigEndian(byte[] raw, int offset, int length) {
        if (raw == null) {
            return new byte[]{};
        }
        if (offset < 0 || length <= 0) {
            return new byte[]{};
        }
        if (length + offset > raw.length) {
            return new byte[]{};
        }
        byte[] data = new byte[length];
        int position = offset;
        for (int c = 0;c < length;c++) {
            data[c] = raw[position++];
        }
        return data;
    }
    // 小端处理subData
    public static byte[] subDataLittleEndian(byte[] raw, int offset, int length) {
        if (raw == null) {
            return new byte[]{};
        }
        if (offset < 0 || length <= 0) {
            return new byte[]{};
        }
        if (length + offset > raw.length) {
            return new byte[]{};
        }
        byte[] data = new byte[length];
        int position = offset + length - 1;
        for (int c = 0;c < length;c++) {
            data[c] = raw[position--];
        }
        return data;
    }

    // 获取发射功率
    public static Integer extractTxPower(List<BLESeg> segments) {
        // 在列表中找到txPower代码段
        for (BLESeg segment : segments) {
            if (segment.type == BLESegType.txPowerLevel) {
                return (new UInt8((int)segment.data[0])).value;
            }
        }
        return null;
    }

    public static List<BLEManuData> extractManufacturerData(List<BLESeg> segments) {
        // 在列表中找到制造商数据代码段
        List<BLEManuData> manufacturerData = new ArrayList<>();
        for (BLESeg segment : segments) {
            if (segment.type == BLESegType.manufacturerData) {
                // 确保数据有足够的数据长度
                if (segment.data.length < 2) {
                    continue;
                    // 可能会产生和制造商数据相同的数据类型的有效段
                }
                // 创建制造商数据段
                int intValue = ((segment.data[1]&0xff) << 8) | (segment.data[0]&0xff);
                manufacturerData.add(new BLEManuData(intValue,subDataBigEndian(segment.data,2,segment.dataLength - 2), segment.raw));
            }
        }
        return manufacturerData;
    }

    public static List <BLEAppleManuSeg> extractAppleManufacturerSegments(List <BLEManuData> manuData) {
        final List<BLEAppleManuSeg> appleSegments = new ArrayList<>();
        for (BLEManuData manu : manuData) {
            int bytePos = 0;
            while (bytePos < manu.data.length) {
                final byte type = manu.data[bytePos];
                final int typeValue = type & 0xFF;
                // "01" 意味着legacy service UUID编码的时候没有长度数据
                if (type == 0x01) {
                    final int length = manu.data.length - bytePos - 1;
                    final Data data = new Data(subDataBigEndian(manu.data, bytePos + 1, length));
                    final Data raw = new Data(subDataBigEndian(manu.data, bytePos, manu.data.length - bytePos));
                    final BLEAppleManuSeg segment = new BLEAppleManuSeg(typeValue, length, data.value, raw);
                    appleSegments.add(segment);
                    bytePos = manu.data.length;
                }
                // 根据类型长度数据进行解析
                else {
                    final int length = manu.data[bytePos + 1] & 0xFF;
                    final int maxLength = (length < manu.data.length - bytePos - 2 ? length : manu.data.length - bytePos - 2);
                    final Data data = new Data(subDataBigEndian(manu.data, bytePos + 2, maxLength));
                    final Data raw = new Data(subDataBigEndian(manu.data, bytePos, maxLength + 2));
                    final BLEAppleManuSeg segment = new BLEAppleManuSeg(typeValue, length, data.value, raw);
                    appleSegments.add(segment);
                    bytePos += (maxLength + 2);
                }
            }
        }
        return appleSegments;
    }

    public static List<BLEServiceData> extractServiceUUID16Data(List<BLESeg> segments) {
        // 在列表中找到serviceData代码段
        List<BLEServiceData> serviceData = new ArrayList<>();
        for (BLESeg segment : segments) {
            if (segment.type == BLESegType.serviceUUID16Data) {
                // 确保数据有足够的数据长度
                if (segment.data.length < 2) {
                    continue;
                    //可能会产生和制造商数据相同的数据类型的有效段
                }
                // 创建服务数据段
                final byte[] serviceUUID16LittleEndian = subDataLittleEndian(segment.data,0,2);
                serviceData.add(new BLEServiceData(serviceUUID16LittleEndian, subDataBigEndian(segment.data,2,segment.dataLength - 2), segment.raw));
            }
        }
        return serviceData;
    }


}
