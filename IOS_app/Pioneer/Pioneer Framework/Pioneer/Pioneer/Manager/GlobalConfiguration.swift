//
//  GlobalConfiguration.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation

public struct GlobalConfiguration{
    
    //默认服务器地址为95.179.230.181//
    public static let serverHost: String = "95.179.230.181"
    public static let serverPort: Int = 446
    public static let serverCertificate: String = "server"
    public static let clientCertificate: String = "client"
    public static let clientCertificatePass: String = "123456"
    public static let TestForAutoDownload: Bool = false
    public static let TimeIntervalForAutoDownload: TimeInterval = 12 * TimeInterval.hour + 0 * TimeInterval.minute
    
}

public class InsideEncounter {
    public let startTime: Date
    public let mac: String
    public let period: Int16
    public let contactIdentifier: String
    
    public init(_ startTime: Date, _ mac: String, _ period: Int16, _ contactIdentifier: String){
        self.startTime = startTime
        self.mac = mac
        self.period = period
        self.contactIdentifier = contactIdentifier
    }
}
