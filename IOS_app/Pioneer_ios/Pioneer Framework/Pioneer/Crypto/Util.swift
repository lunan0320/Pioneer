//
//  Util.swift
//  Pioneer
//
//  Created by Beh on 2021/6/3.
//

import Foundation

public class Util
{
    public static func bigEndianToInt(srcByteArray bs:Array<UInt8>,offset off:Int) -> UInt32
    {
        var n:UInt32 = UInt32(bs[off] << 24)
        n |= UInt32((bs[off+1] & 0xff)<<16)
        n |= UInt32((bs[off+2] & 0xff)<<8)
        n |= UInt32(bs[off+3] & 0xff)
        return n
    }
    
    public static func intToBigEndian(srcInt n:UInt32,desByteArray bs:inout Array<UInt8>,offset off:Int)
    {
        bs[off] = UInt8(truncatingIfNeeded:n>>24)
        bs[off+1] = UInt8(truncatingIfNeeded:n>>16)
        bs[off+2] = UInt8(truncatingIfNeeded:n>>8)
        bs[off+3] = UInt8(truncatingIfNeeded:n)
    }
    
    public static func intToBigEndian(srcIntArray ns:Array<UInt32>,desByteArray bs:inout Array<UInt8>,offset off:Int)
    {
        var offset = off
        for i in 0..<ns.count
        {
            intToBigEndian(srcInt:ns[i],desByteArray:&bs,offset:offset)
            offset += 4
        }
    }
    
    public static func bigEndianToLong(srcByteArray bs:Array<UInt8>,offset off:Int) -> UInt64
    {
        let hi:UInt32 = bigEndianToInt(srcByteArray:bs,offset:off)
        let lo:UInt32 = bigEndianToInt(srcByteArray:bs,offset:off+4)
        return UInt64((hi & 0xffffffff)<<32) | UInt64(lo & 0xffffffff)
    }
    
    public static func longToBigEndian(srcLong n:UInt64,desByteArray bs:inout Array<UInt8>,offset off:Int)
    {
        intToBigEndian(srcInt:UInt32(n >> 32),desByteArray:&bs,offset:off)
        intToBigEndian(srcInt:UInt32(n & 0xffffffff),desByteArray:&bs,offset:off+4)
    }
}
