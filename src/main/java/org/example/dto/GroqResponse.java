package org.example.dto;

import java.util.List;

@SuppressWarnings("unused")
public class GroqResponse {
    private List<Choice> choices;

    public GroqResponse() {}

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    @SuppressWarnings("unused")
    public static class Choice {
        private GroqMessage message;

        public Choice() {}

        public GroqMessage getMessage() {
            return message;
        }

        public void setMessage(GroqMessage message) {
            this.message = message;
        }
    }
}