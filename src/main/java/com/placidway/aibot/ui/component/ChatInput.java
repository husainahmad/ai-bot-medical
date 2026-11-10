package com.placidway.aibot.ui.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;

/**
 * Message composer bar pinned to the bottom of the chat view.
 * <p>
 * Enter sends the message, Shift+Enter inserts a newline. The composer is
 * disabled while the assistant is replying.
 *
 * @author Husain Harmoni
 */
public class ChatInput extends HorizontalLayout {

    /** Callback interface for when the user submits a message. */
    public interface SendListener {
        /**
         * Called with the trimmed message text when the user sends.
         *
         * @param message the non-empty, trimmed message text
         */
        void onSend(String message);
    }

    private final TextArea textArea;
    private final Button sendButton;
    private boolean waitingForResponse = false;
    private SendListener sendListener;

    /** Creates the composer with the text area, Enter-to-send handling, and send button. */
    public ChatInput() {
        addClassName("chat-input");
        setWidthFull();
        setDefaultVerticalComponentAlignment(Alignment.END);
        setSpacing(true);

        textArea = new TextArea();
        textArea.setPlaceholder("Type your message...");
        textArea.addClassName("chat-textarea");
        textArea.setMinHeight("42px");
        textArea.setMaxHeight("120px");
        textArea.setWidthFull();

        // Enter sends; Shift+Enter inserts a newline. Handled on the client so the
        // default newline can be prevented before it is inserted.
        textArea.getElement().executeJs("""
                this.addEventListener('keydown', (event) => {
                    if (event.key === 'Enter' && !event.shiftKey) {
                        event.preventDefault();
                        this.dispatchEvent(new CustomEvent('chat-send', { bubbles: true }));
                    }
                });
                """);
        textArea.getElement().addEventListener("chat-send", event -> sendMessage());

        sendButton = new Button(new Icon(VaadinIcon.ARROW_UP));
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        sendButton.addClassName("send-button");
        sendButton.setAriaLabel("Send");
        sendButton.addClickListener(e -> sendMessage());

        add(textArea, sendButton);
        setFlexGrow(1, textArea);
    }

    /**
     * Registers a listener that will be called when the user sends a message.
     *
     * @param listener the callback to invoke with the message text
     */
    public void setSendListener(SendListener listener) {
        this.sendListener = listener;
    }

    private void sendMessage() {
        String message = textArea.getValue();
        if (message == null || message.trim().isEmpty() || waitingForResponse) {
            return;
        }
        message = message.trim();
        textArea.clear();
        textArea.focus();
        if (sendListener != null) {
            sendListener.onSend(message);
        }
    }

    /**
     * Toggles the waiting-for-response state. When waiting, both the text area
     * and send button are disabled to prevent duplicate submissions.
     *
     * @param waiting {@code true} to disable input while the assistant is responding
     */
    public void setWaitingForResponse(boolean waiting) {
        this.waitingForResponse = waiting;
        boolean enabled = isEnabled() && !waiting;
        sendButton.setEnabled(enabled);
        textArea.setEnabled(enabled);
    }

    /**
     * Returns whether the chat is currently awaiting an assistant response.
     *
     * @return {@code true} while input and the send button are disabled
     */
    public boolean isWaitingForResponse() {
        return waitingForResponse;
    }

    /** Sets focus to the text area for immediate typing. */
    public void focus() {
        textArea.focus();
    }
}
