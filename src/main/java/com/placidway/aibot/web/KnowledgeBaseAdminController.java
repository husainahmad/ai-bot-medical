package com.placidway.aibot.web;

import com.placidway.aibot.knowledge.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manual trigger for refreshing the assistant's knowledge base.
 * <p>
 * Call {@code POST /admin/knowledge-base/refresh} after the approved pages
 * change.
 *
 * @author Husain Harmoni
 */
@RestController
@RequestMapping("/admin/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseAdminController {

    private final KnowledgeBaseService knowledgeBase;

    /**
     * Refreshes the knowledge base from the configured pages.
     *
     * @return the refresh result
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh() {
        boolean refreshed = knowledgeBase.refresh();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("refreshed", refreshed);
        body.put("loadedPages", knowledgeBase.loadedPages());
        body.put("chars", knowledgeBase.content().length());
        body.put("lastRefreshedAt", knowledgeBase.lastRefreshedAt());
        body.put("refreshing", knowledgeBase.isRefreshing());
        return ResponseEntity.ok(body);
    }
}
