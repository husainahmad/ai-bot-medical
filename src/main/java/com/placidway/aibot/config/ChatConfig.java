package com.placidway.aibot.config;

import com.placidway.aibot.knowledge.KnowledgeBaseProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Spring AI chat client used by the medical assistant.
 * <p>
 * The client carries a conversation memory advisor so each browser session
 * keeps its own short chat history. The system prompt (including the knowledge
 * base) is supplied per request by {@link SystemPromptProvider}, so a
 * knowledge-base refresh takes effect immediately.
 *
 * @author Husain Harmoni
 */
@Configuration
@EnableConfigurationProperties(KnowledgeBaseProperties.class)
public class ChatConfig {

    private static final int MAX_MEMORY_MESSAGES = 20;

    /**
     * In-memory conversation store. Suitable for a single-instance demo; swap for a
     * persistent {@code ChatMemoryRepository} when conversations must survive restarts.
     *
     * @return the chat memory used by the assistant
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(MAX_MEMORY_MESSAGES)
                .build();
    }

    /**
     * Builds the shared chat client with the memory advisor.
     *
     * @param chatModel  the auto-configured OpenAI-compatible chat model
     * @param chatMemory the conversation memory
     * @return the configured chat client
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
