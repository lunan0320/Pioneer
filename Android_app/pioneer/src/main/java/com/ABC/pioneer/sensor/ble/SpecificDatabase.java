

package com.ABC.pioneer.sensor.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;

import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.PayloadSharingData;
import com.ABC.pioneer.sensor.datatype.PseudoDeviceAddress;
import com.ABC.pioneer.sensor.datatype.RSSI;
import com.ABC.pioneer.sensor.datatype.TargetIdentifier;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpecificDatabase implements Database, DeviceDelegate {
    private final Queue<BLEDatabaseDelegate> delegates = new ConcurrentLinkedQueue<>();
    private final Map<TargetIdentifier, BLEDevice> database = new ConcurrentHashMap<>();
    private final ExecutorService queue = Executors.newSingleThreadExecutor();

    @Override
    public void add(final BLEDatabaseDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public BLEDevice device(final TargetIdentifier targetIdentifier) {
        return database.get(targetIdentifier);
    }

    @Override
    public BLEDevice device(final BluetoothDevice bluetoothDevice) {
        final TargetIdentifier identifier = new TargetIdentifier(bluetoothDevice);
        BLEDevice device = database.get(identifier);
        if (device == null) {
            final BLEDevice newDevice = new BLEDevice(identifier, this);
            device = newDevice;
            database.put(identifier, newDevice);
            queue.execute(new Runnable() {
                @Override
                public void run() {
                    for (BLEDatabaseDelegate delegate : delegates) {
                        delegate.bleDatabaseDidCreate(newDevice);
                    }
                }
            });
        }
        device.peripheral(bluetoothDevice);
        return device;
    }

    @Override
    public BLEDevice device(final ScanResult scanResult) {
        // 通过目标标识符获取设备
        final BluetoothDevice bluetoothDevice = scanResult.getDevice();
        final TargetIdentifier targetIdentifier = new TargetIdentifier(bluetoothDevice);
        final BLEDevice existingDevice = database.get(targetIdentifier);
        if (existingDevice != null) {
            return existingDevice;
        }
        // 通过伪设备地址获取设备信息
        final PseudoDeviceAddress pseudoDeviceAddress = pseudoDeviceAddress(scanResult);
        if (pseudoDeviceAddress != null) {
            // 重用现有的Android设备
            BLEDevice deviceWithSamePseudoDeviceAddress = null;
            for (final BLEDevice device : database.values()) {
                if (device.pseudoDeviceAddress() != null && device.pseudoDeviceAddress().equals(pseudoDeviceAddress)) {
                    deviceWithSamePseudoDeviceAddress = device;
                    break;
                }
            }
            if (deviceWithSamePseudoDeviceAddress != null) {
                database.put(targetIdentifier, deviceWithSamePseudoDeviceAddress);
                if (deviceWithSamePseudoDeviceAddress.peripheral() != bluetoothDevice) {
                    deviceWithSamePseudoDeviceAddress.peripheral(bluetoothDevice);
                }
                if (deviceWithSamePseudoDeviceAddress.operatingSystem() != DeviceOperatingSystem.android) {
                    deviceWithSamePseudoDeviceAddress.operatingSystem(DeviceOperatingSystem.android);
                }
                return deviceWithSamePseudoDeviceAddress;
            }
            // 创建新的Android设备
            else {
                final BLEDevice newDevice = device(bluetoothDevice);
                newDevice.pseudoDeviceAddress(pseudoDeviceAddress);
                newDevice.operatingSystem(DeviceOperatingSystem.android);
                return newDevice;
            }
        }
        // 创建新的设备
        return device(bluetoothDevice);
    }

    /// 获取Android设备的伪设备地址
    private PseudoDeviceAddress pseudoDeviceAddress(final ScanResult scanResult) {
        final ScanRecord scanRecord = scanResult.getScanRecord();
        if (scanRecord == null) {
            return null;
        }
        // Pioneer伪设备地址
        if (scanRecord.getManufacturerSpecificData(Configurations.manufacturerIdForSensor) != null) {
            final byte[] data = scanRecord.getManufacturerSpecificData(Configurations.manufacturerIdForSensor);
            if (data != null && data.length == 6) {
                return new PseudoDeviceAddress(data);
            }
        }
        // 未找到
        return null;
    }

    @Override
    public BLEDevice device(PayloadData payloadData) {
        BLEDevice device = null;
        for (BLEDevice candidate : database.values()) {
            if (payloadData.equals(candidate.payloadData())) {
                device = candidate;
                break;
            }
        }
        if (device == null) {
            final TargetIdentifier identifier = new TargetIdentifier();
            final BLEDevice newDevice = new BLEDevice(identifier, this);
            device = newDevice;
            database.put(identifier, newDevice);
            queue.execute(new Runnable() {
                @Override
                public void run() {
                    for (BLEDatabaseDelegate delegate : delegates) {
                        delegate.bleDatabaseDidCreate(newDevice);
                    }
                }
            });
        }
        device.payloadData(payloadData);
        return device;
    }

    @Override
    public List<BLEDevice> devices() {
        return new ArrayList<>(database.values());
    }

    @Override
    public void delete(final BLEDevice device) {
        if (device == null) {
            return;
        }
        final List<TargetIdentifier> identifiers = new ArrayList<>();
        for (final Map.Entry<TargetIdentifier,BLEDevice> entry : database.entrySet()) {
            if (entry.getValue() == device) {
                identifiers.add(entry.getKey());
            }
        }
        if (identifiers.isEmpty()) {
            return;
        }
        for (final TargetIdentifier identifier : identifiers) {
            database.remove(identifier);
        }
        queue.execute(new Runnable() {
            @Override
            public void run() {
            for (final BLEDatabaseDelegate delegate : delegates) {
                delegate.bleDatabaseDidDelete(device);
            }
            }
        });
    }

    @Override
    public PayloadSharingData payloadSharingData(final BLEDevice peer) {
        final RSSI rssi = peer.rssi();
        if (rssi == null) {
            return new PayloadSharingData(new RSSI(127), new Data(new byte[0]));
        }
        // 获取此设备最近查看过的其他设备
        final List<BLEDevice> unknownDevices = new ArrayList<>();
        final List<BLEDevice> knownDevices = new ArrayList<>();
        for (BLEDevice device : database.values()) {
            // 最近看过的设备信息
            if (device.timeIntervalSinceLastUpdate().value >= Configurations.payloadSharingExpiryTimeInterval.value) {
                continue;
            }
            // 设备含有payload数据
            if (device.payloadData() == null) {
                continue;
            }
            // 设备是IOS设备或者是receive-only（J16）设备
            if (!(device.operatingSystem() == DeviceOperatingSystem.ios || device.receiveOnly())) {
                continue;
            }
            // 设备是Pioneer设备
            if (device.signalCharacteristic() == null) {
                continue;
            }
            // payload数据不是连接方的数据
            if (peer.payloadData() != null && (Arrays.equals(device.payloadData().value, peer.payloadData().value))) {
                continue;
            }
            // 对于连接方来说，payload是新的payload
            if (peer.payloadSharingData.contains(device.payloadData())) {
                knownDevices.add(device);
            } else {
                unknownDevices.add(device);
            }
        }
        // 最近看到的未知设备优先
        final List<BLEDevice> devices = new ArrayList<>();
        Collections.sort(unknownDevices, new Comparator<BLEDevice>() {
            @Override
            public int compare(BLEDevice d0, BLEDevice d1) {
                return Long.compare(d1.lastUpdatedAt.getTime(), d0.lastUpdatedAt.getTime());
            }
        });
        Collections.sort(knownDevices, new Comparator<BLEDevice>() {
            @Override
            public int compare(BLEDevice d0, BLEDevice d1) {
                return Long.compare(d1.lastUpdatedAt.getTime(), d0.lastUpdatedAt.getTime());
            }
        });
        devices.addAll(unknownDevices);
        devices.addAll(knownDevices);
        if (devices.size() == 0) {
            return new PayloadSharingData(new RSSI(127), new Data(new byte[0]));
        }
        // 限制共享量，以避免通过BLE传输过多的数据
        // 根据规范限制为512个字节，响应为510个字节，iOS需要响应
        final Set<PayloadData> sharedPayloads = new HashSet<>(devices.size());
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (BLEDevice device : devices) {
            final PayloadData payloadData = device.payloadData();
            if (payloadData == null) {
                continue;
            }
            // 消除重复（当同一设备更改地址但旧版本尚未过期时，会发生这种情况）
            if (sharedPayloads.contains(payloadData)) {
                continue;
            }
            // 通过BLE传输限制限制有效负载共享
            if (payloadData.value.length + byteArrayOutputStream.toByteArray().length > 510) {
                break;
            }
            try {
                byteArrayOutputStream.write(payloadData.value);
                peer.payloadSharingData.add(payloadData);
                sharedPayloads.add(payloadData);
            } catch (Throwable e) {
            }
        }
        final Data data = new Data(byteArrayOutputStream.toByteArray());
        return new PayloadSharingData(rssi, data);
    }

    // BLEDeviceDelegate

    @Override
    public void device(final BLEDevice device, final DeviceAttribute didUpdate) {
        queue.execute(new Runnable() {
            @Override
            public void run() {
                for (BLEDatabaseDelegate delegate : delegates) {
                    delegate.bleDatabaseDidUpdate(device, didUpdate);
                }
            }
        });
    }
}
