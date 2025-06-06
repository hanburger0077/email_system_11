package com.example.backend.Server.Config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Data
@Component
public class ServerPortConfig {

    @Value("${server.smtp.port}")
    private int smtpPort;

    @Value("${server.imap.port}")
    private int imapPort;
}