package com.example.backend.mapper;


import com.example.backend.entity.Mail;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;


@Mapper
public interface MailMapper {
    //加入新邮件
    void insertMail(Mail mail);

    // 根据用户id以及其他状态选择邮件
    // 实际先区分为接收方还是发送方
    // 接收方有 INBOX:0/TRASH:1/JUNK:2
    // 发送方有 SENT:0/DRAFT:1

    // 用户作为接收方
    List<Long> selectByReceiverIdWithSign(long userId, long senderId, String subject, String body,LocalDateTime since, short receiver_sign, String read, String receiver_star);

    // 用户作为发送方
    List<Long> selectBySenderIdWithSign(long userId, long receiverId, String subject, String body, LocalDateTime since, short sender_sign, String sender_star);

    int countBySenderIdWithSign(long userId, short sender_sign);

    int countByReceiverIdWithSign(long userId, short receiver_sign);

    //根据邮件id返回邮件信息
    Mail selectByMailId(long mailId);

    //修改邮件状态
    void changeState(long mailId, String sign, String op);
}
