//
//  SensorArray.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import UIKit
import Foundation


///用于组合多种检测和跟踪方法的传感器阵列。
public class SensorArray : NSObject, Sensor {
    private let logger = SpecificSensorLogger(subsystem: "Sensor", category: "SensorArray")
    private var sensorArray: [Sensor] = []
    public var payloadDataSupplier: PayloadDataSupplier?
    public static let deviceDescription = "\(UIDevice.current.name) (iOS \(UIDevice.current.systemVersion))"
    
    private var specificBle: SpecificBLESensor?;
    
    public init(_ payloadDataSupplier: PayloadDataSupplier) {
        logger.debug("init")
        self.payloadDataSupplier = payloadDataSupplier
        
        //-行程传感器启用后台BLE广播检测
        //-这是可选的，因为Android设备可以充当中继，但启用位置传感器将在后台启用直接iOS-iOS检测。
        //-请注意，Pioneer从未使用或记录实际位置。
        if let travelSensorResolution = BLESensorConfiguration.TravelSensorEnabled {
            sensorArray.append(SpecificTravelSensor(resolution: travelSensorResolution, rangeForBeacon: UUID(uuidString:  BLESensorConfiguration.serviceUUID.uuidString)))
        }
        specificBle = SpecificBLESensor(payloadDataSupplier)
        sensorArray.append(specificBle!)
        
        
        // 初始化时用于在日志中标识此设备的有效负载数据
        let payloadData = payloadDataSupplier.payload(PayloadTimestamp(), device: nil)
        super.init()
        logger.debug("device (os=\(UIDevice.current.systemName)\(UIDevice.current.systemVersion),model=\(deviceModel()))")

        if let payloadData = payloadData {
            logger.info("DEVICE (payloadPrefix=\(payloadData.shortName),description=\(SensorArray.deviceDescription))")
        } else {
            logger.info("DEVICE (payloadPrefix=EMPTY,description=\(SensorArray.deviceDescription))")
        }
    }
    
    public func getNewPayloadData() -> PayloadData{
        return (self.payloadDataSupplier?.payload(PayloadTimestamp(), device: nil))!
    }
    
    private func deviceModel() -> String {
        var deviceInformation = utsname()
        uname(&deviceInformation)
        let mirror = Mirror(reflecting: deviceInformation.machine)
        return mirror.children.reduce("") { identifier, element in
            guard let value = element.value as? Int8, value != 0 else {
                return identifier
            }
            return identifier + String(UnicodeScalar(UInt8(value)))
        }
    }

    
    public func add(delegate: SensorDelegate) {
        sensorArray.forEach { $0.add(delegate: delegate) }
    }
    
    public func start() {
        logger.debug("start")
        sensorArray.forEach { $0.start() }
    }
    
    public func stop() {
        logger.debug("stop")
        sensorArray.forEach { $0.stop() }
    }
}
