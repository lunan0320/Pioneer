//
//  RandomGenerator.swift
//  Pioneer
//
//  Created by Beh on 2021/6/3.
//

import Foundation

public protocol RandomGenerator
{
    func addSeedMaterial(from seed: [UInt8])
    
    func addSeedMaterial(from seed: UInt64)
    
    func nextBytes(to bytes: inout [UInt8])
    
    func nextBytes(to bytes: inout [UInt8], start: Int, len: Int)
}
