

package com.ABC.pioneer.sensor.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;

import com.ABC.pioneer.sensor.PayloadSupplier;
import com.ABC.pioneer.sensor.SensorDelegate;
import com.ABC.pioneer.sensor.analysis.Sample;
import com.ABC.pioneer.sensor.ble.filter.BLEDeviceFilter;
import com.ABC.pioneer.sensor.datatype.BluetoothState;
import com.ABC.pioneer.sensor.datatype.Callback;
import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.datatype.ImmediateSendData;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.PayloadSharingData;
import com.ABC.pioneer.sensor.datatype.RSSI;
import com.ABC.pioneer.sensor.datatype.SignalCharacteristicData;
import com.ABC.pioneer.sensor.datatype.SignalCharacteristicDataType;
import com.ABC.pioneer.sensor.datatype.TargetIdentifier;
import com.ABC.pioneer.sensor.datatype.TimeInterval;
import com.ABC.pioneer.sensor.payload.Crypto.SpecificUsePayloadSupplier;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpecificReceiver extends BluetoothGattCallback implements Receiver {
    // 扫描 开/关/过程 持续时间
    private final static long scanOnDurationMillis = TimeInterval.seconds(4).millis();
    private final static long scanRestDurationMillis = TimeInterval.seconds(1).millis();
    private final static long scanProcessDurationMillis = TimeInterval.seconds(60).millis();
    private final static long scanOffDurationMillis = TimeInterval.seconds(2).millis();
    private final static long timeToConnectDeviceLimitMillis = TimeInterval.seconds(12).millis();
    private final static Sample timeToConnectDevice = new Sample();
    private final static Sample timeToProcessDevice = new Sample();
    private final static int defaultMTU = 20;
    private final Context context;
    private final BluetoothStateManager bluetoothStateManager;
    private final Database database;
    private final Transmitter transmitter;
    private final PayloadSupplier payloadSupplier;
    private final BLEDeviceFilter deviceFilter;
    private final ExecutorService operationQueue = Executors.newSingleThreadExecutor();
    private final Queue<ScanResult> scanResults = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean receiverEnabled = new AtomicBoolean(false);

    private enum NextTask {
        nothing, readPayload, writePayload, writeRSSI, writePayloadSharing, immediateSend,
        readModel, readDeviceName
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult scanResult) {
            scanResults.add(scanResult);
            // 在数据库中创建或更新设备
            final BLEDevice device = database.device(scanResult);
            device.registerDiscovery();
            // 从扫描结果中读取RSSI
            device.rssi(new RSSI(scanResult.getRssi()));
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult scanResult : results) {
                onScanResult(0, scanResult);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    /**
     * 启用蓝牙后，Receiver会自动启动。
     */
    public SpecificReceiver(Context context, BluetoothStateManager bluetoothStateManager, Timer timer, Database database, Transmitter transmitter, PayloadSupplier payloadSupplier) {
        this.context = context;
        this.bluetoothStateManager = bluetoothStateManager;
        this.database = database;
        this.transmitter = transmitter;
        this.payloadSupplier = payloadSupplier;
        timer.add(new ScanLoopTask());

        if (Configurations.deviceFilterTrainingEnabled) {
            Configurations.deviceIntrospectionEnabled = true;
            Configurations.payloadDataUpdateTimeInterval = TimeInterval.minute;
            this.deviceFilter = new BLEDeviceFilter(context, "filter.csv");
        } else {
            this.deviceFilter = new BLEDeviceFilter();
        }
    }

    // BLEReceiver

    @Override
    public void add(SensorDelegate delegate) {
        delegates.add(delegate);
    }

    @Override
    public void start() {
        if (receiverEnabled.compareAndSet(false, true)) {
        } else {
        }
    }

    @Override
    public void stop() {
        if (receiverEnabled.compareAndSet(true, false)) {
        } else {
        }
    }

    @Override
    public boolean immediateSend(Data data, TargetIdentifier targetIdentifier) {
        BLEDevice device = database.device(targetIdentifier);
        if (null == device) {
            return false;
        }

        final Data dataToSend = SignalCharacteristicData.encodeImmediateSend(new ImmediateSendData(data));

        // 立即发送过程
        // 1. 设置设备的立即发送数据
        // 2. 启动与设备的连接
        // 3. onConnectionStateChange（）将触发服务和特征发现
        // 4. signalCharacteristic发现将触发nextTask（）
        // 5. 如果已为设备设置了立即发送数据，则nextTask（）将为.immediateSend
        // 6. 将调用writeSignalCharacteristic（）执行立即发送到signalCharacteristic
        // 7. 写入信号数据后将触发onCharacteristicWrite（）
        // 8. 写入完成后，设备的立即发送数据将设置为null
        // 9. 连接立即关闭
        device.immediateSendData(dataToSend);
        return taskConnectDevice(device);
    }

    @Override
    public boolean immediateSendAll(Data data) {
        // 编码数据
        final Data dataToSend = SignalCharacteristicData.encodeImmediateSend(new ImmediateSendData(data));
        // 按看到时间的降序排列（最新的优先）
        // 选择Targets
        SortedSet<BLEDevice> targets = new TreeSet<>(new DeviceUpdatedComparator());
        // 在最后一分钟获取看到的目标（通过广告获取RSSI）
        for (BLEDevice device : database.devices()) {
            if (!device.ignore() && device.signalCharacteristic() != null && device.timeIntervalSinceLastUpdate().value < 60) {
                targets.add(device);
            }
        }
        // 发送信息
        // 连接并立即发送给每个设备
        // NOTE: 这个单独的循环还没有排序交互。 一旦工作，重构就可以了。
        for (BLEDevice target : targets) {
            target.immediateSendData(dataToSend);
        }
        // 现在强制执行乱序连接（并因此立即发送作为下一个操作）
        for (BLEDevice target : targets) {
            taskConnectDevice(target);
        }
        return true;
    }

    // 设定startScan-wait-stopScan-processScanResults-wait-repeat的扫描循环

    private enum ScanLoopState {
        scanStarting, scanStarted, scanStopping, scanStopped, processing, processed
    }

    private class ScanLoopTask implements TimerDelegate {
        private ScanLoopState scanLoopState = ScanLoopState.processed;
        private long lastStateChangeAt = System.currentTimeMillis();

        private void state(final long now, ScanLoopState state) {
            final long elapsed = now - lastStateChangeAt;
            this.scanLoopState = state;
            lastStateChangeAt = now;
        }

        private long timeSincelastStateChange(final long now) {
            return now - lastStateChangeAt;
        }

        private BluetoothLeScanner bluetoothLeScanner() {
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                return null;
            }
            final BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bluetoothLeScanner == null) {
                return null;
            }
            return bluetoothLeScanner;
        }

        @Override
        public void bleTimer(final long now) {
            switch (scanLoopState) {
                case processed: {
                    if (receiverEnabled.get() && bluetoothStateManager.state() == BluetoothState.poweredOn) {
                        final long period = timeSincelastStateChange(now);
                        if (period >= scanOffDurationMillis) {
                            final BluetoothLeScanner bluetoothLeScanner = bluetoothLeScanner();
                            if (bluetoothLeScanner == null) {
                                return;
                            }
                            state(now, ScanLoopState.scanStarting);
                            startScan(bluetoothLeScanner, new Callback<Boolean>() {
                                @Override
                                public void accept(Boolean value) {
                                    state(now, value ? ScanLoopState.scanStarted : ScanLoopState.scanStopped);
                                }
                            });
                        }
                    }
                    break;
                }
                case scanStarted: {
                    final long period = timeSincelastStateChange(now);
                    if (period >= scanOnDurationMillis) {
                        final BluetoothLeScanner bluetoothLeScanner = bluetoothLeScanner();
                        if (bluetoothLeScanner == null) {
                            return;
                        }
                        state(now, ScanLoopState.scanStopping);
                        stopScan(bluetoothLeScanner, new Callback<Boolean>() {
                            @Override
                            public void accept(Boolean value) {
                                state(now, ScanLoopState.scanStopped);
                            }
                        });
                    }
                    break;
                }
                case scanStopped: {
                    if (bluetoothStateManager.state() == BluetoothState.poweredOn) {
                        final long period = timeSincelastStateChange(now);
                        if (period >= scanRestDurationMillis) {
                            state(now, ScanLoopState.processing);
                            processScanResults(new Callback<Boolean>() {
                                @Override
                                public void accept(Boolean value) {
                                    state(now, ScanLoopState.processed);
                                    if (!receiverEnabled.get()) {
                                    }
                                }
                            });
                        }
                    }
                    break;
                }
            }
        }
    }


    /// 获取BLE扫描器并开始扫描
    private void startScan(final BluetoothLeScanner bluetoothLeScanner, final Callback<Boolean> callback) {
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    scanForPeripherals(bluetoothLeScanner);
                    if (callback != null) {
                        callback.accept(true);
                    }
                } catch (Throwable e) {
                    if (callback != null) {
                        callback.accept(false);
                    }
                }
            }
        });
    }


    /// 扫描广告传感器服务的设备以及所有Apple设备，如下所示：
    // iOS后台广告不包含服务UUID。 传感器可能会花一些时间与没有重复运行传感器代码的Apple设备进行通信，
    // 但是没有可靠的方法对此进行过滤，因为仅由于暂时性问题而可能会缺少该服务。
    // 这将在taskConnect中处理。
    private void scanForPeripherals(final BluetoothLeScanner bluetoothLeScanner) {
        final List<ScanFilter> filter = new ArrayList<>(4);
    // 在iOS（后台）设备上扫描Pioneer协议服务
        filter.add(new ScanFilter.Builder().setManufacturerData(
                Configurations.manufacturerIdForApple, new byte[0], new byte[0]).build());
        // 在Android或iOS（前台）设备上扫描Pioneer协议服务
        filter.add(new ScanFilter.Builder().setServiceUuid(
                new ParcelUuid(Configurations.serviceUUID),
                new ParcelUuid(new UUID(0xFFFFFFFFFFFFFFFFL, 0)))
                .build());
        final ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build();
        bluetoothLeScanner.startScan(filter, settings, scanCallback);
    }

    private void processScanResults(final Callback<Boolean> callback) {
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    processScanResults();
                } catch (Throwable e) {
                    callback.accept(false);
                }
                callback.accept(true);
            }
        });
    }

    /// 获取BLE扫描器并停止扫描 Get BLE scanner and stop scan
    private void stopScan(final BluetoothLeScanner bluetoothLeScanner, final Callback<Boolean> callback) {
        operationQueue.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    bluetoothLeScanner.stopScan(scanCallback);
                } catch (Throwable e) {
                }
                try {
                    final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (bluetoothAdapter != null) {
                        bluetoothAdapter.cancelDiscovery();
                    }
                } catch (Throwable e) {
                }
                callback.accept(true);
            }
        });
    }

    // 处理扫描结果

    /// 处理扫描结果。
    private void processScanResults() {
        final long t0 = System.currentTimeMillis();
        // Identify devices discovered in last scan
        final List<BLEDevice> didDiscover = didDiscover();
        taskRemoveExpiredDevices();
        taskCorrectConnectionStatus();
        taskConnect(didDiscover);
        final long t1 = System.currentTimeMillis();
    }

    // MARK:- didDiscover

    /**
     * 将扫描结果处理为...
     * 1.从扫描结果中为新设备创建BLEDevice
     * 2.阅读RSSI
     * 3.尽可能确定操作系统
     */
    private List<BLEDevice> didDiscover() {
        // 取得可同时修改的扫描结果的当前副本
        final List<ScanResult> scanResultList = new ArrayList<>(scanResults.size());
        while (scanResults.size() > 0) {
            scanResultList.add(scanResults.poll());
        }

        // 处理扫描结果并返回在扫描结果中创建/更新的设备
        final Set<BLEDevice> deviceSet = new HashSet<>();
        final List<BLEDevice> devices = new ArrayList<>();
        for (ScanResult scanResult : scanResultList) {
            final BLEDevice device = database.device(scanResult);
            if (deviceSet.add(device)) {
                devices.add(device);
            }
            // 设置扫描记录
            device.scanRecord(scanResult.getScanRecord());
            // 设置发射功率
            if (device.scanRecord() != null) {
                int txPowerLevel = device.scanRecord().getTxPowerLevel();
                if (txPowerLevel != Integer.MIN_VALUE) {
                    device.txPower(new TxPower(txPowerLevel));
                }
            }
            // 尽可能从扫描记录中识别操作系统
            // - 发现了Sensor service + Manufacturer是苹果 -> iOS (前台)
            // - 发现了Sensor service + Manufacturer不是苹果 -> Android
            // - 未发现Sensor service + Manufacturer是苹果 -> iOS (后台)或者是IOS未广播Sensor服务
            // - 未发现Sensor service + Manufacturer不是苹果 -> Ignore
            final boolean hasSensorService = hasSensorService(scanResult);
            final boolean isAppleDevice = isAppleDevice(scanResult);
            if (hasSensorService && isAppleDevice) {
                // 绝对是前景模式下提供传感器服务的iOS设备
                device.operatingSystem(DeviceOperatingSystem.ios);
            } else if (hasSensorService) { // 隐含着!isAppleDevice
                if (device.operatingSystem() != DeviceOperatingSystem.android) {
                    device.operatingSystem(DeviceOperatingSystem.android_tbc);
                }
            } else if (isAppleDevice) { // 隐含着!hasSensorService
                final BLEDeviceFilter.MatchingPattern matchingPattern = deviceFilter.match(device);
                if (device.operatingSystem() != DeviceOperatingSystem.ios && matchingPattern != null) {
                    device.operatingSystem(DeviceOperatingSystem.ignore);
                }
                // 可能是一台iOS设备在后台模式下提供传感器服务
                // 无法确定是否在连接后再进行其他检查，因此仅在未知时才能设置操作系统以进行猜测。
                if (device.operatingSystem() == DeviceOperatingSystem.unknown) {
                    device.operatingSystem(DeviceOperatingSystem.ios_tbc);
                }
            }else {
                if (!(device.operatingSystem() == DeviceOperatingSystem.ios || device.operatingSystem() == DeviceOperatingSystem.android)) {
                    device.operatingSystem(DeviceOperatingSystem.ignore);
                }
            }
        }
        return devices;
    }

    /// 扫描结果是否包括用于传感器服务的广告？
    private static boolean hasSensorService(final ScanResult scanResult) {
        final ScanRecord scanRecord = scanResult.getScanRecord();
        if (scanRecord == null) {
            return false;
        }
        final List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
        if (serviceUuids == null || serviceUuids.size() == 0) {
            return false;
        }
        for (ParcelUuid serviceUuid : serviceUuids) {
            if (serviceUuid.getUuid().equals(Configurations.serviceUUID)) {
                return true;
            }
        }
        return false;
    }

    /// 扫描结果是否表明该设备是Apple制造的？
    private static boolean isAppleDevice(final ScanResult scanResult) {
        final ScanRecord scanRecord = scanResult.getScanRecord();
        if (scanRecord == null) {
            return false;
        }
        final byte[] data = scanRecord.getManufacturerSpecificData(Configurations.manufacturerIdForApple);
        return data != null;
    }

    // 信息清理

    /// 删除超过15分钟未更新的设备，
    // 因为UUID在超出范围20分钟后可能已更改，因此需要进行发现。
    private void taskRemoveExpiredDevices() {
        final List<BLEDevice> devicesToRemove = new ArrayList<>();
        for (BLEDevice device : database.devices()) {
            if (device.timeIntervalSinceLastUpdate().value > TimeInterval.minutes(15).value) {
                devicesToRemove.add(device);
            }
        }
        for (BLEDevice device : devicesToRemove) {
            database.delete(device);
        }
    }

    /// 连接保持的时间不应超过1分钟，可能没有收到onConnectionStateChange回调。
    private void taskCorrectConnectionStatus() {
        for (BLEDevice device : database.devices()) {
            if (device.state() == DeviceState.connected && device.timeIntervalSinceConnected().value > TimeInterval.minute.value) {
                device.state(DeviceState.disconnected);
            }
        }
    }


    // 连接任务

    private void taskConnect(final List<BLEDevice> discovered) {
        // 连接优先级在这里毫无意义
        // 因为三星A10和A20之类的设备会在每次扫描调用时更改mac地址，因此优化新设备的处理效率更高。
        final long timeStart = System.currentTimeMillis();
        int devicesProcessed = 0;
        for (BLEDevice device : discovered) {
            // 如果超过时间限制，则停止处理
            final long elapsedTime = System.currentTimeMillis() - timeStart;
            if (elapsedTime >= scanProcessDurationMillis) {
                break;
            }
            if (devicesProcessed > 0) {
                final long predictedElapsedTime = Math.round((elapsedTime / (double) devicesProcessed) * (devicesProcessed + 1));
                if (predictedElapsedTime > scanProcessDurationMillis) {
                    break;
                }
            }
            if (nextTaskForDevice(device) == NextTask.nothing) {
                continue;
            }
            taskConnectDevice(device);
            devicesProcessed++;
        }
    }

    private boolean taskConnectDevice(final BLEDevice device) {
        if (device.state() == DeviceState.connected) {
            return true;
        }
        // 连接
        final long timeConnect = System.currentTimeMillis();
        device.state(DeviceState.connecting);
        BluetoothGatt gatt = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // API 23及更高版本-仅强制低能耗
            gatt = device.peripheral().connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE);
        } else {
            // 支持回到API 21
            gatt = device.peripheral().connectGatt(context, false, this);
        }
        if (gatt == null) {
            device.state(DeviceState.disconnected);
            return false;
        }
        // 等待连接
        // 连接请求通常应导致.connected或.disconnected状态，该状态由回调函数onConnectionStateChange（）异步设置。
        // 但是，由于BLE问题，某些连接可能会无限期地陷入.connecting状态，因此从不调用回调函数，从而使设备处于不确定状态。
        // 这样，跟随循环将运行固定时间，以检查连接是否成功，否则中止连接以使设备处于一致的默认.disconnected状态。
        while (device.state() != DeviceState.connected && device.state() != DeviceState.disconnected && (System.currentTimeMillis() - timeConnect) < timeToConnectDeviceLimitMillis) {
            try {
                Thread.sleep(200);
            } catch (Throwable e) {
            }
        }
        if (device.state() != DeviceState.connected) {
            // 无法在时限内建立连接，假设连接失败并断开设备以使其处于一致的默认.disconnected状态
            try {
                gatt.close();
            } catch (Throwable e) {
            }
            return false;
        } else {
            // 连接成功，记下建立连接的时间，以通知timeToConnectDeviceLimitMillis的设置。
            // 先前的实现使用自适应算法来根据设备功能调整此参数，但是由于目标设备在确定连接时间中起着很大的作用，
            // 并且由于环境的原因而无法预测，因此认为这对于获得最小的性能而言太不可靠了。
            final long connectElapsed = System.currentTimeMillis() - timeConnect;
            timeToConnectDevice.add(connectElapsed);
        }
        // 等待断开连接
        // 此时设备已连接，并且所有实际工作都由该函数外部的回调方法异步执行。
        // 因此，此时唯一需要做的工作就是跟踪连接时间，以确保连接保持的时间不会太长
        // 如果连接保持的时间过长，则此函数将通过调用gatt.close（）断开设备
        // 以使其处于一致的默认.disconnected状态来强制断开连接。
        while (device.state() != DeviceState.disconnected && (System.currentTimeMillis() - timeConnect) < scanProcessDurationMillis) {
            try {
                Thread.sleep(500);
            } catch (Throwable e) {
            }
        }
        boolean success = true;
        // 超时连接（如果需要），并且始终将状态设置为断开
        if (device.state() != DeviceState.disconnected) {
          try {
                gatt.close();
            } catch (Throwable e) {
            }
            success = false;
        }
        // 最后始终将状态设置为.disconnected
        device.state(DeviceState.disconnected);
        final long timeDisconnect = System.currentTimeMillis();
        final long timeElapsed = (timeDisconnect - timeConnect);
        if (success) {
            timeToProcessDevice.add(timeElapsed);
        } else {
        }
        if (Configurations.deviceFilterTrainingEnabled) {
            deviceFilter.train(device, device.payloadCharacteristic() == null);
        }
        return success;
    }

    // BluetoothStateManagerDelegate

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        final BLEDevice device = database.

                device(gatt.getDevice());
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            device.state(DeviceState.connected);
            gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            gatt.close();
            device.state(DeviceState.disconnected);
            if (status != 0) {
            }
        } else {
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        final BLEDevice device = database.device(gatt.getDevice());

        // Sensor characteristics
        BluetoothGattService service = gatt.getService(Configurations.serviceUUID);
        if (service == null) {
            if (!Configurations.deviceFilterTrainingEnabled) {
                // 除非是经过确认的iOS或Android设备（之前已找到传感器服务），否则请暂时忽略该设备，
                // 因此请在有限的时间内忽略该设备，然后在不久的将来重试。
                if (!(device.operatingSystem() == DeviceOperatingSystem.ios || device.operatingSystem() == DeviceOperatingSystem.android)) {
                    device.operatingSystem(DeviceOperatingSystem.ignore);
                }
                gatt.disconnect();
                return;
            } else {
            }
        } else {
            device.invalidateCharacteristics();
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                // 确认具有signal characteristic的操作系统
                if (characteristic.getUuid().equals(Configurations.androidSignalCharacteristicUUID)) {
                    device.operatingSystem(DeviceOperatingSystem.android);
                    device.signalCharacteristic(characteristic);
                } else if (characteristic.getUuid().equals(Configurations.iosSignalCharacteristicUUID)) {
                    device.operatingSystem(DeviceOperatingSystem.ios);
                    device.signalCharacteristic(characteristic);
                } else if (characteristic.getUuid().equals(Configurations.payloadCharacteristicUUID)) {
                    device.payloadCharacteristic(characteristic);
                }
            }
            // 如果为空，则将旧有负载特征复制到负载特征
            if (device.payloadCharacteristic() == null && device.legacyPayloadCharacteristic() != null) {
                device.payloadCharacteristic(device.legacyPayloadCharacteristic());
            }
        }

        if (Configurations.deviceIntrospectionEnabled) {
            if (device.deviceName() == null) {
                device.deviceNameCharacteristic(serviceCharacteristic(gatt, Configurations.bluetoothGenericAccessServiceUUID, Configurations.bluetoothGenericAccessServiceDeviceNameCharacteristicUUID));
                if (device.supportsDeviceNameCharacteristic()) {
                }
            }
            if (device.model() == null) {
                device.modelCharacteristic(serviceCharacteristic(gatt, Configurations.bluetoothDeviceInformationServiceUUID, Configurations.bluetoothDeviceInformationServiceModelCharacteristicUUID));
                if (device.supportsModelCharacteristic()) {
                }
            }
        }

        nextTask(gatt);
    }

    /// 获取Bluetooth service characteristic，如果未找到，则为null。
    private BluetoothGattCharacteristic serviceCharacteristic(BluetoothGatt gatt, UUID service, UUID characteristic) {
        try {
            final BluetoothGattService bluetoothGattService = gatt.getService(service);
            if (bluetoothGattService == null) {
                return null;
            }
            final BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(characteristic);
            return bluetoothGattCharacteristic;
        } catch (Throwable e) {
            return null;
        }
    }

    /// 给定设备的当前状态，为其建立下一个任务。
    /// 这是必需的，因为所有BLE活动都是异步的，
    // 因此BLEDevice对象充当存储库，用于从异步调用中整理所有设备状态和信息更新。
    // 此功能检查设备状态和信息，以确定在连接设备时下一个要执行的任务（如果有）。
    // 请注意，必须在每个连接上的设备上执行服务和特征发现（不能缓存），
    // 因此，一旦与目标设备建立了连接，便应尽可能多地执行操作。
    private NextTask nextTaskForDevice(final BLEDevice device) {
        // 对于标记为.ignore的设备，没有任何任务
        if (device.ignore()) {
            return NextTask.nothing;
        }
        // 如果标记为忽略但忽略已过期，请更改为未知
        if (device.operatingSystem() == DeviceOperatingSystem.ignore) {
            device.operatingSystem(DeviceOperatingSystem.unknown);
        }
        // 对于标记为仅接收的设备，没有任务（无连接）
        if (device.receiveOnly()) {
            return NextTask.nothing;
        }
        if (Configurations.deviceIntrospectionEnabled && device.supportsModelCharacteristic() && device.model() == null) {
            return NextTask.readModel;
        }
        if (Configurations.deviceIntrospectionEnabled && device.supportsDeviceNameCharacteristic() && device.deviceName() == null) {
            return NextTask.readDeviceName;
        }
        // 通过读取触发特征发现的有效载荷来解析或确认操作系统，以确认操作系统
        if (device.operatingSystem() == DeviceOperatingSystem.unknown ||
                device.operatingSystem() == DeviceOperatingSystem.ios_tbc) {
            return NextTask.readPayload;
        }
        // 仅在发现服务和特征并且已确认操作系统的情况下才支持立即发送
        if (device.immediateSendData() != null) {
            return NextTask.immediateSend;
        }
        // 将payload作为最高优先级
        if (device.payloadData() == null) {
            return NextTask.readPayload;
        }
        // 根据需要获取payload更新
        if (device.timeIntervalSinceLastPayloadDataUpdate().value > Configurations.payloadDataUpdateTimeInterval.value) {
            return NextTask.readPayload;
        }
        if (device.protocolIsLegacy() && device.timeIntervalSinceLastPayloadDataUpdate().value > TimeInterval.minutes(5).value) {
            return NextTask.readPayload;
        }
        // 如果此设备无法发送数据，则写入有效负载，rssi和有效负载共享数据
        if (!transmitter.isSupported()) {
            // 将写有效载荷数据作为最高优先级
            if (device.timeIntervalSinceLastWritePayload().value > TimeInterval.minutes(5).value) {
                return NextTask.writePayload;
            }
            // 如果有要共享的数据，则将有效载荷共享数据写入iOS设备（在有效载荷共享和写入RSSI之间交替）
            final PayloadSharingData payloadSharingData = database.payloadSharingData(device);
            if (device.operatingSystem() == DeviceOperatingSystem.ios
                    && payloadSharingData.data.value.length > 0
                    && device.timeIntervalSinceLastWritePayloadSharing().value >= TimeInterval.seconds(15).value
                    && device.timeIntervalSinceLastWritePayloadSharing().value >= device.timeIntervalSinceLastWriteRssi().value) {
                return NextTask.writePayloadSharing;
            }
            //尽可能合理地写入RSSI（在写入RSSI和写入有效负载之间交替）
            if (device.rssi() != null
                    && device.timeIntervalSinceLastWriteRssi().value >= TimeInterval.seconds(15).value
                    && (device.timeIntervalSinceLastWritePayload().value < Configurations.payloadDataUpdateTimeInterval.value
                    || device.timeIntervalSinceLastWriteRssi().value >= device.timeIntervalSinceLastWritePayload().value)) {
                return NextTask.writeRSSI;
            }
            //根据需要写入有效负载更新
            if (device.timeIntervalSinceLastWritePayload().value > Configurations.payloadDataUpdateTimeInterval.value) {
                return NextTask.writePayload;
            }
        }
        // 将有效负载共享数据写入iOS
        if (device.operatingSystem() == DeviceOperatingSystem.ios && !device.protocolIsLegacy()) {
            // 如果有要共享的数据，则将有效负载共享数据写入iOS设备
            final PayloadSharingData payloadSharingData = database.payloadSharingData(device);
            if (device.operatingSystem() == DeviceOperatingSystem.ios
                    && payloadSharingData.data.value.length > 0
                    && device.timeIntervalSinceLastWritePayloadSharing().value >= TimeInterval.seconds(15).value) {
                return NextTask.writePayloadSharing;
            }
        }
        return NextTask.nothing;
    }

    // 给定打开的连接，为设备执行下一个任务。
    // 使用此函数定义用于在设备上实现任务的实际代码（例如readPayload）。
    //  任务的实际优先级在函数nextTaskForDevice（）中定义。
    //  有关其他设计详细信息，请参见函数nextTaskForDevice（）。
    private void nextTask(BluetoothGatt gatt) {
        final BLEDevice device = database.device(gatt.getDevice());
        final NextTask nextTask = nextTaskForDevice(device);
        switch (nextTask) {
            case readModel: {
                final BluetoothGattCharacteristic modelCharacteristic = device.modelCharacteristic();
                if (modelCharacteristic == null) {
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                if (!gatt.readCharacteristic(modelCharacteristic)) {
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                return; // => onCharacteristicRead | timeout
            }
            case readDeviceName: {
                final BluetoothGattCharacteristic deviceNameCharacteristic = device.deviceNameCharacteristic();
                if (deviceNameCharacteristic == null) {
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                if (!gatt.readCharacteristic(deviceNameCharacteristic)) {
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                return; // => onCharacteristicRead | timeout
            }
            case readPayload: {
                final BluetoothGattCharacteristic payloadCharacteristic = device.payloadCharacteristic();
                if (payloadCharacteristic == null) {
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                if (device.protocolIsLegacy()) {
                    gatt.requestMtu(512);
                    return; // => onCharacteristicRead | timeout
                }
                else if (!gatt.readCharacteristic(payloadCharacteristic)) {
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                // TODO incorporate Android non-auth security patch once license confirmed
                return; // => onCharacteristicRead | timeout
            }
            case writePayload: {
                final PayloadData payloadData = transmitter.payloadData();
                if (payloadData == null || payloadData.value == null || payloadData.value.length == 0) {
                    gatt.disconnect();
                    return; // => onConnectionStateChange
                }
                final Data data = SignalCharacteristicData.encodeWritePayload(transmitter.payloadData());
                writeSignalCharacteristic(gatt, NextTask.writePayload, data.value);
                return;
            }
            case writePayloadSharing: {
                final PayloadSharingData payloadSharingData = database.payloadSharingData(device);
                if (payloadSharingData == null) {
                    gatt.disconnect();
                    return;
                }
                final Data data = SignalCharacteristicData.encodeWritePayloadSharing(payloadSharingData);
                writeSignalCharacteristic(gatt, NextTask.writePayloadSharing, data.value);
                return;
            }
            case writeRSSI: {
                final BluetoothGattCharacteristic signalCharacteristic = device.signalCharacteristic();
                if (signalCharacteristic == null) {
                    gatt.disconnect();
                    return;
                }
                final RSSI rssi = device.rssi();
                if (rssi == null) {
                    gatt.disconnect();
                    return;
                }
                final Data data = SignalCharacteristicData.encodeWriteRssi(rssi);
                writeSignalCharacteristic(gatt, NextTask.writeRSSI, data.value);
                return;
            }
            case immediateSend: {
                final BluetoothGattCharacteristic signalCharacteristic = device.signalCharacteristic();
                if (signalCharacteristic == null) {
                    gatt.disconnect();
                    return;
                }
                final Data data = device.immediateSendData();
                if (data == null) {
                    gatt.disconnect();
                    return;
                }
                writeSignalCharacteristic(gatt, NextTask.immediateSend, data.value);
                device.immediateSendData(null);
                // 确保其被发送后删除数据
                return;
            }
        }
        gatt.disconnect();
    }

    private void writeSignalCharacteristic(BluetoothGatt gatt, NextTask task, byte[] data) {
        final BLEDevice device = database.device(gatt.getDevice());
        final BluetoothGattCharacteristic signalCharacteristic = device.signalCharacteristic();
        if (signalCharacteristic == null) {
            gatt.disconnect();
            return;
        }
        if (data == null || data.length == 0) {
            gatt.disconnect();
            return;
        }
        if (signalCharacteristic.getUuid().equals(Configurations.iosSignalCharacteristicUUID)) {
            device.signalCharacteristicWriteValue = data;
            device.signalCharacteristicWriteQueue = null;
            signalCharacteristic.setValue(data);
            signalCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            if (!gatt.writeCharacteristic(signalCharacteristic)) {
                gatt.disconnect();
            } else {
                // => onCharacteristicWrite
            }
            return;
        }
        if (signalCharacteristic.getUuid().equals(Configurations.androidSignalCharacteristicUUID)) {
            device.signalCharacteristicWriteValue = data;
            device.signalCharacteristicWriteQueue = fragmentDataByMtu(data);
            if (writeAndroidSignalCharacteristic(gatt) == WriteAndroidSignalCharacteristicResult.failed) {
                gatt.disconnect();
            } else {
                // => onCharacteristicWrite
            }
        }
    }

    private enum WriteAndroidSignalCharacteristicResult {
        moreToWrite, complete, failed
    }

    private WriteAndroidSignalCharacteristicResult writeAndroidSignalCharacteristic(BluetoothGatt gatt) {
        final BLEDevice device = database.device(gatt.getDevice());
        final BluetoothGattCharacteristic signalCharacteristic = device.signalCharacteristic();
        if (signalCharacteristic == null) {
            return WriteAndroidSignalCharacteristicResult.failed;
        }
        if (device.signalCharacteristicWriteQueue == null || device.signalCharacteristicWriteQueue.size() == 0) {
            return WriteAndroidSignalCharacteristicResult.complete;
        }
        final byte[] data = device.signalCharacteristicWriteQueue.poll();
        signalCharacteristic.setValue(data);
        signalCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        if (!gatt.writeCharacteristic(signalCharacteristic)) {
            return WriteAndroidSignalCharacteristicResult.failed;
        } else {
            return WriteAndroidSignalCharacteristicResult.moreToWrite;
        }
    }

    /// 将数据拆分为片段，其中每个片段的长度 <= mtu
    private Queue<byte[]> fragmentDataByMtu(byte[] data) {
        final Queue<byte[]> fragments = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < data.length; i += SpecificReceiver.defaultMTU) {
            final byte[] fragment = new byte[Math.min(SpecificReceiver.defaultMTU, data.length - i)];
            System.arraycopy(data, i, fragment, 0, fragment.length);
            fragments.add(fragment);
        }
        return fragments;
    }

 @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        final BLEDevice device = database.device(gatt.getDevice());
        final BluetoothGattCharacteristic characteristic = device.legacyPayloadCharacteristic();
        if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null && gatt.readCharacteristic(characteristic)) {
            return; // => onCharacteristicRead | timeout
        }
        gatt.disconnect();
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        final BLEDevice device = database.device(gatt.getDevice());
        final boolean success = (status == BluetoothGatt.GATT_SUCCESS);
        if (characteristic.getUuid().equals(Configurations.payloadCharacteristicUUID)) {
            final PayloadData payloadData = (characteristic.getValue() != null ? new PayloadData(characteristic.getValue()) : null);
            if (success) {
                if (payloadData != null) {
                    if(SpecificUsePayloadSupplier.checkPayloadtime(payloadData)) {
                        device.payloadData(payloadData);
                        // TODO incorporate Android non-auth security patch once license confirmed
                    }else{
                    }

                    // TODO incorporate Android non-auth security patch once license confirmed
                } else {
                }
            } else {
            }
        }else if (characteristic.getUuid().equals(Configurations.bluetoothDeviceInformationServiceModelCharacteristicUUID)) {
            final String model = characteristic.getStringValue(0);
            if (success) {
                if (model != null) {
                    device.model(model);
                } else {
                }
            } else {
            }
        } else if (characteristic.getUuid().equals(Configurations.bluetoothGenericAccessServiceDeviceNameCharacteristicUUID)) {
            final String deviceName = characteristic.getStringValue(0);
            if (success) {
                if (deviceName != null) {
                    device.deviceName(deviceName);
                } else {
                }
            } else {
            }
        } else {
        }
        nextTask(gatt);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        final BLEDevice device = database.device(gatt.getDevice());
        final boolean success = (status == BluetoothGatt.GATT_SUCCESS);
        // Pioneer信号特性写支持
        final BluetoothGattCharacteristic signalCharacteristic = device.signalCharacteristic();
        if (signalCharacteristic.getUuid().equals(Configurations.androidSignalCharacteristicUUID)) {
            if (success && writeAndroidSignalCharacteristic(gatt) == WriteAndroidSignalCharacteristicResult.moreToWrite) {
                return;
            }
        }
        final SignalCharacteristicDataType signalCharacteristicDataType = SignalCharacteristicData.detect(new Data(device.signalCharacteristicWriteValue));
        signalCharacteristic.setValue(new byte[0]);
        device.signalCharacteristicWriteValue = null;
        device.signalCharacteristicWriteQueue = null;
        switch (signalCharacteristicDataType) {
            case payload:
                if (success) {
                    device.registerWritePayload();
                } else {
                }
                break;
            case rssi:
                if (success) {
                    device.registerWriteRssi();
                } else {
                }
                break;
            case payloadSharing:
                if (success) {
                    device.registerWritePayloadSharing();
                } else {
                }
                break;
            case immediateSend:
                if (success) {
                    device.immediateSendData(null);
                } else {
                    // 无需重试立即发送
                    device.immediateSendData(null);
                }
                // 完成立即发送后立即关闭连接
                gatt.disconnect();
                // 不要执行任何其他任务
                return;
            default:
                break;
        }
        nextTask(gatt);
    }

    // 蓝牙编码转换器

    private static String bleStatus(final int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            return "GATT成功";
        } else {
            return "GATT失败";
        }
    }

    private static String bleState(final int state) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                return "已连接";
            case BluetoothProfile.STATE_DISCONNECTED:
                return "已断开";
            default:
                return "未知状态" + state;
        }
    }

    private static String onScanFailedErrorCodeToString(final int errorCode) {
        switch (errorCode) {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                return "无法启动扫描，BLE已设置完毕";
            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                return "无法启动扫描，因为无法注册应用程序。";
            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                return "无法启动扫描，内部错误";
            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                return "无法启动电源优化扫描，因为不支持此功能";
            default:
                return "未知错误代码" + errorCode;
        }
    }
    private static long ConvertToLong(String date, String format) {
        try {
            if (date != null&&format != null) {
                SimpleDateFormat sf = new SimpleDateFormat(format);
                return sf.parse(date).getTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
