package it.smartcommunitylab.aac.authorization.controller;

import java.util.ArrayList;
import java.util.List;

import it.smartcommunitylab.aac.authorization.beans.AccountAttributeDTO;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationDTO;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationNodeDTO;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationNodeParamDTO;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationNodeValueDTO;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationResourceDTO;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationUserDTO;
import it.smartcommunitylab.aac.authorization.beans.RequestedAuthorizationDTO;
import it.smartcommunitylab.aac.authorization.model.AccountAttribute;
import it.smartcommunitylab.aac.authorization.model.Authorization;
import it.smartcommunitylab.aac.authorization.model.AuthorizationNode;
import it.smartcommunitylab.aac.authorization.model.AuthorizationNodeParam;
import it.smartcommunitylab.aac.authorization.model.AuthorizationNodeValue;
import it.smartcommunitylab.aac.authorization.model.AuthorizationUser;
import it.smartcommunitylab.aac.authorization.model.FQname;
import it.smartcommunitylab.aac.authorization.model.RequestedAuthorization;
import it.smartcommunitylab.aac.authorization.model.Resource;

public class AuthorizationConverter {

	public static Resource convert(String domain, AuthorizationResourceDTO resource) {
		List<AuthorizationNodeValue> convertedValueList = new ArrayList<>();
		resource.getValues().stream().forEach(dto -> {
			convertedValueList.add(convert(domain, dto));
		});
		Resource convertedObj = new Resource(new FQname(domain, resource.getQnameRef()), convertedValueList);
		return convertedObj;
	}

	public static AuthorizationNodeValue convert(String domain, AuthorizationNodeValueDTO nodeValue) {
		AuthorizationNodeValue convertedObj = new AuthorizationNodeValue(nodeValue.getQname(), nodeValue.getName(),
				nodeValue.getValue());
		return convertedObj;
	}

	public static Authorization convert(String domain, AuthorizationDTO authorization) {
		Authorization convertedObj = new Authorization(convert(authorization.getSubject()), authorization.getAction(),
				convert(domain, authorization.getResource()), convert(authorization.getEntity()));
		return convertedObj;
	}

	public static RequestedAuthorization convert(String domain, RequestedAuthorizationDTO authorization) {
		RequestedAuthorization convertedObj = new RequestedAuthorization(authorization.getAction(),
				convert(domain, authorization.getResource()), convert(authorization.getEntity()));
		return convertedObj;
	}

	private static AuthorizationUser convert(AuthorizationUserDTO authorizationUser) {
		if (authorizationUser != null) {
			AuthorizationUser convertedObj = new AuthorizationUser(convert(authorizationUser.getAccountAttribute()),
					authorizationUser.getType());
			return convertedObj;
		}
		return null;
	}

	public static AuthorizationDTO convert(Authorization authorization) {
		AuthorizationDTO convertedObj = null;
		if (authorization != null) {
			convertedObj = new AuthorizationDTO();
			convertedObj.setAction(authorization.getActions());
			convertedObj.setEntity(convert(authorization.getEntity()));
			convertedObj.setSubject(convert(authorization.getSubject()));
			convertedObj.setId(authorization.getId());

			convertedObj.setResource(convert(authorization.getResource()));
		}

		return convertedObj;
	}

	private static AuthorizationUserDTO convert(AuthorizationUser authorizationUser) {
		AuthorizationUserDTO convertedObj = null;
		if (authorizationUser != null) {
			convertedObj = new AuthorizationUserDTO();
			convertedObj.setAccountAttribute(convert(authorizationUser.getAccountAttribute()));
			convertedObj.setType(authorizationUser.getType());
		}
		return convertedObj;
	}

	private static AccountAttributeDTO convert(AccountAttribute accountAttribute) {
		AccountAttributeDTO convertedObj = null;
		if (accountAttribute != null) {
			convertedObj = new AccountAttributeDTO();
			convertedObj.setAccountName(accountAttribute.getAccountName());
			convertedObj.setAttributeName(accountAttribute.getAttributeName());
			convertedObj.setAttributeValue(accountAttribute.getAttributeValue());
		}

		return convertedObj;
	}

	private static AccountAttribute convert(AccountAttributeDTO accountAttribute) {
		AccountAttribute convertedObj = null;
		if (accountAttribute != null) {
			convertedObj = new AccountAttribute(accountAttribute.getAccountName(), accountAttribute.getAttributeName(),
					accountAttribute.getAttributeValue());
		}

		return convertedObj;
	}

	private static AuthorizationResourceDTO convert(Resource resource) {
		AuthorizationResourceDTO convertedObj = null;
		if (resource != null) {
			convertedObj = new AuthorizationResourceDTO();
			convertedObj.setQnameRef(resource.getFqnameRef().getQname());
			if (resource.getValues() != null) {
				List<AuthorizationNodeValueDTO> values = new ArrayList<>();
				resource.getValues().stream().forEach(nodeValue -> {
					values.add(convert(nodeValue));
				});
				convertedObj.setValues(values);
			}
		}
		return convertedObj;
	}

	private static AuthorizationNodeValueDTO convert(AuthorizationNodeValue nodeValue) {
		AuthorizationNodeValueDTO convertedObj = null;
		if (nodeValue != null) {
			convertedObj = new AuthorizationNodeValueDTO();
			if (nodeValue.getDefinition() != null) {
				convertedObj.setName(nodeValue.getDefinition().getName());
				convertedObj.setQname(nodeValue.getDefinition().getQname());
			}
			convertedObj.setValue(nodeValue.getValue());
		}
		return convertedObj;
	}

	public static AuthorizationNode convert(String domain, AuthorizationNodeDTO node) {
		AuthorizationNode convertedObj = null;
		if (node != null) {
			convertedObj = new AuthorizationNode(new FQname(domain, node.getQname()));
			for (AuthorizationNodeParamDTO param : node.getParameters()) {
				convertedObj.getParameters().add(convert(domain, param));
			}
		}

		return convertedObj;
	}

	private static AuthorizationNodeParam convert(String domain, AuthorizationNodeParamDTO param) {
		AuthorizationNodeParam convertedObj = null;
		if (param != null) {
			convertedObj = new AuthorizationNodeParam(param.getQname(), param.getName());
		}
		return convertedObj;
	}

	public static AuthorizationNodeDTO convert(AuthorizationNode node) {
		AuthorizationNodeDTO convertedObj = null;
		if (node != null) {
			convertedObj = new AuthorizationNodeDTO();
			convertedObj.setQname(node.getFqname().getQname());
			List<AuthorizationNodeParamDTO> params = new ArrayList<>();
			node.getParameters().stream().forEach(param -> {
				AuthorizationNodeParamDTO paramDTO = new AuthorizationNodeParamDTO();
				paramDTO.setQname(param.getQname());
				paramDTO.setName(param.getName());
				params.add(paramDTO);
			});
			convertedObj.setParameters(params);
		}

		return convertedObj;
	}

}
