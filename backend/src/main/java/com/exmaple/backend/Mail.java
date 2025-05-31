package com.example.email.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "mail")
@Getter
@Setter
public class Mail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 邮件ID（自增主键）

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;  // 发件人（关联用户实体）

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;  // 收件人（关联用户实体）

    @Column(nullable = false, length = 255)
    private String subject;  // 邮件主题（最大255字符）

    @Column(columnDefinition = "TEXT")
    private String content;  // 邮件正文（支持HTML）

    @Column(nullable = false)
    private boolean isRead = false;  // 是否已读（默认未读）

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;  // 发送时间（自动生成）

    @OneToMany(mappedBy = "mail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments;  // 附件列表（一对多关联）

    // 业务方法示例：标记为已读
    public void markAsRead() {
        this.isRead = true;
    }
}