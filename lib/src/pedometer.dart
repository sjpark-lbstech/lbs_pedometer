part of lbs_pedometer;

class Pedometer {
  static final Pedometer _instance = Pedometer._internal();
  static _PedometerController _controller;

  Pedometer._internal();

  factory Pedometer() {
    if (_controller == null) _controller = _PedometerController(_instance);
    return _instance;
  }

  /// 일정한 걸음 간격으로 (android - 10 걸음, iOS - 약 4~6걸음)현제 센서값을
  /// [StepData]객체로 반환하는 Callback.
  OnTakeStep _onTakeStep;

  /// android 의 lifecycle 에서 onResume 이 호출됬을때
  /// 현재 Service 가 실행중인 경우에는 pause 이후 축적된 데이터가 [StepData]의 List 들로 반환한다.
  /// Service 가 실행되지 않는 경우에는 null 이 반환된다.
  OnAndroidResumed _onAndroidResumed;

  /// android 에서 위치 추적을 종료하는 경우 시작부터 종료까지의 모든 데이터를
  /// [StepData]의 List 들로 반환한다.
  OnEnd _onEnd;

  /// 현제 위치를 가져오는 일회성 함수이다.
  ///
  ///
  /// @return type : [Coordinate]
  Future<Coordinate> get location => _controller.getLocation();

  /// ios 의 현제 권한 상태를 가져온다.
  ///
  ///
  /// @return type : [AuthorizationState]
  Future<AuthorizationState> get authorizationState =>
      _controller.getAuthorizationState();

  /// 백그라운드에서 SQLite 데이터가 기록되고 있는 중인경우
  /// 해당 함수를 호출하는 시점 이전의 모든 기록을 가져오는 함수.
  Future<List<StepData>> get history {
    if (Platform.isIOS) return null;
    return _controller.getHistory();
  }

  /// 걸음과 위치에 대한 정보를 받기 시작한다.
  ///
  /// [onTakeStep]에서 데이터에 대한 콜백 처리를한다.
  /// [storeHistory]를 true로 주게되면 디바이스 데이터 베이스에 데이터를 기록하며 기본값은 `true`이다.
  /// 안드로이드 한정으로 [onAndroidResumed]에서 onResume의 콜백 처리를 한다.
  /// 마찬가지로 안드로이드 한정으로 [resultStepsUnit]과 [storeStepsUnit]는 각각 Flutter로 데이터를 보내는 걸음 단위와 디바이스 데이터 베이스에 저장하는 걸음 단위를 의미한다.
  /// 예를 들어 [resultStepsUnit]이 3이고 [storeStepsUnit]가 10이면 매 3걸음 마다 [onTakeStep]이 불리며 10걸음마다 디바이스 데이터 베이스에 데이터를 저장한다.
  /// [resultStepsUnit]와 [storeStepsUnit] 둘 중 하나의 값 많이 정의 되더라도 다른 값 또한 같은 값으로 정의된며 기본값은 둘 다 10이다.
  void start({
    OnTakeStep onTakeStep,
    OnAndroidResumed onAndroidResumed,
    bool storeHistory = true,
    int resultStepsUnit = 10,
    int storeStepsUnit = 10,
  }) {
    _onTakeStep = onTakeStep;
    _onAndroidResumed = onAndroidResumed;
    _controller.start(
      storeHistory: storeHistory,
      resultStepsUnit: resultStepsUnit,
      storeStepsUnit: storeStepsUnit,
    );
    return null;
  }

  /// 현제 pedometer의 이벤트를 받고 있을 경우 해당 이벤트 스트림을 취소한다.
  ///
  /// onAndroidStop 의 경우 안드로이드에서만 호출되는 함수이며, 최종 기록들을 모두 불러온다.
  Future<void> stop({OnEnd onEnd}) {
    _onEnd = onEnd;
    _onTakeStep = null;
    _controller.stop();
    return null;
  }

  /// android 의 notification 의 title 과 content 를 설정한다.
  ///
  /// title default value : 'GPS 위치정보 추적중'
  ///
  /// content default value : '백그라운드에서 현재 위치를 지속적으로 추적하고 있습니다'
  void setAndroidNotification({
    @required String title,
    @required String content,
  }) {
    if (Platform.isIOS) return;
    return _controller.setAndroidNotification(title: title, content: content);
  }

  /// permission 을 요청하는 함수
  Future<void> requestPermission() async => _controller.requestPermission();

  /// 현제 위치를 가져오는 일회성 함수이다.
  ///
  ///
  /// @return type : [Coordinate]
  @Deprecated('Effective Dart문서에 따라 getter를 쓰기로합니다. 2.x.x 이후 사라집니다.')
  Future<Coordinate> getLocation() => _controller.getLocation();

  /// ios 의 현제 권한 상태를 가져온다.
  ///
  ///
  /// @return type : [AuthorizationState]
  @Deprecated('Effective Dart문서에 따라 getter를 쓰기로합니다. 2.x.x 이후 사라집니다.')
  Future<AuthorizationState> getAuthorizationState() async =>
      _controller.getAuthorizationState();

  /// 백그라운드에서 SQLite 데이터가 기록되고 있는 중인경우
  /// 해당 함수를 호출하는 시점 이전의 모든 기록을 가져오는 함수.
  @Deprecated('Effective Dart문서에 따라 getter를 쓰기로합니다. 2.x.x 이후 사라집니다.')
  Future<List<StepData>> getHistory() async {
    if (Platform.isIOS) return null;
    return _controller.getHistory();
  }
}
