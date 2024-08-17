//
//  BatteryLog.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation
import NotificationCenter
import os


// 电池日志，用于随时间监视电池电量
public class BatteryLog {
    private let logger = SpecificSensorLogger(subsystem: "Sensor", category: "BatteryLog")
    private let dateFormatter = DateFormatter()
    private let updateInterval = TimeInterval(30)

    public init() {
        dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        // 是否启用了电池监视
        UIDevice.current.isBatteryMonitoringEnabled = true
        NotificationCenter.default.addObserver(self, selector: #selector(batteryLevelDidChange), name: UIDevice.batteryLevelDidChangeNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(batteryStateDidChange), name: UIDevice.batteryStateDidChangeNotification, object: nil)
        // 设置定时时间，每隔updataInterval时间间隔，执行一次update（）功能
        let _ = Timer.scheduledTimer(timeInterval: updateInterval, target: self, selector: #selector(update), userInfo: nil, repeats: true)
    }
    
    private func timestamp() -> String {
        let timestamp = dateFormatter.string(from: Date())
        return timestamp
    }

    @objc func update() {
        // 记录电池日志
        let powerSource = (UIDevice.current.batteryState == .unplugged ? "battery" : "external")
        let batteryLevel = Float(UIDevice.current.batteryLevel * 100).description
        logger.debug("update (powerSource=\(powerSource),batteryLevel=\(batteryLevel))");
    }
    
    @objc func batteryLevelDidChange(_ sender: NotificationCenter) {
        update()
    }
    
    @objc func batteryStateDidChange(_ sender: NotificationCenter) {
        update()
    }
}
