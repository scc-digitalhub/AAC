package it.smartcommunitylab.aac.openid.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.nimbusds.jose.jwk.JWK;

import it.smartcommunitylab.aac.openid.service.JWTSigningAndValidationService;
import it.smartcommunitylab.aac.openid.view.JWKSetView;

@Controller
public class JWKSetPublishingEndpoint {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String URL = "jwk";

	@Autowired
	private JWTSigningAndValidationService jwtService;

	@RequestMapping(value = "/" + URL, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getJwk(Model m) {

		// map from key id to key
		Map<String, JWK> keys = jwtService.getAllPublicKeys();

		// TODO: check if keys are empty, return a 404 here or just an empty list?

		m.addAttribute("keys", keys);

		return JWKSetView.VIEWNAME;
	}

	/**
	 * @return the jwtService
	 */
	public JWTSigningAndValidationService getJwtService() {
		return jwtService;
	}

	/**
	 * @param jwtService the jwtService to set
	 */
	public void setJwtService(JWTSigningAndValidationService jwtService) {
		this.jwtService = jwtService;
	}

}