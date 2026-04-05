package org.example.dto;

import java.util.List;

@SuppressWarnings("unused")
public class GroqRequest {
    private String model;
    private List<GroqMessage> messages;

    public GroqRequest() {}

    public GroqRequest(String model, List<GroqMessage> messages) {
        this.model = model;
        this.messages = messages;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<GroqMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<GroqMessage> messages) {
        this.messages = messages;
    }
}