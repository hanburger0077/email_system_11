package com.example.backend;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import jakarta.mail.Message;
import com.example.backend.service.MailService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;


@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class SendTest {

    public static void main(String[] args) {
        SpringApplication.run(SendTest.class, args);
    }

    @Bean
    public CommandLineRunner run(MailService mailService) {
        return args -> {


            // 启动 GreenMail 服务器（监听 SMTP 端口）
            GreenMail greenMail = new GreenMail(new ServerSetup(3025, "localhost", "smtp"));
            greenMail.setUser("admin@example.com","123456");
            greenMail.start();
            Message[] receivedMessages;
            while(true) {

                // 模拟等待一段时间，让邮件发送完成（实际中可以通过轮询或事件监听）
                Thread.sleep(5000);
                // 获取接收到的邮件
                receivedMessages = greenMail.getReceivedMessages();
                if (receivedMessages.length > 0) {
                    Message receivedMessage;
                    for(int i=0;i<receivedMessages.length;i++){
                        receivedMessage = receivedMessages[0];
                        System.out.println("Parsed Mail:"+receivedMessages.length);
                        mailService.parseMail(receivedMessage);
                    }
                    System.out.println("Parsed Mail Successfully!");
                    greenMail.purgeEmailFromAllMailboxes();
                } else {
                    System.out.println("No emails received.");
                }
            }
        };
    }
}

