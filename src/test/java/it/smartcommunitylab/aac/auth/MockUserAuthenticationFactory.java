package it.smartcommunitylab.aac.auth;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.auth.DefaultUserAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationToken;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.model.Subject;
import it.smartcommunitylab.aac.password.PasswordIdentityAuthority;
import it.smartcommunitylab.aac.password.auth.UsernamePasswordAuthenticationToken;
import it.smartcommunitylab.aac.password.model.InternalPasswordUserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.password.provider.PasswordIdentityProvider;

/*
 * A factory for building a mock security context from a mock authentication
 */
public class MockUserAuthenticationFactory
        implements WithSecurityContextFactory<WithMockUserAuthentication> {

    private final PasswordIdentityAuthority passwordAuthority;

    public MockUserAuthenticationFactory(PasswordIdentityAuthority passwordAuthority) {
        Assert.notNull(passwordAuthority, "password authority can not be null");
        this.passwordAuthority = passwordAuthority;
    }

    private PasswordIdentityProvider getProvider(String realm) throws NoSuchProviderException {
        return passwordAuthority.getProvidersByRealm(realm).stream().findFirst()
                .orElseThrow(NoSuchProviderException::new);
    }

    @Override
    public SecurityContext createSecurityContext(WithMockUserAuthentication annotation) {
        // build a new context
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        try {
            String username = annotation.username();
            String password = annotation.password();
            String realm = annotation.realm();

            // map all authorities as-is
            Set<GrantedAuthority> authorities = Stream.of(annotation.authorities())
                    .map(a -> new SimpleGrantedAuthority(a))
                    .collect(Collectors.toSet());

            // fetch provider
            PasswordIdentityProvider idp = getProvider(realm);

            // fetch account
            InternalUserAccount account = idp.getAccountProvider().getAccount(username);
            String userId = account.getUserId();

            // build auth token
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username, password,
                    account, authorities);

            // build principal
            InternalPasswordUserAuthenticatedPrincipal principal = new InternalPasswordUserAuthenticatedPrincipal(
                    idp.getProvider(), realm, userId, username);
            principal.setName(username);
            principal.setAccountAttributes(account);

            // build an extended token
            ExtendedAuthenticationToken extToken = new ExtendedAuthenticationToken(
                    idp.getAuthority(), idp.getProvider(), realm,
                    principal, authentication);

            // resolve
            Subject subject = idp.getSubjectResolver().resolveByAccountId(username);
            UserIdentity identity = idp.convertIdentity(principal, userId);
            Collection<UserAttributes> attributeSets = idp.getAttributeProvider().convertPrincipalAttributes(principal,
                    account);

            // build user authentication
            DefaultUserAuthenticationToken auth = new DefaultUserAuthenticationToken(
                    subject, realm,
                    extToken, identity, attributeSets,
                    authorities);

            // set authentication
            context.setAuthentication(auth);

        } catch (NoSuchProviderException | NoSuchUserException e) {
            e.printStackTrace();
        }

        return context;
    }

}
