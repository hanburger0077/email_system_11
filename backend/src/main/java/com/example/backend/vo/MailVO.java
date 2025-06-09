package com.example.backend.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MailVO  {
    private long mail_id;
    private long sender_id;
    private long receiver_id;
    private String subject;
    private String content;
    private LocalDateTime create_at;
}
