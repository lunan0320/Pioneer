//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.ABC.pioneer.sensor;

import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.datatype.LegacyPayload;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.PayloadTimestamp;

import java.util.List;

public interface PayloadSupplier {
    LegacyPayload legacyPayload(PayloadTimestamp timestamp, Device device);

    // 获取给定时间戳的有效负载。 使用它与任何有效负载生成器集成
    PayloadData payload(PayloadTimestamp timestamp, Device device);

    /// 将原始数据解析为有效载荷
    List<PayloadData> payload(Data data);
}
