package com.placidway.aibot.ui;

import com.placidway.aibot.service.ChatService;
import com.placidway.aibot.ui.component.AiMessage;
import com.placidway.aibot.ui.component.ChatInput;
import com.placidway.aibot.ui.component.Header;
import com.placidway.aibot.ui.component.TypingIndicator;
import com.placidway.aibot.ui.component.UserMessage;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import reactor.core.Disposable;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Main chat view: a header, a scrollable conversation, and a message composer.
 * <p>
 * Replies are streamed from Spring AI and rendered incrementally. There is no
 * login or registration — each browser session gets its own conversation id so
 * the assistant remembers the chat while the session lives.
 *
 * @author Husain Harmoni
 */
@Route(value = "", layout = MainLayout.class)
public class ChatView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(ChatView.class);

    private static final String CONVERSATION_ID = "conversationId";
    private static final String NO_RESPONSE_FALLBACK = "Sorry, I did not get a response. Please try again.";
    private static final String SERVICE_DOWN_FALLBACK = "Sorry, the assistant is unavailable right now. Please try again later.";
    private static final long FLUSH_INTERVAL_NS = 50_000_000L;

    private final ChatService chatService;
    private final Environment environment;

    private String conversationId;
    private Header header;
    private Scroller chatScroller;
    private VerticalLayout chatContainer;
    private ChatInput chatInput;
    private TypingIndicator typingIndicator;
    private Disposable subscription;

    /**
     * Creates the chat view.
     *
     * @param chatService the assistant service
     * @param environment the environment used to read the assistant name
     */
    public ChatView(ChatService chatService, Environment environment) {
        this.chatService = chatService;
        this.environment = environment;

        addClassName("chat-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        chatContainer = new VerticalLayout();
        chatContainer.addClassName("chat-container");
        chatContainer.setWidthFull();
        chatContainer.setPadding(true);
        chatContainer.setSpacing(true);

        chatScroller = new Scroller(chatContainer);
        chatScroller.addClassName("chat-scroller");
        chatScroller.setWidthFull();
        chatScroller.getStyle().set("flex", "1").set("overflow-y", "auto").set("overflow-x", "hidden");
        add(chatScroller);
        setFlexGrow(1, chatScroller);

        chatInput = new ChatInput();
        chatInput.setSendListener(this::handleUserMessage);
        add(chatInput);

        typingIndicator = new TypingIndicator();
        typingIndicator.setVisible(false);
    }

    /**
     * Resolves the conversation id for this session and shows the greeting on
     * first entry.
     *
     * @param event the navigation event
     */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (header == null) {
            header = new Header(
                    environment.getProperty("app.assistant-name", "Medical AI Assistant"),
                    this::startNewChat);
            addComponentAtIndex(0, header);
        }

        Object stored = VaadinSession.getCurrent().getAttribute(CONVERSATION_ID);
        if (stored instanceof String existing && !existing.isBlank()) {
            conversationId = existing;
        } else {
            conversationId = UUID.randomUUID().toString();
            VaadinSession.getCurrent().setAttribute(CONVERSATION_ID, conversationId);
        }

        if (chatContainer.getComponentCount() == 0) {
            showGreeting();
        }
        chatInput.focus();
    }

    /**
     * Handles a user-submitted message: appends the user bubble, shows the
     * typing indicator, and subscribes to the streaming assistant reply.
     *
     * @param message the user's message text
     */
    private void handleUserMessage(String message) {
        if (chatInput.isWaitingForResponse()) {
            log.debug("Ignoring message while waiting for a response conversationId={}", conversationId);
            return;
        }

        log.debug("User message conversationId={} chars={}", conversationId,
                message == null ? 0 : message.length());
        addUserMessage(message);
        showTypingIndicator();

        UI ui = UI.getCurrent();
        StringBuilder buffer = new StringBuilder();
        AtomicReference<AiMessage> bubbleRef = new AtomicReference<>();
        AtomicLong lastFlush = new AtomicLong(0);

        subscription = chatService.stream(conversationId, message).subscribe(
                chunk -> {
                    if (chunk == null || chunk.isEmpty()) {
                        return;
                    }
                    buffer.append(chunk);
                    long now = System.nanoTime();
                    if (now - lastFlush.get() < FLUSH_INTERVAL_NS) {
                        return;
                    }
                    lastFlush.set(now);
                    String snapshot = buffer.toString();
                    access(ui, () -> renderStreaming(snapshot, bubbleRef));
                },
                error -> access(ui, () -> finishWithError(error, buffer.toString(), bubbleRef)),
                () -> access(ui, () -> finishReply(buffer.toString(), bubbleRef)));
    }

    /**
     * Renders the partial reply while it is still streaming in.
     *
     * @param text      the text received so far
     * @param bubbleRef the assistant bubble, created on the first chunk
     */
    private void renderStreaming(String text, AtomicReference<AiMessage> bubbleRef) {
        hideTypingIndicator();
        AiMessage bubble = bubbleRef.get();
        if (bubble == null) {
            bubble = new AiMessage(text);
            chatContainer.add(bubble);
            bubbleRef.set(bubble);
        } else {
            bubble.setText(text);
        }
        scrollToBottom();
    }

    /**
     * Renders the final assistant message and clears the waiting state.
     *
     * @param rawText   the text accumulated from the stream
     * @param bubbleRef the assistant bubble, or {@code null} if nothing streamed
     */
    private void finishReply(String rawText, AtomicReference<AiMessage> bubbleRef) {
        hideTypingIndicator();
        String clean = rawText == null ? "" : rawText.trim();
        if (clean.isEmpty()) {
            clean = NO_RESPONSE_FALLBACK;
        }
        log.debug("Reply finished conversationId={} chars={}", conversationId, clean.length());
        AiMessage bubble = bubbleRef.get();
        if (bubble == null) {
            chatContainer.add(new AiMessage(clean));
        } else {
            bubble.setText(clean);
        }
        chatInput.setWaitingForResponse(false);
        scrollToBottom();
    }

    /**
     * Renders whatever was received before a stream failure, or a service-down
     * message when nothing usable arrived.
     *
     * @param error     the stream error
     * @param rawText   the text accumulated before the failure
     * @param bubbleRef the assistant bubble, or {@code null}
     */
    private void finishWithError(Throwable error, String rawText, AtomicReference<AiMessage> bubbleRef) {
        log.error("Chat stream failed: {}", error.getMessage(), error);
        String clean = rawText == null ? "" : rawText.trim();
        finishReply(clean.isEmpty() ? SERVICE_DOWN_FALLBACK : clean, bubbleRef);
    }

    /**
     * Clears the conversation memory and starts a fresh chat.
     */
    private void startNewChat() {
        if (subscription != null) {
            subscription.dispose();
            subscription = null;
        }
        if (conversationId != null) {
            chatService.clear(conversationId);
        }
        conversationId = UUID.randomUUID().toString();
        VaadinSession.getCurrent().setAttribute(CONVERSATION_ID, conversationId);

        chatContainer.removeAll();
        chatInput.setWaitingForResponse(false);
        showGreeting();
        chatInput.focus();
    }

    /**
     * Shows the opening assistant message.
     */
    private void showGreeting() {
        chatContainer.add(new AiMessage(
                "Hello! I'm your medical information assistant. How can I help you today?"));
    }

    private void showTypingIndicator() {
        chatInput.setWaitingForResponse(true);
        typingIndicator.setVisible(true);
        if (typingIndicator.getParent().isEmpty()) {
            chatContainer.add(typingIndicator);
        }
        scrollToBottom();
    }

    private void hideTypingIndicator() {
        typingIndicator.setVisible(false);
        if (typingIndicator.getParent().isPresent()) {
            chatContainer.remove(typingIndicator);
        }
    }

    private void addUserMessage(String message) {
        chatContainer.add(new UserMessage(message));
    }

    /**
     * Runs a UI update on the Vaadin thread, ignoring updates for a closed UI.
     *
     * @param ui   the UI to update, or {@code null}
     * @param task the update to run
     */
    private void access(UI ui, Command task) {
        if (ui == null) {
            return;
        }
        try {
            ui.access(task);
        } catch (Exception ex) {
            log.debug("Skipping UI update: {}", ex.getMessage());
        }
    }

    /**
     * Scrolls the chat scroller to the newest message via JavaScript.
     */
    private void scrollToBottom() {
        UI ui = UI.getCurrent();
        if (ui == null) {
            return;
        }
        ui.getPage().executeJs("""
            var scroller = document.querySelector('.chat-scroller');
            if (scroller) {
                scroller.scrollTop = scroller.scrollHeight;
            }
        """);
    }

    /** Cancels any in-flight stream when the view is detached. */
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (subscription != null) {
            subscription.dispose();
            subscription = null;
        }
        super.onDetach(detachEvent);
    }
}
