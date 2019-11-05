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

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import it.smartcommunitylab.aac.common.RegistrationException;

/**
 * @author raman
 *
 */
@Component
public class MailSender {
    private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private static JavaMailSenderImpl mailSender = null;
    
	@Value("${mail.username}")
	private String mailUser;
	@Autowired @Value("${mail.password}")
	private String mailPwd;
	@Autowired @Value("${mail.host}")
	private String mailHost;
	@Autowired @Value("${mail.port}")
	private Integer mailPort;
	@Autowired @Value("${mail.protocol}")
	private String mailProtocol;

	@Value("classpath:/javamail.properties")
	private Resource mailProps;
		
	@Autowired
	private TemplateEngine templateEngine;

    public MailSender() throws IOException {
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
    
	public void sendEmail(String email, String template, String subject, Map<String,Object> vars) throws RegistrationException {
		
		try {
			final Context ctx = new Context();
			if (vars != null) {
				for (String var : vars.keySet()) {
					ctx.setVariable(var, vars.get(var));
				}
			}
			
			final MimeMessage mimeMessage = mailSender.createMimeMessage();
			final MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true, "UTF-8"); 
			message.setSubject(subject);
			message.setFrom(mailUser);
			message.setTo(email);

			// Create the HTML body using Thymeleaf
			// TODO use some template engine
			final String htmlContent = this.templateEngine.process(template, ctx);
			message.setText(htmlContent, true);
			// Send mail
			mailSender.send(mimeMessage);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new RegistrationException(e);
		}
	}
}
