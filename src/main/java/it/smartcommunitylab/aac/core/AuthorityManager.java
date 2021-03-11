package it.smartcommunitylab.aac.core;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.authorities.IdentityAuthority;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;
import it.smartcommunitylab.aac.openid.OIDCAuthority;

@Service
public class AuthorityManager {

    /*
     * Identity
     */

    @Autowired
    private InternalIdentityAuthority internalAuthority;

    @Autowired
    private OIDCAuthority oidcAuthority;

    public IdentityAuthority getIdentityAuthority(String authority) {
        if (SystemKeys.AUTHORITY_INTERNAL.equals(authority)) {
            return internalAuthority;
        } else if (SystemKeys.AUTHORITY_OIDC.equals(authority)) {
            return oidcAuthority;
        }

        return null;
    }

    public List<IdentityAuthority> listIdentityAuthorities() {
        List<IdentityAuthority> result = new ArrayList<>();
        result.add(internalAuthority);
        result.add(oidcAuthority);

        return result;
    }

    /*
     * Attributes
     */

}
