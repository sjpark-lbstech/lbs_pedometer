//
//  SensorController.swift
//  lbs_pedometer
//
//  Created by Maximilian on 2020/03/20.
//

import Flutter
import Foundation
import CoreLocation
import CoreMotion

class SensorController: NSObject, CLLocationManagerDelegate {
    let STATE_UNDEFINED = 0
    let STATE_NOT_DETERMINED = 1
    let STATE_DENIED = 2
    let STATE_WHEN_IN_USE = 3
    let STATE_ALWAYS = 4
    
    let locationManager = CLLocationManager()
    let channel : FlutterMethodChannel
    let sql = SQLController.instance
    
    var pedoStep : NSNumber = -1
    var location : CLLocation?
    var state : Int
    var isRunning = false
    
    var startDate: Date?
    
    init(channel : FlutterMethodChannel) {
        self.channel = channel
        state = STATE_UNDEFINED
        super.init()
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let last = locations.last {
            location = last
            self.channel.invokeMethod("takeSteps", arguments: [location?.coordinate.latitude, location?.coordinate.longitude])
            
            if self.sql.isAvailable() {
                self.sql.insert(lat: location!.coordinate.latitude , lng: location!.coordinate.longitude)
            }
            
            // 제한 시간 설정
            if let start = startDate{
                let interval = Date().timeIntervalSince(start)
                if interval >= (60*60*12) { // 12시간
                    locationManager.stopUpdatingLocation()
                    locationManager.stopMonitoringSignificantLocationChanges()
                }
            }
        }
    }
    
    func requestPermission(){
        locationManager.requestAlwaysAuthorization();
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        switch status {
        case CLAuthorizationStatus.notDetermined:
            state = STATE_NOT_DETERMINED
        case CLAuthorizationStatus.denied:
            state = STATE_DENIED
        case CLAuthorizationStatus.authorizedWhenInUse:
            state = STATE_WHEN_IN_USE
        case CLAuthorizationStatus.authorizedAlways:
            state = STATE_WHEN_IN_USE
        default:
            state = STATE_UNDEFINED
        }
        print("Authorization state was changed... current state : \(state)")
    }

    func start(){
        if #available(iOS 9.0, *) {
            locationManager.allowsBackgroundLocationUpdates = true
        }
        locationManager.delegate = self
        
        locationManager.requestAlwaysAuthorization()
        locationManager.startUpdatingLocation()
        locationManager.startMonitoringSignificantLocationChanges()
        
        if startDate == nil {
            startDate = Date()
        }
        
        isRunning = true
        UserDefaults.standard.set(startDate, forKey: "isRunning")
        
        print("ios location sensor turn on.")
    }
    
    func start(start: Date){
        if #available(iOS 9.0, *) {
            locationManager.allowsBackgroundLocationUpdates = true
        }
        locationManager.delegate = self
        
        locationManager.requestAlwaysAuthorization()
        locationManager.startUpdatingLocation()
        locationManager.startMonitoringSignificantLocationChanges()
        isRunning = true
        startDate = start
        
        print("ios location sensor turn on.")
    }
    
    func stop() -> [[Double]]{
        locationManager.stopMonitoringSignificantLocationChanges()
        locationManager.stopUpdatingLocation()
        
        var result : [[Double]] = []
        if sql.isAvailable() {
            result = sql.selctAll()
            sql.cleanDB()
        }
        isRunning = false
        startDate = nil
        UserDefaults.standard.removeObject(forKey: "isRunning")
        print("ios pedometer and location sensor turn off.")
        return result
    }
    
    
    func pause() {
        locationManager.stopMonitoringSignificantLocationChanges()
        locationManager.stopUpdatingLocation()
    }
}
