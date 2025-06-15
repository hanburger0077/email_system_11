package com.example.backend.controller;

import com.example.backend.entity.Attachment;
import com.example.backend.service.AttachmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

@RestController
@RequestMapping("/attachments")
public class AttachmentController {
    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /**
     * 生成附件下载链接（供协议模块调用）
     */
    @GetMapping("/url/{attachmentId}")
    public String getDownloadUrl(@PathVariable Long attachmentId) {
        return attachmentService.generateDownloadUrl(attachmentId);
    }

    /**
     * 下载附件（返回文件流）
     */
    @GetMapping("/download/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long attachmentId) {
        Attachment attachment = attachmentService.getAttachmentById(attachmentId);
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(Paths.get(attachment.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("File not found", e);
        }

        return ResponseEntity.ok()
                .header("Content-Type", attachment.getFileType())
                .header("Content-Disposition", "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(fileBytes);
    }
}