package com.pas.chat.messages;

import org.jivesoftware.smack.packet.Message;

public class ChatMessage {
    private Message message;
    private boolean isSender;
    private String messageString;

    public ChatMessage(Message message, boolean isSender) {
        this.message = message;
        this.isSender = isSender;
        this.messageString = null;
    }

    public ChatMessage(String messageString, boolean isSender) {
        this.messageString = messageString;
        this.isSender = isSender;
        this.message = null;
    }

    public boolean isSender() {
        return isSender;
    }

    public String getBody() {
        if (message == null)
            return messageString;
        else {
            return message.getBody();
        }
    }
}
