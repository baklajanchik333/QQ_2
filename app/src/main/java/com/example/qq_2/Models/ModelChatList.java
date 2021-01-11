package com.example.qq_2.Models;

public class ModelChatList {
    private String id; //Этот идентификатор понадобится, чтобы получить список чата, идентификатор отправителя/получателя

    //Пустой конструктор
    public ModelChatList() {
    }

    //Конструктор
    public ModelChatList(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
