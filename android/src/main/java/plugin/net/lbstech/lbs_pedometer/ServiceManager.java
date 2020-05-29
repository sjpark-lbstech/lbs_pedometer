package plugin.net.lbstech.lbs_pedometer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.EventChannel;

public class ServiceManager implements ServiceConnection {
    static final int MSG_INIT = 0XAA1;
    static final int MSG_SENSOR_TRIGGER = 0XAA2;
    static final int MSG_LIFECYCLE_START = 0XAA3;
    static final int MSG_LIFECYCLE_STOP = 0XAA4;

    private Context context;

    private SQLController sqlController;
    private Messenger serviceMessenger = null;
    private Messenger pluginMessenger = new Messenger(new Receiver());

    private boolean isServiceConnected() {
        return serviceMessenger != null;
    }

    private boolean isRebind = false;

    private boolean storeHistory;
    private int resultStepsUnit;
    private int storeStepsUnit;
    private EventChannel.EventSink eventSink;

    ServiceManager(Context context) {
        this.context = context;
        sqlController = SQLController.getInstance(context);
    }

    // 서비스 시작
    void serviceStart(
            @NonNull String notificationTitle,
            @NonNull String notificationContent,
            boolean storeHistory,
            int resultStepsUnit,
            int storeStepsUnit,
            EventChannel.EventSink eventSink
    ) {
        this.storeHistory = storeHistory;
        this.resultStepsUnit = resultStepsUnit;
        this.storeStepsUnit = storeStepsUnit;
        this.eventSink = eventSink;

        Intent serviceIntent = new Intent(context, ServiceSensor.class);
        serviceIntent.putExtra("title", notificationTitle);
        serviceIntent.putExtra("text", notificationContent);
        ContextCompat.startForegroundService(context, serviceIntent);

        serviceBind();
    }

    // App 이 start 된 시점에서 서비스 돌아가면 rebind 하기 위해서 분리.
    void serviceBind() {
        context.bindService(new Intent(context, ServiceSensor.class), this, 0);
        Log.i("PEDOLOG_SM", "startForegroundSensor _ Service Binding");
    }

    // 서비스 종료
    ArrayList<HashMap<String, Object>> serviceStop() {
        if (isServiceConnected()) {
            ArrayList<HashMap<String, Object>> result = sqlController.getHistory();
            try {
                context.unbindService(this);
                Intent serviceIntent = new Intent(context, ServiceSensor.class);
                context.stopService(serviceIntent);
            } catch (Exception e) {
                Log.i("ANDOIRD_SM", "Error occurred :" + e.getLocalizedMessage());
            }
            serviceMessenger = null;
            isRebind = false;
            return result;
        } else {
            return null;
        }
    }

    // =================================== life cycle 관련 ========================================
    // 해당 라이프 사이클에 호출되는 메서드들

    void requestPermission() {
        // android Q 버전 부터 동적 퍼미션 필요.
        // Q's API level : 29
        if (Build.VERSION.SDK_INT >= 29) {
            // TODO : API 29 부터 ACCESS_BACKGROUND_LOCATION 동적 퍼미션 체크.
        }
    }

    void onStarted(boolean isServiceRunning) {
        // 만약 서비스가 진짜 돌아가면 rebind.
        Log.i("PEDOLOG_SM", "onStart. 서비스 존재 여부 : " + isServiceRunning);
        if (isServiceRunning) {
            isRebind = true;
            serviceBind();

            try {
                Message msg = Message.obtain(null, MSG_LIFECYCLE_START);
                msg.replyTo = pluginMessenger;
                serviceMessenger.send(msg);
                isRebind = true;
                Log.i("PEDOLOG_SM", "service attached");
            } catch (RemoteException e) {
                Log.i("PEDOLOG_SM", "try to send, but Service doesn't exist.." +
                        "\nmsg: MSG_INIT");
                e.printStackTrace();
            }
        }
    }

    void onStopped() {
        if (isServiceConnected()) {
            try {
                Message msg = Message.obtain(null, MSG_LIFECYCLE_STOP);
                serviceMessenger.send(msg);
            } catch (RemoteException e) {
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

        Map obj = new HashMap<String, Object>();
        obj.put("storeHistory", storeHistory);
        obj.put("resultStepsUnit", resultStepsUnit);
        obj.put("storeStepsUnit", storeStepsUnit);

        try {
            Message msg = Message.obtain(null, what, obj);
            msg.replyTo = pluginMessenger;
            serviceMessenger.send(msg);
            isRebind = true;
            Log.i("PEDOLOG_SM", "service attached");
        } catch (RemoteException e) {
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
                eventSink.success(msg.obj);
            }
            super.handleMessage(msg);
        }
    }

}
