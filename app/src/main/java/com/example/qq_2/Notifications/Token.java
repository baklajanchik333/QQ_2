package com.example.qq_2.Notifications;

public class Token {
    private String token;

    //Пустой конструктор
    public Token() {
    }

    //Конструктор
    public Token(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
