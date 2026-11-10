package com.placidway.aibot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * {@link ChatService} backed by a Spring AI {@link ChatClient}.
 * <p>
 * Each call is tagged with the conversation id so the memory advisor can load
 * and append to the right history.
 *
 * @author Husain Harmoni
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpringAiChatService implements ChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    /** {@inheritDoc} */
    @Override
    public Flux<String> stream(String conversationId, String message) {
        log.debug("Streaming chat request conversationId={} messageChars={}",
                conversationId, message == null ? 0 : message.length());
        return chatClient.prompt()
                .user(message)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .doOnComplete(() -> log.debug("Chat stream completed conversationId={}", conversationId))
                .doOnError(error -> log.debug("Chat stream failed conversationId={}: {}",
                        conversationId, error.getMessage()));
    }

    /** {@inheritDoc} */
    @Override
    public void clear(String conversationId) {
        log.debug("Clearing chat memory conversationId={}", conversationId);
        chatMemory.clear(conversationId);
    }
}
