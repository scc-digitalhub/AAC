package it.smartcommunitylab.aac.config;

import it.smartcommunitylab.aac.claims.LocalGraalExecutionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(9)
public class ExecutionConfig {

    @Bean
    public LocalGraalExecutionService localGraalExecutionService() {
        return new LocalGraalExecutionService();
    }
}
