//
//  ExtendedDigest.swift
//  Pioneer
//
//  Created by Beh on 2021/6/3.
//

import Foundation

public protocol ExtendedDigest : Digest
{
    func getByteLength() -> Int
}
