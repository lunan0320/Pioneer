//
//  PioneerHash.swift
//  Pioneer
//
//  Created by Beh on 2021/6/3.
//

import Foundation

public class PioneerHash
{
    var digest:Digest
    
    init()
    {
        digest = SM3Digest()
    }
    
    public func generateHash(from message:[UInt8],to hash:inout [UInt8])
    {
        generateHash(from: message, inOff: 0, len: message.count, hash: &hash, outOff: 0)
    }
    
    public func generateHash(from message:[UInt8],inOff:Int,len:Int,hash:inout [UInt8],outOff:Int)
    {
        digest.update(inbytes: message, inOff: inOff, inLen: len)
        _ = digest.doFinal(outbytes: &hash, outOff: outOff)
    }
}
