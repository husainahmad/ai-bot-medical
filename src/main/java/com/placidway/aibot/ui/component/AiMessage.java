package com.placidway.aibot.ui.component;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Left-aligned chat bubble representing an assistant message.
 *
 * @author Husain Harmoni
 */
public class AiMessage extends VerticalLayout {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final VerticalLayout messageWrapper;
    private final MarkdownText text;

    /**
     * Creates an assistant message bubble with the given text.
     *
     * @param message the assistant's reply text
     */
    public AiMessage(String message) {
        addClassName("assistant-message");
        setWidthFull();
        setPadding(false);
        setSpacing(false);

        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(JustifyContentMode.START);
        layout.setDefaultVerticalComponentAlignment(Alignment.START);
        layout.setSpacing(true);

        messageWrapper = new VerticalLayout();
        messageWrapper.addClassName("message-bubble");
        messageWrapper.addClassName("assistant-bubble");
        messageWrapper.setPadding(false);
        messageWrapper.setSpacing(false);

        Icon avatar = new Icon(VaadinIcon.HEART);
        avatar.addClassName("message-avatar");
        avatar.setSize("28px");

        text = new MarkdownText(message);
        text.addClassName("message-text");

        Span timestamp = new Span(LocalTime.now().format(TIME_FMT));
        timestamp.addClassName("message-timestamp");

        messageWrapper.add(text, timestamp);

        layout.add(avatar, messageWrapper);
        add(layout);
    }

    /**
     * Replaces the bubble text, used while a reply is streaming in.
     *
     * @param message the current reply text
     */
    public void setText(String message) {
        text.setMarkdown(message);
    }
}
