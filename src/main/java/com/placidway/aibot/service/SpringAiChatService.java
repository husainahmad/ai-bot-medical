package com.placidway.aibot.service;

import com.placidway.aibot.config.SystemPromptProvider;
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
 * and append to the right history. The system prompt is built per request from
 * the current knowledge base, so refreshes apply without a restart.
 *
 * @author Husain Harmoni
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpringAiChatService implements ChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final SystemPromptProvider systemPromptProvider;

    /** {@inheritDoc} */
    @Override
    public Flux<String> stream(String conversationId, String message) {
        log.debug("Streaming chat request conversationId={} messageChars={}",
                conversationId, message == null ? 0 : message.length());
        return chatClient.prompt()
                .system(systemPromptProvider.build())
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
