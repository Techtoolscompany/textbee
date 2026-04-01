package com.vernu.sms.dtos;

public class DeviceHeartbeatRequestDTO {
    private String fcmToken;
    private String deviceName;
    private String phoneNumber;
    private String manufacturer;
    private String model;
    private String buildId;
    private String appVersionName;
    private int appVersionCode;
    private Boolean gatewayEnabled;
    private Boolean receiveSmsEnabled;
    private Integer preferredSim;

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getAppVersionName() {
        return appVersionName;
    }

    public void setAppVersionName(String appVersionName) {
        this.appVersionName = appVersionName;
    }

    public int getAppVersionCode() {
        return appVersionCode;
    }

    public void setAppVersionCode(int appVersionCode) {
        this.appVersionCode = appVersionCode;
    }

    public Boolean getGatewayEnabled() {
        return gatewayEnabled;
    }

    public void setGatewayEnabled(Boolean gatewayEnabled) {
        this.gatewayEnabled = gatewayEnabled;
    }

    public Boolean getReceiveSmsEnabled() {
        return receiveSmsEnabled;
    }

    public void setReceiveSmsEnabled(Boolean receiveSmsEnabled) {
        this.receiveSmsEnabled = receiveSmsEnabled;
    }

    public Integer getPreferredSim() {
        return preferredSim;
    }

    public void setPreferredSim(Integer preferredSim) {
        this.preferredSim = preferredSim;
    }
}
