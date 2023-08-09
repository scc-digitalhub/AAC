/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.audit;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.auth.ExtendedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.ProviderWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.RealmWrappedAuthenticationToken;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.core.provider.config.ConfigurableIdentityProvider;
import it.smartcommunitylab.aac.identity.service.IdentityProviderService;
import it.smartcommunitylab.aac.internal.auth.InternalAuthenticationException;
import it.smartcommunitylab.aac.internal.auth.InternalUserAuthenticationFailureEvent;
import it.smartcommunitylab.aac.openid.auth.OIDCAuthenticationException;
import it.smartcommunitylab.aac.openid.auth.OIDCUserAuthenticationFailureEvent;
import it.smartcommunitylab.aac.saml.auth.SamlAuthenticationException;
import it.smartcommunitylab.aac.saml.auth.SamlUserAuthenticationFailureEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

public class ExtendedAuthenticationEventPublisher
    implements AuthenticationEventPublisher, ApplicationEventPublisherAware, InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ApplicationEventPublisher applicationEventPublisher;
    private final DefaultAuthenticationEventPublisher defaultPublisher;

    private IdentityProviderService providerService;

    public ExtendedAuthenticationEventPublisher() {
        this(null);
    }

    public ExtendedAuthenticationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
        // build default to handle exceptions
        defaultPublisher = new DefaultAuthenticationEventPublisher(applicationEventPublisher);
        // register authority events
        //        // TODO convert from hardcoded to provider beans
        //        Map<Class<? extends AuthenticationException>, Class<? extends AbstractAuthenticationFailureEvent>> mappings = new HashMap<>();
        //
        //        mappings.put(SpidAuthenticationException.class, SpidUserAuthenticationFailureEvent.class);
        //
        //        defaultPublisher.setAdditionalExceptionMappings(mappings);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(providerService, "provider manager can not be null");
    }

    public void setProviderService(IdentityProviderService providerService) {
        this.providerService = providerService;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
        defaultPublisher.setApplicationEventPublisher(applicationEventPublisher);
    }

    public void publishUserAuthenticationSuccess(
        String authority,
        String provider,
        String realm,
        UserAuthentication authentication
    ) {
        if (this.applicationEventPublisher != null) {
            UserAuthenticationSuccessEvent event = new UserAuthenticationSuccessEvent(
                authority,
                provider,
                realm,
                authentication
            );
            this.applicationEventPublisher.publishEvent(event);
        }
    }

    @Override
    public void publishAuthenticationSuccess(Authentication authentication) {
        if (authentication instanceof UserAuthentication) {
            UserAuthentication userAuth = (UserAuthentication) authentication;
            // we expect a single token event for audit, otherwise the first should be the
            // one matching the request
            ExtendedAuthenticationToken token = CollectionUtils.firstElement(userAuth.getAuthentications());
            if (token == null) {
                logger.error("empty token on authentication success");
                return;
            }
            UserAuthenticationSuccessEvent event = new UserAuthenticationSuccessEvent(
                token.getAuthority(),
                token.getProvider(),
                token.getRealm(),
                userAuth
            );
            applicationEventPublisher.publishEvent(event);
        } else {
            // generic event, let default handle
            defaultPublisher.publishAuthenticationSuccess(authentication);
        }
    }

    @Override
    public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {
        // build a custom model to carry realm/provider etc
        if (authentication instanceof ProviderWrappedAuthenticationToken) {
            ProviderWrappedAuthenticationToken token = (ProviderWrappedAuthenticationToken) authentication;
            String realm = null;
            // resolve providers via service
            ConfigurableIdentityProvider idp = providerService.findProvider(token.getProvider());

            if (idp != null) {
                realm = idp.getRealm();
            } else {
                // fallback to system realm
                realm = SystemKeys.REALM_SYSTEM;
            }

            UserAuthenticationFailureEvent event = translateAuthenticationException(
                token.getAuthority(),
                token.getProvider(),
                realm,
                token,
                exception
            );

            applicationEventPublisher.publishEvent(event);

            return;
        }

        if (authentication instanceof RealmWrappedAuthenticationToken) {
            RealmWrappedAuthenticationToken token = (RealmWrappedAuthenticationToken) authentication;

            UserAuthenticationFailureEvent event = translateAuthenticationException(
                token.getAuthority(),
                null,
                token.getRealm(),
                token,
                exception
            );

            applicationEventPublisher.publishEvent(event);
            return;
        }

        // generic event, let default handle
        defaultPublisher.publishAuthenticationFailure(exception, authentication);
    }

    private UserAuthenticationFailureEvent translateAuthenticationException(
        String authority,
        String provider,
        String realm,
        Authentication authentication,
        AuthenticationException exception
    ) {
        // TODO use converters..
        if (exception instanceof SamlAuthenticationException) {
            return new SamlUserAuthenticationFailureEvent(
                authority,
                provider,
                realm,
                authentication,
                (SamlAuthenticationException) exception
            );
        }
        if (exception instanceof OIDCAuthenticationException) {
            return new OIDCUserAuthenticationFailureEvent(
                authority,
                provider,
                realm,
                authentication,
                (OIDCAuthenticationException) exception
            );
        }
        if (exception instanceof InternalAuthenticationException) {
            return new InternalUserAuthenticationFailureEvent(
                authority,
                provider,
                realm,
                authentication,
                (InternalAuthenticationException) exception
            );
        }

        return new UserAuthenticationFailureEvent(authority, provider, realm, null, authentication, exception);
    }
}
