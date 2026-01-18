package dev.oauth2_playbook.example.models;

import java.util.List;


public class User {

    public String userName;
    public String givenName;
    public String familyName;
    public List<UserEmail> emails;
    public String password;
    public String id;

    public User() {

    }
    public User(String userName, String givenName, String familyName, List<UserEmail> emails, String password) {
        this.userName = userName;
        this.givenName = givenName;
        this.familyName = familyName;
        this.emails = emails;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public List<UserEmail> getEmails() {
        return emails;
    }

    public String getPassword() {
        return password;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public void setEmails(List<UserEmail> emails) {
        this.emails = emails;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
