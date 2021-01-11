package com.example.qq_2.Models;

public class ModelPost {
    private String pId, pTitle, pDesc, pImage, pTime, uid, uName, uEmail, uDp, pLikes, pComments;

    //Пустой конструктор
    public ModelPost() {
    }

    //Конструктор
    public ModelPost(String pId, String pTitle, String pDesc, String pImage, String pTime, String uid, String uName, String uEmail, String uDp, String pLikes, String pComments) {
        this.pId = pId;
        this.pTitle = pTitle;
        this.pDesc = pDesc;
        this.pImage = pImage;
        this.pTime = pTime;
        this.uid = uid;
        this.uName = uName;
        this.uEmail = uEmail;
        this.uDp = uDp;
        this.pLikes = pLikes;
        this.pComments = pComments;
    }

    public String getpId() {
        return pId;
    }

    public void setpId(String pId) {
        this.pId = pId;
    }

    public String getpTitle() {
        return pTitle;
    }

    public void setpTitle(String pTitle) {
        this.pTitle = pTitle;
    }

    public String getpDesc() {
        return pDesc;
    }

    public void setpDesc(String pDesc) {
        this.pDesc = pDesc;
    }

    public String getpImage() {
        return pImage;
    }

    public void setpImage(String pImage) {
        this.pImage = pImage;
    }

    public String getpTime() {
        return pTime;
    }

    public void setpTime(String pTime) {
        this.pTime = pTime;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getuName() {
        return uName;
    }

    public void setuName(String uName) {
        this.uName = uName;
    }

    public String getuEmail() {
        return uEmail;
    }

    public void setuEmail(String uEmail) {
        this.uEmail = uEmail;
    }

    public String getuDp() {
        return uDp;
    }

    public void setuDp(String uDp) {
        this.uDp = uDp;
    }

    public String getpLikes() {
        return pLikes;
    }

    public void setpLikes(String pLikes) {
        this.pLikes = pLikes;
    }

    public String getpComments() {
        return pComments;
    }

    public void setpComments(String pComments) {
        this.pComments = pComments;
    }
}
