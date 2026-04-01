package com.vernu.sms.receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.vernu.sms.AppConstants;
import com.vernu.sms.GatewaySyncManager;
import com.vernu.sms.TextBeeUtils;

public class SmsSentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String messageId = intent.getStringExtra(AppConstants.EXTRA_MESSAGE_ID);
        if (messageId == null || messageId.trim().isEmpty()) {
            return;
        }

        if (getResultCode() == Activity.RESULT_OK) {
            GatewaySyncManager.reportMessageStatus(context, messageId, "sent", null);
            return;
        }

        TextBeeUtils.clearDispatchedMessage(context, messageId);
        GatewaySyncManager.reportMessageStatus(
                context,
                messageId,
                "failed",
                resolveFailureMessage(getResultCode())
        );
    }

    private String resolveFailureMessage(int resultCode) {
        switch (resultCode) {
            case android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                return "Generic SMS failure";
            case android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE:
                return "No cellular service";
            case android.telephony.SmsManager.RESULT_ERROR_NULL_PDU:
                return "SMS payload was empty";
            case android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF:
                return "Device radio is off";
            default:
                return "SMS send failed";
        }
    }
}
