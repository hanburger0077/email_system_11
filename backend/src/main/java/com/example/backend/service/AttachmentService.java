package com.example.backend.service;

import com.example.backend.entity.Attachment;
import com.example.backend.utils.ResultVo;

public interface AttachmentService {
    /**
     * 根据附件ID获取附件信息
     * @param attachmentId 附件ID
     * @return 附件实体（含文件路径）
     */
    ResultVo getAttachmentById(Long attachmentId);
}