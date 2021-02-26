package it.smartcommunitylab.aac.core.base;

/*
 * An instantiable user account
 */
public class DefaultAccountImpl extends BaseAccount {

    private String internalUserId;
    private String username;

    public DefaultAccountImpl(String authority, String provider, String realm) {
        super(authority, provider, realm);

    }

    public String getInternalUserId() {
        return internalUserId;
    }

    public void setInternalUserId(String internalUserId) {
        this.internalUserId = internalUserId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getUserId() {
        // leverage the default mapper to translate internalId
        return exportInternalId(internalUserId);
    }

}
