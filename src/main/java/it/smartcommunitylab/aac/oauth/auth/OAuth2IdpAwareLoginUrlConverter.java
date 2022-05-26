package it.smartcommunitylab.aac.oauth.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.AuthorityManager;
import it.smartcommunitylab.aac.core.auth.LoginUrlRequestConverter;
import it.smartcommunitylab.aac.core.model.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.service.IdentityProviderService;

public class OAuth2IdpAwareLoginUrlConverter implements LoginUrlRequestConverter {

    public static final String IDP_PARAMETER_NAME = "idp_hint";

    private final IdentityProviderService providerService;
    private final AuthorityManager authorityManager;

    public OAuth2IdpAwareLoginUrlConverter(IdentityProviderService providerService,
            AuthorityManager authorityManager) {
        Assert.notNull(providerService, "provider service is required");
        Assert.notNull(authorityManager, "authority service is required");

        this.authorityManager = authorityManager;
        this.providerService = providerService;

    }

    @Override
    public String convert(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) {

        // check if idp hint via param
        String idpHint = null;
        if (request.getParameter(IDP_PARAMETER_NAME) != null) {
            idpHint = request.getParameter(IDP_PARAMETER_NAME);
        }

        // check if idp hint via attribute
        if (request.getAttribute(IDP_PARAMETER_NAME) != null) {
            idpHint = (String) request.getAttribute(IDP_PARAMETER_NAME);
        }

        // check if idp hint
        if (StringUtils.hasText(idpHint)) {
            try {
                ConfigurableIdentityProvider idp = providerService.getProvider(idpHint);
                // TODO check if active

                // fetch providers for given realm
                IdentityProvider<? extends UserIdentity> provider = authorityManager
                        .fetchIdentityProvider(idp.getAuthority(), idp.getProvider());
                if (provider == null) {
                    throw new NoSuchProviderException();
                }

                return provider.getAuthenticationUrl();

            } catch (NoSuchProviderException e) {
                // no valid response
                return null;
            }
        }

        // not found
        return null;

    }
}