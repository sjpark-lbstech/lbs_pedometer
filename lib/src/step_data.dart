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

  // TYPE_ACCELEROMETER
  final double acX, acY, acZ;
  // TYPE_GYROSCOPE
  final double gyX, gyY, gyZ;
  // TYPE_MAGNETIC_FIELD
  final double maX, maY, maZ;
  // TYPE_PRESSURE
  final double pressure;
  // TYPE_ROTATION_VECTOR
  final double veX, veY, veZ, veCos, veAccuracy;
  // TYPE_AMBIENT_TEMPERATURE
  final double tmpDegree;
  // TYPE_RELATIVE_HUMIDITY
  final double humidity;
  // TYPE_MOTION_DETECT, TYPE_STEP_DETECTOR, TYPE_SIGNIFICANT_MOTION
  // 해당 불린은 저장이후 false 로 변환
  final bool step, motion, sigMotion;

  StepData({this.acX, this.acY, this.acZ, this.gyX, this.gyY, this.gyZ,
      this.maX, this.maY, this.maZ, this.pressure, this.veX, this.veY,
      this.veZ, this.veCos, this.veAccuracy, this.tmpDegree, this.humidity,
      this.step, this.motion, this.sigMotion, this.coordinate, this.timeStamp,
      this.stepCnt});

  factory StepData.fromMap(Map map) {
    DateTime timeStamp = DateTime.fromMillisecondsSinceEpoch(map['timestamp']);
    return StepData(
      stepCnt: map['stepCnt'],
      coordinate: Coordinate(latitude: map['latitude'], longitude: map['longitude'],),
      timeStamp: timeStamp,
      acX: map['acX'],
      acY: map['acY'],
      acZ: map['acZ'],
      gyX: map['gyX'],
      gyY: map['gyY'],
      gyZ: map['gyZ'],
      maX: map['maX'],
      maY: map['maY'],
      maZ: map['maZ'],
      veX: map['veX'],
      veY: map['veY'],
      veZ: map['veZ'],
      veCos: map['veCos'],
      veAccuracy: map['veAccuracy'],
      pressure: map['pressure'],
      humidity: map['humidity'],
      tmpDegree: map['tmpDegree'],
      step: map['step'],
      motion: map['motion'],
      sigMotion: map['sigMotion'],
    );
  }

  Map<String, dynamic> toMap() {
    var map = Map<String, dynamic>();
    map['stepCnt'] = this.stepCnt;
    map['latitude'] = this.coordinate.latitude;
    map['longitude'] = this.coordinate.longitude;
    map['timestamp'] = this.timeStamp.millisecondsSinceEpoch;
    map['acX'] = this.acX;
    map['acY'] = this.acY;
    map['acZ'] = this.acZ;
    map['gyX'] = this.gyX;
    map['gyY'] = this.gyY;
    map['gyZ'] = this.gyZ;
    map['maX'] = this.maX;
    map['maY'] = this.maY;
    map['maZ'] = this.maZ;
    map['veX'] = this.veX;
    map['veY'] = this.veY;
    map['veZ'] = this.veZ;
    map['veCos'] = this.veCos;
    map['veAccuracy'] = this.veAccuracy;
    map['pressure'] = this.pressure;
    map['humidity'] = this.humidity;
    map['tmpDegree'] = this.tmpDegree;
    map['step'] = this.step;
    map['motion'] = this.motion;
    map['sigMotion'] = this.sigMotion;
    return map;
  }

  @override
  String toString() {
    return "stepCnt : $stepCnt, "
        "timestamp : $timeStamp, "
        "lat : ${coordinate.latitude}, "
        "lng : ${coordinate.longitude}, "
        "ac : $acX, $acY, $acZ ,"
        "ma : $maX, $maY, $maZ, "
        "ve : $veX, $veY, $veZ, $veCos, $veAccuracy, "
        "humidity : $humidity, pressure : $pressure, "
        "step : $step, motion : $motion, sigMotion : $sigMotion, ";
  }
}
