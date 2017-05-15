package it.smartcommunitylab.aac.authorization.controller;

import java.util.ArrayList;
import java.util.List;

import it.smartcommunitylab.aac.authorization.beans.AuthorizationDTO;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationNodeDTO;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationNodeParamDTO;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationNodeValueDTO;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationResourceDTO;
import it.smartcommunitylab.aac.authorization.beans.AuthorizationUserDTO;
import it.smartcommunitylab.aac.authorization.model.Authorization;
import it.smartcommunitylab.aac.authorization.model.AuthorizationNode;
import it.smartcommunitylab.aac.authorization.model.AuthorizationNodeParam;
import it.smartcommunitylab.aac.authorization.model.AuthorizationNodeValue;
import it.smartcommunitylab.aac.authorization.model.AuthorizationUser;
import it.smartcommunitylab.aac.authorization.model.Resource;

public class AuthorizationConverter {

	public static Resource convert(AuthorizationResourceDTO resource) {
		List<AuthorizationNodeValue> convertedValueList = new ArrayList<>();
		resource.getValues().stream().forEach(dto -> {
			convertedValueList.add(convert(dto));
		});
		Resource convertedObj = new Resource(resource.getQnameRef(), convertedValueList);
		return convertedObj;
	}

	public static AuthorizationNodeValue convert(AuthorizationNodeValueDTO nodeValue) {
		AuthorizationNodeValue convertedObj = new AuthorizationNodeValue(nodeValue.getQname(), nodeValue.getName(),
				nodeValue.getValue());
		return convertedObj;
	}

	public static Authorization convert(AuthorizationDTO authorization) {
		Authorization convertedObj = new Authorization(convert(authorization.getSubject()), authorization.getAction(),
				convert(authorization.getResource()), convert(authorization.getEntity()));
		return convertedObj;
	}

	private static AuthorizationUser convert(AuthorizationUserDTO authorizationUser) {
		AuthorizationUser convertedObj = new AuthorizationUser(authorizationUser.getId(), authorizationUser.getType());
		return convertedObj;
	}

	public static AuthorizationDTO convert(Authorization authorization) {
		AuthorizationDTO convertedObj = null;
		if (authorization != null) {
			convertedObj = new AuthorizationDTO();
			convertedObj.setAction(authorization.getAction());
			AuthorizationUserDTO entity = new AuthorizationUserDTO();
			entity.setId(authorization.getEntity().getId());
			entity.setType(authorization.getEntity().getType());
			convertedObj.setEntity(entity);
			AuthorizationUserDTO subject = new AuthorizationUserDTO();
			subject.setId(authorization.getSubject().getId());
			subject.setType(authorization.getSubject().getType());
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
			convertedObj.setId(authorizationUser.getId());
			convertedObj.setType(authorizationUser.getType());
		}
		return convertedObj;
	}

	private static AuthorizationResourceDTO convert(Resource resource) {
		AuthorizationResourceDTO convertedObj = new AuthorizationResourceDTO();
		if (resource != null) {
			convertedObj = new AuthorizationResourceDTO();
			convertedObj.setQnameRef(resource.getQnameRef());
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

	public static AuthorizationNode convert(AuthorizationNodeDTO node) {
		AuthorizationNode convertedObj = null;
		if (node != null) {
			convertedObj = new AuthorizationNode(node.getQname());
			for (AuthorizationNodeParamDTO param : node.getParameters()) {
				convertedObj.getParameters().add(convert(param));
			}
		}

		return convertedObj;
	}

	private static AuthorizationNodeParam convert(AuthorizationNodeParamDTO param) {
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
			convertedObj.setQname(node.getQname());
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
