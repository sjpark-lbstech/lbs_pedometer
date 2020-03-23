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

    private Location location;
    private int stepCnt;
    static int currentSavedStepCnt = 0;
    private boolean isAppAlive() { return appMessenger != null; }

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
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null) {
            Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
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
        sensorManager.unregisterListener(this);
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        locationCallback = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        switch (type){
            case Sensor.TYPE_STEP_DETECTOR :
                ++stepCnt;
                Log.i("PEDOLOG_SS", "step detector. Steps : " + stepCnt);
                if(stepCnt % 10 == 2 && location != null){
                    currentSavedStepCnt = stepCnt;
                    sqlController.insert(stepCnt, location.getLatitude(), location.getLongitude());
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
}
