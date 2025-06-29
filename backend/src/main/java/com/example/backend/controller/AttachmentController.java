package com.example.backend.controller;

import com.example.backend.entity.Attachment;
import com.example.backend.service.AttachmentService;
import com.example.backend.utils.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    @Autowired
    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    /**
     * 获取附件基本信息（不包含数据块）
     */
    @GetMapping("/{attachmentId}")
    public ResultVo getAttachmentInfo(@PathVariable Long attachmentId) {
        return attachmentService.getAttachmentById(attachmentId);
    }

    /**
     * 下载附件（返回文件流）
     */
    @GetMapping("/download/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable Long attachmentId) {
        ResultVo<Attachment> resultVo = attachmentService.getAttachmentById(attachmentId);
        Attachment attachment = resultVo.getData();
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(Paths.get(attachment.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("File not found", e);
        }

        // 使用 ContentDisposition 工具类处理文件名编码
        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(attachment.getFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header("Content-Type", attachment.getFileType())
                .header("Content-Disposition", contentDisposition.toString())
                .body(fileBytes);
    }
}