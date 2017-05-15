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

@RestController
public class AuthorizationController {

	@Autowired
	private AuthorizationHelper authorizationHelper;

	@Autowired
	private AuthorizationSchemaHelper authorizationSchemaHelper;

	@RequestMapping(value = "/authorization/{id}", method = RequestMethod.DELETE)
	public void removeAuthorization(@PathVariable String id) {
		authorizationHelper.remove(id);
	}

	@RequestMapping(value = "/authorization", method = RequestMethod.POST)
	public AuthorizationDTO insertAuthorization(@RequestBody AuthorizationDTO authorization)
			throws NotValidResourceException {
		return convert(authorizationHelper.insert(convert(authorization)));
	}

	@RequestMapping(value = "/authorization/validate", method = RequestMethod.POST)
	public boolean validateAuthorization(@RequestBody AuthorizationDTO authorization) {
		return authorizationHelper.validate(convert(authorization));
	}

	@RequestMapping(value = "/authorization/schema", method = RequestMethod.POST)
	public void addRootChildToSchema(@RequestBody AuthorizationNodeDTO node) throws AuthorizationNodeAlreadyExist {
		authorizationSchemaHelper.addRootChild(convert(node));
	}

	@RequestMapping(value = "/authorization/schema/{parentQname}", method = RequestMethod.POST)
	public void addChildToSchema(@RequestBody AuthorizationNodeDTO childNode, @PathVariable String parentQname)
			throws AuthorizationNodeAlreadyExist {
		authorizationSchemaHelper.addChild(parentQname, convert(childNode));
	}

	@RequestMapping(value = "/authorization/schema/{qname}", method = RequestMethod.GET)
	public AuthorizationNodeDTO getNode(@PathVariable String qname) {
		return convert(authorizationSchemaHelper.getNode(qname));
	}

	@RequestMapping(value = "/authorization/schema/validate", method = RequestMethod.POST)
	public boolean validateResource(@RequestBody AuthorizationResourceDTO resource) {
		return authorizationSchemaHelper.isValid(AuthorizationConverter.convert(resource));
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
