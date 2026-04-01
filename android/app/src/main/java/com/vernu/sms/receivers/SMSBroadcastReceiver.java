package com.vernu.sms.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.vernu.sms.ApiManager;
import com.vernu.sms.AppConstants;
import com.vernu.sms.TextBeeUtils;
import com.vernu.sms.dtos.InboundSmsRequestDTO;
import com.vernu.sms.dtos.SMSForwardResponseDTO;
import com.vernu.sms.helpers.SharedPreferenceHelper;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Response;

public class SMSBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "SMSBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent.getAction());

        if (!Objects.equals(intent.getAction(), Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            Log.d(TAG, "Ignoring unsupported intent");
            return;
        }

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (messages == null || messages.length == 0) {
            Log.d(TAG, "No messages found");
            return;
        }

        String deviceId = SharedPreferenceHelper.getSharedPreferenceString(context, AppConstants.SHARED_PREFS_DEVICE_ID_KEY, "");
        String authorization = TextBeeUtils.getDeviceAuthHeader(context);
        boolean gatewayEnabled = SharedPreferenceHelper.getSharedPreferenceBoolean(context, AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY, false);
        boolean receiveSMSEnabled = SharedPreferenceHelper.getSharedPreferenceBoolean(context, AppConstants.SHARED_PREFS_RECEIVE_SMS_ENABLED_KEY, false);

        if (deviceId.isEmpty() || authorization.isEmpty() || !gatewayEnabled || !receiveSMSEnabled) {
            Log.d(TAG, "Device is not enrolled, gateway is disabled, or inbound SMS is disabled");
            return;
        }

        InboundSmsRequestDTO receivedSMSDTO = new InboundSmsRequestDTO();
        receivedSMSDTO.setDeviceId(deviceId);
        receivedSMSDTO.setToNumber(SharedPreferenceHelper.getSharedPreferenceString(context, AppConstants.SHARED_PREFS_PHONE_NUMBER_KEY, ""));

        StringBuilder messageBody = new StringBuilder();
        long receivedAtInMillis = 0L;
        for (SmsMessage message : messages) {
            messageBody.append(message.getMessageBody());
            receivedSMSDTO.setFromNumber(message.getOriginatingAddress());
            receivedAtInMillis = Math.max(receivedAtInMillis, message.getTimestampMillis());
        }

        receivedSMSDTO.setBody(messageBody.toString());
        receivedSMSDTO.setReceivedAtInMillis(receivedAtInMillis);

        Call<SMSForwardResponseDTO> apiCall = ApiManager.getApiService().sendReceivedSMS(authorization, receivedSMSDTO);
        apiCall.enqueue(new retrofit2.Callback<SMSForwardResponseDTO>() {
            @Override
            public void onResponse(Call<SMSForwardResponseDTO> call, Response<SMSForwardResponseDTO> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Inbound SMS forwarded to Fellowship 360");
                } else {
                    Log.e(TAG, "Failed to forward inbound SMS: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<SMSForwardResponseDTO> call, Throwable t) {
                Log.e(TAG, "Failed to forward inbound SMS", t);
            }
        });
    }
}
