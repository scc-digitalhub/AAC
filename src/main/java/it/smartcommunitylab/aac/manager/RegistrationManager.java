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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
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
import it.smartcommunitylab.aac.model.Attribute;
import it.smartcommunitylab.aac.model.Registration;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.RegistrationRepository;
import it.smartcommunitylab.aac.repository.UserRepository;

/**
 * @author raman
 *
 */
@Component
@Transactional(rollbackFor = Throwable.class)
public class RegistrationManager {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${application.url}")
    private String applicationURL;
    
    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Autowired
    private RegistrationRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailSender sender;

    @Resource(name = "messageSource")
    private MessageSource messageSource;

    @Autowired
    private ProviderServiceAdapter providerServiceAdapter;

//    @PostConstruct
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void init() throws Exception {
        // create admin as superuser
        User user = userRepository.findByUsername(adminUsername);
        Long id = user.getId();
        
        logger.debug("create admin registration for " + user.getUsername());

        Registration admin = getUserByUserId(Long.toString(id));
        if (admin != null) {
            // update
            admin = updateRegistration(admin.getId(), user.getName(), user.getSurname(), adminPassword, null);
        } else {
            // check if user is registered with different id
            admin = getUserByEmail(user.getUsername());

            if (admin != null) {
                // remove old registration
                repository.delete(admin);
            }

            // register
            admin = registerOffline(user.getName(), user.getSurname(), user.getUsername(), adminPassword,
                    null, false, null);
        }

        logger.trace("admin registration id " + String.valueOf(admin.getId()));

    }

    /*
     * Search users
     */

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

    public Registration getUserByUserId(String id) {
        return repository.findByUserId(id);
    }

    public Registration getUserByEmail(String email) {
        return repository.findByEmail(email);
    }

    public Registration getUserByPwdResetToken(String confirmationToken) throws RegistrationException {
        Registration existing;
        try {
            existing = getUserByToken(confirmationToken);
        } catch (Exception e) {
            throw new RegistrationException(e);
        }
        if (existing == null) {
            throw new NotRegisteredException();
        }
        if (!existing.isConfirmed()) {
            throw new NotConfirmedException();
        }

        return existing;
    }

    /*
     * Registration
     */
    public Registration register(String name, String surname, String email, String password, String lang)
            throws RegistrationException {
        if (!StringUtils.hasText(name) ||
                !StringUtils.hasText(surname) ||
                !StringUtils.hasText(email) ||
                !StringUtils.hasText(password)) {
            throw new InvalidDataException();
        }

        if (lang == null) {
            lang = "en";
        }

        Registration existing = getUserByEmail(email);
        // case when for some reason arrives duplicate call: return existing
        // registration in
        if (existing != null && existing.isConfirmed()) {
            throw new AlreadyRegisteredException("User is already registered");
        } else if (existing != null) {
            return existing;
        }

        Registration reg = new Registration();
        String key;
        try {
            reg.setName(name);
            reg.setSurname(surname);
            reg.setEmail(email);
            reg.setConfirmed(false);
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE, 1);
            reg.setConfirmationDeadline(c.getTime());
            key = generateKey();
            reg.setConfirmationKey(key);
            reg.setPassword(PasswordHash.createHash(password));
            reg.setLang(lang);
            repository.save(reg);
        } catch (NoSuchAlgorithmException e1) {
            logger.error("Error saving (NoSuchAlgorithmException)", e1);
            throw new RegistrationException(e1);
        } catch (InvalidKeySpecException e1) {
            logger.error("Error saving (InvalidKeySpecException)", e1);
            throw new RegistrationException(e1);
        } catch (Exception e1) {
            // failed to save: check there exist one already
            existing = getUserByEmail(email);
            if (existing != null) {
                return existing;
            }
            logger.error("Error saving (save)", e1);
            throw new RegistrationException(e1);
        }

        try {
            sendConfirmationMail(reg, key);
            return reg;
        } catch (Exception e) {
            logger.error("Error saving (Send email)", e);
            throw new RegistrationException(e);
        }
    }

    public Registration registerOffline(String name, String surname, String email, String password, String lang,
            boolean changePwdOnFirstAccess, String confirmationKey) throws RegistrationException {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new InvalidDataException();
        }

        if (lang == null) {
            lang = "en";
        }

        Registration existing = getUserByEmail(email);
        if (existing != null) {
            throw new AlreadyRegisteredException("User is already registered");
        }

        Registration reg = new Registration();
        try {
            reg.setName(name);
            reg.setSurname(surname);
            reg.setEmail(email);
            reg.setPassword(PasswordHash.createHash(password));
            reg.setLang(lang);
            if (StringUtils.hasText(confirmationKey)) {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DATE, 1);
                reg.setConfirmationDeadline(c.getTime());
                reg.setConfirmationKey(confirmationKey);
                reg.setConfirmed(false);
            } else {
                reg.setConfirmed(true);
                reg.setConfirmationDeadline(null);
                reg.setConfirmationKey(null);
            }
            reg.setChangeOnFirstAccess(changePwdOnFirstAccess);
            User globalUser = providerServiceAdapter.updateUser(Config.IDP_INTERNAL, toMap(reg), null);
            reg.setUserId("" + globalUser.getId());

            reg = repository.save(reg);

            return reg;

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RegistrationException(e);
        }
    }

    public User updateRegistration(String email, String name, String surname, String password, String lang)
            throws RegistrationException {
        Registration existing = getUserByEmail(email);
        if (existing == null) {
            throw new NotRegisteredException();
        }

        try {
            existing.setName(name);
            existing.setSurname(surname);
            if (lang != null) {
                existing.setLang(lang);
            }
            if (password != null) {
                existing.setPassword(PasswordHash.createHash(password));
            }
            User globalUser = providerServiceAdapter.updateUser(Config.IDP_INTERNAL, toMap(existing), null);
            existing.setUserId("" + globalUser.getId());

            repository.save(existing);

            return globalUser;
        } catch (Exception e) {
            throw new RegistrationException("Failed to save registration data");
        }
    }

    public Registration updateRegistration(Long registrationId, String name, String surname, String password,
            String lang)
            throws RegistrationException {
        Registration existing = repository.findOne(registrationId);
        if (existing == null) {
            throw new NotRegisteredException();
        }

        try {
            existing.setName(name);
            existing.setSurname(surname);
            if (lang != null) {
                existing.setLang(lang);
            }
            if (password != null) {
                existing.setPassword(PasswordHash.createHash(password));
            }
            User globalUser = providerServiceAdapter.updateUser(Config.IDP_INTERNAL, toMap(existing), null);
            existing.setUserId("" + globalUser.getId());

            existing = repository.save(existing);

            return existing;
        } catch (Exception e) {
            throw new RegistrationException("Failed to save registration data");
        }
    }

    /*
     * Actions
     */
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

        if (!existing.isConfirmed()) {
            existing.setConfirmed(true);
//			existing.setConfirmationKey(null);
//			existing.setConfirmationDeadline(null);
            User globalUser = providerServiceAdapter.updateUser(Config.IDP_INTERNAL, toMap(existing), null);
            existing.setUserId("" + globalUser.getId());
            repository.save(existing);
        }

        return existing;
    }

    public void resendConfirm(String email) throws RegistrationException {
        Registration existing = getUserByEmail(email);
        if (existing == null) {
            throw new NotRegisteredException();
        }

        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 1);
        // if there were duplicate calls too close in time, simply ignore
        if (existing.getConfirmationDeadline() != null
                && c.getTimeInMillis() - existing.getConfirmationDeadline().getTime() < 2000) {
            return;
        }
        try {
//			existing.setConfirmed(false);
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
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 1);
        // if there were duplicate calls too close in time, simply ignore
        if (existing.getConfirmationDeadline() != null
                && c.getTimeInMillis() - existing.getConfirmationDeadline().getTime() < 2000) {
            return;
        }
        try {
//			existing.setConfirmed(false);
            existing.setConfirmationDeadline(c.getTime());
            existing.setChangeOnFirstAccess(false);
            String key = generateKey();
            existing.setConfirmationKey(key);
//			existing.setPassword(null);
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
            existing.setChangeOnFirstAccess(false);
            repository.save(existing);
        } catch (Exception e) {
            throw new RegistrationException(e);
        }
    }

    /*
     * Helpers
     */

    private Map<String, String> toMap(Registration existing) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("name", existing.getName());
        map.put("surname", existing.getSurname());
        map.put(Config.NAME_ATTR, existing.getName());
        map.put(Config.SURNAME_ATTR, existing.getSurname());
        map.put("email", existing.getEmail());
        return map;
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
        vars.put("url", applicationURL + "/internal/confirm?reset=true&confirmationCode=" + key);
        String subject = messageSource.getMessage("reset.subject", null, Locale.forLanguageTag(reg.getLang()));
        sender.sendEmail(reg.getEmail(), "mail/reset_" + lang, subject, vars);
    }

}
