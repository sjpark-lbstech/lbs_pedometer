import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:lbs_pedometer/lbs_pedometer.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final Pedometer _pedometer = Pedometer();

  static List<StepData> steps = [];

  @override
  void initState() {
    super.initState();
    _pedometer.requestPermission();
    _pedometer.getHistory().then((list) {
      if (list != null){
        steps.addAll(list);
        _pedometer.start(onTakeStep: onTakeStep);
        for(StepData data in list){
          print(data.toString());
        }

        if (mounted){
          setState(() { });
        }
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
          body: SafeArea(
            child: Container(
              padding: EdgeInsets.all(16),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: <Widget>[

                  Container(
                    padding: EdgeInsets.all(8),
                    child: Text(
                      'background pedometer plug in'.toUpperCase(),
                      style: TextStyle(fontWeight: FontWeight.w900),
                    ),
                  ),

                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: <Widget>[
                      MaterialButton(
                        padding: EdgeInsets.all(16),
                        onPressed: clickLocation,
                        child: Center(
                          child: Text(
                            'LOCATION',
                            style: TextStyle(color: Colors.blue),
                          ),
                        ),
                      ),
                      MaterialButton(
                        padding: EdgeInsets.all(16),
                        onPressed: clickStart,
                        child: Center(
                          child: Text(
                            'START',
                            style: TextStyle(color: Colors.blue),
                          ),
                        ),
                      ),
                      MaterialButton(
                        padding: EdgeInsets.all(16),
                        onPressed: clickStop,
                        child: Center(
                          child: Text(
                            'STOP',
                            style: TextStyle(color: Colors.blue),
                          ),
                        ),
                      ),
                    ],
                  ),

                  SizedBox(height: 30,),

                  Expanded(
                    child: ListView.builder(
                      itemBuilder: (context, pos){
                        DateTime time = steps[pos].timeStamp;
                        return ListTile(
                          title: Text(time.toIso8601String()),
                          subtitle: Text('lat: ${steps[pos].coordinate.latitude}, '
                              '\nlng: ${steps[pos].coordinate.latitude}'),
                        );
                      },
                      itemCount: steps.length,
                    ),
                  ),

                ],
              ),
            ),
          )
      ),
    );
  }

  void clickStart() {
    _pedometer.start(
      onTakeStep: onTakeStep,
      onAndroidResumed: onAndroidResume,
    );
  }

  void clickStop() async{
    _pedometer.stop(onEnd: (list){
      for(StepData data in list){
        print(data);
      }
    });
    steps.clear();
    if (mounted){
      setState(() { });
    }
  }

  void clickLocation() {
    print(DateTime.now().millisecondsSinceEpoch);
    _pedometer.getLocation().then((coordinate){
      print("lat : ${coordinate.latitude}, lng : ${coordinate.longitude}");
      print(DateTime.now().millisecondsSinceEpoch);
    });
  }

  void onTakeStep(StepData stepData) {
    print(stepData);
    steps.add(stepData);
    if (mounted){
      setState(() { });
    }
  }

  void onAndroidResume(List<StepData> stepList) {
    for(StepData data in stepList){
      print(data);
    }
    steps.clear();
    steps.addAll(stepList);
    if(mounted){
      setState(() { });
    }
  }
}
