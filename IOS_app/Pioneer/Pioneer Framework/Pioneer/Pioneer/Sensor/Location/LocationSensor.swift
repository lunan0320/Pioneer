//
//  LocationSensor.swift
//  Pioneer
//
//  Created by Beh on 2021/5/22.
//

import Foundation
import CoreLocation

protocol TravelSensor : Sensor {
}

/**
    可选配置：基于定位的位置传感器，在全局配置中将mobilitySensorEnabled设置为nil不使用GPS位置。
    Pioneer使用位置传感器仅用于评估行程范围，而非记录用户的位置信息。用户的行程范围未来将有可能作为评估用户近期社交活跃度的依据。
    使用该功能需要定位权限
    Requires : Signing & Capabilities : BackgroundModes : LocationUpdates = YES
    同时使用该功能需要隐私权限请求
    Requires : Info.plist : Privacy - Location When In Use Usage Description
    Requires : Info.plist : Privacy - Location Always and When In Use Usage Description
 */
class SpecificTravelSensor : NSObject, TravelSensor, CLLocationManagerDelegate {
    private let logger = SpecificSensorLogger(subsystem: "Sensor", category: "SpecificTravelSensor")
    private var delegates: [SensorDelegate] = []
    private let locationManager = CLLocationManager()
    private let rangeForBeacon: UUID?
   
    // 根据CoreLocation的定义，移动传感器分辨率最小为3km
    public static let minimumResolution: Distance = Distance(kCLLocationAccuracyThreeKilometers)

    // 最后一个位置仅用于计算累计行驶距离
    private let resolution: Distance
    private var lastLocation: CLLocation?
    private var lastUpdate: Date?
    private var totalDistance: Distance = 0

    init(resolution: Distance = minimumResolution, rangeForBeacon: UUID? = nil) {
        self.resolution = resolution
        self.rangeForBeacon = rangeForBeacon
        super.init()
        let accuracy = SpecificTravelSensor.locationAccuracy(resolution)
        logger.debug("init(resolution=\(resolution),accuracy=\(accuracy),rangeForBeacon=\(rangeForBeacon == nil ? "disabled" : rangeForBeacon!.description))")
        locationManager.delegate = self
        
        //申请位置权限并初始化位置管理器
        if #available(iOS 13.4, *) {
            self.locationManager.requestWhenInUseAuthorization()
        } else {
            self.locationManager.requestAlwaysAuthorization()
        }
        locationManager.pausesLocationUpdatesAutomatically = false
        locationManager.desiredAccuracy = accuracy
        locationManager.distanceFilter = resolution
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.showsBackgroundLocationIndicator = false
    }
    
    
    // 根据所需的分辨率确定所需的定位精度
    private static func locationAccuracy(_ resolution: Distance) -> CLLocationAccuracy {
        if resolution < 10 {
            return kCLLocationAccuracyBest
        }
        if resolution < 100 {
            return kCLLocationAccuracyNearestTenMeters
        }
        if resolution < 1000 {
            return kCLLocationAccuracyHundredMeters
        }
        if resolution < 3000 {
            return kCLLocationAccuracyKilometer
        }
        return kCLLocationAccuracyThreeKilometers
    }
    
    func add(delegate: SensorDelegate) {
        delegates.append(delegate)
    }
    

    func start() {
        logger.debug("start")
        locationManager.startUpdatingLocation()
        logger.debug("startUpdatingLocation")

        guard let beaconUUID = rangeForBeacon else {
            return
        }
        if #available(iOS 13.0, *) {
            locationManager.startRangingBeacons(satisfying: CLBeaconIdentityConstraint(uuid: beaconUUID))
            logger.debug("startRangingBeacons(ios>=13.0,beaconUUID=\(beaconUUID.description))")
        } else {
            let beaconRegion = CLBeaconRegion(proximityUUID: beaconUUID, identifier: beaconUUID.uuidString)
            locationManager.startRangingBeacons(in: beaconRegion)
            logger.debug("startRangingBeacons(ios<13.0,beaconUUID=\(beaconUUID.uuidString)))")
        }
    }
    
    func stop() {
        logger.debug("stop")
        locationManager.stopUpdatingLocation()
        logger.debug("stopUpdatingLocation")
        guard let beaconUUID = rangeForBeacon else {
            return
        }
        if #available(iOS 13.0, *) {
            locationManager.stopRangingBeacons(satisfying: CLBeaconIdentityConstraint(uuid: beaconUUID))
            logger.debug("stopRangingBeacons(ios>=13.0,beaconUUID=\(beaconUUID.description))")
        } else {
            let beaconRegion = CLBeaconRegion(proximityUUID: beaconUUID, identifier: beaconUUID.uuidString)
            locationManager.stopRangingBeacons(in: beaconRegion)
            logger.debug("stopRangingBeacons(ios<13.0,beaconUUID=\(beaconUUID.description))")
        }
    }
    
    // MARK:- CLLocationManagerDelegate
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        var state = SensorState.off
        
        if status == CLAuthorizationStatus.authorizedWhenInUse {
            self.locationManager.requestAlwaysAuthorization()
            state = .on
        }
        
        if status == CLAuthorizationStatus.authorizedAlways {
            state = .on
        }
        if status == CLAuthorizationStatus.notDetermined {
            if #available(iOS 13.4, *) {
                self.locationManager.requestWhenInUseAuthorization()
            } else {
                self.locationManager.requestAlwaysAuthorization()
            }
            locationManager.stopUpdatingLocation()
            locationManager.startUpdatingLocation()
        }
        if status != CLAuthorizationStatus.notDetermined {
            delegates.forEach({ $0.sensor(.TRAVEL, didUpdateState: state) })
        }
    }

    @available(iOS 14.0, *)
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        var state = SensorState.off
        if manager.authorizationStatus == CLAuthorizationStatus.authorizedWhenInUse {
            self.locationManager.requestAlwaysAuthorization()
            state = .on
        }
        
        if manager.authorizationStatus == CLAuthorizationStatus.authorizedAlways {
            state = .on
        }
        if manager.authorizationStatus == CLAuthorizationStatus.notDetermined {
            locationManager.requestWhenInUseAuthorization()
            locationManager.stopUpdatingLocation()
            locationManager.startUpdatingLocation()
        }
        if manager.authorizationStatus != CLAuthorizationStatus.notDetermined {
            delegates.forEach({ $0.sensor(.TRAVEL, didUpdateState: state) })
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        
        // 仅在启用了行程传感器的情况下处理位置数据
        guard let resolution = BLESensorConfiguration.TravelSensorEnabled else {
            return
        }
        guard locations.count > 0 else {
            return
        }

        // 累计总的行驶距离并以设置的分辨率报告，注意：在移动性检测中不报告实际行驶位置和方向，只报告以分辨率单位表示的累积行驶距离。
        locations.forEach() { location in
            guard let lastLocation = lastLocation, let lastUpdate = lastUpdate else {
                self.lastLocation = location
                self.lastUpdate = location.timestamp
                return
            }

        //  计算总的行程距离。注意，行程距离按直线计算（尽管这看来时粗略的。
            let distance = location.distance(from: lastLocation)
            totalDistance += distance
            logger.debug("didUpdateLocations(distance=\(distance))")

        //  行程数据仅以设置的分辨率报告
            if totalDistance >= resolution {
                let didVisit = Location(value: TravelLocationReference(distance: totalDistance), time: (start: lastUpdate, end: location.timestamp))
                delegates.forEach { $0.sensor(.TRAVEL, didVisit: didVisit) }
                totalDistance = 0
                self.lastUpdate = location.timestamp
            }
            self.lastLocation = location
        }
    }
}
