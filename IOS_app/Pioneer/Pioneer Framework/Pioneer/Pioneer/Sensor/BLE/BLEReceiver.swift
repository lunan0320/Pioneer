//
//  BLEReceiver.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation
import CoreBluetooth
import os

/**
 信标接收器扫描具有固定服务UUID的外围设备。
 */
protocol BLEReceiver : Sensor {
}

/**
 信标接收器在前台和后台模式下扫描具有固定服务UUID的外围设备。Android设备的后台扫描是不需要特殊考虑的因为scanForPeripherals每次调用都会返回所有存在的Android设备。iOS后台扫描因后台模式下的复杂性，需要开放连接才能订阅用作触发器而将两个iOS设备保持在后台状态（而不是挂起或者被杀）的通知特性。对于iOS-iOS设备，一旦检测到，接收器将
 （1）向发送器写入空白数据，从而触发发送器在8秒后发送特征数据更新，然后
 （2）触发接收器接收更新通知
 （3）以此为 readRSSI调用创建机会，并重复此循环过程，使两者保持唤醒。
 请注意，iOS进程不可靠如果
 （1）用户通过飞行模式设置关闭蓝牙，
 （2）设备重新启动，
 （3）如果应用程序被用户杀死，蓝牙状态将完全失去不能恢复。
 */
class SpecificBLEReceiver: NSObject, BLEReceiver, BLEDatabaseDelegate, CBCentralManagerDelegate, CBPeripheralDelegate {
    private let logger = SpecificSensorLogger(subsystem: "Sensor", category: "BLE.SpecificBLEReceiver")
    private var delegates: [SensorDelegate] = []
    /// 所有BLETransmitter和BLEReceiver任务的专用队列
    private let queue: DispatchQueue!
    private let delegateQueue: DispatchQueue
    private let database: BLEDatabase
    private let payloadDataSupplier: PayloadDataSupplier
    private var central: CBCentralManager!
    private let emptyData = Data(repeating: 0, count: 0)
    
    private var scanTimer: DispatchSourceTimer?
    private let scanTimerQueue = DispatchQueue(label: "Sensor.BLE.SpecificBLEReceiver.ScanTimer")
    private let scheduleScanQueue = DispatchQueue(label: "Sensor.BLE.SpecificBLEReceiver.ScheduleScan")
    /// 用于记录发现的设备的扫描结果队列，记录没有立即挂起的操作的设备。
    private var scanResults: [BLEDevice] = []
    
    
    ///初始化BLEReceiver，该接收器与发送器共享相同的顺序调度队列，因为同时进行传输和接收操作影响蓝牙稳定性。接收器和发射器共享一个设备的公共数据库。
    required init(queue: DispatchQueue, delegateQueue: DispatchQueue, database: BLEDatabase, payloadDataSupplier: PayloadDataSupplier) {
        self.queue = queue
        self.delegateQueue = delegateQueue
        self.database = database
        self.payloadDataSupplier = payloadDataSupplier
        super.init()
        if central == nil {
            self.central = CBCentralManager(delegate: self, queue: queue, options: [
                CBCentralManagerOptionRestoreIdentifierKey : "Sensor.BLE.SpecificBLEReceiver",
                CBCentralManagerOptionShowPowerAlertKey : true]
            )
        }
        database.add(delegate: self)
    }
    
    func add(delegate: SensorDelegate) {
        delegates.append(delegate)
    }
    
    func start() {
        logger.debug("start")
        guard central != nil else {
            return
        }
        if central.state == .poweredOn {
            scan("start")
        }
    }
    
    func stop() {
        logger.debug("stop")
        guard central != nil else {
            return
        }
        guard central.isScanning else {
            logger.fault("stop denied, already stopped")
            return
        }
        scanTimer?.cancel()
        scanTimer = nil
        queue.async {
            self.central.stopScan()
        }
        database.devices().forEach() { device in
            if let peripheral = device.peripheral, peripheral.state != .disconnected {
                disconnect("stop", peripheral)
            }
        }
    }
    
   
    
    // MARK:- 扫描外设并在需要时建立连接
    
    /// 所有的工作都基于scan循环
    func scan(_ source: String) {
        logger.debug("scan (source=\(source)")
        guard central.state == .poweredOn else {
            logger.fault("scan failed, bluetooth is not powered on")
            return
        }
        // 扫描广播Pioneer服务的外围设备。
        // 这将找到所有Android和iOS前台广告
        // 但它将错过iOS的后台广播，除非行程传感器已启用，屏幕暂时打开。
        queue.async { self.taskScanForPeripherals() }
        // 注册已经连接正在广播传感器服务的设备。
        //它捕捉到在状态恢复时可能被CoreBooth遗漏或内部错误的孤立的设备。
        queue.async { self.taskRegisterConnectedPeripherals() }
        //        解析通过发送器获得的设备标识的外围设备。当iOS中心设备连接到此外围设备，发送器代码注册中心设备的地址作为新设备并挂至此处以建立对等连接。这可以启用检测另一个的设备（例如，屏幕打开时）的设备并触发两个设备以相互检测。
        queue.async { self.taskResolveDevicePeripherals() }
        //删除一段时间没有看到的设备，大约20分钟后标识符就会改变，因此，维护一个连接是浪费的。
        queue.async { self.taskRemoveExpiredDevices() }
        
        queue.async { self.taskRemoveDuplicatePeripherals() }
        // iOS devices are kept in background state indefinitely
        // (instead of dropping into suspended or terminated state)
        // by a series of time delayed BLE operations. While this
        // device is awake, it will write data to other iOS devices
        // to keep them awake, and vice versa.
        //iOS设备无限期地保持在后台状态（而不是进入暂停或终止状态）通过一系列延时的BLE操作。当这个设备处于唤醒状态，它将向其它iOS设备写入数据让他们保持唤醒，反之亦然。
        queue.async { self.taskWakeTransmitters() }
        
        queue.async { self.taskIosMultiplex() }
//        如果发现的设备有挂起的任务，将连接到该设备。绝大多数设备将立即连接发现，如果他们有一个挂起的任务（例如，建立其操作系统或读取其有效负载）。
//        但如果他们已经完全完成了任务，就不会有悬而未决的任务（例如，有操作系统、有效负载和最近的RSSI测量），它们被放入扫描结果队列中通过此连接任务进行定期检查（例如，如果RSSI现有值现在已过期）。
        queue.async { self.taskConnect() }
       
        scheduleScan("scan")
    }
    
    
     ///在延迟8秒后安排信标扫描，以便状态从后台更改为挂起之前再次开始扫描。
    private func scheduleScan(_ source: String) {
        scheduleScanQueue.sync {
            scanTimer?.cancel()
            scanTimer = DispatchSource.makeTimerSource(queue: scanTimerQueue)
            scanTimer?.schedule(deadline: DispatchTime.now() + BLESensorConfiguration.notificationDelay)
            scanTimer?.setEventHandler { [weak self] in
                self?.scan("scheduleScan|"+source)
            }
            scanTimer?.resume()
        }
    }
    
    /**
     扫描广播信标服务的外围设备。
     */
    private func taskScanForPeripherals() {
        // Scan for peripherals -> didDiscover
        let scanForServices: [CBUUID] = [BLESensorConfiguration.serviceUUID]
        central.scanForPeripherals(
            withServices: scanForServices,
            options: [CBCentralManagerScanOptionSolicitedServiceUUIDsKey: [BLESensorConfiguration.serviceUUID]])
    }
    
    /**
     注册所有连接的将Pioneer服务作为设备进行广播的外围设备。
     */
    private func taskRegisterConnectedPeripherals() {
        central.retrieveConnectedPeripherals(withServices: [BLESensorConfiguration.serviceUUID]).forEach() { peripheral in
            let targetIdentifier = TargetIdentifier(peripheral: peripheral)
            let device = database.device(targetIdentifier)
            if device.peripheral == nil || device.peripheral != peripheral {
                logger.debug("taskRegisterConnectedPeripherals (device=\(device))")
                _ = database.device(peripheral, delegate: self)
            }
        }
    }

    /**
     解析所有数据库中设备的外围设备。这将启用对称连接功能。
     */
    private func taskResolveDevicePeripherals() {
        let devicesToResolve = database.devices().filter { $0.peripheral == nil }
        devicesToResolve.forEach() { device in
            guard let identifier = UUID(uuidString: device.identifier) else {
                return
            }
            let peripherals = central.retrievePeripherals(withIdentifiers: [identifier])
            if let peripheral = peripherals.last {
                logger.debug("taskResolveDevicePeripherals (resolved=\(device))")
                _ = database.device(peripheral, delegate: self)
            }
        }
    }
    
    /**
     删除一段时间未更新的设备，因为UUID可能在超出范围超过20分钟后发生更改，因此需要重新发现。
     */
    private func taskRemoveExpiredDevices() {
        let devicesToRemove = database.devices().filter { Date().timeIntervalSince($0.lastUpdatedAt) > BLESensorConfiguration.TimeIntervalPeripheralClean }
        devicesToRemove.forEach() { device in
            logger.debug("taskRemoveExpiredDevices (remove=\(device))")
            database.delete(device.identifier)
            if let peripheral = device.peripheral {
                disconnect("taskRemoveExpiredDevices", peripheral)
            }
        }
    }
    
    /**
     删除具有相同有效负载数据但不同外围设备的设备。
     */
    private func taskRemoveDuplicatePeripherals() {
        var index: [PayloadData:BLEDevice] = [:]
        let devices = database.devices()
        devices.forEach() { device in
            guard let payloadData = device.payloadData else {
                return
            }
            guard let duplicate = index[payloadData] else {
                index[payloadData] = device
                return
            }
            var keeping = device
            if device.peripheral != nil, duplicate.peripheral == nil {
                keeping = device
            } else if duplicate.peripheral != nil, device.peripheral == nil {
                keeping = duplicate
            } else if device.payloadDataLastUpdatedAt > duplicate.payloadDataLastUpdatedAt {
                keeping = device
            } else {
                keeping = duplicate
            }
            let discarding = (keeping.identifier == device.identifier ? duplicate : device)
            index[payloadData] = keeping
            database.delete(discarding.identifier)
            self.logger.debug("taskRemoveDuplicatePeripherals (payload=\(payloadData.shortName),device=\(device.identifier),duplicate=\(duplicate.identifier),keeping=\(keeping.identifier))")
        }
    }
    
    /**
     唤醒所有连接的iOS设备的Transmitter
     */
    private func taskWakeTransmitters() {
        database.devices().forEach() { device in
            guard device.operatingSystem == .ios, let peripheral = device.peripheral, peripheral.state == .connected else {
                return
            }
            guard device.timeIntervalSinceLastUpdate < TimeInterval.minute else {
                connect("taskWakeTransmitters", peripheral)
                return
            }
            wakeTransmitter("taskWakeTransmitters", device)
        }
    }
    
    /**
     连接到设备并保持并发连接最大配额
     */
    private func taskConnect() {
        let didDiscover = taskConnectScanResults()
        
        let hasPendingTask = didDiscover.filter({ deviceHasPendingTask($0) })
        
        let toBeRefreshed = database.devices().filter({ !hasPendingTask.contains($0) && $0.peripheral?.state == .connected })
        let asymmetric = database.devices().filter({ !hasPendingTask.contains($0) && $0.operatingSystem == .unknown && $0.peripheral?.state != .connected })
        
        hasPendingTask.forEach() { device in
            guard let peripheral = device.peripheral else {
                return
            }
            connect("taskConnect|hasPending", peripheral);
        }

        toBeRefreshed.forEach() { device in
            guard let peripheral = device.peripheral else {
                return
            }
            connect("taskConnect|refresh", peripheral);
        }
        asymmetric.forEach() { device in
            guard let peripheral = device.peripheral else {
                return
            }
            connect("taskConnect|asymmetric", peripheral);
        }
    }

    /// 清空扫描结果以生成最近发现的设备列表，以便进行连接和处理
    private func taskConnectScanResults() -> [BLEDevice] {
        var set: Set<BLEDevice> = []
        var list: [BLEDevice] = []
        while let device = scanResults.popLast() {
            if set.insert(device).inserted, let peripheral = device.peripheral, peripheral.state != .connected {
                list.append(device)
                logger.debug("taskConnectScanResults, didDiscover (device=\(device))")
            }
        }
        return list
    }
    
    
    ///检查设备是否有挂起的任务
    private func deviceHasPendingTask(_ device: BLEDevice) -> Bool {
      
        if device.operatingSystem == .unknown || device.operatingSystem == .restored {
            return true
        }
        
        if device.payloadData == nil {
            return true
        }
        
        if device.timeIntervalSinceLastPayloadDataUpdate > BLESensorConfiguration.TimeIntervalForPayloadDataUpdate {
            return true
        }
        
        if device.operatingSystem == .ios, let peripheral = device.peripheral, peripheral.state != .connected {
            return true
        }
        return false
    }
    
    /// 检查iOS设备是否正在等待连接，如果需要，检查可用容量
    private func taskIosMultiplex() {
       
        let devices = database.devices().filter({ $0.operatingSystem == .ios && $0.peripheral != nil })
        
        let connected = devices.filter({ $0.peripheral?.state == .connected }).sorted(by: { $0.timeIntervalBetweenLastConnectedAndLastAdvert > $1.timeIntervalBetweenLastConnectedAndLastAdvert })
        
        let pending = devices.filter({ $0.peripheral?.state != .connected }).sorted(by: { $0.lastConnectRequestedAt < $1.lastConnectRequestedAt })
        logger.debug("taskIosMultiplex summary (connected=\(connected.count),pending=\(pending.count))")
        connected.forEach() { device in
            logger.debug("taskIosMultiplex, connected (device=\(device.description),upTime=\(device.timeIntervalBetweenLastConnectedAndLastAdvert))")
        }
        pending.forEach() { device in
            logger.debug("taskIosMultiplex, pending (device=\(device.description),downTime=\(device.timeIntervalSinceLastConnectRequestedAt))")
        }
        
        // 如果有剩余容量，请重试所有挂起的连接
        if connected.count < BLESensorConfiguration.maxConcurrentConnection {
            pending.forEach() { device in
                guard let toBeConnected = device.peripheral else {
                    return
                }
                connect("taskIosMultiplex|retry", toBeConnected);
            }
        }
        // 达到容量时启动多路复用
        guard connected.count > BLESensorConfiguration.maxConcurrentConnection, pending.count > 0, let deviceToBeDisconnected = connected.first, let peripheralToBeDisconnected = deviceToBeDisconnected.peripheral, deviceToBeDisconnected.timeIntervalBetweenLastConnectedAndLastAdvert > TimeInterval.minute else {
            return
        }
        logger.debug("taskIosMultiplex, multiplexing (toBeDisconnected=\(deviceToBeDisconnected.description))")
        disconnect("taskIosMultiplex", peripheralToBeDisconnected)
        pending.forEach() { device in
            guard let toBeConnected = device.peripheral else {
                return
            }
            connect("taskIosMultiplex|multiplex", toBeConnected);
        }
    }
    

    /// 根据当前状态和设备记录的可用信息在外围设备上启动下一个操作
    private func taskNextAction(_ source: String, peripheral: CBPeripheral) {
        let device = database.device(peripheral, delegate: self)
        if device.rssi == nil {
            // 1. RSSI
            logger.debug("taskNextAction (goal=rssi,device=\(device))")
            readRSSI("taskNextAction|" + source, peripheral)
        } else if !(device.signalCharacteristic != nil && device.payloadCharacteristic != nil) {
            // 2. Characteristics
            logger.debug("taskNextAction (goal=characteristics,device=\(device))")
            discoverServices("taskNextAction|" + source, peripheral)
        } else if device.payloadData == nil {
            // 3. Payload
            logger.debug("taskNextAction (goal=payload,device=\(device))")
            readPayload("taskNextAction|" + source, device)
        } else if device.timeIntervalSinceLastPayloadDataUpdate > BLESensorConfiguration.TimeIntervalForPayloadDataUpdate {
            // 4. Payload update
            logger.debug("taskNextAction (goal=payloadUpdate,device=\(device),elapsed=\(device.timeIntervalSinceLastPayloadDataUpdate))")
            readPayload("taskNextAction|" + source, device)
        } else if device.operatingSystem != .ios {
            // 5. Disconnect Android
            logger.debug("taskNextAction (goal=disconnect|\(device.operatingSystem.rawValue),device=\(device))")
            disconnect("taskNextAction|" + source, peripheral)
        } else {
            // 6. Scan
            logger.debug("taskNextAction (goal=scan,device=\(device))")
            scheduleScan("taskNextAction|" + source)
        }
    }
    
    /**
     连接外设。根据Apple文档的建议，在启动connect之前，扫描会暂时停止，否则挂起的扫描操作往往具有优先权，连接需要更长的时间才能启动。扫描计划稍后继续，以确保扫描恢复，即使连接失败。
     */
    private func connect(_ source: String, _ peripheral: CBPeripheral) {
        let device = database.device(peripheral, delegate: self)
        logger.debug("connect (source=\(source),device=\(device))")
        guard central.state == .poweredOn else {
            logger.fault("connect denied, central not powered on (source=\(source),device=\(device))")
            return
        }
        queue.async {
            device.lastConnectRequestedAt = Date()
            self.central.retrievePeripherals(withIdentifiers: [peripheral.identifier]).forEach {
                if $0.state != .connected {
                    
                    if let lastAttempt = device.lastConnectionInitiationAttempt {
                        
                        if (Date() > lastAttempt + BLESensorConfiguration.toConnectionTimeout) {
                            
                            self.logger.fault("connect, timeout forcing disconnect (source=\(source),device=\(device),elapsed=\(-lastAttempt.timeIntervalSinceNow))")
                            device.lastConnectionInitiationAttempt = nil
                            self.queue.async { self.central.cancelPeripheralConnection(peripheral) }
                        } else {
                            
                            self.logger.debug("connect, retrying (source=\(source),device=\(device),elapsed=\(-lastAttempt.timeIntervalSinceNow))")
                            self.central.connect($0)
                        }
                    } else {
                        
                        self.logger.debug("connect, initiation (source=\(source),device=\(device))")
                        device.lastConnectionInitiationAttempt = Date()
                        self.central.connect($0)
                    }
                } else {
                    self.taskNextAction("connect|" + source, peripheral: $0)
                }
            }
        }
        scheduleScan("connect")
    }
    
    /**
    
     断开外围设备。断开连接时，将为iOS设备发出连接请求，以保持开放的连接，Android没有进一步的行动。
     */
    private func disconnect(_ source: String, _ peripheral: CBPeripheral) {
        let targetIdentifier = TargetIdentifier(peripheral: peripheral)
        logger.debug("disconnect (source=\(source),peripheral=\(targetIdentifier))")
        guard peripheral.state == .connected || peripheral.state == .connecting else {
            logger.fault("disconnect denied, peripheral not connected or connecting (source=\(source),peripheral=\(targetIdentifier),state=\(peripheral.state))")
            return
        }
        queue.async { self.central.cancelPeripheralConnection(peripheral) }
    }
    
    /// 读取RSSI
    private func readRSSI(_ source: String, _ peripheral: CBPeripheral) {
        let targetIdentifier = TargetIdentifier(peripheral: peripheral)
        logger.debug("readRSSI (source=\(source),peripheral=\(targetIdentifier))")
        guard peripheral.state == .connected else {
            logger.fault("readRSSI denied, peripheral not connected (source=\(source),peripheral=\(targetIdentifier))")
            scheduleScan("readRSSI")
            return
        }
        queue.async { peripheral.readRSSI() }
    }
    
    /// 搜索Pioneer服务
    private func discoverServices(_ source: String, _ peripheral: CBPeripheral) {
        let targetIdentifier = TargetIdentifier(peripheral: peripheral)
        logger.debug("discoverServices (source=\(source),peripheral=\(targetIdentifier))")
        guard peripheral.state == .connected else {
            logger.fault("discoverServices denied, peripheral not connected (source=\(source),peripheral=\(targetIdentifier))")
            scheduleScan("discoverServices")
            return
        }
        queue.async {
            let services: [CBUUID] = [BLESensorConfiguration.serviceUUID]
            peripheral.discoverServices(services)
        }
    }
    
    /// 从设备中读取Payload
    private func readPayload(_ source: String, _ device: BLEDevice) {
        logger.debug("readPayload (source=\(source),peripheral=\(device.identifier))")
        guard let peripheral = device.peripheral, peripheral.state == .connected else {
            logger.fault("readPayload denied, peripheral not connected (source=\(source),peripheral=\(device.identifier))")
            return
        }
        guard let payloadCharacteristic = device.payloadCharacteristic != nil ? device.payloadCharacteristic : nil  else {
            logger.fault("readPayload denied, device missing payload characteristic (source=\(source),peripheral=\(device.identifier))")
            discoverServices("readPayload", peripheral)
            return
        }
        let timeIntervalSinceLastReadPayloadRequestedAt = Date().timeIntervalSince(device.lastReadPayloadRequestedAt)
        guard timeIntervalSinceLastReadPayloadRequestedAt > 2 else {
            logger.fault("readPayload denied, duplicate request (source=\(source),peripheral=\(device.identifier),elapsed=\(timeIntervalSinceLastReadPayloadRequestedAt)")
            return
        }
        // Initiate read payload
        device.lastReadPayloadRequestedAt = Date()
        if device.operatingSystem == .android, let peripheral = device.peripheral {
            discoverServices("readPayload|android", peripheral)
        } else {
            queue.async { peripheral.readValue(for: payloadCharacteristic) }
        }
    }
    

    /**
     通过向信标特性写入空白数据唤醒发射机。这将触发发送器在8秒内生成数据值更新通知，这将触发此接收器接收didUpdateValueFor调用，以保持发射器和接收器处于唤醒状态，同时最大化蓝牙呼叫之间的时间间隔，以最大限度地降低功耗。
     */
    private func wakeTransmitter(_ source: String, _ device: BLEDevice) {
        guard device.operatingSystem == .ios, let peripheral = device.peripheral, let characteristic = device.signalCharacteristic else {
            return
        }
        logger.debug("wakeTransmitter (source=\(source),peripheral=\(device.identifier),write=\(characteristic.properties.contains(.write))")
        queue.async { peripheral.writeValue(self.emptyData, for: characteristic, type: .withResponse) }
    }
    
    // MARK:- BLEDatabaseDelegate
    
    func bleDatabase(didCreate device: BLEDevice) {
//        特点：写时对称连接
//        BLETransmitter中的所有CoreBluetooth代理回调都将注册与此外设交互的中心
//        并在这里生成一个didCreate回调来触发scan，其中包括一个用于解析所有
//        实际外设的设备标识符。
        scheduleScan("bleDatabase:didCreate (device=\(device.identifier))")
    }
    
    // MARK:- CBCentralManagerDelegate
    
    /// 恢复状态后恢复设备
    func centralManager(_ central: CBCentralManager, willRestoreState dict: [String : Any]) {
        
        logger.debug("willRestoreState")
        self.central = central
        central.delegate = self
        if let restoredPeripherals = dict[CBCentralManagerRestoredStatePeripheralsKey] as? [CBPeripheral] {
            for peripheral in restoredPeripherals {
                let targetIdentifier = TargetIdentifier(peripheral: peripheral)
                let device = database.device(peripheral, delegate: self)
                if device.operatingSystem == .unknown {
                    device.operatingSystem = .restored
                }
                if peripheral.state == .connected {
                    device.lastConnectedAt = Date()
                }
                logger.debug("willRestoreState (peripheral=\(targetIdentifier))")
            }
        }
        
    }
    
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        // Bluetooth on -> Scan
        if (central.state == .poweredOn) {
            logger.debug("Update state (state=poweredOn))")
            delegateQueue.async {
                self.delegates.forEach({ $0.sensor(.BLE, didUpdateState: .on) })
            }
            scan("updateState")
        } else {
            logger.debug("Update state (state=\(central.state.description))")
            delegateQueue.async {
                self.delegates.forEach({ $0.sensor(.BLE, didUpdateState: .off) })
            }
            
        }
    }
    
    /// 在具有相同伪设备地址的设备之间共享有效负载数据
    private func shareDataAcrossDevices(_ pseudoDeviceAddress: BLEPseudoDeviceAddress) {
        
        let devicesWithSamePseudoAddress = database.devices().filter({ pseudoDeviceAddress.address == $0.pseudoDeviceAddress?.address && $0.timeIntervalSinceCreated <= BLESensorConfiguration.TimeIntervalForAndroidAdvertRefresh })
        
        guard let mostRecentDevice = devicesWithSamePseudoAddress.filter({ $0.payloadData != nil }).sorted(by: { $0.payloadDataLastUpdatedAt > $1.payloadDataLastUpdatedAt }).first, let payloadData = mostRecentDevice.payloadData else {
            return
        }
        
        let payloadDataLastUpdatedAt = mostRecentDevice.payloadDataLastUpdatedAt
        let devicesToCopyPayload = devicesWithSamePseudoAddress.filter({ $0.payloadData == nil })
        devicesToCopyPayload.forEach({
            $0.signalCharacteristic = mostRecentDevice.signalCharacteristic
            $0.payloadCharacteristic = mostRecentDevice.payloadCharacteristic
            $0.operatingSystem = .android
            $0.payloadData = payloadData
            $0.payloadDataLastUpdatedAt = payloadDataLastUpdatedAt
            logger.debug("shareDataAcrossDevices, copied payload data (from=\(mostRecentDevice.description),to=\($0.description))")
        })
        
        let devicesWithSamePayload = database.devices().filter({ payloadData == $0.payloadData })
        let devicesToCopyAddress = devicesWithSamePayload.filter({ $0.pseudoDeviceAddress == nil })
        devicesToCopyAddress.forEach({
            $0.pseudoDeviceAddress = pseudoDeviceAddress
            logger.debug("shareDataAcrossDevices, copied pseudo address (payloadData=\(payloadData.shortName),to=\($0.description))")
        })
    }
    
    
    /// 设备发现将触发连接以解析操作系统并读取iOS和Android设备的有效负载。
    /// 对于正在进行的RSSI测量，iOS设备的连接保持活动状态，对于Android设备的连接保持关闭状态，因为
    /// iOS设备可以依赖此发现回调（由常规扫描调用触发）来提供持续的RSSI和TX电源更新，从而消除了对Android设备保持连接打开的需要，这对Android设备来说可能会导致系统的稳定性问题。
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        let device = database.device(peripheral, delegate: self)
        device.lastDiscoveredAt = Date()
        device.rssi = BLE_RSSI(RSSI.intValue)
        if let pseudoDeviceAddress = BLEPseudoDeviceAddress(fromAdvertisementData: advertisementData) {
            device.pseudoDeviceAddress = pseudoDeviceAddress
            shareDataAcrossDevices(pseudoDeviceAddress)
        }
        if let txPower = (advertisementData[CBAdvertisementDataTxPowerLevelKey] as? NSNumber)?.intValue {
            device.txPower = BLE_TxPower(txPower)
        }
        logger.debug("didDiscover (device=\(device),rssi=\((String(describing: device.rssi))),txPower=\((String(describing: device.txPower))))")
        if deviceHasPendingTask(device) {
            connect("didDiscover", peripheral);
        } else {
            scanResults.append(device)
        }
        scheduleScan("didDiscover")
    }
    
    /// 成功连接到设备将启动下一个挂起的操作。
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        // connect -> readRSSI -> discoverServices
        let device = database.device(peripheral, delegate: self)
        device.lastConnectedAt = Date()
        logger.debug("didConnect (device=\(device))")
        taskNextAction("didConnect", peripheral: peripheral)
    }
    
    
    /// 未能连接到设备将导致无效设备取消注册或以其他方式尝试重新连接。
    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        
        let device = database.device(peripheral, delegate: self)
        logger.debug("didFailToConnect (device=\(device),error=\(String(describing: error)))")
        if String(describing: error).contains("Device is invalid") {
            logger.debug("Unregister invalid device (device=\(device))")
            database.delete(device.identifier)
        } else {
            connect("didFailToConnect", peripheral)
        }
    }

    ///正常断开通常是由于设备超出范围或设备更改标识而导致的，因此会启动重新连接调用
    ///在这里，iOS设备可以在可能的情况下恢复连接。这对于Android设备来说是不必要的，因为它们可以被用户重新发现
    ///常规扫描。请注意，重新连接到iOS设备可能会在长时间超出范围后失败
    ///目标设备可能在大约20分钟后改变了身份。这需要重新发现，如果iOS设备
    ///处于后台状态，因此需要启用位置和屏幕打开来触发重新发现。
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        // Disconnected -> Connect if iOS
        
        let device = database.device(peripheral, delegate: self)
        device.lastDisconnectedAt = Date()
        logger.debug("didDisconnectPeripheral (device=\(device),error=\(String(describing: error)))")
        if device.operatingSystem == .ios {
            
            device.signalCharacteristic = nil
            device.payloadCharacteristic = nil
           
            connect("didDisconnectPeripheral", peripheral)
        }
    }
    
    // MARK: - CBPeripheralDelegate
    
    
    func peripheral(_ peripheral: CBPeripheral, didReadRSSI RSSI: NSNumber, error: Error?) {
        // Read RSSI -> Read Code | Notify delegates -> Scan again
        // scan -> wakeTransmitter -> didUpdateValueFor -> readRSSI -> notifyDelegates -> scheduleScan -> scan
        let device = database.device(peripheral, delegate: self)
        device.rssi = BLE_RSSI(RSSI.intValue)
        logger.debug("didReadRSSI (device=\(device),rssi=\(String(describing: device.rssi)),error=\(String(describing: error)))")
        taskNextAction("didReadRSSI", peripheral: peripheral)
    }
    
    
  
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        // Discover services -> Discover characteristics | Disconnect
        let device = database.device(peripheral, delegate: self)
        logger.debug("didDiscoverServices (device=\(device),error=\(String(describing: error)))")
        guard let services = peripheral.services else {
            disconnect("didDiscoverServices|serviceEmpty", peripheral)
            return
        }
        for service in services {
            if service.uuid == BLESensorConfiguration.serviceUUID {
                logger.debug("didDiscoverServices, found sensor service (device=\(device))")
                queue.async { peripheral.discoverCharacteristics(nil, for: service) }
                return
            }
        }
        disconnect("didDiscoverServices|serviceNotFound", peripheral)
        
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        // Discover characteristics -> Notify delegates -> Disconnect | Wake transmitter -> Scan again
        let device = database.device(peripheral, delegate: self)
        logger.debug("didDiscoverCharacteristicsFor (device=\(device),error=\(String(describing: error)))")
        guard let characteristics = service.characteristics else {
            disconnect("didDiscoverCharacteristicsFor|characteristicEmpty", peripheral)
            return
        }
        for characteristic in characteristics {
            switch characteristic.uuid {
            case BLESensorConfiguration.androidSignalCharacteristicUUID:
                device.operatingSystem = .android
                device.signalCharacteristic = characteristic
                logger.debug("didDiscoverCharacteristicsFor, found android signal characteristic (device=\(device))")
            case BLESensorConfiguration.iosSignalCharacteristicUUID:
                
                let notify = characteristic.properties.contains(.notify)
                let write = characteristic.properties.contains(.write)
                device.operatingSystem = .ios
                device.signalCharacteristic = characteristic
                queue.async { peripheral.setNotifyValue(true, for: characteristic) }
                logger.debug("didDiscoverCharacteristicsFor, found ios signal characteristic (device=\(device),notify=\(notify),write=\(write))")
            case BLESensorConfiguration.payloadCharacteristicUUID:
                device.payloadCharacteristic = characteristic
                logger.debug("didDiscoverCharacteristicsFor, found payload characteristic (device=\(device))")
            default:
                logger.fault("didDiscoverCharacteristicsFor, found unknown characteristic (device=\(device),characteristic=\(characteristic.uuid))")
            }
        }
        
        if device.operatingSystem == .android, let payloadCharacteristic = device.payloadCharacteristic {
            if device.payloadData == nil || device.timeIntervalSinceLastPayloadDataUpdate > BLESensorConfiguration.TimeIntervalForPayloadDataUpdate {
                queue.async { peripheral.readValue(for: payloadCharacteristic) }
            } else {
                disconnect("didDiscoverCharacteristicsFor|android", peripheral)
            }
        }
        
        scheduleScan("didDiscoverCharacteristicsFor")
    }
    
    
    
    func peripheral(_ peripheral: CBPeripheral, didWriteValueFor characteristic: CBCharacteristic, error: Error?) {
        // Wrote characteristic -> Scan again
        let device = database.device(peripheral, delegate: self)
        logger.debug("didWriteValueFor (device=\(device),error=\(String(describing: error)))")
    
        
        scheduleScan("didWriteValueFor")
    }
    
    
    func peripheral(_ peripheral: CBPeripheral, didModifyServices invalidatedServices: [CBService]) {
        // iOS only
        // Modified service -> Invalidate beacon -> Scan
        let device = database.device(peripheral, delegate: self)
        let characteristics = invalidatedServices.map { $0.characteristics }.count
        logger.debug("didModifyServices (device=\(device),characteristics=\(characteristics))")
        guard characteristics == 0 else {
            return
        }
        device.signalCharacteristic = nil
        device.payloadCharacteristic = nil
        if peripheral.state == .connected {
            discoverServices("didModifyServices", peripheral)
        } else if peripheral.state != .connecting {
            connect("didModifyServices", peripheral)
        }
    }
    
    /// All read characteristic requests will trigger this call back to handle the response.
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        // Updated value -> Read RSSI | Read Payload
        // Beacon characteristic is writable, primarily to enable non-transmitting Android devices to submit their
        // beacon code and RSSI as data to the transmitter via GATT write. The characteristic is also notifying on
        // iOS devices, to offer a mechanism for waking receivers. The process works as follows, (1) receiver writes
        // blank data to transmitter, (2) transmitter broadcasts value update notification after 8 seconds, (3)
        // receiver is woken up to handle didUpdateValueFor notification, (4) receiver calls readRSSI, (5) readRSSI
        // call completes and schedules scan after 8 seconds, (6) scan writes blank data to all iOS transmitters.
        // Process repeats to keep both iOS transmitters and receivers awake while maximising time interval between
        // bluetooth calls to minimise power usage.
        let timestamp = Date()
        let device = database.device(peripheral, delegate: self)
        logger.debug("didUpdateValueFor (device=\(device),characteristic=\(characteristic.uuid),error=\(String(describing: error)))")
        switch characteristic.uuid {
        case BLESensorConfiguration.iosSignalCharacteristicUUID:
           
            logger.debug("didUpdateValueFor (device=\(device),characteristic=iosSignalCharacteristic,error=\(String(describing: error)))")
            device.lastNotifiedAt = Date()
            readRSSI("didUpdateValueFor", peripheral)
            return
        case BLESensorConfiguration.androidSignalCharacteristicUUID:
           
            logger.fault("didUpdateValueFor (device=\(device),characteristic=androidSignalCharacteristic,error=\(String(describing: error)))")
        case BLESensorConfiguration.payloadCharacteristicUUID:
            // Read payload data
            logger.debug("didUpdateValueFor (device=\(device),characteristic=payloadCharacteristic,error=\(String(describing: error)))")
            if let data = characteristic.value {
                guard PayloadData(data).isValid(timestamp:timestamp) else{
                    return
                }
                device.payloadData = PayloadData(data)
            }
            if device.operatingSystem == .android {
                disconnect("didUpdateValueFor|payload|android", peripheral)
            }
        default:
            logger.fault("didUpdateValueFor, unknown characteristic (device=\(device),characteristic=\(characteristic.uuid),error=\(String(describing: error)))")
        }
        scheduleScan("didUpdateValueFor")
        return
    }
}

