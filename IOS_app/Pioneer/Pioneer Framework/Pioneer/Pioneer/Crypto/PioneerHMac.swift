//
//  PioneerHMac.swift
//  Pioneer
//
//  Created by Beh on 2021/6/3.
//

import Foundation

public class PioneerHMac
{
    var digest: Digest
    var hmac: MAC
    
    init(from key: [UInt8])
    {
        digest = SM3Digest()
        hmac = HMAC(which:digest)
        hmac.macinit(from: KeyParameter(from:key))
    }
    
    public func generateMac(from message: [UInt8],to mac: inout [UInt8])
    {
        generateMac(from: message, inOff: 0, len: message.count, to: &mac, outOff: 0)
    }
    
    public func generateMac(from message: [UInt8], inOff: Int, len: Int, to mac: inout [UInt8], outOff: Int)
    {
        hmac.update(from: message, inOff: inOff, len: len)
        _ = hmac.doFinal(to: &mac, outOff: outOff)
    }
}
