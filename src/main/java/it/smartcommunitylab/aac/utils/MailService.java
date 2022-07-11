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
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

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

    @Value("${application.name}")
    private String applicationName;

    @Value("${application.url}")
    private String applicationUrl;

    @Value("${application.email}")
    private String applicationEmail;

    @Value("classpath:/javamail.properties")
    private org.springframework.core.io.Resource mailProps;

    @Resource(name = "messageSource")
    private MessageSource messageSource;

    @Autowired
    private TemplateEngine templateEngine;

    public MailService() throws IOException {
        mailSender = new org.springframework.mail.javamail.JavaMailSenderImpl();
    }

    @PostConstruct
    public void init() throws IOException {
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setProtocol(mailProtocol);
        mailSender.setPassword(mailPwd);
        mailSender.setUsername(mailUser);

        Properties props = new Properties();
        props.load(mailProps.getInputStream());
        mailSender.setJavaMailProperties(props);

    }

    public MessageSource getMessageSource() {
        return messageSource;
    }

    /**
     * Send email based on specified template
     * 
     * @param email    recepient
     * @param template Thymeleaf template reference
     * @param subject  mail subject
     * @param vars     template variables
     * @throws MessagingException
     */
    public void sendEmail(String email, String template, String subject, Map<String, Object> vars)
            throws MessagingException {

        final Context ctx = new Context();

        // set app context
        Map<String, String> application = new HashMap<>();
        application.put("name", applicationName);
        application.put("url", applicationUrl);
        application.put("email", applicationEmail);
        ctx.setVariable("application", application);

        // set template vars
        // note: we let template override app context vars
        if (vars != null) {
            for (String var : vars.keySet()) {
                ctx.setVariable(var, vars.get(var));
            }
        }

        final MimeMessage mimeMessage = mailSender.createMimeMessage();
        final MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        message.setSubject(subject);
        try {
            message.setFrom(mailUser, applicationEmail);
        } catch (UnsupportedEncodingException | MessagingException e) {
            throw new MessagingException("invalid-mail-sender");
        }
        message.setReplyTo(applicationEmail);
        message.setTo(email);

        // Create the HTML body using Thymeleaf
        final String htmlContent = this.templateEngine.process(template, ctx);
        message.setText(htmlContent, true);
        // Send mail
        mailSender.send(mimeMessage);

    }
}