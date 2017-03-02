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

package it.smartcommunitylab.aac.manager;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.InvalidDataException;
import it.smartcommunitylab.aac.common.InvalidPasswordException;
import it.smartcommunitylab.aac.common.NotConfirmedException;
import it.smartcommunitylab.aac.common.NotRegisteredException;
import it.smartcommunitylab.aac.common.PasswordHash;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.dto.RegistrationBean;
import it.smartcommunitylab.aac.model.Registration;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.RegistrationRepository;

/**
 * @author raman
 *
 */
@Component
@Transactional
public class RegistrationManager {

	protected static final Logger logger = Logger.getLogger(RegistrationManager.class);
	
	@Value("${application.url}")
	private String applicationURL;

	@Autowired
	private RegistrationRepository repository;

	@Autowired
	private MailSender sender;

	@Resource(name = "messageSource")
	private MessageSource messageSource;

	@Autowired
	private ProviderServiceAdapter providerServiceAdapter;

	public Registration register(String name, String surname, String email, String password, String lang) throws RegistrationException {
		if (!StringUtils.hasText(name) || !StringUtils.hasText(surname) || !StringUtils.hasText(email) || !StringUtils.hasText(password)) {
			throw new InvalidDataException();
		}

		if (lang == null) {
			lang = "en";
		}

		Registration existing = getUserByEmail(email);
		if (existing != null) {
			throw new AlreadyRegisteredException();
		}

		Registration reg = new Registration();
		try {
			reg.setName(name);
			reg.setSurname(surname);
			reg.setEmail(email);
			reg.setConfirmed(false);
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, 1);
			reg.setConfirmationDeadline(c.getTime());
			String key = generateKey();
			reg.setConfirmationKey(key);
			reg.setPassword(PasswordHash.createHash(password));
			reg.setLang(lang);
			repository.save(reg);
			sendConfirmationMail(reg, key);
			return reg;
		} catch (Exception e) {
			// repository.delete(reg);
			e.printStackTrace();
			throw new RegistrationException(e);
		}
	}

	public Registration confirm(String confirmationToken) throws RegistrationException {
		Registration existing;
		try {
			existing = getUserByToken(confirmationToken);
		} catch (Exception e) {
			throw new RegistrationException(e);
		}
		if (existing == null) {
			throw new NotRegisteredException();
		}
		if (existing.getConfirmationDeadline().before(new Date())) {
			throw new InvalidDataException();
		}

		existing.setConfirmed(true);
		existing.setConfirmationKey(null);
		existing.setConfirmationDeadline(null);
		User globalUser = providerServiceAdapter.updateUser(Config.IDP_INTERNAL, toMap(existing), null);
		existing.setUserId("" + globalUser.getId());

		repository.save(existing);

		return existing;
	}

	public User registerOffline(String name, String surname, String email, String password, String lang) throws RegistrationException {
		if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
			throw new InvalidDataException();
		}

		if (lang == null) {
			lang = "en";
		}

		Registration existing = getUserByEmail(email);
		if (existing != null) {
			throw new AlreadyRegisteredException();
		}

		Registration reg = new Registration();
		try {
			reg.setName(name);
			reg.setSurname(surname);
			reg.setEmail(email);
			reg.setPassword(PasswordHash.createHash(password));
			reg.setLang(lang);
			reg.setConfirmed(true);
			reg.setConfirmationKey(null);
			reg.setConfirmationDeadline(null);
			User globalUser = providerServiceAdapter.updateUser("internal", toMap(reg), null);
			reg.setUserId("" + globalUser.getId());

			repository.save(reg);

			return globalUser;

		} catch (Exception e) {
			e.printStackTrace();
			throw new RegistrationException(e);
		}
	}

	/**
	 * @param existing
	 * @return
	 */
	private Map<String, String> toMap(Registration existing) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("name", existing.getName());
		map.put("surname", existing.getSurname());
		map.put("email", existing.getEmail());
		return map;
	}

	public void resendConfirm(String email) throws RegistrationException {
		Registration existing = getUserByEmail(email);
		if (existing == null) {
			throw new NotRegisteredException();
		}
		try {
			existing.setConfirmed(false);
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, 1);
			existing.setConfirmationDeadline(c.getTime());
			String key = generateKey();
			existing.setConfirmationKey(key);
			repository.save(existing);
			sendConfirmationMail(existing, key);
		} catch (Exception e) {
			throw new RegistrationException(e);
		}
	}

	public void resetPassword(String email) throws RegistrationException {
		Registration existing = getUserByEmail(email);
		if (existing == null) {
			throw new NotRegisteredException();
		}
		try {
			existing.setConfirmed(false);
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DATE, 1);
			existing.setConfirmationDeadline(c.getTime());
			String key = generateKey();
			existing.setConfirmationKey(key);
			existing.setPassword(null);
			repository.save(existing);
			sendResetMail(existing, key);
		} catch (Exception e) {
			throw new RegistrationException(e);
		}
	}

	public void updatePassword(String email, String password) throws RegistrationException {
		Registration existing = getUserByEmail(email);
		if (existing == null) {
			throw new NotRegisteredException();
		}
		try {
			existing.setPassword(PasswordHash.createHash(password));
			repository.save(existing);
		} catch (Exception e) {
			throw new RegistrationException(e);
		}
	}

	public Registration getUser(String email, String password) throws RegistrationException {
		Registration existing = getUserByEmail(email);
		if (existing == null) {
			throw new NotRegisteredException();
		}
		if (!existing.isConfirmed()) {
			throw new NotConfirmedException();
		}
		if (existing.getPassword() == null) {
			throw new InvalidPasswordException();
		}

		boolean matches = false;
		try {
			matches = PasswordHash.validatePassword(password, existing.getPassword());
		} catch (Exception e) {
			throw new RegistrationException(e);
		}

		if (!matches) {
			throw new InvalidPasswordException();
		}
		return existing;
	}

	private Registration getUserByEmail(String email) {
		return repository.findByEmail(email);
	}

	private Registration getUserByToken(String confirmationToken) {
		return repository.findByConfirmationKey(confirmationToken);
	}

	private String generateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		String rnd = UUID.randomUUID().toString();
		return rnd;
	}

	/**
	 * @param reg
	 * @param key
	 * @throws RegistrationException
	 */
	private void sendConfirmationMail(Registration reg, String key) throws RegistrationException {
		RegistrationBean user = new RegistrationBean(reg.getEmail(), reg.getName(), reg.getSurname());
		String lang = reg.getLang();
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("user", user);
		vars.put("url", applicationURL + "/internal/confirm?confirmationCode=" + key);
		String subject = messageSource.getMessage("confirmation.subject", null, Locale.forLanguageTag(reg.getLang()));
		sender.sendEmail(reg.getEmail(), "mail/confirmation_" + lang, subject, vars);
	}

	/**
	 * @param existing
	 * @param key
	 * @throws RegistrationException
	 */
	private void sendResetMail(Registration reg, String key) throws RegistrationException {
		RegistrationBean user = new RegistrationBean(reg.getEmail(), reg.getName(), reg.getSurname());
		String lang = reg.getLang();
		Map<String, Object> vars = new HashMap<String, Object>();
		vars.put("user", user);
		vars.put("url", applicationURL + "/internal/confirm?confirmationCode=" + key);
		String subject = messageSource.getMessage("reset.subject", null, Locale.forLanguageTag(reg.getLang()));
		sender.sendEmail(reg.getEmail(), "mail/reset_" + lang, subject, vars);
	}

}
