
package com.ABC.pioneer.sensor.data;

import android.content.Context;

import com.ABC.pioneer.sensor.DefaultSensorDelegate;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.SensorType;
import com.ABC.pioneer.sensor.datatype.TargetIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// CSV联系人日志，用于事后分析和可视化
public class DetectionLog extends DefaultSensorDelegate {
    private final TextFile textFile;
    private final PayloadData payloadData;
    private final String deviceName = android.os.Build.MODEL;
    private final String deviceOS = Integer.toString(android.os.Build.VERSION.SDK_INT);
    private final Map<String, String> payloads = new ConcurrentHashMap<>();
    private final PayloadDataFormatter payloadDataFormatter;

    public DetectionLog(final Context context, final String filename, final PayloadData payloadData, PayloadDataFormatter payloadDataFormatter) {
        textFile = new TextFile(context, filename);
        this.payloadData = payloadData;
        this.payloadDataFormatter = payloadDataFormatter;
        write();
    }

    public DetectionLog(final Context context, final String filename, final PayloadData payloadData) {
        this(context, filename, payloadData, new ConcretePayloadDataFormatter());
    }

    private String csv(String value) {
        return TextFile.csv(value);
    }

    private void write() {
        final StringBuilder content = new StringBuilder();
        content.append(csv(deviceName));
        content.append(',');
        content.append("Android");
        content.append(',');
        content.append(csv(deviceOS));
        content.append(',');
        content.append(csv(payloadDataFormatter.shortFormat(payloadData)));
        final List<String> payloadList = new ArrayList<>(payloads.size());
        for (String payload : payloads.keySet()) {
            if (payload.equals(payloadDataFormatter.shortFormat(payloadData))) {
                continue;
            }
            payloadList.add(payload);
        }
        Collections.sort(payloadList);
        for (String payload : payloadList) {
            content.append(',');
            content.append(payload);
        }
        content.append("\n");
        textFile.overwrite(content.toString());
    }


    // MARK:- SensorDelegate

    @Override
    public void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget) {
        if (payloads.put(payloadDataFormatter.shortFormat(didRead), fromTarget.value) == null) {
            write();
        }
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {
        for (PayloadData payloadData : didShare) {
            if (payloads.put(payloadDataFormatter.shortFormat(payloadData), fromTarget.value) == null) {
                write();
            }
        }
    }
}
