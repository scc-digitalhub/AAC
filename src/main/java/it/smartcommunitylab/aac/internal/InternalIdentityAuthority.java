package it.smartcommunitylab.aac.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.IdentityAuthority;
import it.smartcommunitylab.aac.core.persistence.RoleEntityRepository;
import it.smartcommunitylab.aac.core.provider.IdentityProvider;
import it.smartcommunitylab.aac.core.service.UserEntityService;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;
import it.smartcommunitylab.aac.internal.provider.InternalIdentityProvider;

@Service
public class InternalIdentityAuthority implements IdentityAuthority {

    @Autowired
    private InternalUserManager internalUserManager;

    @Autowired
    private UserEntityService userService;

    @Autowired
    private InternalUserAccountRepository userRepository;

    @Autowired
    private RoleEntityRepository roleRepository;

    // idp, keep track of global
    public static final String GLOBAL_IDP = "_global";
    private InternalIdentityProvider globalIdp;
    private Map<String, InternalIdentityProvider> providers = new HashMap<>();

    @Override
    public String getAuthorityId() {
        return SystemKeys.AUTHORITY_INTERNAL;
    }

    @PostConstruct
    public void init() throws Exception {
        // create global idp
        // these users access every realm, they will have realm=""
        // we expect no client/services in global realm!
        this.globalIdp = new InternalIdentityProvider(
                GLOBAL_IDP,
                userService, userRepository, roleRepository,
                SystemKeys.REALM_GLOBAL);

        // create internal idp for internal realm
        // kind of system realm fully functional
        InternalIdentityProvider internalIdp = new InternalIdentityProvider(
                "internal",
                userService, userRepository, roleRepository,
                SystemKeys.REALM_INTERNAL);

        registerIdp(internalIdp);
    }

    private void registerIdp(InternalIdentityProvider idp) {
        providers.put(idp.getProvider(), idp);
    }

    @Override
    public IdentityProvider getIdentityProvider(String providerId) {
        if (GLOBAL_IDP.equals(providerId)) {
            return globalIdp;
        } else {
            return providers.get(providerId);
        }
    }

    @Override
    public List<IdentityProvider> getIdentityProviders(String realm) {
        return providers.values().stream().filter(idp -> idp.getRealm().equals(realm)).collect(Collectors.toList());
    }

    @Override
    public IdentityProvider getUserIdentityProvider(String userId) {
        // unpack id
        // TODO
        return null;
    }

}
