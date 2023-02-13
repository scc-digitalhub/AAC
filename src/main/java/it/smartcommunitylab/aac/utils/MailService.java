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

package it.smartcommunitylab.aac.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import it.smartcommunitylab.aac.config.ApplicationProperties;
import it.smartcommunitylab.aac.core.service.RealmService;
import it.smartcommunitylab.aac.model.Realm;

/**
 * Mail send utilities
 * 
 * 
 * TODO rework, add link to userEntityService + realmService TODO implement here
 * message handling for userDetails/userIdentity/userAccount
 *
 * @author raman
 */
@Component
public class MailService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static JavaMailSenderImpl mailSender = null;

    @Value("${mail.username}")
    private String mailUser;

    @Value("${mail.password}")
    private String mailPwd;

    @Value("${mail.host}")
    private String mailHost;

    @Value("${mail.port}")
    private Integer mailPort;

    @Value("${mail.protocol}")
    private String mailProtocol;

    @Autowired
    private ApplicationProperties appProps;

    @Value("classpath:/javamail.properties")
    private org.springframework.core.io.Resource mailProps;

    @Resource(name = "messageSource")
    private MessageSource messageSource;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private RealmService realmService;

    public MailService() throws IOException {
        if (mailSender == null) {
            mailSender = new org.springframework.mail.javamail.JavaMailSenderImpl();
        }
    }

    @PostConstruct
    public void init() throws IOException {
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setProtocol(mailProtocol);
        if(StringUtils.hasText(mailPwd) && StringUtils.hasText(mailUser)) {
        	mailSender.setPassword(mailPwd);
            mailSender.setUsername(mailUser);	
        }

        Properties props = new Properties();
        props.load(mailProps.getInputStream());
        mailSender.setJavaMailProperties(props);

    }

    public void sendEmail(String email, String template, String lang, Map<String, Object> vars)
            throws MessagingException {

        logger.debug("send mail for " + String.valueOf(template) + " to " + String.valueOf(email));
        if (logger.isTraceEnabled()) {
            logger.trace("mail vars {}", vars.toString());
        }

        final Context ctx = new Context();
        Locale locale = lang != null ? Locale.forLanguageTag(lang) : Locale.forLanguageTag(appProps.getLang());
        Realm realm = new Realm("", appProps.getName());

        // build logo path
        String applicationLogo = appProps.getUrl() + "/logo";

        // set app context
        Map<String, String> application = new HashMap<>();
        application.put("name", appProps.getName());
        application.put("url", appProps.getUrl());
        application.put("logo", applicationLogo);
        application.put("email", appProps.getEmail());
        ctx.setVariable("application", application);

        if (logger.isTraceEnabled()) {
            logger.trace("application context {}", application.toString());
        }

        // set template context
        String templatePath = "mail/messages/" + template + "_" + locale.getLanguage();
        ctx.setVariable("template", templatePath);

        String subjectKey = "mail." + template + ".subject";
        String subject = messageSource.getMessage(subjectKey, null, locale);

        ctx.setVariable("subject", subject);

        // note: we let template override app context vars
        if (vars != null) {
            for (String var : vars.keySet()) {
                ctx.setVariable(var, vars.get(var));
            }

            // check if realm context available
            Object slug = vars.get("realm");
            if (slug != null) {
                realm = realmService.findRealm((String) slug);
            }
        }

        // set realm context
        ctx.setVariable("realm", realm);

        // build message
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        String subjectText = realm.getName() + ": " + subject;
        message.setSubject(subjectText);
        try {
            message.setFrom(mailUser, appProps.getEmail());
        } catch (UnsupportedEncodingException | MessagingException e) {
            throw new MessagingException("invalid-mail-sender");
        }
        message.setReplyTo(appProps.getEmail());
        message.setTo(email);

        // render html mail from base template and message
        String htmlContent = this.templateEngine.process("mail/template", ctx);
        message.setText(htmlContent, true);
//        if (logger.isTraceEnabled()) {
//            logger.trace("send mail for " + String.valueOf(template) + " content:\n " + htmlContent);
//        }

        // send
        mailSender.send(mimeMessage);
    }
}