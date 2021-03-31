package it.smartcommunitylab.aac.core.model;

import it.smartcommunitylab.aac.model.Credentials;

public interface UserCredentials extends Credentials {
    public String getUserId();

    public boolean canSet();

    public boolean canReset();

}
