package com.matrix.autoreply;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.SpannableString;
import android.util.Log;

import androidx.core.app.RemoteInput;

import com.matrix.autoreply.model.CustomRepliesData;
import com.matrix.autoreply.model.preferences.PreferencesManager;
import com.matrix.autoreply.model.utils.DbUtils;
import com.matrix.autoreply.model.utils.NotificationHelper;
import com.matrix.autoreply.model.utils.NotificationUtils;

import static java.lang.Math.max;

public class ForegroundNotificationService extends NotificationListenerService {
    private final String TAG = ForegroundNotificationService.class.getSimpleName();
    CustomRepliesData customRepliesData;
    private DbUtils dbUtils;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        if(canReply(sbn)) {
            sendReply(sbn);
        }
    }

    private boolean canReply(StatusBarNotification sbn){
        return isServiceEnabled() &&
                isSupportedPackage(sbn) &&
                NotificationUtils.isNewNotification(sbn) &&
                isGroupMessageAndReplyAllowed(sbn) &&
                canSendReplyNow(sbn);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //START_STICKY  to order the system to restart your service as soon as possible when it was killed.
        return START_STICKY;
    }

    private void sendReply(StatusBarNotification sbn) {
        NotificationWear notificationWear = NotificationUtils.extractWearNotification(sbn);
        if (notificationWear.getRemoteInputs().isEmpty()) { return;}


        customRepliesData = CustomRepliesData.getInstance(this);

        RemoteInput[] remoteInputs = new RemoteInput[notificationWear.getRemoteInputs().size()];

        Intent localIntent = new Intent();
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle localBundle = new Bundle();//notificationWear.bundle;
        int i = 0;
        for(RemoteInput remoteIn : notificationWear.getRemoteInputs()){
            remoteInputs[i] = remoteIn;
            localBundle.putCharSequence(remoteInputs[i].getResultKey(), customRepliesData.getTextToSendOrElse(null));
            i++;
        }

        RemoteInput.addResultsToIntent(remoteInputs, localIntent, localBundle);
        try {
            if (notificationWear.getPendingIntent() != null) {
                if(dbUtils == null) {
                    dbUtils = new DbUtils(getApplicationContext());
                }
                dbUtils.logReply(sbn, NotificationUtils.getTitle(sbn));
                notificationWear.getPendingIntent().send(this, 0, localIntent);
                if(PreferencesManager.getPreferencesInstance(this).isShowNotificationEnabled()) {
                    NotificationHelper.getInstance(getApplicationContext()).sendNotification(sbn.getNotification().extras.getString("android.title"), sbn.getNotification().extras.getString("android.text"), sbn.getPackageName());
                }
                cancelNotification(sbn.getKey());
                if(canPurgeMessages()){
                    dbUtils.purgeMessageLogs();
                    PreferencesManager.getPreferencesInstance(this).setPurgeMessageTime(System.currentTimeMillis());
                }
            }
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "replyToLastNotification error: " + e.getLocalizedMessage());
        }
    }

    private boolean canPurgeMessages() {
        long daysBeforePurgeInMS = 30 * 24 * 60 * 60 * 1000L;
        return (System.currentTimeMillis() - PreferencesManager.getPreferencesInstance(this).getLastPurgedTime()) > daysBeforePurgeInMS;
    }

    private boolean isSupportedPackage(StatusBarNotification sbn) {
        return PreferencesManager.getPreferencesInstance(this)
                .getEnabledApps()
                .contains(sbn.getPackageName());
    }

    private boolean canSendReplyNow(StatusBarNotification sbn){
        int DELAY_BETWEEN_REPLY_IN_MILLISEC = 10 * 1000;

        String title = NotificationUtils.getTitle(sbn);
        String selfDisplayName = sbn.getNotification().extras.getString("android.selfDisplayName");
        if(title != null && selfDisplayName != null && title.equalsIgnoreCase(selfDisplayName)){ //to protect double reply in case where if notification is not dismissed and existing notification is updated with our reply
            return false;
        }
        if(dbUtils == null) {
            dbUtils = new DbUtils(getApplicationContext());
        }
        long timeDelay = PreferencesManager.getPreferencesInstance(this).getAutoReplyDelay();
        return (System.currentTimeMillis() - dbUtils.getLastRepliedTime(sbn.getPackageName(), title) >= max(timeDelay, DELAY_BETWEEN_REPLY_IN_MILLISEC));
    }

    private boolean isGroupMessageAndReplyAllowed(StatusBarNotification sbn){
        String rawTitle = NotificationUtils.getTitleRaw(sbn);
        SpannableString rawText = SpannableString.valueOf("" + sbn.getNotification().extras.get("android.text"));
        boolean isPossiblyAnImageGrpMsg = ((rawTitle != null) && ": ".contains(rawTitle))
                && ((rawText != null) && rawText.toString().startsWith("\uD83D\uDCF7"));
        if(!sbn.getNotification().extras.getBoolean("android.isGroupConversation")){
            return !isPossiblyAnImageGrpMsg;
        }else {
            return PreferencesManager.getPreferencesInstance(this).isGroupReplyEnabled();
        }
    }

    private boolean isServiceEnabled(){
        return PreferencesManager.getPreferencesInstance(this).isServiceEnabled();
    }
}
