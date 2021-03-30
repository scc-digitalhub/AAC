package it.smartcommunitylab.aac.core.model;

public interface UserCredentials extends Credentials {
    public String getUserId();

    public boolean canSet();

    public boolean canReset();

}
