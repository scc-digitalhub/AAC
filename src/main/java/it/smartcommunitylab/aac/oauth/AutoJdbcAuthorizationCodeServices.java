/**
 *    Copyright 2012-2013 Trento RISE
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
 */

package it.smartcommunitylab.aac.oauth;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.provider.code.JdbcAuthorizationCodeServices;

/**
 * Authorization code services with DB table creation on startup.
 * 
 * @see {@link JdbcAuthorizationCodeServices}
 * @author raman
 *
 */
public class AutoJdbcAuthorizationCodeServices extends JdbcAuthorizationCodeServices {

	private JdbcTemplate jdbcTemplate;
	
	private static final String DEFAULT_CREATE_CODE_TABLE_STATEMENT = "CREATE TABLE IF NOT EXISTS oauth_code (code VARCHAR(256), authentication BLOB);";

	private String createStatement = DEFAULT_CREATE_CODE_TABLE_STATEMENT; 
	
	/**
	 * @param dataSource
	 */
	public AutoJdbcAuthorizationCodeServices(DataSource dataSource) {
		super(dataSource);
		initSchema(dataSource);
	}

	/**
	 * @param dataSource
	 * @param createStatement
	 */
	public AutoJdbcAuthorizationCodeServices(DataSource dataSource, String createStatement) {
		super(dataSource);
		this.createStatement = createStatement;
		initSchema(dataSource);
	}


	protected void initSchema(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute(createStatement);
	}
}
