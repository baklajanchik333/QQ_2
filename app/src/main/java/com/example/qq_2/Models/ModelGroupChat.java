package com.example.qq_2.Models;

public class ModelGroupChat {
    private String message, sender, timestamp,  type;

    //Пустой конструктор
    public ModelGroupChat() {
    }

    //Конструктор
    public ModelGroupChat(String message, String sender, String timestamp, String type) {
        this.message = message;
        this.sender = sender;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
