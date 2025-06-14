package com.example.backend.service.Impl;

import com.example.backend.entity.Attachment;
import com.example.backend.mapper.AttachmentMapper;
import com.example.backend.service.AttachmentService;
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
    public String generateDownloadUrl(Long attachmentId) {
        return "/attachments/download/" + attachmentId;
    }

    @Override
    public Attachment getAttachmentById(Long attachmentId) {
        return attachmentMapper.selectById(attachmentId);
    }
}
