package dev.oauth2_playbook.example.models;

public class UserEmail {
    
    public String value;
    public String type;
    public boolean primary;

    public UserEmail() {

    }

    public UserEmail(String value, String type, boolean primary) {
        this.value = value;
        this.type = type;
        this.primary = primary;
    }

    public String getValue() {
        return value;
    }
    public String getType() {
        return type;
    }
    public boolean isPrimary() {
        return primary;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
