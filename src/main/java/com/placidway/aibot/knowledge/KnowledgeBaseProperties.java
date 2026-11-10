package com.placidway.aibot.knowledge;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the assistant's knowledge base.
 * <p>
 * The bot is grounded strictly on the pages listed here. Nothing outside these
 * pages is used to answer questions.
 *
 * @author Husain Harmoni
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.knowledge-base")
public class KnowledgeBaseProperties {

    private boolean enabled = true;

    private int timeoutSeconds = 30;

    private List<Page> pages = new ArrayList<>();

    /**
     * A single knowledge-base page.
     */
    @Getter
    @Setter
    public static class Page {

        private String title;

        private String url;
    }
}
