package it.smartcommunitylab.aac.openid.controller;

import java.text.ParseException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.util.OAuth2Utils;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.manager.ClientDetailsManager;
import it.smartcommunitylab.aac.model.ClientAppBasic;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.openid.service.SelfAssertionValidator;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

@Controller
public class EndSessionEndpoint {

public static final String URL = "endsession";
    private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private static final String CLIENT_KEY = "client";
	private static final String STATE_KEY = "state";
	private static final String REDIRECT_URI_KEY = "redirectUri";

	
	@Autowired
	private SelfAssertionValidator validator;
	
	@Autowired
	private ClientDetailsRepository clientRepo;
	@Autowired
	private ClientDetailsManager clientService;
	@Value("${openid.issuer}")
	private String issuer;

	@RequestMapping(value = "/" + URL, method = RequestMethod.GET)
	public String endSession(@RequestParam (value = "id_token_hint", required = false) String idTokenHint,  
		    @RequestParam (value = "post_logout_redirect_uri", required = false) String postLogoutRedirectUri,
		    @RequestParam (value = STATE_KEY, required = false) String state,
		    HttpServletRequest request,
		    HttpServletResponse response,
		    HttpSession session,
		    Authentication auth, Model m) {

		// conditionally filled variables
		JWTClaimsSet idTokenClaims = null; // pulled from the parsed and validated ID token
		ClientDetailsEntity client = null; // pulled from ID token's audience field
		
		if (!Strings.isNullOrEmpty(postLogoutRedirectUri)) {
			session.setAttribute(REDIRECT_URI_KEY, postLogoutRedirectUri);
		}
		if (!Strings.isNullOrEmpty(state)) {
			session.setAttribute(STATE_KEY, state);
		}
		
		// parse the ID token hint to see if it's valid
		if (!Strings.isNullOrEmpty(idTokenHint)) {
			try {
				JWT idToken = JWTParser.parse(idTokenHint);
				
				if (validator.isValid(idToken)) {
					// we issued this ID token, figure out who it's for
					idTokenClaims = idToken.getJWTClaimsSet();
					
					String clientId = Iterables.getOnlyElement(idTokenClaims.getAudience());
					
					client = clientRepo.findByClientId(clientId);
					
					// save a reference in the session for us to pick up later
					//session.setAttribute("endSession_idTokenHint_claims", idTokenClaims);
					session.setAttribute(CLIENT_KEY, clientService.convertToClientApp(client));
				}
			} catch (ParseException e) {
				// it's not a valid ID token, ignore it
				logger.debug("Invalid id token hint", e);
			} catch (InvalidClientException e) {
				// couldn't find the client, ignore it
				logger.debug("Invalid client", e);
			}
		} else {
			String clientId = (String)session.getAttribute(OAuth2Utils.CLIENT_ID);
			if (clientId != null) {
				client = clientRepo.findByClientId(clientId);
				
				// save a reference in the session for us to pick up later
				//session.setAttribute("endSession_idTokenHint_claims", idTokenClaims);
				session.setAttribute(CLIENT_KEY, clientService.convertToClientApp(client));
			}
		}
		
		// are we logged in or not?
		if (auth == null || !request.isUserInRole("ROLE_USER")) {
			// we're not logged in anyway, process the final redirect bits if needed
			return processLogout(null, request, response, session, auth, m);
		} else {
			// we are logged in, need to prompt the user before we log out
			m.addAttribute("client", session.getAttribute(CLIENT_KEY));
			m.addAttribute("idToken", idTokenClaims);
			m.addAttribute("issuer", issuer);
			// display the log out confirmation page
			return "logoutConfirmation";
		}
	}
	
	@RequestMapping(value = "/" + URL, method = RequestMethod.POST)
	public String processLogout(@RequestParam(value = "approve", required = false) String approved,
			HttpServletRequest request,
			HttpServletResponse response,
		    HttpSession session,
		    Authentication auth, Model m) {

		String redirectUri = (String) session.getAttribute(REDIRECT_URI_KEY);
		String state = (String) session.getAttribute(STATE_KEY);
		ClientAppBasic client = (ClientAppBasic) session.getAttribute(CLIENT_KEY);
		
		if (!Strings.isNullOrEmpty(approved)) {
			// use approved, perform the logout
			if (auth != null){    
				new SecurityContextLogoutHandler().logout(request, response, auth);
			}
			SecurityContextHolder.getContext().setAuthentication(null);
			// TODO: hook into other logout post-processing
		}
		
		// if the user didn't approve, don't log out but hit the landing page anyway for redirect as needed

		
		
		// if we have a client AND the client has post-logout redirect URIs
		// registered AND the URI given is in that list, then...
		if (!Strings.isNullOrEmpty(redirectUri) && 
			client != null && client.getRedirectUris() != null) {
			Set<String> redirects = Utils.delimitedStringToSet(client.getRedirectUris(), ",");
			if (redirects.contains(redirectUri)) {
				// TODO: future, add the redirect URI to the model for the display page for an interstitial
				// m.addAttribute("redirectUri", postLogoutRedirectUri);
				
				UriComponents uri = UriComponentsBuilder.fromHttpUrl(redirectUri).queryParam("state", state).build();
				
				return "redirect:" + uri;
			}
		}
		
		// otherwise, return to a nice post-logout landing page
		return "redirect:/";
	}

}