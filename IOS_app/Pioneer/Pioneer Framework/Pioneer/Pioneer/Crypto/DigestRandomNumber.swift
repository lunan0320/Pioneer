//
//  DigestRandomNumber.swift
//  Pioneer
//
//  Created by Beh on 2021/6/3.
//

import Foundation

public class DigestRandomNumber : RandomGenerator
{
    private static let CYCLE_COUNT:Int64 = 10
    private var stateCounter:UInt64
    private var seedCounter:UInt64
    private var digest:Digest
    private var state:[UInt8]
    private var seed:[UInt8]
    
    init(from digest:Digest)
    {
        self.digest = digest
        
        self.seed = [UInt8](repeating:0,count:digest.getDigestSize())
        self.seedCounter = 1
        
        self.state = [UInt8](repeating:0,count:digest.getDigestSize())
        self.stateCounter = 1
    }
    
    public func addSeedMaterial(from inSeed: [UInt8]) {
        if(seed.count != 0)
        {
            digestUpdate(from:inSeed)
        }
        digestUpdate(from:self.seed)
        digestDoFinal(to:&self.seed)
    }
    
    public func addSeedMaterial(from rSeed: UInt64) {
        digestAddCounter(from:rSeed)
        digestUpdate(from:self.seed)
        digestDoFinal(to:&self.seed)
    }
    
    public func nextBytes(to bytes: inout [UInt8]) {
        nextBytes(to: &bytes, start: 0, len: bytes.count)
    }
    
    public func nextBytes(to bytes: inout [UInt8], start: Int, len: Int) {
        var stateOff:Int = 0
        
        generateState()
        
        let end:Int = start + len
        
        for i in start..<end
        {
            if(stateOff == state.count)
            {
                generateState()
                stateOff = 0
            }
            bytes[i] = state[stateOff]
            stateOff += 1
        }
    }
    
    private func cycleSeed()
    {
        digestUpdate(from:self.seed)
        digestAddCounter(from:self.seedCounter)
        self.seedCounter += 1
        digestDoFinal(to:&self.seed)
    }
    
    private func generateState()
    {
        digestAddCounter(from: self.stateCounter)
        stateCounter += 1
        digestUpdate(from: self.state)
        digestUpdate(from: self.seed)
        
        digestDoFinal(to: &self.state)
        
        if((stateCounter % UInt64(Self.CYCLE_COUNT)) == 0)
        {
            cycleSeed()
        }
    }
    
    private func digestAddCounter(from seed:UInt64)
    {
        var tmpseed:UInt64 = seed
        for _ in 0..<8
        {
            digest.update(inbyte: UInt8(truncatingIfNeeded:tmpseed))
            tmpseed >>= 8
        }
    }
    
    private func digestUpdate(from inSeed:[UInt8])
    {
        digest.update(inbytes: inSeed, inOff: 0, inLen: inSeed.count)
    }
    
    private func digestDoFinal(to result:inout [UInt8])
    {
        let _ = digest.doFinal(outbytes: &result, outOff: 0)
    }
}
