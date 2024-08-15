
package com.ABC.pioneer.sensor.data;

import android.content.Context;

import com.ABC.pioneer.sensor.DefaultSensorDelegate;

import java.text.SimpleDateFormat;
import java.util.Date;

// 用于事后分析和可视化的CSV联系日志
public class CalibrationLog extends DefaultSensorDelegate {
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final TextFile textFile;

    public CalibrationLog(final Context context, final String filename) {
        textFile = new TextFile(context, filename);
        if (textFile.empty()) {
            textFile.write("time,payload,rssi,x,y,z");
        }
    }

    private static String timestamp() {
        return dateFormatter.format(new Date());
    }

    private static String csv(String value) {
        return TextFile.csv(value);
    }

}
