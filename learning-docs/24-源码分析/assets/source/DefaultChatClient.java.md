<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/DefaultChatClient.java -->
<!-- 正文引用：[[源码:DefaultChatClient.java#L起-L止]] ；代码块头部：(src: DefaultChatClient.java#L起-L止) -->

# DefaultChatClient.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/DefaultChatClient.java` |
| 行数 | 1306 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:54.416Z |

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
   19 | import java.io.IOException;
   20 | import java.net.URISyntaxException;
   21 | import java.net.URL;
   22 | import java.nio.charset.Charset;
   23 | import java.util.ArrayList;
   24 | import java.util.Arrays;
   25 | import java.util.Collection;
   26 | import java.util.HashMap;
   27 | import java.util.List;
   28 | import java.util.Map;
   29 | import java.util.Optional;
   30 | import java.util.function.Consumer;
   31 | 
   32 | import io.micrometer.observation.Observation;
   33 | import io.micrometer.observation.ObservationRegistry;
   34 | import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
   35 | import org.apache.commons.logging.Log;
   36 | import org.apache.commons.logging.LogFactory;
   37 | import org.jspecify.annotations.Nullable;
   38 | import reactor.core.publisher.Flux;
   39 | 
   40 | import org.springframework.ai.chat.client.advisor.ChatModelCallAdvisor;
   41 | import org.springframework.ai.chat.client.advisor.ChatModelStreamAdvisor;
   42 | import org.springframework.ai.chat.client.advisor.DefaultAroundAdvisorChain;
   43 | import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
   44 | import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
   45 | import org.springframework.ai.chat.client.advisor.api.Advisor;
   46 | import org.springframework.ai.chat.client.advisor.api.BaseAdvisorChain;
   47 | import org.springframework.ai.chat.client.advisor.api.MemoryAdvisor;
   48 | import org.springframework.ai.chat.client.advisor.api.ToolAdvisor;
   49 | import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
   50 | import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
   51 | import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
   52 | import org.springframework.ai.chat.client.observation.ChatClientObservationDocumentation;
   53 | import org.springframework.ai.chat.client.observation.DefaultChatClientObservationConvention;
   54 | import org.springframework.ai.chat.messages.AbstractMessage;
   55 | import org.springframework.ai.chat.messages.Message;
   56 | import org.springframework.ai.chat.metadata.FinishReasonCategory;
   57 | import org.springframework.ai.chat.model.ChatModel;
   58 | import org.springframework.ai.chat.model.ChatResponse;
   59 | import org.springframework.ai.chat.model.Generation;
   60 | import org.springframework.ai.chat.model.GuardrailRefusal;
   61 | import org.springframework.ai.chat.prompt.ChatOptions;
   62 | import org.springframework.ai.chat.prompt.Prompt;
   63 | import org.springframework.ai.content.Media;
   64 | import org.springframework.ai.converter.BeanOutputConverter;
   65 | import org.springframework.ai.converter.StructuredOutputConverter;
   66 | import org.springframework.ai.support.ToolCallbacks;
   67 | import org.springframework.ai.template.TemplateRenderer;
   68 | import org.springframework.ai.template.st.StTemplateRenderer;
   69 | import org.springframework.ai.tool.ToolCallback;
   70 | import org.springframework.ai.tool.ToolCallbackProvider;
   71 | import org.springframework.core.ParameterizedTypeReference;
   72 | import org.springframework.core.io.Resource;
   73 | import org.springframework.util.Assert;
   74 | import org.springframework.util.CollectionUtils;
   75 | import org.springframework.util.MimeType;
   76 | import org.springframework.util.StringUtils;
   77 | 
   78 | /**
   79 |  * The default implementation of {@link ChatClient} as created by the
   80 |  * {@link Builder#build()} } method.
   81 |  *
   82 |  * @author Mark Pollack
   83 |  * @author Christian Tzolov
   84 |  * @author Josh Long
   85 |  * @author Arjen Poutsma
   86 |  * @author Soby Chacko
   87 |  * @author Dariusz Jedrzejczyk
   88 |  * @author Thomas Vitale
   89 |  * @author Jonatan Ivanov
   90 |  * @author Wenli Tian
   91 |  * @author Sebastien Deleuze
   92 |  * @since 1.0.0
   93 |  */
   94 | public class DefaultChatClient implements ChatClient {
   95 | 
   96 | 	private static final Log logger = LogFactory.getLog(DefaultChatClient.class);
   97 | 
   98 | 	private static final ChatClientObservationConvention DEFAULT_CHAT_CLIENT_OBSERVATION_CONVENTION = new DefaultChatClientObservationConvention();
   99 | 
  100 | 	private static final TemplateRenderer DEFAULT_TEMPLATE_RENDERER = StTemplateRenderer.builder().build();
  101 | 
  102 | 	private static final ChatClientMessageAggregator CHAT_CLIENT_MESSAGE_AGGREGATOR = new ChatClientMessageAggregator();
  103 | 
  104 | 	private final DefaultChatClientRequestSpec defaultChatClientRequest;
  105 | 
  106 | 	public DefaultChatClient(DefaultChatClientRequestSpec defaultChatClientRequest) {
  107 | 		Assert.notNull(defaultChatClientRequest, "defaultChatClientRequest cannot be null");
  108 | 		this.defaultChatClientRequest = defaultChatClientRequest;
  109 | 	}
  110 | 
  111 | 	@Override
  112 | 	public ChatClientRequestSpec prompt() {
  113 | 		return new DefaultChatClientRequestSpec(this.defaultChatClientRequest);
  114 | 	}
  115 | 
  116 | 	@Override
  117 | 	public ChatClientRequestSpec prompt(String content) {
  118 | 		Assert.hasText(content, "content cannot be null or empty");
  119 | 		return prompt(new Prompt(content));
  120 | 	}
  121 | 
  122 | 	@Override
  123 | 	public ChatClientRequestSpec prompt(Prompt prompt) {
  124 | 		Assert.notNull(prompt, "prompt cannot be null");
  125 | 
  126 | 		DefaultChatClientRequestSpec spec = new DefaultChatClientRequestSpec(this.defaultChatClientRequest);
  127 | 
  128 | 		// Messages
  129 | 		if (prompt.getInstructions() != null) {
  130 | 			spec.messages(prompt.getInstructions());
  131 | 		}
  132 | 
  133 | 		// ChatOptions
  134 | 		if (prompt.getOptions() != null) {
  135 | 			ChatOptions.Builder<?> requestOptionsBuilder = prompt.getOptions().mutate();
  136 | 			if (spec.getOptionsCustomizer() != null) {
  137 | 				spec.getOptionsCustomizer().combineWith(requestOptionsBuilder);
  138 | 			}
  139 | 			else {
  140 | 				spec.options(requestOptionsBuilder);
  141 | 			}
  142 | 		}
  143 | 
  144 | 		return spec;
  145 | 	}
  146 | 
  147 | 	/**
  148 | 	 * Return a {@link ChatClient.Builder} to create a new {@link ChatClient} whose
  149 | 	 * settings are replicated from this {@link ChatClientRequest}.
  150 | 	 */
  151 | 	@Override
  152 | 	public Builder mutate() {
  153 | 		return this.defaultChatClientRequest.mutate();
  154 | 	}
  155 | 
  156 | 	public static class DefaultPromptUserSpec implements PromptUserSpec {
  157 | 
  158 | 		private final Map<String, Object> params = new HashMap<>();
  159 | 
  160 | 		private final Map<String, Object> metadata = new HashMap<>();
  161 | 
  162 | 		private final List<Media> media = new ArrayList<>();
  163 | 
  164 | 		private @Nullable String text;
  165 | 
  166 | 		@Override
  167 | 		public PromptUserSpec media(Media... media) {
  168 | 			Assert.notNull(media, "media cannot be null");
  169 | 			Assert.noNullElements(media, "media cannot contain null elements");
  170 | 			this.media.addAll(Arrays.asList(media));
  171 | 			return this;
  172 | 		}
  173 | 
  174 | 		@Override
  175 | 		public PromptUserSpec media(MimeType mimeType, URL url) {
  176 | 			Assert.notNull(mimeType, "mimeType cannot be null");
  177 | 			Assert.notNull(url, "url cannot be null");
  178 | 			try {
  179 | 				this.media.add(Media.builder().mimeType(mimeType).data(url.toURI()).build());
  180 | 			}
  181 | 			catch (URISyntaxException e) {
  182 | 				throw new RuntimeException(e);
  183 | 			}
  184 | 			return this;
  185 | 		}
  186 | 
  187 | 		@Override
  188 | 		public PromptUserSpec media(MimeType mimeType, Resource resource) {
  189 | 			Assert.notNull(mimeType, "mimeType cannot be null");
  190 | 			Assert.notNull(resource, "resource cannot be null");
  191 | 			this.media.add(Media.builder().mimeType(mimeType).data(resource).build());
  192 | 			return this;
  193 | 		}
  194 | 
  195 | 		@Override
  196 | 		public PromptUserSpec text(String text) {
  197 | 			Assert.hasText(text, "text cannot be null or empty");
  198 | 			this.text = text;
  199 | 			return this;
  200 | 		}
  201 | 
  202 | 		@Override
  203 | 		public PromptUserSpec text(Resource text, Charset charset) {
  204 | 			Assert.notNull(text, "text cannot be null");
  205 | 			Assert.notNull(charset, "charset cannot be null");
  206 | 			try {
  207 | 				this.text(text.getContentAsString(charset));
  208 | 			}
  209 | 			catch (IOException e) {
  210 | 				throw new RuntimeException(e);
  211 | 			}
  212 | 			return this;
  213 | 		}
  214 | 
  215 | 		@Override
  216 | 		public PromptUserSpec text(Resource text) {
  217 | 			Assert.notNull(text, "text cannot be null");
  218 | 			this.text(text, Charset.defaultCharset());
  219 | 			return this;
  220 | 		}
  221 | 
  222 | 		@Override
  223 | 		public PromptUserSpec param(String key, Object value) {
  224 | 			Assert.hasText(key, "key cannot be null or empty");
  225 | 			Assert.notNull(value, "value cannot be null");
  226 | 			this.params.put(key, value);
  227 | 			return this;
  228 | 		}
  229 | 
  230 | 		@Override
  231 | 		public PromptUserSpec params(Map<String, Object> params) {
  232 | 			Assert.notNull(params, "params cannot be null");
  233 | 			Assert.noNullElements(params.keySet(), "param keys cannot contain null elements");
  234 | 			Assert.noNullElements(params.values(), "param values cannot contain null elements");
  235 | 			this.params.putAll(params);
  236 | 			return this;
  237 | 		}
  238 | 
  239 | 		@Override
  240 | 		public PromptUserSpec metadata(Map<String, Object> metadata) {
  241 | 			Assert.notNull(metadata, "metadata cannot be null");
  242 | 			Assert.noNullElements(metadata.keySet(), "metadata keys cannot contain null elements");
  243 | 			Assert.noNullElements(metadata.values(), "metadata values cannot contain null elements");
  244 | 			this.metadata.putAll(metadata);
  245 | 			return this;
  246 | 		}
  247 | 
  248 | 		@Override
  249 | 		public PromptUserSpec metadata(String key, Object value) {
  250 | 			Assert.hasText(key, "metadata key cannot be null or empty");
  251 | 			Assert.notNull(value, "metadata value cannot be null");
  252 | 			this.metadata.put(key, value);
  253 | 			return this;
  254 | 		}
  255 | 
  256 | 		protected @Nullable String text() {
  257 | 			return this.text;
  258 | 		}
  259 | 
  260 | 		protected Map<String, Object> params() {
  261 | 			return this.params;
  262 | 		}
  263 | 
  264 | 		protected List<Media> media() {
  265 | 			return this.media;
  266 | 		}
  267 | 
  268 | 		protected Map<String, Object> metadata() {
  269 | 			return this.metadata;
  270 | 		}
  271 | 
  272 | 	}
  273 | 
  274 | 	public static class DefaultPromptSystemSpec implements PromptSystemSpec {
  275 | 
  276 | 		private final Map<String, Object> params = new HashMap<>();
  277 | 
  278 | 		private final Map<String, Object> metadata = new HashMap<>();
  279 | 
  280 | 		private @Nullable String text;
  281 | 
  282 | 		@Override
  283 | 		public PromptSystemSpec text(String text) {
  284 | 			Assert.hasText(text, "text cannot be null or empty");
  285 | 			this.text = text;
  286 | 			return this;
  287 | 		}
  288 | 
  289 | 		@Override
  290 | 		public PromptSystemSpec text(Resource text, Charset charset) {
  291 | 			Assert.notNull(text, "text cannot be null");
  292 | 			Assert.notNull(charset, "charset cannot be null");
  293 | 			try {
  294 | 				this.text(text.getContentAsString(charset));
  295 | 			}
  296 | 			catch (IOException e) {
  297 | 				throw new RuntimeException(e);
  298 | 			}
  299 | 			return this;
  300 | 		}
  301 | 
  302 | 		@Override
  303 | 		public PromptSystemSpec text(Resource text) {
  304 | 			Assert.notNull(text, "text cannot be null");
  305 | 			this.text(text, Charset.defaultCharset());
  306 | 			return this;
  307 | 		}
  308 | 
  309 | 		@Override
  310 | 		public PromptSystemSpec param(String key, Object value) {
  311 | 			Assert.hasText(key, "key cannot be null or empty");
  312 | 			Assert.notNull(value, "value cannot be null");
  313 | 			this.params.put(key, value);
  314 | 			return this;
  315 | 		}
  316 | 
  317 | 		@Override
  318 | 		public PromptSystemSpec params(Map<String, Object> params) {
  319 | 			Assert.notNull(params, "params cannot be null");
  320 | 			Assert.noNullElements(params.keySet(), "param keys cannot contain null elements");
  321 | 			Assert.noNullElements(params.values(), "param values cannot contain null elements");
  322 | 			this.params.putAll(params);
  323 | 			return this;
  324 | 		}
  325 | 
  326 | 		@Override
  327 | 		public PromptSystemSpec metadata(Map<String, Object> metadata) {
  328 | 			Assert.notNull(metadata, "metadata cannot be null");
  329 | 			Assert.noNullElements(metadata.keySet(), "metadata keys cannot contain null elements");
  330 | 			Assert.noNullElements(metadata.values(), "metadata values cannot contain null elements");
  331 | 			this.metadata.putAll(metadata);
  332 | 			return this;
  333 | 		}
  334 | 
  335 | 		@Override
  336 | 		public PromptSystemSpec metadata(String key, Object value) {
  337 | 			Assert.hasText(key, "metadata key cannot be null or empty");
  338 | 			Assert.notNull(value, "metadata value cannot be null");
  339 | 			this.metadata.put(key, value);
  340 | 			return this;
  341 | 		}
  342 | 
  343 | 		protected @Nullable String text() {
  344 | 			return this.text;
  345 | 		}
  346 | 
  347 | 		protected Map<String, Object> params() {
  348 | 			return this.params;
  349 | 		}
  350 | 
  351 | 		protected Map<String, Object> metadata() {
  352 | 			return this.metadata;
  353 | 		}
  354 | 
  355 | 	}
  356 | 
  357 | 	public static class DefaultAdvisorSpec implements AdvisorSpec {
  358 | 
  359 | 		private final List<Advisor> advisors = new ArrayList<>();
  360 | 
  361 | 		private final Map<String, Object> params = new HashMap<>();
  362 | 
  363 | 		@Override
  364 | 		public AdvisorSpec param(String key, Object value) {
  365 | 			Assert.hasText(key, "key cannot be null or empty");
  366 | 			Assert.notNull(value, "value cannot be null");
  367 | 			this.params.put(key, value);
  368 | 			return this;
  369 | 		}
  370 | 
  371 | 		@Override
  372 | 		public AdvisorSpec params(Map<String, Object> params) {
  373 | 			Assert.notNull(params, "params cannot be null");
  374 | 			Assert.noNullElements(params.keySet(), "param keys cannot contain null elements");
  375 | 			Assert.noNullElements(params.values(), "param values cannot contain null elements");
  376 | 			this.params.putAll(params);
  377 | 			return this;
  378 | 		}
  379 | 
  380 | 		@Override
  381 | 		public AdvisorSpec advisors(Advisor... advisors) {
  382 | 			Assert.notNull(advisors, "advisors cannot be null");
  383 | 			Assert.noNullElements(advisors, "advisors cannot contain null elements");
  384 | 			this.advisors.addAll(List.of(advisors));
  385 | 			return this;
  386 | 		}
  387 | 
  388 | 		@Override
  389 | 		public AdvisorSpec advisors(List<Advisor> advisors) {
  390 | 			Assert.notNull(advisors, "advisors cannot be null");
  391 | 			Assert.noNullElements(advisors, "advisors cannot contain null elements");
  392 | 			this.advisors.addAll(advisors);
  393 | 			return this;
  394 | 		}
  395 | 
  396 | 		public List<Advisor> getAdvisors() {
  397 | 			return this.advisors;
  398 | 		}
  399 | 
  400 | 		public Map<String, Object> getParams() {
  401 | 			return this.params;
  402 | 		}
  403 | 
  404 | 	}
  405 | 
  406 | 	/**
  407 | 	 * Default implementation of {@link EntityParamSpec}.
  408 | 	 */
  409 | 	public static class DefaultEntityParamSpec implements EntityParamSpec {
  410 | 
  411 | 		private boolean enableNative = false;
  412 | 
  413 | 		private boolean validated = false;
  414 | 
  415 | 		public boolean isEnableNative() {
  416 | 			return this.enableNative;
  417 | 		}
  418 | 
  419 | 		public boolean isValidated() {
  420 | 			return this.validated;
  421 | 		}
  422 | 
  423 | 		@Override
  424 | 		public EntityParamSpec useProviderStructuredOutput() {
  425 | 			this.enableNative = true;
  426 | 			return this;
  427 | 		}
  428 | 
  429 | 		@Override
  430 | 		public EntityParamSpec validateSchema() {
  431 | 			this.validated = true;
  432 | 			return this;
  433 | 		}
  434 | 
  435 | 	}
  436 | 
  437 | 	public static class DefaultCallResponseSpec implements CallResponseSpec {
  438 | 
  439 | 		private final ChatClientRequest request;
  440 | 
  441 | 		private final BaseAdvisorChain advisorChain;
  442 | 
  443 | 		private final ObservationRegistry observationRegistry;
  444 | 
  445 | 		private final ChatClientObservationConvention observationConvention;
  446 | 
  447 | 		public DefaultCallResponseSpec(ChatClientRequest chatClientRequest, BaseAdvisorChain advisorChain,
  448 | 				ObservationRegistry observationRegistry, ChatClientObservationConvention observationConvention) {
  449 | 			Assert.notNull(chatClientRequest, "chatClientRequest cannot be null");
  450 | 			Assert.notNull(advisorChain, "advisorChain cannot be null");
  451 | 			Assert.notNull(observationRegistry, "observationRegistry cannot be null");
  452 | 			Assert.notNull(observationConvention, "observationConvention cannot be null");
  453 | 
  454 | 			this.request = chatClientRequest;
  455 | 			this.advisorChain = advisorChain;
  456 | 			this.observationRegistry = observationRegistry;
  457 | 			this.observationConvention = observationConvention;
  458 | 		}
  459 | 
  460 | 		@Override
  461 | 		public <T> ResponseEntity<ChatResponse, T> responseEntity(Class<T> type,
  462 | 				Consumer<EntityParamSpec> entityParamSpecConsumer) {
  463 | 			Assert.notNull(type, "type cannot be null");
  464 | 			Assert.notNull(entityParamSpecConsumer, "entityParamSpecConsumer cannot be null");
  465 | 			var converter = new BeanOutputConverter<>(type);
  466 | 			return doResponseEntity(converter, resolveAdvisorChain(entityParamSpecConsumer, converter));
  467 | 		}
  468 | 
  469 | 		@Override
  470 | 		public <T> ResponseEntity<ChatResponse, T> responseEntity(Class<T> type) {
  471 | 			Assert.notNull(type, "type cannot be null");
  472 | 			return doResponseEntity(new BeanOutputConverter<>(type));
  473 | 		}
  474 | 
  475 | 		@Override
  476 | 		public <T> ResponseEntity<ChatResponse, T> responseEntity(ParameterizedTypeReference<T> type,
  477 | 				Consumer<EntityParamSpec> entityParamSpecConsumer) {
  478 | 			Assert.notNull(type, "type cannot be null");
  479 | 			Assert.notNull(entityParamSpecConsumer, "entityParamSpecConsumer cannot be null");
  480 | 			var converter = new BeanOutputConverter<>(type);
  481 | 			return doResponseEntity(converter, resolveAdvisorChain(entityParamSpecConsumer, converter));
  482 | 		}
  483 | 
  484 | 		@Override
  485 | 		public <T> ResponseEntity<ChatResponse, T> responseEntity(ParameterizedTypeReference<T> type) {
  486 | 			Assert.notNull(type, "type cannot be null");
  487 | 			return doResponseEntity(new BeanOutputConverter<>(type));
  488 | 		}
  489 | 
  490 | 		@Override
  491 | 		public <T> ResponseEntity<ChatResponse, T> responseEntity(
  492 | 				StructuredOutputConverter<T> structuredOutputConverter,
  493 | 				Consumer<EntityParamSpec> entityParamSpecConsumer) {
  494 | 			Assert.notNull(structuredOutputConverter, "structuredOutputConverter cannot be null");
  495 | 			Assert.notNull(entityParamSpecConsumer, "entityParamSpecConsumer cannot be null");
  496 | 			return doResponseEntity(structuredOutputConverter,
  497 | 					resolveAdvisorChain(entityParamSpecConsumer, structuredOutputConverter));
  498 | 		}
  499 | 
  500 | 		@Override
  501 | 		public <T> ResponseEntity<ChatResponse, T> responseEntity(
  502 | 				StructuredOutputConverter<T> structuredOutputConverter) {
  503 | 			Assert.notNull(structuredOutputConverter, "structuredOutputConverter cannot be null");
  504 | 			return doResponseEntity(structuredOutputConverter);
  505 | 		}
  506 | 
  507 | 		protected <T> ResponseEntity<ChatResponse, T> doResponseEntity(StructuredOutputConverter<T> outputConverter) {
  508 | 			return this.doResponseEntity(outputConverter, this.advisorChain);
  509 | 		}
  510 | 
  511 | 		protected <T> ResponseEntity<ChatResponse, T> doResponseEntity(StructuredOutputConverter<T> outputConverter,
  512 | 				BaseAdvisorChain advisorChain) {
  513 | 
  514 | 			Assert.notNull(outputConverter, "structuredOutputConverter cannot be null");
  515 | 			Assert.notNull(advisorChain, "advisor chain cannot be null");
  516 | 
  517 | 			this.request.context().put(ChatClientAttributes.OUTPUT_FORMAT.getKey(), outputConverter.getFormat());
  518 | 
  519 | 			if (Boolean.TRUE
  520 | 				.equals(this.request.context().get(ChatClientAttributes.STRUCTURED_OUTPUT_NATIVE.getKey()))) {
  521 | 				this.request.context()
  522 | 					.put(ChatClientAttributes.STRUCTURED_OUTPUT_SCHEMA.getKey(), outputConverter.getJsonSchema());
  523 | 			}
  524 | 
  525 | 			var chatResponse = doGetObservableChatClientResponse(this.request, advisorChain).chatResponse();
  526 | 			if (GuardrailRefusal.isRefusal(chatResponse)) {
  527 | 				// The refusal is not the requested output. The caller has the response,
  528 | 				// and reads the refusal there.
  529 | 				return new ResponseEntity<>(chatResponse, null);
  530 | 			}
  531 | 			var responseContent = getContentFromChatResponse(chatResponse);
  532 | 			boolean contentFiltered = isContentFiltered(chatResponse);
  533 | 			if (responseContent == null || (contentFiltered && !StringUtils.hasText(responseContent))) {
  534 | 				return new ResponseEntity<>(chatResponse, null);
  535 | 			}
  536 | 			try {
  537 | 				T entity = outputConverter.convert(responseContent);
  538 | 				return new ResponseEntity<>(chatResponse, entity);
  539 | 			}
  540 | 			catch (RuntimeException ex) {
  541 | 				if (contentFiltered) {
  542 | 					// What a filter left is not the output that was asked for. The
  543 | 					// caller has the response and reads the finish reason there.
  544 | 					logger.debug("Returning no entity for a response that a safety filter stopped, "
  545 | 							+ "as it could not be converted", ex);
  546 | 					return new ResponseEntity<>(chatResponse, null);
  547 | 				}
  548 | 				throw ex;
  549 | 			}
  550 | 		}
  551 | 
  552 | 		@Override
  553 | 		public <T> @Nullable T entity(ParameterizedTypeReference<T> type,
  554 | 				Consumer<EntityParamSpec> entitySpecConsumer) {
  555 | 			Assert.notNull(type, "type cannot be null");
  556 | 			Assert.notNull(entitySpecConsumer, "entitySpecConsumer cannot be null");
  557 | 			var converter = new BeanOutputConverter<>(type);
  558 | 			return doSingleWithBeanOutputConverter(converter, resolveAdvisorChain(entitySpecConsumer, converter));
  559 | 		}
  560 | 
  561 | 		@Override
  562 | 		public <T> @Nullable T entity(Class<T> type, Consumer<EntityParamSpec> entitySpecConsumer) {
  563 | 			Assert.notNull(type, "type cannot be null");
  564 | 			Assert.notNull(entitySpecConsumer, "entitySpecConsumer cannot be null");
  565 | 			var converter = new BeanOutputConverter<>(type);
  566 | 			return doSingleWithBeanOutputConverter(converter, resolveAdvisorChain(entitySpecConsumer, converter));
  567 | 		}
  568 | 
  569 | 		@Override
  570 | 		public <T> @Nullable T entity(ParameterizedTypeReference<T> type) {
  571 | 			Assert.notNull(type, "type cannot be null");
  572 | 			return doSingleWithBeanOutputConverter(new BeanOutputConverter<>(type));
  573 | 		}
  574 | 
  575 | 		@Override
  576 | 		public <T> @Nullable T entity(StructuredOutputConverter<T> structuredOutputConverter,
  577 | 				Consumer<EntityParamSpec> entitySpecConsumer) {
  578 | 			Assert.notNull(structuredOutputConverter, "structuredOutputConverter cannot be null");
  579 | 			Assert.notNull(entitySpecConsumer, "entitySpecConsumer cannot be null");
  580 | 			return doSingleWithBeanOutputConverter(structuredOutputConverter,
  581 | 					resolveAdvisorChain(entitySpecConsumer, structuredOutputConverter));
  582 | 		}
  583 | 
  584 | 		@Override
  585 | 		public <T> @Nullable T entity(StructuredOutputConverter<T> structuredOutputConverter) {
  586 | 			Assert.notNull(structuredOutputConverter, "structuredOutputConverter cannot be null");
  587 | 			return doSingleWithBeanOutputConverter(structuredOutputConverter);
  588 | 		}
  589 | 
  590 | 		@Override
  591 | 		public <T> @Nullable T entity(Class<T> type) {
  592 | 			Assert.notNull(type, "type cannot be null");
  593 | 			return doSingleWithBeanOutputConverter(new BeanOutputConverter<>(type));
  594 | 		}
  595 | 
  596 | 		private BaseAdvisorChain resolveAdvisorChain(Consumer<EntityParamSpec> consumer,
  597 | 				StructuredOutputConverter<?> converter) {
  598 | 			var spec = new DefaultEntityParamSpec();
  599 | 			consumer.accept(spec);
  600 | 			if (spec.isEnableNative()) {
  601 | 				this.request.context().put(ChatClientAttributes.STRUCTURED_OUTPUT_NATIVE.getKey(), true);
  602 | 			}
  603 | 			if (spec.isValidated()) {
  604 | 				var validationAdvisor = StructuredOutputValidationAdvisor.builder()
  605 | 					.outputJsonSchema(converter.getJsonSchema())
  606 | 					.build();
  607 | 				return this.advisorChain.mutate().push(validationAdvisor).build();
  608 | 			}
  609 | 			return this.advisorChain;
  610 | 		}
  611 | 
  612 | 		private <T> @Nullable T doSingleWithBeanOutputConverter(StructuredOutputConverter<T> outputConverter) {
  613 | 			return doSingleWithBeanOutputConverter(outputConverter, this.advisorChain);
  614 | 		}
  615 | 
  616 | 		private <T> @Nullable T doSingleWithBeanOutputConverter(StructuredOutputConverter<T> outputConverter,
  617 | 				BaseAdvisorChain advisorChain) {
  618 | 
  619 | 			if (StringUtils.hasText(outputConverter.getFormat())) {
  620 | 				// Used for default structured output format support, based on prompt
  621 | 				// instructions.
  622 | 				this.request.context().put(ChatClientAttributes.OUTPUT_FORMAT.getKey(), outputConverter.getFormat());
  623 | 			}
  624 | 
  625 | 			if (Boolean.TRUE
  626 | 				.equals(this.request.context().get(ChatClientAttributes.STRUCTURED_OUTPUT_NATIVE.getKey()))) {
  627 | 				// Used for native structured output support, e.g. AI model API should
  628 | 				// provide structured output support.
  629 | 				this.request.context()
  630 | 					.put(ChatClientAttributes.STRUCTURED_OUTPUT_SCHEMA.getKey(), outputConverter.getJsonSchema());
  631 | 
  632 | 			}
  633 | 
  634 | 			var chatClientResponse = doGetObservableChatClientResponse(this.request, advisorChain);
  635 | 			var chatResponse = chatClientResponse.chatResponse();
  636 | 			if (GuardrailRefusal.isRefusal(chatResponse)) {
  637 | 				// The refusal is not the requested output: converting it would fail as
  638 | 				// malformed, and returning null would hide that the turn was refused.
  639 | 				throw new GuardrailRefusalException(chatClientResponse);
  640 | 			}
  641 | 
  642 | 			var stringResponse = getContentFromChatResponse(chatResponse);
  643 | 			boolean contentFiltered = isContentFiltered(chatResponse);
  644 | 			if (contentFiltered && !StringUtils.hasText(stringResponse)) {
  645 | 				// The filter left nothing. Returning no entity would hide what happened.
  646 | 				throw new ContentFilteredException(chatClientResponse, null);
  647 | 			}
  648 | 			if (stringResponse == null) {
  649 | 				return null;
  650 | 			}
  651 | 			try {
  652 | 				return outputConverter.convert(stringResponse);
  653 | 			}
  654 | 			catch (RuntimeException ex) {
  655 | 				if (contentFiltered) {
  656 | 					// What the filter left is not the output that was asked for. A
  657 | 					// response that converts all the same is returned as before.
  658 | 					throw new ContentFilteredException(chatClientResponse, ex);
  659 | 				}
  660 | 				throw ex;
  661 | 			}
  662 | 		}
  663 | 
  664 | 		@Override
  665 | 		public ChatClientResponse chatClientResponse() {
  666 | 			return doGetObservableChatClientResponse(this.request);
  667 | 		}
  668 | 
  669 | 		@Override
  670 | 		public @Nullable ChatResponse chatResponse() {
  671 | 			return doGetObservableChatClientResponse(this.request).chatResponse();
  672 | 		}
  673 | 
  674 | 		@Override
  675 | 		public @Nullable String content() {
  676 | 			ChatResponse chatResponse = doGetObservableChatClientResponse(this.request).chatResponse();
  677 | 			return getContentFromChatResponse(chatResponse);
  678 | 		}
  679 | 
  680 | 		private ChatClientResponse doGetObservableChatClientResponse(ChatClientRequest chatClientRequest) {
  681 | 			return doGetObservableChatClientResponse(chatClientRequest, this.advisorChain);
  682 | 		}
  683 | 
  684 | 		private ChatClientResponse doGetObservableChatClientResponse(ChatClientRequest chatClientRequest,
  685 | 				BaseAdvisorChain advisorChain) {
  686 | 
  687 | 			String outputFormat = (String) chatClientRequest.context()
  688 | 				.getOrDefault(ChatClientAttributes.OUTPUT_FORMAT.getKey(), null);
  689 | 
  690 | 			ChatClientObservationContext observationContext = ChatClientObservationContext.builder()
  691 | 				.request(chatClientRequest)
  692 | 				.advisors(advisorChain.getCallAdvisors())
  693 | 				.stream(false)
  694 | 				.format(outputFormat)
  695 | 				.build();
  696 | 
  697 | 			var observation = ChatClientObservationDocumentation.AI_CHAT_CLIENT.observation(this.observationConvention,
  698 | 					DEFAULT_CHAT_CLIENT_OBSERVATION_CONVENTION, () -> observationContext, this.observationRegistry);
  699 | 
  700 | 			// CHECKSTYLE:OFF
  701 | 			var chatClientResponse = observation.observe(() -> {
  702 | 				// Apply the advisor chain that terminates with the ChatModelCallAdvisor.
  703 | 				var response = advisorChain.nextCall(chatClientRequest);
  704 | 				observationContext.setResponse(response);
  705 | 				return response;
  706 | 			});
  707 | 			// CHECKSTYLE:ON
  708 | 			return chatClientResponse != null ? chatClientResponse : ChatClientResponse.builder().build();
  709 | 		}
  710 | 
  711 | 		/**
  712 | 		 * Whether a safety filter or a refusal of the model stopped the generation that
  713 | 		 * {@link #getContentFromChatResponse(ChatResponse)} reads, which is the first.
  714 | 		 */
  715 | 		private static boolean isContentFiltered(@Nullable ChatResponse chatResponse) {
  716 | 			Generation result = (chatResponse != null) ? chatResponse.getResult() : null;
  717 | 			return result != null && result.getMetadata().getFinishReasonCategory() == FinishReasonCategory.FILTERED;
  718 | 		}
  719 | 
  720 | 		private static @Nullable String getContentFromChatResponse(@Nullable ChatResponse chatResponse) {
  721 | 			return Optional.ofNullable(chatResponse)
  722 | 				.map(ChatResponse::getResult)
  723 | 				.map(Generation::getOutput)
  724 | 				.map(AbstractMessage::getText)
  725 | 				.orElse(null);
  726 | 		}
  727 | 
  728 | 	}
  729 | 
  730 | 	public static class DefaultStreamResponseSpec implements StreamResponseSpec {
  731 | 
  732 | 		private final ChatClientRequest request;
  733 | 
  734 | 		private final BaseAdvisorChain advisorChain;
  735 | 
  736 | 		private final ObservationRegistry observationRegistry;
  737 | 
  738 | 		private final ChatClientObservationConvention observationConvention;
  739 | 
  740 | 		public DefaultStreamResponseSpec(ChatClientRequest chatClientRequest, BaseAdvisorChain advisorChain,
  741 | 				ObservationRegistry observationRegistry, ChatClientObservationConvention observationConvention) {
  742 | 			Assert.notNull(chatClientRequest, "chatClientRequest cannot be null");
  743 | 			Assert.notNull(advisorChain, "advisorChain cannot be null");
  744 | 			Assert.notNull(observationRegistry, "observationRegistry cannot be null");
  745 | 			Assert.notNull(observationConvention, "observationConvention cannot be null");
  746 | 
  747 | 			this.request = chatClientRequest;
  748 | 			this.advisorChain = advisorChain;
  749 | 			this.observationRegistry = observationRegistry;
  750 | 			this.observationConvention = observationConvention;
  751 | 		}
  752 | 
  753 | 		private Flux<ChatClientResponse> doGetObservableFluxChatResponse(ChatClientRequest chatClientRequest) {
  754 | 			return Flux.deferContextual(contextView -> {
  755 | 
  756 | 				ChatClientObservationContext observationContext = ChatClientObservationContext.builder()
  757 | 					.request(chatClientRequest)
  758 | 					.advisors(this.advisorChain.getStreamAdvisors())
  759 | 					.stream(true)
  760 | 					.build();
  761 | 
  762 | 				Observation observation = ChatClientObservationDocumentation.AI_CHAT_CLIENT.observation(
  763 | 						this.observationConvention, DEFAULT_CHAT_CLIENT_OBSERVATION_CONVENTION,
  764 | 						() -> observationContext, this.observationRegistry);
  765 | 
  766 | 				Observation parentObservation = contextView.getOrDefault(ObservationThreadLocalAccessor.KEY, null);
  767 | 				observation.parentObservation(parentObservation);
  768 | 				// Briefly make the parent observation current while starting this one, so
  769 | 				// Micrometer tracing derives the span's parent from the parent
  770 | 				// observation rather than from whatever scope happens to be open on the
  771 | 				// current thread (e.g. the servlet HTTP span). This keeps span parenting
  772 | 				// correct without relying on automatic context propagation.
  773 | 				try (Observation.Scope ignored = parentObservation != null ? parentObservation.openScope()
  774 | 						: Observation.Scope.NOOP) {
  775 | 					observation.start();
  776 | 				}
  777 | 
  778 | 				// @formatter:off
  779 | 				// Apply the advisor chain that terminates with the ChatModelStreamAdvisor.
  780 | 				Flux<ChatClientResponse> chatClientResponse = this.advisorChain.nextStream(chatClientRequest)
  781 | 						.doOnError(observation::error)
  782 | 						.doFinally(s -> observation.stop())
  783 | 						.contextWrite(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, observation));
  784 | 				// @formatter:on
  785 | 				return CHAT_CLIENT_MESSAGE_AGGREGATOR.aggregateChatClientResponse(chatClientResponse,
  786 | 						observationContext::setResponse);
  787 | 			});
  788 | 		}
  789 | 
  790 | 		@Override
  791 | 		public Flux<ChatClientResponse> chatClientResponse() {
  792 | 			return doGetObservableFluxChatResponse(this.request);
  793 | 		}
  794 | 
  795 | 		@Override
  796 | 		@SuppressWarnings("NullAway") // https://github.com/uber/NullAway/issues/1290
  797 | 		public Flux<ChatResponse> chatResponse() {
  798 | 			return doGetObservableFluxChatResponse(this.request).mapNotNull(ChatClientResponse::chatResponse);
  799 | 		}
  800 | 
  801 | 		@Override
  802 | 		public Flux<String> content() {
  803 | 			// @formatter:off
  804 | 			return chatResponse()
  805 | 					.map(r -> Optional.ofNullable(r.getResult())
  806 | 							.map(Generation::getOutput)
  807 | 							.map(AbstractMessage::getText)
  808 | 							.orElse(""))
  809 | 					.filter(StringUtils::hasLength);
  810 | 			// @formatter:on
  811 | 		}
  812 | 
  813 | 	}
  814 | 
  815 | 	public static class DefaultChatClientRequestSpec implements ChatClientRequestSpec {
  816 | 
  817 | 		private final ObservationRegistry observationRegistry;
  818 | 
  819 | 		private final ChatClientObservationConvention chatClientObservationConvention;
  820 | 
  821 | 		private final @Nullable AdvisorObservationConvention advisorObservationConvention;
  822 | 
  823 | 		private final ChatModel chatModel;
  824 | 
  825 | 		private final List<Media> media = new ArrayList<>();
  826 | 
  827 | 		private final List<ToolCallback> toolCallbacks = new ArrayList<>();
  828 | 
  829 | 		private final List<ToolCallbackProvider> toolCallbackProviders = new ArrayList<>();
  830 | 
  831 | 		private final List<Message> messages = new ArrayList<>();
  832 | 
  833 | 		private final Map<String, Object> userParams = new HashMap<>();
  834 | 
  835 | 		private final Map<String, Object> userMetadata = new HashMap<>();
  836 | 
  837 | 		private final Map<String, Object> systemParams = new HashMap<>();
  838 | 
  839 | 		private final Map<String, Object> systemMetadata = new HashMap<>();
  840 | 
  841 | 		private final List<Advisor> advisors = new ArrayList<>();
  842 | 
  843 | 		private final Map<String, Object> advisorParams = new HashMap<>();
  844 | 
  845 | 		private final Map<String, Object> toolContext = new HashMap<>();
  846 | 
  847 | 		private TemplateRenderer templateRenderer;
  848 | 
  849 | 		private @Nullable String userText;
  850 | 
  851 | 		private @Nullable String systemText;
  852 | 
  853 | 		private ChatOptions.@Nullable Builder<?> optionsCustomizer;
  854 | 
  855 | 		private final ToolCallingAdvisor.Builder<?> toolCallingAdvisorBuilder;
  856 | 
  857 | 		/* copy constructor */
  858 | 		DefaultChatClientRequestSpec(DefaultChatClientRequestSpec ccr) {
  859 | 			this(ccr.chatModel, ccr.userText, ccr.userParams, ccr.userMetadata, ccr.systemText, ccr.systemParams,
  860 | 					ccr.systemMetadata, ccr.toolCallbacks, ccr.toolCallbackProviders, ccr.messages, ccr.media,
  861 | 					ccr.optionsCustomizer, ccr.advisors, ccr.advisorParams, ccr.observationRegistry,
  862 | 					ccr.chatClientObservationConvention, ccr.toolContext, ccr.templateRenderer,
  863 | 					ccr.advisorObservationConvention, ccr.toolCallingAdvisorBuilder);
  864 | 		}
  865 | 
  866 | 		public DefaultChatClientRequestSpec(ChatModel chatModel, @Nullable String userText,
  867 | 				Map<String, Object> userParams, Map<String, Object> userMetadata, @Nullable String systemText,
  868 | 				Map<String, Object> systemParams, Map<String, Object> systemMetadata, List<ToolCallback> toolCallbacks,
  869 | 				List<ToolCallbackProvider> toolCallbackProviders, List<Message> messages, List<Media> media,
  870 | 				ChatOptions.@Nullable Builder<?> customizer, List<Advisor> advisors, Map<String, Object> advisorParams,
  871 | 				ObservationRegistry observationRegistry,
  872 | 				@Nullable ChatClientObservationConvention chatClientObservationConvention,
  873 | 				Map<String, Object> toolContext, @Nullable TemplateRenderer templateRenderer,
  874 | 				@Nullable AdvisorObservationConvention advisorObservationConvention,
  875 | 				ToolCallingAdvisor.Builder<?> toolCallingAdvisorBuilder) {
  876 | 
  877 | 			Assert.notNull(chatModel, "chatModel cannot be null");
  878 | 			Assert.notNull(userParams, "userParams cannot be null");
  879 | 			Assert.notNull(userMetadata, "userMetadata cannot be null");
  880 | 			Assert.notNull(systemParams, "systemParams cannot be null");
  881 | 			Assert.notNull(systemMetadata, "systemMetadata cannot be null");
  882 | 			Assert.notNull(toolCallbacks, "toolCallbacks cannot be null");
  883 | 			Assert.notNull(toolCallbackProviders, "toolCallbackProviders cannot be null");
  884 | 			Assert.notNull(messages, "messages cannot be null");
  885 | 			Assert.notNull(media, "media cannot be null");
  886 | 			Assert.notNull(advisors, "advisors cannot be null");
  887 | 			Assert.notNull(advisorParams, "advisorParams cannot be null");
  888 | 			Assert.notNull(observationRegistry, "observationRegistry cannot be null");
  889 | 			Assert.notNull(toolContext, "toolContext cannot be null");
  890 | 			Assert.notNull(toolCallingAdvisorBuilder, "toolCallingAdvisorBuilder cannot be null");
  891 | 
  892 | 			this.chatModel = chatModel;
  893 | 			this.toolCallingAdvisorBuilder = toolCallingAdvisorBuilder;
  894 | 			this.optionsCustomizer = customizer != null ? customizer.clone() : null;
  895 | 
  896 | 			this.userText = userText;
  897 | 			this.userParams.putAll(userParams);
  898 | 			this.userMetadata.putAll(userMetadata);
  899 | 
  900 | 			this.systemText = systemText;
  901 | 			this.systemParams.putAll(systemParams);
  902 | 			this.systemMetadata.putAll(systemMetadata);
  903 | 
  904 | 			this.toolCallbacks.addAll(toolCallbacks);
  905 | 			this.toolCallbackProviders.addAll(toolCallbackProviders);
  906 | 			this.messages.addAll(messages);
  907 | 			this.media.addAll(media);
  908 | 			this.advisors.addAll(advisors);
  909 | 			this.advisorParams.putAll(advisorParams);
  910 | 			this.observationRegistry = observationRegistry;
  911 | 			this.chatClientObservationConvention = chatClientObservationConvention != null
  912 | 					? chatClientObservationConvention : DEFAULT_CHAT_CLIENT_OBSERVATION_CONVENTION;
  913 | 			this.toolContext.putAll(toolContext);
  914 | 			this.templateRenderer = templateRenderer != null ? templateRenderer : DEFAULT_TEMPLATE_RENDERER;
  915 | 			this.advisorObservationConvention = advisorObservationConvention;
  916 | 		}
  917 | 
  918 | 		public @Nullable String getUserText() {
  919 | 			return this.userText;
  920 | 		}
  921 | 
  922 | 		public Map<String, Object> getUserParams() {
  923 | 			return this.userParams;
  924 | 		}
  925 | 
  926 | 		public Map<String, Object> getUserMetadata() {
  927 | 			return this.userMetadata;
  928 | 		}
  929 | 
  930 | 		public @Nullable String getSystemText() {
  931 | 			return this.systemText;
  932 | 		}
  933 | 
  934 | 		public Map<String, Object> getSystemParams() {
  935 | 			return this.systemParams;
  936 | 		}
  937 | 
  938 | 		public Map<String, Object> getSystemMetadata() {
  939 | 			return this.systemMetadata;
  940 | 		}
  941 | 
  942 | 		public List<Advisor> getAdvisors() {
  943 | 			return this.advisors;
  944 | 		}
  945 | 
  946 | 		public Map<String, Object> getAdvisorParams() {
  947 | 			return this.advisorParams;
  948 | 		}
  949 | 
  950 | 		public List<Message> getMessages() {
  951 | 			return this.messages;
  952 | 		}
  953 | 
  954 | 		public List<Media> getMedia() {
  955 | 			return this.media;
  956 | 		}
  957 | 
  958 | 		public List<ToolCallback> getToolCallbacks() {
  959 | 			return this.toolCallbacks;
  960 | 		}
  961 | 
  962 | 		public List<ToolCallbackProvider> getToolCallbackProviders() {
  963 | 			return this.toolCallbackProviders;
  964 | 		}
  965 | 
  966 | 		public Map<String, Object> getToolContext() {
  967 | 			return this.toolContext;
  968 | 		}
  969 | 
  970 | 		public TemplateRenderer getTemplateRenderer() {
  971 | 			return this.templateRenderer;
  972 | 		}
  973 | 
  974 | 		/* package */ ChatModel getChatModel() {
  975 | 			return this.chatModel;
  976 | 		}
  977 | 
  978 | 		/* package */ ChatOptions.@Nullable Builder<?> getOptionsCustomizer() {
  979 | 			return this.optionsCustomizer;
  980 | 		}
  981 | 
  982 | 		/**
  983 | 		 * Return a {@link ChatClient.Builder} to create a new {@link ChatClient} whose
  984 | 		 * settings are replicated from this {@link ChatClientRequest}.
  985 | 		 */
  986 | 		@Override
  987 | 		public Builder mutate() {
  988 | 			DefaultChatClientBuilder builder = (DefaultChatClientBuilder) ChatClient
  989 | 				.builder(this.chatModel, this.observationRegistry, this.chatClientObservationConvention,
  990 | 						this.advisorObservationConvention, this.toolCallingAdvisorBuilder)
  991 | 				.defaultTemplateRenderer(this.templateRenderer)
  992 | 				.defaultTools(this.toolCallbacks.toArray(new ToolCallback[0]))
  993 | 				.defaultTools((Object[]) this.toolCallbackProviders.toArray(new ToolCallbackProvider[0]))
  994 | 				.defaultToolContext(this.toolContext);
  995 | 
  996 | 			if (!CollectionUtils.isEmpty(this.advisors)) {
  997 | 				builder.defaultAdvisors(a -> a.advisors(this.advisors).params(this.advisorParams));
  998 | 			}
  999 | 
 1000 | 			if (StringUtils.hasText(this.userText)) {
 1001 | 				String text = this.userText;
 1002 | 				builder.defaultUser(u -> u.text(text)
 1003 | 					.params(this.userParams)
 1004 | 					.media(this.media.toArray(new Media[0]))
 1005 | 					.metadata(this.userMetadata));
 1006 | 			}
 1007 | 
 1008 | 			if (StringUtils.hasText(this.systemText)) {
 1009 | 				String text = this.systemText;
 1010 | 				builder.defaultSystem(s -> s.text(text).params(this.systemParams).metadata(this.systemMetadata));
 1011 | 			}
 1012 | 
 1013 | 			if (this.optionsCustomizer != null) {
 1014 | 				builder.defaultOptions(this.optionsCustomizer);
 1015 | 			}
 1016 | 
 1017 | 			builder.addMessages(this.messages);
 1018 | 
 1019 | 			return builder;
 1020 | 		}
 1021 | 
 1022 | 		@Override
 1023 | 		public ChatClientRequestSpec advisors(Consumer<ChatClient.AdvisorSpec> consumer) {
 1024 | 			Assert.notNull(consumer, "consumer cannot be null");
 1025 | 			var advisorSpec = new DefaultAdvisorSpec();
 1026 | 			consumer.accept(advisorSpec);
 1027 | 			this.advisorParams.putAll(advisorSpec.getParams());
 1028 | 			this.advisors.addAll(advisorSpec.getAdvisors());
 1029 | 			return this;
 1030 | 		}
 1031 | 
 1032 | 		@Override
 1033 | 		public ChatClientRequestSpec advisors(Advisor... advisors) {
 1034 | 			Assert.notNull(advisors, "advisors cannot be null");
 1035 | 			Assert.noNullElements(advisors, "advisors cannot contain null elements");
 1036 | 			this.advisors.addAll(Arrays.asList(advisors));
 1037 | 			return this;
 1038 | 		}
 1039 | 
 1040 | 		@Override
 1041 | 		public ChatClientRequestSpec advisors(List<Advisor> advisors) {
 1042 | 			Assert.notNull(advisors, "advisors cannot be null");
 1043 | 			Assert.noNullElements(advisors, "advisors cannot contain null elements");
 1044 | 			this.advisors.addAll(advisors);
 1045 | 			return this;
 1046 | 		}
 1047 | 
 1048 | 		@Override
 1049 | 		public ChatClientRequestSpec messages(Message... messages) {
 1050 | 			Assert.notNull(messages, "messages cannot be null");
 1051 | 			Assert.noNullElements(messages, "messages cannot contain null elements");
 1052 | 			this.messages.addAll(List.of(messages));
 1053 | 			return this;
 1054 | 		}
 1055 | 
 1056 | 		@Override
 1057 | 		public ChatClientRequestSpec messages(List<Message> messages) {
 1058 | 			Assert.notNull(messages, "messages cannot be null");
 1059 | 			Assert.noNullElements(messages, "messages cannot contain null elements");
 1060 | 			this.messages.addAll(messages);
 1061 | 			return this;
 1062 | 		}
 1063 | 
 1064 | 		@Override
 1065 | 		public <B extends ChatOptions.Builder<?>> ChatClientRequestSpec options(B customizer) {
 1066 | 			Assert.notNull(customizer, "customizer cannot be null");
 1067 | 			this.optionsCustomizer = customizer;
 1068 | 			return this;
 1069 | 		}
 1070 | 
 1071 | 		@Override
 1072 | 		public ChatClientRequestSpec toolCallbacks(ToolCallback... toolCallbacks) {
 1073 | 			return tools(toolCallbacks);
 1074 | 		}
 1075 | 
 1076 | 		@Override
 1077 | 		public ChatClientRequestSpec toolCallbacks(List<ToolCallback> toolCallbacks) {
 1078 | 			return tools(toolCallbacks);
 1079 | 		}
 1080 | 
 1081 | 		@Override
 1082 | 		public ChatClientRequestSpec tools(Object... toolObjects) {
 1083 | 			Assert.notNull(toolObjects, "toolObjects cannot be null");
 1084 | 			Assert.noNullElements(toolObjects, "toolObjects cannot contain null elements");
 1085 | 
 1086 | 			List<Object> pojos = new ArrayList<>();
 1087 | 			for (Object toolObject : toolObjects) {
 1088 | 				if (toolObject instanceof ToolCallback toolCallback) {
 1089 | 					this.toolCallbacks.add(toolCallback);
 1090 | 				}
 1091 | 				else if (toolObject instanceof ToolCallbackProvider toolCallbackProvider) {
 1092 | 					this.toolCallbackProviders.add(toolCallbackProvider);
 1093 | 				}
 1094 | 				else if (toolObject instanceof ToolCallback[] callbacks) {
 1095 | 					Assert.noNullElements(callbacks, "toolCallbacks cannot contain null elements");
 1096 | 					this.toolCallbacks.addAll(Arrays.asList(callbacks));
 1097 | 				}
 1098 | 				else if (toolObject instanceof ToolCallbackProvider[] providers) {
 1099 | 					Assert.noNullElements(providers, "toolCallbackProviders cannot contain null elements");
 1100 | 					this.toolCallbackProviders.addAll(Arrays.asList(providers));
 1101 | 				}
 1102 | 				else if (toolObject instanceof Collection<?> collection) {
 1103 | 					Assert.noNullElements(collection, "toolObjects collection cannot contain null elements");
 1104 | 					for (Object element : collection) {
 1105 | 						if (element instanceof ToolCallback toolCallback) {
 1106 | 							this.toolCallbacks.add(toolCallback);
 1107 | 						}
 1108 | 						else if (element instanceof ToolCallbackProvider toolCallbackProvider) {
 1109 | 							this.toolCallbackProviders.add(toolCallbackProvider);
 1110 | 						}
 1111 | 						else {
 1112 | 							pojos.add(element);
 1113 | 						}
 1114 | 					}
 1115 | 				}
 1116 | 				else {
 1117 | 					pojos.add(toolObject);
 1118 | 				}
 1119 | 			}
 1120 | 
 1121 | 			if (!pojos.isEmpty()) {
 1122 | 				this.toolCallbacks.addAll(Arrays.asList(ToolCallbacks.from(pojos.toArray())));
 1123 | 			}
 1124 | 
 1125 | 			return this;
 1126 | 		}
 1127 | 
 1128 | 		@Override
 1129 | 		public ChatClientRequestSpec toolCallbacks(ToolCallbackProvider... toolCallbackProviders) {
 1130 | 			return tools(toolCallbackProviders);
 1131 | 		}
 1132 | 
 1133 | 		@Override
 1134 | 		public ChatClientRequestSpec toolContext(Map<String, Object> toolContext) {
 1135 | 			Assert.notNull(toolContext, "context cannot be null");
 1136 | 			Assert.noNullElements(toolContext.keySet(), "context keys cannot contain null elements");
 1137 | 			Assert.noNullElements(toolContext.values(), "context values cannot contain null elements");
 1138 | 			toolContext.keySet().forEach(key -> Assert.hasText(key, "context key cannot be null or empty"));
 1139 | 			this.toolContext.putAll(toolContext);
 1140 | 			return this;
 1141 | 		}
 1142 | 
 1143 | 		@Override
 1144 | 		public ChatClientRequestSpec system(String text) {
 1145 | 			Assert.hasText(text, "text cannot be null or empty");
 1146 | 			this.systemText = text;
 1147 | 			return this;
 1148 | 		}
 1149 | 
 1150 | 		@Override
 1151 | 		public ChatClientRequestSpec system(Resource text, Charset charset) {
 1152 | 			Assert.notNull(text, "text cannot be null");
 1153 | 			Assert.notNull(charset, "charset cannot be null");
 1154 | 
 1155 | 			try {
 1156 | 				this.systemText = text.getContentAsString(charset);
 1157 | 			}
 1158 | 			catch (IOException e) {
 1159 | 				throw new RuntimeException(e);
 1160 | 			}
 1161 | 			return this;
 1162 | 		}
 1163 | 
 1164 | 		@Override
 1165 | 		public ChatClientRequestSpec system(Resource text) {
 1166 | 			Assert.notNull(text, "text cannot be null");
 1167 | 			return this.system(text, Charset.defaultCharset());
 1168 | 		}
 1169 | 
 1170 | 		@Override
 1171 | 		public ChatClientRequestSpec system(Consumer<PromptSystemSpec> consumer) {
 1172 | 			Assert.notNull(consumer, "consumer cannot be null");
 1173 | 
 1174 | 			var systemSpec = new DefaultPromptSystemSpec();
 1175 | 			consumer.accept(systemSpec);
 1176 | 			this.systemText = StringUtils.hasText(systemSpec.text()) ? systemSpec.text() : this.systemText;
 1177 | 			this.systemParams.putAll(systemSpec.params());
 1178 | 			this.systemMetadata.putAll(systemSpec.metadata());
 1179 | 			return this;
 1180 | 		}
 1181 | 
 1182 | 		@Override
 1183 | 		public ChatClientRequestSpec user(String text) {
 1184 | 			Assert.hasText(text, "text cannot be null or empty");
 1185 | 			this.userText = text;
 1186 | 			return this;
 1187 | 		}
 1188 | 
 1189 | 		@Override
 1190 | 		public ChatClientRequestSpec user(Resource text, Charset charset) {
 1191 | 			Assert.notNull(text, "text cannot be null");
 1192 | 			Assert.notNull(charset, "charset cannot be null");
 1193 | 
 1194 | 			try {
 1195 | 				this.userText = text.getContentAsString(charset);
 1196 | 			}
 1197 | 			catch (IOException e) {
 1198 | 				throw new RuntimeException(e);
 1199 | 			}
 1200 | 			return this;
 1201 | 		}
 1202 | 
 1203 | 		@Override
 1204 | 		public ChatClientRequestSpec user(Resource text) {
 1205 | 			Assert.notNull(text, "text cannot be null");
 1206 | 			return this.user(text, Charset.defaultCharset());
 1207 | 		}
 1208 | 
 1209 | 		@Override
 1210 | 		public ChatClientRequestSpec user(Consumer<PromptUserSpec> consumer) {
 1211 | 			Assert.notNull(consumer, "consumer cannot be null");
 1212 | 
 1213 | 			var us = new DefaultPromptUserSpec();
 1214 | 			consumer.accept(us);
 1215 | 			this.userText = StringUtils.hasText(us.text()) ? us.text() : this.userText;
 1216 | 			this.userParams.putAll(us.params());
 1217 | 			this.media.addAll(us.media());
 1218 | 			this.userMetadata.putAll(us.metadata());
 1219 | 			return this;
 1220 | 		}
 1221 | 
 1222 | 		@Override
 1223 | 		public ChatClientRequestSpec templateRenderer(TemplateRenderer templateRenderer) {
 1224 | 			Assert.notNull(templateRenderer, "templateRenderer cannot be null");
 1225 | 			this.templateRenderer = templateRenderer;
 1226 | 			return this;
 1227 | 		}
 1228 | 
 1229 | 		@Override
 1230 | 		public CallResponseSpec call() {
 1231 | 			BaseAdvisorChain advisorChain = buildAdvisorChain();
 1232 | 			return new DefaultCallResponseSpec(DefaultChatClientUtils.toChatClientRequest(this), advisorChain,
 1233 | 					this.observationRegistry, this.chatClientObservationConvention);
 1234 | 		}
 1235 | 
 1236 | 		@Override
 1237 | 		public StreamResponseSpec stream() {
 1238 | 			BaseAdvisorChain advisorChain = buildAdvisorChain();
 1239 | 			return new DefaultStreamResponseSpec(DefaultChatClientUtils.toChatClientRequest(this), advisorChain,
 1240 | 					this.observationRegistry, this.chatClientObservationConvention);
 1241 | 		}
 1242 | 
 1243 | 		private BaseAdvisorChain buildAdvisorChain() {
 1244 | 			autoRegisterToolCallingAdvisor();
 1245 | 			validateSingleToolAdvisor();
 1246 | 
 1247 | 			// At the stack bottom add the model call advisors.
 1248 | 			// They play the role of the last advisors in the advisor chain.
 1249 | 			List<Advisor> chain = new ArrayList<>(this.advisors);
 1250 | 			chain.add(ChatModelCallAdvisor.builder().chatModel(this.chatModel).build());
 1251 | 			chain.add(ChatModelStreamAdvisor.builder().chatModel(this.chatModel).build());
 1252 | 
 1253 | 			return DefaultAroundAdvisorChain.builder(this.observationRegistry)
 1254 | 				.observationConvention(this.advisorObservationConvention)
 1255 | 				.pushAll(chain)
 1256 | 				.build();
 1257 | 		}
 1258 | 
 1259 | 		/**
 1260 | 		 * Auto-registers a {@link ToolCallingAdvisor} unless auto-registration is
 1261 | 		 * disabled or a {@link ToolAdvisor} is already present in the chain. The advisor
 1262 | 		 * is always registered so that tools injected dynamically at runtime (e.g. by
 1263 | 		 * another advisor) are handled correctly even when no static tools are configured
 1264 | 		 * on the call.
 1265 | 		 * <p>
 1266 | 		 * Disables the advisor's internal conversation history when a
 1267 | 		 * {@link MemoryAdvisor} with a higher order (i.e. downstream in the request
 1268 | 		 * direction) is already registered, since that memory advisor will handle history
 1269 | 		 * for every tool-call iteration.
 1270 | 		 */
 1271 | 		private void autoRegisterToolCallingAdvisor() {
 1272 | 
 1273 | 			boolean autoRegisterDisabled = Boolean.FALSE
 1274 | 				.equals(this.advisorParams.get(ChatClientAttributes.TOOL_CALLING_ADVISOR_AUTO_REGISTER.getKey()));
 1275 | 			if (autoRegisterDisabled) {
 1276 | 				return;
 1277 | 			}
 1278 | 
 1279 | 			boolean hasToolCallingAdvisor = this.advisors.stream().anyMatch(a -> a instanceof ToolAdvisor);
 1280 | 			if (hasToolCallingAdvisor) {
 1281 | 				return;
 1282 | 			}
 1283 | 
 1284 | 			int configuredOrder = this.toolCallingAdvisorBuilder.getAdvisorOrder();
 1285 | 
 1286 | 			boolean hasDownstreamMemoryAdvisor = this.advisors.stream()
 1287 | 				.anyMatch(a -> a instanceof MemoryAdvisor && a.getOrder() > configuredOrder);
 1288 | 
 1289 | 			this.advisors.add(this.toolCallingAdvisorBuilder.copy()
 1290 | 				.conversationHistoryEnabled(!hasDownstreamMemoryAdvisor)
 1291 | 				.build());
 1292 | 		}
 1293 | 
 1294 | 		private void validateSingleToolAdvisor() {
 1295 | 			List<Advisor> toolAdvisors = this.advisors.stream().filter(a -> a instanceof ToolAdvisor).toList();
 1296 | 			if (toolAdvisors.size() > 1) {
 1297 | 				String names = String.join(", ",
 1298 | 						toolAdvisors.stream().map(a -> a.getName() + " (order=" + a.getOrder() + ")").toList());
 1299 | 				throw new IllegalStateException("At most one ToolAdvisor is allowed in the advisor chain, but found "
 1300 | 						+ toolAdvisors.size() + ": [" + names + "]");
 1301 | 			}
 1302 | 		}
 1303 | 
 1304 | 	}
 1305 | 
 1306 | }
```
