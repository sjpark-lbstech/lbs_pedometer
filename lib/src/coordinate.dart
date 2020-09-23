part of lbs_pedometer;

/// latitude, longitude 를 가지는 객체
class Coordinate{

  final double latitude;

  final double longitude;

  Coordinate({this.latitude = 0.0, this.longitude = 0.0});

  factory Coordinate._fromJson(List data){
    return Coordinate(latitude: data[0], longitude: data[1]);
  }

  @override
  String toString() {
    return 'Coordinate >> 위경도 : $latitude, $longitude';
  }
}