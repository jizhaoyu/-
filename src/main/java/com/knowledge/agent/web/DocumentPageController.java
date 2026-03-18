package com.knowledge.agent.web;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Hidden
@Controller
public class DocumentPageController {

    @GetMapping("/details-{id:[1-9]\\d*}")
    public String detailPage() {
        return "forward:/document-detail.html";
    }
}
