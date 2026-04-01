package com.vernu.sms.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.vernu.sms.GatewaySyncManager;
import com.vernu.sms.TextBeeUtils;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (TextBeeUtils.getDeviceAuthHeader(context).isEmpty()) {
                return;
            }

            BroadcastReceiver.PendingResult pendingResult = goAsync();
            GatewaySyncManager.syncDeviceState(context, new GatewaySyncManager.SyncCallback() {
                @Override
                public void onSuccess(com.vernu.sms.dtos.EnrollDeviceResponseDTO response) {
                    pendingResult.finish();
                }

                @Override
                public void onError(String message) {
                    pendingResult.finish();
                }
            });
        }
    }
}
