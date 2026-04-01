package com.vernu.sms.dtos;

public class InboundSmsRequestDTO {
    private String deviceId;
    private String fromNumber;
    private String toNumber;
    private String body;
    private long receivedAtInMillis;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getFromNumber() {
        return fromNumber;
    }

    public void setFromNumber(String fromNumber) {
        this.fromNumber = fromNumber;
    }

    public String getToNumber() {
        return toNumber;
    }

    public void setToNumber(String toNumber) {
        this.toNumber = toNumber;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public long getReceivedAtInMillis() {
        return receivedAtInMillis;
    }

    public void setReceivedAtInMillis(long receivedAtInMillis) {
        this.receivedAtInMillis = receivedAtInMillis;
    }
}
