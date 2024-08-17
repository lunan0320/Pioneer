//
//  SensorDelegate.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation
/// 接收传感器事件的传感器委派。
public protocol SensorDelegate {
    
    func sensor(_ sensor: SensorType, didDetect: TargetIdentifier)
    

    func sensor(_ sensor: SensorType, didRead: PayloadData, fromTarget: TargetIdentifier)
    

    func sensor(_ sensor: SensorType, didShare: [PayloadData], fromTarget: TargetIdentifier)
    

    func sensor(_ sensor: SensorType, didMeasure: Proximity, fromTarget: TargetIdentifier)
    

    func sensor(_ sensor: SensorType, didVisit: Location?)
    
    func sensor(_ sensor: SensorType, didMeasure: Proximity, fromTarget: TargetIdentifier, withPayload: PayloadData)
    
    func sensor(_ sensor: SensorType, didUpdateState: SensorState)
}


/// 传感器委托功能都是可选的。
public extension SensorDelegate {
    func sensor(_ sensor: SensorType, didDetect: TargetIdentifier) {}
    func sensor(_ sensor: SensorType, didRead: PayloadData, fromTarget: TargetIdentifier) {}
    func sensor(_ sensor: SensorType, didShare: [PayloadData], fromTarget: TargetIdentifier) {}
    func sensor(_ sensor: SensorType, didMeasure: Proximity, fromTarget: TargetIdentifier) {}
    func sensor(_ sensor: SensorType, didVisit: Location?) {}
    func sensor(_ sensor: SensorType, didMeasure: Proximity, fromTarget: TargetIdentifier, withPayload: PayloadData) {}
    func sensor(_ sensor: SensorType, didUpdateState: SensorState) {}
}

// MARK:- SensorDelegate data



public enum SensorType : String {
    /// 低功耗蓝牙 Bluetooth Low Energy (BLE)
    case BLE
    /// 行程传感器，使用位置传感器
    case TRAVEL
    /// 其他未来可扩充的传感器，以更加准确的手机数据
    case OTHER
}


public enum SensorState : String {
    
    case on
 
    case off

    case unavailable
}

/// 检测目标的临时标识符（例如智能手机、信标、地点）。这可能是一个UUID，但使用字符串作为可变标识符长度。
public typealias TargetIdentifier = String

// MARK:- Proximity data

/// 用于估计传感器和目标之间接近度的原始数据，例如用于BLE的RSSI。
public struct Proximity {

    public let unit: ProximityMeasurementUnit
    public let value: Double
    public let calibration: Calibration?
    public var description: String { get {
        guard let calibration = calibration else {
            return "\(unit.rawValue):\(value.description)"
        }
        return "\(unit.rawValue):\(value.description)[\(calibration.description)]"
    }}
    
    public init(unit: ProximityMeasurementUnit, value: Double, calibration: Calibration? = nil) {
        self.unit = unit
        self.value = value
        self.calibration = calibration
    }
}

public enum ProximityMeasurementUnit : String {
    /// 接收信号强度指示
    case RSSI
    /// 往返时间
    case RTT
}

/// 用于解释传感器和目标之间接近值的校准数据，例如BLE的发射功率。
public struct Calibration {
    
    public let unit: CalibrationMeasurementUnit
   
    public let value: Double
    
    public var description: String { get {
        unit.rawValue + ":" + value.description
    }}
}


/// 用于校准近距离传输数据值的测量装置，例如BLE传输功率
public enum CalibrationMeasurementUnit : String {
    /// 蓝牙发射功率，用于描述1米处的预期RSSI，用于解释测量的RSSI值。
    case BLETransmitPower
}

// MARK:- Location data

/// 用于估算间接暴露的原始位置数据
public struct Location {
    let value: LocationReference
    let time: (start: Date, end: Date)
    public var description: String { get {
        value.description + ":[from=" + time.start.description + ",to=" + time.end.description + "]"
    }}
}

public protocol LocationReference {
    var description: String { get }
}


// 距离以单位米(m)估计
public typealias Distance = Double


// 沿任何方向行进的距离（以米为单位），作为移动范围的指标。
public struct TravelLocationReference : LocationReference {
    let distance: Distance
    public var description: String {
        get {
        "Travel(distance=\(distance))"
        }
        
    }
}

