package it.smartcommunitylab.aac.core;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface RealmAwareUserDetailsService extends UserDetailsService {

    public UserDetails loadUserByUsername(String realm, String userName) throws UsernameNotFoundException;
}
