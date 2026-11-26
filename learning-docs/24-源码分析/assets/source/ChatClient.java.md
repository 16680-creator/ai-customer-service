<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/ChatClient.java -->
<!-- 正文引用：[[源码:ChatClient.java#L起-L止]] ；代码块头部：(src: ChatClient.java#L起-L止) -->

# ChatClient.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/ChatClient.java` |
| 行数 | 637 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:54.343Z |

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
   17 | package org.springframework.ai.chat.client;
   18 | 
   19 | import java.net.URL;
   20 | import java.nio.charset.Charset;
   21 | import java.util.Collection;
   22 | import java.util.List;
   23 | import java.util.Map;
   24 | import java.util.function.Consumer;
   25 | 
   26 | import io.micrometer.observation.ObservationRegistry;
   27 | import org.jspecify.annotations.Nullable;
   28 | import reactor.core.publisher.Flux;
   29 | 
   30 | import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
   31 | import org.springframework.ai.chat.client.advisor.api.Advisor;
   32 | import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
   33 | import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
   34 | import org.springframework.ai.chat.messages.Message;
   35 | import org.springframework.ai.chat.model.ChatModel;
   36 | import org.springframework.ai.chat.model.ChatResponse;
   37 | import org.springframework.ai.chat.prompt.ChatOptions;
   38 | import org.springframework.ai.chat.prompt.Prompt;
   39 | import org.springframework.ai.content.Media;
   40 | import org.springframework.ai.converter.StructuredOutputConverter;
   41 | import org.springframework.ai.template.TemplateRenderer;
   42 | import org.springframework.ai.tool.ToolCallback;
   43 | import org.springframework.ai.tool.ToolCallbackProvider;
   44 | import org.springframework.core.ParameterizedTypeReference;
   45 | import org.springframework.core.io.Resource;
   46 | import org.springframework.util.Assert;
   47 | import org.springframework.util.MimeType;
   48 | 
   49 | /**
   50 |  * Client to perform stateless requests to an AI Model, using a fluent API.
   51 |  * <p>
   52 |  * Use {@link ChatClient#builder(ChatModel)} to prepare an instance.
   53 |  *
   54 |  * @author Mark Pollack
   55 |  * @author Christian Tzolov
   56 |  * @author Josh Long
   57 |  * @author Arjen Poutsma
   58 |  * @author Thomas Vitale
   59 |  * @since 1.0.0
   60 |  */
   61 | public interface ChatClient {
   62 | 
   63 | 	static ChatClient create(ChatModel chatModel) {
   64 | 		return create(chatModel, ObservationRegistry.NOOP);
   65 | 	}
   66 | 
   67 | 	static ChatClient create(ChatModel chatModel, ObservationRegistry observationRegistry) {
   68 | 		return create(chatModel, observationRegistry, null, null);
   69 | 	}
   70 | 
   71 | 	static ChatClient create(ChatModel chatModel, ObservationRegistry observationRegistry,
   72 | 			@Nullable ChatClientObservationConvention chatClientObservationConvention,
   73 | 			@Nullable AdvisorObservationConvention advisorObservationConvention) {
   74 | 		Assert.notNull(chatModel, "chatModel cannot be null");
   75 | 		Assert.notNull(observationRegistry, "observationRegistry cannot be null");
   76 | 		return builder(chatModel, observationRegistry, chatClientObservationConvention, advisorObservationConvention)
   77 | 			.build();
   78 | 	}
   79 | 
   80 | 	static Builder builder(ChatModel chatModel) {
   81 | 		return builder(chatModel, ObservationRegistry.NOOP, null, null);
   82 | 	}
   83 | 
   84 | 	static Builder builder(ChatModel chatModel, ObservationRegistry observationRegistry,
   85 | 			@Nullable ChatClientObservationConvention chatClientObservationConvention,
   86 | 			@Nullable AdvisorObservationConvention advisorObservationConvention) {
   87 | 		return builder(chatModel, observationRegistry, chatClientObservationConvention, advisorObservationConvention,
   88 | 				null);
   89 | 	}
   90 | 
   91 | 	/**
   92 | 	 * Creates a {@link Builder} for constructing a {@link ChatClient}.
   93 | 	 * <p>
   94 | 	 * When {@code toolCallingAdvisorBuilder} is {@code null}, a default
   95 | 	 * {@link org.springframework.ai.chat.client.advisor.ToolCallingAdvisor} is created
   96 | 	 * with a {@link org.springframework.ai.model.tool.ToolCallingManager} backed by the
   97 | 	 * supplied {@code observationRegistry}.
   98 | 	 * <p>
   99 | 	 * When {@code toolCallingAdvisorBuilder} is non-null it is used as-is. The caller is
  100 | 	 * then responsible for configuring the builder's
  101 | 	 * {@link org.springframework.ai.model.tool.ToolCallingManager}, including any
  102 | 	 * {@link io.micrometer.observation.ObservationRegistry}, since the supplied
  103 | 	 * {@code observationRegistry} will not be automatically applied to it.
  104 | 	 * @param chatModel the chat model to use
  105 | 	 * @param observationRegistry the observation registry for client-level observations;
  106 | 	 * also used to configure the default {@code ToolCallingManager} when
  107 | 	 * {@code toolCallingAdvisorBuilder} is {@code null}
  108 | 	 * @param chatClientObservationConvention optional custom observation convention for
  109 | 	 * the chat client
  110 | 	 * @param advisorObservationConvention optional custom observation convention for
  111 | 	 * advisors
  112 | 	 * @param toolCallingAdvisorBuilder optional builder for the
  113 | 	 * {@link org.springframework.ai.chat.client.advisor.ToolCallingAdvisor}; when
  114 | 	 * {@code null} a default is created
  115 | 	 * @return a new {@link Builder}
  116 | 	 */
  117 | 	static Builder builder(ChatModel chatModel, ObservationRegistry observationRegistry,
  118 | 			@Nullable ChatClientObservationConvention chatClientObservationConvention,
  119 | 			@Nullable AdvisorObservationConvention advisorObservationConvention,
  120 | 			ToolCallingAdvisor.@Nullable Builder<?> toolCallingAdvisorBuilder) {
  121 | 		Assert.notNull(chatModel, "chatModel cannot be null");
  122 | 		Assert.notNull(observationRegistry, "observationRegistry cannot be null");
  123 | 
  124 | 		return new DefaultChatClientBuilder(chatModel, observationRegistry, chatClientObservationConvention,
  125 | 				advisorObservationConvention, toolCallingAdvisorBuilder);
  126 | 	}
  127 | 
  128 | 	ChatClientRequestSpec prompt();
  129 | 
  130 | 	ChatClientRequestSpec prompt(String content);
  131 | 
  132 | 	ChatClientRequestSpec prompt(Prompt prompt);
  133 | 
  134 | 	/**
  135 | 	 * Return a {@link ChatClient.Builder} to create a new {@link ChatClient} whose
  136 | 	 * settings are replicated from the default {@link ChatClientRequestSpec} of this
  137 | 	 * client.
  138 | 	 */
  139 | 	Builder mutate();
  140 | 
  141 | 	interface PromptUserSpec {
  142 | 
  143 | 		PromptUserSpec text(String text);
  144 | 
  145 | 		PromptUserSpec text(Resource text, Charset charset);
  146 | 
  147 | 		PromptUserSpec text(Resource text);
  148 | 
  149 | 		PromptUserSpec params(Map<String, Object> p);
  150 | 
  151 | 		PromptUserSpec param(String k, Object v);
  152 | 
  153 | 		PromptUserSpec media(Media... media);
  154 | 
  155 | 		PromptUserSpec media(MimeType mimeType, URL url);
  156 | 
  157 | 		PromptUserSpec media(MimeType mimeType, Resource resource);
  158 | 
  159 | 		PromptUserSpec metadata(Map<String, Object> metadata);
  160 | 
  161 | 		PromptUserSpec metadata(String k, Object v);
  162 | 
  163 | 	}
  164 | 
  165 | 	/**
  166 | 	 * Specification for a prompt system.
  167 | 	 */
  168 | 	interface PromptSystemSpec {
  169 | 
  170 | 		PromptSystemSpec text(String text);
  171 | 
  172 | 		PromptSystemSpec text(Resource text, Charset charset);
  173 | 
  174 | 		PromptSystemSpec text(Resource text);
  175 | 
  176 | 		PromptSystemSpec params(Map<String, Object> p);
  177 | 
  178 | 		PromptSystemSpec param(String k, Object v);
  179 | 
  180 | 		PromptSystemSpec metadata(Map<String, Object> metadata);
  181 | 
  182 | 		PromptSystemSpec metadata(String k, Object v);
  183 | 
  184 | 	}
  185 | 
  186 | 	interface AdvisorSpec {
  187 | 
  188 | 		AdvisorSpec param(String k, Object v);
  189 | 
  190 | 		AdvisorSpec params(Map<String, Object> p);
  191 | 
  192 | 		AdvisorSpec advisors(Advisor... advisors);
  193 | 
  194 | 		AdvisorSpec advisors(List<Advisor> advisors);
  195 | 
  196 | 	}
  197 | 
  198 | 	/**
  199 | 	 * Configures optional behaviour for {@code entity(...)} calls. Options may be
  200 | 	 * combined.
  201 | 	 */
  202 | 	interface EntityParamSpec {
  203 | 
  204 | 		/**
  205 | 		 * Delivers the JSON schema to the AI provider as an API-level constraint rather
  206 | 		 * than appending it as prompt text. Has no effect if the underlying
  207 | 		 * {@link org.springframework.ai.chat.model.ChatModel} does not support
  208 | 		 * {@link org.springframework.ai.model.tool.StructuredOutputChatOptions}.
  209 | 		 *
  210 | 		 * <p>
  211 | 		 * <b>Not enabled by default</b> because native structured output support varies
  212 | 		 * across models and providers. Known limitations:
  213 | 		 * <ul>
  214 | 		 * <li><b>Ollama</b>: models with a built-in reasoning/thinking mode (e.g.
  215 | 		 * {@code qwen3:8b}, {@code qwen3.5:9b}) may return plain text instead of JSON,
  216 | 		 * causing deserialization failures. Use {@link #validateSchema()} alongside this
  217 | 		 * option for automatic retry, or switch to a non-reasoning model such as
  218 | 		 * {@code llama3.1:latest}.</li>
  219 | 		 * <li><b>OpenAI</b>: the Structured Outputs API does not accept a top-level JSON
  220 | 		 * array schema. Requesting a {@code List<T>} with this option enabled will fail.
  221 | 		 * Wrap the list in a container record or use the default prompt-based approach
  222 | 		 * instead.</li>
  223 | 		 * </ul>
  224 | 		 */
  225 | 		EntityParamSpec useProviderStructuredOutput();
  226 | 
  227 | 		/**
  228 | 		 * Validates the model's JSON response against the entity schema and retries with
  229 | 		 * the error feedback on failure, up to {@code maxRepeatAttempts} times (default:
  230 | 		 * 3). Streaming is not supported.
  231 | 		 */
  232 | 		EntityParamSpec validateSchema();
  233 | 
  234 | 	}
  235 | 
  236 | 	interface CallResponseSpec {
  237 | 
  238 | 		/**
  239 | 		 * Deserializes the response into a {@code T} instance, with behaviour configured
  240 | 		 * via the {@code entityParamSpecConsumer}.
  241 | 		 * @param type the target parameterized type
  242 | 		 * @param entityParamSpecConsumer configures options such as
  243 | 		 * {@link EntityParamSpec#useProviderStructuredOutput()} and
  244 | 		 * {@link EntityParamSpec#validateSchema()}
  245 | 		 * @return the deserialized entity, or {@code null} if the response is empty
  246 | 		 * @throws GuardrailRefusalException if a guardrail refused the turn, so that
  247 | 		 * there is no answer of the model to deserialize
  248 | 		 * @throws ContentFilteredException if a safety filter or a refusal of the model
  249 | 		 * stopped the response, and what it left is empty or cannot be converted
  250 | 		 */
  251 | 		<T> @Nullable T entity(ParameterizedTypeReference<T> type, Consumer<EntityParamSpec> entityParamSpecConsumer);
  252 | 
  253 | 		/**
  254 | 		 * Deserializes the response into a {@code T} instance.
  255 | 		 * @param type the target parameterized type
  256 | 		 * @return the deserialized entity, or {@code null} if the response is empty
  257 | 		 * @throws GuardrailRefusalException if a guardrail refused the turn, so that
  258 | 		 * there is no answer of the model to deserialize
  259 | 		 * @throws ContentFilteredException if a safety filter or a refusal of the model
  260 | 		 * stopped the response, and what it left is empty or cannot be converted
  261 | 		 */
  262 | 		<T> @Nullable T entity(ParameterizedTypeReference<T> type);
  263 | 
  264 | 		/**
  265 | 		 * Deserializes the response using the given converter, with behaviour configured
  266 | 		 * via the {@code entityParamSpecConsumer}.
  267 | 		 * @param structuredOutputConverter the converter for parsing and schema
  268 | 		 * resolution
  269 | 		 * @param entityParamSpecConsumer configures options such as
  270 | 		 * {@link EntityParamSpec#useProviderStructuredOutput()} and
  271 | 		 * {@link EntityParamSpec#validateSchema()}
  272 | 		 * @return the deserialized entity, or {@code null} if the response is empty
  273 | 		 * @throws GuardrailRefusalException if a guardrail refused the turn, so that
  274 | 		 * there is no answer of the model to deserialize
  275 | 		 * @throws ContentFilteredException if a safety filter or a refusal of the model
  276 | 		 * stopped the response, and what it left is empty or cannot be converted
  277 | 		 */
  278 | 		<T> @Nullable T entity(StructuredOutputConverter<T> structuredOutputConverter,
  279 | 				Consumer<EntityParamSpec> entityParamSpecConsumer);
  280 | 
  281 | 		/**
  282 | 		 * Deserializes the response using the given converter.
  283 | 		 * @param structuredOutputConverter the converter for parsing and schema
  284 | 		 * resolution
  285 | 		 * @return the deserialized entity, or {@code null} if the response is empty
  286 | 		 * @throws GuardrailRefusalException if a guardrail refused the turn, so that
  287 | 		 * there is no answer of the model to deserialize
  288 | 		 * @throws ContentFilteredException if a safety filter or a refusal of the model
  289 | 		 * stopped the response, and what it left is empty or cannot be converted
  290 | 		 */
  291 | 		<T> @Nullable T entity(StructuredOutputConverter<T> structuredOutputConverter);
  292 | 
  293 | 		/**
  294 | 		 * Deserializes the response into a {@code T} instance, with behaviour configured
  295 | 		 * via the {@code entityParamSpecConsumer}.
  296 | 		 * @param type the target class
  297 | 		 * @param entityParamSpecConsumer configures options such as
  298 | 		 * {@link EntityParamSpec#useProviderStructuredOutput()} and
  299 | 		 * {@link EntityParamSpec#validateSchema()}
  300 | 		 * @return the deserialized entity, or {@code null} if the response is empty
  301 | 		 * @throws GuardrailRefusalException if a guardrail refused the turn, so that
  302 | 		 * there is no answer of the model to deserialize
  303 | 		 * @throws ContentFilteredException if a safety filter or a refusal of the model
  304 | 		 * stopped the response, and what it left is empty or cannot be converted
  305 | 		 */
  306 | 		<T> @Nullable T entity(Class<T> type, Consumer<EntityParamSpec> entityParamSpecConsumer);
  307 | 
  308 | 		/**
  309 | 		 * Deserializes the response into a {@code T} instance.
  310 | 		 * @param type the target class
  311 | 		 * @return the deserialized entity, or {@code null} if the response is empty
  312 | 		 * @throws GuardrailRefusalException if a guardrail refused the turn, so that
  313 | 		 * there is no answer of the model to deserialize
  314 | 		 * @throws ContentFilteredException if a safety filter or a refusal of the model
  315 | 		 * stopped the response, and what it left is empty or cannot be converted
  316 | 		 */
  317 | 		<T> @Nullable T entity(Class<T> type);
  318 | 
  319 | 		ChatClientResponse chatClientResponse();
  320 | 
  321 | 		@Nullable ChatResponse chatResponse();
  322 | 
  323 | 		@Nullable String content();
  324 | 
  325 | 		/**
  326 | 		 * Returns a {@link ResponseEntity} containing both the complete
  327 | 		 * {@link ChatResponse} object and a specific entity type, with behaviour
  328 | 		 * configured via the {@code entityParamSpecConsumer}.
  329 | 		 * @param type the target class
  330 | 		 * @param entityParamSpecConsumer configures options such as
  331 | 		 * {@link EntityParamSpec#useProviderStructuredOutput()} and
  332 | 		 * {@link EntityParamSpec#validateSchema()}
  333 | 		 * @return the {@link ResponseEntity} containing both the complete
  334 | 		 * {@link ChatResponse} object and the deserialized entity. The entity is
  335 | 		 * {@code null} when a guardrail refused the turn, and when a safety filter or a
  336 | 		 * refusal of the model stopped the response and what it left is empty or cannot
  337 | 		 * be converted. The response then holds the refusal, or the finish reason
  338 | 		 */
  339 | 		<T> ResponseEntity<ChatResponse, T> responseEntity(Class<T> type,
  340 | 				Consumer<EntityParamSpec> entityParamSpecConsumer);
  341 | 
  342 | 		/**
  343 | 		 * Returns a {@link ResponseEntity} containing both the complete
  344 | 		 * {@link ChatResponse} object and a specific entity type.
  345 | 		 * @param type the target class
  346 | 		 * @return the {@link ResponseEntity} containing both the complete
  347 | 		 * {@link ChatResponse} object and the deserialized entity. The entity is
  348 | 		 * {@code null} when a guardrail refused the turn, and when a safety filter or a
  349 | 		 * refusal of the model stopped the response and what it left is empty or cannot
  350 | 		 * be converted. The response then holds the refusal, or the finish reason
  351 | 		 */
  352 | 		<T> ResponseEntity<ChatResponse, T> responseEntity(Class<T> type);
  353 | 
  354 | 		/**
  355 | 		 * Returns a {@link ResponseEntity} containing both the complete
  356 | 		 * {@link ChatResponse} object and a specific entity type, with behaviour
  357 | 		 * configured via the {@code entityParamSpecConsumer}.
  358 | 		 * @param type the target parameterized type
  359 | 		 * @param entityParamSpecConsumer configures options such as
  360 | 		 * {@link EntityParamSpec#useProviderStructuredOutput()} and
  361 | 		 * {@link EntityParamSpec#validateSchema()}
  362 | 		 * @return the {@link ResponseEntity} containing both the complete
  363 | 		 * {@link ChatResponse} object and the deserialized entity. The entity is
  364 | 		 * {@code null} when a guardrail refused the turn, and when a safety filter or a
  365 | 		 * refusal of the model stopped the response and what it left is empty or cannot
  366 | 		 * be converted. The response then holds the refusal, or the finish reason
  367 | 		 */
  368 | 		<T> ResponseEntity<ChatResponse, T> responseEntity(ParameterizedTypeReference<T> type,
  369 | 				Consumer<EntityParamSpec> entityParamSpecConsumer);
  370 | 
  371 | 		/**
  372 | 		 * Returns a {@link ResponseEntity} containing both the complete
  373 | 		 * {@link ChatResponse} object and a {@link Collection} of entity types.
  374 | 		 * @param type the target parameterized type
  375 | 		 * @return the {@link ResponseEntity} containing both the complete
  376 | 		 * {@link ChatResponse} object and the deserialized entities. The entity is
  377 | 		 * {@code null} when a guardrail refused the turn, and when a safety filter or a
  378 | 		 * refusal of the model stopped the response and what it left is empty or cannot
  379 | 		 * be converted. The response then holds the refusal, or the finish reason
  380 | 		 */
  381 | 		<T> ResponseEntity<ChatResponse, T> responseEntity(ParameterizedTypeReference<T> type);
  382 | 
  383 | 		/**
  384 | 		 * Returns a {@link ResponseEntity} containing both the complete
  385 | 		 * {@link ChatResponse} object and an entity converted using a specified
  386 | 		 * {@link StructuredOutputConverter}, with behaviour configured via the
  387 | 		 * {@code entityParamSpecConsumer}.
  388 | 		 * @param structuredOutputConverter the converter for parsing and schema
  389 | 		 * resolution
  390 | 		 * @param entityParamSpecConsumer configures options such as
  391 | 		 * {@link EntityParamSpec#useProviderStructuredOutput()} and
  392 | 		 * {@link EntityParamSpec#validateSchema()}
  393 | 		 * @return the {@link ResponseEntity} containing both the complete
  394 | 		 * {@link ChatResponse} object and the deserialized entity. The entity is
  395 | 		 * {@code null} when a guardrail refused the turn, and when a safety filter or a
  396 | 		 * refusal of the model stopped the response and what it left is empty or cannot
  397 | 		 * be converted. The response then holds the refusal, or the finish reason
  398 | 		 */
  399 | 		<T> ResponseEntity<ChatResponse, T> responseEntity(StructuredOutputConverter<T> structuredOutputConverter,
  400 | 				Consumer<EntityParamSpec> entityParamSpecConsumer);
  401 | 
  402 | 		/**
  403 | 		 * Returns a {@link ResponseEntity} containing both the complete
  404 | 		 * {@link ChatResponse} object and an entity converted using a specified
  405 | 		 * {@link StructuredOutputConverter}.
  406 | 		 * @param structuredOutputConverter the converter for parsing and schema
  407 | 		 * resolution
  408 | 		 * @return the {@link ResponseEntity} containing both the complete
  409 | 		 * {@link ChatResponse} object and the deserialized entity. The entity is
  410 | 		 * {@code null} when a guardrail refused the turn, and when a safety filter or a
  411 | 		 * refusal of the model stopped the response and what it left is empty or cannot
  412 | 		 * be converted. The response then holds the refusal, or the finish reason
  413 | 		 */
  414 | 		<T> ResponseEntity<ChatResponse, T> responseEntity(StructuredOutputConverter<T> structuredOutputConverter);
  415 | 
  416 | 	}
  417 | 
  418 | 	interface StreamResponseSpec {
  419 | 
  420 | 		Flux<ChatClientResponse> chatClientResponse();
  421 | 
  422 | 		Flux<ChatResponse> chatResponse();
  423 | 
  424 | 		Flux<String> content();
  425 | 
  426 | 	}
  427 | 
  428 | 	interface ChatClientRequestSpec {
  429 | 
  430 | 		/**
  431 | 		 * Return a {@link ChatClient.Builder} to create a new {@link ChatClient} whose
  432 | 		 * settings are replicated from this {@link ChatClientRequest}.
  433 | 		 */
  434 | 		Builder mutate();
  435 | 
  436 | 		ChatClientRequestSpec advisors(Consumer<AdvisorSpec> consumer);
  437 | 
  438 | 		ChatClientRequestSpec advisors(Advisor... advisors);
  439 | 
  440 | 		ChatClientRequestSpec advisors(List<Advisor> advisors);
  441 | 
  442 | 		ChatClientRequestSpec messages(Message... messages);
  443 | 
  444 | 		ChatClientRequestSpec messages(List<Message> messages);
  445 | 
  446 | 		<B extends ChatOptions.Builder<?>> ChatClientRequestSpec options(B customizer);
  447 | 
  448 | 		/**
  449 | 		 * Register one or more tools for this chat request. The method accepts a
  450 | 		 * heterogeneous mix of tool representations and routes each element to the
  451 | 		 * appropriate internal list automatically:
  452 | 		 *
  453 | 		 * <ul>
  454 | 		 * <li>{@link org.springframework.ai.tool.ToolCallback} — registered directly as a
  455 | 		 * callback.</li>
  456 | 		 * <li>{@link org.springframework.ai.tool.ToolCallbackProvider} — registered
  457 | 		 * directly as a provider; its callbacks are resolved lazily at request time.</li>
  458 | 		 * <li>{@code ToolCallback[]} or {@code ToolCallbackProvider[]} — every element of
  459 | 		 * the array is registered as above.</li>
  460 | 		 * <li>{@link java.util.Collection} — iterated and each element is dispatched by
  461 | 		 * the same rules.</li>
  462 | 		 * <li>Any other object — treated as a {@code @Tool}-annotated POJO; a
  463 | 		 * {@link org.springframework.ai.tool.ToolCallback} is generated for each
  464 | 		 * {@link org.springframework.ai.tool.annotation.Tool}-annotated method it
  465 | 		 * contains.</li>
  466 | 		 * </ul>
  467 | 		 *
  468 | 		 * <p>
  469 | 		 * Mixed calls are fully supported:
  470 | 		 *
  471 | 		 * <pre>{@code
  472 | 		 * chatClient.prompt()
  473 | 		 *     .tools(new DateTimeTools(), existingCallback, myProvider)
  474 | 		 *     .toolContext(Map.of("tenantId", "acme"))
  475 | 		 *     .call().content();
  476 | 		 * }</pre>
  477 | 		 *
  478 | 		 * <p>
  479 | 		 * Tools registered here are available only for this specific request. Use
  480 | 		 * {@link Builder#defaultTools(Object...)} to register tools that apply to every
  481 | 		 * request built from the same {@link Builder}.
  482 | 		 * @param tools tool objects to register; must not be {@code null} and must not
  483 | 		 * contain {@code null} elements
  484 | 		 * @return this spec for chaining
  485 | 		 * @throws IllegalArgumentException if {@code tools} is {@code null}, contains
  486 | 		 * {@code null} elements, or if a POJO argument has no
  487 | 		 * {@link org.springframework.ai.tool.annotation.Tool}-annotated methods
  488 | 		 */
  489 | 		ChatClientRequestSpec tools(Object... tools);
  490 | 
  491 | 		/**
  492 | 		 * @deprecated as of 2.0.0, in favor of {@link #tools(Object...)}. To be removed
  493 | 		 * in 3.0.0.
  494 | 		 */
  495 | 		@Deprecated(since = "2.0.0", forRemoval = true)
  496 | 		ChatClientRequestSpec toolCallbacks(ToolCallback... toolCallbacks);
  497 | 
  498 | 		/**
  499 | 		 * @deprecated as of 2.0.0, in favor of {@link #tools(Object...)}. To be removed
  500 | 		 * in 3.0.0.
  501 | 		 */
  502 | 		@Deprecated(since = "2.0.0", forRemoval = true)
  503 | 		ChatClientRequestSpec toolCallbacks(List<ToolCallback> toolCallbacks);
  504 | 
  505 | 		/**
  506 | 		 * @deprecated as of 2.0.0, in favor of {@link #tools(Object...)}. To be removed
  507 | 		 * in 3.0.0.
  508 | 		 */
  509 | 		@Deprecated(since = "2.0.0", forRemoval = true)
  510 | 		ChatClientRequestSpec toolCallbacks(ToolCallbackProvider... toolCallbackProviders);
  511 | 
  512 | 		ChatClientRequestSpec toolContext(Map<String, Object> toolContext);
  513 | 
  514 | 		ChatClientRequestSpec system(String text);
  515 | 
  516 | 		ChatClientRequestSpec system(Resource textResource, Charset charset);
  517 | 
  518 | 		ChatClientRequestSpec system(Resource text);
  519 | 
  520 | 		ChatClientRequestSpec system(Consumer<PromptSystemSpec> consumer);
  521 | 
  522 | 		ChatClientRequestSpec user(String text);
  523 | 
  524 | 		ChatClientRequestSpec user(Resource text, Charset charset);
  525 | 
  526 | 		ChatClientRequestSpec user(Resource text);
  527 | 
  528 | 		ChatClientRequestSpec user(Consumer<PromptUserSpec> consumer);
  529 | 
  530 | 		ChatClientRequestSpec templateRenderer(TemplateRenderer templateRenderer);
  531 | 
  532 | 		CallResponseSpec call();
  533 | 
  534 | 		StreamResponseSpec stream();
  535 | 
  536 | 	}
  537 | 
  538 | 	/**
  539 | 	 * A mutable builder for creating a {@link ChatClient}.
  540 | 	 */
  541 | 	interface Builder {
  542 | 
  543 | 		Builder defaultAdvisors(Advisor... advisors);
  544 | 
  545 | 		Builder defaultAdvisors(Consumer<AdvisorSpec> advisorSpecConsumer);
  546 | 
  547 | 		Builder defaultAdvisors(List<Advisor> advisors);
  548 | 
  549 | 		Builder defaultOptions(ChatOptions.Builder chatOptions);
  550 | 
  551 | 		Builder defaultUser(String text);
  552 | 
  553 | 		Builder defaultUser(Resource text, Charset charset);
  554 | 
  555 | 		Builder defaultUser(Resource text);
  556 | 
  557 | 		Builder defaultUser(Consumer<PromptUserSpec> userSpecConsumer);
  558 | 
  559 | 		Builder defaultSystem(String text);
  560 | 
  561 | 		Builder defaultSystem(Resource text, Charset charset);
  562 | 
  563 | 		Builder defaultSystem(Resource text);
  564 | 
  565 | 		Builder defaultSystem(Consumer<PromptSystemSpec> systemSpecConsumer);
  566 | 
  567 | 		Builder defaultTemplateRenderer(TemplateRenderer templateRenderer);
  568 | 
  569 | 		/**
  570 | 		 * Register one or more default tools that will be available to every request
  571 | 		 * built from this {@link Builder}. The method accepts the same heterogeneous mix
  572 | 		 * of tool representations as {@link ChatClientRequestSpec#tools(Object...)} and
  573 | 		 * applies the same automatic dispatch rules:
  574 | 		 *
  575 | 		 * <ul>
  576 | 		 * <li>{@link org.springframework.ai.tool.ToolCallback} — registered directly as a
  577 | 		 * callback.</li>
  578 | 		 * <li>{@link org.springframework.ai.tool.ToolCallbackProvider} — registered
  579 | 		 * directly as a provider; its callbacks are resolved lazily at request time.</li>
  580 | 		 * <li>{@code ToolCallback[]} or {@code ToolCallbackProvider[]} — every element of
  581 | 		 * the array is registered as above.</li>
  582 | 		 * <li>{@link java.util.Collection} — iterated and each element is dispatched by
  583 | 		 * the same rules.</li>
  584 | 		 * <li>Any other object — treated as a {@code @Tool}-annotated POJO; a
  585 | 		 * {@link org.springframework.ai.tool.ToolCallback} is generated for each
  586 | 		 * {@link org.springframework.ai.tool.annotation.Tool}-annotated method it
  587 | 		 * contains.</li>
  588 | 		 * </ul>
  589 | 		 *
  590 | 		 * <p>
  591 | 		 * Default tools are shared across all requests produced by {@link ChatClient}
  592 | 		 * instances built from this builder. If a request also provides its own tools via
  593 | 		 * {@link ChatClientRequestSpec#tools(Object...)}, those runtime tools completely
  594 | 		 * override the defaults for that request.
  595 | 		 *
  596 | 		 * <p>
  597 | 		 * WARNING: Because default tools are shared, be careful not to register tools
  598 | 		 * that should only be available in specific contexts.
  599 | 		 * @param tools tool objects to register; must not be {@code null} and must not
  600 | 		 * contain {@code null} elements
  601 | 		 * @return this builder for chaining
  602 | 		 * @throws IllegalArgumentException if {@code tools} is {@code null}, contains
  603 | 		 * {@code null} elements, or if a POJO argument has no
  604 | 		 * {@link org.springframework.ai.tool.annotation.Tool}-annotated methods
  605 | 		 */
  606 | 		Builder defaultTools(Object... tools);
  607 | 
  608 | 		/**
  609 | 		 * @deprecated as of 2.0.0, in favor of {@link #defaultTools(Object...)}. To be
  610 | 		 * removed in 3.0.0.
  611 | 		 */
  612 | 		@Deprecated(since = "2.0.0", forRemoval = true)
  613 | 		Builder defaultToolCallbacks(ToolCallback... toolCallbacks);
  614 | 
  615 | 		/**
  616 | 		 * @deprecated as of 2.0.0, in favor of {@link #defaultTools(Object...)}. To be
  617 | 		 * removed in 3.0.0.
  618 | 		 */
  619 | 		@Deprecated(since = "2.0.0", forRemoval = true)
  620 | 		Builder defaultToolCallbacks(List<ToolCallback> toolCallbacks);
  621 | 
  622 | 		/**
  623 | 		 * @deprecated as of 2.0.0, in favor of {@link #defaultTools(Object...)}. To be
  624 | 		 * removed in 3.0.0.
  625 | 		 */
  626 | 		@Deprecated(since = "2.0.0", forRemoval = true)
  627 | 		Builder defaultToolCallbacks(ToolCallbackProvider... toolCallbackProviders);
  628 | 
  629 | 		Builder defaultToolContext(Map<String, Object> toolContext);
  630 | 
  631 | 		Builder clone();
  632 | 
  633 | 		ChatClient build();
  634 | 
  635 | 	}
  636 | 
  637 | }
```
