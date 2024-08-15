//  Copyright 2020 VMware, Inc.
//  SPDX-License-Identifier: Apache-2.0
//

package com.ABC.pioneer.sensor.service;

import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/// 用于启用前台服务的通知服务（必须显示通知以显示应用程序正在后台运行）。
public class NotificationService {
    private static NotificationService shared = null;
    private static Application application = null;
    private final Context context;
    private int notificationId;
    private Notification notification;

    private NotificationService(final Application application) {
        this.application = application;
        this.context = application.getApplicationContext();
    }

    /// 获取通知服务的共享全局实例
    public final static NotificationService shared(final Application application) {
        if (shared == null) {
            shared = new NotificationService(application);
        }
        return shared;
    }

    /// 启动前台服务开启后台扫描
    public void startForegroundService(Notification notification, int notificationId) {
        this.notification = notification;
        this.notificationId = notificationId;

        final Intent intent = new Intent(context, ForegroundService.class);
        intent.setAction(ForegroundService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /// 停止当前前台服务
    public void stopForegroundService() {
        final Intent intent = new Intent(context, ForegroundService.class);
        intent.setAction(ForegroundService.ACTION_STOP);
        context.startService(intent);
    }

    public Notification getForegroundServiceNotification() {
        return this.notification;
    }

    public int getForegroundServiceNotificationId() {
        return this.notificationId;
    }
}
