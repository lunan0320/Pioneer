//
//  Crypto.swift
//  Pioneer
//
//  Created by Beh on 2021/6/3.
//

import Foundation

public class Crypto {
    
    //Hash生成
    public static func Hash(message: Data) -> Data {
        var Hash = [UInt8](repeating: 0,count: 32)
        let pHash: PioneerHash = PioneerHash()
        pHash.generateHash(from: [UInt8].init(message), to: &Hash)
        return Data.init(Hash)
    }
    
    
    //MAC生成
    public static func MAC(message: Data, key: Data) -> Data {
        var MAC = [UInt8](repeating: 0, count: 32)
        let pMAC = PioneerHMac(from: [UInt8].init(key))
        pMAC.generateMac(from: [UInt8].init(message), to: &MAC)
        return Data.init(MAC)
    }
}
