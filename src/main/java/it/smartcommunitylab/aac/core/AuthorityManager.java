package it.smartcommunitylab.aac.core;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;
import it.smartcommunitylab.aac.openid.OIDCIdentityAuthority;
import it.smartcommunitylab.aac.saml.SamlIdentityAuthority;

@Service
public class AuthorityManager {

    /*
     * Identity
     */

    @Autowired
    private InternalIdentityAuthority internalAuthority;

    @Autowired
    private OIDCIdentityAuthority oidcAuthority;

    @Autowired
    private SamlIdentityAuthority samlAuthority;

    public IdentityAuthority getIdentityAuthority(String authority) {
        if (SystemKeys.AUTHORITY_INTERNAL.equals(authority)) {
            return internalAuthority;
        } else if (SystemKeys.AUTHORITY_OIDC.equals(authority)) {
            return oidcAuthority;
        } else if (SystemKeys.AUTHORITY_SAML.equals(authority)) {
            return samlAuthority;
        }
        return null;
    }

    public List<IdentityAuthority> listIdentityAuthorities() {
        List<IdentityAuthority> result = new ArrayList<>();
        result.add(internalAuthority);
        result.add(oidcAuthority);
        result.add(samlAuthority);

        return result;
    }

    /*
     * Attributes
     */

}
