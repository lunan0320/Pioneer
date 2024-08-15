

package com.ABC.pioneer.sensor.ble;

import java.util.Comparator;
//设备更新比较器
//这里我们按最后一次更新的时间对device进行排序，以便于后续的操作
public class DeviceUpdatedComparator implements Comparator<BLEDevice> {
    public int compare(BLEDevice a, BLEDevice b)
    {
        // 最后更新时间的降序（因此逻辑相反）
        long bt = b.lastUpdatedAt.getTime();
        long at = a.lastUpdatedAt.getTime();
        if (bt > at) {
            return 1;
        }
        if (bt < at) {
            return -1;
        }
        return 0;
    }
}
