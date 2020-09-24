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
    let isRunning = UserDefaults.standard.bool(forKey: "isRunning")
    if isRunning {
        controller?.start()
    }
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    guard let cont = SwiftLbsPedometerPlugin.controller else { return }

    var arg : Any?
    if call.method == "start" {
        cont.start()
    }else if call.method == "stop" {
        arg = cont.stop()
    }else if call.method == "getState" {
        arg = cont.state
    }else if call.method == "requestPermission" {
        cont.requestPermission()
    }else if call.method == "getHistory"{
        arg = SQLController.instance.selctAll()
    }
    
    result(arg)
  }
    
    public func applicationWillTerminate(_ application: UIApplication) {
        // 비정상 종료 또는 어플리케이션 실핼 도중 종료 누르지 않고 어플리케이션 종료하는 경우.
        print("application Will Terminate call backs")
        guard let cont = SwiftLbsPedometerPlugin.controller else { return }
        if cont.isRunning {
            _ = cont.pause()
        }
    }

    
}
