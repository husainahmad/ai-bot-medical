package com.placidway.aibot.knowledge;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads the assistant's knowledge base from a fixed set of pages at startup.
 * <p>
 * Only the configured pages are fetched and cached in memory. The extracted
 * text is injected into the system prompt so the assistant can answer solely
 * from this content and never crawls beyond it.
 *
 * @author Husain Harmoni
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private static final String USER_AGENT = "Mozilla/5.0 (compatible; PlacidWayMedicalBot/1.0)";

    private static final String CONTENT_SELECTOR = "h1,h2,h3,h4,h5,h6,p,li,tr,blockquote,figcaption";

    private final KnowledgeBaseProperties properties;

    private volatile String content = "";

    /**
     * Fetches and caches every configured page. Failures are logged and skipped
     * so a single unavailable page does not prevent the app from starting.
     */
    @PostConstruct
    void load() {
        if (!properties.isEnabled()) {
            log.info("Knowledge base disabled; the assistant will not be grounded on any page.");
            return;
        }

        List<String> sections = new ArrayList<>();
        int loaded = 0;
        log.debug("Loading knowledge base from {} configured pages", properties.getPages().size());
        for (KnowledgeBaseProperties.Page page : properties.getPages()) {
            if (page.getUrl() == null || page.getUrl().isBlank()) {
                log.debug("Skipping knowledge base page with blank URL: {}", safeTitle(page));
                continue;
            }
            try {
                log.debug("Fetching knowledge base page '{}' from {}", safeTitle(page), page.getUrl());
                String text = fetch(page.getUrl());
                if (text.isBlank()) {
                    log.warn("Knowledge base page returned no text: {}", page.getUrl());
                    continue;
                }
                log.debug("Extracted {} chars from {}", text.length(), page.getUrl());
                sections.add("### " + safeTitle(page) + "\nSource: " + page.getUrl() + "\n\n" + text);
                loaded++;
                log.info("Loaded knowledge base page '{}' ({} chars)", safeTitle(page), text.length());
            } catch (Exception ex) {
                log.warn("Failed to load knowledge base page {}: {}", page.getUrl(), ex.getMessage());
                log.debug("Knowledge base page load failure detail", ex);
            }
        }

        this.content = String.join("\n\n---\n\n", sections);
        log.info("Knowledge base ready: {}/{} pages, {} chars", loaded, properties.getPages().size(), content.length());
        log.debug("Knowledge base preview: {}", preview(content));
    }

    /**
     * Returns the cached knowledge-base text, or an empty string when nothing
     * could be loaded.
     *
     * @return the combined page content
     */
    public String content() {
        return content;
    }

    private String fetch(String url) throws Exception {
        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(properties.getTimeoutSeconds() * 1000)
                .maxBodySize(0)
                .get();

        doc.select("script,style,noscript,svg,nav,footer,header,select,option,button,form,"
                + ".consultation-container,.chatbot-launcher,.breadcrumb,.nav,.tabs,.tab-content").remove();

        Element main = doc.selectFirst("main");
        Element root = main != null ? main : doc.body();
        return extractText(root);
    }

    private String extractText(Element root) {
        StringBuilder sb = new StringBuilder();
        String last = null;
        for (Element element : root.select(CONTENT_SELECTOR)) {
            String line = element.text().replaceAll("\\s+", " ").trim();
            if (line.isEmpty() || line.equals(last)) {
                continue;
            }
            sb.append(line).append('\n');
            last = line;
        }
        if (sb.isEmpty()) {
            return root.text().replaceAll("\\s+", " ").trim();
        }
        return sb.toString().trim();
    }

    private String safeTitle(KnowledgeBaseProperties.Page page) {
        return page.getTitle() == null || page.getTitle().isBlank() ? page.getUrl() : page.getTitle();
    }

    private String preview(String text) {
        if (text == null || text.isBlank()) {
            return "(empty)";
        }
        int limit = Math.min(300, text.length());
        return text.substring(0, limit).replaceAll("\\s+", " ") + (text.length() > limit ? "..." : "");
    }
}
