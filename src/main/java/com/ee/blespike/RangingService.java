package com.ee.blespike;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class RangingService extends Service {

    private BeaconManager beaconManager;
    private Region region;
    private NotificationManager notificationManager;
    private static int NOTIFICATION_ID = 1;
    private List<Beacon> inRegionBeacons;
    private Beacon beacon;
    private List<Beacon> inRangeBeacons;


    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        inRegionBeacons = new ArrayList<Beacon>();
        inRangeBeacons = new ArrayList<Beacon>();

        beaconManager = new BeaconManager(this);

        beaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(1), 0);
        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> monitoredBeacons) {
                Beacon foundBeacon = null;
                for (Beacon monitoredBeacon : monitoredBeacons) {
                    if (monitoredBeacon.getMacAddress().equals(beacon.getMacAddress())) {
                        foundBeacon = monitoredBeacon;
                    }
                }
                if(foundBeacon!=null)
                    if(!inRegionBeacons.contains(foundBeacon)) {
                        inRegionBeacons.add(foundBeacon);
                        postNotification("Welcome to EE!");

                        try {
                            beaconManager.startRanging(region);

                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }

                    }
            }

            @Override
            public void onExitedRegion(Region region) {
                Log.i(MainActivity.TAG,"Exited " + region.getProximityUUID() + "  :  " +region.getMajor());
                for(Beacon inRegionBeacon : inRegionBeacons){
                    if(inRegionBeacon.getProximityUUID().equalsIgnoreCase(region.getProximityUUID()) && inRegionBeacon.getMajor() == region.getMajor()) {
                        inRegionBeacons.remove(inRegionBeacon);
                        inRangeBeacons.remove(inRegionBeacon);
                        postNotification("Bye from EE!");

                        try {
                            beaconManager.stopRanging(region);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }

                    }
                }


            }
        });
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> rangedBeacons) {
//                // Note that results are not delivered on UI thread.
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
                        // Just in case if there are multiple beacons with the same uuid, major, minor.
                        Beacon foundBeacon = null;
                        for (Beacon rangedBeacon : rangedBeacons) {
                            if (rangedBeacon.getMacAddress().equals(beacon.getMacAddress())) {
                                foundBeacon = rangedBeacon;
                            }
                        }
                        if (foundBeacon != null) {

                            if(foundBeacon.getRssi() > -70) {
                                if(!inRangeBeacons.contains(foundBeacon)) {
                                    inRangeBeacons.add(foundBeacon);
                                    postNotification("Welcome to Giftbak Desk!");
                                }
                            }else{
                                if(inRangeBeacons.contains(foundBeacon)) {
                                    inRangeBeacons.remove(foundBeacon);
                                    postNotification("Bye from Giftbak!");
                                }

                            }

                        }
                    }
                });
//            }
//        });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

       Log.i(MainActivity.TAG, "FLAGS " + flags + "    FLAGS " + intent.getFlags()) ;

        region = intent.getParcelableExtra(MainActivity.EXTRAS_REGION);
        beacon = intent.getParcelableExtra(ListBeaconsActivity.EXTRAS_BEACON);
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {

                    beaconManager.startMonitoring(region);

                } catch (RemoteException e) {
                    Log.d(MainActivity.TAG, "Error while starting monitoring");
                }
            }
        });
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void postNotification(String msg) {
        Intent notifyIntent = new Intent(RangingService.this, RangingService.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivities(
                RangingService.this,
                0,
                new Intent[]{notifyIntent},
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(RangingService.this)
                .setSmallIcon(R.drawable.beacon_gray)
                .setContentTitle("BLE")
                .setContentText(msg)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        notificationManager.notify(NOTIFICATION_ID++, notification);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //beaconManager.stopMonitoring(region);
    }
}
