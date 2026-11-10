package com.placidway.aibot.ui.component;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Animated typing indicator shown while the assistant reply is streaming.
 *
 * @author Husain Harmoni
 */
public class TypingIndicator extends VerticalLayout {

    /** Creates the animated three-dot typing indicator. */
    public TypingIndicator() {
        addClassName("typing-indicator");
        setWidthFull();
        setPadding(false);
        setSpacing(false);

        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(JustifyContentMode.START);
        layout.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        layout.setSpacing(true);

        Icon avatar = new Icon(VaadinIcon.HEART);
        avatar.addClassName("message-avatar");
        avatar.setSize("28px");

        HorizontalLayout dots = new HorizontalLayout();
        dots.addClassName("typing-dots");
        dots.setSpacing(false);
        for (int i = 0; i < 3; i++) {
            Span dot = new Span();
            dot.addClassName("typing-dot");
            dots.add(dot);
        }

        Span text = new Span("typing");
        text.addClassName("typing-text");

        HorizontalLayout bubble = new HorizontalLayout(dots, text);
        bubble.addClassName("message-bubble");
        bubble.addClassName("assistant-bubble");
        bubble.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        bubble.setSpacing(true);

        layout.add(avatar, bubble);
        add(layout);
    }
}
