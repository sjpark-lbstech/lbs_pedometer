part of lbs_pedometer;

typedef OnTakeStep = void Function(StepData stepData);

typedef OnAndroidResumed = void Function(List<StepData> stepList);

typedef OnEnd = void Function(List<StepData> stepList);
