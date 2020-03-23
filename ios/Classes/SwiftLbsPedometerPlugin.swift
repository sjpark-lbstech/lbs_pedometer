import Flutter
import UIKit

public class SwiftLbsPedometerPlugin: NSObject, FlutterPlugin {
    
    static let channelName = "lbstech.net.plugin/lbs_pedoemter";
    static var controller : SensorController?
    static var channel : FlutterMethodChannel?
    
  public static func register(with registrar: FlutterPluginRegistrar) {
    channel = FlutterMethodChannel(name: channelName, binaryMessenger: registrar.messenger())
    let instance = SwiftLbsPedometerPlugin()
    registrar.addMethodCallDelegate(instance, channel: SwiftLbsPedometerPlugin.channel!)
    registrar.addApplicationDelegate(instance)
    
    controller = SensorController(channel: SwiftLbsPedometerPlugin.channel!)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    guard let cont = SwiftLbsPedometerPlugin.controller else { return }

    var arg : Any?
    if call.method == "start" {
        cont.start()
    }else if call.method == "stop" {
        arg = cont.stop()
    }else if call.method == "getLocation"{
        arg = cont.getLocation()
    }else if call.method == "getState" {
        arg = cont.state
    }else if call.method == "requestPermission" {
        cont.requestPermission()
    }
    
    result(arg)
  }
    
    public func applicationWillTerminate(_ application: UIApplication) {
        // 비정상 종료 또는 어플리케이션 실핼 도중 종료 누르지 않고 어플리케이션 종료하는 경우.
        print("application Will Terminate call backs")
        guard let methodChannel = SwiftLbsPedometerPlugin.channel else { return }
        
        methodChannel.invokeMethod("onEnd", arguments: "어플리케이션 종료")
        print("invoke method 완료")
    }
    
    
}
