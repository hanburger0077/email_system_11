package com.example.backend.utils;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class FileStorageUtil {

    private final Path rootLocation = Paths.get("uploads");

    public FileStorageUtil() throws IOException {
        Files.createDirectories(rootLocation);
    }

    // 存储文件
    public String store(MultipartFile file) throws IOException {
        String fileKey = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Files.copy(file.getInputStream(), rootLocation.resolve(fileKey), StandardCopyOption.REPLACE_EXISTING);
        return fileKey;
    }

    // 加载文件
    public Resource load(String fileKey) throws IOException {
        Path filePath = rootLocation.resolve(fileKey);
        return new UrlResource(filePath.toUri());
    }

    // 删除文件
    public void delete(String fileKey) throws IOException {
        Files.deleteIfExists(rootLocation.resolve(fileKey));
    }
}