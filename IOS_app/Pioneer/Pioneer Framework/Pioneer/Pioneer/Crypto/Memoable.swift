//
//  Memoable.swift
//  Pioneer
//
//  Created by Beh on 2021/6/3.
//

import Foundation

public protocol Memoable
{
    func copy() -> Memoable
    
    func reset(from other:Memoable)
}
