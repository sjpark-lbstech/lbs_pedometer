part of lbs_pedometer;

class OutlierFilter {
  /// ### 거리 변화량 제한값.
  /// 이 값을 사용해서 filter 를 작동시킨다.
  double threshold;
  
  /// 최초에 받은 GPS 리스트
  final List<Coordinate> _rawGPS;

  /// 각 GPS 사이의 거리를 가지는 [List].
  ///
  /// [_rawGPS]의 길이 -1의 길이를 가진다.
  final List<double> _diff = [];

  /// 각 [_diff]들 간의 차이를 가지는 [List].
  ///
  /// 즉, 점 사이의 거리 변화량이며, [_dist]의 첫번째 값은 [_diff]의 첫번째 값과 같다.
  /// ```
  /// assert(_dist[0] == _diff[0]);
  /// ```
  final List<double> _dist = [];

  /// 표쥰점수
  final List<double> _zScore = [];

  /// 각 GPS 점간의 평군 거리
  double _averOfDiff;

  /// 거리의 평균 변동량
  double _averOfDist;

  /// GPS 사이의 거리들의 표준편차
  double _standardDeviation;

  /// ### [OutlierFilter] 생성자
  /// 여과되지 않은 GPS를 받아와서 통계적 수치들을 계산
  OutlierFilter(this._rawGPS) {
    int rawSize = _rawGPS.length;

    double sumOfDist = 0.0;
    double sumOfDiff = 0.0;
    for (int i = 0; i < rawSize - 1; i ++) {
      final d = _distance(_rawGPS[i], _rawGPS[i + 1]);
      _dist.insert(i, d);
      sumOfDist += d;

      final dif = i == 0 ? d.abs() : (d - _dist[i - 1]).abs();
      _diff.insert(i, dif);
      sumOfDiff += dif;
    }
    _averOfDist = sumOfDist / _dist.length;
    _averOfDiff = sumOfDiff / _diff.length;

    double sumOfSquaredDeviation = 0.0;
    for (double dist in _dist) {
      sumOfSquaredDeviation = pow(dist - _averOfDist, 2);
    }
    _standardDeviation = sqrt(sumOfSquaredDeviation / (_dist.length - 1));
    for (int i = 0; i < rawSize - 1; i ++) {
      _zScore.insert(i, (_dist[i] - _averOfDist) / _standardDeviation);
    }

    // 예측가능한 최대 거리 이동량 - (평균 거리변동량 X 2) -> 가중치
    threshold = _averOfDist*sqrt(_dist.length) - _averOfDiff*2;
  }

  /// 두 GPS 포인트간의 거리를 반환하는 함수
  double _distance(Coordinate from, Coordinate to){
    double theta, dist;
    theta = from.longitude - to.longitude;
    dist = sin(radians(from.latitude)) * sin(radians(to.latitude)) +
        cos(radians(from.latitude)) *
            cos(radians(to.latitude)) *
            cos(radians(theta));
    dist = acos(dist);
    dist = degrees(dist);
    dist = dist * 60 * 1.1515;
    dist = dist * 1.609344;
    dist = dist * 1000.0;
    return dist >= 0 ? dist : -dist;
  }

  /// ### 이상치를 제거하여 필터를 적용한 GPS 리스트를 만들어내는 함수
  List<Coordinate> refine() {
    List<int> outliersIndex = [];

    for (int i = 0; i < _dist.length; i ++) {
      if (_dist[i] >= threshold) {
        print('이상치 인덱스 탐지: idx : $i, 간격: ${_dist[i]}, gps : ${_rawGPS[i + 1]}, z-score: ${_zScore[i]}');
        outliersIndex.add(i);
      }
    }

    int fromIdx = 0;
    int toIdx;
    List<List<Coordinate>> _subLists = [];
    for (int i = 0; i < outliersIndex.length; i++) {
      toIdx = outliersIndex[i] + 1;
      _subLists.add(_rawGPS.sublist(fromIdx, toIdx));
      fromIdx = outliersIndex[i] + 1;
    }
    _subLists.add(_rawGPS.sublist(fromIdx));

    List<Coordinate> result = [];
    if (_subLists.isEmpty) {
      result = _rawGPS;
    }else if (_subLists.length == 2) {
      _subLists.sort((a, b) => b.length - a.length);
      result.addAll(_subLists.first);
    }else { // 3개 이상
      for (int i = 0; i < _subLists.length; i ++) {
        if (_subLists[i].length > 12){
          result.addAll(_subLists[i]);
        }else {
          if (i + 2 < _subLists.length &&
              _distance(_subLists[i].last, _subLists[i+2].first) <= threshold) {
            // 중간의 이상치들을 넘어서 접점이 있다
            result.addAll(_subLists[i]);
          }else if (i - 2 > 0 &&
              _distance(_subLists[i].last, _subLists[i-2].first) <= threshold) {
            result.addAll(_subLists[i]);
          }else {
            _reportOutlier(_subLists[i]);
          }
        }
      }
    }
    return result;
  }

  /// 제거될 이상치를 발견한 경우 이를 print하여 알려주는 함수.
  void _reportOutlier(List<Coordinate> outlier) {
    print("이상치 발견 GPS 길이: ${outlier.length}");
    outlier.forEach((coordinate) {
      print("${coordinate.latitude}, ${coordinate.longitude}");
    });
  }

  @override
  String toString() {
    return '표준편차 : $_standardDeviation\n'
        '이분 지표: ${_averOfDist*sqrt(_dist.length) - _averOfDiff*1.5}\n'
        '평균 간격 : $_averOfDist\n'
        '평균 변화량 : $_averOfDiff\n';
  }
}