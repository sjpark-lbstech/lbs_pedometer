part of lbs_pedometer;

/// ## Pedometer - rs2020
///
/// ### ANDROID FLOW
/// 1. start()를 통한 foreground service 실행
/// 2. 위치 업데이트가 발생할 때 마다 sqlite에 데이터 저장
/// 3. 만약 중간에 의도치 않은 이유로 foreground가 종료되면 위치이벤트는 종료
///   - 그러나 SQLite 는 지워지지 않는다.
///   - 즉, SQLite에 데이터가 있으면 종료되지 않은것!
/// 4. stop() 시에 모든 저장된 데이터가 불러와지고, SQLite clear 된다.
class Pedometer{
  static final Pedometer _instance = Pedometer._internal();
  static _PedometerController _controller;

  Pedometer._internal();

  factory Pedometer(){
    if (_controller == null) _controller = _PedometerController(_instance);
    return _instance;
  }

  /// 일정한 걸음 간격으로 (android - 10 걸음, iOS - 약 4~6걸음)현제 센서값을
  /// [StepData]객체로 반환하는 Callback.
  OnTakeStep _onTakeStep;

  /// ios의 pedometer 의 이벤트를 받기 시작한다.
  /// 이때 CLLocationManager 를 이용한 위치 변경 이벤트도 같이 받는다.
  ///
  ///
  /// OnTakeStep call back 으로 step의 수와 timeStemp,
  /// 그리고 [Coordinate] 객체를 전달한다.
  ///
  /// onAndroidStarted 함수는 안드로이드가 종료된 이후 다시 시작했을때
  /// 이전까지 기록된 센서 값을 반환한다.
  Future<void> start({OnTakeStep onTakeStep}) {
    _onTakeStep = onTakeStep;
    _controller.start();
    return null;
  } 

  /// 현제 pedometer의 이벤트를 받고 있을 경우 해당 이벤트 스트림을 취소한다.
  ///
  /// onAndroidStop 의 경우 안드로이드에서만 호출되는 함수이며, 최종 기록들을 모두 불러온다.
  Future<List<Coordinate>> stop() async{
    _onTakeStep = null;
    final result = (await _controller.stop()) ?? [];
    if (result.length >= 15) { // 어느정도 길이가 형성이 되어야 가능
      return OutlierFilter(result).refine();
    }
    return result;
  }

  /// permission 을 요청하는 함수
  Future<void> requestPermission() async {
    return _controller.requestPermission();
  }

  /// ios 의 현제 권한 상태를 가져온다.
  ///
  ///
  /// @return type : [AuthorizationState]
  Future<AuthorizationState> getAuthorizationState() async{
    return _controller.getAuthorizationState();
  }

  /// android 의 notification 의 title 과 content 를 설정한다.
  ///
  /// title default value : 'GPS 위치정보 추적중'
  ///
  /// content default value : '백그라운드에서 현재 위치를 지속적으로 추적하고 있습니다'
  void setAndroidNotification({@required String title, @required String content}){
    if (Platform.isIOS) { return; }
    return _controller.setAndroidNotification(title: title, content: content);
  }

  /// 백그라운드에서 SQLite 데이터가 기록되고 있는 중인경우
  /// 해당 함수를 호출하는 시점 이전의 모든 기록을 가져오는 함수.
  Future<List<Coordinate>> getHistory() async{
    if (Platform.isIOS) { return null; }
    return _controller.getHistory();
  }

}