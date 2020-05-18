package it.smartcommunitylab.aac.saml;

import java.util.ArrayList;
import java.util.List;

import org.opensaml.saml2.core.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.security.core.userdetails.UserDetails;

public class AACSAMLUserDetailsService implements SAMLUserDetailsService {

    private String userid_attribute;

    public AACSAMLUserDetailsService() {
        this.userid_attribute = null;
    }

    public AACSAMLUserDetailsService(String attributeName) {
        Assert.hasText(attributeName, "A non-empty attribute name  for userId is required");
        this.userid_attribute = attributeName;
    }

    // Logger
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public UserDetails loadUserBySAML(SAMLCredential credential)
            throws UsernameNotFoundException {

        // The method is supposed to identify local account of user referenced by
        // data in the SAML assertion and return UserDetails object describing the user.

        logger.debug(credential.toString());

        String userId = credential.getNameID().getValue();

        logger.info(userId + " from saml credentials");

        logger.debug(userId + "  attributes " + credential.getAttributes().toString());
        logger.debug(userId + "  assertion " + credential.getAuthenticationAssertion().toString());

        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_USER");
        authorities.add(authority);

        // extract userid from attributes if defined
        if (userid_attribute != null) {
            String userAttr = credential.getAttributeAsString(userid_attribute);

            if (StringUtils.hasText(userAttr)) {
                userId = userAttr;
            }
        }

        logger.info("user " + userId + " from saml credentials");

        return new User(userId, "", authorities);
    }

}
