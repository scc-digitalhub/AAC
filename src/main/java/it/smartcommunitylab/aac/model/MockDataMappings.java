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

package it.smartcommunitylab.aac.model;

import java.util.HashMap;
import java.util.List;

/**
 * Mock data mapping for test purposes. When the identity attributes
 * match the criteria (attribute regex, value regex), the data is updated with mock values
 * before being passed to the authentication
 *  
 * @author raman
 *
 */
public class MockDataMappings {
	private List<MockDataMapping> mappings;

	public List<MockDataMapping> getMappings() {
		return mappings;
	}
	public void setMappings(List<MockDataMapping> mappings) {
		this.mappings = mappings;
	}

	public static class MockDataMapping {
		private String attribute, value;
		private HashMap<String,String> mock;
		
		public String getAttribute() {
			return attribute;
		}
		public void setAttribute(String attribute) {
			this.attribute = attribute;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
		public HashMap<String, String> getMock() {
			return mock;
		}
		public void setMock(HashMap<String, String> mock) {
			this.mock = mock;
		}
	}
	
}
