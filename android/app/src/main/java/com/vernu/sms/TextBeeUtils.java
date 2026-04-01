package com.vernu.sms;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.vernu.sms.helpers.SharedPreferenceHelper;
import com.vernu.sms.services.StickyNotificationService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TextBeeUtils {
    public static boolean isPermissionGranted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static List<SubscriptionInfo> getAvailableSimSlots(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return new ArrayList<>();
        }

        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        List<SubscriptionInfo> subscriptions = subscriptionManager.getActiveSubscriptionInfoList();
        return subscriptions == null ? new ArrayList<>() : subscriptions;
    }

    public static void startStickyNotificationService(Context context) {
        if (!isPermissionGranted(context, Manifest.permission.RECEIVE_SMS)) {
            return;
        }

        Intent notificationIntent = new Intent(context, StickyNotificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(notificationIntent);
        } else {
            context.startService(notificationIntent);
        }
    }

    public static void stopStickyNotificationService(Context context) {
        Intent notificationIntent = new Intent(context, StickyNotificationService.class);
        context.stopService(notificationIntent);
    }

    public static String buildDeviceName() {
        String manufacturer = safeDeviceValue(Build.MANUFACTURER);
        String model = safeDeviceValue(Build.MODEL);
        if (manufacturer.isEmpty() && model.isEmpty()) {
            return "Android Device";
        }
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return model;
        }
        return (manufacturer + " " + model).trim();
    }

    public static String getPhoneNumber(Context context) {
        for (SubscriptionInfo subscriptionInfo : getAvailableSimSlots(context)) {
            String number = subscriptionInfo.getNumber();
            if (!TextUtils.isEmpty(number)) {
                return number.trim();
            }
        }

        return SharedPreferenceHelper.getSharedPreferenceString(context, AppConstants.SHARED_PREFS_PHONE_NUMBER_KEY, "");
    }

    public static String getDeviceAuthHeader(Context context) {
        String token = SharedPreferenceHelper.getSharedPreferenceString(context, AppConstants.SHARED_PREFS_DEVICE_AUTH_TOKEN_KEY, "");
        if (token == null || token.trim().isEmpty()) {
            return "";
        }
        return "Bearer " + token.trim();
    }

    public static boolean markMessageDispatched(Context context, String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            return false;
        }

        Set<String> dispatched = SharedPreferenceHelper.getSharedPreferenceStringSet(
                context,
                AppConstants.SHARED_PREFS_DISPATCHED_MESSAGE_IDS_KEY
        );
        if (dispatched.contains(messageId)) {
            return false;
        }

        dispatched.add(messageId);
        SharedPreferenceHelper.setSharedPreferenceStringSet(
                context,
                AppConstants.SHARED_PREFS_DISPATCHED_MESSAGE_IDS_KEY,
                trimDispatchHistory(dispatched)
        );
        return true;
    }

    public static void clearDispatchedMessage(Context context, String messageId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            return;
        }

        Set<String> dispatched = SharedPreferenceHelper.getSharedPreferenceStringSet(
                context,
                AppConstants.SHARED_PREFS_DISPATCHED_MESSAGE_IDS_KEY
        );
        if (!dispatched.remove(messageId)) {
            return;
        }

        SharedPreferenceHelper.setSharedPreferenceStringSet(
                context,
                AppConstants.SHARED_PREFS_DISPATCHED_MESSAGE_IDS_KEY,
                dispatched
        );
    }

    public static void clearEnrollmentState(Context context) {
        SharedPreferenceHelper.clearSharedPreference(context, AppConstants.SHARED_PREFS_DEVICE_ID_KEY);
        SharedPreferenceHelper.clearSharedPreference(context, AppConstants.SHARED_PREFS_DEVICE_AUTH_TOKEN_KEY);
        SharedPreferenceHelper.clearSharedPreference(context, AppConstants.SHARED_PREFS_ORGANIZATION_ID_KEY);
        SharedPreferenceHelper.clearSharedPreference(context, AppConstants.SHARED_PREFS_ORGANIZATION_NAME_KEY);
        SharedPreferenceHelper.clearSharedPreference(context, AppConstants.SHARED_PREFS_PHONE_NUMBER_KEY);
        SharedPreferenceHelper.clearSharedPreference(context, AppConstants.SHARED_PREFS_PREFERRED_SIM_KEY);
        SharedPreferenceHelper.clearSharedPreference(context, AppConstants.SHARED_PREFS_LAST_SYNC_AT_KEY);
        SharedPreferenceHelper.clearSharedPreference(context, AppConstants.SHARED_PREFS_LAST_SYNC_ERROR_KEY);
        SharedPreferenceHelper.clearSharedPreference(context, AppConstants.SHARED_PREFS_DISPATCHED_MESSAGE_IDS_KEY);
    }

    public static String normalizeEnrollmentToken(String rawValue) {
        if (rawValue == null) {
            return "";
        }

        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        try {
            JSONObject object = new JSONObject(trimmed);
            String jsonToken = firstNonEmpty(
                    object.optString("enrollmentToken"),
                    object.optString("token"),
                    object.optString("code")
            );
            if (!jsonToken.isEmpty()) {
                return jsonToken;
            }
        } catch (JSONException ignored) {
        }

        try {
            Uri uri = Uri.parse(trimmed);
            String queryToken = firstNonEmpty(
                    uri.getQueryParameter("enrollmentToken"),
                    uri.getQueryParameter("token"),
                    uri.getLastPathSegment()
            );
            if (!queryToken.isEmpty() && !trimmed.equals(queryToken)) {
                return queryToken;
            }
        } catch (Exception ignored) {
        }

        return trimmed;
    }

    private static String safeDeviceValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static Set<String> trimDispatchHistory(Set<String> dispatched) {
        if (dispatched.size() <= 200) {
            return dispatched;
        }

        List<String> ordered = new ArrayList<>(dispatched);
        int startIndex = Math.max(ordered.size() - 200, 0);
        return new HashSet<>(ordered.subList(startIndex, ordered.size()));
    }
}
