//
//  Sensor.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation

///用于综合检测和跟踪各种疾病传播媒介的传感器，例如位置传感器、蓝牙传感器等。
public protocol Sensor {
    ///添加用于响应传感器事件的委托。
    func add(delegate: SensorDelegate)
    
    func start()
    
    func stop()
}

