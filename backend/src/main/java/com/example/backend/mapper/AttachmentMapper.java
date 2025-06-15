package com.example.backend.mapper;

import com.example.backend.entity.Attachment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AttachmentMapper {
    // 插入附件记录
    void insert(Attachment attachment);

    // 根据ID查询附件
    Attachment selectById(Long id);

    // 根据邮件ID查询附件列表（可选）
    List<Long> selectByEmailId(Long emailId);
}
