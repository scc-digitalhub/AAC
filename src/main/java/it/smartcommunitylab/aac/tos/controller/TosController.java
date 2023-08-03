/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.tos.controller;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.AuthenticationHelper;
import it.smartcommunitylab.aac.core.MyUserManager;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.auth.RealmAwareAuthenticationEntryPoint;

@Controller
@RequestMapping
public class TosController {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private AuthenticationHelper authHelper;

	@Autowired
	private MyUserManager myUserManager;

	@GetMapping("/terms/{approveTOS}")
	public String terms(@PathVariable @Valid @Pattern(regexp = SystemKeys.SLUG_PATTERN) String approveTOS,
			HttpServletRequest request, Model model, Locale locale)
			throws NoSuchProviderException, NoSuchUserException, NoSuchRealmException {

		UserDetails user = authHelper.getUserDetails();

		if (user == null) {
			throw new InsufficientAuthenticationException("error.unauthenticated_user");
		}

		String realm = user.getRealm();
		model.addAttribute("acceptUrl", "/terms/accept");
		model.addAttribute("realm", realm);
		model.addAttribute("displayName", realm);

		if (approveTOS.equals("true")) {
			model.addAttribute("rejectUrl", "/terms/refuse");
			return "tos/tos_approval";
		}

		return "tos/tos_ok";

	}

	@PostMapping("/terms/accept")
	public String termsAccepted(HttpServletRequest request, Model model, Locale locale)
			throws NoSuchProviderException, NoSuchUserException, NoSuchRealmException {

		UserDetails user = authHelper.getUserDetails();

		if (user == null) {
			throw new InsufficientAuthenticationException("error.unauthenticated_user");
		}

		request.getSession().setAttribute("termsManaged", "true");
		this.logger.debug("terms of service approved");

		return "redirect:/";
	}

	@GetMapping("/terms/refuse")
	public String termsRefused(HttpServletRequest request, Model model, Locale locale)
			throws NoSuchProviderException, NoSuchUserException, NoSuchRealmException {

		UserDetails user = authHelper.getUserDetails();

		if (user == null) {
			throw new InsufficientAuthenticationException("error.unauthenticated_user");
		}

		this.logger.debug("logout user after terms refusal");
		SecurityContextHolder.clearContext();
		request.setAttribute(RealmAwareAuthenticationEntryPoint.REALM_URI_VARIABLE_NAME, user.getRealm());

		return "error/tos_refuse";

	}

}