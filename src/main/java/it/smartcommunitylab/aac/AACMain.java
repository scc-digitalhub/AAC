package it.smartcommunitylab.aac;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AACMain {

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
        System.out.println("======================================");
        System.out.println(" ____                _                ");
        System.out.println("|  _ \\ ___  __ _  __| |_   _          ");
        System.out.println("| |_) / _ \\/ _` |/ _` | | | |         ");
        System.out.println("|  _ <  __/ (_| | (_| | |_| |_        ");
        System.out.println("|_| \\_\\___|\\__,_|\\__,_|\\__, (_)       ");
        System.out.println(" :AAC AuthN_AuthZ      |___/          ");
        System.out.println("======================================");
    }
}
