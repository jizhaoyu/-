package com.knowledge.agent.service;

import java.nio.file.Path;

public interface DocumentParserService {

    String parse(Path path, String sourceType);
}
