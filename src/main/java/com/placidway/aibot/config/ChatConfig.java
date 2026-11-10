package com.placidway.aibot.config;

import com.placidway.aibot.knowledge.KnowledgeBaseProperties;
import com.placidway.aibot.knowledge.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
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
 * The client carries a medical-assistant system prompt, the knowledge base
 * loaded from the configured pages, and a conversation memory advisor, so each
 * browser session keeps its own short chat history.
 *
 * @author Husain Harmoni
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(KnowledgeBaseProperties.class)
public class ChatConfig {

    private static final int MAX_MEMORY_MESSAGES = 20;

    private static final String SYSTEM_PROMPT = """
            You are a helpful medical information assistant for PlacidWay, a medical tourism platform.

            Your role:
            - Answer general health and medical questions in clear, plain language.
            - Help users understand treatments, procedures, and medical travel options.
            - Help users explore doctors, hospitals, and destinations when asked.
            - Ask a short clarifying question when a request is ambiguous.

            Safety rules:
            - You are not a doctor and cannot diagnose, prescribe, or replace professional care.
            - For emergencies or urgent symptoms, tell the user to contact local emergency services immediately.
            - Always recommend consulting a qualified healthcare professional before making medical decisions.
            - Do not invent doctors, prices, hospitals, or availability. If you do not know, say so.

            Style:
            - Be warm, concise, and reassuring.
            - Use short paragraphs and bullet points when helpful.
            - Always reply in the same language the user writes in.
            """;

    private static final String GROUNDING_RULES = """

            === STRICT GROUNDING RULES (these override everything above) ===
            You are a CLOSED-DOMAIN assistant. You may answer ONLY from the KNOWLEDGE BASE above.

            Allowed topics (only these):
            - ITC Immunity Therapy Center in Tijuana, Mexico.
            - The specific treatment packages listed in the knowledge base (adenocarcinoma, adrenal, anal).
            - The clinic's prices, price list, and price comparison shown in the knowledge base.
            - The clinic's videos, doctors, services, and contact/consultation details shown in the knowledge base.

            Mandatory behaviour:
            - Be honest. If the answer is not explicitly present in the knowledge base, say so
              plainly ("I don't know" / "I don't have that information"). Never guess.
            - ALWAYS end a refusal by suggesting the user contact PlacidWay for a free quote.
            - Write the refusal IN THE USER'S LANGUAGE. Do not copy any English wording when the
              user wrote in another language.
            - Never invent prices, clinics, doctors, treatments, availability, or medical facts.
            - Never answer from general knowledge, memory, or assumptions — even for simple questions
              (e.g. geography, math, other clinics, other countries, other treatments, news).
            - Do not browse or reference any page other than the sources listed in the knowledge base.
            - When you state a fact, mention the source page it came from.
            - If the knowledge base is empty or unavailable, say you cannot answer from the approved
              sources right now and suggest contacting PlacidWay for a free quote.
            """;

    private static final String LANGUAGE_RULES = """

            === LANGUAGE RULES (highest priority) ===
            - Detect the language of the user's latest message and reply in that SAME language.
            - This applies to every reply, including refusals, greetings, and clarifying questions.
            - Translate the refusal template into the user's language; do not reply in English unless
              the user wrote in English.
            - Keep prices, clinic names, treatment names, and source URLs unchanged.
            - If the user switches language mid-conversation, switch with them.
            """;

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
     * Builds the shared chat client with the medical system prompt, the loaded
     * knowledge base, and the memory advisor.
     *
     * @param chatModel     the auto-configured OpenAI-compatible chat model
     * @param chatMemory    the conversation memory
     * @param knowledgeBase the pages the assistant is allowed to answer from
     * @return the configured chat client
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory, KnowledgeBaseService knowledgeBase) {
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt(knowledgeBase))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    private String systemPrompt(KnowledgeBaseService knowledgeBase) {
        String content = knowledgeBase.content();
        String knowledge = content == null || content.isBlank()
                ? "(The knowledge base is currently unavailable.)"
                : content;
        String prompt = SYSTEM_PROMPT + "\n\n=== KNOWLEDGE BASE START ===\n"
                + knowledge + "\n=== KNOWLEDGE BASE END ===\n" + GROUNDING_RULES + LANGUAGE_RULES;
        log.debug("Built system prompt: knowledgeBaseChars={} promptChars={}",
                knowledge.length(), prompt.length());
        return prompt;
    }
}
