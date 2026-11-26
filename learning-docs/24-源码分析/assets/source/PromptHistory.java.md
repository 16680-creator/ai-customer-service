<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-session-advisor:2.1.0 | path: org/springframework/ai/session/advisor/PromptHistory.java -->
<!-- 正文引用：[[源码:PromptHistory.java#L起-L止]] ；代码块头部：(src: PromptHistory.java#L起-L止) -->

# PromptHistory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-session-advisor:2.1.0` |
| 路径 | `org/springframework/ai/session/advisor/PromptHistory.java` |
| 行数 | 276 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-session-advisor/2.1.0/spring-ai-session-advisor-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T17:09:29.495Z |

```java
    1 | /*
    2 |  * Copyright 2023-present the original author or authors.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *      https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | 
   17 | package org.springframework.ai.session.advisor;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Arrays;
   21 | import java.util.HashSet;
   22 | import java.util.List;
   23 | import java.util.Objects;
   24 | import java.util.Set;
   25 | 
   26 | import org.jspecify.annotations.Nullable;
   27 | import org.slf4j.Logger;
   28 | import org.slf4j.LoggerFactory;
   29 | 
   30 | import org.springframework.ai.chat.messages.AssistantMessage;
   31 | import org.springframework.ai.chat.messages.Message;
   32 | import org.springframework.ai.chat.messages.MessageType;
   33 | import org.springframework.ai.chat.messages.SystemMessage;
   34 | import org.springframework.ai.chat.messages.ToolResponseMessage;
   35 | import org.springframework.ai.chat.messages.UserMessage;
   36 | import org.springframework.ai.content.MediaContent;
   37 | import org.springframework.ai.session.SessionEvent;
   38 | 
   39 | /**
   40 |  * The history a {@link SessionMemoryAdvisor} sends with a model call, and how it becomes
   41 |  * the prompt, in two stages: {@link #of} picks the history from the session's active
   42 |  * view, the stored system prompt if any, the synthetic system messages a custom strategy
   43 |  * may write, and every other message as the conversation; {@link #prompt} makes the
   44 |  * prompt valid whatever the history, prepending only the part the request does not
   45 |  * already carry (a tool-calling loop re-sends the turn so far each round), dropping a
   46 |  * tool call or tool result left without its counterpart, and putting the system messages
   47 |  * first, each text once. Pure functions over messages: nothing here reads or writes the
   48 |  * session.
   49 |  *
   50 |  * @param system the system messages of the history, in log order
   51 |  * @param conversation the other messages of the history, in log order
   52 |  * @author Christian Tzolov
   53 |  * @since 2.1.0
   54 |  */
   55 | record PromptHistory(List<Message> system, List<Message> conversation) {
   56 | 
   57 | 	// The advisor's category, so its logging configuration keeps applying
   58 | 	private static final Logger logger = LoggerFactory.getLogger(SessionMemoryAdvisor.class);
   59 | 
   60 | 	PromptHistory {
   61 | 		system = List.copyOf(system);
   62 | 		conversation = List.copyOf(conversation);
   63 | 	}
   64 | 
   65 | 	/**
   66 | 	 * The history to send, from the events of the active view. A {@code ChatClient} that
   67 | 	 * sends its system prompt with each request stores none, so {@code systemPrompt} is
   68 | 	 * {@code null} and only synthetic summaries come from the log.
   69 | 	 * @param events the events read, oldest first
   70 | 	 * @param systemPrompt the system prompt stored in the session, the latest stored
   71 | 	 * system message, wherever it was found; {@code null} when there is none
   72 | 	 */
   73 | 	static PromptHistory of(List<SessionEvent> events, @Nullable Message systemPrompt) {
   74 | 		List<Message> system = new ArrayList<>();
   75 | 		if (systemPrompt != null) {
   76 | 			system.add(systemPrompt);
   77 | 		}
   78 | 		List<Message> conversation = new ArrayList<>();
   79 | 		for (SessionEvent event : events) {
   80 | 			if (event.getMessageType() != MessageType.SYSTEM) {
   81 | 				conversation.add(event.getMessage());
   82 | 			}
   83 | 			else if (event.isSynthetic()) {
   84 | 				system.add(event.getMessage());
   85 | 			}
   86 | 			// a stored system message is the system prompt (passed in) or superseded
   87 | 		}
   88 | 		return new PromptHistory(system, conversation);
   89 | 	}
   90 | 
   91 | 	/**
   92 | 	 * The latest stored system message among the events: the session's system prompt.
   93 | 	 * Earlier ones are superseded; synthetic ones do not count.
   94 | 	 */
   95 | 	static @Nullable SessionEvent latestStoredSystemPrompt(List<SessionEvent> events) {
   96 | 		for (int i = events.size() - 1; i >= 0; i--) {
   97 | 			SessionEvent event = events.get(i);
   98 | 			if (!event.isSynthetic() && event.getMessageType() == MessageType.SYSTEM) {
   99 | 				return event;
  100 | 			}
  101 | 		}
  102 | 		return null;
  103 | 	}
  104 | 
  105 | 	/**
  106 | 	 * The prompt for the request's messages: the history the request does not carry, then
  107 | 	 * the request's messages, with every system message first.
  108 | 	 * @param requestMessages the messages of the request, as the tool-calling loop or the
  109 | 	 * application sends them
  110 | 	 * @param sessionId the session, for the warning about dropped tool messages
  111 | 	 */
  112 | 	List<Message> prompt(List<Message> requestMessages, String sessionId) {
  113 | 		List<Message> requestConversation = requestMessages.stream()
  114 | 			.filter(message -> !(message instanceof SystemMessage))
  115 | 			.toList();
  116 | 		// A tool-calling loop re-sends the turn so far, which the log ends with: prepend
  117 | 		// only what comes before that overlap. System messages are left out on both
  118 | 		// sides, they all go first
  119 | 		List<Message> missing = this.conversation.subList(0,
  120 | 				this.conversation.size() - overlap(this.conversation, requestConversation));
  121 | 
  122 | 		List<Message> prompt = new ArrayList<>(this.system);
  123 | 		prompt.addAll(withoutOrphanToolMessages(missing, requestConversation, sessionId));
  124 | 		prompt.addAll(requestMessages);
  125 | 
  126 | 		// Every system message first, in order, each text once: a stored system prompt
  127 | 		// the request also sends goes once. Texts are never merged or rewritten, so the
  128 | 		// prompt stays byte-stable for prompt caching. A blank one, the tool-calling
  129 | 		// loop's stand-in for a request without a system prompt, is not sent at all
  130 | 		Set<String> seenSystemTexts = new HashSet<>();
  131 | 		List<Message> systemMessages = prompt.stream()
  132 | 			.filter(SystemMessage.class::isInstance)
  133 | 			.filter(message -> message.getText() != null && !message.getText().isBlank())
  134 | 			.filter(message -> seenSystemTexts.add(message.getText()))
  135 | 			.toList();
  136 | 		prompt.removeIf(SystemMessage.class::isInstance);
  137 | 		prompt.addAll(0, systemMessages);
  138 | 		return prompt;
  139 | 	}
  140 | 
  141 | 	/** The last user or tool response message, or {@code null} when there is none. */
  142 | 	static @Nullable Message lastUserOrToolResponse(List<Message> messages) {
  143 | 		for (int i = messages.size() - 1; i >= 0; i--) {
  144 | 			Message message = messages.get(i);
  145 | 			if (message instanceof UserMessage || message instanceof ToolResponseMessage) {
  146 | 				return message;
  147 | 			}
  148 | 		}
  149 | 		return null;
  150 | 	}
  151 | 
  152 | 	/**
  153 | 	 * How many of the last {@code history} messages the request starts with: the largest
  154 | 	 * {@code k} for which the history's last {@code k} messages are the request's first
  155 | 	 * {@code k}, compared by {@link #sameContent}.
  156 | 	 */
  157 | 	private static int overlap(List<Message> history, List<Message> request) {
  158 | 		// Only a tool-calling round re-sends the turn so far, and it carries more than a
  159 | 		// user message; a request that is one user message is new, even when the user
  160 | 		// repeats what the log ends with
  161 | 		if (request.size() < 2) {
  162 | 			return 0;
  163 | 		}
  164 | 		for (int k = Math.min(history.size(), request.size()); k > 0; k--) {
  165 | 			int offset = history.size() - k;
  166 | 			int i = 0;
  167 | 			while (i < k && sameContent(history.get(offset + i), request.get(i))) {
  168 | 				i++;
  169 | 			}
  170 | 			if (i == k) {
  171 | 				return k;
  172 | 			}
  173 | 		}
  174 | 		return 0;
  175 | 	}
  176 | 
  177 | 	/**
  178 | 	 * Compares two messages by what the model sees rather than by {@code equals}: the
  179 | 	 * type, the text, the tool calls and the tool responses. A repository may not
  180 | 	 * round-trip everything else (e.g. {@code JdbcSessionRepository} stores neither
  181 | 	 * metadata such as the finish reason nor media), so a message reloaded from the
  182 | 	 * session would otherwise never equal the one the tool-calling loop re-sends. Media
  183 | 	 * is compared only when both messages carry it.
  184 | 	 */
  185 | 	private static boolean sameContent(Message a, Message b) {
  186 | 		if (a == b) {
  187 | 			return true;
  188 | 		}
  189 | 		if (a.getMessageType() != b.getMessageType()
  190 | 				|| !Objects.requireNonNullElse(a.getText(), "").equals(Objects.requireNonNullElse(b.getText(), ""))) {
  191 | 			return false;
  192 | 		}
  193 | 		if (a instanceof AssistantMessage assistantA && b instanceof AssistantMessage assistantB
  194 | 				&& !assistantA.getToolCalls().equals(assistantB.getToolCalls())) {
  195 | 			return false;
  196 | 		}
  197 | 		if (a instanceof ToolResponseMessage toolA && b instanceof ToolResponseMessage toolB
  198 | 				&& !toolA.getResponses().equals(toolB.getResponses())) {
  199 | 			return false;
  200 | 		}
  201 | 		if (a instanceof MediaContent mediaA && b instanceof MediaContent mediaB && !mediaA.getMedia().isEmpty()
  202 | 				&& !mediaB.getMedia().isEmpty() && !mediaA.getMedia().equals(mediaB.getMedia())) {
  203 | 			return false;
  204 | 		}
  205 | 		return true;
  206 | 	}
  207 | 
  208 | 	/**
  209 | 	 * Returns {@code history} without tool messages that have no counterpart: an
  210 | 	 * assistant message whose tool calls are not all answered by the tool responses that
  211 | 	 * directly follow it (in the history, then in {@code following}, the request's own
  212 | 	 * messages), or a tool response that does not answer the assistant tool call directly
  213 | 	 * before it. Calls and responses are matched by id; one response message may answer
  214 | 	 * several parallel calls, and a partially answered call is dropped together with its
  215 | 	 * responses. The request's messages are never dropped.
  216 | 	 */
  217 | 	private static List<Message> withoutOrphanToolMessages(List<Message> history, List<Message> following,
  218 | 			String sessionId) {
  219 | 		if (history.stream()
  220 | 			.noneMatch(m -> m instanceof ToolResponseMessage
  221 | 					|| (m instanceof AssistantMessage assistant && assistant.hasToolCalls()))) {
  222 | 			return history;
  223 | 		}
  224 | 		List<Message> all = new ArrayList<>(history);
  225 | 		all.addAll(following);
  226 | 		boolean[] keep = new boolean[history.size()];
  227 | 		Arrays.fill(keep, true);
  228 | 		for (int i = 0; i < history.size(); i++) {
  229 | 			Message message = history.get(i);
  230 | 			if (message instanceof AssistantMessage assistant && assistant.hasToolCalls()) {
  231 | 				Set<String> unanswered = new HashSet<>();
  232 | 				assistant.getToolCalls().forEach(call -> unanswered.add(call.id()));
  233 | 				for (int j = i + 1; j < all.size() && !unanswered.isEmpty(); j++) {
  234 | 					if (!(all.get(j) instanceof ToolResponseMessage responses)) {
  235 | 						break;
  236 | 					}
  237 | 					responses.getResponses().forEach(response -> unanswered.remove(response.id()));
  238 | 				}
  239 | 				keep[i] = unanswered.isEmpty();
  240 | 			}
  241 | 			else if (message instanceof ToolResponseMessage responses) {
  242 | 				int call = i - 1;
  243 | 				while (call >= 0 && history.get(call) instanceof ToolResponseMessage) {
  244 | 					call--;
  245 | 				}
  246 | 				keep[i] = call >= 0 && keep[call] && history.get(call) instanceof AssistantMessage assistant
  247 | 						&& assistant.hasToolCalls() && answers(assistant, responses);
  248 | 			}
  249 | 		}
  250 | 		List<Message> kept = new ArrayList<>(history.size());
  251 | 		int dropped = 0;
  252 | 		for (int i = 0; i < history.size(); i++) {
  253 | 			if (keep[i]) {
  254 | 				kept.add(history.get(i));
  255 | 			}
  256 | 			else {
  257 | 				dropped++;
  258 | 			}
  259 | 		}
  260 | 		if (dropped > 0) {
  261 | 			logger.warn("Dropped {} tool message(s) without their counterpart from the prompt for session [{}]. "
  262 | 					+ "A messageTypes filter that hides TOOL events, a log an earlier version wrote with a "
  263 | 					+ "MessageFilter that skipped them, or a loop that failed mid-way is the likely cause; the "
  264 | 					+ "session log is unchanged", dropped, sessionId);
  265 | 		}
  266 | 		return kept;
  267 | 	}
  268 | 
  269 | 	/** Whether every response answers one of the assistant's tool calls. */
  270 | 	private static boolean answers(AssistantMessage assistant, ToolResponseMessage responses) {
  271 | 		Set<String> callIds = new HashSet<>();
  272 | 		assistant.getToolCalls().forEach(call -> callIds.add(call.id()));
  273 | 		return responses.getResponses().stream().allMatch(response -> callIds.contains(response.id()));
  274 | 	}
  275 | 
  276 | }
```
