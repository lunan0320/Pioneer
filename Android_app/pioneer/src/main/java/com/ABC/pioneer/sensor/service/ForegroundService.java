//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.ABC.pioneer.sensor.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;



/// Foreground service for enabling continuous BLE operation in background
public class ForegroundService extends Service {
    public static final String ACTION_START = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP = "ACTION_STOP_FOREGROUND_SERVICE";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            String action = intent.getAction();

            switch (action) {
                case ACTION_START:
                    this.startForegroundService();
                    break;
                case ACTION_STOP:
                    this.stopForegroundService();
                    break;
            }
        }

        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startForegroundService() {
        final NotificationService notificationService = NotificationService.shared(getApplication());
        startForeground(notificationService.getForegroundServiceNotificationId(), notificationService.getForegroundServiceNotification());
    }

    private void stopForegroundService() {
        stopForeground(true);
        stopSelf();
    }
}