package plugin.net.lbstech.lbs_pedometer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ServiceSensor extends Service implements SensorEventListener {
    private final int MSG_INIT = 0XAA1;
    private final int MSG_SENSOR_TRIGGER = 0XAA2;
    private final int MSG_LIFECYCLE_START = 0XAA3;
    private final int MSG_LIFECYCLE_STOP = 0XAA4;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private SensorManager sensorManager;
    private Messenger appMessenger;
    private SQLController sqlController;

    private int stepCnt;
    static int currentSavedStepCnt = 0;
    private Timer timer = new Timer();
    private SaveTask saveTask = new SaveTask();

    private boolean isAppAlive() { return appMessenger != null; }


    // sensor 값 저장을 위한 변수 선언 부분

    // fused location
    private Location location;
    // TYPE_ACCELEROMETER
    float acX, acY, acZ;
    // TYPE_GYROSCOPE
    float gyX, gyY, gyZ;
    // TYPE_MAGNETIC_FIELD
    float maX, maY, maZ;
    // TYPE_PRESSURE
    float pressure;
    // TYPE_ROTATION_VECTOR
    float veX, veY, veZ, veCos, veAccuracy;
    // TYPE_AMBIENT_TEMPERATURE
    float tmpDegree;
    // TYPE_RELATIVE_HUMIDITY
    float humidity;
    // TYPE_MOTION_DETECT, TYPE_STEP_DETECTOR, TYPE_SIGNIFICANT_MOTION
    // 해당 불린은 저장이후 false 로 변환
    boolean step, motion, sigMotion;

    // ==================================== anonymous class =====================================
    // message handler - APP 으로부터의 메세지를 다루는 익명 클래스
    @SuppressLint("HandlerLeak")
    private Messenger messenger = new Messenger(new Handler(){
        @Override
        public void handleMessage(Message msg) {
            int data = msg.what;
            switch (data){
                case MSG_INIT:
                    Log.i("PEDOLOG_SS", "received msg: MSG_INIT");
                    appMessenger = msg.replyTo;
                    sensorStart();
                    break;
                case MSG_LIFECYCLE_START:
                    Log.i("PEDOLOG_SS", "received msg: MSG_LIFECYCLE_START");
                    appMessenger = msg.replyTo;
                    break;
                case MSG_LIFECYCLE_STOP:
                    Log.i("PEDOLOG_SS", "received msg: MSG_LIFECYCLE_STOP");
                    appMessenger = null;
                    break;
            }
            super.handleMessage(msg);
        }
    });


    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(getApplicationContext());
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sqlController = SQLController.getInstance(getApplicationContext());
        stepCnt = 0;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i("PEDOLOG_SS", "onBind called.");
        return messenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("PEDOLOG_SS", "onStartCommand called.");
        if(intent != null) {
            Bundle bundle = intent.getExtras();
            String title = "GPS Activating";
            String text = "App is receive location update";
            if (bundle != null) {
                if (bundle.containsKey("title")) title = bundle.getString("title");
                if (bundle.containsKey("text")) text = bundle.getString("text");
            }
            Notification notification = buildNotification(title, text);
            startForeground(1, notification);
        }
        return START_STICKY;
    }

    private Notification buildNotification(String title, String text){
        NotificationCompat.Builder builder;
        String CHANNEL_ID = "lbstech_service_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID,
                    NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);
        }
        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setShowWhen(false)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_small_noti);

        return builder.build();
    }

    @Override
    public void onDestroy() {
        Log.i("PEDOLOG_SS", "onDestroy called.");
        sensorStop();
        sqlController.clear();
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("PEDOLOG_SS", "onUnbind called.");
        sqlController.clear();
        return super.onUnbind(intent);
    }

    // ================================== sensor 관련 =====================================

    // MSG_INIT 받으면 호출
    private void sensorStart(){
        locationStart();

        int[] sensorTypes = new int[]{
                Sensor.TYPE_ACCELEROMETER,
                Sensor.TYPE_GYROSCOPE,
                Sensor.TYPE_MAGNETIC_FIELD,
                Sensor.TYPE_PRESSURE,
                Sensor.TYPE_ROTATION_VECTOR,
                Sensor.TYPE_STEP_DETECTOR,
                Sensor.TYPE_AMBIENT_TEMPERATURE,
                Sensor.TYPE_RELATIVE_HUMIDITY,
                Sensor.TYPE_SIGNIFICANT_MOTION,
        };
        Sensor tmp;
        for (int type : sensorTypes) {
            tmp = sensorManager.getDefaultSensor(type);
            if (tmp != null){
                sensorManager.registerListener(this, tmp, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            Sensor motion = sensorManager.getDefaultSensor(Sensor.TYPE_MOTION_DETECT);
            if (motion != null) {
                sensorManager.registerListener(this, motion, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        timer.schedule(saveTask, 1000, 1000);
    }

    private void locationStart() {
        locationCallback = new PedometerLocationCallback();
        LocationRequest request = LocationRequest.create();
        request.setInterval(100);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationProviderClient.requestLocationUpdates(request, locationCallback, null);
    }

    // onDestroy 에서 호출
    private void sensorStop() {
        timer.cancel();
        sensorManager.unregisterListener(this);
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        locationCallback = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        float[] values = event.values;
        switch (type){
            case Sensor.TYPE_STEP_DETECTOR :
                step = true;
                ++stepCnt;
                currentSavedStepCnt = stepCnt;
                if(stepCnt % 10 == 2 && location != null){
                    if(isAppAlive()){
                        try {
                            Message msg = Message.obtain(null, MSG_SENSOR_TRIGGER);
                            appMessenger.send(msg);
                        }catch (RemoteException e){
                            Log.i("PEDOLOG_SM", "try to send, but Service doesn't exist.." +
                                    "\nmsg: MSG_LIFECYCLE_STOP");
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case Sensor.TYPE_ACCELEROMETER :
                acX = values[0];
                acY = values[1];
                acZ = values[2];
                break;
            case Sensor.TYPE_GYROSCOPE :
                gyX = values[0];
                gyY = values[1];
                gyZ = values[2];
                break;
            case Sensor.TYPE_MAGNETIC_FIELD :
                maX = values[0];
                maY = values[1];
                maZ = values[1];
                break;
            case Sensor.TYPE_ROTATION_VECTOR :
                veX = values[0];
                veY = values[1];
                veZ = values[2];
                veCos = values[3];
                veAccuracy = values[4];
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE :
                tmpDegree = values[0];
                break;
            case Sensor.TYPE_RELATIVE_HUMIDITY :
                humidity = values[0];
                break;
            case Sensor.TYPE_PRESSURE :
                pressure = values[0];
                break;
            case Sensor.TYPE_MOTION_DETECT :
                motion = true;
                break;
            case Sensor.TYPE_SIGNIFICANT_MOTION :
                sigMotion = true;
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}


    // ================================== inner class ===========================================

    class PedometerLocationCallback extends LocationCallback {
        boolean isFirst = true;
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (isFirst) {
                Log.i("PEDOLOG_SS", "location update start.");
                isFirst = false;
            }
            if (locationResult != null){
                List<Location> locationList = locationResult.getLocations();
                location = locationList.get(locationList.size() - 1);
            }
        }
    }


    // ======================= Timer Task 상속받는 inner class ==================
    class SaveTask extends TimerTask{
        @Override
        public void run() {
            double lat, lng;
            if(location == null) lat = lng = 0.0;
            else {
                lat = location.getLatitude();
                lng = location.getLongitude();
            }
            sqlController.insert(stepCnt, lat, lng,
                    acX, acY, acZ, gyX, gyY, gyZ, maX, maY, maZ, veX, veY, veZ, veCos, veAccuracy,
                    pressure, tmpDegree, humidity, step, motion, sigMotion);
            step = false;
            motion = false;
            sigMotion = false;
        }
    }
}
