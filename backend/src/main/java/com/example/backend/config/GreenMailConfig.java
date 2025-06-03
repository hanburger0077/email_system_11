package com.example.backend.config;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GreenMailConfig {



    @Bean(initMethod = "start", destroyMethod = "stop")
    public GreenMail greenMail() {
        return new GreenMail(ServerSetup.SMTP)
                .withConfiguration(GreenMailConfiguration.aConfig()
                        .withUser("admin@example.com", "123456"));
    }
}