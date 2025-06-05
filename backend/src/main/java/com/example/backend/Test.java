package com.example.backend;

import com.example.backend.Server.Server;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;


@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@MapperScan("com.example.backend.mapper")
public class Test {

    public static void main(String[] args) {
        SpringApplication.run(Test.class, args);
    }

    @Bean
    public CommandLineRunner startEmailServer(Server emailServer) {
        return args -> {
            try {
                emailServer.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
}
