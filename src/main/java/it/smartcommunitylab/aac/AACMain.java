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

package it.smartcommunitylab.aac;

import it.smartcommunitylab.aac.bootstrap.AACBootstrap;
import it.smartcommunitylab.aac.config.DatabasePropertiesListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AACMain {

    @Autowired
    BuildProperties buildProperties;

    @Autowired
    private AACBootstrap bootstrap;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AACMain.class);
        app.addListeners(new DatabasePropertiesListener());
        app.run(args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            printBanner();
        };
    }

    @Bean
    public CommandLineRunner bootstrapRunner(ApplicationContext ctx) {
        return args -> {
            bootstrap.bootstrap();
            bootstrap.bootstrapConfig();
        };
    }

    public void printBanner() {
        String version = buildProperties.getVersion();
        System.out.println("======================================");
        System.out.println("                    _____            ");
        System.out.println("     /\\       /\\   / ____|          ");
        System.out.println("    /  \\     /  \\ | |              ");
        System.out.println("   / /\\ \\   / /\\ \\| |            ");
        System.out.println("  / ____ \\ / ____ \\ |____          ");
        System.out.println(" /_/    \\_/_/    \\_\\_____|       ");
        System.out.println(" :ready                  (v." + version + ")");
        System.out.println("======================================");
    }
}
