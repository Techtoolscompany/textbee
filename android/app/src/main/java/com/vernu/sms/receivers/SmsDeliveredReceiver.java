package com.vernu.sms.receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.vernu.sms.AppConstants;
import com.vernu.sms.GatewaySyncManager;

public class SmsDeliveredReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String messageId = intent.getStringExtra(AppConstants.EXTRA_MESSAGE_ID);
        if (messageId == null || messageId.trim().isEmpty()) {
            return;
        }

        if (getResultCode() == Activity.RESULT_OK) {
            GatewaySyncManager.reportMessageStatus(context, messageId, "delivered", null);
        }
    }
}
