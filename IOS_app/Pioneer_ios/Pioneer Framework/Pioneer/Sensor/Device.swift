//
//  Device.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation
public class Device : NSObject {
    /// 设备注册时间戳
    var createdAt: Date
    /// 最后一次属性更新时间
    var lastUpdatedAt: Date
    
    /// 临时设备标识符
    public var identifier: TargetIdentifier
    
    init(_ identifier: TargetIdentifier) {
        self.createdAt = Date()
        self.identifier = identifier
        lastUpdatedAt = createdAt
    }
}
