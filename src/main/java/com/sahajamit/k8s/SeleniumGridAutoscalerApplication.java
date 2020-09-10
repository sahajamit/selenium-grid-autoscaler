package com.sahajamit.k8s;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SeleniumGridAutoscalerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeleniumGridAutoscalerApplication.class, args);
    }
}
