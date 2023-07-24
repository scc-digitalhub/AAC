package it.smartcommunitylab.aac.model;

public interface Credentials {
    public Object getCredentials();

    public boolean isActive();

    public boolean isExpired();

    public boolean isRevoked();
}
