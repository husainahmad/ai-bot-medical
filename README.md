# ai-bot-medical

A medical information chatbot for PlacidWay, built with **Vaadin**, **Spring Boot**,
and **Spring AI**. It is a closed-domain assistant: it answers **only** from a fixed
set of approved PlacidWay pages about the ITC Immunity Therapy Center in Tijuana,
and it says "I don't know" (and suggests a free quote) for anything outside them.

There is no login or registration. Each browser session gets its own conversation,
and replies stream into the chat as they are generated.

---

## Features

- Streaming chat replies rendered incrementally in the browser.
- Per-session conversation memory (last 20 messages).
- Closed-domain grounding on 7 approved pages, loaded at startup.
- Honest refusals: never invents prices, clinics, doctors, or medical facts.
- Replies in the user's language, including refusals.
- Markdown rendering (headings, lists, tables, code) in assistant bubbles.
- "New chat" resets the conversation and memory.

---

## Tech stack, tools, and models

| Area | Choice |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.2 |
| UI | Vaadin 25.3.5 (server-side Java UI, `@Push` for streaming) |
| AI integration | Spring AI 2.0.1 (`spring-ai-starter-model-openai`) |
| LLM provider | Kenari (`https://kenari.id/v1`), OpenAI-compatible API |
| Default model | `agnes-3-0-flash:free` (512k context, tool support, free tier) |
| HTML extraction | jsoup 1.18.3 |
| Markdown | commonmark 0.24.0 + GFM tables extension |
| Boilerplate | Lombok |
| Build | Maven (wrapper included) |
| Tests | JUnit 5 + Spring Boot Test |

### Model notes

The provider is configured through the OpenAI-compatible starter, so any
OpenAI-compatible endpoint works. The default `agnes-3-0-flash:free` is fast and
follows the grounding rules well. Other free Kenari models with tool support
include `glm-4-7-flash:free` and `qwen3-8-27b:free`, but those emit reasoning
tokens and are less reliable for strict refusals. Override with `AI_MODEL`.

---

## Architecture

```
Browser (Vaadin UI)
   │  user message
   ▼
ChatView ──► ChatService (interface)
                 │
                 ▼
        SpringAiChatService
                 │  prompt + conversationId
                 ▼
        ChatClient (Spring AI)
          ├─ defaultSystem(system prompt + knowledge base + rules)
          ├─ MessageChatMemoryAdvisor (per-session history)
          └─ ChatModel ──► Kenari /v1/chat/completions (streaming)
                 │
                 ▼
        Flux<String> chunks ──► ChatView renders incrementally
```

At startup, `KnowledgeBaseService` fetches the 7 approved pages, extracts the
`<main>` content with jsoup, and caches the combined text. `ChatConfig` injects
that text into the system prompt together with the medical role, strict grounding
rules, and language rules.

### Key components

| Class | Responsibility |
| --- | --- |
| `AibotApplication` | Entry point; Vaadin theme, `@Push`, PWA config |
| `config/ChatConfig` | Builds the `ChatClient`, system prompt, and chat memory |
| `knowledge/KnowledgeBaseProperties` | Binds `app.knowledge-base.*` config |
| `knowledge/KnowledgeBaseService` | Fetches, extracts, and caches the approved pages |
| `service/ChatService` | Streaming chat abstraction |
| `service/SpringAiChatService` | Calls the model and streams chunks |
| `ui/ChatView` | Chat screen: header, conversation, composer, streaming |
| `ui/MainLayout` | App layout |
| `ui/component/*` | Message bubbles, markdown renderer, typing indicator, input, header |

### Conversation flow

1. `ChatView` assigns a conversation id per `VaadinSession`.
2. The user message is sent to `ChatService.stream(conversationId, message)`.
3. `SpringAiChatService` calls the `ChatClient` with the conversation id, so the
   memory advisor loads and appends the right history.
4. Chunks arrive as a `Flux<String>` and are flushed to the UI on the Vaadin
   thread (throttled to ~20 updates/second).
5. On completion or error, the final bubble is rendered and the input re-enabled.

---

## Setup

### Prerequisites

- JDK 21
- A Kenari API key (https://kenari.id)
- Internet access (the knowledge base is fetched at startup)

### 1. Configure

Copy the example config and set your key:

```bash
cp src/main/resources/application.yaml.example src/main/resources/application.yaml
export KENARI_API_KEY=kn-...
```

`application.yaml` is git-ignored because it may contain secrets.

### 2. Run

```bash
./mvnw spring-boot:run
```

Open http://localhost:8086.

### 3. Build a jar

```bash
./mvnw clean package
java -jar target/ai-bot-medical-0.0.1-SNAPSHOT.jar
```

### 4. Test

```bash
./mvnw test
```

---

## Configuration

All settings live in `src/main/resources/application.yaml`.

### AI provider

| Property | Env var | Default |
| --- | --- | --- |
| `spring.ai.openai.base-url` | `AI_BASE_URL` | `https://kenari.id/v1` |
| `spring.ai.openai.api-key` | `AI_API_KEY` / `KENARI_API_KEY` | — (required) |
| `spring.ai.openai.chat.options.model` | `AI_MODEL` | `agnes-3-0-flash:free` |
| `spring.ai.openai.chat.options.temperature` | — | `0.2` |

### Knowledge base

| Property | Default | Description |
| --- | --- | --- |
| `app.knowledge-base.enabled` | `true` | Turn grounding on/off |
| `app.knowledge-base.timeout-seconds` | `30` | Per-page fetch timeout |
| `app.knowledge-base.pages[].title` | — | Human-readable page name |
| `app.knowledge-base.pages[].url` | — | Page to fetch |

### Other

| Property | Default |
| --- | --- |
| `server.port` | `8086` |
| `app.assistant-name` | `Medical AI Assistant` |
| `logging.level.com.placidway.aibot` | `DEBUG` |

---

## Knowledge base

The bot is grounded **only** on these 7 pages:

1. ITC Immunity Therapy Center — clinic profile
2. Alternative Adenocarcinoma Cancer Treatment in Tijuana
3. Alternative Adrenal Cancer Treatment in Tijuana
4. Alternative Anal Cancer Treatment in Tijuana
5. ITC Tijuana Videos — alternative cancer treatment
6. ITC Immunity Therapy Center — price list
7. Alternative Medicine Cost Abroad — compare prices 2026

At startup each page is fetched, its `<main>` content is extracted, and the
combined text (~100k chars) is injected into the system prompt. The bot never
crawls beyond these pages. A page that fails to load is logged and skipped; the
app still starts.

To change the sources, edit `app.knowledge-base.pages` and restart.

---

## Guardrails

The system prompt enforces:

- **Closed domain** — answer only from the knowledge base.
- **Honest "I don't know"** — if the answer is not in the content, say so.
- **Free quote** — every refusal suggests contacting PlacidWay for a free quote.
- **No invention** — never fabricate prices, clinics, doctors, treatments, or facts.
- **Language** — reply in the user's language, including refusals.
- **Medical safety** — not a doctor; recommend professional care and emergency
  services for urgent symptoms.

---

## Project structure

```
src/main/java/com/placidway/aibot/
├── AibotApplication.java
├── config/ChatConfig.java
├── knowledge/
│   ├── KnowledgeBaseProperties.java
│   └── KnowledgeBaseService.java
├── service/
│   ├── ChatService.java
│   └── SpringAiChatService.java
└── ui/
    ├── ChatView.java
    ├── MainLayout.java
    └── component/
        ├── AiMessage.java
        ├── ChatInput.java
        ├── Header.java
        ├── MarkdownText.java
        ├── TypingIndicator.java
        └── UserMessage.java

src/main/resources/
├── application.yaml          # local config (git-ignored)
├── application.yaml.example  # template
└── frontend/themes/aibot/styles.css
```

---

## Troubleshooting

| Symptom | Cause / fix |
| --- | --- |
| `402` / usage error | Provider account out of credits; switch provider or top up. |
| `401` / unauthorized | Missing or invalid `KENARI_API_KEY`. |
| Bot says the knowledge base is unavailable | Pages failed to load at startup; check network and logs. |
| Replies are slow | Large system prompt (~25k tokens) plus model latency; try a faster model. |
| Off-topic answers | Lower `temperature`, or use a model that follows instructions better. |

---

## Notes

- Conversation memory is in-memory and per browser session. Use a persistent
  `ChatMemoryRepository` if history must survive restarts.
- The knowledge base is fetched on every startup; restart to pick up page changes.
- The assistant is not a substitute for professional medical advice.
