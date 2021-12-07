#import "NativeCoinsLibPlugin.h"
#if __has_include(<native_coins_lib/native_coins_lib-Swift.h>)
#import <native_coins_lib/native_coins_lib-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "native_coins_lib-Swift.h"
#endif

@implementation NativeCoinsLibPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftNativeCoinsLibPlugin registerWithRegistrar:registrar];
}
@end
