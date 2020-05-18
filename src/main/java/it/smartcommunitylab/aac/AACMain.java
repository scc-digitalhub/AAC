package it.smartcommunitylab.aac;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.velocity.VelocityAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

//disable velocity auto configuration, tries to log to deprecated library
@SpringBootApplication(exclude = VelocityAutoConfiguration.class)
public class AACMain {
    
    @Autowired
    BuildProperties buildProperties;
    
    public static void main(String[] args) {
        SpringApplication.run(AACMain.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            printBanner();
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
        System.out.println(" :ready                  (v."+version+")");
        System.out.println("======================================");
    }
}
