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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.model.Registration;
import it.smartcommunitylab.aac.model.User;

@Component
public class RegistrationService {

	@Autowired
	private RegistrationManager manager;
	
	public Registration register(String name, String surname, String email, String password, String lang) throws RegistrationException {
		try {
			return manager.register(name, surname, email, password, lang);
		} catch (Exception e) {
			// try to recover from duplicate creation
			return manager.register(name, surname, email, password, lang);
		}
	}
	
	public Registration confirm(String confirmationToken) throws RegistrationException {
		return manager.confirm(confirmationToken);
	}

	public void resendConfirm(String email) throws RegistrationException {
		manager.resendConfirm(email);
	}

	public void resetPassword(String email) throws RegistrationException {
		manager.resetPassword(email);
	}
	
	public void updatePassword(String email, String password) throws RegistrationException {
		manager.updatePassword(email, password);
	}
	
	public User registerOffline(String name, String surname, String email, String password, String lang, boolean changePwdOnFirstAccess, String confirmationKey) throws RegistrationException {
		return manager.registerOffline(name, surname, email, password, lang, changePwdOnFirstAccess, confirmationKey);
	}

	
	public Registration getUser(String email, String password) throws RegistrationException {
		return manager.getUser(email, password); 
	}

}
