library lbs_pedometer;

import 'dart:async';
import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';
import 'dart:io' show Platform;

part 'src/pedometer.dart';
part 'src/callbacks.dart';
part 'src/method_name.dart';
part 'src/controller.dart';
part 'src/coordinate.dart';
part 'src/authorization_state.dart';
part 'src/step_data.dart';

const String _METHOD_CHANNEL_NAME = 'lbstech.net.plugin/lbs_pedoemter/method';
const String _EVENT_CHANNEL_NAME = 'lbstech.net.plugin/lbs_pedoemter/event';