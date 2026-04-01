package com.vernu.sms.activities;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.vernu.sms.ApiManager;
import com.vernu.sms.AppConstants;
import com.vernu.sms.BuildConfig;
import com.vernu.sms.GatewaySyncManager;
import com.vernu.sms.R;
import com.vernu.sms.TextBeeUtils;
import com.vernu.sms.dtos.EnrollDeviceRequestDTO;
import com.vernu.sms.dtos.EnrollDeviceResponseDTO;
import com.vernu.sms.helpers.SharedPreferenceHelper;

import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private Context mContext;
    private Switch gatewaySwitch;
    private Switch receiveSMSSwitch;
    private EditText enrollmentTokenEditText;
    private EditText fcmTokenEditText;
    private Button enrollDeviceBtn;
    private Button grantSMSPermissionBtn;
    private Button scanQRBtn;
    private Button syncNowBtn;
    private Button resetEnrollmentBtn;
    private ImageButton copyDeviceIdImgBtn;
    private TextView deviceBrandAndModelTxt;
    private TextView deviceIdTxt;
    private TextView organizationNameTxt;
    private TextView syncStatusTxt;
    private RadioGroup defaultSimSlotRadioGroup;
    private static final int SCAN_QR_REQUEST_CODE = 49374;
    private static final int PERMISSION_REQUEST_CODE = 0;
    private static final int DEVICE_DEFAULT_SIM_ID = 123456;
    private String deviceId = "";
    private boolean isRefreshingUi = false;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();
        deviceId = SharedPreferenceHelper.getSharedPreferenceString(mContext, AppConstants.SHARED_PREFS_DEVICE_ID_KEY, "");

        gatewaySwitch = findViewById(R.id.gatewaySwitch);
        receiveSMSSwitch = findViewById(R.id.receiveSMSSwitch);
        enrollmentTokenEditText = findViewById(R.id.enrollmentTokenEditText);
        fcmTokenEditText = findViewById(R.id.fcmTokenEditText);
        enrollDeviceBtn = findViewById(R.id.enrollDeviceBtn);
        grantSMSPermissionBtn = findViewById(R.id.grantSMSPermissionBtn);
        scanQRBtn = findViewById(R.id.scanQRButton);
        syncNowBtn = findViewById(R.id.syncNowBtn);
        resetEnrollmentBtn = findViewById(R.id.resetEnrollmentBtn);
        deviceBrandAndModelTxt = findViewById(R.id.deviceBrandAndModelTxt);
        deviceIdTxt = findViewById(R.id.deviceIdTxt);
        organizationNameTxt = findViewById(R.id.organizationNameTxt);
        syncStatusTxt = findViewById(R.id.syncStatusTxt);
        copyDeviceIdImgBtn = findViewById(R.id.copyDeviceIdImgBtn);
        defaultSimSlotRadioGroup = findViewById(R.id.defaultSimSlotRadioGroup);

        deviceBrandAndModelTxt.setText(TextBeeUtils.buildDeviceName());
        refreshUiFromPreferences();
        bindPermissionState();
        bindCopyDeviceId();
        bindGatewayToggle();
        bindReceiveSmsToggle();
        bindEnrollmentActions();
        bindSyncActions();
        triggerBackgroundSync(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUiFromPreferences();
        triggerBackgroundSync(false);
    }

    private void bindPermissionState() {
        String[] missingPermissions = Arrays.stream(AppConstants.requiredPermissions)
                .filter(permission -> !TextBeeUtils.isPermissionGranted(mContext, permission))
                .toArray(String[]::new);
        if (missingPermissions.length == 0) {
            grantSMSPermissionBtn.setEnabled(false);
            grantSMSPermissionBtn.setText("Permissions Granted");
            renderAvailableSimOptions();
            return;
        }

        Snackbar.make(grantSMSPermissionBtn, "Grant SMS permissions to use Fellowship 360 Gateway", Snackbar.LENGTH_SHORT).show();
        grantSMSPermissionBtn.setEnabled(true);
        grantSMSPermissionBtn.setOnClickListener(this::handleRequestPermissions);
    }

    private void bindCopyDeviceId() {
        copyDeviceIdImgBtn.setOnClickListener(view -> {
            if (deviceId == null || deviceId.trim().isEmpty()) {
                Snackbar.make(view, "Device ID not available yet", Snackbar.LENGTH_LONG).show();
                return;
            }
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Device ID", deviceId);
            clipboard.setPrimaryClip(clip);
            Snackbar.make(view, "Device ID copied", Snackbar.LENGTH_LONG).show();
        });
    }

    private void bindGatewayToggle() {
        gatewaySwitch.setChecked(SharedPreferenceHelper.getSharedPreferenceBoolean(mContext, AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY, false));
        gatewaySwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isRefreshingUi) {
                return;
            }
            SharedPreferenceHelper.setSharedPreferenceBoolean(mContext, AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY, isChecked);
            Snackbar.make(compoundButton, "Gateway " + (isChecked ? "enabled" : "disabled"), Snackbar.LENGTH_LONG).show();
            triggerBackgroundSync(false);
        });
    }

    private void bindReceiveSmsToggle() {
        receiveSMSSwitch.setChecked(SharedPreferenceHelper.getSharedPreferenceBoolean(mContext, AppConstants.SHARED_PREFS_RECEIVE_SMS_ENABLED_KEY, false));
        receiveSMSSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isRefreshingUi) {
                return;
            }
            SharedPreferenceHelper.setSharedPreferenceBoolean(mContext, AppConstants.SHARED_PREFS_RECEIVE_SMS_ENABLED_KEY, isChecked);
            Snackbar.make(compoundButton, "Receive SMS " + (isChecked ? "enabled" : "disabled"), Snackbar.LENGTH_LONG).show();
            triggerBackgroundSync(false);
        });
    }

    private void bindEnrollmentActions() {
        enrollDeviceBtn.setOnClickListener(view -> handleEnrollDevice());
        scanQRBtn.setOnClickListener(view -> {
            IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
            intentIntegrator.setPrompt("Scan the Fellowship 360 enrollment QR code from your organization settings");
            intentIntegrator.setRequestCode(SCAN_QR_REQUEST_CODE);
            intentIntegrator.initiateScan();
        });
    }

    private void bindSyncActions() {
        syncNowBtn.setOnClickListener(view -> triggerBackgroundSync(true));
        resetEnrollmentBtn.setOnClickListener(view -> {
            TextBeeUtils.clearEnrollmentState(mContext);
            SharedPreferenceHelper.setSharedPreferenceBoolean(mContext, AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY, false);
            SharedPreferenceHelper.setSharedPreferenceBoolean(mContext, AppConstants.SHARED_PREFS_RECEIVE_SMS_ENABLED_KEY, false);
            refreshUiFromPreferences();
            Snackbar.make(view, "Enrollment reset on this device", Snackbar.LENGTH_LONG).show();
        });
    }

    private void syncEnrollmentState() {
        deviceId = SharedPreferenceHelper.getSharedPreferenceString(mContext, AppConstants.SHARED_PREFS_DEVICE_ID_KEY, "");
        String organizationName = SharedPreferenceHelper.getSharedPreferenceString(mContext, AppConstants.SHARED_PREFS_ORGANIZATION_NAME_KEY, "Not enrolled");
        deviceIdTxt.setText(deviceId == null || deviceId.isEmpty() ? "Not enrolled" : deviceId);
        organizationNameTxt.setText(organizationName == null || organizationName.isEmpty() ? "Not enrolled" : organizationName);
        enrollDeviceBtn.setText(deviceId == null || deviceId.isEmpty() ? "Enroll Device" : "Re-enroll Device");
        resetEnrollmentBtn.setEnabled(deviceId != null && !deviceId.isEmpty());
    }

    private void renderAvailableSimOptions() {
        try {
            defaultSimSlotRadioGroup.removeAllViews();
            RadioButton defaultSimSlotRadioBtn = new RadioButton(this);
            defaultSimSlotRadioBtn.setText("Device Default");
            defaultSimSlotRadioBtn.setId(DEVICE_DEFAULT_SIM_ID);
            defaultSimSlotRadioGroup.addView(defaultSimSlotRadioBtn);

            TextBeeUtils.getAvailableSimSlots(mContext).forEach(subscriptionInfo -> {
                String simInfo = "SIM " + (subscriptionInfo.getSimSlotIndex() + 1) + " (" + subscriptionInfo.getDisplayName() + ")";
                RadioButton radioButton = new RadioButton(this);
                radioButton.setText(simInfo);
                radioButton.setId(subscriptionInfo.getSubscriptionId());
                defaultSimSlotRadioGroup.addView(radioButton);
            });

            int preferredSim = SharedPreferenceHelper.getSharedPreferenceInt(mContext, AppConstants.SHARED_PREFS_PREFERRED_SIM_KEY, -1);
            if (preferredSim == -1 || findViewById(preferredSim) == null) {
                defaultSimSlotRadioGroup.check(defaultSimSlotRadioBtn.getId());
            } else {
                defaultSimSlotRadioGroup.check(preferredSim);
            }

            defaultSimSlotRadioGroup.setOnCheckedChangeListener((radioGroup, checkedId) -> {
                if (isRefreshingUi) {
                    return;
                }
                RadioButton radioButton = findViewById(checkedId);
                if (radioButton == null) {
                    return;
                }
                radioButton.setChecked(true);
                if (DEVICE_DEFAULT_SIM_ID == checkedId) {
                    SharedPreferenceHelper.clearSharedPreference(mContext, AppConstants.SHARED_PREFS_PREFERRED_SIM_KEY);
                } else {
                    SharedPreferenceHelper.setSharedPreferenceInt(mContext, AppConstants.SHARED_PREFS_PREFERRED_SIM_KEY, checkedId);
                }
                triggerBackgroundSync(false);
            });
        } catch (Exception e) {
            Snackbar.make(defaultSimSlotRadioGroup.getRootView(), "Error: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
            Log.e(TAG, "SIM_SLOT_ERROR " + e.getMessage(), e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != PERMISSION_REQUEST_CODE) {
            return;
        }
        boolean allPermissionsGranted = Arrays.stream(permissions).allMatch(permission -> TextBeeUtils.isPermissionGranted(mContext, permission));
        if (allPermissionsGranted) {
            Snackbar.make(findViewById(R.id.grantSMSPermissionBtn), "All permissions granted", Snackbar.LENGTH_SHORT).show();
            grantSMSPermissionBtn.setEnabled(false);
            grantSMSPermissionBtn.setText("Permissions Granted");
            renderAvailableSimOptions();
        } else {
            Snackbar.make(findViewById(R.id.grantSMSPermissionBtn), "Grant all required permissions to continue", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void handleEnrollDevice() {
        String enrollmentToken = TextBeeUtils.normalizeEnrollmentToken(enrollmentTokenEditText.getText().toString());
        if (enrollmentToken.isEmpty()) {
            Snackbar.make(enrollDeviceBtn, "Enter or scan an enrollment token first", Snackbar.LENGTH_LONG).show();
            return;
        }

        enrollDeviceBtn.setEnabled(false);
        enrollDeviceBtn.setText("Enrolling...");
        View view = findViewById(R.id.enrollDeviceBtn);

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Snackbar.make(view, "Failed to obtain the FCM token", Snackbar.LENGTH_LONG).show();
                resetEnrollButtonState();
                return;
            }

            String token = task.getResult();
            fcmTokenEditText.setText(token);

            EnrollDeviceRequestDTO requestDTO = new EnrollDeviceRequestDTO();
            requestDTO.setEnrollmentToken(enrollmentToken);
            requestDTO.setFcmToken(token);
            requestDTO.setDeviceName(TextBeeUtils.buildDeviceName());
            String phoneNumber = TextBeeUtils.getPhoneNumber(mContext);
            requestDTO.setPhoneNumber(phoneNumber.isEmpty() ? null : phoneNumber);
            requestDTO.setManufacturer(Build.MANUFACTURER);
            requestDTO.setModel(Build.MODEL);
            requestDTO.setBuildId(Build.ID);
            requestDTO.setAppVersionCode(BuildConfig.VERSION_CODE);
            requestDTO.setAppVersionName(BuildConfig.VERSION_NAME);

            ApiManager.getApiService().enrollDevice(requestDTO).enqueue(new Callback<EnrollDeviceResponseDTO>() {
                @Override
                public void onResponse(Call<EnrollDeviceResponseDTO> call, Response<EnrollDeviceResponseDTO> response) {
                    Log.d(TAG, response.toString());
                    if (!response.isSuccessful() || response.body() == null) {
                        Snackbar.make(view, "Enrollment failed: " + response.message(), Snackbar.LENGTH_LONG).show();
                        resetEnrollButtonState();
                        return;
                    }

                    applyEnrollmentResponse(response.body(), phoneNumber);
                    triggerBackgroundSync(false);
                    Snackbar.make(view, "Device enrolled with Fellowship 360 Gateway", Snackbar.LENGTH_LONG).show();
                    resetEnrollButtonState();
                }

                @Override
                public void onFailure(Call<EnrollDeviceResponseDTO> call, Throwable t) {
                    Snackbar.make(view, "An error occurred during enrollment", Snackbar.LENGTH_LONG).show();
                    Log.e(TAG, "API_ERROR " + t.getMessage(), t);
                    resetEnrollButtonState();
                }
            });
        });
    }

    private void applyEnrollmentResponse(EnrollDeviceResponseDTO response, String phoneNumber) {
        GatewaySyncManager.applyEnrollmentState(mContext, response, phoneNumber);
        refreshUiFromPreferences();
    }

    private void resetEnrollButtonState() {
        enrollDeviceBtn.setEnabled(true);
        enrollDeviceBtn.setText(deviceId == null || deviceId.isEmpty() ? "Enroll Device" : "Re-enroll Device");
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private void handleRequestPermissions(View view) {
        boolean allPermissionsGranted = Arrays.stream(AppConstants.requiredPermissions)
                .allMatch(permission -> TextBeeUtils.isPermissionGranted(mContext, permission));
        if (allPermissionsGranted) {
            Snackbar.make(view, "Permissions already granted", Snackbar.LENGTH_SHORT).show();
            return;
        }
        String[] permissionsToRequest = Arrays.stream(AppConstants.requiredPermissions)
                .filter(permission -> !TextBeeUtils.isPermissionGranted(mContext, permission))
                .toArray(String[]::new);
        Snackbar.make(view, "Grant required SMS permissions to continue", Snackbar.LENGTH_SHORT).show();
        ActivityCompat.requestPermissions(this, permissionsToRequest, PERMISSION_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SCAN_QR_REQUEST_CODE) {
            return;
        }

        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (intentResult == null || intentResult.getContents() == null) {
            Toast.makeText(getBaseContext(), "Scan canceled", Toast.LENGTH_SHORT).show();
            return;
        }

        String enrollmentToken = TextBeeUtils.normalizeEnrollmentToken(intentResult.getContents());
        enrollmentTokenEditText.setText(enrollmentToken);
        handleEnrollDevice();
    }

    private void refreshUiFromPreferences() {
        isRefreshingUi = true;
        try {
            gatewaySwitch.setChecked(SharedPreferenceHelper.getSharedPreferenceBoolean(mContext, AppConstants.SHARED_PREFS_GATEWAY_ENABLED_KEY, false));
            receiveSMSSwitch.setChecked(SharedPreferenceHelper.getSharedPreferenceBoolean(mContext, AppConstants.SHARED_PREFS_RECEIVE_SMS_ENABLED_KEY, false));
            syncEnrollmentState();
            updateSyncStatus();
            renderAvailableSimOptions();
        } finally {
            isRefreshingUi = false;
        }
    }

    private void updateSyncStatus() {
        String syncError = SharedPreferenceHelper.getSharedPreferenceString(
                mContext,
                AppConstants.SHARED_PREFS_LAST_SYNC_ERROR_KEY,
                ""
        );
        String lastSyncAt = SharedPreferenceHelper.getSharedPreferenceString(
                mContext,
                AppConstants.SHARED_PREFS_LAST_SYNC_AT_KEY,
                ""
        );

        if (syncError != null && !syncError.trim().isEmpty()) {
            syncStatusTxt.setText("Sync issue: " + syncError);
            return;
        }

        if (lastSyncAt == null || lastSyncAt.trim().isEmpty()) {
            syncStatusTxt.setText(isDeviceEnrolled() ? "Waiting for first sync" : "Enroll this device to begin syncing");
            return;
        }

        try {
            syncStatusTxt.setText("Last synced at " + android.text.format.DateFormat.format("MMM d, h:mm a", Long.parseLong(lastSyncAt)));
        } catch (NumberFormatException ignored) {
            syncStatusTxt.setText("Last sync timestamp is invalid");
        }
    }

    private boolean isDeviceEnrolled() {
        return deviceId != null && !deviceId.trim().isEmpty();
    }

    private void triggerBackgroundSync(boolean userInitiated) {
        if (!isDeviceEnrolled()) {
            updateSyncStatus();
            return;
        }

        if (userInitiated) {
            syncNowBtn.setEnabled(false);
            syncNowBtn.setText("Syncing...");
        }

        GatewaySyncManager.syncDeviceState(mContext, new GatewaySyncManager.SyncCallback() {
            @Override
            public void onSuccess(EnrollDeviceResponseDTO response) {
                applyEnrollmentResponse(response, TextBeeUtils.getPhoneNumber(mContext));
                updateSyncStatus();
                if (userInitiated) {
                    Snackbar.make(syncNowBtn, "Device synced with Fellowship 360", Snackbar.LENGTH_LONG).show();
                    syncNowBtn.setEnabled(true);
                    syncNowBtn.setText("Sync Now");
                }
            }

            @Override
            public void onError(String message) {
                updateSyncStatus();
                if (userInitiated) {
                    Snackbar.make(syncNowBtn, message, Snackbar.LENGTH_LONG).show();
                    syncNowBtn.setEnabled(true);
                    syncNowBtn.setText("Sync Now");
                }
            }
        });
    }
}
