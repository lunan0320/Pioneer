//
//  KeyManager.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation
import CommonCrypto
import Accelerate


public typealias SecretKey = Data

public typealias MatchingKey = Data

public typealias ContactIdentifier = Data

/// 密钥派生函数
public class KeyManager {
    private static let secretKeyLength = 2048
    
    private static let days = 2000
    
    private static let periods = 240
    
    private static let epoch = KeyManager.getEpoch()
    
    static func date(_ fromString: String) -> Date? {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .iso8601)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ssXXXX"
        return formatter.date(from: fromString)
    }
    
    
    static func getEpoch() -> TimeInterval {
        return date("2020-09-01T00:00:00+0000")!.timeIntervalSince1970
    }
    
    
    public static func day(_ onDate: Date = Date()) -> Int {
        let (day,_) = Int(onDate.timeIntervalSince1970 - epoch).dividedReportingOverflow(by: 86400)
        return day
    }
    
    
    public static func period(_ atTime: Date = Date()) -> Int {
        let (second,_) = Int(atTime.timeIntervalSince1970 - epoch).remainderReportingOverflow(dividingBy: 86400)
        let (period,_) = second.dividedReportingOverflow(by: 86400 / periods)
        return period
    }
    
    public static func endTime(atTime: Date) -> UInt32{
        let (second,_) = Int(atTime.timeIntervalSince1970).remainderReportingOverflow(dividingBy: 86400)
        let (period,_) = second.dividedReportingOverflow(by: 86400 / periods)
        return UInt32(Int(atTime.timeIntervalSince1970) - second + (period + 1) * 360)
    }
    
    
    static func secretKey() -> SecretKey? {
        var bytes = [UInt8](repeating: 0, count: secretKeyLength)
        let status = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        guard status == errSecSuccess else {
            return nil
        }
        return SecretKey(bytes)
    }
    
    
    static func matchingKeys(_ secretKey: SecretKey) -> [MatchingKey] {
        let n = KeyManager.days
        /**
         
         面向安全的匹配密钥种子由一个带截断的反向哈希链生成，以确保将来的密钥不能从历史密钥派生。加密散列函数为前向安全性提供了一个单向函数。截断函数通过删除中间密钥材料提供额外的保证，因此受损的哈希函数仍将保持前向安全性。
         */
        var matchingKeySeed: [MatchingKeySeed] = Array(repeating: MatchingKeySeed(), count: n + 1)
        /**
         最后一个匹配的密钥种子在2000天（从epoch算起超过5年）是密钥的散列。在2000天耗尽所有匹配的密钥种子之前，需要建立一个新的密钥。
         */
        matchingKeySeed[n] = MatchingKeySeed(Crypto.Hash(message: secretKey))
        for i in (0...n - 1).reversed() {
            matchingKeySeed[i] = MatchingKeySeed(Crypto.Hash(message: KeyF.t(matchingKeySeed[i + 1])))
        }
        /**
         第i天的匹配密钥是第i天与第i-1天的匹配密钥种子的异或的哈希。有必要将匹配密钥与其种子分离，因为在分散的联系人跟踪解决方案中，匹配密钥由服务器分发给所有手机进行设备上匹配。如果一个种子用于派生其他日期的种子，则发布哈希可防止攻击者建立其他种子。
         */
        var matchingKey: [MatchingKey] = Array(repeating: MatchingKey(), count: n + 1)
        for i in 1...n {
            matchingKey[i] = MatchingKey(Crypto.Hash(message: KeyF.xor(matchingKeySeed[i], matchingKeySeed[i - 1])))
        }
        /**
         第0天的匹配密钥派生自第0天和第- 1天的匹配密钥种子。在上面的代码中实现为特例。
         */
        let matchingKeySeedMinusOne = MatchingKeySeed(Crypto.Hash(message: KeyF.t(matchingKeySeed[0])))
        matchingKey[0] = MatchingKey(Crypto.Hash(message: KeyF.xor(matchingKeySeed[0], matchingKeySeedMinusOne)))
        return matchingKey
    }
    
    
    public static func contactKeys(_ matchingKey: MatchingKey) -> [ContactKey] {
        let n = KeyManager.periods

        /**
         
         面向安全接触密钥种子是由一个带有截断的反向哈希链生成的，以确保将来的密钥不能从历史密钥中派生。这与生成匹配密钥种子的过程相同。种子永远不会通过手机传送。它们在密码学上具有挑战性，难以从广播接触人密钥中泄露，而在给定匹配密钥或私密密钥的情况下很容易生成。
         */
        var contactKeySeed: [ContactKeySeed] = Array(repeating: ContactKeySeed(), count: n + 1)
       
        contactKeySeed[n] = Crypto.Hash(message: matchingKey)
        for j in (0...n - 1).reversed() {
            contactKeySeed[j] = ContactKeySeed(Crypto.Hash(message: KeyF.t(contactKeySeed[j + 1])))
        }
        
        var contactKey: [ContactKey] = Array(repeating: ContactKey(), count: n + 1)
        for j in 1...n {
            contactKey[j] = ContactKey(Crypto.Hash(message: KeyF.xor(contactKeySeed[j], contactKeySeed[j - 1])))
        }
        
        let contactKeySeedMinusOne = ContactKeySeed(Crypto.Hash(message: KeyF.t(contactKeySeed[0])))
        contactKey[0] = ContactKey(Crypto.Hash(message: KeyF.xor(contactKeySeed[0], contactKeySeedMinusOne)))
        return contactKey
    }

    
    public static func contactIdentifier(_ contactKey: ContactKey) -> ContactIdentifier {
        return ContactIdentifier(KeyF.t(contactKey, 16))
    }
    
    static func fortnightMatching(secretKey: SecretKey,time: Date) -> String{
        var description: String = ""
        let matching: [MatchingKey] = KeyManager.matchingKeys(secretKey)
        let day: Int = KeyManager.day(time)
        for index in day-13...day{
            description.append(matching[index].base64EncodedString())
        }
        return description
    }
}


private class KeyF {
    
    
    fileprivate static func h(_ data: Data) -> Data {
        var hash = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
        data.withUnsafeBytes({ _ = CC_SHA256($0.baseAddress, CC_LONG(data.count), &hash) })
        return Data(hash)
    }
    

    /// 截断函数：删除数据的后半部分
    fileprivate static func t(_ data: Data) -> Data {
        return KeyF.t(data, data.count / 2)
    }
    

    /// 截断函数：保留数据的前n个字节
    fileprivate static func t(_ data: Data, _ n: Int) -> Data {
        return data.subdata(in: 0..<n)
    }
    
    /// 异或函数：计算左异或右，假设左和右的长度相同
    fileprivate static func xor(_ left: Data, _ right: Data) -> Data {
        let leftByteArray: [UInt8] = Array(left)
        let rightByteArray: [UInt8] = Array(right)
        var resultByteArray: [UInt8] = [UInt8]()
        for i in 0..<leftByteArray.count {
            resultByteArray.append(leftByteArray[i] ^ rightByteArray[i])
        }
        return Data(resultByteArray)
    }


}

fileprivate typealias Binary16 = UInt16
fileprivate typealias MatchingKeySeed = Data
fileprivate typealias ContactKeySeed = Data
public typealias ContactKey = Data


