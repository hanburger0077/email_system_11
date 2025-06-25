package com.example.backend.service.Impl;

import com.example.backend.Client.ImapClient;
import com.example.backend.Client.Manager.ImapClientManager;
import com.example.backend.entity.Attachment;
import com.example.backend.mapper.AttachmentMapper;
import com.example.backend.service.AttachmentService;
import com.example.backend.utils.ResultVo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.annotation.RequestScope;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequestScope
public class AttachmentServiceImpl implements AttachmentService {
    @Value("${attachment.storage.dir}") // 从配置文件中读取存储路径
    private String storageDir;

    // 用户邮箱
    private String userEmail;

    @Autowired
    private ImapClientManager imapClientManager;

    private ImapClient imapClient;

    public AttachmentServiceImpl(ImapClientManager imapClientManager,  @Autowired(required = false) HttpServletRequest request) {
        this.imapClientManager = imapClientManager;
        userEmail = extractUserEmailFromRequest(request);
        imapClient = imapClientManager.getClientForUser(userEmail);
    }

    private String extractUserEmailFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("loginUserEmail".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    System.out.println(cookie.getValue());
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public ResultVo getAttachmentById(Long attachmentId) {
        try {
            imapClient.doneCommand();
        } catch (InterruptedException e) {
            return ResultVo.fail("操作失败", "Failed to send DONE command" + e.getMessage());
        }
        Attachment attachment = null;
        try {
            attachment = imapClient.attachmentCommand(attachmentId);
            return ResultVo.success("操作成功", attachment);
        } catch (InterruptedException e) {
            return ResultVo.fail("操作失败", "Failed to send ATTACHMENT command" + e.getMessage());
        } finally {
            try {
                imapClient.idleCommand();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
