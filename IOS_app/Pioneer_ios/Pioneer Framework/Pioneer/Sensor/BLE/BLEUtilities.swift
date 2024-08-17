//
//  BLEUtilities.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation
import CoreBluetooth

@available(iOS 10.0, *)
extension CBManagerState: CustomStringConvertible {
    public var description: String {
        switch self {
        case .poweredOff: return ".poweredOff"
        case .poweredOn: return ".poweredOn"
        case .resetting: return ".resetting"
        case .unauthorized: return ".unauthorized"
        case .unknown: return ".unknown"
        case .unsupported: return ".unsupported"
        @unknown default: return "undefined"
        }
    }
}



extension CBPeripheralState: CustomStringConvertible {
    public var description: String {
        switch self {
        case .connected: return ".connected"
        case .connecting: return ".connecting"
        case .disconnected: return ".disconnected"
        case .disconnecting: return ".disconnecting"
        @unknown default: return "undefined"
        }
    }
}


extension TimeInterval {
    public static var never: TimeInterval { get { TimeInterval(Int.max) } }
    public static var fortnight: TimeInterval { get { TimeInterval(1209600) }}
    public static var week: TimeInterval { get { TimeInterval(604800) }}
    public static var day: TimeInterval { get { TimeInterval(86400) } }
    public static var hour: TimeInterval { get { TimeInterval(3600) } }
    public static var minute: TimeInterval { get { TimeInterval(60) } }
}

