
package com.ABC.pioneer.sensor.datatype;

import java.util.Date;

/// 有效负载时间戳记通常应为Date，但将来可能会更改为UInt64以使用服务器同步的相对时间戳记。
public class PayloadTimestamp {
    public final Date value;

    public PayloadTimestamp(Date value) {
        this.value = value;
    }

    public PayloadTimestamp() {
        this(new Date());
    }
}
