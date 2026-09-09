package com.meethybridhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;


@SpringBootApplication
@EnableCaching
public class MeethybridHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeethybridHubApplication.class, args);
    }
}
