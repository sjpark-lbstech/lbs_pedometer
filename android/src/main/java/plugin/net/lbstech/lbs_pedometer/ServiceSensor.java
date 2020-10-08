package plugin.net.lbstech.lbs_pedometer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Calendar;
import java.util.List;

import io.flutter.Log;

public class ServiceSensor extends Service{
    private final int MSG_INIT = 0XAA1;
    private final int MSG_SENSOR_TRIGGER = 0XAA2;
    private final int MSG_LIFECYCLE_START = 0XAA3;
    private final int MSG_LIFECYCLE_STOP = 0XAA4;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Messenger appMessenger;
    private SQLController sqlController;

    static Location currentSavedLocation;
    static Calendar startTime;

    boolean isAppAlive() { return appMessenger != null; }

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
        sqlController = SQLController.getInstance(getApplicationContext());
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
    }

    private void locationStart() {
        // 시작할때 flush.
        fusedLocationProviderClient.flushLocations();

        locationCallback = new PedometerLocationCallback();
        LocationRequest request = LocationRequest.create();
        request.setInterval(10000);
        request.setFastestInterval(10000);
        request.setMaxWaitTime(20010);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationProviderClient.requestLocationUpdates(request, locationCallback, null);

        startTime = Calendar.getInstance();
    }

    // onDestroy 에서 호출
    private void sensorStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        locationCallback = null;
    }


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
                if (isAppAlive()) {
                    currentSavedLocation = locationList.get(locationList.size() -1);
                    try {
                        Message msg = Message.obtain(null, MSG_SENSOR_TRIGGER);
                        appMessenger.send(msg);
                    } catch (RemoteException e) {
                        Log.i("PEDOLOG_SM", "try to send, but Service doesn't exist.." +
                                "\nmsg: MSG_LIFECYCLE_STOP");
                        e.printStackTrace();
                    }
                }
                int len = locationList.size();
                Log.i("PEDOLOG_SS", "위치 갱신");
                for (int i = 0; i < len; i++) {
                    Log.i("PEDOLOG_SS", i + ":" + locationList.get(i).getLatitude() + ", " + locationList.get(i).getLongitude());
                    sqlController.insert(locationList.get(i).getLatitude(), locationList.get(i).getLongitude());
                }
            }
            if (Calendar.getInstance().getTimeInMillis() - ServiceSensor.startTime.getTimeInMillis()
                    > 43200000){
                // 12 시간 초과시
                sensorStop();
            }
        }
    }
}
