package org.example.dto;

@SuppressWarnings("unused")
public class GroqMessage {
    private String role;
    private String content;

    public GroqMessage() {}

    public GroqMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}