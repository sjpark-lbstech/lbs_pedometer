part of lbs_pedometer;

/// native 에서 측정된 센서값을 가지는 객체
/// 추후 가속도, 지자계 센서등 추가적으로 센서를 사용하는 경우
/// 맴버 변수를 추가한다. 이후 생성자와 factory 를 수정
///
///
/// android - 10걸음을 기준으로 인스턴스 생성
///
/// ios - 일정하지 않지만 약 4~5걸음마다 인스턴스 생성
class StepData {
  ///  latitude, longitude 를 가진 VO class
  final Coordinate coordinate;

  /// 센서가 측정된 시간을 가지는 객체
  final DateTime timeStamp;

  /// 스텝카운트의 스텝수
  final int stepCnt;

  StepData({this.coordinate, this.timeStamp, this.stepCnt});

  factory StepData.fromMap(Map map) {
    int step = map['step'];
    double latitude = map['latitude'];
    double longitude = map['longitude'];
    DateTime timeStamp = DateTime.fromMillisecondsSinceEpoch(map['timestamp']);
    return StepData(
      stepCnt: step,
      coordinate: Coordinate(latitude: latitude, longitude: longitude,),
      timeStamp: timeStamp,
    );
  }

  Map<String, dynamic> toMap() {
    var map = Map<String, dynamic>();
    map['step'] = this.stepCnt;
    map['latitude'] = this.coordinate.latitude;
    map['longitude'] = this.coordinate.longitude;
    map['timestamp'] = this.timeStamp.millisecondsSinceEpoch;
    return map;
  }

  @override
  String toString() {
    return "step : $stepCnt, "
        "timestamp : $timeStamp, "
        "lat : ${coordinate.latitude}, "
        "lng : ${coordinate.longitude}";
  }
}
