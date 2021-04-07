package it.smartcommunitylab.aac.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import it.smartcommunitylab.aac.attributes.store.AutoJdbcAttributeStore;

@Configuration
@Order(4)
public class PersistenceConfig {

    @Autowired
    private DataSource dataSource;

    /*
     * Wire persistence services bound to dataSource
     */

    @Bean
    public AutoJdbcAttributeStore attributeStore() {
        return new AutoJdbcAttributeStore(dataSource);
    }
}
