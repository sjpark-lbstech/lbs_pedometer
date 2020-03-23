# lbs_pedometer

사용자의 걸음을 측정하고 특정 간격마다 위치를 비롯한 여러 센서값을 가져오는 plug-in입니다.

## Getting Started

This project is a starting point for a Flutter
[plug-in package](https://flutter.dev/developing-packages/),
a specialized package that includes platform-specific implementation code for
Android and/or iOS.

For help getting started with Flutter, view our 
[online documentation](https://flutter.dev/docs), which offers tutorials, 
samples, guidance on mobile development, and a full API reference.

해당 Plug in을 사용하기 위해 flutter의 pubspec.yaml 문서에서 dependencies 아래에서 종속성 추가하십시오.

```
lbs_pedometer
  git:
    url: https://github.com/LBSTECH/lbs_pedometer.git
    ref: master
```

백그라운드의 `CLLocaionManager`와 `CMPedometer`를 사용해서 이벤트를 수신하므로 프로젝트 ios모듈의 `info.plist`에 
해당 기능에 대한 권한과 목적을 명시해야 합니다.

> info.plist code view.
```xml
<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
  <string>[Why you need this permission?]</string>
<key>NSLocationWhenInUseUsageDescription</key>
  <string>[Why you need this permission?]</string>
<key>NSMotionUsageDescription</key>
  <string>[Why you need this permission?]</string>
```
> info.plist xcode view.

```
Privacy - Location Always and When In Use Usage Description
Privacy - Location When In Use Usage Description
Privacy - Motion Usage Description
```

그리고 Xcode의 Runner setting 페이지에서 Singing & Capabilities에 들어가서
background Modes를 추가하고 location updates를 선택한다.

![xcode-config](./)

ios 모듈에서 해야하는 작업을 마치면 안드로이드 모듈의 Manifest에 해당 권한과 서비스의 사용을 명시한다.

```xml
<manifest>
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
  
  <application>
    .
    .
    <service android:name="net.lbstech.flutter_pedometer.ServiceSensor"
            android:stopWithTask="false"
            android:exported="false"/>
    .
    .
  </application>
</manifest>  
```

위치관련 권한과 포어그라운드 관련 권한 명시부분이다.
