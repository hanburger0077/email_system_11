package com.example.backend.controller;


import com.example.backend.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;


@RestController
public class MailController {

    @Autowired
    private MailService mailService;

    private final int pageSize = 10;

    @RequestMapping("/send-mail")
    public String sendMail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content,
            @RequestParam(required = false) String attachmentName,
            @RequestParam(required = false) byte[] attachmentContent) {
        return mailService.sendMail(to, subject, content, attachmentName, attachmentContent);
    }

    @RequestMapping("/view-mail/{mailbox}/fetch-mail/{mailId}")
    public String fetchMail(
            @PathVariable long mailId,
            @PathVariable String mailbox){
        return mailService.fetchMail(mailId, mailbox);
    }

    @RequestMapping("/view-mail/{mailbox}/{pageNum}")
    public String viewMailbox(
            @PathVariable String mailbox,
            @PathVariable int pageNum){
        return mailService.viewMail(mailbox, pageNum, pageSize);
    }


    @RequestMapping("/search-mail/{mailbox}/{pageNum}")
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
            @RequestParam(defaultValue = "false") boolean receiver_star){
        return mailService.searchMail(mailbox, pageNum, pageSize, from, to, subject, body, since, unseen, sender_star, receiver_star);
    }

    @RequestMapping("/view-mail/{mailbox}/fetch-mail/{mailId}/change-mail/{sign}/{op}")
    public RedirectView changeMail(
            @PathVariable String mailbox,
            @PathVariable long mailId,
            @PathVariable String sign,
            @PathVariable String op){
        mailService.changeMail(mailId, sign, op);
        if (sign.equals("TRASH") || sign.equals("READ") && op.equals("-FLAG")) {
            return new RedirectView("/view-mail/" + mailbox + "/1");
        }
        else {
            return new RedirectView("/view-mail/" + mailbox + "/fetch-mail/" + mailId);
        }
    }
}
