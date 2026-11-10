package com.placidway.aibot.ui.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Top app bar displayed at the top of the chat view.
 * <p>
 * Shows the assistant name on the left and a "New chat" action on the right.
 *
 * @author Husain Harmoni
 */
public class Header extends HorizontalLayout {

    /**
     * Creates the header bar with the given assistant name.
     *
     * @param assistantName the name to display; falls back to "Medical AI Assistant" if blank
     * @param onNewChat     callback invoked when the new-chat action is clicked
     */
    public Header(String assistantName, Runnable onNewChat) {
        addClassName("header");
        setWidthFull();
        setDefaultVerticalComponentAlignment(Alignment.CENTER);
        setSpacing(false);

        String name = assistantName == null || assistantName.isBlank()
                ? "Medical AI Assistant"
                : assistantName;

        Icon logo = new Icon(VaadinIcon.HEART);
        logo.addClassName("header-logo");
        logo.setSize("20px");

        Span title = new Span(name);
        title.addClassName("store-name");

        HorizontalLayout left = new HorizontalLayout(logo, title);
        left.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        left.addClassName("header-left");

        Button newChatButton = new Button("New chat", new Icon(VaadinIcon.PLUS));
        newChatButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        newChatButton.addClassName("header-action");
        newChatButton.setAriaLabel("New chat");
        newChatButton.addClickListener(event -> {
            if (onNewChat != null) {
                onNewChat.run();
            }
        });

        add(left, newChatButton);
        setFlexGrow(1, left);
    }
}
