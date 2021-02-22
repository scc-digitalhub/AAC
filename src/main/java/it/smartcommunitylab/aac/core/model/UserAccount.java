package it.smartcommunitylab.aac.core.model;

public interface UserAccount {

    public String getAuthority();

    public String getRealm();

    public String getUserId();

    public String getProvider();

    public String getUsername();

//    public AccountProfile toProfile();
    
}
