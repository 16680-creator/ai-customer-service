<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/DefaultChatClientUtils.java -->
<!-- 正文引用：[[源码:DefaultChatClientUtils.java#L起-L止]] ；代码块头部：(src: DefaultChatClientUtils.java#L起-L止) -->

# DefaultChatClientUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/DefaultChatClientUtils.java` |
| 行数 | 139 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:54.583Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | import java.util.concurrent.ConcurrentHashMap;
   22 | 
   23 | import org.springframework.ai.chat.messages.Message;
   24 | import org.springframework.ai.chat.messages.SystemMessage;
   25 | import org.springframework.ai.chat.messages.UserMessage;
   26 | import org.springframework.ai.chat.prompt.ChatOptions;
   27 | import org.springframework.ai.chat.prompt.Prompt;
   28 | import org.springframework.ai.chat.prompt.Prompt.Builder;
   29 | import org.springframework.ai.chat.prompt.PromptTemplate;
   30 | import org.springframework.ai.model.tool.ToolCallingChatOptions;
   31 | import org.springframework.ai.tool.ToolCallback;
   32 | import org.springframework.util.Assert;
   33 | import org.springframework.util.CollectionUtils;
   34 | import org.springframework.util.StringUtils;
   35 | 
   36 | /**
   37 |  * Utilities for supporting the {@link DefaultChatClient} implementation.
   38 |  *
   39 |  * @author Thomas Vitale
   40 |  * @author Sun Yuhan
   41 |  * @author Sebastien Deleuze
   42 |  * @since 1.0.0
   43 |  */
   44 | final class DefaultChatClientUtils {
   45 | 
   46 | 	private DefaultChatClientUtils() {
   47 | 		// prevents instantiation
   48 | 	}
   49 | 
   50 | 	static ChatClientRequest toChatClientRequest(DefaultChatClient.DefaultChatClientRequestSpec inputRequest) {
   51 | 		Assert.notNull(inputRequest, "inputRequest cannot be null");
   52 | 
   53 | 		/*
   54 | 		 * ==========* MESSAGES * ==========
   55 | 		 */
   56 | 
   57 | 		List<Message> processedMessages = new ArrayList<>();
   58 | 
   59 | 		// System Text => First in the list
   60 | 		String processedSystemText = inputRequest.getSystemText();
   61 | 		if (StringUtils.hasText(processedSystemText)) {
   62 | 			if (!CollectionUtils.isEmpty(inputRequest.getSystemParams())) {
   63 | 				processedSystemText = PromptTemplate.builder()
   64 | 					.template(processedSystemText)
   65 | 					.variables(inputRequest.getSystemParams())
   66 | 					.renderer(inputRequest.getTemplateRenderer())
   67 | 					.build()
   68 | 					.render();
   69 | 			}
   70 | 			processedMessages.add(SystemMessage.builder()
   71 | 				.text(processedSystemText)
   72 | 				.metadata(inputRequest.getSystemMetadata())
   73 | 				.build());
   74 | 		}
   75 | 
   76 | 		// Messages => In the middle of the list
   77 | 		if (!CollectionUtils.isEmpty(inputRequest.getMessages())) {
   78 | 			processedMessages.addAll(inputRequest.getMessages());
   79 | 		}
   80 | 
   81 | 		// User Text => Last in the list
   82 | 		String processedUserText = inputRequest.getUserText();
   83 | 		if (StringUtils.hasText(processedUserText)) {
   84 | 			if (!CollectionUtils.isEmpty(inputRequest.getUserParams())) {
   85 | 				processedUserText = PromptTemplate.builder()
   86 | 					.template(processedUserText)
   87 | 					.variables(inputRequest.getUserParams())
   88 | 					.renderer(inputRequest.getTemplateRenderer())
   89 | 					.build()
   90 | 					.render();
   91 | 			}
   92 | 			processedMessages.add(UserMessage.builder()
   93 | 				.text(processedUserText)
   94 | 				.media(inputRequest.getMedia())
   95 | 				.metadata(inputRequest.getUserMetadata())
   96 | 				.build());
   97 | 		}
   98 | 
   99 | 		/*
  100 | 		 * ==========* OPTIONS * ==========
  101 | 		 */
  102 | 
  103 | 		ChatOptions.Builder<?> builder = inputRequest.getChatModel().getOptions().mutate();
  104 | 		if (inputRequest.getOptionsCustomizer() != null) {
  105 | 			builder = builder.combineWith(inputRequest.getOptionsCustomizer());
  106 | 		}
  107 | 
  108 | 		if (builder instanceof ToolCallingChatOptions.Builder<?> tbuilder) {
  109 | 
  110 | 			List<ToolCallback> toolCallbacks = new ArrayList<>(inputRequest.getToolCallbacks());
  111 | 			for (var provider : inputRequest.getToolCallbackProviders()) {
  112 | 				toolCallbacks.addAll(java.util.List.of(provider.getToolCallbacks()));
  113 | 			}
  114 | 
  115 | 			if (!toolCallbacks.isEmpty()) {
  116 | 				ToolCallingChatOptions.validateToolCallbacks(toolCallbacks);
  117 | 				tbuilder.toolCallbacks(toolCallbacks);
  118 | 			}
  119 | 
  120 | 			if (!inputRequest.getToolContext().isEmpty()) {
  121 | 				tbuilder.toolContext(inputRequest.getToolContext());
  122 | 			}
  123 | 
  124 | 		}
  125 | 
  126 | 		ChatOptions processedChatOptions = builder.build();
  127 | 
  128 | 		/*
  129 | 		 * ==========* REQUEST * ==========
  130 | 		 */
  131 | 
  132 | 		Builder promptBuilder = Prompt.builder().messages(processedMessages).chatOptions(processedChatOptions);
  133 | 		return ChatClientRequest.builder()
  134 | 			.prompt(promptBuilder.build())
  135 | 			.context(new ConcurrentHashMap<>(inputRequest.getAdvisorParams()))
  136 | 			.build();
  137 | 	}
  138 | 
  139 | }
```
