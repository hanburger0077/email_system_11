package com.example.backend;

import com.example.backend.Server.Server;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@SpringBootApplication
@MapperScan("com.example.backend.mapper")
public class Test {

    public static void main(String[] args) {
        SpringApplication.run(Test.class, args);
    }


    // 注册 BCryptPasswordEncoder 为 Spring Bean
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
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
