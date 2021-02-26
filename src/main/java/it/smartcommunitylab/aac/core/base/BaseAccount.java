package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.profiles.model.AccountProfile;

/*
 * Base class for user accounts
 * offers mapper to profile
 */
public abstract class BaseAccount extends AbstractAccount {

    protected BaseAccount(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    public AccountProfile toProfile() {
        AccountProfile ap = new AccountProfile();
        ap.setAuthority(getAuthority());
        ap.setProvider(getProvider());
        ap.setRealm(getRealm());
        ap.setUserId(getUserId());
        ap.setUsername(getUsername());

        return ap;
    }
}
