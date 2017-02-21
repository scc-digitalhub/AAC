/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.authority;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import it.smartcommunitylab.aac.jaxbmodel.Attributes;
import it.smartcommunitylab.aac.jaxbmodel.AuthorityMapping;

/**
 * Default handler. Extract the attributes as specified by the authority mapping
 * 
 * @author raman
 * 
 */
public class DefaultAuthorityHandler implements AuthorityHandler {

	public DefaultAuthorityHandler() {
	}

	@Override
	public Map<String, String> extractAttributes(HttpServletRequest request,
			Map<String, String> map, AuthorityMapping mapping) {
		Map<String, String> attrs = new HashMap<String, String>();
		for (String key : mapping.getIdentifyingAttributes()) {
			Object value = readAttribute(request, key, mapping.isUseParams(), map);
			if (value != null) {
				attrs.put(key, value.toString());
			}
		}
		for (Attributes attribute : mapping.getAttributes()) {
			// used alias if present to set attribute in map
			String key = (attribute.getAlias() != null && !attribute.getAlias()
					.isEmpty()) ? attribute.getAlias() : attribute.getValue();
			Object value = readAttribute(request, attribute.getValue(), mapping.isUseParams(), map);
			if (value != null) {
				attrs.put(key, value.toString());
			}
		}
		return attrs;
	}

	/**
	 * Read either request attribute or a request parameter from HTTP request
	 * 
	 * @param request
	 * @param key
	 * @param useParams
	 *            whether to extract parameter instead of attribute
	 * @return
	 */
	private Object readAttribute(HttpServletRequest request, String key,
			boolean useParams, Map<String,String> extParams) {
		if (request == null) {
			return extParams != null ? extParams.get(key) : null;
		}
		if (useParams) {
			return request.getParameter(key);
		}
		Object param = request.getAttribute(key);
		if (param == null || param.toString().isEmpty()) {
			param = request.getHeader(key);
		}
		return param;
	}

}
