package plugin.net.lbstech.lbs_pedometer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class ServiceManager implements ServiceConnection {
    private final int MSG_INIT = 0XAA1;
    private final int MSG_SENSOR_TRIGGER = 0XAA2;
    private final int MSG_LIFECYCLE_START = 0XAA3;
    private final int MSG_LIFECYCLE_STOP = 0XAA4;

    private Context context;
    private Activity activity;
    private LbsPedometerPlugin plugin;

    private SQLController sqlController;
    private Messenger serviceMessenger = null;
    private Messenger pluginMessenger = new Messenger(new Receiver());

    private boolean isServiceConnected() { return serviceMessenger != null; }
    private boolean isRebind = false;

    ServiceManager(Context context, Activity activity, LbsPedometerPlugin plugin) {
        this.context = context;
        this.activity = activity;
        this.plugin = plugin;
        sqlController = SQLController.getInstance(context);
    }

    // 서비스 시작
    void serviceStart(@NonNull String notificationTitle, @NonNull String notificationContent) {
        Intent serviceIntent = new Intent(context, ServiceSensor.class);
        serviceIntent.putExtra("title", notificationTitle);
        serviceIntent.putExtra("text", notificationContent);
        ContextCompat.startForegroundService(context, serviceIntent);

        serviceBind();
    }

    // App 이 start 된 시점에서 서비스 돌아가면 rebind 하기 위해서 분리.
    void serviceBind(){
        context.bindService(new Intent(context, ServiceSensor.class), this, 0);
        Log.i("PEDOLOG_SM", "startForegroundSensor _ Service Binding");
    }

    // 서비스 종료
    ArrayList<double[]> serviceStop(){
        ArrayList<double[]> result = sqlController.getHistory();
        serviceMessenger = null;
        isRebind = false;
        sqlController.clear();
        if (isServiceConnected()) {
            try {
                context.unbindService(this);
                Intent serviceIntent = new Intent(context, ServiceSensor.class);
                context.stopService(serviceIntent);
            } catch (Exception e){
                Log.i("ANDOIRD_SM", "Error occurred :" + e.getLocalizedMessage());
            }
        }
        return result;
    }

    // =================================== life cycle 관련 ========================================
    // 해당 라이프 사이클에 호출되는 메서드들

    void requestPermission(){
        // android Q 버전 부터 동적 퍼미션 필요.
        // Q's API level : 29

    }

    void onStarted(boolean isServiceRunning){
        // 만약 서비스가 진짜 돌아가면 rebind.
        Log.i("PEDOLOG_SM", "onStart. 서비스 존재 여부 : " + isServiceRunning);
        if (isServiceRunning){
            isRebind = true;
            serviceBind();
            try {
                Message msg = Message.obtain(null, MSG_LIFECYCLE_START);
                msg.replyTo = pluginMessenger;
                serviceMessenger.send(msg);
                isRebind = true;
                Log.i("PEDOLOG_SM", "service attached");
            }catch (RemoteException e){
                Log.i("PEDOLOG_SM", "try to send, but Service doesn't exist.." +
                        "\nmsg: MSG_INIT");
                e.printStackTrace();
            }
        }
    }

    void onStopped(){
        if (isServiceConnected()){
            try {
                Message msg = Message.obtain(null, MSG_LIFECYCLE_STOP);
                serviceMessenger.send(msg);
            }catch (RemoteException e){
                Log.i("PEDOLOG_SM", "try to send, but Service doesn't exist.." +
                        "\nmsg: MSG_LIFECYCLE_STOP");
                e.printStackTrace();
            }
        }
    }

    // ============================= ServiceConnection implementation =========================
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        serviceMessenger = new Messenger(service);
        int what = isRebind ? MSG_LIFECYCLE_START : MSG_INIT;

        try {
            Message msg = Message.obtain(null, what);
            msg.replyTo = pluginMessenger;
            serviceMessenger.send(msg);
            isRebind = true;
            Log.i("PEDOLOG_SM", "service attached");
        }catch (RemoteException e){
            Log.i("PEDOLOG_SM", "try to send, but Service doesn't exist.." +
                    "\nmsg: MSG_INIT");
            e.printStackTrace();
        }

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.i("PEDOLOG_SM", name.getClassName() + ": successfully disconnected");
    }

    // ===================================== inner class ========================================
    // message handler - 서비스로부터의 메세지를 다루는 내부 클래스
    @SuppressLint("HandlerLeak")
    class Receiver extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int data = msg.what;
            if (data == MSG_SENSOR_TRIGGER) {
                Log.i("PEDOLOG_SM", "receive trigger.");
                Location last = ServiceSensor.currentSavedLocation;
                if (last == null) return;
                plugin.sendDataToFlutter(new double[]{last.getLatitude(), last.getLongitude()});
            }
            super.handleMessage(msg);
        }
    }

}
