//
//  SensorLogger.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation
import UIKit
import os

public protocol SensorLogger {
    init(subsystem: String, category: String)
    
    func log(_ level: SensorLoggerLevel, _ message: String)
    
    func debug(_ message: String)
    
    func info(_ message: String)
    
    func fault(_ message: String)
}

public enum SensorLoggerLevel: String {
    case off, debug, info, fault
}

public class SpecificSensorLogger: NSObject, SensorLogger {
    private let subsystem: String
    private let category: String
    private let dateFormatter = DateFormatter()
    
    public required init(subsystem: String, category: String) {
        self.subsystem = subsystem
        self.category = category
        dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
    }
    
    private func suppress(_ level: SensorLoggerLevel) -> Bool {
        if (BLESensorConfiguration.logLevel == .off) {
            return true
        }
        switch level {
        case .debug:
            return (BLESensorConfiguration.logLevel == .info || BLESensorConfiguration.logLevel == .fault)
        case .info:
            return (BLESensorConfiguration.logLevel == .fault)
        default:
            return false
        }
    }
    
    public func log(_ level: SensorLoggerLevel, _ message: String) {
        guard !suppress(level) else {
            return
        }
        
        let timestamp = dateFormatter.string(from: Date())
        let csvMessage = message.replacingOccurrences(of: "\"", with: "'")
        let quotedMessage = (message.contains(",") ? "\"" + csvMessage + "\"" : csvMessage)
        let entry = timestamp + "｜" + level.rawValue + "｜" + subsystem + "｜" + category + "｜" + quotedMessage
        print(entry)
        
    }
    
    public func debug(_ message: String) {
        log(.debug, message)
    }
    
    public func info(_ message: String) {
        log(.info, message)
    }
    
    public func fault(_ message: String) {
        log(.fault, message)
    }
    
}

