package com.example.backend.dao;

import com.example.backend.entity.Mail;
import org.apache.ibatis.annotations.Mapper;



@Mapper
public interface MailDao {
    void insertMail(Mail mail);
}
