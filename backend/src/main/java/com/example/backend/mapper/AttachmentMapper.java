package com.example.backend.mapper;

import com.example.backend.entity.Attachment;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AttachmentMapper {
    @Insert("INSERT INTO attachment (mail_id, file_name, file_key, file_size, file_type) " +
            "VALUES (#{mailId}, #{fileName}, #{fileKey}, #{fileSize}, #{fileType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Attachment attachment);

    @Select("SELECT * FROM attachment WHERE id = #{id}")
    Attachment selectById(Long id);

    @Delete("DELETE FROM attachment WHERE id = #{id}")
    void deleteById(Long id);
}