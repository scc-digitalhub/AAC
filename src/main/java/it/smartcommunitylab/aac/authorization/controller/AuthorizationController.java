package it.smartcommunitylab.aac.authorization.controller;

import static it.smartcommunitylab.aac.authorization.controller.AuthorizationConverter.convert;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

@RestController
public class AuthorizationController {

	@Autowired
	private AuthorizationHelper authorizationHelper;

	@Autowired
	private AuthorizationSchemaHelper authorizationSchemaHelper;

	@RequestMapping(value = "/authorization/{domain}/{id}", method = RequestMethod.DELETE)
	public void removeAuthorization(@PathVariable String domain, @PathVariable String id) {
		authorizationHelper.remove(id);
	}

	@RequestMapping(value = "/authorization/{domain}", method = RequestMethod.POST)
	public AuthorizationDTO insertAuthorization(@PathVariable String domain,
			@RequestBody AuthorizationDTO authorizationDTO) throws NotValidResourceException {
		return convert(authorizationHelper.insert(convert(domain, authorizationDTO)));
	}

	@RequestMapping(value = "/authorization/{domain}/validate", method = RequestMethod.POST)
	public boolean validateAuthorization(@PathVariable String domain, @RequestBody AuthorizationDTO authorization) {
		return authorizationHelper.validate(convert(domain, authorization));
	}

	@RequestMapping(value = "/authorization/{domain}/schema", method = RequestMethod.POST)
	public void addRootChildToSchema(@PathVariable String domain, @RequestBody AuthorizationNodeDTO node)
			throws AuthorizationNodeAlreadyExist {
		authorizationSchemaHelper.addRootChild(convert(domain, node));
	}

	@RequestMapping(value = "/authorization/{domain}/schema/{parentQname}", method = RequestMethod.POST)
	public void addChildToSchema(@PathVariable String domain, @RequestBody AuthorizationNodeDTO childNode,
			@PathVariable String parentQname) throws AuthorizationNodeAlreadyExist {
		authorizationSchemaHelper.addChild(new FQname(domain, parentQname), convert(domain, childNode));
	}

	@RequestMapping(value = "/authorization/{domain}/schema/{qname}", method = RequestMethod.GET)
	public AuthorizationNodeDTO getNode(@PathVariable String domain, @PathVariable String qname) {
		return convert(authorizationSchemaHelper.getNode(new FQname(domain, qname)));
	}

	@RequestMapping(value = "/authorization/{domain}/schema/validate", method = RequestMethod.POST)
	public boolean validateResource(@PathVariable String domain, @RequestBody AuthorizationResourceDTO resource) {
		return authorizationSchemaHelper.isValid(AuthorizationConverter.convert(domain, resource));
	}

	@ExceptionHandler(AuthorizationNodeAlreadyExist.class)
	@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "authorization node already exists")
	public void authorizationNodeAlreadyExist() {

	}

	@ExceptionHandler(NotValidResourceException.class)
	@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "resource in authorization is not valid")
	public void notValidResource() {

	}

}
