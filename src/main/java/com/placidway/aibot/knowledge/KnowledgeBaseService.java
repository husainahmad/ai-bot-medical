package com.placidway.aibot.knowledge;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads and refreshes the assistant's knowledge base from a fixed set of pages.
 * <p>
 * Only the configured pages are fetched and cached in memory. The extracted
 * text is injected into the system prompt so the assistant can answer solely
 * from this content and never crawls beyond it.
 * <p>
 * The knowledge base is refreshed on a schedule and on demand. A refresh swaps
 * the cached text atomically, so in-flight requests keep the version they
 * started with, and a failed refresh keeps the previous content instead of
 * leaving the assistant empty.
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

    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    private volatile String content = "";

    private volatile Instant lastRefreshedAt;

    private volatile int loadedPages;

    /**
     * Loads the knowledge base at startup. Failures are logged and skipped so a
     * single unavailable page does not prevent the app from starting.
     */
    @PostConstruct
    void load() {
        refresh();
    }

    /**
     * Refreshes the knowledge base on the configured schedule.
     * <p>
     * Runs according to {@code app.knowledge-base.refresh.cron}. Disabled when
     * the knowledge base or the schedule is disabled.
     */
    @Scheduled(cron = "${app.knowledge-base.refresh.cron:0 0 3 * * *}")
    public void scheduledRefresh() {
        if (!properties.isEnabled() || !properties.getRefresh().isEnabled()) {
            log.debug("Scheduled knowledge base refresh skipped (disabled)");
            return;
        }
        log.info("Scheduled knowledge base refresh starting");
        refresh();
    }

    /**
     * Fetches every configured page and swaps the cached content.
     * <p>
     * Refreshes are single-flight: if one is already running, the call returns
     * immediately with {@code false}. If every page fails, the previous content
     * is kept.
     *
     * @return {@code true} when the content was replaced, {@code false} otherwise
     */
    public boolean refresh() {
        if (!properties.isEnabled()) {
            log.debug("Knowledge base refresh skipped: disabled");
            return false;
        }
        if (!refreshing.compareAndSet(false, true)) {
            log.info("Knowledge base refresh skipped: another refresh is already running");
            return false;
        }

        try {
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

            if (loaded == 0) {
                log.error("Knowledge base refresh failed: no pages could be loaded; keeping previous content");
                return false;
            }

            String newContent = String.join("\n\n---\n\n", sections);
            this.content = newContent;
            this.loadedPages = loaded;
            this.lastRefreshedAt = Instant.now();
            log.info("Knowledge base ready: {}/{} pages, {} chars, refreshed at {}",
                    loaded, properties.getPages().size(), newContent.length(), lastRefreshedAt);
            log.debug("Knowledge base preview: {}", preview(newContent));
            return true;
        } finally {
            refreshing.set(false);
        }
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

    /**
     * Returns how many pages were loaded successfully in the last refresh.
     *
     * @return the number of loaded pages
     */
    public int loadedPages() {
        return loadedPages;
    }

    /**
     * Returns when the knowledge base was last refreshed successfully.
     *
     * @return the last successful refresh time, or {@code null} if never
     */
    public Instant lastRefreshedAt() {
        return lastRefreshedAt;
    }

    /**
     * Returns whether a refresh is currently running.
     *
     * @return {@code true} while a refresh is in progress
     */
    public boolean isRefreshing() {
        return refreshing.get();
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
