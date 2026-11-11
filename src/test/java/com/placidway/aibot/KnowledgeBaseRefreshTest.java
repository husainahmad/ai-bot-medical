package com.placidway.aibot;

import com.placidway.aibot.knowledge.KnowledgeBaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class KnowledgeBaseRefreshTest {

    @Autowired
    KnowledgeBaseService knowledgeBase;

    @Test
    void manualRefreshReplacesContent() {
        assertTrue(knowledgeBase.loadedPages() > 0, "startup load should succeed");
        assertNotNull(knowledgeBase.lastRefreshedAt());

        int pages = knowledgeBase.loadedPages();
        int chars = knowledgeBase.content().length();

        assertTrue(knowledgeBase.refresh(), "manual refresh should succeed");

        assertEquals(pages, knowledgeBase.loadedPages());
        assertTrue(knowledgeBase.content().length() > 0);
        assertFalse(knowledgeBase.isRefreshing());
        System.out.println("REFRESH_OK pages=" + pages + " charsBefore=" + chars
                + " charsAfter=" + knowledgeBase.content().length());
    }
}
