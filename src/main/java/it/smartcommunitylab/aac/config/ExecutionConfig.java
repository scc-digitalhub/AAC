package it.smartcommunitylab.aac.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import it.smartcommunitylab.aac.claims.LocalGraalExecutionService;

@Configuration
@Order(9)
public class ExecutionConfig {

    @Bean
    public LocalGraalExecutionService localGraalExecutionService() {
        return new LocalGraalExecutionService();
    }

}
