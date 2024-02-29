/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import it.smartcommunitylab.aac.files.store.AutoJdbcFileStore;
import it.smartcommunitylab.aac.files.store.FileStore;
import it.smartcommunitylab.aac.files.store.LocalFileStore;

@Configuration
@Order(2)
public class FileStoreConfig {

    @Autowired
    @Qualifier("jdbcDataSource")
    private DataSource jdbcDataSource;
    
    @Value("${persistence.files.store}")
    private String fileStoreType;
    
    @Value("${persistence.files.path}")
	private String basePath;

    @Bean
    public FileStore fileStoreService() {
    	if ("jdbc".equals(fileStoreType)) {
    		return new AutoJdbcFileStore(jdbcDataSource);
    	} else if ("filesystem".equals(fileStoreType)) {
    		LocalFileStore fileStore = new  LocalFileStore();
    		fileStore.setBasePath(basePath);
    		return fileStore;
    	}
    	throw new IllegalArgumentException();    	
    }
   
}
