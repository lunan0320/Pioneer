

package com.ABC.pioneer.sensor;

import android.content.Context;

import com.ABC.pioneer.sensor.ble.Configurations;
import com.ABC.pioneer.sensor.ble.SpecificBLESensor;
import com.ABC.pioneer.sensor.data.CalibrationLog;
import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.PayloadTimestamp;
import com.ABC.pioneer.sensor.datatype.TargetIdentifier;
import com.ABC.pioneer.sensor.motion.ConcreteInertiaSensor;

import java.util.ArrayList;
import java.util.List;

// 用来集成多种探测和追踪方法的传感器列表
public class SensorArray implements Sensor {
    private final Context context;
    private final List<Sensor> sensorArray = new ArrayList<>();

    private final PayloadData payloadData;
    public final static String deviceDescription = android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + ")";

    private final SpecificBLESensor specificBleSensor;

    public SensorArray(final Context context, PayloadSupplier payloadSupplier) {
        this.context = context;
        // 定义 sensor 列表
        specificBleSensor = new SpecificBLESensor(context, payloadSupplier);
        sensorArray.add(specificBleSensor);
        //惯性传感器配置用于自动RSSI距离校准数据捕获
        if (Configurations.inertiaSensorEnabled) {
            sensorArray.add(new ConcreteInertiaSensor(context));
            add(new CalibrationLog(context, "calibration.csv"));
        }
        payloadData = payloadSupplier.payload(new PayloadTimestamp(), null);
    }


    ///立即发送数据。
    public boolean immediateSend(Data data, TargetIdentifier targetIdentifier) {
        return specificBleSensor.immediateSend(data,targetIdentifier);
    }

    ///立即发送给所有人（已连接/最近/附近）
    public boolean immediateSendAll(Data data) {
        return specificBleSensor.immediateSendAll(data);
    }

    public final PayloadData payloadData() {
        return payloadData;
    }

    @Override
    public void add(final SensorDelegate delegate) {
        for (Sensor sensor : sensorArray) {
            sensor.add(delegate);
        }
    }

    @Override
    public void start() {
        for (Sensor sensor : sensorArray) {
            sensor.start();
        }
    }

    @Override
    public void stop() {
        for (Sensor sensor : sensorArray) {
            sensor.stop();
        }
    }
}
