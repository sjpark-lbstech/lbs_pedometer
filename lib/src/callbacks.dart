part of lbs_pedometer;

typedef OnTakeStep = void Function(Coordinate coordinate);

typedef OnAndroidResumed = void Function(List<Coordinate> coordinates);

typedef OnEnd = void Function(List<Coordinate> coordinates);
