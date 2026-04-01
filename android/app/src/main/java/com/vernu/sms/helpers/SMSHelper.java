package com.vernu.sms.helpers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

import com.vernu.sms.AppConstants;
import com.vernu.sms.TextBeeUtils;
import com.vernu.sms.receivers.SmsDeliveredReceiver;
import com.vernu.sms.receivers.SmsSentReceiver;

import java.util.ArrayList;

public class SMSHelper {
    private static final String TAG = "SMSHelper";

    private SMSHelper() {
    }

    public static boolean dispatchOutboundMessage(Context context, String messageId, String phoneNo, String message) {
        int preferredSim = SharedPreferenceHelper.getSharedPreferenceInt(
                context,
                AppConstants.SHARED_PREFS_PREFERRED_SIM_KEY,
                -1
        );
        return dispatchOutboundMessage(context, messageId, phoneNo, message, preferredSim);
    }

    public static boolean dispatchOutboundMessage(Context context, String messageId, String phoneNo, String message, int preferredSim) {
        if (messageId == null || messageId.trim().isEmpty() || phoneNo == null || phoneNo.trim().isEmpty() || message == null || message.trim().isEmpty()) {
            Log.w(TAG, "Skipping malformed outbound SMS payload");
            return false;
        }

        if (!TextBeeUtils.markMessageDispatched(context, messageId)) {
            Log.d(TAG, "Skipping duplicate outbound message " + messageId);
            return true;
        }

        try {
            SmsManager smsManager = preferredSim < 0
                    ? SmsManager.getDefault()
                    : SmsManager.getSmsManagerForSubscriptionId(preferredSim);

            ArrayList<String> parts = smsManager.divideMessage(message);
            ArrayList<PendingIntent> sentIntents = new ArrayList<>();
            ArrayList<PendingIntent> deliveredIntents = new ArrayList<>();

            for (int index = 0; index < parts.size(); index++) {
                sentIntents.add(buildSentIntent(context, messageId, index));
                deliveredIntents.add(buildDeliveredIntent(context, messageId, index));
            }

            if (parts.size() > 1) {
                smsManager.sendMultipartTextMessage(phoneNo, null, parts, sentIntents, deliveredIntents);
            } else {
                PendingIntent sentIntent = sentIntents.isEmpty() ? null : sentIntents.get(0);
                PendingIntent deliveredIntent = deliveredIntents.isEmpty() ? null : deliveredIntents.get(0);
                smsManager.sendTextMessage(phoneNo, null, message, sentIntent, deliveredIntent);
            }

            return true;
        } catch (Exception exception) {
            TextBeeUtils.clearDispatchedMessage(context, messageId);
            Log.e(TAG, "Failed to dispatch SMS " + messageId, exception);
            return false;
        }
    }

    private static PendingIntent buildSentIntent(Context context, String messageId, int partIndex) {
        Intent intent = new Intent(context, SmsSentReceiver.class);
        intent.setAction(AppConstants.ACTION_SMS_SENT);
        intent.putExtra(AppConstants.EXTRA_MESSAGE_ID, messageId);
        return PendingIntent.getBroadcast(
                context,
                (messageId + "-sent-" + partIndex).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent buildDeliveredIntent(Context context, String messageId, int partIndex) {
        Intent intent = new Intent(context, SmsDeliveredReceiver.class);
        intent.setAction(AppConstants.ACTION_SMS_DELIVERED);
        intent.putExtra(AppConstants.EXTRA_MESSAGE_ID, messageId);
        return PendingIntent.getBroadcast(
                context,
                (messageId + "-delivered-" + partIndex).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
