

package com.ABC.pioneer.sensor.ble;

import android.content.Context;

import com.ABC.pioneer.sensor.PayloadSupplier;
import com.ABC.pioneer.sensor.SensorDelegate;
import com.ABC.pioneer.sensor.datatype.BluetoothState;
import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.Proximity;
import com.ABC.pioneer.sensor.datatype.ProximityMeasurementUnit;
import com.ABC.pioneer.sensor.datatype.RSSI;
import com.ABC.pioneer.sensor.datatype.SensorState;
import com.ABC.pioneer.sensor.datatype.SensorType;
import com.ABC.pioneer.sensor.datatype.TargetIdentifier;
import com.ABC.pioneer.sensor.datatype.TimeInterval;

import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpecificBLESensor implements BLESensor, BLEDatabaseDelegate, BluetoothStateManagerDelegate {
    private final Queue<SensorDelegate> delegates = new ConcurrentLinkedQueue<>();
    private final Transmitter transmitter;
    private final Receiver receiver;
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();
    // 记录有效负载数据以启用重复数据删除
    private final Map<PayloadData, Date> didReadPayloadData = new ConcurrentHashMap<>();

    public SpecificBLESensor(Context context, PayloadSupplier payloadSupplier) {
        final BluetoothStateManager bluetoothStateManager = new SpecificBluetoothStateManager(context);
        final Database database = new SpecificDatabase();
        final Timer timer = new Timer(context);
        bluetoothStateManager.delegates.add(this);
        transmitter = new SpecificTransmitter(context, bluetoothStateManager, timer, payloadSupplier, database);
        receiver = new SpecificReceiver(context, bluetoothStateManager, timer, database, transmitter, payloadSupplier);
        database.add(this);
    }

    @Override
    public void add(SensorDelegate delegate) {
        delegates.add(delegate);
        transmitter.add(delegate);
        receiver.add(delegate);
    }

    @Override
    public void start() {
        transmitter.start();
        receiver.start();
    }

    @Override
    public void stop() {
        transmitter.stop();
        receiver.stop();
    }

    public boolean immediateSend(Data data, TargetIdentifier targetIdentifier) {
        return receiver.immediateSend(data, targetIdentifier);
    }

    public boolean immediateSendAll(Data data) {
        return receiver.immediateSendAll(data);
    }

    // BLEDatabaseDelegate

    @Override
    public void bleDatabaseDidCreate(final BLEDevice device) {
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                for (SensorDelegate delegate : delegates) {
                    delegate.sensor(SensorType.BLE, device.identifier);
                }
            }
        });
    }

    @Override
    public void bleDatabaseDidUpdate(final BLEDevice device, DeviceAttribute attribute) {
        switch (attribute) {
            case rssi: {
                final RSSI rssi = device.rssi();
                if (rssi == null) {
                    return;
                }
                final Proximity proximity = new Proximity(ProximityMeasurementUnit.RSSI, (double) rssi.value, device.calibration());
                operationQueue.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.BLE, proximity, device.identifier);
                        }
                    }
                });
                final PayloadData payloadData = device.payloadData();
                if (payloadData == null) {
                    return;
                }

                break;
            }
            case payloadData: {
                final PayloadData payloadData = device.payloadData();
                if (payloadData == null) {
                    return;
                }
                // 最近一次对有效负载进行重复数据删除
                if (Configurations.filterDuplicatePayloadData != TimeInterval.never) {
                    final long removePayloadDataBefore = new Date().getTime() - Configurations.filterDuplicatePayloadData.millis();
                    for (Map.Entry<PayloadData, Date> entry : didReadPayloadData.entrySet()) {
                        if (entry.getValue().getTime() < removePayloadDataBefore) {
                            didReadPayloadData.remove(entry.getKey());
                        }
                    }
                    final Date lastReportedAt = didReadPayloadData.get(payloadData);
                    if (lastReportedAt != null) {
                        return;
                    }
                    didReadPayloadData.put(payloadData, new Date());
                }
                // 通知delegates
                operationQueue.execute(new Runnable() {
                    @Override
                    public void run() {
                        for (SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.BLE, payloadData, device.identifier);
                        }
                    }
                });
                break;
            }
            default: {
            }
        }
    }

    @Override
    public void bleDatabaseDidDelete(BLEDevice device) {
    }

    // BluetoothStateManagerDelegate

    @Override
    public void bluetoothStateManager(BluetoothState didUpdateState) {
        SensorState sensorState = SensorState.off;
        if (didUpdateState == BluetoothState.poweredOn) {
            sensorState = SensorState.on;
        } else if (didUpdateState == BluetoothState.unsupported) {
            sensorState = SensorState.unavailable;
        }

    }
}
