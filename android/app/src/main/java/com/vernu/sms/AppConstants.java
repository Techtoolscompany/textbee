package com.vernu.sms;

import android.Manifest;

public class AppConstants {
    public static final String API_BASE_URL = BuildConfig.GATEWAY_API_BASE_URL;
    public static final String[] requiredPermissions = new String[]{
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE
    };
    public static final String SHARED_PREFS_DEVICE_ID_KEY = "DEVICE_ID";
    public static final String SHARED_PREFS_DEVICE_AUTH_TOKEN_KEY = "DEVICE_AUTH_TOKEN";
    public static final String SHARED_PREFS_GATEWAY_ENABLED_KEY = "GATEWAY_ENABLED";
    public static final String SHARED_PREFS_PREFERRED_SIM_KEY = "PREFERRED_SIM";
    public static final String SHARED_PREFS_RECEIVE_SMS_ENABLED_KEY = "RECEIVE_SMS_ENABLED";
    public static final String SHARED_PREFS_TRACK_SENT_SMS_STATUS_KEY = "TRACK_SENT_SMS_STATUS";
    public static final String SHARED_PREFS_ORGANIZATION_ID_KEY = "ORGANIZATION_ID";
    public static final String SHARED_PREFS_ORGANIZATION_NAME_KEY = "ORGANIZATION_NAME";
    public static final String SHARED_PREFS_PHONE_NUMBER_KEY = "PHONE_NUMBER";
    public static final String SHARED_PREFS_LAST_SYNC_AT_KEY = "LAST_SYNC_AT";
    public static final String SHARED_PREFS_LAST_SYNC_ERROR_KEY = "LAST_SYNC_ERROR";
    public static final String SHARED_PREFS_DISPATCHED_MESSAGE_IDS_KEY = "DISPATCHED_MESSAGE_IDS";
    public static final String FCM_TYPE_SEND_SMS = "SEND_SMS";
    public static final String ACTION_SMS_SENT = "com.vernu.sms.ACTION_SMS_SENT";
    public static final String ACTION_SMS_DELIVERED = "com.vernu.sms.ACTION_SMS_DELIVERED";
    public static final String EXTRA_MESSAGE_ID = "messageId";

    private AppConstants() {
    }
}
