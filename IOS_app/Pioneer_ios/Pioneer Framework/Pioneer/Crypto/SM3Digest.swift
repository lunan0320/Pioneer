//
//  SM3Digest.swift
//  Pioneer
//
//  Created by Beh on 2021/6/3.
//

import Foundation

public class SM3Digest : GeneralDigest
{
    private static let DIGEST_LENGTH:Int = 32
    private static let BLOCK_SIZE:Int = 64/4
    
    private var V = Array<UInt32>(repeating:0,count:DIGEST_LENGTH/4)
    private var inwords = Array<UInt32>(repeating:0,count:BLOCK_SIZE)
    private var xOff:Int = 0
    
    private var W = Array<UInt32>(repeating:0,count:68)
    private static var T:Array<UInt32> = {
        var init_t = Array<UInt32>(repeating:0,count:64)
        for i in 0..<16
        {
            let t:UInt32 = 0x79cc4519
            init_t[i] = (t<<i) | (t>>(32-i))
        }
        for i in 16..<64
        {
            let n:Int = i % 32
            let t:UInt32 = 0x7a879d8a
            init_t[i] = (t<<n) | (t>>(32-n))
        }
        return init_t
    }()
    
    override init()
    {
        super.init()
        reset()
    }
    
    init(from t:SM3Digest)
    {
        super.init(from:t)
        copyIn(from:t)
    }
    
    fileprivate func copyIn(from t:SM3Digest)
    {
        V[0..<V.count] = t.V[0..<V.count]
        inwords[0..<inwords.count] = t.inwords[0..<inwords.count]
        xOff = t.xOff
    }
    
    public override func getAlgorithmName() -> String {
        return "SM3"
    }
    public override func getDigestSize() -> Int {
        return Self.DIGEST_LENGTH
    }
    public override func copy() -> Memoable {
        return SM3Digest(from:self)
    }
    
    public override func reset(from other: Memoable) {
        let d = SM3Digest(from:other as! SM3Digest)
        super.copyIn(from:d)
        copyIn(from:d)
    }
    
    public override func reset()
    {
        super.reset()
        V[0] = 0x7380166f
        V[1] = 0x4914b2b9
        V[2] = 0x172442d7
        V[3] = 0xda8a0600
        V[4] = 0xa96f30bc
        V[5] = 0x163138aa
        V[6] = 0xe38dee4d
        V[7] = 0xb0fb0e4e
        
        xOff=0
    }
    
    public override func doFinal(outbytes: inout Array<UInt8>, outOff: Int) -> Int {
        finish()
        Util.intToBigEndian(srcIntArray: V, desByteArray: &outbytes, offset: outOff)
        reset()
        return Self.DIGEST_LENGTH
    }
    
    override func processWord(inbytes: Array<UInt8>, inOff: Int) {
        let n:UInt32 = ((UInt32(inbytes[inOff] & 0xff)<<24) |
        (UInt32(inbytes[inOff+1] & 0xff)<<16) |
        (UInt32(inbytes[inOff+2] & 0xff)<<8) |
        (UInt32(inbytes[inOff+3] & 0xff)))
        inwords[xOff] = n
        xOff += 1
        if(xOff>=16)
        {
            processBlock()
        }
    }
    
    override func processLength(bitlength: Int64) {
        if(xOff > (Self.BLOCK_SIZE - 2))
        {
            inwords[xOff] = 0
            xOff += 1
            
            processBlock()
        }
        
        while(xOff < (Self.BLOCK_SIZE-2))
        {
            inwords[xOff] = 0
            xOff += 1
        }
        
        inwords[xOff] = UInt32(bitlength)>>32
        xOff += 1
        inwords[xOff] = UInt32(bitlength)
        xOff += 1
    }
    
    private func P0(x:UInt32) -> UInt32
    {
        let r9:UInt32 = ((x<<9) | (x>>(32-9)))
        let r17:UInt32 = ((x<<17) | (x>>(32-17)))
        return x^r9^r17
    }
    
    private func P1(x:UInt32) -> UInt32
    {
        let r15:UInt32 = ((x<<15) | x>>(32-15))
        let r23:UInt32 = ((x<<23) | x>>(32-23))
        return x^r15^r23
    }
    
    private func FF0(x:UInt32,y:UInt32,z:UInt32) -> UInt32
    {
        return x^y^z
    }
    
    private func FF1(x:UInt32,y:UInt32,z:UInt32) -> UInt32
    {
        return ((x&y) | (x&z) | (y&z))
    }
    
    private func GG0(x:UInt32,y:UInt32,z:UInt32) -> UInt32
    {
        return x^y^z
    }
    
    private func GG1(x:UInt32,y:UInt32,z:UInt32) -> UInt32
    {
        return ((x&y) | ((~x)&z))
    }
    
    override func processBlock()
    {
        for j in 0..<16
        {
            W[j] = inwords[j]
        }
        
        for j in 16..<68
        {
            let wj3:UInt32 = W[j-3]
            let r15:UInt32 = ((wj3<<15) | (wj3>>(32-15)))
            let wj13:UInt32 = W[j-13]
            let r7:UInt32 = ((wj13<<7) | wj13>>(32-7))
            W[j] = P1(x:W[j-16]^W[j-9]^r15)^r7^W[j-6]
        }
        
        var A:UInt32 = V[0]
        var B:UInt32 = V[1]
        var C:UInt32 = V[2]
        var D:UInt32 = V[3]
        var E:UInt32 = V[4]
        var F:UInt32 = V[5]
        var G:UInt32 = V[6]
        var H:UInt32 = V[7]
        
        for j in 0..<16
        {
            let a12:UInt32 = ((A<<12) | A>>(32-12))
            let s1_:UInt32 = a12 &+ E &+ Self.T[j]
            let SS1:UInt32 = ((s1_<<7) | (s1_>>(32-7)))
            let SS2:UInt32 = SS1 ^ a12
            let Wj:UInt32 = W[j]
            let W1j:UInt32 = Wj ^ W[j+4]
            let TT1:UInt32 = FF0(x:A,y:B,z:C) &+ D &+ SS2 &+ W1j
            let TT2 = GG0(x:E,y:F,z:G) &+ H &+ SS1 &+ Wj
            D = C
            C = ((B<<9) | (B>>(32-9)))
            B = A
            A = TT1
            H = G
            G = ((F<<19) | F>>(32-19))
            F = E
            E = P0(x:TT2)
        }
        
        for j in 16..<64
        {
            let a12:UInt32 = ((A<<12) | A>>(32-12))
            let s1_ = a12 &+ E &+ Self.T[j]
            let SS1:UInt32 = ((s1_<<7) | (s1_>>(32-7)))
            let SS2:UInt32 = SS1^a12
            let Wj:UInt32 = W[j]
            let W1j:UInt32 = Wj^W[j+4]
            let TT1 = FF1(x:A,y:B,z:C) &+ D &+ SS2 &+ W1j
            let TT2 = GG1(x:E,y:F,z:G) &+ H &+ SS1 &+ Wj
            D = C
            C = ((B<<9) | B>>(32-9))
            B = A
            A = TT1
            H = G
            G = ((F<<19) | F>>(32-19))
            F = E
            E = P0(x:TT2)
        }
        
        V[0] ^= A
        V[1] ^= B
        V[2] ^= C
        V[3] ^= D
        V[4] ^= E
        V[5] ^= F
        V[6] ^= G
        V[7] ^= H
        xOff = 0
    }
}
