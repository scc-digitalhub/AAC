package it.smartcommunitylab.aac.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.config.ApplicationProperties;
import it.smartcommunitylab.aac.core.auth.RealmGrantedAuthority;
import it.smartcommunitylab.aac.core.auth.UserAuthentication;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.realms.RealmManager;
import it.smartcommunitylab.aac.users.MyUserManager;
import it.smartcommunitylab.aac.users.UserManager;

@RestController
@Hidden
@PreAuthorize("hasAuthority('" + Config.R_USER + "')")
@RequestMapping("/console/dev")
public class DevConsoleController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Value("${application.url}")
	private String applicationUrl;

	@Autowired
	private ApplicationProperties appProps;

	@Autowired
	private MyUserManager myUserManager;

	@Autowired
	private RealmManager realmManager;

	@Autowired
	private UserManager userManager;

	@GetMapping("/myrealms")
	public ResponseEntity<Page<Realm>> myRealms(UserAuthentication auth) {
		List<Realm> realms = new ArrayList<>();
		boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Config.R_ADMIN));

		if (isAdmin) {
			realms = (List<Realm>) realmManager.listRealms();
		} else {
			Set<String> realmsIds = auth.getAuthorities().stream().filter(RealmGrantedAuthority.class::isInstance)
					.map(a -> (RealmGrantedAuthority) a).filter(a -> a.getRole().equals(Config.R_DEVELOPER))
					.map(a -> (a.getRealm())).collect(Collectors.toSet());
			for (String id : realmsIds) {
				Realm temp = realmManager.findRealm(id);
				if (temp != null) {
					realms.add(temp);
				}
			}
		}

		PageImpl<Realm> realmPage = new PageImpl<>(realms);
		return ResponseEntity.ok(realmPage);
	}

}
