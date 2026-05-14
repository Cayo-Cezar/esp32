package com.grainscale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GrainScaleApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrainScaleApplication.class, args);
    }
}
