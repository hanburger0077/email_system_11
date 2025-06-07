package com.example.backend.controller;

import com.example.backend.entity.Attachment;
import com.example.backend.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    @Autowired
    private AttachmentService attachmentService;

    // 上传附件
    @PostMapping("/upload")
    public Attachment upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mailId") Long mailId) throws Exception {
        return attachmentService.upload(file, mailId);
    }

    // 下载附件
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws Exception {
        Resource resource = attachmentService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // 删除附件
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) throws IOException {
        attachmentService.delete(id);
    }
}