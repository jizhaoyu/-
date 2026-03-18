package com.knowledge.agent.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class StaticResourceSmokeTest {

    @Test
    @DisplayName("index should reference relative static assets and diagnostics links")
    void index_shouldReferenceRelativeAssetsAndDiagnosticsLinks() throws IOException {
        String indexHtml = readClasspathResource("static/index.html");

        assertThat(indexHtml).contains("href=\"app.css\"");
        assertThat(indexHtml).contains("src=\"app.js\"");
        assertThat(indexHtml).contains("href=\"swagger-ui/index.html\"");
        assertThat(indexHtml).contains("href=\"doc.html\"");
        assertThat(indexHtml).contains("href=\"actuator/health\"");
        assertThat(indexHtml).contains("rel=\"icon\" href=\"data:,\"");
    }

    @Test
    @DisplayName("detail page should reference shared styles and dedicated script")
    void detailPage_shouldReferenceSharedStylesAndDedicatedScript() throws IOException {
        String detailHtml = readClasspathResource("static/document-detail.html");

        assertThat(detailHtml).contains("href=\"app.css\"");
        assertThat(detailHtml).contains("src=\"detail.js\"");
        assertThat(detailHtml).contains("href=\"./#documents\"");
    }

    @Test
    @DisplayName("app script should resolve api base from current context path")
    void appScript_shouldResolveApiBaseFromCurrentContextPath() throws IOException {
        String appJs = readClasspathResource("static/app.js");

        assertThat(appJs).contains("new URL(\"./api/kb\", document.baseURI)");
    }

    @Test
    @DisplayName("detail script should resolve api base from current context path")
    void detailScript_shouldResolveApiBaseFromCurrentContextPath() throws IOException {
        String detailJs = readClasspathResource("static/detail.js");

        assertThat(detailJs).contains("new URL(\"./api/kb\", document.baseURI)");
    }

    @Test
    @DisplayName("static assets should exist")
    void staticAssets_shouldExist() {
        assertThat(new ClassPathResource("static/index.html").exists()).isTrue();
        assertThat(new ClassPathResource("static/app.js").exists()).isTrue();
        assertThat(new ClassPathResource("static/app.css").exists()).isTrue();
        assertThat(new ClassPathResource("static/document-detail.html").exists()).isTrue();
        assertThat(new ClassPathResource("static/detail.js").exists()).isTrue();
    }

    private String readClasspathResource(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
