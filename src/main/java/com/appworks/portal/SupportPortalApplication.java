package com.appworks.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SupportPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportPortalApplication.class, args);
    }
}
