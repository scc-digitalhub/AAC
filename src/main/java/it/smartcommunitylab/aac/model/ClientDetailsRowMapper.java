/**
 *    Copyright 2012-2013 Trento RISE
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
 */

package it.smartcommunitylab.aac.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;

import it.smartcommunitylab.aac.Config.ROLE_SCOPE;
import it.smartcommunitylab.aac.repository.UserRepository;

/**
 * DB mapper for the client app information
 * 
 * @author raman
 *
 */
public class ClientDetailsRowMapper implements RowMapper<ClientDetails> {

	private static ObjectMapper mapper = new ObjectMapper();
	private static Log logger = LogFactory.getLog(ClientDetailsRowMapper.class);

	private final UserRepository userRepository;
	
	public ClientDetailsRowMapper(UserRepository userRepository) {
		super();
		this.userRepository = userRepository;
	}

	public ClientDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
		BaseClientDetails details = new BaseClientDetails(rs.getString("client_id"), rs.getString("resource_ids"),
				rs.getString("scope"), rs.getString("authorized_grant_types"), rs.getString("authorities"),
				rs.getString("web_server_redirect_uri"));
		details.setClientSecret(rs.getString("client_secret"));
		if (rs.getObject("access_token_validity") != null) {
			details.setAccessTokenValiditySeconds(rs.getInt("access_token_validity"));
		}
		if (rs.getObject("refresh_token_validity") != null) {
			details.setRefreshTokenValiditySeconds(rs.getInt("refresh_token_validity"));
		}
		String json = rs.getString("additional_information");
		if (json != null) {
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> additionalInformation = mapper.readValue(json, Map.class);
				details.setAdditionalInformation(additionalInformation);
			} catch (Exception e) {
				logger.warn("Could not decode JSON for additional information: " + details, e);
			}
		} else {
			details.setAdditionalInformation(new HashMap<String, Object>());
		}
		
		// merge developer roles into authorities
		it.smartcommunitylab.aac.model.User developer = userRepository.findOne(rs.getLong("developerId"));
		if (developer != null) {
			List<GrantedAuthority> list = new LinkedList<GrantedAuthority>();
			if (details.getAuthorities() != null) list.addAll(details.getAuthorities());
			list.addAll(developer.getRoles().stream().filter(r -> r.getScope().equals(ROLE_SCOPE.tenant) || r.getScope().equals(ROLE_SCOPE.application)).collect(Collectors.toList()));
			details.setAuthorities(list);
		}
		return details;
	}

}
