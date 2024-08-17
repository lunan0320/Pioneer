//
//  SpecificDataSupplier.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation


public protocol PayloadDataSupplier {
    
    func payload(_ timestamp: PayloadTimestamp, device: Device?) -> PayloadData?
    
    func payload(_ data: Data) -> [PayloadData]
}

public extension PayloadDataSupplier {
    

    func payload(_ data: Data) -> [PayloadData] {
        
        let fixedLengthPayload = payload(PayloadTimestamp(), device: nil)
        
        var payloads: [PayloadData] = []
        if let fixedLengthPayload = fixedLengthPayload {
            let payloadLength = fixedLengthPayload.count
            var indexStart = 0, indexEnd = payloadLength
            while indexEnd <= data.count {
                let payload = PayloadData(data.subdata(in: indexStart..<indexEnd))
                payloads.append(payload)
                indexStart += payloadLength
                indexEnd += payloadLength
            }
        }
        return payloads
    }
}


public typealias PayloadTimestamp = Date

// 从目标接收的加密有效负载数据
public class PayloadData : Hashable, Equatable {
    public var data: Data
    public var shortName: String {
        guard data.count > 0 else {
            return ""
        }
        guard data.count > 3 else {
            return data.base64EncodedString()
        }
        return String(data.subdata(in: 3..<data.count).base64EncodedString().prefix(6))
    }

    public init(_ data: Data) {
        self.data = data
    }

    public init?(base64Encoded: String) {
        guard let data = Data(base64Encoded: base64Encoded) else {
            return nil
        }
        self.data = data
    }

    public init(repeating: UInt8, count: Int) {
        self.data = Data(repeating: repeating, count: count)
    }

    public init() {
        self.data = Data()
    }
    
    // MARK:- 解析Payload
    
    public func getCommonHeader() -> Data?{
        return data.subdata(in: 0..<5)
    }
    
    public func getContactIdentifier() -> String?{
        return data.subdata(in: 5..<21).base64EncodedString()
    }
    
    public func getStartTime() -> Date?{
        let timeInterval = TimeInterval(data.uint64(21)! / 1000)
        return Date(timeIntervalSince1970: timeInterval)
    }
    
    public func getEndTime(_ startTime:Date) -> Date?{
        let timeInterval = startTime.timeIntervalSince1970 + TimeInterval.minute * 6
        return Date(timeIntervalSince1970: timeInterval)
    }
    
    public func getMAC() -> String?{
        return data.subdata(in: 29..<count).base64EncodedString()
    }
    
    public func isValid(timestamp:Date = Date()) -> Bool{
        guard let startTime = self.getStartTime(),let endTime = self.getEndTime(startTime) else{
            return false
        }
        guard startTime <= timestamp,endTime >= timestamp else{
            return false
        }
        return true
    }
    
    // MARK:- Data
    
    public var count: Int { get { data.count }}
    
    public var hexEncodedString: String { get { data.hexEncodedString }}
    
    public func base64EncodedString() -> String {
        return data.base64EncodedString()
    }
    
    public func subdata(in range: Range<Data.Index>) -> Data {
        return data.subdata(in: range)
    }
    
    // MARK:- Hashable
    
    public var hashValue: Int { get { data.hashValue } }
    
    public func hash(into hasher: inout Hasher) {
        data.hash(into: &hasher)
    }
    
    // MARK:- Equatable
    
    public static func ==(lhs: PayloadData, rhs: PayloadData) -> Bool {
        return lhs.data == rhs.data
    }
    
    // MARK:- Append
    public func append(_ other: PayloadData) {
        data.append(other.data)
    }
    
    public func append(_ other: Data) {
        data.append(other)
    }

    public func append(_ other: Int8) {
        data.append(other)
    }

    public func append(_ other: Int16) {
        data.append(other)
    }
    
    public func append(_ other: Int32) {
        data.append(other)
    }
    
    public func append(_ other: Int64) {
        data.append(other)
    }

    public func append(_ other: UInt8) {
        data.append(other)
    }

    public func append(_ other: UInt16) {
        data.append(other)
    }
    
    public func append(_ other: UInt32) {
        data.append(other)
    }
    
    public func append(_ other: UInt64) {
        data.append(other)
    }
    
}

