package com.example.backend;


import com.example.backend.entity.Mail;
import com.example.backend.service.MailService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@SpringBootApplication
@MapperScan(basePackages = "com.example.backend.dao", annotationClass = org.apache.ibatis.annotations.Mapper.class)
@EnableAutoConfiguration(exclude = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
})
public class MailTest {

    public static void main(String[] args) {
        SpringApplication.run(MailTest.class, args);
    }

    @Bean
    public CommandLineRunner run(MailService mailService) {
        return args -> {
            // 创建并插入一个测试邮件
            Mail mail = new Mail();
            mail.setSender_id(1L);  // 确保 senderId 在 user 表中存在
            mail.setReceiver_id(2L); // 确保 receiverId 在 user 表中存在
            mail.setSubject("Test Subject");
            mail.setContent("Test Content");
            mail.setCreate_at(LocalDateTime.now());

            mailService.createMail(mail);

            System.out.println("Mail created successfully!");
        };
    }
}
