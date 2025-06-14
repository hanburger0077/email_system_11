package com.example.backend.mapper;

import com.example.backend.entity.Attachment;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AttachmentMapper {
    // 插入附件记录
    @Insert("INSERT INTO email_attachment (email_id, file_name, file_path, file_type, file_size) " +
            "VALUES (#{emailId}, #{fileName}, #{filePath}, #{fileType}, #{fileSize})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Attachment attachment);

    // 根据ID查询附件
    @Select("SELECT * FROM email_attachment WHERE id = #{id}")
    Attachment selectById(@Param("id") Long id);

    // 根据邮件ID查询附件列表（可选）
    @Select("SELECT * FROM email_attachment WHERE email_id = #{emailId}")
    List<Attachment> selectByEmailId(@Param("emailId") Long emailId);
}