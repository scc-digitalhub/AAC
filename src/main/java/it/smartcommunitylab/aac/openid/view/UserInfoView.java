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
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.view.AbstractView;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.dto.BasicProfile;

@Component(UserInfoView.VIEWNAME)
public class UserInfoView extends AbstractView {

	public static final String REQUESTED_CLAIMS = "requestedClaims";
	public static final String AUTHORIZED_CLAIMS = "authorizedClaims";
	public static final String SCOPE = "scope";
	public static final String USER_INFO = "userInfo";

	public static final String VIEWNAME = "userInfoView";

	private static JsonParser jsonParser = new JsonParser();
	
	private SetMultimap<String, String> scopesToClaims = HashMultimap.create();

	/**
	 * Logger for this class
	 */
	private static final Logger logger = LoggerFactory.getLogger(UserInfoView.class);

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

	public UserInfoView() {
		super();
		// standard
		scopesToClaims.put("openid", "sub");
		// standard
		scopesToClaims.put("profile", "name");
		scopesToClaims.put("profile", "preferred_username");
		scopesToClaims.put("profile", "given_name");
		scopesToClaims.put("profile", "family_name");
		scopesToClaims.put("profile", "middle_name");
		scopesToClaims.put("profile", "nickname");
		scopesToClaims.put("profile", "profile");
		scopesToClaims.put("profile", "picture");
		scopesToClaims.put("profile", "website");
		scopesToClaims.put("profile", "gender");
		scopesToClaims.put("profile", "zoneinfo");
		scopesToClaims.put("profile", "locale");
		scopesToClaims.put("profile", "updated_at");
		scopesToClaims.put("profile", "birthdate");
		// standard
		scopesToClaims.put("email", "email");
		scopesToClaims.put("email", "email_verified");
		// standard
		scopesToClaims.put("phone", "phone_number");
		scopesToClaims.put("phone", "phone_number_verified");
		// standard
		scopesToClaims.put("address", "address");
		
		// aac-specific
		scopesToClaims.put(Config.BASIC_PROFILE_SCOPE, "name");
		scopesToClaims.put(Config.BASIC_PROFILE_SCOPE, "preferred_username");
		scopesToClaims.put(Config.BASIC_PROFILE_SCOPE, "given_name");
		scopesToClaims.put(Config.BASIC_PROFILE_SCOPE, "family_name");
		scopesToClaims.put(Config.BASIC_PROFILE_SCOPE, "email");

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.web.servlet.view.AbstractView#renderMergedOutputModel
	 * (java.util.Map, javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {

		BasicProfile userInfo = (BasicProfile) model.get(USER_INFO);

		@SuppressWarnings("unchecked")
		Set<String> scope = (Set<String>) model.get(SCOPE);

		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");


		JsonObject authorizedClaims = null;
		JsonObject requestedClaims = null;
		if (model.get(AUTHORIZED_CLAIMS) != null) {
			authorizedClaims = jsonParser.parse((String) model.get(AUTHORIZED_CLAIMS)).getAsJsonObject();
		}
		if (model.get(REQUESTED_CLAIMS) != null) {
			requestedClaims = jsonParser.parse((String) model.get(REQUESTED_CLAIMS)).getAsJsonObject();
		}
		JsonObject json = toJsonFromRequestObj(userInfo, scope, authorizedClaims, requestedClaims);

		writeOut(json, model, request, response);
	}

	protected void writeOut(JsonObject json, Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) {
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
	 * @param ui the UserInfo to filter
	 * @param scope the allowed scopes to filter by
	 * @param authorizedClaims the claims authorized by the client or user
	 * @param requestedClaims the claims requested in the RequestObject
	 * @return the filtered JsonObject result
	 */
	private JsonObject toJsonFromRequestObj(BasicProfile ui, Set<String> scope, JsonObject authorizedClaims, JsonObject requestedClaims) {

		// get the base object
		JsonObject obj = toJson(ui);

		Set<String> allowedByScope = getClaimsForScopeSet(scope);
		Set<String> authorizedByClaims = extractUserInfoClaimsIntoSet(authorizedClaims);
		Set<String> requestedByClaims = extractUserInfoClaimsIntoSet(requestedClaims);

		// Filter claims by performing a manual intersection of claims that are allowed by the given scope, requested, and authorized.
		// We cannot use Sets.intersection() or similar because Entry<> objects will evaluate to being unequal if their values are
		// different, whereas we are only interested in matching the Entry<>'s key values.
		JsonObject result = new JsonObject();
		for (Entry<String, JsonElement> entry : obj.entrySet()) {

			if (allowedByScope.contains(entry.getKey())
					|| authorizedByClaims.contains(entry.getKey())) {
				// it's allowed either by scope or by the authorized claims (either way is fine with us)

				if (requestedByClaims.isEmpty() || requestedByClaims.contains(entry.getKey())) {
					// the requested claims are empty (so we allow all), or they're not empty and this claim was specifically asked for
					result.add(entry.getKey(), entry.getValue());
				} // otherwise there were specific claims requested and this wasn't one of them
			}
		}

		return result;
	}

	/**
	 * @param ui
	 * @return
	 */
	private JsonObject toJson(BasicProfile ui) {

		JsonObject obj = new JsonObject();
		obj.addProperty("sub", ui.getUserId());

		obj.addProperty("name", ui.getSurname() + " " + ui.getName());
		obj.addProperty("preferred_username", ui.getUsername());
		obj.addProperty("given_name", ui.getName());
		obj.addProperty("family_name", ui.getSurname());

		obj.addProperty("email", ui.getUsername());

		return obj;
	}

	/**
	 * Pull the claims that have been targeted into a set for processing.
	 * Returns an empty set if the input is null.
	 * @param claims the claims request to process
	 */
	private Set<String> extractUserInfoClaimsIntoSet(JsonObject claims) {
		Set<String> target = new HashSet<>();
		if (claims != null) {
			JsonObject userinfoAuthorized = claims.getAsJsonObject("userinfo");
			if (userinfoAuthorized != null) {
				for (Entry<String, JsonElement> entry : userinfoAuthorized.entrySet()) {
					target.add(entry.getKey());
				}
			}
		}
		return target;
	}
	
	public Set<String> getClaimsForScope(String scope) {
		if (scopesToClaims.containsKey(scope)) {
			return scopesToClaims.get(scope);
		} else {
			return new HashSet<>();
		}
	}
	public Set<String> getClaimsForScopeSet(Set<String> scopes) {
		Set<String> result = new HashSet<>();
		for (String scope : scopes) {
			result.addAll(getClaimsForScope(scope));
		}
		return result;
	}
}