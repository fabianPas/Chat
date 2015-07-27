package com.pas.chat;

import android.app.Application;

import com.pas.chat.client.Client;

import org.jivesoftware.smack.chat.Chat;

public class ChatApplication extends Application {
    private Client _client = null;
    private Chat _chat = null;

    public void setChat(Chat chat)
    {
        this._chat = chat;
    }

    public Chat getChat()
    {
        return this._chat;
    }

    public Client getClient()
    {
        return this._client;
    }

    public void setClient(Client client)
    {
        this._client = client;
    }
}
