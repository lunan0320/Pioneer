//
//  BLESensor.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation
import CoreBluetooth

protocol BLESensor : Sensor {
/**
     BLE位置传感器基于CoreBluetooth，这是Pioneer系统用户交互基于的最主要的部分，几乎所有的工作都是围绕低功耗蓝牙传感器而展开的。
     使用该功能需要蓝牙权限并申请后台权限
     Requires : Signing & Capabilities : BackgroundModes : Uses Bluetooth LE accessories  = YES
     Requires : Signing & Capabilities : BackgroundModes : Acts as a Bluetooth LE accessory  = YES
     同时使用该功能需要隐私权限请求
     Requires : Info.plist : Privacy - Bluetooth Always Usage Description
     Requires : Info.plist : Privacy - Bluetooth Peripheral Usage Description
*/
    
}



// 定义BLE传感器配置数据
public struct BLESensorConfiguration {
    
    // MARK:-BLE服务和特性UUID及制造商ID
    //Pioneer系统的专用信标服务UUID，只要运行Pioneer系统的设备均会广播此服务，以被其他运行Pioneer系统的设备所识别
    public static let serviceUUID: CBUUID = CBUUID(string: "9ea51088-5add-438b-a269-dd6289844034")
    
    //用于控制外围设备和中心设备之间的连接的信令特性，主要用于识别设备上运行的系统
    //Android设备对应的信令特征
    public static let androidSignalCharacteristicUUID: CBUUID = CBUUID(string: "0ff27076-1fb2-4551-9670-7b468af8a9e7")
    //iOS设备对应的信令特征
    public static let iosSignalCharacteristicUUID: CBUUID = CBUUID(string: "64451d23-b364-4967-bb43-51b82a56f3a5")
    
    //用于控制设备之间Payload（读取）的信令特性，只有当Pioneer专用信标服务下有该特征时，Payload才允许被其他设备读取
    public static let payloadCharacteristicUUID: CBUUID = CBUUID(string: "2b5362a6-c995-4ab7-b744-cb366d4785bd")
    
    //Android设备需要使用制造商ID来存储伪设备地址以屏蔽某些Android设备专用ID的过快更新
    public static let BLEManufacturerId: UInt16 = UInt16(65530)


    
    // MARK:- BLE信号特征动作码
    //信令特征动作码用于写在交互数据前，作为交互信息的首部
    //写入有效负载信令特征动作码
    public static let signalCharacteristicActionForPayload: UInt8 = UInt8(1)
    //写入RSSI信令特征动作码
    public static let signalCharacteristicActionForRSSI: UInt8 = UInt8(2)
    //写入共享有效负载信令特征动作码
    public static let signalCharacteristicActionForPayloadSharing: UInt8 = UInt8(3)

    
    
    // MARK:- 可配置的应用程序功能
    
    public static let logLevel: SensorLoggerLevel = .debug
    
    
    ///设置为nil以禁用传感器，设置为distance(SpecificMobilitySensor.minimumResolution)启用传感器以给定的分辨率进行行程行性感应。
    ///另外，启用位置行程性传感器并结合iOS的屏幕唤醒可以使iOS设备之间直接交互，而非必需通过Android设备作为中继
    public static var TravelSensorEnabled: Distance? = nil
    
    

    ///Payload定期更新时间间隔。设置后立即生效，无需重新启动BLESensor，也可在BLESensor激活时应用。
    ///设置为.never以禁用此功能
    public static var TimeIntervalForPayloadDataUpdate: TimeInterval = TimeInterval.never
    

    ///Payload过滤时间间隔
    ///设置.never以禁用此功能
    ///设置时间间隔N以过滤在过去N秒内看到的重复的有效负载数据
    public static var TimeIntervalForFilterDuplicatePayloadData: TimeInterval = TimeInterval.never

    
    /// 删除一段时间未更新的外围设备记录。
    /// Pioneer旨在保持与所有外设的短暂连接，以便记录所有接近的外设的接近度和接触持续时间。因此Pioneer会频繁地进行数据采样，为了减少工作量需要定期删除长时间未更新的外围设备（这些设备可能已经超出范围或者由于其他原因停止了Pioneer工作）记录以终止数据采样，而不是一直尝试与这些设备进行连接。
    ///当然，不管是ios设备还是Android设备，基于蓝牙的底层协议，设备的蓝牙地址会定期轮换，当某设备地址超时后显然Pioneer不必再为此连接做努力并记录，因为此地址已失效也就是短期内基本不会再探测到该设备地址。因此TimeIntervalPeripheralClean的设置必须合理，建议将其设置在Android设备扫描周期（大约2分钟）和ios蓝牙地址轮换周期之间。
    public static var TimeIntervalPeripheralClean: TimeInterval = TimeInterval.minute * 2
    
    
    // MARK:- BLE事件计时
    //订阅通知之间的延迟
    public static var notificationDelay: DispatchTimeInterval = DispatchTimeInterval.seconds(2)
    //广播重置时间延迟
    public static var TimeIntervalForAdvertRestart: TimeInterval = TimeInterval.hour
    //并发连接的最大设备数目
    public static var maxConcurrentConnection: Int = 12
    //Android设备的广播刷新时间间隔
    public static var TimeIntervalForAndroidAdvertRefresh: TimeInterval = TimeInterval.minute * 15
    //连接超时时间间隔
    public static var toConnectionTimeout: TimeInterval = TimeInterval(10)
    
}



class SpecificBLESensor : NSObject, BLESensor, BLEDatabaseDelegate {
    private let logger = SpecificSensorLogger(subsystem: "Sensor", category: "BLE.SpecificBLESensor")
    private let sensorQueue = DispatchQueue(label: "Sensor.SpecificBLESensor.SensorQueue")
    private let delegateQueue = DispatchQueue(label: "Sensor.SpecificLESensor.DelegateQueue")
    private var delegates: [SensorDelegate] = []
    private let database: BLEDatabase
    private let transmitter: BLETransmitter
    private let receiver: BLEReceiver
    private var didReadPayloadData: [PayloadData:Date] = [:]

    init(_ payloadDataSupplier: PayloadDataSupplier) {
        database = SpecificBLEDatabase()
        transmitter = SpecificBLETransmitter(queue: sensorQueue, delegateQueue: delegateQueue, database: database, payloadDataSupplier: payloadDataSupplier)
        receiver = SpecificBLEReceiver(queue: sensorQueue, delegateQueue: delegateQueue, database: database, payloadDataSupplier: payloadDataSupplier)
        super.init()
        database.add(delegate: self)
    }
    
    func start() {
        logger.debug("start")
        receiver.start()
    }

    func stop() {
        logger.debug("stop")
        transmitter.stop()
        receiver.stop()
    }
    
    func add(delegate: SensorDelegate) {
        delegates.append(delegate)
        transmitter.add(delegate: delegate)
        receiver.add(delegate: delegate)
    }
    
    
    // MARK:- BLEDatabaseDelegate
    
    func bleDatabase(didCreate device: BLEDevice) {
        logger.debug("didDetect (device=\(device.identifier),payloadData=\(device.payloadData?.shortName ?? "nil"))")
        delegateQueue.async {
            self.delegates.forEach { $0.sensor(.BLE, didDetect: device.identifier) }
        }
    }
    
    func bleDatabase(didUpdate device: BLEDevice, attribute: BLEDeviceAttribute) {
        switch attribute {
        case .rssi:
            guard let rssi = device.rssi else {
                return
            }
            let proximity = Proximity(unit: .RSSI, value: Double(rssi), calibration: device.calibration)
            logger.debug("didMeasure (device=\(device.identifier),payloadData=\(device.payloadData?.shortName ?? "nil"),proximity=\(proximity.description))")
            delegateQueue.async {
                self.delegates.forEach { $0.sensor(.BLE, didMeasure: proximity, fromTarget: device.identifier) }
            }
            guard let payloadData = device.payloadData else {
                return
            }
            delegateQueue.async {
                self.delegates.forEach { $0.sensor(.BLE, didMeasure: proximity, fromTarget: device.identifier, withPayload: payloadData) }
            }
        case .payloadData:
            guard let payloadData = device.payloadData else {
                return
            }
            if BLESensorConfiguration.TimeIntervalForFilterDuplicatePayloadData != .never {
                let removePayloadDataBefore = Date() - BLESensorConfiguration.TimeIntervalForFilterDuplicatePayloadData
                let recentDidReadPayloadData = didReadPayloadData.filter({ $0.value >= removePayloadDataBefore })
                didReadPayloadData = recentDidReadPayloadData
                if let lastReportedAt = didReadPayloadData[payloadData] {
                    logger.debug("didRead, filtered duplicate (device=\(device.identifier),payloadData=\(payloadData.shortName),lastReportedAt=\(lastReportedAt.description))")
                    return
                }
                didReadPayloadData[payloadData] = Date()
            }
            logger.debug("didRead (device=\(device.identifier),payloadData=\(payloadData.shortName))")
            delegateQueue.async {
                self.delegates.forEach { $0.sensor(.BLE, didRead: payloadData, fromTarget: device.identifier) }
            }
        default:
            return
        }
    }
    
}

extension TargetIdentifier {
    init(peripheral: CBPeripheral) {
        self.init(peripheral.identifier.uuidString)
    }
    init(central: CBCentral) {
        self.init(central.identifier.uuidString)
    }
}

