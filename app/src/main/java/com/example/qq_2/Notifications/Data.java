package com.example.qq_2.Notifications;

public class Data {
    private String user, body, title, sent, notificationsType;
    private Integer icon;

    //Пустой конструктор
    public Data() {
    }

    //Конструктор
    public Data(String user, String body, String title, String sent, String notificationsType, Integer icon) {
        this.user = user;
        this.body = body;
        this.title = title;
        this.sent = sent;
        this.notificationsType = notificationsType;
        this.icon = icon;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSent() {
        return sent;
    }

    public void setSent(String sent) {
        this.sent = sent;
    }

    public String getNotificationsType() {
        return notificationsType;
    }

    public void setNotificationsType(String notificationsType) {
        this.notificationsType = notificationsType;
    }

    public Integer getIcon() {
        return icon;
    }

    public void setIcon(Integer icon) {
        this.icon = icon;
    }
}
