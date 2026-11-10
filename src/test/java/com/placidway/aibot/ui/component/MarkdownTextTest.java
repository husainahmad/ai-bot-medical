package com.placidway.aibot.ui.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTextTest {

    @Test
    void rendersBoldAndListsAsHtml() {
        MarkdownText text = new MarkdownText("**Hello**\n\n- one\n- two");

        String html = text.getElement().getProperty("innerHTML", "");

        assertTrue(html.contains("<strong>Hello</strong>"));
        assertTrue(html.contains("<li>one</li>"));
        assertTrue(html.contains("<li>two</li>"));
    }

    @Test
    void escapesRawHtmlFromModelOutput() {
        MarkdownText text = new MarkdownText("<script>alert('x')</script>");

        String html = text.getElement().getProperty("innerHTML", "");

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void blankInputRendersNothing() {
        MarkdownText text = new MarkdownText("   ");

        assertEquals("", text.getText());
        assertEquals("", text.getElement().getProperty("innerHTML", ""));
    }

    @Test
    void setMarkdownReplacesContent() {
        MarkdownText text = new MarkdownText("first");
        text.setMarkdown("**second**");

        String html = text.getElement().getProperty("innerHTML", "");

        assertTrue(html.contains("<strong>second</strong>"));
        assertFalse(html.contains("first"));
    }
}
