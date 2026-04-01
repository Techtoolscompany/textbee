package com.vernu.sms.dtos;

import java.util.List;

public class PendingMessagesResponseDTO {
    private List<PendingMessageDTO> messages;

    public List<PendingMessageDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<PendingMessageDTO> messages) {
        this.messages = messages;
    }

    public static class PendingMessageDTO {
        private String id;
        private String to;
        private String body;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }
}
