package plugin.net.lbstech.lbs_pedometer;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** LbsPedometerPlugin */
public class LbsPedometerPlugin implements FlutterPlugin,
        MethodCallHandler,
        Application.ActivityLifecycleCallbacks,
        ActivityAware {
  private static final String CHANNEL_NAME = "lbstech.net.plugin/lbs_pedoemter";
  private static MethodChannel channel;

  private static LocationHandler locationHandler;
  private static ServiceManager serviceManager;

  private static Context applicationContext;
  private static Activity activity;

  private static boolean isRegisterLifecycleCallBack = false;
  private static boolean isInit = true;
  private static AtomicInteger initialActivityHashCode = null;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    Log.i("PEDOLOG_PP", "onAttachedToEngine");
    applicationContext = flutterPluginBinding.getApplicationContext();
    LbsPedometerPlugin instance = new LbsPedometerPlugin();
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), CHANNEL_NAME);
    channel.setMethodCallHandler(this);
  }

  public static void registerWith(Registrar registrar) {
    LbsPedometerPlugin instance = new LbsPedometerPlugin();
    applicationContext = registrar.context();
    activity = registrar.activity();
    activity.getApplication().registerActivityLifecycleCallbacks(instance);
    isRegisterLifecycleCallBack = true;
    channel = new MethodChannel(registrar.messenger(), CHANNEL_NAME);
    channel.setMethodCallHandler(instance);

    serviceManager = new ServiceManager(applicationContext, activity, instance);
    locationHandler = new LocationHandler(applicationContext, activity, instance);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

    if (call.method.equals("getLocation"))
    {
      Log.i("PEDOLOG_PP", "getLocation method called");
      locationHandler.getLocation(result);
    }
    else if (call.method.equals("getState"))
    {
      Log.i("PEDOLOG_PP", "getState method called");
      result.success(locationHandler.getLocationPermissionState());
    }
    else if (call.method.equals("start"))
    {
      Log.i("PEDOLOG_PP", "start method called");

      if (!isServiceRunning(ServiceSensor.class)) {
        String title = call.argument("title");
        String content = call.argument("content");
        assert content != null && title != null;
        serviceManager.serviceStart(title, content);
      }
      result.success(null);
    }
    else if (call.method.equals("stop"))
    {
      Log.i("PEDOLOG_PP", "stop method called");
      ArrayList<double[]> list = serviceManager.serviceStop();
      result.success(list);
    }
    else if (call.method.equals("requestPermission"))
    {
      Log.i("PEDOLOG_PP", "requestPermission method called");
      locationHandler.requestPermission();
      serviceManager.requestPermission();
      result.success(null);
    }
    else if(call.method.equals("getHistory"))
    {
      Log.i("PEDOLOG_PP", "getHistory method called");
      ArrayList<double[]> list = SQLController.getInstance(applicationContext).getHistory();
      result.success(list);
    }

  }

  void sendDataToFlutter(Object data){
    channel.invokeMethod("takeSteps", data);
  }

  private boolean isServiceRunning(Class<?> serviceClass) {
    ActivityManager manager = (ActivityManager) applicationContext.getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      if (serviceClass.getName().equals(service.service.getClassName())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) { }


  // ================================= ActivityAware =================================


  @Override
  public void onAttachedToActivity(ActivityPluginBinding binding) {
    Log.i("PEDOLOG_PP", "onAttachToActivity");
    activity = binding.getActivity();
    if (isInit){
      initialActivityHashCode = new AtomicInteger(activity.hashCode());
      isInit = false;
    }
    if (!isRegisterLifecycleCallBack) {
      activity.getApplication().registerActivityLifecycleCallbacks(this);
      isRegisterLifecycleCallBack = true;
    }

    if(locationHandler == null || serviceManager == null) {
      locationHandler = new LocationHandler(applicationContext, activity, this);
      serviceManager = new ServiceManager(applicationContext, activity, this);
    }
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    this.onDetachedFromActivity();
  }

  @Override
  public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
    binding.getActivity().getApplication().registerActivityLifecycleCallbacks(this);
  }

  @Override
  public void onDetachedFromActivity() { }


  // ================================= life cycle call back ======================================

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    // created 이후에 registerActivityLifecycleCallbacks 함수가 불리기 떄문에
    // 실제로는 호출 되지 않음.
    Log.i("PEDOLOG_PP", "onCreated");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onActivityStarted(Activity activity) {
    Log.i("PEDOLOG_PP", "onStarted");

    if (serviceManager == null) return;
    serviceManager.onStarted(isServiceRunning(ServiceSensor.class));
  }

  @Override
  public void onActivityResumed(Activity activity) {
    Log.i("PEDOLOG_PP", "onResumed");
    if(isServiceRunning(ServiceSensor.class)){
      ArrayList<double[]> list = SQLController.getInstance(applicationContext).getHistory();
      channel.invokeMethod("androidResume", list);
    }
  }

  @Override
  public void onActivityPaused(Activity activity) {
    Log.i("PEDOLOG_PP", "onPaused");
  }

  @Override
  public void onActivityStopped(Activity activity) {
    Log.i("PEDOLOG_PP", "onStopped");
    if(isServiceRunning(ServiceSensor.class)){
      serviceManager.onStopped();
    }
  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }

  @Override

  public void onActivityDestroyed(Activity activity) {
    Log.i("PEDOLOG_PP", "onDestroy");
    if (initialActivityHashCode != null && initialActivityHashCode.intValue() == activity.hashCode()) {
      channel.setMethodCallHandler(null);
      activity.getApplication().unregisterActivityLifecycleCallbacks(this);
    }
  }

}
