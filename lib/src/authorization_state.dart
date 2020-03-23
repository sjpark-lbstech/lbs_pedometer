part of lbs_pedometer;

enum AuthorizationState{
  /// 아직 native의 CLLocationManager와 권한상태가 연동되지 않은 상태.
  UNDEFINED,

  /// CLLocationManager가 권한 상태와 연동하고 있지만 아직 물어보지 않은 상태.
  NOT_DETERMINED,

  /// 권한 동의가 거부된 상태
  DENIED,

  /// '어플이 사용중 일때만 허용' 단계의 권한 동의를 한 상태
  WHEN_IN_USE,

  /// '항상' 단계의 권한 동의를 한 상태
  ALWAYS
}