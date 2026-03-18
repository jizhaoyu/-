package com.knowledge.agent.service;

import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    Path store(MultipartFile file, String kbId);

    void delete(String storagePath);

    String detectSourceType(String fileName);
}
