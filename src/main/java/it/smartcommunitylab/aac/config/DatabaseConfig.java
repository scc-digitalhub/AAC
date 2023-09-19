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

import com.zaxxer.hikari.HikariDataSource;
import it.smartcommunitylab.aac.autoconfigure.JdbcDataSourceInitializer;
import it.smartcommunitylab.aac.repository.IsolationSupportHibernateJpaDialect;
import java.beans.PropertyVetoException;
import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database config is @1, we need dataSources to bootstrap
 *
 * @author raman
 *
 */
@Configuration
@Order(1)
@EnableTransactionManagement
@EntityScan(
    basePackages = {
        "it.smartcommunitylab.aac.accounts.persistence",
        "it.smartcommunitylab.aac.attributes.persistence",
        "it.smartcommunitylab.aac.clients.persistence",
        "it.smartcommunitylab.aac.core.persistence",
        "it.smartcommunitylab.aac.credentials.persistence",
        "it.smartcommunitylab.aac.groups.persistence",
        "it.smartcommunitylab.aac.internal.persistence",
        "it.smartcommunitylab.aac.oauth.persistence",
        "it.smartcommunitylab.aac.oidc.persistence",
        "it.smartcommunitylab.aac.password.persistence",
        "it.smartcommunitylab.aac.realms.persistence",
        "it.smartcommunitylab.aac.roles.persistence",
        "it.smartcommunitylab.aac.saml.persistence",
        "it.smartcommunitylab.aac.services.persistence",
        "it.smartcommunitylab.aac.templates.persistence",
        "it.smartcommunitylab.aac.users.persistence",
        "it.smartcommunitylab.aac.webauthn.persistence",
    }
)
@EnableJpaRepositories(
    basePackages = {
        "it.smartcommunitylab.aac.accounts.persistence",
        "it.smartcommunitylab.aac.attributes.persistence",
        "it.smartcommunitylab.aac.clients.persistence",
        "it.smartcommunitylab.aac.core.persistence",
        "it.smartcommunitylab.aac.credentials.persistence",
        "it.smartcommunitylab.aac.groups.persistence",
        "it.smartcommunitylab.aac.internal.persistence",
        "it.smartcommunitylab.aac.oauth.persistence",
        "it.smartcommunitylab.aac.oidc.persistence",
        "it.smartcommunitylab.aac.password.persistence",
        "it.smartcommunitylab.aac.realms.persistence",
        "it.smartcommunitylab.aac.roles.persistence",
        "it.smartcommunitylab.aac.saml.persistence",
        "it.smartcommunitylab.aac.services.persistence",
        "it.smartcommunitylab.aac.templates.persistence",
        "it.smartcommunitylab.aac.users.persistence",
        "it.smartcommunitylab.aac.webauthn.persistence",
        "it.smartcommunitylab.aac.repository",
    },
    queryLookupStrategy = QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND
)
@EnableJpaAuditing
public class DatabaseConfig {

    @Autowired
    private Environment env;

    //    @Bean
    //    public ComboPooledDataSource getDataSource() throws PropertyVetoException {
    //        ComboPooledDataSource bean = new ComboPooledDataSource();
    //
    //        bean.setDriverClass(env.getProperty("jdbc.driver"));
    //        bean.setJdbcUrl(env.getProperty("jdbc.url"));
    //        bean.setUser(env.getProperty("jdbc.user"));
    //        bean.setPassword(env.getProperty("jdbc.password"));
    //        bean.setAcquireIncrement(5);
    //        bean.setIdleConnectionTestPeriod(60);
    //        bean.setMaxPoolSize(100);
    //        bean.setMaxStatements(50);
    //        bean.setMinPoolSize(10);
    //
    //        return bean;
    //    }

    @Bean(name = "jdbcDataSource")
    public HikariDataSource jdbcDataSource() throws PropertyVetoException {
        HikariDataSource bean = new HikariDataSource();

        bean.setDriverClassName(env.getProperty("jdbc.driver"));
        bean.setJdbcUrl(env.getProperty("jdbc.url"));
        bean.setUsername(env.getProperty("jdbc.user"));
        bean.setPassword(env.getProperty("jdbc.password"));

        //
        //        bean.setAcquireIncrement(5);
        //        bean.setIdleConnectionTestPeriod(60);
        //        bean.setMaxPoolSize(100);
        //        bean.setMaxStatements(50);
        bean.setMinimumIdle(10);

        return bean;
    }

    @Primary
    @Bean(name = "jpaDataSource")
    public HikariDataSource jpaDataSource() throws PropertyVetoException {
        HikariDataSource bean = new HikariDataSource();

        bean.setDriverClassName(env.getProperty("jdbc.driver"));
        bean.setJdbcUrl(env.getProperty("jdbc.url"));
        bean.setUsername(env.getProperty("jdbc.user"));
        bean.setPassword(env.getProperty("jdbc.password"));

        //
        //        bean.setAcquireIncrement(5);
        //        bean.setIdleConnectionTestPeriod(60);
        //        bean.setMaxPoolSize(100);
        //        bean.setMaxStatements(50);
        bean.setMinimumIdle(10);

        return bean;
    }

    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean getEntityManagerFactory() throws PropertyVetoException {
        LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();
        bean.setPersistenceUnitName("aac");
        bean.setDataSource(jpaDataSource());

        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setDatabasePlatform(env.getProperty("jdbc.dialect"));
        if (Boolean.parseBoolean(env.getProperty("jdbc.show-sql", "false"))) {
            adapter.setShowSql(true);
        }
        adapter.setGenerateDdl(true);
        bean.setJpaVendorAdapter(adapter);

        bean.setJpaDialect(new IsolationSupportHibernateJpaDialect());

        Properties props = new Properties();
        props.setProperty("hibernate.hbm2ddl.auto", "update");
        bean.setJpaProperties(props);

        // explicitly mark packages for persistence unit
        // TODO rework, align with @EntityScan
        // spring boot 2.x should fix the issue
        bean.setPackagesToScan(
            "it.smartcommunitylab.aac.accounts.persistence",
            "it.smartcommunitylab.aac.attributes.persistence",
            "it.smartcommunitylab.aac.clients.persistence",
            "it.smartcommunitylab.aac.core.persistence",
            "it.smartcommunitylab.aac.credentials.persistence",
            "it.smartcommunitylab.aac.groups.persistence",
            "it.smartcommunitylab.aac.internal.persistence",
            "it.smartcommunitylab.aac.oauth.persistence",
            "it.smartcommunitylab.aac.oidc.persistence",
            "it.smartcommunitylab.aac.password.persistence",
            "it.smartcommunitylab.aac.realms.persistence",
            "it.smartcommunitylab.aac.roles.persistence",
            "it.smartcommunitylab.aac.saml.persistence",
            "it.smartcommunitylab.aac.services.persistence",
            "it.smartcommunitylab.aac.templates.persistence",
            "it.smartcommunitylab.aac.users.persistence",
            "it.smartcommunitylab.aac.webauthn.persistence"
        );
        //		bean.setPersistenceUnitManager(null);

        return bean;
    }

    @Bean(name = "transactionManager")
    public JpaTransactionManager getTransactionManager() throws PropertyVetoException {
        JpaTransactionManager bean = new JpaTransactionManager();
        bean.setEntityManagerFactory(getEntityManagerFactory().getObject()); // ???
        return bean;
    }

    @Bean(name = "coreJdbcDataSourceInitializer")
    public JdbcDataSourceInitializer jdbcDataSourceInitializer(
        @Qualifier("jdbcDataSource") DataSource dataSource,
        JdbcProperties properties
    ) {
        return new JdbcDataSourceInitializer(dataSource, properties);
    }

    @Bean(name = "oauth2JdbcDataSourceInitializer")
    public JdbcDataSourceInitializer oauth2DataSourceInitializer(
        @Qualifier("jdbcDataSource") DataSource dataSource,
        JdbcProperties properties
    ) {
        return new JdbcDataSourceInitializer(dataSource, properties, "classpath:db/sql/oauth2/schema-@@platform@@.sql");
    }

    //TODO add optional ObjectProvider<DataSource> to support separated dataSource for audit
    @Bean(name = "auditJdbcDataSourceInitializer")
    public JdbcDataSourceInitializer auditDataSourceInitializer(
        @Qualifier("jdbcDataSource") DataSource dataSource,
        JdbcProperties properties
    ) {
        return new JdbcDataSourceInitializer(dataSource, properties, "classpath:db/sql/audit/schema-@@platform@@.sql");
    }
}
