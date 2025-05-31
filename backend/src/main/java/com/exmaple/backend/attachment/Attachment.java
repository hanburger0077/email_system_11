package com.example.email.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "attachment")
@Getter
@Setter
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 附件ID（自增主键）

    @Column(nullable = false, length = 255)
    private String fileName;  // 原始文件名

    @Column(nullable = false, length = 512)
    private String filePath;  // 存储路径（本地或云存储URL）

    @Column(nullable = false)
    private Long fileSize;  // 文件大小（字节）

    @Column(length = 100)
    private String fileType;  // MIME类型（如 application/pdf）

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mail_id", nullable = false)
    private Mail mail;  // 所属邮件（多对一关联）

    // 业务方法示例：获取下载URL（假设存储在本地）
    public String getDownloadUrl() {
        return "/api/attachments/download/" + this.id;
    }
}