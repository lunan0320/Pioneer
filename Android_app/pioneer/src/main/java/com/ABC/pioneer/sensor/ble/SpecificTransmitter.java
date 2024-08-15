

package com.ABC.pioneer.sensor.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;

import com.ABC.pioneer.sensor.PayloadSupplier;
import com.ABC.pioneer.sensor.SensorDelegate;
import com.ABC.pioneer.sensor.datatype.BluetoothState;
import com.ABC.pioneer.sensor.datatype.Callback;
import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.datatype.ImmediateSendData;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.PayloadSharingData;
import com.ABC.pioneer.sensor.datatype.PayloadTimestamp;
import com.ABC.pioneer.sensor.datatype.PseudoDeviceAddress;
import com.ABC.pioneer.sensor.datatype.RSSI;
import com.ABC.pioneer.sensor.datatype.SensorType;
import com.ABC.pioneer.sensor.datatype.SignalCharacteristicData;
import com.ABC.pioneer.sensor.datatype.TargetIdentifier;
import com.ABC.pioneer.sensor.datatype.TimeInterval;
import com.ABC.pioneer.sensor.datatype.Triple;
import com.ABC.pioneer.sensor.payload.DigitalSignature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS;

public class SpecificTransmitter implements Transmitter, BluetoothStateManagerDelegate {
    private final static long advertOffDurationMillis = TimeInterval.seconds(4).millis();
    private final Context context;
    private final BluetoothStateManager bluetoothStateManager;
    private final PayloadSupplier payloadSupplier;
    private final Database database;
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();
    private final AtomicBoolean transmitterEnabled = new AtomicBoolean(false);


    // 仅由startAdvert和stopExistingGattServer引用
    private BluetoothGattServer bluetoothGattServer = null;

    /**
     * 启用蓝牙后，Transmitter会自动启动。
     */
    public SpecificTransmitter(Context context, BluetoothStateManager bluetoothStateManager, Timer timer, PayloadSupplier payloadSupplier, Database database) {
        this.context = context;
        this.bluetoothStateManager = bluetoothStateManager;
        this.payloadSupplier = payloadSupplier;
        this.database = database;
        bluetoothStateManager.delegates.add(this);
        bluetoothStateManager(bluetoothStateManager.state());
        timer.add(new AdvertLoopTask());
    }

    @Override
    public void add(SensorDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public void start() {
        if (transmitterEnabled.compareAndSet(false, true)) {
        } else {
        }
    }

    @Override
    public void stop() {
        if (transmitterEnabled.compareAndSet(true, false)) {
        } else {
        }
    }

    // 广播循环

    private enum AdvertLoopState {
        starting, started, stopping, stopped
    }

    /// 获取 Bluetooth LE advertiser
    private BluetoothLeAdvertiser bluetoothLeAdvertiser() {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            return null;
        }
        boolean supported = bluetoothAdapter.isMultipleAdvertisementSupported();
        try {
            final BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            if (bluetoothLeAdvertiser == null) {
                return null;
            }
            return bluetoothLeAdvertiser;
        } catch (Exception e) {
            // 将其记录下来，因为这将使我们能够识别具有预期API实现的手机（来自Android API源代码）
            return null;
        }
    }

    private class AdvertLoopTask implements TimerDelegate {
        private AdvertLoopState advertLoopState = AdvertLoopState.stopped;
        private long lastStateChangeAt = System.currentTimeMillis();
        private BluetoothGattServer bluetoothGattServer;
        private AdvertiseCallback advertiseCallback;

        private void state(final long now, AdvertLoopState state) {
            final long elapsed = now - lastStateChangeAt;
            this.advertLoopState = state;
            lastStateChangeAt = now;
        }

        private long timeSincelastStateChange(final long now) {
            return now - lastStateChangeAt;
        }

        @Override
        public void bleTimer(final long now) {
            if (!transmitterEnabled.get() || !isSupported() || bluetoothStateManager.state() == BluetoothState.poweredOff) {
                if (advertLoopState != AdvertLoopState.stopped) {
                    stopAdvert(bluetoothLeAdvertiser(), advertiseCallback, bluetoothGattServer, new Callback<Boolean>() {
                        @Override
                        public void accept(Boolean value) {
                            advertiseCallback = null;
                            bluetoothGattServer = null;
                            state(now, AdvertLoopState.stopped);
                        }
                    });
                }
                return;
            }
            switch (advertLoopState) {
                case stopped: {
                    if (bluetoothStateManager.state() == BluetoothState.poweredOn) {
                        final long period = timeSincelastStateChange(now);
                        if (period >= advertOffDurationMillis) {
                            final BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothLeAdvertiser();
                            if (bluetoothLeAdvertiser == null) {
                                return;
                            }
                            state(now, AdvertLoopState.starting);
                            startAdvert(bluetoothLeAdvertiser, new Callback<Triple<Boolean, AdvertiseCallback, BluetoothGattServer>>() {
                                @Override
                                public void accept(Triple<Boolean, AdvertiseCallback, BluetoothGattServer> value) {
                                    advertiseCallback = value.b;
                                    bluetoothGattServer = value.c;
                                    state(now, value.a ? AdvertLoopState.started : AdvertLoopState.stopped);
                                }
                            });
                        }
                    }
                    break;
                }
                case started: {
                    final long period = timeSincelastStateChange(now);
                    if (period >= Configurations.advertRefreshTimeInterval.millis()) {
                        final BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothLeAdvertiser();
                        if (bluetoothLeAdvertiser == null) {
                            return;
                        }
                        state(now, AdvertLoopState.stopping);
                        stopAdvert(bluetoothLeAdvertiser, advertiseCallback, bluetoothGattServer, new Callback<Boolean>() {
                            @Override
                            public void accept(Boolean value) {
                                advertiseCallback = null;
                                bluetoothGattServer = null;
                                state(now, AdvertLoopState.stopped);
                            }
                        });
                    }
                    break;
                }
            }
        }
    }

    // Start and stop advert

    private void startAdvert(final BluetoothLeAdvertiser bluetoothLeAdvertiser, final Callback<Triple<Boolean, AdvertiseCallback, BluetoothGattServer>> callback) {
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                boolean result = true;
                // 如果已经有代理参考，则停止现有广告。
                //  永远不会发生这种情况，因为只有AdvertLoopTask会调用startAdvert，
                //  并且仅应在先前调用stopAdvert之后再调用startAdvert。
                //  记录此条件以验证是否可能发生这种情况以便后期测试数据。
                if (bluetoothGattServer != null) {
                    try {
                        bluetoothGattServer.clearServices();
                        bluetoothGattServer.close();
                    } catch (Throwable e) {
                    }
                    bluetoothGattServer = null;
                }
                // 设定 new GATT server
                try {
                    bluetoothGattServer = startGattServer(context, payloadSupplier, database);
                } catch (Throwable e) {
                    result = false;
                }
                if (bluetoothGattServer == null) {
                    result = false;
                } else {
                    try {
                        setGattService(context, bluetoothGattServer);
                    } catch (Throwable e) {
                        try {
                            bluetoothGattServer.clearServices();
                            bluetoothGattServer.close();
                            bluetoothGattServer = null;
                        } catch (Throwable e2) {
                            bluetoothGattServer = null;
                        }
                        result = false;
                    }
                }
                if (!result) {
                    callback.accept(new Triple<Boolean, AdvertiseCallback, BluetoothGattServer>(false, null, null));
                    return;
                }
                try {
                    final BluetoothGattServer bluetoothGattServerConfirmed = bluetoothGattServer;
                    final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
                        @Override
                        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                            callback.accept(new Triple<Boolean, AdvertiseCallback, BluetoothGattServer>(true, this, bluetoothGattServerConfirmed));
                        }

                        @Override
                        public void onStartFailure(int errorCode) {
                            callback.accept(new Triple<Boolean, AdvertiseCallback, BluetoothGattServer>(false, this, bluetoothGattServerConfirmed));
                        }
                    };
                    startAdvertising(bluetoothLeAdvertiser, advertiseCallback);
                } catch (Throwable e) {
                    callback.accept(new Triple<Boolean, AdvertiseCallback, BluetoothGattServer>(false, null, null));
                }
            }
        });
    }

    private void stopAdvert(final BluetoothLeAdvertiser bluetoothLeAdvertiser, final AdvertiseCallback advertiseCallback, final BluetoothGattServer bluetoothGattServer, final Callback<Boolean> callback) {
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                boolean result = true;
                try {
                    if (bluetoothLeAdvertiser != null && advertiseCallback != null) {
                        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
                    }
                } catch (Throwable e) {
                    result = false;
                }
                try {
                    if (bluetoothGattServer != null) {
                        bluetoothGattServer.clearServices();
                        bluetoothGattServer.close();
                    }
                } catch (Throwable e) {
                    result = false;
                }
                if (result) {
                } else {
                }
                callback.accept(result);
            }
        });
    }


    @Override
    public PayloadData payloadData() {
        return payloadSupplier.payload(new PayloadTimestamp(new Date()), null);
    }

    @Override
    public boolean isSupported() {
        return bluetoothLeAdvertiser() != null;
    }

    @Override
    public void bluetoothStateManager(BluetoothState didUpdateState) {
    }

    private void startAdvertising(final BluetoothLeAdvertiser bluetoothLeAdvertiser, final AdvertiseCallback advertiseCallback) {
        final AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .build();

        final PseudoDeviceAddress pseudoDeviceAddress = new PseudoDeviceAddress();
        final AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(Configurations.serviceUUID))
                .addManufacturerData(Configurations.manufacturerIdForSensor, pseudoDeviceAddress.data)
                .build();
        bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback);
    }

    private static BluetoothGattServer startGattServer(final Context context, final PayloadSupplier payloadSupplier, final Database database) {
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return null;
        }
        // Data = rssi (4字节的int型) + payload (剩余的字节数)
        final AtomicReference<BluetoothGattServer> server = new AtomicReference<>(null);
        final BluetoothGattServerCallback callback = new BluetoothGattServerCallback() {
            private final Map<String, PayloadData> onCharacteristicReadPayloadData = new ConcurrentHashMap<>();
            private final Map<String, byte[]> onCharacteristicWriteSignalData = new ConcurrentHashMap<>();

            private PayloadData onCharacteristicReadPayloadData(BluetoothDevice bluetoothDevice) {
                final BLEDevice device = database.device(bluetoothDevice);
                final String key = bluetoothDevice.getAddress();
                if (onCharacteristicReadPayloadData.containsKey(key)) {
                    return onCharacteristicReadPayloadData.get(key);
                }
                final PayloadData payloadData = payloadSupplier.payload(new PayloadTimestamp(), device);
                onCharacteristicReadPayloadData.put(key, payloadData);
                return payloadData;
            }

            private byte[] onCharacteristicWriteSignalData(BluetoothDevice device, byte[] value) {
                final String key = device.getAddress();
                byte[] partialData = onCharacteristicWriteSignalData.get(key);
                if (partialData == null) {
                    partialData = new byte[0];
                }
                byte[] data = new byte[partialData.length + (value == null ? 0 : value.length)];
                System.arraycopy(partialData, 0, data, 0, partialData.length);
                if (value != null) {
                    System.arraycopy(value, 0, data, partialData.length, value.length);
                }
                onCharacteristicWriteSignalData.put(key, data);
                return data;
            }

            private void removeData(BluetoothDevice device) {
                final String deviceAddress = device.getAddress();
                for (String deviceRequestId : new ArrayList<>(onCharacteristicReadPayloadData.keySet())) {
                    if (deviceRequestId.startsWith(deviceAddress)) {
                        onCharacteristicReadPayloadData.remove(deviceRequestId);
                    }
                }
                for (String deviceRequestId : new ArrayList<>(onCharacteristicWriteSignalData.keySet())) {
                    if (deviceRequestId.startsWith(deviceAddress)) {
                        onCharacteristicWriteSignalData.remove(deviceRequestId);
                    }
                }
            }

            @Override
            public void onConnectionStateChange(BluetoothDevice bluetoothDevice, int status, int newState) {
                final BLEDevice device = database.device(bluetoothDevice);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    device.state(DeviceState.connected);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    device.state(DeviceState.disconnected);
                    removeData(bluetoothDevice);
                }
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                final BLEDevice targetDevice = database.device(device);
                final TargetIdentifier targetIdentifier = targetDevice.identifier;
                if (characteristic.getUuid() != Configurations.androidSignalCharacteristicUUID) {
                    if (responseNeeded) {
                        server.get().sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, offset, value);
                    }
                    return;
                }
                final Data data = new Data(onCharacteristicWriteSignalData(device, value));
                switch (SignalCharacteristicData.detect(data)) {
                    case rssi: {
                        final RSSI rssi = SignalCharacteristicData.decodeWriteRSSI(data);
                        if (rssi == null) {
                            break;
                        }
                        // 只有receive-only Android devices写RSSI
                        targetDevice.operatingSystem(DeviceOperatingSystem.android);
                        targetDevice.receiveOnly(true);
                        targetDevice.rssi(rssi);
                        break;
                    }
                    case payload: {
                        final PayloadData payloadData = SignalCharacteristicData.decodeWritePayload(data);
                        if (payloadData == null) {
                            // 零碎的有效载荷数据可能不完整
                            break;
                        }
                        // 只有receive-only Android devices写payload
                        targetDevice.operatingSystem(DeviceOperatingSystem.android);
                        targetDevice.receiveOnly(true);
                        targetDevice.payloadData(payloadData);
                        onCharacteristicWriteSignalData.remove(device.getAddress());
                        break;
                    }
                    case payloadSharing: {
                        final PayloadSharingData payloadSharingData = SignalCharacteristicData.decodeWritePayloadSharing(data);
                        if (payloadSharingData == null) {
                            // 零碎的有效载荷数据可能不完整
                            break;
                        }
                        final List<PayloadData> didSharePayloadData = payloadSupplier.payload(payloadSharingData.data);
                        for (SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.BLE, didSharePayloadData, targetIdentifier);
                        }
                        // 只有android设备才需要写payload sharing数据
                        targetDevice.operatingSystem(DeviceOperatingSystem.android);
                        targetDevice.rssi(payloadSharingData.rssi);
                        for (final PayloadData payloadData : didSharePayloadData) {
                            final BLEDevice sharedDevice = database.device(payloadData);
                            sharedDevice.operatingSystem(DeviceOperatingSystem.shared);
                            sharedDevice.rssi(payloadSharingData.rssi);
                        }
                        break;
                    }
                    case immediateSend: {
                        final ImmediateSendData immediateSendData = SignalCharacteristicData.decodeImmediateSend(data);
                        if (immediateSendData == null) {
                            // 零散的立即发送数据可能不完整
                            break;
                        }
                        for ( SensorDelegate delegate : delegates) {
                            delegate.sensor(SensorType.BLE, immediateSendData, targetIdentifier);
                        }
                        break;
                    }
                }
                if (responseNeeded) {
                    server.get().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                final BLEDevice targetDevice = database.device(device);
                if (characteristic.getUuid() == Configurations.payloadCharacteristicUUID) {
                    final PayloadData payloadData = onCharacteristicReadPayloadData(device);
                    if (offset > payloadData.value.length) {
                        server.get().sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset, null);
                    } else {
                        final byte[] value = Arrays.copyOfRange(payloadData.value, offset, payloadData.value.length);
                        server.get().sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                    }
                } else {
                    server.get().sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, null);
                }
            }
        };
        server.set(bluetoothManager.openGattServer(context, callback));
        return server.get();
    }

    private static void setGattService(final Context context, final BluetoothGattServer bluetoothGattServer) {
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return;
        }
        if (bluetoothGattServer == null) {
            return;
        }
        for (BluetoothDevice device : bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
            bluetoothGattServer.cancelConnection(device);
        }
        for (BluetoothDevice device : bluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER)) {
            bluetoothGattServer.cancelConnection(device);
        }
        bluetoothGattServer.clearServices();

        // 逻辑监测 -确保现在没有Gatt服务
        List<BluetoothGattService> services = bluetoothGattServer.getServices();
        for (BluetoothGattService svc : services) {
        }

        final BluetoothGattService service = new BluetoothGattService(Configurations.serviceUUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        final BluetoothGattCharacteristic signalCharacteristic = new BluetoothGattCharacteristic(
                Configurations.androidSignalCharacteristicUUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        signalCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        final BluetoothGattCharacteristic payloadCharacteristic = new BluetoothGattCharacteristic(
                Configurations.payloadCharacteristicUUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        service.addCharacteristic(signalCharacteristic);
        service.addCharacteristic(payloadCharacteristic);
        bluetoothGattServer.addService(service);

        // 逻辑监测 - 确保只有一个Pioneer设备
        services = bluetoothGattServer.getServices();
        int count = 0;
        for (BluetoothGattService svc : services) {
            if (svc.getUuid().equals(Configurations.serviceUUID)) {
                count++;
            }
        }
        if (count > 1) {
        }
    }

    private static String onConnectionStateChangeStatusToString(final int state) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                return "已连接";
            case BluetoothProfile.STATE_CONNECTING:
                return "连接中";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "断开连接中";
            case BluetoothProfile.STATE_DISCONNECTED:
                return "已断开连接";
            default:
                return "未知状态" + state;
        }
    }

    private static String onStartFailureErrorCodeToString(final int errorCode) {
        switch (errorCode) {
            case ADVERTISE_FAILED_DATA_TOO_LARGE:
                return "无法启动广播，数据量过大";
            case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                return "无法启动广播，没有可用的广播设置。";
            case ADVERTISE_FAILED_ALREADY_STARTED:
                return "无法启动广播，已有广播在运行";
            case ADVERTISE_FAILED_INTERNAL_ERROR:
                return "无法启动广播，内部错误";
            case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                return "不支持广播功能";
            default:
                return "未知错误" + errorCode;
        }
    }
    private static PayloadData add(PayloadData init_payloadData){
        final long start;
        final long end;
        PayloadData payloadData = init_payloadData;
        start = (new PayloadTimestamp()).value.getTime();
        end = start + 60*1000*6;
        java.text.SimpleDateFormat dateformat=new java.text.SimpleDateFormat("yyyy:MM:dd HH-mm-ss");
        dateformat.setTimeZone(TimeZone.getTimeZone("GMT+08"));
        String date_str0=dateformat.format(start);
        String date_str1=dateformat.format(end);
        Data term_of_validity = new Data();
        term_of_validity.append(date_str0);
        term_of_validity.append(date_str1);
        payloadData.append(term_of_validity);
        DigitalSignature assign = new DigitalSignature();
        payloadData.append(assign.genMAC());
        return payloadData;
    }
}
