package com.knowledge.agent.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DocumentPageControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DocumentPageController()).build();

    @Test
    @DisplayName("detail route should forward to static detail page")
    void detailRoute_shouldForwardToStaticDetailPage() throws Exception {
        mockMvc.perform(get("/details-2033817102969135106"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/document-detail.html"));
    }
}
