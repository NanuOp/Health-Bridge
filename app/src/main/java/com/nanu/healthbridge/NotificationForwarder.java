package com.nanu.healthbridge;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.topstep.fitcloud.sdk.v2.FcConnector;
import com.topstep.fitcloud.sdk.v2.features.FcMessageFeature;
import com.topstep.wearkit.base.connector.ConnectorState;

import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Listens for phone notifications and forwards them to the watch.
 * Uses FcMessageFeature.sendNotification(type, title, content).
 */
public class NotificationForwarder extends NotificationListenerService {
    private static final String TAG = "NotifForwarder";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;

        // Skip our own notifications
        if ("com.nanu.healthbridge".equals(sbn.getPackageName())) return;

        FcConnector connector = HealthBridgeApp.getFcSDK().getConnector();
        if (connector.getConnectorState() != ConnectorState.CONNECTED) return;

        String title = "";
        String content = "";

        if (sbn.getNotification().extras != null) {
            CharSequence t = sbn.getNotification().extras.getCharSequence("android.title");
            CharSequence c = sbn.getNotification().extras.getCharSequence("android.text");
            if (t != null) title = t.toString();
            if (c != null) content = c.toString();
        }

        if (title.isEmpty() && content.isEmpty()) return;

        // Map package to notification type
        int notifType = getNotificationType(sbn.getPackageName());
        final String fTitle = title;

        FcMessageFeature messageFeature = connector.messageFeature();
        messageFeature.sendNotification(notifType, title, content)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> {
                    Log.d(TAG, "Forwarded: " + fTitle);
                }, error -> {
                    Log.e(TAG, "Forward failed: " + error.getMessage());
                });
    }

    /**
     * Map app package to FitCloud notification type constants.
     * Common types: 0=Other, 1=QQ, 2=WeChat, 3=SMS, 4=Email, 5=Facebook,
     * 6=Twitter, 7=WhatsApp, 8=Messenger, 9=Instagram, 10=Skype,
     * 11=LinkedIn, 12=Line, 13=Telegram, 14=Viber
     */
    private int getNotificationType(String packageName) {
        if (packageName == null) return 0;
        switch (packageName) {
            case "com.whatsapp":
            case "com.whatsapp.w4b":
                return 7;
            case "com.facebook.katana":
                return 5;
            case "com.facebook.orca":
                return 8;
            case "com.twitter.android":
            case "com.twitter.android.lite":
                return 6;
            case "com.instagram.android":
                return 9;
            case "org.telegram.messenger":
                return 13;
            case "com.google.android.gm":
                return 4;
            case "com.google.android.apps.messaging":
            case "com.samsung.android.messaging":
                return 3;
            case "com.skype.raider":
                return 10;
            case "com.linkedin.android":
                return 11;
            case "jp.naver.line.android":
                return 12;
            case "com.viber.voip":
                return 14;
            default:
                return 0; // Other/generic
        }
    }
}
