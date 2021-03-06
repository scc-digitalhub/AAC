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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

import it.smartcommunitylab.aac.dto.AccountProfile;

/**
 * @author raman
 *
 */
public class FileEmailIdentitySource implements IdentitySource {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	@Value("${security.identity.source}")
	private Resource source;
	
	@Override
	public String getUserIdentity(String provider, Long userId, AccountProfile profile) {
		try {
			List<String[]> list = streamToList(source.getInputStream());
			for (Map<String, String> accountData : profile.getAccounts().values()) {
				for (String key : accountData.keySet()) {
					if (key.toLowerCase().indexOf("email") >= 0) {
						Optional<String[]> res = findRow(list, provider, accountData.get(key));
						if (res.isPresent()) {
							return res.get()[2];
						}
					}
				}
			}
		} catch (IOException e) {
		}
		return null;
	}

	private Optional<String[]> findRow(List<String[]> list, String provider, String key) throws IOException {
            return list.stream().filter(arr -> arr.length == 3 && arr[0].equals(provider) && arr[1].equals(key)).findFirst();
	}
	
	private List<String[]> streamToList(InputStream is) throws IOException {
		try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
            return buffer.lines().map(l -> l.split(",")).collect(Collectors.toList());
        }
	}
}
