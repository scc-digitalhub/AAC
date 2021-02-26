package it.smartcommunitylab.aac.internal.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.core.persistence.RoleEntity;
import it.smartcommunitylab.aac.core.persistence.RoleEntityRepository;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;

public class InternalUserDetailsService implements UserDetailsService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String realm;

    private final InternalUserAccountRepository userRepository;
    private final RoleEntityRepository roleRepository;

    public InternalUserDetailsService(InternalUserAccountRepository userRepository,
            RoleEntityRepository roleRepository, String realm) {
        Assert.notNull(userRepository, "user repository is mandatory");
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.realm = realm;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        logger.debug("load by user id " + userId);

        // expected that the user name is already unwrapped ready for repository
        String username = userId;

        InternalUserAccount account = userRepository.findByRealmAndUsername(realm, username);
        if (account == null) {
            throw new UsernameNotFoundException(
                    "Internal user with username " + username + " does not exist for realm " + realm);
        }

        // fetch system roles to translate authorities
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (roleRepository != null) {
            List<RoleEntity> roles = roleRepository.findBySubjectAndContextAndSpace(account.getSubject(), null, null);
            authorities
                    .addAll(roles.stream().map(r -> new SimpleGrantedAuthority(r.getRole()))
                            .collect(Collectors.toList()));
        }
        // always grant user role
        authorities.add(new SimpleGrantedAuthority(Config.R_USER));

        // we set the id as username in result
        // also map not confirmed to locked
        return new User(account.getUsername(), account.getPassword(),
                true, true, true,
                account.isConfirmed(),
                authorities);
    }

}
