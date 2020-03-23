#import "LbsPedometerPlugin.h"
#if __has_include(<lbs_pedometer/lbs_pedometer-Swift.h>)
#import <lbs_pedometer/lbs_pedometer-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "lbs_pedometer-Swift.h"
#endif

@implementation LbsPedometerPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftLbsPedometerPlugin registerWithRegistrar:registrar];
}
@end
