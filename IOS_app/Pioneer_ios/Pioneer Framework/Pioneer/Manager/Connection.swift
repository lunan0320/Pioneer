//
//  Connection.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Network
import Foundation

public class Connection{
    
    private let queue: DispatchQueue
    private let host: NWEndpoint.Host
    private let port: NWEndpoint.Port
    public var connection: NWConnection?
    public var decription: String { get {
        return "Not set"
    }
    }
    public init(queue: DispatchQueue){
        self.queue = queue
        self.host = NWEndpoint.Host.init(GlobalConfiguration.serverHost)
        self.port = NWEndpoint.Port(integerLiteral: NWEndpoint.Port.IntegerLiteralType(GlobalConfiguration.serverPort))
        self.connection = NWConnection.init(host: host, port: port, using: createTLSParameters(queue: queue))
    }
    
    public init(host:String,port:IntegerLiteralType,queue:DispatchQueue){
        self.queue = queue
        self.host = NWEndpoint.Host.init(host)
        self.port = NWEndpoint.Port(integerLiteral: NWEndpoint.Port.IntegerLiteralType(port))
        self.connection = NWConnection.init(host: self.host, port: self.port, using: createTLSParameters(queue: self.queue))
    }
    
    public func start(){
        connection?.stateUpdateHandler = { newState in
            switch newState{
                case .ready:
                    print("state ready")
                case .cancelled:
                    print("state cancel")
                case .waiting(let error):
                    print("state waiting \(error)")
                case .failed(let error):
                    print("state failed \(error)")
                default:
                    break
            }
        }
        connection?.start(queue: queue)
    }
    public func state() -> NWConnection.State?{
        return connection?.state
    }
    public func restart(){
        connection?.restart()
    }
    
    public func cancel(){
        connection?.cancel()
    }
    
    public func send(dataMessage:String) -> Bool{
        var isSuccess: Bool = false
        let semaphore = DispatchSemaphore(value: 0)
        let processedData: Data = dataMessage.data(using: .utf8)!
        connection?.send(content: processedData, contentContext: .defaultMessage, isComplete: true, completion: .contentProcessed({error in
            guard error == nil else{
                print("Send Error Message:\(String(describing: error?.localizedDescription))")
                semaphore.signal()
                return
            }
            print("Send Success!")
            isSuccess = true
            semaphore.signal()
        }))
        _ = semaphore.wait(timeout: DispatchTime.distantFuture)
        return isSuccess
    }
    
    public func recieve() -> String?{
        let semaphore = DispatchSemaphore(value: 0)
        var recievedValue: String? = nil
        connection?.receive(minimumIncompleteLength: 0, maximumLength: 65535, completion:{ (recieveData, contentContext, isBool, error) in
            guard error == nil else{
                print("Recieve Error Message:\(String(describing: error?.localizedDescription))")
                semaphore.signal()
                return
            }
            print("Recieve Success!")
            print("ContentContext:" + contentContext!.identifier)
            guard let recieveMessage = recieveData else{
                semaphore.signal()
                return
            }
            if let recievedString = String.init(data: recieveMessage.suffix(from: 2), encoding: .utf8) {
                print("Recieve message:" + recievedString)
                recievedValue = recievedString
            }
            semaphore.signal()
        })
        _ = semaphore.wait(timeout: DispatchTime.distantFuture)
        return recievedValue
    }

    private func createTLSParameters(queue: DispatchQueue) -> NWParameters{
        let options = NWProtocolTLS.Options()
        sec_protocol_options_set_verify_block(options.securityProtocolOptions,{(sec_protocol_metadata, sec_trust, sec_protocol_verify_complete) in
            let trust = sec_trust_copy_ref(sec_trust).takeRetainedValue()
            var error: CFError?
            if SecTrustEvaluateWithError(trust, &error){
                sec_protocol_verify_complete(true)
            }else{
                let certificate = SecTrustGetCertificateAtIndex(trust, 0)!
                let remoteCeritificateData = CFBridgingRetain(SecCertificateCopyData(certificate))!
                let cerPath = Bundle.main.path(forResource: GlobalConfiguration.serverCertificate, ofType: "cer")!
                let cerUrl = URL(fileURLWithPath: cerPath)
                let localCertificateData: Data = try! Data(contentsOf: cerUrl)
                
                if (remoteCeritificateData.isEqual(localCertificateData) == true){
                    sec_protocol_verify_complete(true)
                }else{
                    sec_protocol_verify_complete(false)
                }
            }}, queue)

        sec_protocol_options_set_challenge_block(options.securityProtocolOptions, {(sec_protocol_metadata_t, sec_protocol_challenge_complete_t) in
            var securityError: OSStatus = errSecSuccess
            let path: String = Bundle.main.path(forResource: GlobalConfiguration.clientCertificate, ofType: "p12")!
            let PKCS12Data = NSData(contentsOfFile: path)!
            let key: NSString = kSecImportExportPassphrase as NSString
            let key_options: NSDictionary = [key:GlobalConfiguration.clientCertificatePass]
            var items: CFArray?
            securityError = SecPKCS12Import(PKCS12Data, key_options, &items)
            
            if securityError == errSecSuccess{
                let certItems: CFArray = items!
                let cerItemsArray: Array = certItems as Array
                let dict: AnyObject? = cerItemsArray.first
                if let certEntry: Dictionary = dict as? Dictionary<String, AnyObject>{
                    let identityPointer: AnyObject? = certEntry["identity"]
                    let secIdentityRef: SecIdentity = (identityPointer as! SecIdentity?)!
                    let secIdentity: sec_identity_t? = sec_identity_create(secIdentityRef)
                    sec_protocol_challenge_complete_t(secIdentity)
                }
            }
        }, queue)
        return NWParameters(tls: options)
    }
    
}

