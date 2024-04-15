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

import it.smartcommunitylab.aac.claims.LocalGraalExecutionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(9)
public class ExecutionConfig {

    @Value("${engine.graal.max-cpu-time}")
    private int graalMaxCpuTime;

    @Value("${engine.graal.max-memory}")
    private int graalMaxMemory;

    @Value("${engine.graal.remove-comments}")
    private boolean graalRemoveComments;

    @Bean
    public LocalGraalExecutionService localGraalExecutionService() {
        LocalGraalExecutionService executionService = new LocalGraalExecutionService();
        executionService.setMaxMemory(graalMaxMemory);
        executionService.setMaxCpuTime(graalMaxCpuTime);
        executionService.setRemoveComments(graalRemoveComments);
        return executionService;
    }
}
