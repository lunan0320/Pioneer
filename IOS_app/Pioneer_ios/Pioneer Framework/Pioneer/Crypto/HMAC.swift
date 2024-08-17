//
//  HMac.swift
//  Pioneer
//
//  Created by Beh on 2021/6/3.
//

import Foundation

public class HMAC : MAC
{
    private static let IPAD:UInt8 = 0x36
    private static let OPAD:UInt8 = 0x5c
    private var digest:Digest
    private var digestSize:Int
    private var blockLength:Int
    private var ipadState:Memoable?
    private var opadState:Memoable?
    
    private var inputPad:[UInt8]
    private var outputBuf:[UInt8]
    
    private static var blockLengths:[String:Int] = {
        var tmpdict = ["SM3":64]
        return tmpdict
    }()
    
    private static func getByteLength(which digest:Digest) -> Int
    {
        return blockLengths[digest.getAlgorithmName()]!
    }
    convenience init(which digest:Digest)
    {
        self.init(which:digest,byteLength:Self.getByteLength(which:digest))
    }
    init(which digest:Digest,byteLength:Int)
    {
        self.digest = digest
        self.digestSize = digest.getDigestSize()
        self.blockLength = byteLength
        self.inputPad = [UInt8](repeating:0,count:blockLength)
        self.outputBuf = [UInt8](repeating:0,count:blockLength+digestSize)
    }
    
    public func getAlgorithmName() -> String {
        return digest.getAlgorithmName() + "/HMAC"
    }
    
    public func getUnderlyingDigest() -> Digest
    {
        return digest
    }
    
    public func macinit(from params: CipherParameters) {
        digest.reset()
        
        let key:[UInt8] = (params as! KeyParameter).getKey()
        var keyLength = key.count
        
        if(keyLength > blockLength)
        {
            digest.update(inbytes: key, inOff: 0, inLen: keyLength)
            let _ = digest.doFinal(outbytes: &inputPad, outOff: 0)
            
            keyLength = digestSize
        }
        else
        {
            inputPad[0..<keyLength] = key[0..<keyLength]
        }
        
        for i in keyLength..<inputPad.count
        {
            inputPad[i] = 0
        }
        
        outputBuf[0..<blockLength] = inputPad[0..<blockLength]
        
        Self.xorPad(from: &inputPad, len: blockLength, n: Self.IPAD)
        Self.xorPad(from: &outputBuf, len: blockLength, n: Self.OPAD)
        
        if(digest is Memoable)
        {
            opadState = (digest as! Memoable).copy()
            (opadState as! Digest).update(inbytes: outputBuf, inOff: 0, inLen: blockLength)
        }
        
        digest.update(inbytes: inputPad, inOff: 0, inLen: blockLength)
        
        if(digest is Memoable)
        {
            ipadState = (digest as! Memoable).copy()
        }
        
    }
    
    public func getMacSize() -> Int {
        return digestSize
    }
    
    public func update(from inByte: UInt8) {
        digest.update(inbyte: inByte)
    }
    
    public func update(from inBytes: [UInt8], inOff: Int, len: Int) {
        digest.update(inbytes: inBytes, inOff: inOff, inLen: len)
    }
    
    public func doFinal(to out: inout [UInt8], outOff: Int) -> Int {
        let _ = digest.doFinal(outbytes: &outputBuf, outOff: blockLength)
        
        if(opadState != nil)
        {
            (digest as! Memoable).reset(from:opadState!)
            digest.update(inbytes: outputBuf, inOff: blockLength, inLen: digest.getDigestSize())
        }
        else{
            digest.update(inbytes: outputBuf, inOff: 0, inLen: outputBuf.count)
        }
        
        let len:Int = digest.doFinal(outbytes: &out, outOff: outOff)
        
        for i in blockLength..<outputBuf.count
        {
            outputBuf[i] = 0
        }
        
        if(ipadState != nil)
        {
            (digest as! Memoable).reset(from: ipadState!)
        }
        else
        {
            digest.update(inbytes: inputPad, inOff: 0, inLen: inputPad.count)
        }
        
        
        return len
        
    }
    
    public func reset()
    {
        digest.reset()
        
        digest.update(inbytes: inputPad, inOff: 0, inLen: inputPad.count)
    }
    
    private static func xorPad(from pad:inout [UInt8],len:Int,n:UInt8)
    {
        for i in 0..<len
        {
            pad[i] ^= n
        }
    }
}
