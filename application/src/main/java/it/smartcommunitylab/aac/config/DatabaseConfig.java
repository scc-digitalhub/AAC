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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.smartcommunitylab.aac.autoconfigure.JdbcDataSourceInitializer;
import it.smartcommunitylab.aac.repository.IsolationSupportHibernateJpaDialect;
import java.beans.PropertyVetoException;
import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
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

    @Bean(name = "jdbcDataSource")
    public HikariDataSource jdbcDataSource(@Qualifier("jdbcProperties") JdbcProperties properties)
        throws PropertyVetoException {
        HikariConfig config = buildDataSourceConfig(properties);
        config.setPoolName("jdbcConnectionPool");

        return new HikariDataSource(config);
    }

    @Primary
    @Bean(name = "jpaDataSource")
    public HikariDataSource jpaDataSource(@Qualifier("jdbcProperties") JdbcProperties properties)
        throws PropertyVetoException {
        HikariConfig config = buildDataSourceConfig(properties);
        config.setPoolName("jpaConnectionPool");

        return new HikariDataSource(config);
    }

    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean getEntityManagerFactory(
        @Qualifier("jdbcProperties") JdbcProperties properties,
        @Qualifier("jpaDataSource") DataSource dataSource
    ) throws PropertyVetoException {
        LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();
        bean.setPersistenceUnitName("aac");
        bean.setDataSource(dataSource);

        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        adapter.setDatabasePlatform(properties.getDialect());
        if (properties.isShowSql()) {
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
    public JpaTransactionManager getTransactionManager(EntityManagerFactory entityManagerFactory)
        throws PropertyVetoException {
        JpaTransactionManager bean = new JpaTransactionManager();
        bean.setEntityManagerFactory(entityManagerFactory);
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

    private HikariConfig buildDataSourceConfig(JdbcProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(properties.getDriver());
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUser());
        config.setPassword(properties.getPassword());

        config.setMaximumPoolSize(properties.getMaxPoolSize());
        config.setMinimumIdle(properties.getMinPoolSize());
        config.setIdleTimeout(properties.getIdleTimeout());
        config.setKeepaliveTime(properties.getKeepAliveTimeout());
        config.setConnectionTimeout(properties.getConnectionTimeout());

        if (properties.getDataSourceProperties() != null) {
            properties.getDataSourceProperties().entrySet().forEach(e -> config.addDataSourceProperty(e.getKey(), e));
        }

        return config;
    }
}
