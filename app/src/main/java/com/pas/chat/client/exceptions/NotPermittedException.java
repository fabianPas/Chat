package com.pas.chat.client.exceptions;

public class NotPermittedException extends Exception {
    public NotPermittedException() {};

    public NotPermittedException(String message)
    {
        super(message);
    }
}
