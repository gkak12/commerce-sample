package com.commerce.point;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PointApplication {

    public static void main(String[] args) {
        SpringApplication.run(PointApplication.class, args);
    }
}
