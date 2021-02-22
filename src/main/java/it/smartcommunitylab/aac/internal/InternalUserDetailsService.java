package it.smartcommunitylab.aac.internal;

import java.util.Collections;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.core.RealmAwareUserDetailsService;
import it.smartcommunitylab.aac.core.persistence.RoleEntity;
import it.smartcommunitylab.aac.core.persistence.RoleEntityRepository;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;

public class InternalUserDetailsService implements RealmAwareUserDetailsService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private InternalUserAccountRepository userRepository;
    private RoleEntityRepository roleRepository;

    public InternalUserDetailsService(InternalUserAccountRepository userRepository,
            RoleEntityRepository roleRepository) {
        super();
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String realm, String userName) throws UsernameNotFoundException {
        logger.debug("load by username " + userName + " for realm " + String.valueOf(realm));

        InternalUserAccount account = userRepository.findByRealmAndUsername(realm, userName);
        if (account == null) {
            throw new UsernameNotFoundException("Internal user with username " + userName + " does not exist.");
        }

        // fetch system roles to translate authorities
        Set<GrantedAuthority> authorities = new HashSet<>();
        List<RoleEntity> roles = roleRepository.findBySubjectAndContextAndSpace(account.getSubject(), null, null);
        authorities
                .addAll(roles.stream().map(r -> new SimpleGrantedAuthority(r.getRole())).collect(Collectors.toList()));
        // always grant user role
        authorities.add(new SimpleGrantedAuthority(Config.R_USER));

        // we set the id as username in result
        // also map not confirmed to locked
        return new User(account.getUserId(), account.getPassword(),
                true, true, true,
                !account.isConfirmed(),
                authorities);
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        logger.debug("load by user id " + userId);

        Long id = null;
        // expected that the user name is the numerical identifier
        try {
            id = Long.parseLong(userId);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("Incorrect user id: " + userId);
        }

        InternalUserAccount account = userRepository.findOne(id);
        if (account == null) {
            throw new UsernameNotFoundException("Internal user with id " + id + " does not exist.");
        }

        // fetch system roles to translate authorities
        Set<GrantedAuthority> authorities = new HashSet<>();
        List<RoleEntity> roles = roleRepository.findBySubjectAndContextAndSpace(account.getSubject(), null, null);
        authorities
                .addAll(roles.stream().map(r -> new SimpleGrantedAuthority(r.getRole())).collect(Collectors.toList()));
        // always grant user role
        authorities.add(new SimpleGrantedAuthority(Config.R_USER));

        // we set the id as username in result
        // also map not confirmed to locked
        return new User(account.getUserId(), account.getPassword(),
                true, true, true,
                !account.isConfirmed(),
                authorities);
    }

}
