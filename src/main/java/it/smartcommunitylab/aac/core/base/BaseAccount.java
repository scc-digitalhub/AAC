package it.smartcommunitylab.aac.core.base;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;

/*
 * Base class for user accounts
 * offers mapper to profile
 */
public abstract class BaseAccount extends AbstractAccount {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    protected String userId;

    protected BaseAccount(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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
