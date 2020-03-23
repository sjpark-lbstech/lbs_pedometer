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
    let pedometer = CMPedometer()
    let channel : FlutterMethodChannel
    let sql = SQLController.instance
    
    var pedoStep : NSNumber = -1
    var location : CLLocation?
    var state : Int
    
    init(channel : FlutterMethodChannel) {
        self.channel = channel
        state = STATE_UNDEFINED
        super.init()
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if let last = locations.last {
            location = last
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
        
        print("ios pedometer and location sensor turn on.")
        pedometer.startUpdates(from: Date(), withHandler: { (data, error) in
            if data?.numberOfSteps != self.pedoStep {
                let timeStemp : Int = Int(data!.endDate.timeIntervalSince1970 * 100)
                let stepCnt : Int = Int(truncating: data!.numberOfSteps)
                self.pedoStep = data!.numberOfSteps
                if let pos = self.location {
                    let map : [String : Any] = [
                        "timestamp" : timeStemp,
                        "step" : stepCnt,
                        "latitude" : pos.coordinate.latitude,
                        "longitude" : pos.coordinate.longitude,
                    ]
                    self.channel.invokeMethod("takeSteps", arguments: map)
                    
                    if self.sql.isAvailable() {
                        self.sql.insert(step: stepCnt, lat: pos.coordinate.latitude, lng: pos.coordinate.longitude, timestamp: timeStemp)
                    }
                }
                
            }
        })
    }
    
    func stop() -> [[String : Any]]{
        pedometer.stopUpdates()
        var result : [[String : Any]] = []
        if sql.isAvailable() {
            result = sql.selctAll()
            sql.cleanDB()
        }
        print("ios pedometer and location sensor turn off.")
        return result
    }
    
    func getLocation() -> [String : Double]{
        if state != STATE_WHEN_IN_USE && state != STATE_ALWAYS {
            locationManager.requestAlwaysAuthorization()
        }
        let coordinate : CLLocationCoordinate2D
        if location?.coordinate != nil {
            coordinate = location!.coordinate
        }else {
            coordinate = locationManager.location?.coordinate
                ?? CLLocationCoordinate2D(latitude: 0.0, longitude: 0.0)
        }
        let map : [String : Double] = [
            "lat" : coordinate.latitude,
            "lng" : coordinate.longitude
        ]
        return map
    }
    
}
