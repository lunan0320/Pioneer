//
//  SpecificPayloadDataSupplier.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation

public protocol SimplePayloadDataSupplier : PayloadDataSupplier {
}


public class SpecificPayloadDataSupplier : SimplePayloadDataSupplier {
    private let logger = SpecificSensorLogger(subsystem: "Sensor", category: "Payload.SpecificPayloadDataSupplier")
    public static let payloadLength: Int = 61
    private let commonHeader: Data
    public let matchingKeys: [MatchingKey]
    private var day: Int?
    private var contactIdentifiers: [ContactIdentifier]?
    
    public init(commonHeader: Data, secretKey: SecretKey) {
        self.commonHeader = commonHeader
        matchingKeys = KeyManager.matchingKeys(secretKey)
    }
    
    
    public func matchingKey(_ time: Date) -> MatchingKey? {
        let day = KeyManager.day(time)
        guard day >= 0, day < matchingKeys.count else {
            logger.fault("Matching key out of day range (time=\(time),day=\(day)))")
            return nil
        }
        return matchingKeys[day]
    }
        
    private func  contactIdentifier(_ time: Date) -> ContactIdentifier? {
        let day = KeyManager.day(time)
        let period = KeyManager.period(time)
        
        guard day >= 0, day < matchingKeys.count else {
            logger.fault("Contact identifier out of day range (time=\(time),day=\(day)))")
            return nil
        }
        
        // 按需生成和缓存特定日期的接触人密钥
        if self.day != day {
            contactIdentifiers = KeyManager.contactKeys(matchingKeys[day]).map({ KeyManager.contactIdentifier($0) })
            self.day = day
        }
        
        guard let contactIdentifiers = contactIdentifiers else {
            logger.fault("Contact identifiers unavailable (time=\(time),day=\(day)))")
            return nil
        }
        
        guard period >= 0, period < contactIdentifiers.count else {
            logger.fault("Contact identifier out of period range (time=\(time),period=\(period)))")
            return nil
        }
        
        // 防御性检查
        guard contactIdentifiers[period].count == 16 else {
            logger.fault("Contact identifier not 16 bytes (time=\(time),count=\(contactIdentifiers[period].count))")
            return nil
        }
        
        return contactIdentifiers[period]
    }
    
    
    // MARK:- SimplePayloadDataSupplier
    public func payload(_ timestamp: Date = Date(), device: Device?) -> PayloadData? {
        let payloadData = PayloadData()
        var message: Data = Data.init()
        payloadData.append(commonHeader)
        if let contactIdentifier = contactIdentifier(timestamp) {
            message.append(contactIdentifier)
            message.append(UInt64(timestamp.timeIntervalSince1970 * 1000))
        } else {
            message = Data.init(repeating: 0, count: 24)
        }
        payloadData.append(message)
        payloadData.append(Crypto.MAC(message: message, key: self.matchingKey(timestamp)!))
        return payloadData
    }

    public func payload(_ data: Data) -> [PayloadData] {
        
        var payloads: [PayloadData] = []
        var indexStart = 0, indexEnd = SpecificPayloadDataSupplier.payloadLength
        while indexEnd <= data.count {
            let payload = PayloadData(data.subdata(in: indexStart..<indexEnd))
            payloads.append(payload)
            indexStart += SpecificPayloadDataSupplier.payloadLength
            indexEnd += SpecificPayloadDataSupplier.payloadLength
        }
        return payloads
    }
}


