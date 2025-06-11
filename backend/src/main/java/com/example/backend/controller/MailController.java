package com.example.backend.controller;

import com.example.backend.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

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

    @PostMapping("/send")
    public String sendMail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content,
            @RequestParam(required = false) List<MultipartFile> attachmentFiles) {
        if (attachmentFiles == null || attachmentFiles.isEmpty()) {
            attachmentFiles = null;
        } else {
            System.out.println("Attachments: " + (attachmentFiles != null ? attachmentFiles.size() : 0));
            System.out.println(attachmentFiles.get(0).getOriginalFilename());
        }
        System.out.println("To: " + to);
        System.out.println("Subject: " + subject);
        System.out.println("Content: " + content);
        return mailService.sendMail(to, subject, content, attachmentFiles);
    }

    // 获取单封邮件
    @GetMapping("/{mailbox}/mails/{mailId}")
    public String fetchMail(
            @PathVariable long mailId,
            @PathVariable String mailbox) {
        return mailService.fetchMail(mailId, mailbox);
    }

    // 查看邮箱内容
    @GetMapping("/{mailbox}/pages/{pageNum}")
    public String viewMailbox(
            @PathVariable String mailbox,
            @PathVariable int pageNum) {
        return mailService.viewMail(mailbox, pageNum, pageSize);
    }

    // 搜索邮件
    @GetMapping("/{mailbox}/pages/{pageNum}/search")
    public String searchMailbox(
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
    @GetMapping("/{mailbox}/mails/{mailId}/change/{sign}/{op}")
    public RedirectView changeMail(
            @PathVariable String mailbox,
            @PathVariable long mailId,
            @PathVariable String sign,
            @PathVariable String op) {
        mailService.changeMail(mailId, sign, op);
        if (sign.equals("TRASH") || sign.equals("READ") && op.equals("-FLAG")) {
            return new RedirectView("/api/mail/" + mailbox + "/pages/1");
        } else {
            return new RedirectView("/api/mail/" + mailbox + "/mails/" + mailId);
        }
    }


    // 删除邮件
    @GetMapping("/{mailbox}/mails/{mailId}/delete")
    public RedirectView deleteMail(
            @PathVariable String mailbox,
            @PathVariable long mailId) {
        mailService.deleteMail(mailId);
        return new RedirectView("/api/mail/" + mailbox + "/pages/1");
    }


    // 新建草稿
    @PostMapping("/drafts")
    public String newDraft(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content,
            @RequestParam(required = false) String attachmentName,
            @RequestParam(required = false) byte[] attachmentContent) {
        mailService.draft(0, to, subject, content);
        return "Draft save successfully";
    }


    // 修改草稿
    @PostMapping("/drafts/{mailId}")
    public String updateDraft(
            @PathVariable long mailId,
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content,
            @RequestParam(required = false) String attachmentName,
            @RequestParam(required = false) byte[] attachmentContent) {
        mailService.draft(mailId, to, subject, content);
        return "Draft update successfully";
    }
}