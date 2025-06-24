package com.example.backend.service.Impl;

import com.example.backend.Client.ImapClient;
import com.example.backend.entity.Attachment;
import com.example.backend.mapper.AttachmentMapper;
import com.example.backend.service.AttachmentService;
import com.example.backend.utils.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class AttachmentServiceImpl implements AttachmentService {
    @Value("${attachment.storage.dir}") // 从配置文件中读取存储路径
    private String storageDir;

    @Autowired
    private ImapClient imapClient;

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
