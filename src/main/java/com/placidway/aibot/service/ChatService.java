package com.placidway.aibot.service;

import reactor.core.publisher.Flux;

/**
 * Sends chat messages to the medical assistant.
 *
 * @author Husain Harmoni
 */
public interface ChatService {

    /**
     * Streams the assistant's reply for the given conversation.
     *
     * @param conversationId the conversation to keep memory for
     * @param message        the user's message text
     * @return a stream of reply text chunks
     */
    Flux<String> stream(String conversationId, String message);

    /**
     * Forgets the stored history for the given conversation.
     *
     * @param conversationId the conversation to clear
     */
    void clear(String conversationId);
}
