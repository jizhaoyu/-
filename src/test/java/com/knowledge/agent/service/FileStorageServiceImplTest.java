package com.knowledge.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.knowledge.agent.exception.ServiceException;
import com.knowledge.agent.config.StorageProperties;
import com.knowledge.agent.service.impl.FileStorageServiceImpl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private FileStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        storageService = buildService();
    }

    @Test
    @DisplayName("delete should remove existing file")
    void delete_shouldRemoveExistingFile() throws IOException {
        Path filePath = tempDir.resolve("default-kb").resolve("demo.txt");
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, "hello");

        storageService.delete(filePath.toString());

        assertThat(filePath).doesNotExist();
    }

    @Test
    @DisplayName("delete should ignore missing file")
    void delete_shouldIgnoreMissingFile() {
        Path filePath = tempDir.resolve("default-kb").resolve("missing.txt");

        assertThatCode(() -> storageService.delete(filePath.toString())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("store should keep file under configured storage root")
    void store_shouldKeepFileUnderConfiguredStorageRoot() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "demo.txt", "text/plain", "hello".getBytes());

        Path storedPath = storageService.store(file, "default-kb");

        assertThat(storedPath).exists();
        assertThat(storedPath.normalize()).startsWith(tempDir.toAbsolutePath().normalize());
    }

    @Test
    @DisplayName("store should reject kbId that escapes storage root")
    void store_shouldRejectEscapingKbId() {
        MockMultipartFile file = new MockMultipartFile("file", "demo.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> storageService.store(file, "..\\..\\outside"))
                .isInstanceOf(ServiceException.class)
                .hasMessage("kbId contains unsupported path characters");
    }

    private FileStorageServiceImpl buildService() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setStoragePath(tempDir.toString());
        return new FileStorageServiceImpl(storageProperties);
    }
}
