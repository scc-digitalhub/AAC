package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.core.authorities.AuthorityService;

@Service
public class ProviderAuthorityService {
    private final Map<String, AuthorityService<?>> services;

    public ProviderAuthorityService(Collection<AuthorityService<?>> services) {
        this.services = services.stream().collect(Collectors.toMap(s -> s.getType(), s -> s));
    }

    public AuthorityService<?> findAuthorityService(String type) {
        return services.get(type);
    }

    public AuthorityService<?> getAuthorityService(String type) throws NoSuchProviderException {
        AuthorityService<?> as = findAuthorityService(type);
        if (as == null) {
            throw new NoSuchProviderException();
        }

        return as;
    }

//    public ResourceProvider<?> registerResourceProvider(ConfigurableProvider cp)
//            throws NoSuchAuthorityException, NoSuchProviderException {
//        if (cp == null) {
//            throw new IllegalArgumentException("invalid provider");
//        }
//
//        if (!cp.isEnabled()) {
//            throw new IllegalArgumentException("provider is not enabled");
//        }
//
//        // authority type is the cp type
//        ProviderAuthority<? extends ResourceProvider<?>, ?, ? extends ConfigurableProvider, ?, ?> pa = getAuthorityService(
//                cp.getType()).getAuthority(cp.getAuthority());
//
//        // register directly with authority
//        ResourceProvider<?> p = pa.registerProvider(cp);
//        return p;
//    }
//
//    public void unregisterResourceProvider(ConfigurableProvider cp)
//            throws NoSuchAuthorityException, NoSuchProviderException {
//        if (cp == null) {
//            throw new IllegalArgumentException("invalid provider");
//        }
//
//        // authority type is the cp type
//        ProviderAuthority<? extends ResourceProvider<?>, ?, ? extends ConfigurableProvider, ?, ?> pa = getAuthorityService(
//                cp.getType()).getAuthority(cp.getAuthority());
//
//        pa.unregisterProvider(cp.getProvider());
//    }

}
