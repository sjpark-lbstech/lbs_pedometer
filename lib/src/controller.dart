part of lbs_pedometer;

class _PedometerController {
  final MethodChannel _channel = MethodChannel(_METHOD_CHANNEL_NAME);
  final EventChannel _eventChannel = EventChannel(_EVENT_CHANNEL_NAME);
  final Pedometer pedometer;

  String _androidNotificationTitle = 'GPS 위치정보 추적중';
  String _androidNotificationContent = '백그라운드에서 현재 위치를 지속적으로 추적하고 있습니다';
  StreamSubscription _pedometerStream;

  _PedometerController(this.pedometer) {
    this._channel.setMethodCallHandler((call) {
      if (call.method == _MTD_ANDROID_RESUME) {
        pedometer._onAndroidResumed?.call(_convertToStepData(call.arguments));
      } else if (call.method == "onCancel") {
        List list = call.arguments;
        if (list != null && list.isNotEmpty && pedometer._onEnd != null) {
          pedometer._onEnd(_convertToStepData(list));
        } else {
          print('종료 이후 이전 기록 가져오기의 결과가 비어 있습니다.');
          return null;
        }
      }

      return null;
    });
  }

  void start({bool storeHistory, int resultStepsUnit, int storeStepsUnit}) {
    _pedometerStream = _eventChannel.receiveBroadcastStream({
      'title': _androidNotificationTitle,
      'content': _androidNotificationContent,
    }).listen((stepData) => pedometer._onTakeStep(StepData.fromMap(stepData)));
  }

  void stop() => _pedometerStream?.cancel();

  Future<void> requestPermission() async {
    await _channel.invokeMethod(_MTD_REQUEST_PERMISSION);
  }

  Future<Coordinate> getLocation() async {
    Map arg = await _channel.invokeMethod(_MTD_GET_LOCATION);
    if (arg == null) {
      AssertionError('argument from nvative method call[getLocation] is null.');
      return null;
    }
    return Coordinate(latitude: arg['lat'], longitude: arg['lng']);
  }

  Future<AuthorizationState> getAuthorizationState() async {
    int state = await _channel.invokeMethod(_MTD_GET_STATE);
    return AuthorizationState.values[state];
  }

  void setAndroidNotification({
    @required String title,
    @required String content,
  }) {
    _androidNotificationTitle = title;
    _androidNotificationContent = content;
    return;
  }

  Future<List<StepData>> getHistory() async {
    List list = await _channel.invokeMethod<List>(_MTD_GET_HISTORY);
    if (list != null && list.isNotEmpty) {
      return _convertToStepData(list);
    } else {
      print('이전 기록 가져오기의 결과가 비어 있습니다.');
      return null;
    }
  }

  List<StepData> _convertToStepData(List list) {
    List<StepData> result = List();
    for (Map row in list) {
      result.add(StepData.fromMap(row));
    }
    return result;
  }
}
