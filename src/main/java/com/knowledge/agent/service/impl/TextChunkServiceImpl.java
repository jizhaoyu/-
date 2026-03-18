package com.knowledge.agent.service.impl;

import cn.hutool.core.util.StrUtil;
import com.knowledge.agent.config.RagProperties;
import com.knowledge.agent.service.TextChunkService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TextChunkServiceImpl implements TextChunkService {

    private final RagProperties ragProperties;

    @Override
    public List<String> split(String content) {
        if (StrUtil.isBlank(content)) {
            return List.of();
        }

        int chunkSize = ragProperties.getChunkSize();
        int overlap = Math.min(ragProperties.getChunkOverlap(), Math.max(chunkSize - 1, 0));
        int step = Math.max(chunkSize - overlap, 1);

        List<String> chunks = new ArrayList<>();
        String normalized = content.replace("\r\n", "\n").trim();
        for (int start = 0; start < normalized.length(); start += step) {
            int end = Math.min(start + chunkSize, normalized.length());
            String chunk = normalized.substring(start, end).trim();
            if (StrUtil.isNotBlank(chunk)) {
                chunks.add(chunk);
            }
            if (end >= normalized.length()) {
                break;
            }
        }
        return chunks;
    }
}
