//
//  BLETransmitter.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation
import CoreBluetooth

/**
 信标发送器广播一个固定的服务UUID以启用iOS的后台扫描。当iOS进入后台模式，UUID将从广播中消失，因此Android设备需要先搜索Apple设备，然后连接并发现用于读取UUID的服务。
*/
protocol BLETransmitter : Sensor {
}

/**
 
 BLETransmitter的主要功能有两个：
 1.保持iOS设备之间连接的信令特征，并使没有发送功能的Android设备（仅接收，像三星J6一样）通过将信标代码和RSSI作为数据写入该特征，来使其他Pioneer设备知道它们的存在。
 2.发布信标标识数据的有效负载特征

保持Transmitter和Receiver在iOS后台模式下工作是一项重大挑战，尤其是当两个iOS设备都处于后台模式时。iOS上的Transmitter提供一个可被写入数据而触发的通知信标特性。当特征写入时，Transmitter将在8秒后调用updateValue通知Receiver，用didUpdateValueFor唤醒Receiver。这个过程可以循环重复在Trabsmitter和Receiver之间以保持两个设备处于唤醒状态。这对于可以完全依赖scanForPeripherals进行检测的Android通信是不必要的，Android - iOS和iOS -Android 检测同样。
 
 基于唤醒的通知方法依赖于一个开放的连接，这对于iOS来说似乎很好，但是可能会导致Android的问题。实验发现，Android设备不能接受新的连接（没有显式连接断开）并且蓝牙堆栈在大约500个打开的连接后停止工作。设备需要重新启动才能恢复。但是，如果每个连接都断开，蓝牙协议栈就可以工作无限期，但频繁的连接和断开仍然会导致相同的问题。建议是
 （1）工作完成后，请务必断开与Android的连接
 （2）尽量减少与Android的连接数
 （3）最大化连接之间的时间间隔。
 考虑到这些，Transmitter在Android上不支持这种通知模式，而且仅仅在第一次接触时才执行连接以获取有效负载。
 
 */
class SpecificBLETransmitter : NSObject, BLETransmitter, CBPeripheralManagerDelegate {
    private let logger = SpecificSensorLogger(subsystem: "Sensor", category: "BLE.SpecificBLETransmitter")
    private var delegates: [SensorDelegate] = []
    /// 所有BLETransmitter和BLEReceiver任务的专用队列
    private let queue: DispatchQueue
    private let delegateQueue: DispatchQueue
    private let database: BLEDatabase
    private let payloadDataSupplier: PayloadDataSupplier

    /// 用于管理所有连接的外围设备管理器，为简单起见，使用单个管理器。
    private var peripheral: CBPeripheralManager!
    /// BLETransmitter正在广播的信标服务和特性
    private var signalCharacteristic: CBMutableCharacteristic?
    private var payloadCharacteristic: CBMutableCharacteristic?
    private var advertisingStartedAt: Date = Date.distantPast

    /// 用于写入接收器以触发状态恢复或从挂起状态恢复到背景状态的伪数据。
    private let emptyData = Data(repeating: 0, count: 0)
    
    private var notifyTimer: DispatchSourceTimer?
    private let notifyTimerQueue = DispatchQueue(label: "Sensor.BLE.SpecificBLETransmitter.Timer")

    
    ///初始化BLETransmitter，该BLETransmitter使用与BLEReceiver相同的调度队列。启用蓝牙时，Transmitter自动启动。
    init(queue: DispatchQueue, delegateQueue: DispatchQueue, database: BLEDatabase, payloadDataSupplier: PayloadDataSupplier) {
        self.queue = queue
        self.delegateQueue = delegateQueue
        self.database = database
        self.payloadDataSupplier = payloadDataSupplier
        super.init()
        // 创建支持状态恢复的外围设备
        if peripheral == nil {
            //创建peripheral成功将调用代理对象的peripheralManagerDidUpdateState
            self.peripheral = CBPeripheralManager(delegate: self, queue: queue, options: [
                CBPeripheralManagerOptionRestoreIdentifierKey : "Sensor.BkLE.SpecificBLETransmitter",
                // 如果在蓝牙关闭时打开应用程序，将此设置为false可阻止iOS显示警报。
                CBPeripheralManagerOptionShowPowerAlertKey : true
            ])
        }
    }
    
    func add(delegate: SensorDelegate) {
        delegates.append(delegate)
    }
    
    func start() {
        logger.debug("start")
        guard peripheral != nil else {
            return
        }
        guard peripheral.state == .poweredOn else {
            logger.fault("start denied, not powered on")
            return
        }
        if signalCharacteristic != nil, payloadCharacteristic != nil {
            logger.debug("starting advert with existing characteristics")
            if !peripheral.isAdvertising {
                startAdvertising(withNewCharacteristics: false)
            } else {
                queue.async {
                    self.peripheral.stopAdvertising()
                    self.peripheral.startAdvertising([CBAdvertisementDataServiceUUIDsKey : [BLESensorConfiguration.serviceUUID]])
                }
            }
            logger.debug("start successful, for existing characteristics")
        } else {
            startAdvertising(withNewCharacteristics: true)
            logger.debug("start successful, for new characteristics")
        }
        signalCharacteristic?.subscribedCentrals?.forEach() { central in
            _ = database.device(central.identifier.uuidString)
        }
        notifySubscribers("start")
    }
    
    func stop() {
        logger.debug("stop")
        guard peripheral != nil else {
            return
        }
        guard peripheral.isAdvertising else {
            logger.fault("stop denied, already stopped (source=%s)")
            return
        }
        stopAdvertising()
    }
    
    private func startAdvertising(withNewCharacteristics: Bool) {
        logger.debug("startAdvertising (withNewCharacteristics=\(withNewCharacteristics))")
        if withNewCharacteristics || signalCharacteristic == nil || payloadCharacteristic == nil {
            signalCharacteristic = CBMutableCharacteristic(type: BLESensorConfiguration.iosSignalCharacteristicUUID, properties: [.write, .notify], value: nil, permissions: [.writeable])
            payloadCharacteristic = CBMutableCharacteristic(type: BLESensorConfiguration.payloadCharacteristicUUID, properties: [.read], value: nil, permissions: [.readable])
        }
        let service = CBMutableService(type: BLESensorConfiguration.serviceUUID, primary: true)
        signalCharacteristic?.value = nil
        payloadCharacteristic?.value = nil
        service.characteristics = [signalCharacteristic!, payloadCharacteristic!]
        queue.async {
            self.peripheral.stopAdvertising()
            self.peripheral.removeAllServices()
            self.peripheral.add(service)
            self.peripheral.startAdvertising([CBAdvertisementDataServiceUUIDsKey : [BLESensorConfiguration.serviceUUID]])
        }
    }
    
    private func stopAdvertising() {
        logger.debug("stopAdvertising()")
        queue.async {
            self.peripheral.stopAdvertising()
        }
        notifyTimer?.cancel()
        notifyTimer = nil
    }
    
    /// 所有工作都从通知订阅者循环开始。
    /// 在8秒后生成updateValue通知，通知所有订阅用户并保持iOS接收器处于唤醒状态。
    private func notifySubscribers(_ source: String) {
        notifyTimer?.cancel()
        notifyTimer = DispatchSource.makeTimerSource(queue: notifyTimerQueue)
        notifyTimer?.schedule(deadline: DispatchTime.now() + BLESensorConfiguration.notificationDelay)
        notifyTimer?.setEventHandler { [weak self] in
            guard let s = self, let logger = self?.logger, let signalCharacteristic = self?.signalCharacteristic else {
                return
            }
            s.queue.async {
                logger.debug("notifySubscribers (source=\(source))")
                s.peripheral.updateValue(s.emptyData, for: signalCharacteristic, onSubscribedCentrals: nil)
            }
            let advertUpTime = Date().timeIntervalSince(s.advertisingStartedAt)
            if s.peripheral.isAdvertising, advertUpTime > BLESensorConfiguration.TimeIntervalForAdvertRestart {
                logger.debug("advertRestart (upTime=\(advertUpTime))")
                s.startAdvertising(withNewCharacteristics: true)
            }
        }
        notifyTimer?.resume()
    }
    
    // MARK:- CBPeripheralManagerDelegate
    
    /// 恢复广播并恢复广播的特征。
    func peripheralManager(_ peripheral: CBPeripheralManager, willRestoreState dict: [String : Any]) {
        logger.debug("willRestoreState")
        self.peripheral = peripheral
        peripheral.delegate = self
        if let services = dict[CBPeripheralManagerRestoredStateServicesKey] as? [CBMutableService] {
            for service in services {
                logger.debug("willRestoreState (service=\(service.uuid.uuidString))")
                if let characteristics = service.characteristics {
                    for characteristic in characteristics {
                        logger.debug("willRestoreState (characteristic=\(characteristic.uuid.uuidString))")
                        switch characteristic.uuid {
                        case BLESensorConfiguration.androidSignalCharacteristicUUID:
                            if let mutableCharacteristic = characteristic as? CBMutableCharacteristic {
                                signalCharacteristic = mutableCharacteristic
                                logger.debug("willRestoreState (androidSignalCharacteristic=\(characteristic.uuid.uuidString))")
                            } else {
                                logger.fault("willRestoreState characteristic not mutable (androidSignalCharacteristic=\(characteristic.uuid.uuidString))")
                            }
                        case BLESensorConfiguration.iosSignalCharacteristicUUID:
                            if let mutableCharacteristic = characteristic as? CBMutableCharacteristic {
                                signalCharacteristic = mutableCharacteristic
                                logger.debug("willRestoreState (iosSignalCharacteristic=\(characteristic.uuid.uuidString))")
                            } else {
                                logger.fault("willRestoreState characteristic not mutable (iosSignalCharacteristic=\(characteristic.uuid.uuidString))")
                            }
                        case BLESensorConfiguration.payloadCharacteristicUUID:
                            if let mutableCharacteristic = characteristic as? CBMutableCharacteristic {
                                payloadCharacteristic = mutableCharacteristic
                                logger.debug("willRestoreState (payloadCharacteristic=\(characteristic.uuid.uuidString))")
                            } else {
                                logger.fault("willRestoreState characteristic not mutable (payloadCharacteristic=\(characteristic.uuid.uuidString))")
                            }
                        default:
                            logger.debug("willRestoreState (unknownCharacteristic=\(characteristic.uuid.uuidString))")
                        }
                    }
                }
            }
        }
    }

    
    
    func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        // Bluetooth on -> Advertise
        if (peripheral.state == .poweredOn) {
            logger.debug("Update state (state=poweredOn)")
            start()
        } else {
            logger.debug("Update state (state=\(peripheral.state.description))")
        }
    }
    
    func peripheralManagerDidStartAdvertising(_ peripheral: CBPeripheralManager, error: Error?) {
        logger.debug("peripheralManagerDidStartAdvertising (error=\(String(describing: error)))")
        if error == nil {
            advertisingStartedAt = Date()
        }
    }
    
    /**
     写请求提供了一种机制，使不可使用发送器的设备（例如，三星J6只能接收）能够通过提交信标码和RSSI作为数据来知道它的存在。这也为iOS提供了一种向Transmitter写入空白数据，使其从挂起状态恢复到后台状态以增加了它在后台长时间内扫描的时间而不被杀死。有效负载共享也基于写特性，使Android对等机充当共享iOS设备有效负载的桥梁，从而iOS-iOS背景检测无需位置许可或屏幕打开，只需要后台探测和跟踪的方法。
     */
    func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveWrite requests: [CBATTRequest]) {
        // Write -> Notify delegates -> Write response -> Notify subscribers
        let timstamp = Date()
        for request in requests {
            let targetIdentifier = TargetIdentifier(central: request.central)
            let targetDevice = database.device(targetIdentifier)
            logger.debug("didReceiveWrite (central=\(targetIdentifier))")
            if let data = request.value {
                if data.count == 0 {
                    // 接收机在检测到发射机时写入空白数据，使iOS发射机从挂起状态恢复
                    logger.debug("didReceiveWrite (central=\(targetIdentifier),action=wakeTransmitter)")
                    queue.async { peripheral.respond(to: request, withResult: .success) }
                } else if let actionCode = data.uint8(0) {
                    switch actionCode {
                    case BLESensorConfiguration.signalCharacteristicActionForPayload:
                        // 只接收写有有效负载的Android设备，以使其存在被了解
                        logger.debug("didReceiveWrite (central=\(targetIdentifier),action=writePayload)")
                        // writePayload data 格式
                        // 0-0 : actionCode
                        // 1-2 : payload data count in bytes (UInt16)
                        // 3.. : payload data
                        if let payloadDataCount = data.uint16(1) {
                            logger.debug("didReceiveWrite -> didDetect=\(targetIdentifier)")
                            delegateQueue.async {
                                self.delegates.forEach { $0.sensor(.BLE, didDetect: targetIdentifier) }
                            }
                            if data.count == (3 + payloadDataCount) {
                                let payloadData = PayloadData(data.subdata(in: 3..<data.count))
                                guard payloadData.isValid(timestamp: timstamp) else{
                                    logger.debug("didReceiveWrite is not valid -> didRead=\(payloadData.shortName),fromTarget=\(targetIdentifier)")
                                    return
                                }
                                logger.debug("didReceiveWrite -> didRead=\(payloadData.shortName),fromTarget=\(targetIdentifier)")
                                queue.async { peripheral.respond(to: request, withResult: .success) }
                                targetDevice.operatingSystem = .android
                                targetDevice.receiveOnly = true
                                targetDevice.payloadData = payloadData
                            } else {
                                logger.fault("didReceiveWrite, invalid payload (central=\(targetIdentifier),action=writePayload)")
                                queue.async { peripheral.respond(to: request, withResult: .invalidAttributeValueLength) }
                            }
                        } else {
                            logger.fault("didReceiveWrite, invalid request (central=\(targetIdentifier),action=writePayload)")
                            queue.async { peripheral.respond(to: request, withResult: .invalidAttributeValueLength) }
                        }
                    case BLESensorConfiguration.signalCharacteristicActionForRSSI:
                        //仅支持接收的Android设备写入起RSSI来使使其他设备知道该设备的接近程度
                        // Receive-only Android device writing its RSSI to make its proximity known
                        logger.debug("didReceiveWrite (central=\(targetIdentifier),action=writeRSSI)")
                        // writeRSSI data 格式
                        // 0-0 : actionCode
                        // 1-2 : rssi value (Int16)
                        if let rssi = data.int16(1) {
                            let proximity = Proximity(unit: .RSSI, value: Double(rssi), calibration: targetDevice.calibration)
                            logger.debug("didReceiveWrite -> didMeasure=\(proximity.description),fromTarget=\(targetIdentifier)")
                            queue.async { peripheral.respond(to: request, withResult: .success) }
                            targetDevice.operatingSystem = .android
                            targetDevice.receiveOnly = true
                            targetDevice.rssi = BLE_RSSI(rssi)
                        } else {
                            logger.fault("didReceiveWrite, invalid request (central=\(targetIdentifier),action=writeRSSI)")
                            queue.async { peripheral.respond(to: request, withResult: .invalidAttributeValueLength) }
                        }
                    case BLESensorConfiguration.signalCharacteristicActionForPayloadSharing:
                        // Android设备与此iOS设备共享检测到的iOS设备以达到后台检测的效果
                        logger.debug("didReceiveWrite (central=\(targetIdentifier),action=writePayloadSharing)")
                        // writePayloadSharing data 格式
                        // 0-0 : actionCode
                        // 1-2 : rssi value (Int16)
                        // 3-4 : payload sharing data count in bytes (UInt16)
                        // 5.. : payload sharing data (to be parsed by payload data supplier)
                        if let rssi = data.int16(1), let payloadDataCount = data.uint16(3) {
                            if data.count == (5 + payloadDataCount) {
                                let payloadSharingData = payloadDataSupplier.payload(data.subdata(in: 5..<data.count))
                                logger.debug("didReceiveWrite -> didShare=\(payloadSharingData.description),fromTarget=\(targetIdentifier)")
                                queue.async { peripheral.respond(to: request, withResult: .success) }
                                delegateQueue.async {
                                    self.delegates.forEach { $0.sensor(.BLE, didShare: payloadSharingData, fromTarget: targetIdentifier) }
                                }
                                targetDevice.operatingSystem = .android
                                targetDevice.rssi = BLE_RSSI(rssi)
                                payloadSharingData.forEach() { payloadData in
                                    let sharedDevice = database.device(payloadData)
                                    if sharedDevice.operatingSystem == .unknown {
                                        sharedDevice.operatingSystem = .shared
                                    }
                                    sharedDevice.rssi = BLE_RSSI(rssi)
                                }
                            } else {
                                logger.fault("didReceiveWrite, invalid payload (central=\(targetIdentifier),action=writePayloadSharing)")
                                queue.async { peripheral.respond(to: request, withResult: .invalidAttributeValueLength) }
                            }
                        } else {
                            logger.fault("didReceiveWrite, invalid request (central=\(targetIdentifier),action=writePayloadSharing)")
                            queue.async { peripheral.respond(to: request, withResult: .invalidAttributeValueLength) }
                        }
                    default:
                        logger.fault("didReceiveWrite (central=\(targetIdentifier),action=unknown,actionCode=\(actionCode))")
                        queue.async { peripheral.respond(to: request, withResult: .invalidAttributeValueLength) }
                    }
                }
            } else {
                queue.async { peripheral.respond(to: request, withResult: .invalidAttributeValueLength) }
            }
        }
        notifySubscribers("didReceiveWrite")
    }
    
    /// 来自中央设备的读取请求，用于从该外设获取有效负载数据。
    func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveRead request: CBATTRequest) {
        // Read -> Notify subscribers
        let central = database.device(TargetIdentifier(request.central.identifier.uuidString))
        switch request.characteristic.uuid {
        case BLESensorConfiguration.payloadCharacteristicUUID:
            logger.debug("Read (central=\(central.description),characteristic=payload,offset=\(request.offset))")
            let payloadDataSupplied = payloadDataSupplier.payload(PayloadTimestamp(), device: central)
            guard let payloadData = payloadDataSupplied else {
                logger.fault("Read, no payload data supplied (central=\(central.description),characteristic=payload,offset=\(request.offset),data=BLANK)")
                queue.async { peripheral.respond(to: request, withResult: .invalidOffset) }
                return
            }
            guard request.offset < payloadData.count else {
                logger.fault("Read, invalid offset (central=\(central.description),characteristic=payload,offset=\(request.offset),data=\(payloadData.count))")
                queue.async { peripheral.respond(to: request, withResult: .invalidOffset) }
                return
            }
            request.value = (request.offset == 0 ? payloadData.data : payloadData.subdata(in: request.offset..<payloadData.count))
            queue.async { peripheral.respond(to: request, withResult: .success) }
        default:
            logger.fault("Read (central=\(central.description),characteristic=unknown)")
            queue.async { peripheral.respond(to: request, withResult: .requestNotSupported) }
        }
        notifySubscribers("didReceiveRead")
    }
    
    /// 另一个iOS central已订阅此iOS外围设备，这意味着central也是此设备要连接的外围设备。
    func peripheralManager(_ peripheral: CBPeripheralManager, central: CBCentral, didSubscribeTo characteristic: CBCharacteristic) {
        // Subscribe -> Notify subscribers
        // iOS接收器订阅第一次接触时的信号特征。这样可以确保Transmitter和Receiver在第一次更新时被唤醒，而未来的循环将依赖didReceiveWrite作为触发器。
        logger.debug("Subscribe (central=\(central.identifier.uuidString))")
        _ = database.device(central.identifier.uuidString)
        notifySubscribers("didSubscribeTo")
    }
    
    func peripheralManager(_ peripheral: CBPeripheralManager, central: CBCentral, didUnsubscribeFrom characteristic: CBCharacteristic) {
        // Unsubscribe -> Notify subscribers
        logger.debug("Unsubscribe (central=\(central.identifier.uuidString))")
        _ = database.device(central.identifier.uuidString)
        notifySubscribers("didUnsubscribeFrom")
    }
}
