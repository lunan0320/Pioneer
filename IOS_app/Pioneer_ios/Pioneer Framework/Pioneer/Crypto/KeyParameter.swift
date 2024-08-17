//
//  KeyParameter.swift
//  Pioneer
//
//  Created by Beh on 2021/6/3.
//

import Foundation

public class KeyParameter : CipherParameters
{
    private var key:[UInt8]
    
    convenience init(from key:[UInt8])
    {
        self.init(from:key,keyOff:0,keyLen:key.count)
    }
    
    init(from key:[UInt8],keyOff:Int,keyLen:Int)
    {
        self.key = [UInt8](repeating:0,count:keyLen)
        self.key[0..<keyLen] = key[0..<keyLen]
    }
    public func getKey() -> [UInt8]
    {
        return key
    }
}
