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

package it.smartcommunitylab.aac.auth.cie;

import it.smartcommunitylab.aac.dto.AccountProfile;

/**
 * Interface for User Unique identities to be used for 2factor confirmation.
 * @author raman
 *
 */
public interface IdentitySource {

	/**
	 * Find user identity string for specific provider, userId and profile data
	 * @param provider
	 * @param userId
	 * @param profile
	 * @return
	 */
	String getUserIdentity(String provider, Long userId, AccountProfile profile);
}
