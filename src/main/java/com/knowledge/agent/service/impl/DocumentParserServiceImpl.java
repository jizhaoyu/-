package com.knowledge.agent.service.impl;

import cn.hutool.core.util.StrUtil;
import com.knowledge.agent.common.ErrorCodeEnum;
import com.knowledge.agent.exception.ServiceException;
import com.knowledge.agent.service.DocumentParserService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DocumentParserServiceImpl implements DocumentParserService {

    @Override
    public String parse(Path path, String sourceType) {
        try {
            if ("pdf".equalsIgnoreCase(sourceType)) {
                return parsePdf(path);
            }
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("parse file failed, path={}, sourceType={}", path, sourceType, ex);
            throw new ServiceException(ErrorCodeEnum.DOCUMENT_PARSE_FAILED, "parse file failed");
        }
    }

    private String parsePdf(Path path) throws IOException {
        try (PDDocument document = PDDocument.load(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String content = stripper.getText(document);
            if (StrUtil.isBlank(content)) {
                throw new ServiceException(ErrorCodeEnum.DOCUMENT_PARSE_FAILED, "parsed content is empty");
            }
            return content;
        }
    }
}
