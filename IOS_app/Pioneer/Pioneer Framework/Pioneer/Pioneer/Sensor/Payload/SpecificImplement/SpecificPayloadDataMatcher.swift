//
//  SpecificPayloadDataMatcher.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation
protocol SimplePayloadDataMatcher : PayloadDataMatcher {
}

public class SpecificPayloadDataMatcher : SimplePayloadDataMatcher {
    private var delegate: ContactLogManagerDelegate
    
    public init(delegate: ContactLogManagerDelegate){
        self.delegate = delegate
    }
    
    public func createPredicate(timestamp:Date = Date(),range: Int = 14,contactIdentifier: String) -> NSPredicate{
        let (second,_) = Int(timestamp.timeIntervalSince1970).remainderReportingOverflow(dividingBy: 86400)
        let middle = timestamp.timeIntervalSince1970 - TimeInterval(second) - Double(range) * TimeInterval.fortnight
        let timeSinceRange = Date.init(timeIntervalSince1970: middle)
        return NSPredicate.init(format: "startTime >= %@ && startTime <= %@ && contactIdentifier == %@ ",timeSinceRange as CVarArg,timestamp as CVarArg,contactIdentifier)
    }
    
    
    // MARK:- SimplePayloadDataMatcher

    public func matches(_ timestamp: PayloadTimestamp, _ matchingKeysList: [MatchingKey], _ queue: DispatchQueue) -> Bool {
        var matcheResult: Bool = false
        let semaphore = DispatchSemaphore.init(value: 0)
        queue.async {
            matchingKeysList.forEach{ matchingKey in
                KeyManager.contactKeys(matchingKey).forEach{ contactKey in
                    let contactIdentifier = KeyManager.contactIdentifier(contactKey).base64EncodedString()
                    if self.isVaild(self.delegate.matching(predicate: self.createPredicate(timestamp: timestamp,contactIdentifier: contactIdentifier)),matchingKey){
                        matcheResult = true
                        semaphore.signal()
                    }
                }
            }
            semaphore.signal()
        }
        _ = semaphore.wait(timeout: DispatchTime.distantFuture)
        return matcheResult
    }
    
    public func isVaild(_ encounterList: [InsideEncounter], _ matchingKey: MatchingKey) -> Bool{
        guard encounterList.filter({
            var message = Data.init(base64Encoded: $0.contactIdentifier)!
            message.append(UInt64($0.startTime.timeIntervalSince1970 * 1000))
            return $0.mac == Crypto.MAC(message: message, key: matchingKey).base64EncodedString()
        }).isEmpty else{
            return true
        }
        return false
        
    }
}

