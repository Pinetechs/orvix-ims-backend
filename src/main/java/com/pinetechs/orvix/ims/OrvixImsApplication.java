package com.pinetechs.orvix.ims;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling

@SpringBootApplication
public class OrvixImsApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrvixImsApplication.class, args);
    }
}
