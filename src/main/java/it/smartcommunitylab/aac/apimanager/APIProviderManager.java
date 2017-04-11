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

package it.smartcommunitylab.aac.apimanager;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.util.StringUtils;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.um.ws.api.stub.ClaimValue;
import org.wso2.carbon.um.ws.api.stub.RemoteUserStoreManagerServiceUserStoreExceptionException;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.Config.ROLE_SCOPE;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.dto.RegistrationBean;
import it.smartcommunitylab.aac.manager.MailSender;
import it.smartcommunitylab.aac.manager.RegistrationManager;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientAppInfo;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;
import it.smartcommunitylab.aac.repository.UserRepository;
import it.smartcommunitylab.aac.wso2.services.UserManagementService;
import it.smartcommunitylab.aac.wso2.services.Utils;

/**
 * @author raman
 *
 */
@Transactional
public class APIProviderManager {
	
	@Value("${application.url}")
	private String applicationURL;
	@Resource(name = "messageSource")
	private MessageSource messageSource;

	@Autowired
	private MailSender sender;

	/** APIMananger email */
	public static final String EMAIL_ATTR = "email";

	private static final String API_MGT_CLIENT_ID = "API_MGT_CLIENT_ID";
	private static final String[] GRANT_TYPES = new String []{"password","client_credentials"};
	private static final String[] API_MGT_SCOPES = new String[]{"openid","apim:subscribe","apim:api_view","apim:subscription_view","apim:api_create"};
	/** Predefined tenant role PROVIDER (API provider) */
	public static final String R_PROVIDER = "ROLE_PROVIDER";
	
	@Autowired
	@Qualifier("appTokenServices")
	AuthorizationServerTokenServices tokenService;
	@Autowired
	private UserManager userManager;
	@Autowired
	private ClientDetailsRepository clientDetailsRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private UserManagementService umService;
	@Autowired
	private RegistrationManager regManager;
	@Autowired
	private RoleManager roleManager;
	
	public void createAPIProvider(APIProvider provider) throws RegistrationException {
		//check user exists.
		List<User> users = userRepository.findByAttributeEntities(Config.IDP_INTERNAL, EMAIL_ATTR, provider.getEmail());
		if (users != null && !users.isEmpty()) {
			User user = users.get(0);
			Set<Role> providerRoles = user.role(ROLE_SCOPE.tenant, R_PROVIDER);
			// if the existing user is already a provider for a different domain, throw an exception
			if (!providerRoles.isEmpty() && !providerRoles.iterator().next().getContext().equals(provider.getDomain())) {
				throw new AlreadyRegisteredException("A user with the same username is already registered locally");
			}
		}
		// create WSO2 publisher (tenant and tenant admin)
		String password = generatePassword();
		String key =  RandomStringUtils.randomAlphanumeric(24);
		// create registration data and user attributes
		User created = regManager.registerOffline(provider.getName(), provider.getSurname(), provider.getEmail(), password, provider.getLang(), true, key);
		Role providerRole = new Role(ROLE_SCOPE.tenant, R_PROVIDER, provider.getDomain());
		roleManager.addRole(created, providerRole);

		try {
			umService.createPublisher(provider.getDomain(), provider.getEmail(), password, provider.getName(), provider.getSurname());
			sendConfirmationMail(provider, key);
		} catch (RemoteException | RemoteUserStoreManagerServiceUserStoreExceptionException | TenantMgtAdminServiceExceptionException e) {
			throw new RegistrationException(e.getMessage());
		}
		
	}
	
	/**
	 * Update user password in API Manager
	 * @param reg
	 */
	public void updatePassword(String email, String newPassword) {
		try {
			List<User> users = userRepository.findByAttributeEntities(Config.IDP_INTERNAL, EMAIL_ATTR, email);
			if (users != null && !users.isEmpty()) {
				User user = users.get(0);
				Set<Role> providerRoles = user.role(ROLE_SCOPE.tenant, R_PROVIDER);
				if (providerRoles != null && providerRoles.size() == 1) {
					Role providerRole = providerRoles.iterator().next();
					umService.updatePublisherPassword(email, providerRole.getContext(), newPassword);
				} else {
					umService.updateNormalUserPassword(email, newPassword);
				}
			} else {
				throw new RegistrationException("User does not exist");
			}	
		} catch (RemoteException | RemoteUserStoreManagerServiceUserStoreExceptionException e) {
			throw new RegistrationException(e.getMessage());
		}
	}
	/**
	 * Create new API user in API Manager
	 * @param reg
	 */
	public void createAPIUser(RegistrationBean reg) {
		try {
			ClaimValue[] claims = new ClaimValue[]{
					Utils.createClaimValue("http://wso2.org/claims/emailaddress", reg.getEmail()),
					Utils.createClaimValue("http://wso2.org/claims/givenname", reg.getName()),
					Utils.createClaimValue("http://wso2.org/claims/lastname", reg.getSurname())
			};
			umService.createSubscriber(reg.getEmail(), reg.getPassword(), claims);
		} catch (RemoteException | RemoteUserStoreManagerServiceUserStoreExceptionException e) {
			throw new RegistrationException(e.getMessage());
		}	
	}

	/**
	 * 
	 * @return
	 * @throws Exception
	 */
	public String createToken() throws Exception {
		Map<String, String> requestParameters = new HashMap<>();
		String apiManagerName = getAPIManagerName();
		if (apiManagerName == null) {
			return null;
		}
		requestParameters.put("username", apiManagerName);
		requestParameters.put("password", "");
		
		ClientDetails clientDetails = getAPIMgmtClient();
		TokenRequest tokenRequest = new TokenRequest(requestParameters, clientDetails.getClientId(), scopes(), "password");
		OAuth2Request oAuth2Request = tokenRequest.createOAuth2Request(clientDetails);
		Collection<? extends GrantedAuthority> list = authorities();
		OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, new UsernamePasswordAuthenticationToken(apiManagerName, "", list));
		OAuth2AccessToken accessToken = tokenService.createAccessToken(oAuth2Authentication);
		return accessToken.getValue();
	}

	/**
	 * @return
	 */
	private Collection<? extends GrantedAuthority> authorities() {
		return roleManager.buildAuthorities(userManager.getUser());
	}

	/**
	 * @return
	 */
	private Collection<String> scopes() {
		return Arrays.asList(API_MGT_SCOPES);
	}

	/**
	 * @return
	 * @throws Exception 
	 */
	private ClientDetails getAPIMgmtClient() throws Exception {
		ClientDetails client = clientDetailsRepository.findByClientId(API_MGT_CLIENT_ID);
		if (client == null) {
			ClientDetailsEntity entity = new ClientDetailsEntity();
			ClientAppInfo info = new ClientAppInfo();
			info.setName(API_MGT_CLIENT_ID);
			entity.setAdditionalInformation(info.toJson());
			entity.setClientId(API_MGT_CLIENT_ID);
			entity.setAuthorities(Config.AUTHORITY.ROLE_CLIENT_TRUSTED.name());
			entity.setAuthorizedGrantTypes(defaultGrantTypes());
			entity.setDeveloperId(0L);
			entity.setClientSecret(generateClientSecret());
			entity.setClientSecretMobile(generateClientSecret());
			entity = clientDetailsRepository.save(entity);
			client = entity;
		}
		return client;
	}

	/**
	 * @return
	 */
	private String generateClientSecret() {
		return UUID.randomUUID().toString();
	}

	/**
	 * @return
	 */
	private String defaultGrantTypes() {
		return StringUtils.arrayToCommaDelimitedString(GRANT_TYPES);
	}
	/**
	 * @return
	 */
	private String generatePassword() {
		return RandomStringUtils.randomAlphanumeric(8);
	}
	
	/**
	 * @param user
	 * @return
	 */
	private String getAPIManagerName() {
		User user = userManager.getUser();
		if (user == null) return null;
		Set<Role> providerRoles = user.role(ROLE_SCOPE.tenant, R_PROVIDER);
		if (providerRoles.isEmpty()) return null;
		
		String email = user.attributeValue(Config.IDP_INTERNAL, EMAIL_ATTR);
		if (email == null) return null;
		Role role = providerRoles.iterator().next();
		
		return Utils.getUserNameAtTenant(email, role.getContext());
	}
	
	/**
	 * @param reg
	 * @param key
	 * @throws RegistrationException
	 */
	private void sendConfirmationMail(APIProvider provider,  String key) throws RegistrationException {
		RegistrationBean user = new RegistrationBean(provider.getEmail(), provider.getName(), provider.getSurname());
		String lang = StringUtils.hasText(provider.getLang()) ? provider.getLang() : Config.DEFAULT_LANG;
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("user", user);
		vars.put("url", applicationURL + "/internal/confirm?confirmationCode=" + key);
		String subject = messageSource.getMessage("confirmation.subject", null, Locale.forLanguageTag(lang));
		sender.sendEmail(provider.getEmail(), "mail/provider_reg_" + lang, subject, vars);
	}

}
