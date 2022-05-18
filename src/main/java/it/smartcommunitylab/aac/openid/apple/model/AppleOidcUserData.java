package it.smartcommunitylab.aac.openid.apple.model;

public class AppleOidcUserData {
    private String email;
    private AppleOidcUserDataName name;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public AppleOidcUserDataName getName() {
        return name;
    }

    public void setName(AppleOidcUserDataName name) {
        this.name = name;
    }

    public String getFirstName() {
        if (name != null) {
            return name.firstName;
        }

        return null;
    }

    public String getLastName() {
        if (name != null) {
            return name.lastName;
        }

        return null;
    }
}

class AppleOidcUserDataName {
    public String firstName;
    public String lastName;
}
