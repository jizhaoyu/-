package com.knowledge.agent.service.impl;

import cn.hutool.core.util.StrUtil;
import com.knowledge.agent.common.ErrorCodeEnum;
import com.knowledge.agent.config.StorageProperties;
import com.knowledge.agent.exception.ServiceException;
import com.knowledge.agent.service.FileStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> SUPPORTED_TYPES = Set.of("pdf", "md", "txt");

    private final StorageProperties storageProperties;

    @Override
    public Path store(MultipartFile file, String kbId) {
        String originalName = file.getOriginalFilename();
        String sourceType = detectSourceType(originalName);
        String fileSuffix = "." + sourceType;

        String dateDir = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Path storageRoot = resolveStorageRoot();
        Path targetDir = storageRoot.resolve(kbId).resolve(dateDir).normalize();
        try {
            ensurePathWithinStorageRoot(targetDir, storageRoot, "kbId contains unsupported path characters");
            Files.createDirectories(targetDir);
            String saveName = System.currentTimeMillis() + "-" + java.util.UUID.randomUUID() + fileSuffix;
            Path targetPath = targetDir.resolve(saveName).normalize();
            ensurePathWithinStorageRoot(targetPath, storageRoot, "storage target path is invalid");
            file.transferTo(targetPath);
            return targetPath.toAbsolutePath().normalize();
        } catch (IOException ex) {
            log.error("store file failed, kbId={}, file={}", kbId, originalName, ex);
            throw new ServiceException(ErrorCodeEnum.SYSTEM_ERROR, "store file failed");
        }
    }

    @Override
    public void delete(String storagePath) {
        if (StrUtil.isBlank(storagePath)) {
            return;
        }
        try {
            Path storageRoot = resolveStorageRoot();
            Path targetPath = Paths.get(storagePath).toAbsolutePath().normalize();
            ensurePathWithinStorageRoot(targetPath, storageRoot, "delete target path is invalid");
            Files.deleteIfExists(targetPath);
        } catch (Exception ex) {
            log.error("delete file failed, path={}", storagePath, ex);
            throw new ServiceException(ErrorCodeEnum.SYSTEM_ERROR, "delete file failed");
        }
    }

    @Override
    public String detectSourceType(String fileName) {
        if (StrUtil.isBlank(fileName) || !fileName.contains(".")) {
            throw new ServiceException(ErrorCodeEnum.UNSUPPORTED_FILE_TYPE, "file type is missing");
        }
        String suffix = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!SUPPORTED_TYPES.contains(suffix)) {
            throw new ServiceException(ErrorCodeEnum.UNSUPPORTED_FILE_TYPE,
                    "unsupported file type: " + suffix + ", only pdf/md/txt are supported");
        }
        return suffix;
    }

    private Path resolveStorageRoot() {
        return Paths.get(storageProperties.getStoragePath()).toAbsolutePath().normalize();
    }

    private void ensurePathWithinStorageRoot(Path candidatePath, Path storageRoot, String message) {
        if (!candidatePath.startsWith(storageRoot)) {
            throw new ServiceException(ErrorCodeEnum.BAD_REQUEST, message);
        }
    }
}
