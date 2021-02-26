package it.smartcommunitylab.aac.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.internal.InternalIdentityAuthority;

@Service
public class AuthorityManager {

    @Autowired
    private InternalIdentityAuthority internalAuthority;

    public IdentityAuthority getIdentityAuthority(String authority) {
        if (SystemKeys.AUTHORITY_INTERNAL.equals(authority)) {
            return internalAuthority;
        }

        return null;
    }

}
