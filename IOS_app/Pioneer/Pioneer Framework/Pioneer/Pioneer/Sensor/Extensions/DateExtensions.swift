//
//  DateExtensions.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation

extension Date {

    //定义Date类型运算符“-”
    static func - (lhs: Date, rhs: Date) -> TimeInterval {
        return lhs.timeIntervalSince1970 - rhs.timeIntervalSince1970
    }
}
