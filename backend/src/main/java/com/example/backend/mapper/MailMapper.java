package com.example.backend.mapper;

import com.example.backend.entity.Mail;
import org.apache.ibatis.annotations.Mapper;



@Mapper
public interface MailMapper {
    void insertMail(Mail mail);
}
