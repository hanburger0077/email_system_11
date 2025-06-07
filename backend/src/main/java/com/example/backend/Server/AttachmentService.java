package com.example.backend.service;

import com.example.backend.entity.Attachment;
import com.example.backend.mapper.AttachmentMapper;
import com.example.backend.utils.FileStorageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class AttachmentService {

    @Autowired
    private AttachmentMapper attachmentMapper;

    @Autowired
    private FileStorageUtil fileStorageUtil;

    // 上传附件
    public Attachment upload(MultipartFile file, Long mailId) throws IOException {
        String fileKey = fileStorageUtil.store(file);

        Attachment attachment = new Attachment();
        attachment.setMailId(mailId);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileKey(fileKey);
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());

        attachmentMapper.insert(attachment);
        return attachment;
    }

    // 下载附件
    public Resource download(Long id) throws IOException {
        Attachment attachment = attachmentMapper.selectById(id);
        return fileStorageUtil.load(attachment.getFileKey());
    }

    // 删除附件
    public void delete(Long id) throws IOException {
        Attachment attachment = attachmentMapper.selectById(id);
        fileStorageUtil.delete(attachment.getFileKey());
        attachmentMapper.deleteById(id);
    }
}