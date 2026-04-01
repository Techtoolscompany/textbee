package com.vernu.sms.dtos;

public class DeviceStatusUpdateRequestDTO {
    private String messageId;
    private String status;
    private String errorMessage;

    public DeviceStatusUpdateRequestDTO() {
    }

    public DeviceStatusUpdateRequestDTO(String messageId, String status, String errorMessage) {
        this.messageId = messageId;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
