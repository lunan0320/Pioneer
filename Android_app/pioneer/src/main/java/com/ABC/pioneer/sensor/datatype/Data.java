

package com.ABC.pioneer.sensor.datatype;

import java.nio.charset.Charset;
import java.util.Arrays;

/// 原始字节数组数据
public class Data {
    private final static char[] hexChars = "0123456789ABCDEF".toCharArray();
    public byte[] value = null;

    public Data() {
        this(new byte[0]);
    }

    public Data(byte[] value) {
        this.value = value;
    }

    public Data(final Data data) {
        final byte[] value = new byte[data.value.length];
        System.arraycopy(data.value, 0, value, 0, data.value.length);
        this.value = value;
    }

    public Data(byte repeating, int count) {
        this.value = new byte[count];
        for (int i=count; i-->0;) {
            this.value[i] = repeating;
        }
    }

    public Data(String base64EncodedString) {
        this.value = Base64.decode(base64EncodedString);
    }

    public String base64EncodedString() {
        return Base64.encode(value);
    }

    public String hexEncodedString() {
        if (value == null) {
            return "";
        }
        final StringBuilder stringBuilder = new StringBuilder(value.length * 2);
        for (int i = 0; i < value.length; i++) {
            final int v = value[i] & 0xFF;
            stringBuilder.append(hexChars[v >>> 4]);
            stringBuilder.append(hexChars[v & 0x0F]);
        }
        return stringBuilder.toString();
    }

    public final static Data fromHexEncodedString(String hexEncodedString) {
        final int length = hexEncodedString.length();
        final byte[] value = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            value[i / 2] = (byte) ((Character.digit(hexEncodedString.charAt(i), 16) << 4) +
                    Character.digit(hexEncodedString.charAt(i+1), 16));
        }
        return new Data(value);
    }

    public String description() {
        return base64EncodedString();
    }

    /// 获取从偏移量到结束的子数据
    public Data subdata(int offset) {
        if (offset >=0 && offset < value.length) {
            final byte[] offsetValue = new byte[value.length - offset];
            System.arraycopy(value, offset, offsetValue, 0, offsetValue.length);
            return new Data(offsetValue);
        } else {
            return null;
        }
    }


    /// 获取偏移量到偏移量+长度的子数据
    public Data subdata(int offset, int length) {
        if (offset >= 0 && offset < value.length && offset + length <= value.length) {
            final byte[] offsetValue = new byte[length];
            System.arraycopy(value, offset, offsetValue, 0, length);
            return new Data(offsetValue);
        } else {
            return null;
        }
    }

    /// 将数据附加到此数据的末尾。
    public void append(Data data) {
        append(data.value);
    }

    private void append(byte[] data) {
        final byte[] concatenated = new byte[data.length + value.length];
        System.arraycopy(value, 0, concatenated, 0, value.length);
        System.arraycopy(data, 0, concatenated, value.length, data.length);
        value = concatenated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Data data = (Data) o;
        return Arrays.equals(value, data.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return hexEncodedString();
    }

    // 从内在类型到数据的转换

    public void append(final UInt8 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF)
        });
    }

    public UInt8 uint8(final int index) {
        if (index < 0 || index >= value.length) {
            return null;
        }
        return new UInt8(value[index] & 0xFF);
    }

    public void append(final Int8 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF)
        });
    }

    public Int8 int8(final int index) {
        if (index < 0 || index >= value.length) {
            return null;
        }
        return new Int8(value[index]);
    }

    public void append(final UInt16 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF),        // 最低位
                (byte) ((value.value >>> 8) & 0xFF) // 最高位
        });
    }

    public UInt16 uint16(final int index) {
        if (index < 0 || index + 1 >= value.length) {
            return null;
        }
        final int v =
                value[index] & 0xFF |
                ((value[index + 1] & 0xFF) << 8);
        return new UInt16(v);
    }


    public void append(final Int16 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF), // 最低位
                (byte) (value.value >> 8)    // 最高位
        });
    }

    public Int16 int16(final int index) {
        if (index < 0 || index + 1 >= value.length) {
            return null;
        }
        final int v =
                value[index] & 0xFF |
                ((value[index + 1]) << 8);
        return new Int16(v);
    }

    public void append(final UInt32 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF),        // 最低位
                (byte) ((value.value >>> 8) & 0xFF),
                (byte) ((value.value >>> 16) & 0xFF),
                (byte) ((value.value >>> 24) & 0xFF) // 最高位
        });
    }

    public UInt32 uint32(final int index) {
        if (index < 0 || index + 3 >= value.length) {
            return null;
        }
        final long v =
                (long) (value[index] & 0xFF) |
                ((long) (value[index + 1] & 0xFF) << 8) |
                ((long) (value[index + 2] & 0xFF) << 16) |
                ((long) (value[index + 3] & 0xFF) << 24);
        return new UInt32(v);
    }

    public void append(final Int32 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF),        // 最低位
                (byte) ((value.value >> 8) & 0xFF),
                (byte) ((value.value >> 16) & 0xFF),
                (byte) (value.value >> 24)          // 最高位
        });
    }

    public Int32 int32(final int index) {
        if (index < 0 || index + 3 >= value.length) {
            return null;
        }
        final int v =
                (value[index] & 0xFF) |
                ((value[index + 1] & 0xFF) << 8) |
                ((value[index + 2] & 0xFF) << 16) |
                ((value[index + 3]) << 24);
        return new Int32(v);
    }

    public void append(final UInt64 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF),        // 最低位
                (byte) ((value.value >>> 8) & 0xFF),
                (byte) ((value.value >>> 16) & 0xFF),
                (byte) ((value.value >>> 24) & 0xFF),
                (byte) ((value.value >>> 32) & 0xFF),
                (byte) ((value.value >>> 40) & 0xFF),
                (byte) ((value.value >>> 48) & 0xFF),
                (byte) ((value.value >>> 56) & 0xFF) // 最高位
        });
    }

    public UInt64 uint64(final int index) {
        if (index < 0 || index + 7 >= value.length) {
            return null;
        }
        final long v =
                (long) value[index] & 0xFF |
                ((long) (value[index + 1] & 0xFF) << 8) |
                ((long) (value[index + 2] & 0xFF) << 16) |
                ((long) (value[index + 3] & 0xFF) << 24) |
                ((long) (value[index + 4] & 0xFF) << 32) |
                ((long) (value[index + 5] & 0xFF) << 40) |
                ((long) (value[index + 6] & 0xFF) << 48) |
                ((long) (value[index + 7] & 0xFF) << 56);
        return new UInt64(v);
    }

    public void append(final Int64 value) {
        append(new byte[]{
                (byte) (value.value & 0xFF),        // 最低位
                (byte) ((value.value >> 8) & 0xFF),
                (byte) ((value.value >> 16) & 0xFF),
                (byte) ((value.value >> 24) & 0xFF),
                (byte) ((value.value >> 32) & 0xFF),
                (byte) ((value.value >> 40) & 0xFF),
                (byte) ((value.value >> 48) & 0xFF),
                (byte) ((value.value >> 56)) // 最高位
        });
    }

    public Int64 int64(final int index) {
        if (index < 0 || index + 7 >= value.length) {
            return null;
        }
        final long v =
                (long) value[index] & 0xFF |
                ((long) (value[index + 1] & 0xFF) << 8) |
                ((long) (value[index + 2] & 0xFF) << 16) |
                ((long) (value[index + 3] & 0xFF) << 24) |
                ((long) (value[index + 4] & 0xFF) << 32) |
                ((long) (value[index + 5] & 0xFF) << 40) |
                ((long) (value[index + 6] & 0xFF) << 48) |
                ((long) (value[index + 7]) << 56);
        return new Int64(v);
    }

    public void append(Float16 value) {
        append(value.bigEndian);
    }

    public Float16 float16(int index) {
        if (index < 0 || index + 1 >= value.length) {
            return null;
        }
        return new Float16(new Data(new byte[] {
                value[index], value[index + 1]
        }));
    }

    // 到（从）数据函数的字符串

    /// 字符串长度数据的编码选项作为前缀
    public enum StringLengthEncodingOption {
        UINT8, UINT16, UINT32, UINT64
    }

    /// 将字符串编码为数据，使用UInt8，...，64将长度作为前缀插入。 如果成功，则返回true，否则返回false。
    public boolean append(final String value) {
        return append(value, StringLengthEncodingOption.UINT8);
    }

    public boolean append(final String value, final StringLengthEncodingOption encoding) {
        if (value == null) {
            return false;
        }
        byte[] data = null;
        try {
            data = value.getBytes("UTF-8");
        } catch (Throwable e) {
            return false;
        }
        if (data == null) {
            return false;
        }
        switch (encoding) {
            case UINT8:
            if (!(data.length <= UInt8.max.value)) {
                return false;
            }
            append(new UInt8(data.length));
            break;
        case UINT16:
            if (!(data.length <= UInt16.max.value))  {
                return false;
            }
            append(new UInt16(data.length));
            break;
        case UINT32:
            if (!(data.length <= UInt32.max.value)) {
                return false;
            }
            append(new UInt32(data.length));
            break;
        case UINT64:
            if (!(data.length <= UInt64.max.value)) {
                return false;
            }
            append(new UInt64(data.length));
            break;
        }
        append(data);
        return true;
    }

    /// 数据字节数组中已解码的字符串和开始/结束索引
    public final static class DecodedString {
        public final String value;
        public final int start;
        public final int end;

        public DecodedString(final String value, final int start, final int end) {
            this.value = value;
            this.start = start;
            this.end = end;
        }
    }

    public DecodedString string(final int index) {
        return string(index, StringLengthEncodingOption.UINT8);
    }

    public DecodedString string(final int index, final StringLengthEncodingOption encoding) {
        long start = index;
        long end = index;
        switch (encoding) {
            case UINT8: {
                final UInt8 count = uint8(index);
                if (count == null) {
                    return null;
                }
                start = index + 1;
                end = start + count.value;
                break;
            }
            case UINT16: {
                final UInt16 count = uint16(index);
                if (count == null) {
                    return null;
                }
                start = index + 2;
                end = start + count.value;
                break;
            }
            case UINT32: {
                final UInt32 count = uint32(index);
                if (count == null) {
                    return null;
                }
                start = index + 4;
                end = start + count.value;
                break;
            }
            case UINT64: {
                final UInt64 count = uint64(index);
                if (count == null) {
                    return null;
                }
                start = index + 8;
                end = start + count.value;
                break;
            }
        }
        if (start > Integer.MAX_VALUE || end > Integer.MAX_VALUE) {
            return null;
        }
        if (start == index || start > value.length || end > value.length) {
            return null;
        }
        try {
            final String string = new String(value, (int) start, (int) (end - start), Charset.forName("UTF-8"));
            return new DecodedString(string, (int) start, (int) end);
        } catch (Throwable e) {
            return null;
        }
    }

}
