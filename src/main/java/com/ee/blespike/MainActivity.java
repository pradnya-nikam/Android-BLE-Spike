package com.ee.blespike;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.Region;

import java.lang.Override;
import java.util.List;

public class MainActivity extends Activity {

    public static final String EXTRAS_REGION = "extras_region";
    public static final String TAG = "BLE_SPIKE";
    Beacon beacon;
    static Context mContext;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.service_info);
        mContext = getApplicationContext();
        beacon = getIntent().getParcelableExtra(ListBeaconsActivity.EXTRAS_BEACON);
        Region region = new Region("", beacon.getProximityUUID(), beacon.getMajor(), beacon.getMinor());

        Intent bgServiceIntent = new Intent(this, RangingService.class);
        bgServiceIntent.putExtra(EXTRAS_REGION, region);
        bgServiceIntent.putExtra(ListBeaconsActivity.EXTRAS_BEACON, beacon);

        ServiceConnection serviceConnection =new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.i(TAG, "service connected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(TAG, "service disconnected");
            }
        };

        if(!isServiceRunning(RangingService.class.getSimpleName())) {
            startService(bgServiceIntent);
            bindService(bgServiceIntent, serviceConnection, 0);
        }

    }
    public static boolean isServiceRunning(String serviceClassName){
        final ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        final List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)){
                return true;
            }
        }
        return false;
    }

}