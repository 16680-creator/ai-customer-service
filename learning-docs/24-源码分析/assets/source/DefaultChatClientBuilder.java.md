<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/DefaultChatClientBuilder.java -->
<!-- 正文引用：[[源码:DefaultChatClientBuilder.java#L起-L止]] ；代码块头部：(src: DefaultChatClientBuilder.java#L起-L止) -->

# DefaultChatClientBuilder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/DefaultChatClientBuilder.java` |
| 行数 | 248 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:54.505Z |

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
   20 | import java.nio.charset.Charset;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | import java.util.Objects;
   24 | import java.util.function.Consumer;
   25 | 
   26 | import io.micrometer.observation.ObservationRegistry;
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.ai.chat.client.ChatClient.Builder;
   30 | import org.springframework.ai.chat.client.ChatClient.PromptSystemSpec;
   31 | import org.springframework.ai.chat.client.ChatClient.PromptUserSpec;
   32 | import org.springframework.ai.chat.client.DefaultChatClient.DefaultChatClientRequestSpec;
   33 | import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
   34 | import org.springframework.ai.chat.client.advisor.api.Advisor;
   35 | import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
   36 | import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
   37 | import org.springframework.ai.chat.messages.Message;
   38 | import org.springframework.ai.chat.model.ChatModel;
   39 | import org.springframework.ai.chat.prompt.ChatOptions;
   40 | import org.springframework.ai.model.tool.ToolCallingManager;
   41 | import org.springframework.ai.template.TemplateRenderer;
   42 | import org.springframework.ai.tool.ToolCallback;
   43 | import org.springframework.ai.tool.ToolCallbackProvider;
   44 | import org.springframework.core.io.Resource;
   45 | import org.springframework.util.Assert;
   46 | 
   47 | /**
   48 |  * DefaultChatClientBuilder is a builder class for creating a ChatClient.
   49 |  * <p>
   50 |  * It provides methods to set default values for various properties of the ChatClient.
   51 |  *
   52 |  * @author Mark Pollack
   53 |  * @author Christian Tzolov
   54 |  * @author Josh Long
   55 |  * @author Arjen Poutsma
   56 |  * @author Thomas Vitale
   57 |  * @since 1.0.0
   58 |  */
   59 | public class DefaultChatClientBuilder implements Builder {
   60 | 
   61 | 	protected final DefaultChatClientRequestSpec defaultRequest;
   62 | 
   63 | 	DefaultChatClientBuilder(ChatModel chatModel) {
   64 | 		this(chatModel, ObservationRegistry.NOOP, null, null);
   65 | 	}
   66 | 
   67 | 	@Deprecated(since = "2.0.0", forRemoval = true)
   68 | 	public DefaultChatClientBuilder(ChatModel chatModel, ObservationRegistry observationRegistry,
   69 | 			@Nullable ChatClientObservationConvention chatClientObservationConvention,
   70 | 			@Nullable AdvisorObservationConvention advisorObservationConvention) {
   71 | 		this(chatModel, observationRegistry, chatClientObservationConvention, advisorObservationConvention, null);
   72 | 	}
   73 | 
   74 | 	/**
   75 | 	 * Creates a new {@link DefaultChatClientBuilder}.
   76 | 	 * <p>
   77 | 	 * When {@code toolCallingAdvisorBuilder} is {@code null}, a default
   78 | 	 * {@link org.springframework.ai.chat.client.advisor.ToolCallingAdvisor} is created
   79 | 	 * with a {@link ToolCallingManager} backed by the supplied
   80 | 	 * {@code observationRegistry}.
   81 | 	 * <p>
   82 | 	 * When {@code toolCallingAdvisorBuilder} is non-null it is used as-is. The caller is
   83 | 	 * then responsible for configuring the builder's {@link ToolCallingManager},
   84 | 	 * including any {@link io.micrometer.observation.ObservationRegistry}, since the
   85 | 	 * supplied {@code observationRegistry} will not be automatically applied to it.
   86 | 	 * @param chatModel the chat model to use
   87 | 	 * @param observationRegistry the observation registry for client-level observations;
   88 | 	 * also used to configure the default {@code ToolCallingManager} when
   89 | 	 * {@code toolCallingAdvisorBuilder} is {@code null}
   90 | 	 * @param chatClientObservationConvention optional custom observation convention for
   91 | 	 * the chat client
   92 | 	 * @param advisorObservationConvention optional custom observation convention for
   93 | 	 * advisors
   94 | 	 * @param toolCallingAdvisorBuilder optional builder for the
   95 | 	 * {@link org.springframework.ai.chat.client.advisor.ToolCallingAdvisor}; when
   96 | 	 * {@code null} a default is created
   97 | 	 */
   98 | 	public DefaultChatClientBuilder(ChatModel chatModel, ObservationRegistry observationRegistry,
   99 | 			@Nullable ChatClientObservationConvention chatClientObservationConvention,
  100 | 			@Nullable AdvisorObservationConvention advisorObservationConvention,
  101 | 			ToolCallingAdvisor.@Nullable Builder<?> toolCallingAdvisorBuilder) {
  102 | 		Assert.notNull(chatModel, "the " + ChatModel.class.getName() + " must be non-null");
  103 | 		Assert.notNull(observationRegistry, "the " + ObservationRegistry.class.getName() + " must be non-null");
  104 | 
  105 | 		toolCallingAdvisorBuilder = Objects.requireNonNullElse(toolCallingAdvisorBuilder, ToolCallingAdvisor.builder()
  106 | 			.toolCallingManager(ToolCallingManager.builder().observationRegistry(observationRegistry).build()));
  107 | 
  108 | 		this.defaultRequest = new DefaultChatClientRequestSpec(chatModel, null, Map.of(), Map.of(), null, Map.of(),
  109 | 				Map.of(), List.of(), List.of(), List.of(), List.of(), null, List.of(), Map.of(), observationRegistry,
  110 | 				chatClientObservationConvention, Map.of(), null, advisorObservationConvention,
  111 | 				toolCallingAdvisorBuilder);
  112 | 	}
  113 | 
  114 | 	public ChatClient build() {
  115 | 		return new DefaultChatClient(this.defaultRequest);
  116 | 	}
  117 | 
  118 | 	public Builder clone() {
  119 | 		return this.defaultRequest.mutate();
  120 | 	}
  121 | 
  122 | 	public Builder defaultAdvisors(Advisor... advisors) {
  123 | 		this.defaultRequest.advisors(advisors);
  124 | 		return this;
  125 | 	}
  126 | 
  127 | 	public Builder defaultAdvisors(Consumer<ChatClient.AdvisorSpec> advisorSpecConsumer) {
  128 | 		this.defaultRequest.advisors(advisorSpecConsumer);
  129 | 		return this;
  130 | 	}
  131 | 
  132 | 	public Builder defaultAdvisors(List<Advisor> advisors) {
  133 | 		this.defaultRequest.advisors(advisors);
  134 | 		return this;
  135 | 	}
  136 | 
  137 | 	public Builder defaultOptions(ChatOptions.Builder customizer) {
  138 | 		this.defaultRequest.options(customizer);
  139 | 		return this;
  140 | 	}
  141 | 
  142 | 	public Builder defaultUser(String text) {
  143 | 		this.defaultRequest.user(text);
  144 | 		return this;
  145 | 	}
  146 | 
  147 | 	public Builder defaultUser(Resource text, Charset charset) {
  148 | 		Assert.notNull(text, "text cannot be null");
  149 | 		Assert.notNull(charset, "charset cannot be null");
  150 | 		try {
  151 | 			this.defaultRequest.user(text.getContentAsString(charset));
  152 | 		}
  153 | 		catch (IOException e) {
  154 | 			throw new RuntimeException(e);
  155 | 		}
  156 | 		return this;
  157 | 	}
  158 | 
  159 | 	public Builder defaultUser(Resource text) {
  160 | 		return this.defaultUser(text, Charset.defaultCharset());
  161 | 	}
  162 | 
  163 | 	public Builder defaultUser(Consumer<PromptUserSpec> userSpecConsumer) {
  164 | 		this.defaultRequest.user(userSpecConsumer);
  165 | 		return this;
  166 | 	}
  167 | 
  168 | 	public Builder defaultSystem(String text) {
  169 | 		this.defaultRequest.system(text);
  170 | 		return this;
  171 | 	}
  172 | 
  173 | 	public Builder defaultSystem(Resource text, Charset charset) {
  174 | 		Assert.notNull(text, "text cannot be null");
  175 | 		Assert.notNull(charset, "charset cannot be null");
  176 | 		try {
  177 | 			this.defaultRequest.system(text.getContentAsString(charset));
  178 | 		}
  179 | 		catch (IOException e) {
  180 | 			throw new RuntimeException(e);
  181 | 		}
  182 | 		return this;
  183 | 	}
  184 | 
  185 | 	public Builder defaultSystem(Resource text) {
  186 | 		return this.defaultSystem(text, Charset.defaultCharset());
  187 | 	}
  188 | 
  189 | 	public Builder defaultSystem(Consumer<PromptSystemSpec> systemSpecConsumer) {
  190 | 		this.defaultRequest.system(systemSpecConsumer);
  191 | 		return this;
  192 | 	}
  193 | 
  194 | 	@Override
  195 | 	public Builder defaultTools(Object... toolObjects) {
  196 | 		this.defaultRequest.tools(toolObjects);
  197 | 		return this;
  198 | 	}
  199 | 
  200 | 	/**
  201 | 	 * @deprecated as of 2.0.0, in favor of {@link #defaultTools(Object...)}. To be
  202 | 	 * removed in 3.0.0.
  203 | 	 */
  204 | 	@Deprecated(since = "2.0.0", forRemoval = true)
  205 | 	@Override
  206 | 	public Builder defaultToolCallbacks(ToolCallback... toolCallbacks) {
  207 | 		this.defaultRequest.tools(toolCallbacks);
  208 | 		return this;
  209 | 	}
  210 | 
  211 | 	/**
  212 | 	 * @deprecated as of 2.0.0, in favor of {@link #defaultTools(Consumer)}. To be removed
  213 | 	 * in 3.0.0.
  214 | 	 */
  215 | 	@Deprecated(since = "2.0.0", forRemoval = true)
  216 | 	@Override
  217 | 	public Builder defaultToolCallbacks(List<ToolCallback> toolCallbacks) {
  218 | 		this.defaultRequest.tools(toolCallbacks);
  219 | 		return this;
  220 | 	}
  221 | 
  222 | 	/**
  223 | 	 * @deprecated as of 2.0.0, in favor of {@link #defaultTools(Consumer)}. To be removed
  224 | 	 * in 3.0.0.
  225 | 	 */
  226 | 	@Deprecated(since = "2.0.0", forRemoval = true)
  227 | 	@Override
  228 | 	public Builder defaultToolCallbacks(ToolCallbackProvider... toolCallbackProviders) {
  229 | 		this.defaultRequest.tools(toolCallbackProviders);
  230 | 		return this;
  231 | 	}
  232 | 
  233 | 	public Builder defaultToolContext(Map<String, Object> toolContext) {
  234 | 		this.defaultRequest.toolContext(toolContext);
  235 | 		return this;
  236 | 	}
  237 | 
  238 | 	public Builder defaultTemplateRenderer(TemplateRenderer templateRenderer) {
  239 | 		Assert.notNull(templateRenderer, "templateRenderer cannot be null");
  240 | 		this.defaultRequest.templateRenderer(templateRenderer);
  241 | 		return this;
  242 | 	}
  243 | 
  244 | 	void addMessages(List<Message> messages) {
  245 | 		this.defaultRequest.messages(messages);
  246 | 	}
  247 | 
  248 | }
```
