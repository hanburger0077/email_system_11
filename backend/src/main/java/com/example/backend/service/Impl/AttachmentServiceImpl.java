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
    private final AttachmentMapper attachmentMapper;

    @Value("${attachment.storage.dir}") // 从配置文件中读取存储路径
    private String storageDir;

    @Autowired
    private ImapClient imapClient;

    public AttachmentServiceImpl(AttachmentMapper attachmentMapper) {
        this.attachmentMapper = attachmentMapper;
    }

    @Override
    public Attachment saveAttachment(Long emailId, byte[] fileBytes, String fileName, String fileType) {
        //  生成唯一文件名和存储路径
        String uniqueFileName = UUID.randomUUID() + "_" + fileName;
        Path filePath = Paths.get(storageDir, uniqueFileName);

        // 2. 确保目录存在并写入文件
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, fileBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save attachment", e);
        }

        // 3. 保存记录到数据库
        Attachment attachment = new Attachment();
        attachment.setEmailId(emailId);
        attachment.setFileName(fileName);
        attachment.setFilePath(filePath.toString());
        attachment.setFileType(fileType);
        attachment.setFileSize((long) fileBytes.length);
        attachmentMapper.insert(attachment);

        return attachment;
    }


    @Override
    public ResultVo getAttachmentById(Long attachmentId) {
        try {
            imapClient.doneCommand();
        } catch (InterruptedException e) {
            return ResultVo.fail(0, "Failed to send DONE command" + e.getMessage());
        }
        Attachment attachment = null;
        try {
            attachment = imapClient.attachmentCommand(attachmentId);
            return ResultVo.success("Attachment information gotten successfully", attachment);
        } catch (InterruptedException e) {
            return ResultVo.fail(0, "Failed to send ATTACHMENT command" + e.getMessage());
        } finally {
            try {
                imapClient.idleCommand();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
