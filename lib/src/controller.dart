part of lbs_pedometer;

class _PedometerController{
  final MethodChannel _channel = MethodChannel(_CHANNEL_NAME);
  final Pedometer pedometer;

  String _androidNotificationTitle = 'GPS 위치정보 추적중';
  String _androidNotificationContent = '백그라운드에서 현재 위치를 지속적으로 추적하고 있습니다';

  _PedometerController(this.pedometer){
    this._channel.setMethodCallHandler((call){

      if(call.method == _MTD_TAKE_STEPS){
        if (pedometer._onTakeStep != null){
          pedometer._onTakeStep(StepData.fromMap(call.arguments));
        }
      }else if(call.method == _MTD_ANDROID_RESUME){
        if (pedometer._onAndroidResumed != null){
          pedometer._onAndroidResumed(_convertToStepData(call.arguments));
        }
      }

      return null;
    });
  }

  Future<void> start() async {
    if (Platform.isAndroid){
      Map<String, dynamic> arg = {
        "title" : _androidNotificationTitle,
        "content" : _androidNotificationContent
      };
      await _channel.invokeMethod(_MTD_START, arg);
    } else {
      await _channel.invokeMethod(_MTD_START);
    }
  }

  Future<void> stop() async{
    List list = await _channel.invokeMethod(_MTD_STOP);
    if (list != null && list.isNotEmpty && pedometer._onEnd != null){
       pedometer._onEnd(_convertToStepData(list));
    }else{
      print('종료 이후 이전 기록 가져오기의 결과가 비어 있습니다.');
      return null;
    }
  }

  Future<void> requestPermission() async{
    await _channel.invokeMethod(_MTD_REQUEST_PERMISSION);
  }

  Future<Coordinate> getLocation() async{
    Map arg = await _channel.invokeMethod(_MTD_GET_LOCATION);
    return Coordinate(latitude: arg['lat'], longitude: arg['lng']);
  }

  Future<AuthorizationState> getAuthorizationState() async{
    int state = await _channel.invokeMethod(_MTD_GET_STATE);
    return AuthorizationState.values[state];
  }

  void setAndroidNotification ({@required String title, @required String content}){
    _androidNotificationTitle = title;
    _androidNotificationContent = content;
    return;
  }

  Future<List<StepData>> getHistory() async{
    List list = await _channel.invokeMethod<List>(_MTD_GET_HISTORY);
    if (list != null && list.isNotEmpty){
      return _convertToStepData(list);
    }else{
      print('이전 기록 가져오기의 결과가 비어 있습니다.');
      return null;
    }
  }

  List<StepData> _convertToStepData(List list) {
    List<StepData> result = List();
    for(Map row in list){
      result.add(StepData.fromMap(row));
    }
    return result;
  }

}