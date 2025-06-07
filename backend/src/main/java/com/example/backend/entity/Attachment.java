package com.example.backend.entity;

import lombok.Data;

@Data
public class Attachment {
    private Long id;             // 附件ID
    private Long mailId;         // 关联的邮件ID
    private String fileName;     // 原始文件名（如 "report.pdf"）
    private String fileKey;      // 存储唯一标识（本地路径或云存储Key）
    private Long fileSize;       // 文件大小（字节）
    private String fileType;     // MIME类型（如 "application/pdf"）
}