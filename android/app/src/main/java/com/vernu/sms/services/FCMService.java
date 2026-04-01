package com.vernu.sms.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vernu.sms.AppConstants;
import com.vernu.sms.GatewaySyncManager;
import com.vernu.sms.R;
import com.vernu.sms.activities.MainActivity;
import com.vernu.sms.helpers.SMSHelper;
import com.vernu.sms.helpers.SharedPreferenceHelper;
import com.vernu.sms.models.SMSPayload;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FCMService extends FirebaseMessagingService {

    private static final String TAG = "FirebaseMessagingService";
    private static final String DEFAULT_NOTIFICATION_CHANNEL_ID = "N1";
    private final Gson gson = new Gson();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        Log.d(TAG, data.toString());

        if (data.isEmpty()) {
            return;
        }

        boolean gatewayEnabled = SharedPreferenceHelper.getSharedPreferenceBoolean(this, AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY, false);
        if (!gatewayEnabled) {
            Log.d(TAG, "Gateway is disabled locally; skipping outbound SMS work");
            return;
        }

        SMSPayload payload = parsePayload(data);
        if (payload == null) {
            Log.w(TAG, "Unable to parse Fellowship 360 SMS payload");
            return;
        }

        if (!AppConstants.FCM_TYPE_SEND_SMS.equals(payload.getType())) {
            Log.d(TAG, "Ignoring unsupported FCM type: " + payload.getType());
            return;
        }

        List<SMSPayload.OutboundMessage> messages = payload.getMessages();
        if (messages == null || messages.isEmpty()) {
            Log.d(TAG, "No outbound SMS messages supplied in payload");
            return;
        }

        int preferredSim = SharedPreferenceHelper.getSharedPreferenceInt(this, AppConstants.SHARED_PREFS_PREFERRED_SIM_KEY, -1);
        for (SMSPayload.OutboundMessage message : messages) {
            if (message == null || isBlank(message.getTo()) || isBlank(message.getBody())) {
                Log.w(TAG, "Skipping malformed outbound SMS payload entry");
                continue;
            }

            boolean dispatched = SMSHelper.dispatchOutboundMessage(
                    this,
                    message.getId(),
                    message.getTo(),
                    message.getBody(),
                    preferredSim
            );
            if (!dispatched) {
                GatewaySyncManager.reportMessageStatus(
                        this,
                        message.getId(),
                        "failed",
                        "Failed to dispatch outbound SMS"
                );
            }
        }

        if (remoteMessage.getNotification() != null) {
            // Reserved for app notifications if Fellowship 360 starts sending them.
        }
    }

    @Override
    public void onNewToken(String token) {
        GatewaySyncManager.syncDeviceState(this, token, null);
    }

    private SMSPayload parsePayload(Map<String, String> data) {
        String payloadJson = data.get("payload");
        if (!isBlank(payloadJson)) {
            try {
                return gson.fromJson(payloadJson, SMSPayload.class);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse payload wrapper", e);
            }
        }

        SMSPayload payload = new SMSPayload();
        payload.setType(data.get("type"));

        String messagesJson = data.get("messages");
        if (isBlank(messagesJson)) {
            payload.setMessages(new ArrayList<>());
            return payload;
        }

        try {
            Type listType = new TypeToken<List<SMSPayload.OutboundMessage>>() { }.getType();
            payload.setMessages(gson.fromJson(messagesJson, listType));
            return payload;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse Fellowship 360 messages array", e);
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /* build and show notification */
    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = DEFAULT_NOTIFICATION_CHANNEL_ID;
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, DEFAULT_NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Fellowship 360 Gateway",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }
}
