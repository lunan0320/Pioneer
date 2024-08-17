//
//  GeneralDigest.swift
//  Pioneer
//
//  Created by Beh on 2021/6/3.
//

import Foundation

public class GeneralDigest : ExtendedDigest,Memoable
{
    private static let BYTE_LENGTH:Int = 64
    private var xBuf = Array<UInt8>(repeating:0,count:4)
    private var xBufOff:Int = 0
    private var byteCount:Int64 = 0
    
    init() {}
    init(from t:GeneralDigest)
    {
        copyIn(from:t)
    }
    
    init(from encodedState:Array<UInt8>)
    {
        xBuf[0..<xBuf.count] = encodedState[0..<xBuf.count]
        xBufOff = Int(Util.bigEndianToInt(srcByteArray: encodedState, offset: 4))
        byteCount = Int64(Util.bigEndianToLong(srcByteArray: encodedState, offset: 8))
    }
    
    func copyIn(from t:GeneralDigest)
    {
        xBuf[0..<t.xBuf.count] = t.xBuf[0..<t.xBuf.count]
        xBufOff = t.xBufOff
        byteCount = t.byteCount
    }
    
    public func update(inbyte:UInt8)
    {
        xBuf[xBufOff]=inbyte;
        xBufOff += 1
        if(xBufOff == xBuf.count)
        {
            processWord(inbytes:xBuf,inOff:0)
            xBufOff = 0
        }
    }
    
    public func update(inbytes:Array<UInt8>,inOff:Int,inLen:Int)
    {
        let len:Int = max(inLen,0)
        
        var i:Int = 0
        if(xBufOff != 0)
        {
            while(i<len)
            {
                xBuf[xBufOff] = inbytes[inOff+i]
                xBufOff += 1
                i += 1
                if(xBufOff == 4)
                {
                    processWord(inbytes:xBuf,inOff:0)
                    xBufOff = 0
                    break;
                }
            }
        }
        
        let limit:Int = ((len-i) & ~3)+i
        while(i<limit)
        {
            processWord(inbytes:inbytes,inOff:inOff+i)
            i += 4
        }
        
        while(i<len)
        {
            xBuf[xBufOff] = inbytes[inOff + i]
            xBufOff += 1
            i += 1
        }
        
        byteCount += Int64(len)
    }
    
    public func finish()
    {
        let bitlength:Int64 = (byteCount<<3)
        
        update(inbyte:128)
        
        while(xBufOff != 0)
        {
            update(inbyte:0)
        }
        
        processLength(bitlength:bitlength)
        
        processBlock()
    }
    
    public func reset()
    {
        byteCount=0
        xBufOff=0
        for i in 0..<xBuf.count
        {
            xBuf[i] = 0
        }
    }
    
    
    public func getByteLength() -> Int
    {
        return Self.BYTE_LENGTH
    }
    
    
    
    func processWord(inbytes:Array<UInt8>,inOff:Int)
    {
        // to be overwritten
    }
    func processLength(bitlength:Int64)
    {
        // to be overwritten
    }
    func processBlock()
    {
        // to be overwritten
    }
    
    public func copy() -> Memoable {
        return GeneralDigest(from:self)
    }
    
    public func reset(from other: Memoable) {
        byteCount = (other as! GeneralDigest).byteCount
        xBufOff = (other as! GeneralDigest).xBufOff
        xBuf[0..<xBuf.count] = (other as! GeneralDigest).xBuf[0..<xBuf.count]
    }
    
    public func getAlgorithmName() -> String {
        return "GeneralDigest"
    }
    
    public func getDigestSize() -> Int {
        return 0
    }
    
    public func doFinal(outbytes: inout Array<UInt8>, outOff: Int) -> Int {
        return 0
    }
}


