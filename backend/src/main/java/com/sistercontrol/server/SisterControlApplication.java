package com.sistercontrol.server;

import com.sistercontrol.server.config.SisterControlProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SisterControlProperties.class)
public class SisterControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(SisterControlApplication.class, args);
    }
}
