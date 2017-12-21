/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
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

package it.smartcommunitylab.aac.auth.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.apimanager.APIProviderManager;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.dto.RegistrationBean;
import it.smartcommunitylab.aac.manager.RegistrationService;
import it.smartcommunitylab.aac.model.Registration;
import springfox.documentation.annotations.ApiIgnore;

/**
 * @author raman
 *
 */
@Controller
@RequestMapping
public class RegistrationController {


	protected static final Logger logger = Logger.getLogger(RegistrationController.class);

	@Autowired
	private RegistrationService regService;
	@Autowired
	private APIProviderManager apiProviderManager;
	
	/**
	 * Login the user 
	 * 
	 * @param model
	 * @param username
	 * @param password
	 * @param req
	 * @return
	 */
	@ApiIgnore
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public String login(
			Model model,
			@RequestParam String username, 
			@RequestParam String password,
			HttpServletRequest req) 
	{
		try {
			Registration user = regService.getUser(username, password);
			String target = (String)req.getSession().getAttribute("redirect");
			try {
				if (!StringUtils.hasText(target)) {
					target = "/";
				}
				target = URLEncoder.encode(target, "UTF8");
			} catch (UnsupportedEncodingException e) {
				throw new RegistrationException(e);
			}
			
			List<GrantedAuthority> list = new LinkedList<>();
			list.add(new SimpleGrantedAuthority(Config.R_USER));
			
			AbstractAuthenticationToken a = new UsernamePasswordAuthenticationToken(username, password, list);
			a.setDetails(Config.IDP_INTERNAL);

			SecurityContextHolder.getContext().setAuthentication(a);
			
			String redirect = String
					.format("redirect:/eauth/internal?target=%s&email=%s",
							target,
							user.getEmail());
			return redirect;
		} catch (RegistrationException e) {
			model.addAttribute("error", e.getClass().getSimpleName());
			return "login";
		}
	}
	/**
	 * Redirect to registration page
	 * @param model
	 * @param req
	 * @return
	 */
	@ApiIgnore
	@RequestMapping("/internal/register")
	public String regPage(Model model,
			HttpServletRequest req) {
		model.addAttribute("reg", new RegistrationBean());
		return "registration/register";
	}
	
	/**
	 * Register the user and redirect to the 'registersuccess' page
	 * @param model
	 * @param reg
	 * @param result
	 * @param req
	 * @return
	 */
	@ApiIgnore
	@RequestMapping(value = "/internal/register", method = RequestMethod.POST)
	public String register(Model model, 
			@ModelAttribute("reg") @Valid RegistrationBean reg,
			BindingResult result,
			HttpServletRequest req) 
	{
		if (result.hasErrors()) {
			return "registration/register";
        }
		try {
			apiProviderManager.createAPIUser(reg);

			regService.register(reg.getName(), reg.getSurname(), reg.getEmail(), reg.getPassword(), reg.getLang());
			return "registration/regsuccess";
		} catch (RegistrationException e) {
			model.addAttribute("error", e.getClass().getSimpleName());
			return "registration/register";
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			model.addAttribute("error", RegistrationException.class.getSimpleName());
			return "registration/register";
		}
	}
	
	/**
	 * Register with the REST call
	 * @param model
	 * @param reg
	 * @param result
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/internal/register/rest", method = RequestMethod.POST)
	public @ResponseBody void registerREST(@RequestBody RegistrationBean reg,
			HttpServletResponse res) 
	{
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();
		Set<ConstraintViolation<RegistrationBean>> errors = validator.validate(reg);
	      
		if (errors.size() > 0) {
			res.setStatus(HttpStatus.BAD_REQUEST.value());
			return;
        }
		try {
			apiProviderManager.createAPIUser(reg);
			regService.register(reg.getName(), reg.getSurname(), reg.getEmail(), reg.getPassword(), reg.getLang());
		} catch(AlreadyRegisteredException e) {
			res.setStatus(HttpStatus.CONFLICT.value());
		} catch (RegistrationException e) {
			logger.error(e.getMessage(), e);
			res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		}
	}
	

	/**
	 * Redirect to the resend page to ask for the email
	 * @param model
	 * @param username
	 * @return
	 */
	@ApiIgnore
	@RequestMapping(value = "/internal/resend")
	public String resendPage() {
		return "registration/resend";
	}

	/**
	 * Resend the confirmation link to the registered user.
	 * @param model
	 * @param username
	 * @return
	 */
	@ApiIgnore
	@RequestMapping(value = "/internal/resend", method = RequestMethod.POST)
	public String resendConfirm(Model model, @RequestParam String username) {
		try {
			regService.resendConfirm(username);
			return "registration/regsuccess";
		} catch (RegistrationException e) {
			logger.error(e.getMessage(), e);
			model.addAttribute("error", e.getClass().getSimpleName());
			return "registration/resend";
		}
	}

	/**
	 * Confirm the user given the confirmation code sent via mail. Redirect to confirmsuccess page
	 * @param model
	 * @param confirmationCode
	 * @return
	 */
	@ApiIgnore
	@RequestMapping("/internal/confirm")
	public String confirm(Model model, @RequestParam String confirmationCode, HttpServletRequest req) {
		try {
			Registration user = regService.confirm(confirmationCode);
			if (user.getPassword() != null && !user.isChangeOnFirstAccess()) {
				return "registration/confirmsuccess";
			} else {
				req.getSession().setAttribute("changePwdEmail", user.getEmail());
				model.addAttribute("reg", new RegistrationBean());
				return "registration/changepwd";
			}
		} catch (RegistrationException e) {
			logger.error(e.getMessage(), e);
			model.addAttribute("error", e.getClass().getSimpleName());
			return "registration/confirmerror";
		}
	}
	
	@ApiIgnore
	@RequestMapping(value = "/internal/reset", method = RequestMethod.GET)
	public String resetPage() {
		return "registration/resetpwd";
	}
	@ApiIgnore
	@RequestMapping(value = "/internal/reset", method = RequestMethod.POST)
	public String reset(Model model, @RequestParam String username) {
		try {
			regService.resetPassword(username);
		} catch (RegistrationException e) {
			model.addAttribute("error", e.getClass().getSimpleName());
			return "registration/resetpwd";
		}
		return "registration/resetsuccess";
	}
	@ApiIgnore
	@RequestMapping(value = "/internal/changepwd", method = RequestMethod.POST)
	public String changePwd(Model model, 
			@ModelAttribute("reg") @Valid RegistrationBean reg,
			BindingResult result,
			HttpServletRequest req) 
	{
		if (result.hasFieldErrors("password")) {
			return "registration/changepwd";
		}
		String userMail = (String)req.getSession().getAttribute("changePwdEmail");
		if (userMail == null) {
			model.addAttribute("error", RegistrationException.class.getSimpleName());
			return "registration/changepwd";
		}
		req.getSession().removeAttribute("changePwdEmail");
		
		try {
			apiProviderManager.updatePassword(userMail, reg.getPassword());
			regService.updatePassword(userMail, reg.getPassword());
		} catch (RegistrationException e) {
			logger.error(e.getMessage(), e);
			model.addAttribute("error", e.getClass().getSimpleName());
			return "registration/changepwd";
		}
		return "registration/changesuccess";
	}
}
