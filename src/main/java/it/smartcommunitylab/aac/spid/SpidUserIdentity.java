package it.smartcommunitylab.aac.spid;

import java.io.Serializable;
import java.util.Collections;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.base.DefaultIdentityImpl;
import it.smartcommunitylab.aac.spid.auth.SpidAuthenticatedPrincipal;

public class SpidUserIdentity extends DefaultIdentityImpl implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_SPID_SERIAL_VERSION;

    private String username;
    private SpidAuthenticatedPrincipal principal;

    public SpidUserIdentity(String provider, String realm) {
        super(SystemKeys.AUTHORITY_SPID, provider, realm);
        attributes = Collections.emptySet();
    }

    public SpidUserIdentity(String provider, String realm, SpidAuthenticatedPrincipal principal) {
        super(SystemKeys.AUTHORITY_SAML, provider, realm);
        attributes = Collections.emptySet();
        this.principal = principal;
        this.username = principal.getName();
    }

    @Override
    public String getAuthority() {
        return SystemKeys.AUTHORITY_SAML;
    }

    @Override
    public void eraseCredentials() {
        // we do not have credentials or sensible data

    }

    @Override
    public SpidAuthenticatedPrincipal getPrincipal() {
        return this.principal;
    }

    /*
     * props: only getters we want this to be immutable
     */
    public String getUsername() {
        return username;
    }

}
