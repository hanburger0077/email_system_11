package com.example.backend.entity;

public class Attachment {
    private Long id;          // 附件ID（数据库自增）
    private Long emailId;     // 关联的邮件ID
    private String fileName;  // 原始文件名（如 "report.pdf"）
    private String filePath;  // 服务器存储路径（如 "/data/attachments/uuid_report.pdf"）
    private String fileType;  // 文件类型（如 "application/pdf"）
    private Long fileSize;    // 文件大小（字节）

    // Getter 和 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmailId() { return emailId; }
    public void setEmailId(Long emailId) { this.emailId = emailId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
}