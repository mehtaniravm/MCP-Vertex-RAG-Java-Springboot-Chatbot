package com.company.chatbot.model;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class ChatResponse {
    private String answer;
    private boolean success;

    public static ChatResponse ok(String answer)    { return new ChatResponse(answer, true); }
    public static ChatResponse error(String msg)    { return new ChatResponse(msg, false); }
}
