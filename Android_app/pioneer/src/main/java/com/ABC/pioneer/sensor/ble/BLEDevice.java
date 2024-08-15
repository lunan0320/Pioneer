
package com.ABC.pioneer.sensor.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanRecord;

import com.ABC.pioneer.sensor.Device;
import com.ABC.pioneer.sensor.datatype.Calibration;
import com.ABC.pioneer.sensor.datatype.CalibrationMeasurementUnit;
import com.ABC.pioneer.sensor.datatype.Data;
import com.ABC.pioneer.sensor.datatype.PayloadData;
import com.ABC.pioneer.sensor.datatype.PseudoDeviceAddress;
import com.ABC.pioneer.sensor.datatype.RSSI;
import com.ABC.pioneer.sensor.datatype.TargetIdentifier;
import com.ABC.pioneer.sensor.datatype.TimeInterval;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Queue;

public class BLEDevice extends Device {
    // 伪设备地址，用于跟踪不断更改地址的Android设备。
    private PseudoDeviceAddress pseudoDeviceAddress = null;
    // 用于侦听属性更新事件的委托
    private final DeviceDelegate delegate;
    /// 用于与此设备进行交互的Android蓝牙设备对象。
    private BluetoothDevice peripheral = null;
    /// 蓝牙设备连接状态。
    private DeviceState state = DeviceState.disconnected;
    /// 设备操作系统，这对于为每个平台选择不同的交互过程是必需的。
    private DeviceOperatingSystem operatingSystem = DeviceOperatingSystem.unknown;
    //通过payloadCharacteristic读取从设备获取的有效载荷数据
    private PayloadData payloadData = null;
    private Date lastPayloadDataUpdate = null;
    /// 立即发送数据以发送到下一个设备
    private Data immediateSendData = null;
    /// 通过readRSSI或didDiscover进行最新的RSSI测量。
    private RSSI rssi = null;
    ///  在可用的情况下传输功率数据（仅由Android设备提供）
    private TxPower txPower = null;
    /// 如果设备是receive only设备?
    private boolean receiveOnly = false;
    /// 相应的ignore设置
    private TimeInterval ignoreForDuration = null;
    private Date ignoreUntil = null;
    private ScanRecord scanRecord = null;

    /// BLE characteristics特征
    private BluetoothGattCharacteristic signalCharacteristic = null;
    private BluetoothGattCharacteristic payloadCharacteristic = null;
    private BluetoothGattCharacteristic legacyPayloadCharacteristic = null;
    protected byte[] signalCharacteristicWriteValue = null;
    protected Queue<byte[]> signalCharacteristicWriteQueue = null;

    private BluetoothGattCharacteristic modelCharacteristic = null;
    private String model = null;
    private BluetoothGattCharacteristic deviceNameCharacteristic = null;
    private String deviceName = null;

    /// 跟踪连接时间戳
    private Date lastDiscoveredAt = null;
    private Date lastConnectedAt = null;

    /// 有效负载数据已与此对等方共享
    protected final List<PayloadData> payloadSharingData = new ArrayList<>();

    /// 跟踪写入时间戳
    private Date lastWritePayloadAt = null;
    private Date lastWriteRssiAt = null;
    private Date lastWritePayloadSharingAt = null;

    public TimeInterval timeIntervalSinceConnected() {
        if (state() != DeviceState.connected) {
            return TimeInterval.zero;
        }
        if (lastConnectedAt == null) {
            return TimeInterval.zero;
        }
        return new TimeInterval((new Date().getTime() - lastConnectedAt.getTime()) / 1000);
    }

    /// 自上次属性值更新以来的时间间隔，此间隔用于标识可能已过期并且应从数据库中删除的设备。
    public TimeInterval timeIntervalSinceLastUpdate() {
        if (lastUpdatedAt == null) {
            return TimeInterval.never;
        }
        return new TimeInterval((new Date().getTime() - lastUpdatedAt.getTime()) / 1000);
    }

    public String description() {
        return "BLEDevice[" +
                "id=" + identifier +
                ",os=" + operatingSystem +
                ",payload=" + payloadData() +
                (pseudoDeviceAddress() != null ? ",address=" + pseudoDeviceAddress() : "") +
                (deviceName() != null ? ",name=" + deviceName() : "") +
                (model() != null ? ",model=" + model() : "") +
                "]";
    }

    public BLEDevice(TargetIdentifier identifier, DeviceDelegate delegate) {
        super(identifier);
        this.delegate = delegate;
    }

    public PseudoDeviceAddress pseudoDeviceAddress() {
        return pseudoDeviceAddress;
    }

    public void pseudoDeviceAddress(PseudoDeviceAddress pseudoDeviceAddress) {
        if (this.pseudoDeviceAddress == null || !this.pseudoDeviceAddress.equals(pseudoDeviceAddress)) {
            this.pseudoDeviceAddress = pseudoDeviceAddress;
            lastUpdatedAt = new Date();
        }
    }

    //外围设备信息
    public BluetoothDevice peripheral() {
        return peripheral;
    }

    public void peripheral(BluetoothDevice peripheral) {
        if (this.peripheral != peripheral) {
            this.peripheral = peripheral;
            lastUpdatedAt = new Date();
        }
    }

    //设备状态
    public DeviceState state() {
        return state;
    }

    public void state(DeviceState state) {
        this.state = state;
        lastUpdatedAt = new Date();
        if (state == DeviceState.connected) {
            lastConnectedAt = lastUpdatedAt;
        }
        delegate.device(this, DeviceAttribute.state);
    }

    public DeviceOperatingSystem operatingSystem() {
        return operatingSystem;
    }

    public void operatingSystem(DeviceOperatingSystem operatingSystem) {
        lastUpdatedAt = new Date();
        // 设置忽略时间戳
        if (operatingSystem == DeviceOperatingSystem.ignore) {
            if (ignoreForDuration == null) {
                ignoreForDuration = TimeInterval.minute;
            } else if (ignoreForDuration.value < TimeInterval.minutes(3).value) {
                ignoreForDuration = new TimeInterval(Math.round(ignoreForDuration.value * 1.2));
            }
            ignoreUntil = new Date(lastUpdatedAt.getTime() + ignoreForDuration.millis());
        } else {
            ignoreUntil = null;
        }
        //如果已确认操作系统，则重置持续时间和请求计数的忽略
        if (operatingSystem == DeviceOperatingSystem.ios || operatingSystem == DeviceOperatingSystem.android) {
            ignoreForDuration = null;
        }
        //设置操作系统
        if (this.operatingSystem != operatingSystem) {
            this.operatingSystem = operatingSystem;
            delegate.device(this, DeviceAttribute.operatingSystem);
        }
    }

    /// 时间判断之后忽略该设备
    public boolean ignore() {
        if (ignoreUntil == null) {
            return false;
        }
        if (new Date().getTime() < ignoreUntil.getTime()) {
            return true;
        }
        return false;
    }

    public PayloadData payloadData() {
        return payloadData;
    }

    public void payloadData(PayloadData payloadData) {
        this.payloadData = payloadData;
        lastPayloadDataUpdate = new Date();
        lastUpdatedAt = lastPayloadDataUpdate;
        delegate.device(this, DeviceAttribute.payloadData);
    }

    public TimeInterval timeIntervalSinceLastPayloadDataUpdate() {
        if (lastPayloadDataUpdate == null) {
            return TimeInterval.never;
        }
        return new TimeInterval((new Date().getTime() - lastPayloadDataUpdate.getTime()) / 1000);
    }

    public void immediateSendData(Data immediateSendData) {
        this.immediateSendData = immediateSendData;
    }

    public Data immediateSendData() {
        return immediateSendData;
    }

    public RSSI rssi() {
        return rssi;
    }

    public void rssi(RSSI rssi) {
        this.rssi = rssi;
        lastUpdatedAt = new Date();
        delegate.device(this, DeviceAttribute.rssi);
    }

    public void legacyPayloadCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.legacyPayloadCharacteristic = characteristic;
    }

    public BluetoothGattCharacteristic legacyPayloadCharacteristic() {
        return legacyPayloadCharacteristic;
    }

    public TxPower txPower() {
        return txPower;
    }

    public void txPower(TxPower txPower) {
        this.txPower = txPower;
        lastUpdatedAt = new Date();
        delegate.device(this, DeviceAttribute.txPower);
    }

    public Calibration calibration() {
        if (txPower == null) {
            return null;
        }
        return new Calibration(CalibrationMeasurementUnit.BLETransmitPower, new Double(txPower.value));
    }

    public boolean receiveOnly() {
        return receiveOnly;
    }

    public void receiveOnly(boolean receiveOnly) {
        this.receiveOnly = receiveOnly;
        lastUpdatedAt = new Date();
    }

    public void invalidateCharacteristics() {
        signalCharacteristic = null;
        payloadCharacteristic = null;
        modelCharacteristic = null;
        deviceNameCharacteristic = null;
        legacyPayloadCharacteristic = null;
    }

    public BluetoothGattCharacteristic signalCharacteristic() {
        return signalCharacteristic;
    }

    public void signalCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.signalCharacteristic = characteristic;
        lastUpdatedAt = new Date();
    }

    public BluetoothGattCharacteristic payloadCharacteristic() {
        return payloadCharacteristic;
    }

    public void payloadCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.payloadCharacteristic = characteristic;
        lastUpdatedAt = new Date();
    }

    public boolean supportsModelCharacteristic() { return null != modelCharacteristic; }

    public BluetoothGattCharacteristic modelCharacteristic() { return modelCharacteristic; }

    public void modelCharacteristic(BluetoothGattCharacteristic modelCharacteristic) {
        this.modelCharacteristic = modelCharacteristic;
        lastUpdatedAt = new Date();
    }

    public boolean supportsDeviceNameCharacteristic() { return null != deviceNameCharacteristic; }

    public BluetoothGattCharacteristic deviceNameCharacteristic() { return deviceNameCharacteristic; }

    public void deviceNameCharacteristic(BluetoothGattCharacteristic deviceNameCharacteristic) {
        this.deviceNameCharacteristic = deviceNameCharacteristic;
        lastUpdatedAt = new Date();
    }

    public String deviceName() { return deviceName; }

    public void deviceName(String deviceName) {
        this.deviceName = deviceName;
        lastUpdatedAt = new Date();
    }

    public String model() { return model; }

    public void model(String model) {
        this.model = model;
        lastUpdatedAt = new Date();
    }

    public void registerDiscovery() {
        lastDiscoveredAt = new Date();
        lastUpdatedAt = lastDiscoveredAt;
    }

    public void registerWritePayload() {
        lastUpdatedAt = new Date();
        lastWritePayloadAt = lastUpdatedAt;
    }

    public TimeInterval timeIntervalSinceLastWritePayload() {
        if (lastWritePayloadAt == null) {
            return TimeInterval.never;
        }
        return new TimeInterval((new Date().getTime() - lastWritePayloadAt.getTime()) / 1000);
    }

    public void registerWriteRssi() {
        lastUpdatedAt = new Date();
        lastWriteRssiAt = lastUpdatedAt;
    }

    public TimeInterval timeIntervalSinceLastWriteRssi() {
        if (lastWriteRssiAt == null) {
            return TimeInterval.never;
        }
        return new TimeInterval((new Date().getTime() - lastWriteRssiAt.getTime()) / 1000);
    }

    public void registerWritePayloadSharing() {
        lastUpdatedAt = new Date();
        lastWritePayloadSharingAt = lastUpdatedAt;
    }

    public TimeInterval timeIntervalSinceLastWritePayloadSharing() {
        if (lastWritePayloadSharingAt == null) {
            return TimeInterval.never;
        }
        return new TimeInterval((new Date().getTime() - lastWritePayloadSharingAt.getTime()) / 1000);
    }

    public TimeInterval timeIntervalUntilIgnoreExpires() {
        if (ignoreUntil == null) {
            return TimeInterval.zero;
        }
        if (ignoreUntil.getTime() == Long.MAX_VALUE) {
            return TimeInterval.never;
        }
        return new TimeInterval((ignoreUntil.getTime() - new Date().getTime()) / 1000);
    }


    public boolean protocolIsPioneer() {
        return signalCharacteristic != null && payloadCharacteristic != null;
    }

    public void scanRecord(ScanRecord scanRecord) {
        this.scanRecord = scanRecord;
    }

    public ScanRecord scanRecord() {
        return scanRecord;
    }

    public boolean protocolIsLegacy() {
        return legacyPayloadCharacteristic != null && signalCharacteristic == null;
    }
    @Override
    public String toString() {
        return description();
    }
}
