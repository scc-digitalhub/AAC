package it.smartcommunitylab.aac.authorization.controller;

import static it.smartcommunitylab.aac.authorization.controller.AuthorizationConverter.convert;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.authorization.AuthorizationHelper;
import it.smartcommunitylab.aac.authorization.AuthorizationSchemaHelper;
import it.smartcommunitylab.aac.authorization.NotValidResourceException;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationDTO;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationNodeDTO;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationResourceDTO;
import it.smartcommunitylab.aac.authorization.model.AuthorizationNodeAlreadyExist;
import it.smartcommunitylab.aac.authorization.model.FQname;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

@RestController
public class AuthorizationController {

	@Autowired
	private AuthorizationHelper authorizationHelper;
	@Autowired
	private AuthorizationSchemaHelper authorizationSchemaHelper;
	
	@Autowired
	private UserRepository userRepository;	
	@Autowired
	private ResourceServerTokenServices resourceServerTokenServices;	
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;	
	
	@RequestMapping(value = "/authorization/{domain}/{id}", method = RequestMethod.DELETE)
	public void removeAuthorization(HttpServletRequest request, @PathVariable String domain, @PathVariable String id) throws UnauthorizedDomainException {
		checkDomain(request, domain);
		authorizationHelper.remove(id);
	}

	@RequestMapping(value = "/authorization/{domain}", method = RequestMethod.POST)
	public AuthorizationDTO insertAuthorization(HttpServletRequest request, @PathVariable String domain,
			@RequestBody AuthorizationDTO authorizationDTO) throws NotValidResourceException, UnauthorizedDomainException {
		checkDomain(request, domain);
		return convert(authorizationHelper.insert(convert(domain, authorizationDTO)));
	}

	@RequestMapping(value = "/authorization/{domain}/validate", method = RequestMethod.POST)
	public boolean validateAuthorization(HttpServletRequest request, @PathVariable String domain, @RequestBody AuthorizationDTO authorization) throws UnauthorizedDomainException {
		checkDomain(request, domain);
		return authorizationHelper.validate(convert(domain, authorization));
	}

	@RequestMapping(value = "/authorization/{domain}/schema", method = RequestMethod.POST)
	public void addRootChildToSchema(HttpServletRequest request, @PathVariable String domain, @RequestBody AuthorizationNodeDTO node)
			throws AuthorizationNodeAlreadyExist, UnauthorizedDomainException {
		checkDomain(request, domain);
		authorizationSchemaHelper.addRootChild(convert(domain, node));
	}

	@RequestMapping(value = "/authorization/{domain}/schema/{parentQname}", method = RequestMethod.POST)
	public void addChildToSchema(HttpServletRequest request, @PathVariable String domain, @RequestBody AuthorizationNodeDTO childNode,
			@PathVariable String parentQname) throws AuthorizationNodeAlreadyExist, UnauthorizedDomainException {
		checkDomain(request, domain);
		authorizationSchemaHelper.addChild(new FQname(domain, parentQname), convert(domain, childNode));
	}

	@RequestMapping(value = "/authorization/{domain}/schema/{qname}", method = RequestMethod.GET)
	public AuthorizationNodeDTO getNode(HttpServletRequest request, @PathVariable String domain, @PathVariable String qname) throws UnauthorizedDomainException {
		checkDomain(request, domain);
		return convert(authorizationSchemaHelper.getNode(new FQname(domain, qname)));
	}

	@RequestMapping(value = "/authorization/{domain}/schema/validate", method = RequestMethod.POST)
	public boolean validateResource(HttpServletRequest request, @PathVariable String domain, @RequestBody AuthorizationResourceDTO resource) throws UnauthorizedDomainException {
		checkDomain(request, domain);
		return authorizationSchemaHelper.isValid(AuthorizationConverter.convert(domain, resource));
	}
	
	private void checkDomain(HttpServletRequest request, String domain) throws UnauthorizedDomainException {
		String parsedToken = it.smartcommunitylab.aac.common.Utils.parseHeaderToken(request);
		OAuth2Authentication auth = resourceServerTokenServices.loadAuthentication(parsedToken);
		String clientId = auth.getOAuth2Request().getClientId();
		ClientDetailsEntity client = clientDetailsRepository.findByClientId(clientId);
		Long developerId = client.getDeveloperId();
		
		User developer = userRepository.findOne(developerId);	
		String role = "authorization_" + domain;
		
		if (!developer.getRoles().stream().filter(x -> role.equals(x.getRole())).findFirst().isPresent()) {
			throw new UnauthorizedDomainException();
		}
	}

	@ExceptionHandler(AuthorizationNodeAlreadyExist.class)
	@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "authorization node already exists")
	public void authorizationNodeAlreadyExist() {

	}

	@ExceptionHandler(NotValidResourceException.class)
	@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "resource in authorization is not valid")
	public void notValidResource() {
	}
	
	@ExceptionHandler(UnauthorizedDomainException.class)
	@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "not authorized for requested domain")
	public void unauthorizedDomain() {
		System.out.println("HERE");
	}	

}
