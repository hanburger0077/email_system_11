package com.example.backend.controller;

import com.example.backend.DTO.MailDTO;
import com.example.backend.service.MailService;
import com.example.backend.utils.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/mail")
public class MailController {

    @Autowired
    private MailService mailService;

    private final int pageSize = 10;

    // SSE 端点 - 用于实时邮件通知
    @GetMapping(path = "/sse/emails", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEmails() {
        return mailService.streamEmails();
    }


    @PostMapping("/connect")
    public ResultVo loginIMAP(
            @RequestParam String username,
            @RequestParam String password) {
        return mailService.loginIMAP(username, password);
    }


    @PostMapping("/send")
    public ResultVo sendMail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content,
            @RequestParam(required = false) List<MultipartFile> attachmentFiles) {
        if (attachmentFiles == null || attachmentFiles.isEmpty()) {
            attachmentFiles = null;
        }
        return mailService.sendMail(to, subject, content, attachmentFiles);
    }

    // 获取单封邮件
    @GetMapping("/{mailbox}/mails/{mailId}")
    public ResultVo fetchMail(
            @PathVariable long mailId,
            @PathVariable String mailbox) {
        return mailService.fetchMail(mailId, mailbox);
    }

    // 查看邮箱内容
    @GetMapping("/{mailbox}/pages/{pageNum}")
    public ResultVo viewMailbox(
            @PathVariable String mailbox,
            @PathVariable int pageNum) {
        return mailService.viewMail(mailbox, pageNum, pageSize);
    }

    // 搜索邮件
    @GetMapping("/search")
    public ResultVo searchMailbox(
            @RequestParam String keywords) {
        ResultVo<List<MailDTO>> mail1 = mailService.searchMail("SENT", null, null, keywords, null, 0, null, false, false);
        ResultVo<List<MailDTO>> mail2 = mailService.searchMail("SENT", null, null, null, keywords, 0, null, false, false);
        ResultVo<List<MailDTO>> mail3 = mailService.searchMail("INBOX", null, null, keywords, null, 0, null, false, false);
        ResultVo<List<MailDTO>> mail4 = mailService.searchMail("INBOX", null, null, null, keywords, 0, null, false, false);
        ResultVo<List<MailDTO>> mail5 = mailService.searchMail("DRAFT", null, null, keywords, null, 0, null, false, false);
        ResultVo<List<MailDTO>> mail6 = mailService.searchMail("DRAFT", null, null, null, keywords, 0, null, false, false);
        List<MailDTO> mailDTO = new ArrayList<>();
        int count = 0;
        if(mail1.getCode().equals("code.ok")) {
            mailDTO.addAll(mail1.getData());
            count++;
        }
        if(mail2.getCode().equals("code.ok")) {
            mailDTO.addAll(mail2.getData());
            count++;
        }
        if(mail3.getCode().equals("code.ok")) {
            mailDTO.addAll(mail3.getData());
            count++;
        }
        if(mail4.getCode().equals("code.ok")) {
            mailDTO.addAll(mail4.getData());
            count++;
        }
        if(mail5.getCode().equals("code.ok")) {
            mailDTO.addAll(mail5.getData());
            count++;
        }
        if(mail6.getCode().equals("code.ok")) {
            mailDTO.addAll(mail6.getData());
            count++;
        }
        Set<MailDTO> set = new LinkedHashSet<>(mailDTO);
        mailDTO = new ArrayList<>(set);
        for(MailDTO m:mailDTO){
            System.out.println(m.getMailbox());
        }
        if(count == 0) {
            return mail1;
        } else {
            return ResultVo.success("操作成功", mailDTO);
        }
    }


    // 搜索邮件
    @GetMapping("/{mailbox}/pages/{pageNum}/search")
    public ResultVo searchMailbox(
            @PathVariable String mailbox,
            @PathVariable int pageNum,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String body,
            @RequestParam(defaultValue = "0") int since,
            @RequestParam(required = false) String unseen,
            @RequestParam(defaultValue = "false") boolean sender_star,
            @RequestParam(defaultValue = "false") boolean receiver_star) {
        return mailService.searchMail(mailbox, pageNum, pageSize, from, to, subject, body, since, unseen, sender_star, receiver_star);
    }



    // 修改邮件状态（标记、移动等）
    @PostMapping("/{mailbox}/mails/{mailId}/change/{sign}/{op}")
    public ResultVo changeMail(
            @PathVariable String mailbox,
            @PathVariable long mailId,
            @PathVariable String sign,
            @PathVariable String op) {
        return mailService.changeMail(mailId, sign, op);
    }


    // 删除邮件
    @DeleteMapping("/{mailbox}/mails/{mailId}/delete")
    public ResultVo deleteMail(
            @PathVariable String mailbox,
            @PathVariable long mailId) {
        return mailService.deleteMail(mailId);
    }


    // 新建草稿
    @PostMapping("/drafts")
    public ResultVo newDraft(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content) {
        return mailService.draft(0, to, subject, content);
    }


    // 修改草稿
    @PutMapping("/drafts/{mailId}")
    public ResultVo updateDraft(
            @PathVariable long mailId,
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content) {
        return mailService.draft(mailId, to, subject, content);
    }


    // 断开连接
    @PostMapping("/disconnect")
    public ResultVo disconnect() {
        return mailService.disconnect();
    }
}