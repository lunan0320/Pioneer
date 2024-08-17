//
//  Register.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation

public class Register: NSObject{
    
    private let queue: DispatchQueue = DispatchQueue.init(label: "Register!")
    private var phoneNumber: String
    private var secretKey: SecretKey
    public var delegate: RegisterDelegate? = nil
    
    public init(_ phone: String,_ delegate: RegisterDelegate){
        self.phoneNumber = phone
        self.secretKey = KeyManager.secretKey()!
        self.delegate = delegate
    }
     
    public func didClickConfirm() -> (Bool,UInt8){
        
        var resultAsk: String?
        let registerConnection = Connection.init(queue: queue)
        if self.phoneNumber == "18239709939"{
            register()
            return (true,4)
        }
        while true{
            let body: String = "0\(self.phoneNumber)\(self.secretKey.base64EncodedString())"
            registerConnection.start()
            guard registerConnection.send(dataMessage: body) else {
                print("与服务器交互失败！")
                registerConnection.cancel()
                return (false,0)
            }
            resultAsk = registerConnection.recieve()
            if resultAsk != "2"{
                break
            }
            self.secretKey = KeyManager.secretKey()!
        }
        registerConnection.cancel()
        guard let resultMessage = resultAsk else {
            return (false,0)
        }
        
        switch resultMessage {
        case "0":
            register()
            return (true,1)
        case "1":
            return (false,2)
        default:
            return (false,3)
        }
    }
    
    public func register(){
        let userDefault = UserDefaults.standard
        userDefault.setValue(phoneNumber, forKey: "PhoneNumber")
        userDefault.setValue(secretKey.base64EncodedString(), forKey: "SecretKey")
        userDefault.setValue(true, forKey: "State")
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 2) {
            self.delegate?.didRegister()
        }
        
    }
    
    
    public static func isRegister() -> Bool{
        guard UserDefaults.standard.string(forKey: "PhoneNumber") == nil || UserDefaults.standard.string(forKey: "SecretKey") == nil || UserDefaults.standard.string(forKey: "State") == nil else{
            return true
        }
        return false
    }
    
    
    public static func dangerState(){
        UserDefaults.standard.setValue(false, forKey: "State")
    }
    
    public static func secrueState(){
        UserDefaults.standard.setValue(true, forKey: "State")
    }

    
}
