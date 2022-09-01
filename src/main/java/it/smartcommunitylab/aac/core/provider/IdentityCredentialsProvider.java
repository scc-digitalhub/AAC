package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserCredentials;
import it.smartcommunitylab.aac.internal.model.CredentialsType;

public interface IdentityCredentialsProvider<U extends UserAccount, C extends UserCredentials> {

    public UserCredentialsService<C> getCredentialsService();

    public CredentialsType getCredentialsType();

//    public String getCredentialsUrl();
}
