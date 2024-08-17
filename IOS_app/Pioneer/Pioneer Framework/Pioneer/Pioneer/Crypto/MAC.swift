//
//  Mac.swift
//  Pioneer
//
//  Created by Beh on 2021/6/3.
//

import Foundation

protocol MAC
{
    func macinit(from params:CipherParameters)
    func getAlgorithmName() -> String
    func getMacSize() -> Int
    func update(from inByte:UInt8)
    func update(from inBytes:[UInt8],inOff:Int,len:Int)
    func doFinal(to out:inout [UInt8],outOff:Int) -> Int
    func reset()
}
