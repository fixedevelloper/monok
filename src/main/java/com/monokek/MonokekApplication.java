package com.monokek;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** {@code @EnableScheduling}: required for {@code @Scheduled} (e.g. PrintRetryScheduler) to actually fire — silently ignored otherwise. */
@SpringBootApplication
@EnableScheduling
public class MonokekApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonokekApplication.class, args);
    }
}
