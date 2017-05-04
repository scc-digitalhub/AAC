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

package it.smartcommunitylab.aac.oauth;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import it.smartcommunitylab.aac.model.MockDataMappings;
import it.smartcommunitylab.aac.model.MockDataMappings.MockDataMapping;

/**
 * @author raman
 *
 */
public class MockDataAwareOAuth2SuccessHandler extends ExtOAuth2SuccessHandler {

	private Map<String,List<MockDataMapping>> mappings;

	/**
	 * @param defaultTargetUrl
	 * @param data 
	 */
	public MockDataAwareOAuth2SuccessHandler(String defaultTargetUrl, MockDataMappings data) {
		super(defaultTargetUrl);
		this.mappings = new HashMap<>();
		if (data != null) {
			data.getMappings().forEach(m -> {
				List<MockDataMapping> list = mappings.getOrDefault(m.getAttribute(), new LinkedList<>());
				list.add(m);
				mappings.put(m.getAttribute(), list);
			});
		}
	}

	@Override
	protected Map<String, Object> preprocess(Map<String, Object> details) {
		Map<String, Object> map = super.preprocess(details);
		mappings.forEach((a,v) -> {
			if (map.containsKey(a)) {
				v.forEach(m -> {
					if (map.get(a) != null && map.get(a).toString().matches(m.getValue())) {
						map.putAll(m.getMock());
					}
				});
			}
		});
		return map;
	}

	
}
