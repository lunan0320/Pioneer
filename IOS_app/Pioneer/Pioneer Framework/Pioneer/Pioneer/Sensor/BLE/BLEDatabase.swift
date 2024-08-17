//
//  BLEDatabase.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation
import CoreBluetooth

///BLEDatabase数据库用于存储所有探测设备的注册表
protocol BLEDatabase {
    
    ///添加用于处理数据库事件的委托
    func add(delegate: BLEDatabaseDelegate)
    
    ///获取或创建设备
    func device(_ identifier: TargetIdentifier) -> BLEDevice
    func device(_ peripheral: CBPeripheral, delegate: CBPeripheralDelegate) -> BLEDevice
    func device(_ payload: PayloadData) -> BLEDevice
    
    ///判断某设备是否存在，以有效负载数据为基准
    func hasDevice(_ payload: PayloadData) -> Bool

    ///获取数据库中的全部设备
    func devices() -> [BLEDevice]
    
    /// 从数据库中删除设备
    func delete(_ identifier: TargetIdentifier)
}


/// 用于接收注册表创建/更新/删除事件的委托
protocol BLEDatabaseDelegate {
    
    func bleDatabase(didCreate device: BLEDevice)
    
    func bleDatabase(didUpdate device: BLEDevice, attribute: BLEDeviceAttribute)
    
    func bleDatabase(didDelete device: BLEDevice)
}

extension BLEDatabaseDelegate {
    func bleDatabase(didCreate device: BLEDevice) {}
    
    func bleDatabase(didUpdate device: BLEDevice, attribute: BLEDeviceAttribute) {}
    
    func bleDatabase(didDelete device: BLEDevice) {}
}

class SpecificBLEDatabase : NSObject, BLEDatabase, BLEDeviceDelegate {
    private let logger = SpecificSensorLogger(subsystem: "Sensor", category: "BLE.SpecificBLEDatabase")
    private var delegates: [BLEDatabaseDelegate] = []
    private var database: [TargetIdentifier : BLEDevice] = [:]
    private var queue = DispatchQueue(label: "Sensor.SpecificBLEDatabase")
    
    func add(delegate: BLEDatabaseDelegate) {
        delegates.append(delegate)
    }
    
    func devices() -> [BLEDevice] {
        return database.values.map { $0 }
    }
    
    func device(_ identifier: TargetIdentifier) -> BLEDevice {
        if database[identifier] == nil {
            let device = BLEDevice(identifier, delegate: self)
            database[identifier] = device
            queue.async {
                self.logger.debug("create (device=\(identifier))")
                self.delegates.forEach { $0.bleDatabase(didCreate: device) }
            }
        }
        let device = database[identifier]!
        return device
    }

    func device(_ peripheral: CBPeripheral, delegate: CBPeripheralDelegate) -> BLEDevice {
        let identifier = TargetIdentifier(peripheral: peripheral)
        if database[identifier] == nil {
            let device = BLEDevice(identifier, delegate: self)
            database[identifier] = device
            queue.async {
                self.logger.debug("create (device=\(identifier))")
                self.delegates.forEach { $0.bleDatabase(didCreate: device) }
            }
        }
        let device = database[identifier]!
        if device.peripheral != peripheral {
            device.peripheral = peripheral
            peripheral.delegate = delegate
        }
        return device
    }
    
    func device(_ payload: PayloadData) -> BLEDevice {
        if let device = database.values.filter({ $0.payloadData == payload }).first {
            return device
        }
        // 创建临时UUID以填充，taskRemoveDuplicatePeripherals函数将在与外围设备的直接连接已建立时删除此项
        let identifier = TargetIdentifier(UUID().uuidString)
        let placeholder = device(identifier)
        placeholder.payloadData = payload
        return placeholder
    }
    
    func hasDevice(_ payload: PayloadData) -> Bool {
        if database.values.filter({ $0.payloadData == payload }).first != nil {
            return true
        }
        return false
    }

    func delete(_ identifier: TargetIdentifier) {
        guard let device = database[identifier] else {
            return
        }
        database[identifier] = nil
        queue.async {
            self.logger.debug("delete (device=\(identifier))")
            self.delegates.forEach { $0.bleDatabase(didDelete: device) }
        }
    }
    
    // MARK:- BLEDeviceDelegate
    
    func device(_ device: BLEDevice, didUpdate attribute: BLEDeviceAttribute) {
        queue.async {
            self.logger.debug("update (device=\(device.identifier),attribute=\(attribute.rawValue))")
            self.delegates.forEach { $0.bleDatabase(didUpdate: device, attribute: attribute) }
        }
    }
}

// MARK:- BLEDatabase data
public class BLEDevice : Device {
    
    /// 上次从此设备唤醒的时间（仅限iOS）
    var lastNotifiedAt: Date = Date.distantPast

    /// 伪设备地址，用于跟踪不断更改设备标识符的设备，如三星A10、A20和Note8
    var pseudoDeviceAddress: BLEPseudoDeviceAddress? {
        didSet {
            lastUpdatedAt = Date()
        }}
    
    /// 用于侦听属性更新事件的委托。
    let delegate: BLEDeviceDelegate
    
    /// 用于与此设备交互的CoreBluetooth外围对象。
    var peripheral: CBPeripheral? {
        didSet {
            lastUpdatedAt = Date()
            delegate.device(self, didUpdate: .peripheral)
        }}
    
    /// 在设备之间发送信号的服务特性，例如保持唤醒
    var signalCharacteristic: CBCharacteristic? {
        didSet {
            if signalCharacteristic != nil {
                lastUpdatedAt = Date()
            }
            delegate.device(self, didUpdate: .signalCharacteristic)
        }}
    
    /// 读取有效负载数据的服务特性
    var payloadCharacteristic: CBCharacteristic? {
        didSet {
            if payloadCharacteristic != nil {
                lastUpdatedAt = Date()
            }
            delegate.device(self, didUpdate: .payloadCharacteristic)
        }}

    /// 设备操作系统，这是为每个平台选择不同交互程序所必需的。
    var operatingSystem: BLEDeviceOperatingSystem = .unknown {
        didSet {
            lastUpdatedAt = Date()
            delegate.device(self, didUpdate: .operatingSystem)
        }}
    
    /// 设备仅接收，这对于过滤有效负载共享数据是必需的
    var receiveOnly: Bool = false {
        didSet {
            lastUpdatedAt = Date()
        }}

    /// 通过payloadCharacteristic read从设备获取的有效负载数据
    var payloadData: PayloadData? {
        didSet {
            payloadDataLastUpdatedAt = Date()
            lastUpdatedAt = payloadDataLastUpdatedAt
            delegate.device(self, didUpdate: .payloadData)
        }}
    /// 有效负载数据上次更新时间戳，用于确定需要与对等方共享的内容。
    var payloadDataLastUpdatedAt: Date = Date.distantPast

    /// 已与此对等方共享的负载数据
    var payloadSharingData: [PayloadData] = []
    
    /// 由readRSSI或didDiscover进行的最新RSSI测量。
    public var rssi: BLE_RSSI? {
        didSet {
            lastUpdatedAt = Date()
            rssiLastUpdatedAt = lastUpdatedAt
            delegate.device(self, didUpdate: .rssi)
        }}
    
    /// RSSI最后一次更新的时间戳，用于跟踪上一次广播的时间，而不依赖于didDiscover
    var rssiLastUpdatedAt: Date = Date.distantPast
    
    /// 在可用的情况下传输功率数据（仅由Android设备提供）
    public var txPower: BLE_TxPower? {
        didSet {
            lastUpdatedAt = Date()
            delegate.device(self, didUpdate: .txPower)
        }}
    
    /// 发射功率作为校准数据
    var calibration: Calibration? { get {
        guard let txPower = txPower else {
            return nil
        }
        return Calibration(unit: .BLETransmitPower, value: Double(txPower))
    }}
    
    /// 在某时间戳发现的跟踪对象，由taskConnect用于在设备的并发连接容量不足时对连接进行优先级排序
    var lastDiscoveredAt: Date = Date.distantPast
    
    var lastConnectionInitiationAttempt: Date?
    
    
    var lastConnectRequestedAt: Date = Date.distantPast
    /// 上次连接请求时间戳
    var lastConnectedAt: Date? {
        didSet {
            lastDisconnectedAt = nil
            lastConnectionInitiationAttempt = nil
        }}
    
    var lastReadPayloadRequestedAt: Date = Date.distantPast
    
    var lastDisconnectedAt: Date? {
        didSet {
            lastConnectionInitiationAttempt = nil
        }
    }
    
    var lastAdvertAt: Date { get {
            max(createdAt, lastDiscoveredAt, payloadDataLastUpdatedAt, rssiLastUpdatedAt)
        }}
    
    
    var timeIntervalSinceCreated: TimeInterval { get {
            Date().timeIntervalSince(createdAt)
        }}
    
    /// 自上次属性值更新以来的时间间隔，用于标识可能已过期并应从数据库中删除的设备。
    var timeIntervalSinceLastUpdate: TimeInterval { get {
            Date().timeIntervalSince(lastUpdatedAt)
        }}
    
    /// 自上次有效负载数据更新以来的时间间隔，用于标识需要有效负载更新的设备。
    var timeIntervalSinceLastPayloadDataUpdate: TimeInterval { get {
            Date().timeIntervalSince(payloadDataLastUpdatedAt)
        }}
    
    /// 自上次检测到广播以来的时间间隔，用于检测并发连接配额并确定断开连接的优先级
    var timeIntervalSinceLastAdvert: TimeInterval { get {
        Date().timeIntervalSince(lastAdvertAt)
        }}
    /// 上次连接请求之间的时间间隔，用于优先断开连接
    var timeIntervalSinceLastConnectRequestedAt: TimeInterval { get {
        Date().timeIntervalSince(lastConnectRequestedAt)
        }}
    
    /// 最后一次连接和最后一个广播之间的时间间隔，用于估计连续跟踪的最后一个周期，以优先断开连接
    var timeIntervalSinceLastDisconnectedAt: TimeInterval { get {
        guard let lastDisconnectedAt = lastDisconnectedAt else {
            return Date().timeIntervalSince(createdAt)
        }
        return Date().timeIntervalSince(lastDisconnectedAt)
        }}
    
    var timeIntervalBetweenLastConnectedAndLastAdvert: TimeInterval { get {
        guard let lastConnectedAt = lastConnectedAt, lastAdvertAt > lastConnectedAt else {
            return TimeInterval(0)
        }
        return lastAdvertAt.timeIntervalSince(lastConnectedAt)
        }}
    
    public override var description: String { get {
        return "BLEDevice[id=\(identifier),os=\(operatingSystem.rawValue),payload=\(payloadData?.shortName ?? "nil"),address=\(pseudoDeviceAddress?.data.base64EncodedString() ?? "nil")]"
        }}
    
    init(_ identifier: TargetIdentifier, delegate: BLEDeviceDelegate) {
        self.delegate = delegate
        super.init(identifier);
    }
}

protocol BLEDeviceDelegate {
    func device(_ device: BLEDevice, didUpdate attribute: BLEDeviceAttribute)
}

enum BLEDeviceAttribute : String {
    case peripheral, signalCharacteristic, payloadCharacteristic, payloadSharingCharacteristic, operatingSystem, payloadData, rssi, txPower
}

enum BLEDeviceOperatingSystem : String {
    case android, ios, restored, unknown, shared
}


public typealias BLE_RSSI = Int

public typealias BLE_TxPower = Int

class BLEPseudoDeviceAddress {
    let address: Int64
    let data: Data
    var description: String { get {
        return "BLEPseudoDeviceAddress(address=\(address),data=\(data.base64EncodedString()))"
        }}
    
    init(value: Int64) {
        data = BLEPseudoDeviceAddress.encode(value)
        
        address = BLEPseudoDeviceAddress.decode(data)!
    }
    
    init?(data: Data) {
        guard data.count == 6, let value = BLEPseudoDeviceAddress.decode(data) else {
            return nil
        }
        address = value
        self.data = BLEPseudoDeviceAddress.encode(address)
    }
    
    convenience init?(fromAdvertisementData: [String: Any]) {
        guard let manufacturerData = fromAdvertisementData[CBAdvertisementDataManufacturerDataKey] as? Data else {
            return nil
        }
        guard let manufacturerId = manufacturerData.uint16(0) else {
            return nil
        }
        
        if manufacturerId == BLESensorConfiguration.BLEManufacturerId, manufacturerData.count == 8 {
            self.init(data: Data(manufacturerData.subdata(in: 2..<8)))
        }
        
        else {
            return nil
        }
    }

    private static func encode(_ value: Int64) -> Data {
        var data = Data()
        data.append(value)
        return Data(data.subdata(in: 2..<8))
    }

    private static func decode(_ data: Data) -> Int64? {
        var decoded = Data(repeating: 0, count: 2)
        decoded.append(data)
        return decoded.int64(0)
    }
}


