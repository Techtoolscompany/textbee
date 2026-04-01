package com.vernu.sms.dtos;

public class EnrollDeviceResponseDTO {
    private String deviceId;
    private String deviceAuthToken;
    private String organizationId;
    private String organizationName;
    private Boolean gatewayEnabled;
    private Boolean receiveSmsEnabled;
    private Integer preferredSim;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceAuthToken() {
        return deviceAuthToken;
    }

    public void setDeviceAuthToken(String deviceAuthToken) {
        this.deviceAuthToken = deviceAuthToken;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
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
