/*******************************************************************************
 * Copyright 2018 The MIT Internet Trust Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package it.smartcommunitylab.aac.openid.view;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.view.AbstractView;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.smartcommunitylab.aac.manager.ClaimManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.User;

@Component(UserInfoView.VIEWNAME)
public class UserInfoView extends AbstractView {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String CLIENT = "client";
	public static final String REQUESTED_CLAIMS = "requestedClaims";
	public static final String AUTHORIZED_CLAIMS = "authorizedClaims";
	public static final String SELECTED_AUTHORITIES = "selectedAuthorities";
	public static final String SCOPE = "scope";
	public static final String USER_INFO = "userInfo";
	public static final String USER_ID = "userId";

	public static final String VIEWNAME = "userInfoView";

	private static JsonParser jsonParser = new JsonParser();
	
	@Autowired
	private ClaimManager claimManager;
	
	protected Gson gson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {

		@Override
		public boolean shouldSkipField(FieldAttributes f) {

			return false;
		}

		@Override
		public boolean shouldSkipClass(Class<?> clazz) {
			// skip the JPA binding wrapper
			if (clazz.equals(BeanPropertyBindingResult.class)) {
				return true;
			}
			return false;
		}

	}).create();

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.web.servlet.view.AbstractView#renderMergedOutputModel
	 * (java.util.Map, javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {

		User userId = (User) model.get(USER_INFO);
		ClientDetailsEntity client = (ClientDetailsEntity) model.get(CLIENT);
		
		Set<String> scope = (Set<String>) model.get(SCOPE);

		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");


		JsonObject authorizedClaims = null;
		JsonObject requestedClaims = null;
		Collection<? extends GrantedAuthority> selectedAuthorities = null;
		if (model.get(AUTHORIZED_CLAIMS) != null) {
			authorizedClaims = jsonParser.parse((String) model.get(AUTHORIZED_CLAIMS)).getAsJsonObject();
		}
		if (model.get(REQUESTED_CLAIMS) != null) {
			requestedClaims = jsonParser.parse((String) model.get(REQUESTED_CLAIMS)).getAsJsonObject();
		}
		if (model.get(SELECTED_AUTHORITIES) != null) {
			selectedAuthorities = (Collection<? extends GrantedAuthority>) model.get(SELECTED_AUTHORITIES);
		}

		
		Map<String, Object> json = toJsonFromRequestObj(userId, selectedAuthorities, client, scope, authorizedClaims, requestedClaims);

		//append sub identifier
		json.put("sub", userId.getId().toString());
		
		writeOut(json, model, request, response);
	}

	protected void writeOut(Map<String, Object> json, Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {
		try {
			Writer out = response.getWriter();
			gson.toJson(json, out);
		} catch (IOException e) {

			logger.error("IOException in UserInfoView.java: ", e);

		}

	}

	/**
	 * Build a JSON response according to the request object received.
	 *
	 * Claims requested in requestObj.userinfo.claims are added to any
	 * claims corresponding to requested scopes, if any.
	 *
	 * @param user the User to filter
	 * @param client to filter
	 * @param scope the allowed scopes to filter by
	 * @param authorizedClaims the claims authorized by the client or user
	 * @param requestedClaims the claims requested in the RequestObject
	 * @return the filtered JsonObject result
	 */
	private Map<String, Object> toJsonFromRequestObj(User user, Collection<? extends GrantedAuthority> selectedAuthorities, ClientDetailsEntity client, Set<String> scope, JsonObject authorizedClaims, JsonObject requestedClaims) {		
		return claimManager.createUserClaims(user.getId().toString(), selectedAuthorities, client, scope, authorizedClaims, requestedClaims);
	}

}