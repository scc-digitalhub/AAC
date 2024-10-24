package it.smartcommunitylab.aac.console;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
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
 	
	@GetMapping(
        value = {
        	"/",
            "/{path:^(?!\\S+(?:\\.[a-z0-9]{2,}))\\S+$}",
            "/-/**",
        }
    )
    public ModelAndView console(HttpServletRequest request) {
        String requestUrl = ServletUriComponentsBuilder
            .fromRequestUri(request)
            .replacePath(request.getContextPath())
            .build()
            .toUriString();

        String applicationUrl = StringUtils.hasText(appProps.getUrl())
            ? appProps.getUrl()
            : requestUrl;

        //build config
        Map<String, String> config = new HashMap<>();
        config.put("REACT_APP_APPLICATION_URL", applicationUrl);
        config.put("REACT_APP_API_URL", applicationUrl);
        config.put("REACT_APP_CONTEXT_PATH", "/console/dev/");

        config.put("VITE_APP_NAME", appProps.getName());

        // model.addAttribute("config", config);
        return new ModelAndView("console/dev", Collections.singletonMap("config",config));
    }


	@GetMapping("/myrealms")
	public Page<Realm> myRealms(
		UserAuthentication auth,
	    @RequestParam(required = false) String q,
		Pageable pageRequest
	) {
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

		if(q != null) {
			//filter
			List<Realm> list = realms.stream().filter(r -> (r.getSlug().toLowerCase().contains(q.toLowerCase()) || r.getName().toLowerCase().contains(q.toLowerCase()))).toList();
			return PageableExecutionUtils.getPage(
            	list,
            	pageRequest,
            	() -> list.size()
        	);     
		} else {
			List<Realm> list = realms;
			return PageableExecutionUtils.getPage(
            	list,
            	pageRequest,
            	() -> list.size()
        	);           
		}

	}

	@GetMapping("/myrealms/{slug}")
	public Realm getRealm(@PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug)
			throws NoSuchRealmException {
		return realmManager.getRealm(slug);
	}

	@PostMapping("/myrealms")
	public Realm addRealm(@RequestBody @Valid @NotNull Realm realm) throws RegistrationException {
		return realmManager.addRealm(realm);
	}

	@PutMapping("/myrealms/{slug}")
	public Realm updateRealm(@PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug,
			@RequestBody @Valid @NotNull Realm realm) throws NoSuchRealmException, RegistrationException {
		return realmManager.updateRealm(slug, realm);
	}

	@DeleteMapping("/myrealms/{slug}")
	public void deleteRealm(@PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug)
			throws NoSuchRealmException {
		realmManager.deleteRealm(slug, true);
	}

}
