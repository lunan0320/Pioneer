//
//  UploadManager.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation
public class UploadManager{
    
    //自动化上传PhoneNumber和14天MatchingKeys
    public static func uploadDataAuto(queue:DispatchQueue, time: Date = Date()) -> Bool{
        var body:String = "4"
        body.append(UserDefaults.standard.string(forKey: "PhoneNumber")!)
        let secretKey: SecretKey = SecretKey.init(base64Encoded: UserDefaults.standard.string(forKey: "SecretKey")!)!
        body.append(KeyManager.fortnightMatching(secretKey: secretKey, time: time))
        let upLoadConnection = Connection.init(queue: queue)
        upLoadConnection.start()
        guard upLoadConnection.send(dataMessage: body) else{
            upLoadConnection.cancel()
            return false
        }
        upLoadConnection.cancel()
        return true
    }
    
    //Token模式下上传PhoneNumber和14天MatchingKeys
    public static func uploadDataToken(queue:DispatchQueue,time: Date = Date(),upLoadConnection: Connection) -> Bool{
        var body:String = "3"
        body.append(UserDefaults.standard.string(forKey: "PhoneNumber")!)
        let secretKey: SecretKey = SecretKey.init(base64Encoded: UserDefaults.standard.string(forKey: "SecretKey")!)!
        body.append(KeyManager.fortnightMatching(secretKey: secretKey, time: time))
        guard upLoadConnection.send(dataMessage: body) else{
            upLoadConnection.cancel()
            return false
        }
        guard let response = upLoadConnection.recieve(),response == "0" else{
            upLoadConnection.cancel()
            return false
        }
        upLoadConnection.cancel()
        return true
    }
    
    //自动从服务器下载风险者列表
    public static func downloadRiskIdentifier(queue:DispatchQueue,time: Date = Date()) -> [MatchingKey]{
        let body:String = "5"
        var matchingKeysList: [MatchingKey] = []
        let downloadConnection = Connection.init(queue: queue)
        downloadConnection.start()
        guard downloadConnection.send(dataMessage: body) else{
            downloadConnection.cancel()
            return matchingKeysList
        }
        guard var response = downloadConnection.recieve() else{
            downloadConnection.cancel()
            return matchingKeysList
        }
        //处理收到的列表
        downloadConnection.cancel()
        var header = response.prefix(3)
        var matchingKeysStringList: [String] = []
        
        while !header.isEmpty && response.count > 46{
            if header == "ABC"{
                response.removeSubrange(response.startIndex..<response.index(response.startIndex, offsetBy: 3))
            }else{
                matchingKeysStringList.append(String(response.prefix(44)))
                response.removeSubrange(response.startIndex..<response.index(response.startIndex, offsetBy: 44))
            }
            header = response.prefix(3)
        }
        dump(matchingKeysStringList)
        matchingKeysStringList.forEach{
            matchingKeysList.append(MatchingKey.init(base64Encoded: $0)!)
        }
        return matchingKeysList
        
    }
    
    
    
    
    
}
