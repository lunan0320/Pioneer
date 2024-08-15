
package com.ABC.pioneer.sensor.datatype;

import java.util.Date;

/// 原始位置数据，用于估算间接接触
public class Location {
    /// 测量值，例如 GPS坐标以逗号分隔的字符串格式表示纬度和经度
    public final LocationReference value;
    /// 在位置上花费的时间。
    public final Date start, end;

    public Location(LocationReference value, Date start, Date end) {
        this.value = value;
        this.start = start;
        this.end = end;
    }

    /// 获取邻近数据的纯文本描述
    public String description() {
        return (value == null ? "null" : value.description()) + ":[from=" + start + ",to=" + end + "]";
    }
}
