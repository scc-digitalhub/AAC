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
package it.smartcommunitylab.aac.config;

import java.beans.PropertyVetoException;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import it.smartcommunitylab.aac.oauth.IsolationSupportHibernateJpaDialect;

/**
 * @author raman
 *
 */
@Configuration 
@EntityScan({"it.smartcommunitylab.aac.model", "it.smartcommunitylab.aac.profile.model"})
@EnableTransactionManagement
@EnableSpringDataWebSupport
@EnableJpaRepositories(basePackages = {"it.smartcommunitylab.aac"}, queryLookupStrategy = QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND)
public class DatabaseConfig {

	@Autowired
	private Environment env;
	

	@Bean
	public ComboPooledDataSource getDataSource() throws PropertyVetoException {
		ComboPooledDataSource bean = new ComboPooledDataSource();
		
		bean.setDriverClass(env.getProperty("jdbc.driver"));
		bean.setJdbcUrl(env.getProperty("jdbc.url"));
		bean.setUser(env.getProperty("jdbc.user"));
		bean.setPassword(env.getProperty("jdbc.password"));
		bean.setAcquireIncrement(5);
		bean.setIdleConnectionTestPeriod(60);
		bean.setMaxPoolSize(100);
		bean.setMaxStatements(50);
		bean.setMinPoolSize(10);
		
		return bean;
	}

	@Bean(name="entityManagerFactory")
	public LocalContainerEntityManagerFactoryBean getEntityManagerFactoryBean() throws PropertyVetoException {
		LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();
		bean.setPersistenceUnitName("aac");
		bean.setDataSource(getDataSource());
		
		HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
		adapter.setDatabasePlatform(env.getProperty("jdbc.dialect"));
		adapter.setShowSql(true);
		adapter.setGenerateDdl(true);
		bean.setJpaVendorAdapter(adapter);
		
		bean.setJpaDialect(new IsolationSupportHibernateJpaDialect());
		
		Properties props = new Properties();
		props.setProperty("hibernate.hbm2ddl.auto", "update");
		bean.setJpaProperties(props);
		
//		bean.setPackagesToScan("it.smartcommunitylab.aac.model", "it.smartcommunitylab.aac.profile.model");
//		bean.setPersistenceUnitManager(null);
		
		return bean;
	}
	
	@Bean(name="transactionManager")
	public JpaTransactionManager getTransactionManager() throws PropertyVetoException {
		JpaTransactionManager bean = new JpaTransactionManager();
//		bean.setEntityManagerFactory(getEntityManagerFactoryBean().getNativeEntityManagerFactory()); // ???
		bean.setEntityManagerFactory(getEntityManagerFactoryBean().getObject()); // ???
		return bean;
	}	
	
}
