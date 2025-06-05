package com.example.backend.mapper;


import com.example.backend.entity.Mail;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;


@Mapper
public interface MailMapper {
    //加入新邮件
    void insertMail(Mail mail);

    //根据接收用户id返回邮件信息
    List<Mail> selectByUserId(long userId, int pageSize, int offset);

    //根据发送用户id返回邮件信息
    List<Mail> selectBySenderId(long userId, int pageSize, int offset);

    //根据邮件id返回邮件信息
    Mail selectByMailId(long mailId);
}
