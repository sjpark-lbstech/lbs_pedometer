package plugin.net.lbstech.lbs_pedometer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.HashMap;

import io.flutter.plugin.common.MethodChannel;

class LocationHandler {
    private static final int RQ_CODE_PERMISSION = 0xff01;

    private Context context;
    private Activity activity;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private int locationPermissionState = 22312;
    private boolean isLocationEnable;
    private boolean isGPSEnable;

    LocationHandler(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isLocationEnable = locationManager.isLocationEnabled();
            Log.i("PEDOLOG_LH", "isEnable : " + isLocationEnable);
        } else isLocationEnable = true;
        isGPSEnable = locationManager.isProviderEnabled("gps");
        Log.i("PEDOLOG_LH", "is GPS Enable : " + isGPSEnable);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    void getLocation(final MethodChannel.Result result) {
        if (!isLocationEnable || !isGPSEnable) {
            Log.i("PEDOLOG_LH", "GPS Location 을 사용할 수 없습니다.");
            return;
        }
        requestPermission();
        fusedLocationProviderClient.getLastLocation()
            .addOnSuccessListener(activity, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location current) {
                    if (current != null) {
                        Log.i("PEDOLOG_LH", "fused location : " + current.toString());
                        HashMap<String, Object> arg = new HashMap<String, Object>();
                        arg.put("lat", current.getLatitude());
                        arg.put("lng", current.getLongitude());
                        result.success(arg);
                    } else {
                        Log.i("PEDOLOG_LH", "initial location : null");
                        result.success(null);
                    }
                }
            })
            .addOnFailureListener(activity, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    result.error("Location Error...", e.getMessage(), null);
                }
            });
    }

    int getLocationPermissionState() {
        switch (locationPermissionState) {
            case PackageManager.PERMISSION_GRANTED:
                return 4; // plug-in 상수에서 granted.
            case PackageManager.PERMISSION_DENIED:
                return 2; // plug-in 상수에서 denied.
            default:
                return 1; // plug-in 상수에서 not determined.
        }
    }

    void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            locationPermissionState = PackageManager.PERMISSION_DENIED;
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, RQ_CODE_PERMISSION);
            } else {
                locationPermissionState = PackageManager.PERMISSION_GRANTED;
            }
        } else {
            locationPermissionState = PackageManager.PERMISSION_GRANTED;
        }
    }

}
