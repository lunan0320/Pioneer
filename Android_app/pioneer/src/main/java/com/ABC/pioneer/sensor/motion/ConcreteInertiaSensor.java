

package com.ABC.pioneer.sensor.motion;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.ABC.pioneer.sensor.SensorDelegate;
import com.ABC.pioneer.sensor.datatype.InertiaLocationReference;
import com.ABC.pioneer.sensor.datatype.Location;

import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConcreteInertiaSensor implements InertiaSensor {
    private final Queue<SensorDelegate> delegates = new ConcurrentLinkedQueue<>();
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();
    private final Context context;
    private final SensorManager sensorManager;
    private final Sensor hardwareSensor;
    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
                return;
            }
            try {
                final Date timestamp = new Date();
                final double x = event.values[0];
                final double y = event.values[1];
                final double z = event.values[2];
                final InertiaLocationReference inertiaLocationReference = new InertiaLocationReference(x, y, z);
                final Location didVisit = new Location(inertiaLocationReference, timestamp, timestamp);

            } catch (Throwable e) {
                return;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public ConcreteInertiaSensor(final Context context) {
        this.context = context;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.hardwareSensor = (sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
    }

    @Override
    public void add(SensorDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public void start() {
        // 获取传感器管理器
        if (sensorManager == null) {
            return;
        }
        // 获取硬件传感器
        if (hardwareSensor == null) {
            return;
        }
        // 注册监听器
        sensorManager.unregisterListener(sensorEventListener);
        sensorManager.registerListener(sensorEventListener, hardwareSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void stop() {
        if (sensorManager == null) {
            return;
        }
        // 取消注册监听器
        sensorManager.unregisterListener(sensorEventListener);
    }
}
