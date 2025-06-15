package com.example.backend.service;

import com.example.backend.entity.Attachment;
import com.example.backend.utils.ResultVo;

public interface AttachmentService {
    /**
     * 保存附件到磁盘并记录到数据库
     * @param emailId    关联的邮件ID
     * @param fileBytes 文件字节流（由协议模块解析后传入）
     * @param fileName  原始文件名
     * @param fileType  文件类型
     */
    Attachment saveAttachment(Long emailId, byte[] fileBytes, String fileName, String fileType);


    /**
     * 根据附件ID获取附件信息
     * @param attachmentId 附件ID
     * @return 附件实体（含文件路径）
     */
    ResultVo getAttachmentById(Long attachmentId);
}