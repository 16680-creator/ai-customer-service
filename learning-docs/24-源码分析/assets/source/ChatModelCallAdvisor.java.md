<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/advisor/ChatModelCallAdvisor.java -->
<!-- 正文引用：[[源码:ChatModelCallAdvisor.java#L起-L止]] ；代码块头部：(src: ChatModelCallAdvisor.java#L起-L止) -->

# ChatModelCallAdvisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/advisor/ChatModelCallAdvisor.java` |
| 行数 | 129 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:55.261Z |

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
   17 | package org.springframework.ai.chat.client.advisor;
   18 | 
   19 | import org.jspecify.annotations.Nullable;
   20 | 
   21 | import org.springframework.ai.chat.client.ChatClientAttributes;
   22 | import org.springframework.ai.chat.client.ChatClientRequest;
   23 | import org.springframework.ai.chat.client.ChatClientResponse;
   24 | import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
   25 | import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
   26 | import org.springframework.ai.chat.model.ChatModel;
   27 | import org.springframework.ai.chat.model.ChatResponse;
   28 | import org.springframework.ai.chat.prompt.Prompt;
   29 | import org.springframework.ai.model.tool.StructuredOutputChatOptions;
   30 | import org.springframework.core.Ordered;
   31 | import org.springframework.util.Assert;
   32 | import org.springframework.util.StringUtils;
   33 | 
   34 | /**
   35 |  * A {@link CallAdvisor} that uses a {@link ChatModel} to generate a response.
   36 |  *
   37 |  * @author Thomas Vitale
   38 |  * @author Christian Tzolov
   39 |  * @author Minju Kim
   40 |  * @since 1.0.0
   41 |  */
   42 | public final class ChatModelCallAdvisor implements CallAdvisor {
   43 | 
   44 | 	private final ChatModel chatModel;
   45 | 
   46 | 	private ChatModelCallAdvisor(ChatModel chatModel) {
   47 | 		Assert.notNull(chatModel, "chatModel cannot be null");
   48 | 		this.chatModel = chatModel;
   49 | 	}
   50 | 
   51 | 	@Override
   52 | 	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
   53 | 		Assert.notNull(chatClientRequest, "the chatClientRequest cannot be null");
   54 | 
   55 | 		ChatClientRequest formattedChatClientRequest = augmentWithFormatInstructions(chatClientRequest);
   56 | 
   57 | 		ChatResponse chatResponse = this.chatModel.call(formattedChatClientRequest.prompt());
   58 | 
   59 | 		return ChatClientResponse.builder()
   60 | 			.chatResponse(chatResponse)
   61 | 			.context(formattedChatClientRequest.context())
   62 | 			.build();
   63 | 	}
   64 | 
   65 | 	private static ChatClientRequest augmentWithFormatInstructions(ChatClientRequest chatClientRequest) {
   66 | 
   67 | 		String outputFormat = (String) chatClientRequest.context().get(ChatClientAttributes.OUTPUT_FORMAT.getKey());
   68 | 
   69 | 		String outputSchema = (String) chatClientRequest.context()
   70 | 			.get(ChatClientAttributes.STRUCTURED_OUTPUT_SCHEMA.getKey());
   71 | 
   72 | 		if (!StringUtils.hasText(outputFormat) && !StringUtils.hasText(outputSchema)) {
   73 | 			return chatClientRequest;
   74 | 		}
   75 | 
   76 | 		boolean usesNativeStructuredOutput = chatClientRequest.context()
   77 | 			.containsKey(ChatClientAttributes.STRUCTURED_OUTPUT_NATIVE.getKey());
   78 | 
   79 | 		if (usesNativeStructuredOutput && StringUtils.hasText(outputSchema) && chatClientRequest.prompt()
   80 | 			.getOptions() instanceof StructuredOutputChatOptions structuredOutputChatOptions) {
   81 | 
   82 | 			var augmentedOptions = structuredOutputChatOptions.mutate().outputSchema(outputSchema).build();
   83 | 			Prompt augmentedPrompt = chatClientRequest.prompt().mutate().chatOptions(augmentedOptions).build();
   84 | 
   85 | 			return ChatClientRequest.builder().prompt(augmentedPrompt).context(chatClientRequest.context()).build();
   86 | 		}
   87 | 
   88 | 		Prompt augmentedPrompt = chatClientRequest.prompt()
   89 | 			.augmentUserMessage(userMessage -> userMessage.mutate()
   90 | 				.text(userMessage.getText() + System.lineSeparator() + outputFormat)
   91 | 				.build());
   92 | 
   93 | 		return ChatClientRequest.builder().prompt(augmentedPrompt).context(chatClientRequest.context()).build();
   94 | 	}
   95 | 
   96 | 	@Override
   97 | 	public String getName() {
   98 | 		return "call";
   99 | 	}
  100 | 
  101 | 	@Override
  102 | 	public int getOrder() {
  103 | 		return Ordered.LOWEST_PRECEDENCE;
  104 | 	}
  105 | 
  106 | 	public static Builder builder() {
  107 | 		return new Builder();
  108 | 	}
  109 | 
  110 | 	public static final class Builder {
  111 | 
  112 | 		private @Nullable ChatModel chatModel;
  113 | 
  114 | 		private Builder() {
  115 | 		}
  116 | 
  117 | 		public Builder chatModel(ChatModel chatModel) {
  118 | 			this.chatModel = chatModel;
  119 | 			return this;
  120 | 		}
  121 | 
  122 | 		public ChatModelCallAdvisor build() {
  123 | 			Assert.state(this.chatModel != null, "chatModel cannot be null");
  124 | 			return new ChatModelCallAdvisor(this.chatModel);
  125 | 		}
  126 | 
  127 | 	}
  128 | 
  129 | }
```
