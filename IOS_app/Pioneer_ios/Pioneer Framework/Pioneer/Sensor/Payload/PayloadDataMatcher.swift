//
//  PayloadDataMatcher.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation

protocol PayloadDataMatcher {
    
    
    func matches(_ timestamp: PayloadTimestamp, _ matchingKeysList: [MatchingKey], _ queue: DispatchQueue) -> Bool
}
