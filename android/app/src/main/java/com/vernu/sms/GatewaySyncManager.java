package com.vernu.sms;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.messaging.FirebaseMessaging;
import com.vernu.sms.dtos.DeviceHeartbeatRequestDTO;
import com.vernu.sms.dtos.DeviceStatusUpdateRequestDTO;
import com.vernu.sms.dtos.EnrollDeviceResponseDTO;
import com.vernu.sms.dtos.PendingMessagesResponseDTO;
import com.vernu.sms.dtos.SMSForwardResponseDTO;
import com.vernu.sms.helpers.SMSHelper;
import com.vernu.sms.helpers.SharedPreferenceHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GatewaySyncManager {
    private static final String TAG = "GatewaySyncManager";

    public interface SyncCallback {
        void onSuccess(EnrollDeviceResponseDTO response);
        void onError(String message);
    }

    private GatewaySyncManager() {
    }

    public static void syncDeviceState(Context context) {
        syncDeviceState(context, null, null);
    }

    public static void syncDeviceState(Context context, @Nullable SyncCallback callback) {
        syncDeviceState(context, null, callback);
    }

    public static void syncDeviceState(Context context, @Nullable String explicitFcmToken, @Nullable SyncCallback callback) {
        String authorization = TextBeeUtils.getDeviceAuthHeader(context);
        if (authorization.isEmpty()) {
            if (callback != null) {
                callback.onError("Device is not enrolled yet");
            }
            return;
        }

        if (explicitFcmToken != null && !explicitFcmToken.trim().isEmpty()) {
            performSync(context, authorization, explicitFcmToken.trim(), callback);
            return;
        }

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null || task.getResult().trim().isEmpty()) {
                String message = "Failed to obtain the FCM token";
                recordSyncError(context, message);
                if (callback != null) {
                    callback.onError(message);
                }
                return;
            }

            performSync(context, authorization, task.getResult().trim(), callback);
        });
    }

    public static void fetchPendingMessages(Context context) {
        if (!SharedPreferenceHelper.getSharedPreferenceBoolean(
                context,
                AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY,
                false
        )) {
            return;
        }

        String authorization = TextBeeUtils.getDeviceAuthHeader(context);
        if (authorization.isEmpty()) {
            return;
        }

        ApiManager.getApiService().fetchPendingMessages(authorization).enqueue(new Callback<PendingMessagesResponseDTO>() {
            @Override
            public void onResponse(Call<PendingMessagesResponseDTO> call, Response<PendingMessagesResponseDTO> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().getMessages() == null) {
                    Log.d(TAG, "No pending messages fetched; HTTP " + response.code());
                    return;
                }

                for (PendingMessagesResponseDTO.PendingMessageDTO message : response.body().getMessages()) {
                    if (message == null) {
                        continue;
                    }

                    SMSHelper.dispatchOutboundMessage(
                            context,
                            message.getId(),
                            message.getTo(),
                            message.getBody()
                    );
                }
            }

            @Override
            public void onFailure(Call<PendingMessagesResponseDTO> call, Throwable t) {
                Log.e(TAG, "Failed to fetch pending SMS messages", t);
            }
        });
    }

    public static void reportMessageStatus(Context context, String messageId, String status, @Nullable String errorMessage) {
        if (messageId == null || messageId.trim().isEmpty()) {
            return;
        }

        String authorization = TextBeeUtils.getDeviceAuthHeader(context);
        if (authorization.isEmpty()) {
            return;
        }

        DeviceStatusUpdateRequestDTO requestDTO = new DeviceStatusUpdateRequestDTO(messageId, status, errorMessage);
        ApiManager.getApiService().updateMessageStatus(authorization, requestDTO).enqueue(new Callback<SMSForwardResponseDTO>() {
            @Override
            public void onResponse(Call<SMSForwardResponseDTO> call, Response<SMSForwardResponseDTO> response) {
                Log.d(TAG, "Reported SMS status " + status + " for message " + messageId + " with HTTP " + response.code());
            }

            @Override
            public void onFailure(Call<SMSForwardResponseDTO> call, Throwable t) {
                Log.e(TAG, "Failed to report SMS status for message " + messageId, t);
            }
        });
    }

    public static void applyEnrollmentState(Context context, EnrollDeviceResponseDTO response, @Nullable String phoneNumber) {
        SharedPreferenceHelper.setSharedPreferenceString(
                context,
                AppConstants.SHARED_PREFS_DEVICE_ID_KEY,
                safeValue(response.getDeviceId())
        );
        SharedPreferenceHelper.setSharedPreferenceString(
                context,
                AppConstants.SHARED_PREFS_DEVICE_AUTH_TOKEN_KEY,
                safeValue(response.getDeviceAuthToken())
        );
        SharedPreferenceHelper.setSharedPreferenceString(
                context,
                AppConstants.SHARED_PREFS_ORGANIZATION_ID_KEY,
                safeValue(response.getOrganizationId())
        );
        SharedPreferenceHelper.setSharedPreferenceString(
                context,
                AppConstants.SHARED_PREFS_ORGANIZATION_NAME_KEY,
                safeValue(response.getOrganizationName())
        );
        SharedPreferenceHelper.setSharedPreferenceBoolean(
                context,
                AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY,
                Boolean.TRUE.equals(response.getGatewayEnabled())
        );
        SharedPreferenceHelper.setSharedPreferenceBoolean(
                context,
                AppConstants.SHARED_PREFS_RECEIVE_SMS_ENABLED_KEY,
                Boolean.TRUE.equals(response.getReceiveSmsEnabled())
        );

        if (response.getPreferredSim() == null || response.getPreferredSim() < 0) {
            SharedPreferenceHelper.clearSharedPreference(context, AppConstants.SHARED_PREFS_PREFERRED_SIM_KEY);
        } else {
            SharedPreferenceHelper.setSharedPreferenceInt(
                    context,
                    AppConstants.SHARED_PREFS_PREFERRED_SIM_KEY,
                    response.getPreferredSim()
            );
        }

        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            SharedPreferenceHelper.setSharedPreferenceString(
                    context,
                    AppConstants.SHARED_PREFS_PHONE_NUMBER_KEY,
                    phoneNumber.trim()
            );
        }
    }

    public static void recordSyncError(Context context, String message) {
        SharedPreferenceHelper.setSharedPreferenceString(
                context,
                AppConstants.SHARED_PREFS_LAST_SYNC_ERROR_KEY,
                safeValue(message)
        );
    }

    public static void recordSyncSuccess(Context context) {
        SharedPreferenceHelper.setSharedPreferenceString(
                context,
                AppConstants.SHARED_PREFS_LAST_SYNC_AT_KEY,
                String.valueOf(System.currentTimeMillis())
        );
        SharedPreferenceHelper.clearSharedPreference(context, AppConstants.SHARED_PREFS_LAST_SYNC_ERROR_KEY);
    }

    private static void performSync(Context context, String authorization, String fcmToken, @Nullable SyncCallback callback) {
        DeviceHeartbeatRequestDTO requestDTO = new DeviceHeartbeatRequestDTO();
        requestDTO.setFcmToken(fcmToken);
        requestDTO.setDeviceName(TextBeeUtils.buildDeviceName());
        String phoneNumber = TextBeeUtils.getPhoneNumber(context);
        requestDTO.setPhoneNumber(phoneNumber);
        requestDTO.setManufacturer(Build.MANUFACTURER);
        requestDTO.setModel(Build.MODEL);
        requestDTO.setBuildId(Build.ID);
        requestDTO.setAppVersionName(BuildConfig.VERSION_NAME);
        requestDTO.setAppVersionCode(BuildConfig.VERSION_CODE);
        requestDTO.setGatewayEnabled(SharedPreferenceHelper.getSharedPreferenceBoolean(context, AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY, false));
        requestDTO.setReceiveSmsEnabled(SharedPreferenceHelper.getSharedPreferenceBoolean(context, AppConstants.SHARED_PREFS_RECEIVE_SMS_ENABLED_KEY, false));
        requestDTO.setPreferredSim(resolvePreferredSim(context));

        ApiManager.getApiService().syncDevice(authorization, requestDTO).enqueue(new Callback<EnrollDeviceResponseDTO>() {
            @Override
            public void onResponse(Call<EnrollDeviceResponseDTO> call, Response<EnrollDeviceResponseDTO> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    String message = "Heartbeat failed: HTTP " + response.code();
                    recordSyncError(context, message);
                    if (callback != null) {
                        callback.onError(message);
                    }
                    return;
                }

                applyEnrollmentState(context, response.body(), phoneNumber);
                recordSyncSuccess(context);
                fetchPendingMessages(context);
                if (callback != null) {
                    callback.onSuccess(response.body());
                }
            }

            @Override
            public void onFailure(Call<EnrollDeviceResponseDTO> call, Throwable t) {
                String message = t.getMessage() == null ? "Failed to sync device" : t.getMessage();
                recordSyncError(context, message);
                Log.e(TAG, "Failed to sync device heartbeat", t);
                if (callback != null) {
                    callback.onError(message);
                }
            }
        });
    }

    private static Integer resolvePreferredSim(Context context) {
        int preferredSim = SharedPreferenceHelper.getSharedPreferenceInt(
                context,
                AppConstants.SHARED_PREFS_PREFERRED_SIM_KEY,
                -1
        );
        return preferredSim < 0 ? null : preferredSim;
    }

    private static String safeValue(String value) {
        return value == null ? "" : value;
    }
}
