package plugin.net.lbstech.lbs_pedometer;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * LbsPedometerPlugin
 */
public class LbsPedometerPlugin implements FlutterPlugin,
        MethodCallHandler,
        EventChannel.StreamHandler,
        Application.ActivityLifecycleCallbacks,
        ActivityAware {
    private static final String METHOD_CHANNEL_NAME = "lbstech.net.plugin/lbs_pedoemter/method";
    private static final String EVENT_CHANNEL_NAME = "lbstech.net.plugin/lbs_pedoemter/event";

    private boolean isRegisterLifecycleCallBack;
    private boolean isInit;
    private boolean isServiceRunning;
    private MethodChannel methodChannel;
    private EventChannel eventChannel;
    private LocationHandler locationHandler;
    private ServiceManager serviceManager;
    private Context applicationContext;
    private AtomicInteger initialActivityHashCode = null;

    public static void registerWith(Registrar registrar) {
        new LbsPedometerPlugin(registrar);
    }

    public LbsPedometerPlugin() {
        isRegisterLifecycleCallBack = false;
        isInit = true;
    }

    private LbsPedometerPlugin(Registrar registrar) {
        this.serviceManager = new ServiceManager(registrar.context());
        this.locationHandler = new LocationHandler(registrar.context(), registrar.activity());
        this.methodChannel = new MethodChannel(registrar.messenger(), METHOD_CHANNEL_NAME);
        this.eventChannel = new EventChannel(registrar.messenger(), EVENT_CHANNEL_NAME);

        registrar.activity().getApplication().registerActivityLifecycleCallbacks(this);
        isRegisterLifecycleCallBack = true;
        isInit = true;
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        Log.i("PEDOLOG_PP", "onAttachedToEngine");
        applicationContext = flutterPluginBinding.getApplicationContext();
        methodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), METHOD_CHANNEL_NAME);
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), EVENT_CHANNEL_NAME);
        methodChannel.setMethodCallHandler(this);
        eventChannel.setStreamHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "getLocation":
                Log.i("PEDOLOG_PP", "getLocation method called");
                locationHandler.getLocation(result);
                break;
            case "getState":
                Log.i("PEDOLOG_PP", "getState method called");
                result.success(locationHandler.getLocationPermissionState());
                break;
            case "requestPermission":
                Log.i("PEDOLOG_PP", "requestPermission method called");
                locationHandler.requestPermission();
                serviceManager.requestPermission();
                result.success(null);
                break;
            case "getHistory":
                Log.i("PEDOLOG_PP", "getHistory method called");
                ArrayList<HashMap<String, Object>> list = SQLController.getInstance(applicationContext).getHistory();
                result.success(list);
                break;
            default:
                throw new NoSuchMethodError("'" + call.method + "' does not exist.");
        }
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
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    }


    // ================================= ActivityAware =================================


    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        Log.i("PEDOLOG_PP", "onAttachToActivity");
        Activity activity = binding.getActivity();

        if (isInit) {
            initialActivityHashCode = new AtomicInteger(activity.hashCode());
            isInit = false;
        }

        if (!isRegisterLifecycleCallBack) {
            activity.getApplication().registerActivityLifecycleCallbacks(this);
            isRegisterLifecycleCallBack = true;
        }

        if (locationHandler == null || serviceManager == null) {
            locationHandler = new LocationHandler(applicationContext, activity);
            serviceManager = new ServiceManager(applicationContext);
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
    public void onDetachedFromActivity() {
    }


    // ================================= life cycle call back ======================================

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        // created 이후에 registerActivityLifecycleCallbacks 함수가 불리기 떄문에
        // 실제로는 호출 되지 않음.
        Log.i("PEDOLOG_PP", "onCreated");
        methodChannel.setMethodCallHandler(this);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Log.i("PEDOLOG_PP", "onStarted");

        if (serviceManager == null) return;
        isServiceRunning = isServiceRunning(ServiceSensor.class);
        serviceManager.onStarted(isServiceRunning);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.i("PEDOLOG_PP", "onResumed");
        if (isServiceRunning) {
            ArrayList<HashMap<String, Object>> list = SQLController.getInstance(applicationContext).getHistory();
            methodChannel.invokeMethod("androidResume", list);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.i("PEDOLOG_PP", "onPaused");
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Log.i("PEDOLOG_PP", "onStopped");
        serviceManager.onStopped();
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.i("PEDOLOG_PP", "onDestroy");
        if (initialActivityHashCode != null && initialActivityHashCode.intValue() == activity.hashCode()) {
            methodChannel.setMethodCallHandler(null);
            activity.getApplication().unregisterActivityLifecycleCallbacks(this);
        }
    }

    // ================================= StreamHandler ======================================

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        Log.i("PEDOLOG_PP", "Event channel is opened.");

        if (arguments instanceof Map) {
            String title = (String) ((Map) arguments).get("title");
            String content = (String) ((Map) arguments).get("content");
            boolean storeHistory = (boolean) ((Map) arguments).get("storeHistory");
            int resultStepsUnit = (int) ((Map) arguments).get("resultStepsUnit");
            int storeStepsUnit = (int) ((Map) arguments).get("storeStepsUnit");

            if (!isServiceRunning(ServiceSensor.class)) {
                assert content != null && title != null;
                serviceManager.serviceStart(
                        title,
                        content,
                        storeHistory,
                        resultStepsUnit,
                        storeStepsUnit,
                        events
                );
            }
        }
    }

    @Override
    public void onCancel(Object arguments) {
        Log.i("PEDOLOG_PP", "Event channel is closed.");

        ArrayList<HashMap<String, Object>> list = serviceManager.serviceStop();
        methodChannel.invokeMethod("onCancel", list);
    }
}
