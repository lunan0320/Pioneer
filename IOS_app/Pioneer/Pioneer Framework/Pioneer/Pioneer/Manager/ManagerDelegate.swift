//
//  ManagerDelegate.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation

public protocol ManagerDelegate {
    
}

public protocol RegisterDelegate: ManagerDelegate{
    func didRegister()
}

public protocol ContactLogManagerDelegate: ManagerDelegate{
    func matching(predicate: NSPredicate) -> [InsideEncounter]
    
}

public protocol TestManagerDelegate{
    
}

