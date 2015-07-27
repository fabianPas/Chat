package com.pas.chat.client.exceptions;

public class InvalidGroupException extends Exception {
    public InvalidGroupException() {};

    public InvalidGroupException(String message)
    {
        super(message);
    }
}
