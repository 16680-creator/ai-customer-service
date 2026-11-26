<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/prompt/Prompt.java -->
<!-- 正文引用：[[源码:Prompt.java#L起-L止]] ；代码块头部：(src: Prompt.java#L起-L止) -->

# Prompt.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/prompt/Prompt.java` |
| 行数 | 333 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:04.283Z |

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
   17 | package org.springframework.ai.chat.prompt;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Arrays;
   21 | import java.util.Collections;
   22 | import java.util.HashMap;
   23 | import java.util.List;
   24 | import java.util.Objects;
   25 | import java.util.function.Function;
   26 | 
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.ai.chat.messages.AssistantMessage;
   30 | import org.springframework.ai.chat.messages.Message;
   31 | import org.springframework.ai.chat.messages.SystemMessage;
   32 | import org.springframework.ai.chat.messages.ToolResponseMessage;
   33 | import org.springframework.ai.chat.messages.UserMessage;
   34 | import org.springframework.ai.model.ModelRequest;
   35 | import org.springframework.util.Assert;
   36 | 
   37 | /**
   38 |  * The Prompt class represents a prompt used in AI model requests. A prompt consists of
   39 |  * one or more messages and additional chat options.
   40 |  *
   41 |  * @author Mark Pollack
   42 |  * @author luocongqiu
   43 |  * @author Thomas Vitale
   44 |  * @author Sebastien Deleuze
   45 |  * @author guan xu
   46 |  */
   47 | public class Prompt implements ModelRequest<List<Message>> {
   48 | 
   49 | 	private final List<Message> messages;
   50 | 
   51 | 	private final @Nullable ChatOptions chatOptions;
   52 | 
   53 | 	public Prompt(String contents) {
   54 | 		this(new UserMessage(contents));
   55 | 	}
   56 | 
   57 | 	public Prompt(Message message) {
   58 | 		this(Collections.singletonList(message));
   59 | 	}
   60 | 
   61 | 	public Prompt(List<Message> messages) {
   62 | 		this(messages, null);
   63 | 	}
   64 | 
   65 | 	public Prompt(Message... messages) {
   66 | 		this(Arrays.asList(messages), null);
   67 | 	}
   68 | 
   69 | 	public Prompt(String contents, @Nullable ChatOptions chatOptions) {
   70 | 		this(new UserMessage(contents), chatOptions);
   71 | 	}
   72 | 
   73 | 	public Prompt(Message message, @Nullable ChatOptions chatOptions) {
   74 | 		this(Collections.singletonList(message), chatOptions);
   75 | 	}
   76 | 
   77 | 	public Prompt(List<Message> messages, @Nullable ChatOptions chatOptions) {
   78 | 		Assert.notNull(messages, "messages cannot be null");
   79 | 		Assert.noNullElements(messages, "messages cannot contain null elements");
   80 | 		this.messages = messages;
   81 | 		this.chatOptions = chatOptions;
   82 | 	}
   83 | 
   84 | 	public String getContents() {
   85 | 		StringBuilder sb = new StringBuilder();
   86 | 		for (Message message : getInstructions()) {
   87 | 			sb.append(message.getText());
   88 | 		}
   89 | 		return sb.toString();
   90 | 	}
   91 | 
   92 | 	@Override
   93 | 	public @Nullable ChatOptions getOptions() {
   94 | 		return this.chatOptions;
   95 | 	}
   96 | 
   97 | 	@Override
   98 | 	public List<Message> getInstructions() {
   99 | 		return this.messages;
  100 | 	}
  101 | 
  102 | 	/**
  103 | 	 * Get the first system message in the prompt. If no system message is found, an empty
  104 | 	 * SystemMessage is returned.
  105 | 	 */
  106 | 	public SystemMessage getSystemMessage() {
  107 | 		for (int i = 0; i <= this.messages.size() - 1; i++) {
  108 | 			Message message = this.messages.get(i);
  109 | 			if (message instanceof SystemMessage systemMessage) {
  110 | 				return systemMessage;
  111 | 			}
  112 | 		}
  113 | 		return new SystemMessage("");
  114 | 	}
  115 | 
  116 | 	/**
  117 | 	 * Get the last user message in the prompt. If no user message is found, an empty
  118 | 	 * UserMessage is returned.
  119 | 	 */
  120 | 	public UserMessage getUserMessage() {
  121 | 		for (int i = this.messages.size() - 1; i >= 0; i--) {
  122 | 			Message message = this.messages.get(i);
  123 | 			if (message instanceof UserMessage userMessage) {
  124 | 				return userMessage;
  125 | 			}
  126 | 		}
  127 | 		return new UserMessage("");
  128 | 	}
  129 | 
  130 | 	/**
  131 | 	 * Get the last user or tool response message in the prompt. If no user or tool
  132 | 	 * response message is found, an empty UserMessage is returned.
  133 | 	 */
  134 | 	public Message getLastUserOrToolResponseMessage() {
  135 | 		for (int i = this.messages.size() - 1; i >= 0; i--) {
  136 | 			Message message = this.messages.get(i);
  137 | 			if (message instanceof UserMessage || message instanceof ToolResponseMessage) {
  138 | 				return message;
  139 | 			}
  140 | 		}
  141 | 		return new UserMessage("");
  142 | 	}
  143 | 
  144 | 	/**
  145 | 	 * Get all system messages in the prompt.
  146 | 	 * @return a list of all system messages in the prompt
  147 | 	 */
  148 | 	public List<SystemMessage> getSystemMessages() {
  149 | 		List<SystemMessage> systemMessages = new ArrayList<>();
  150 | 		for (Message message : this.messages) {
  151 | 			if (message instanceof SystemMessage systemMessage) {
  152 | 				systemMessages.add(systemMessage);
  153 | 			}
  154 | 		}
  155 | 		return systemMessages;
  156 | 	}
  157 | 
  158 | 	/**
  159 | 	 * Get all user messages in the prompt.
  160 | 	 * @return a list of all user messages in the prompt
  161 | 	 */
  162 | 	public List<UserMessage> getUserMessages() {
  163 | 		List<UserMessage> userMessages = new ArrayList<>();
  164 | 		for (Message message : this.messages) {
  165 | 			if (message instanceof UserMessage userMessage) {
  166 | 				userMessages.add(userMessage);
  167 | 			}
  168 | 		}
  169 | 		return userMessages;
  170 | 	}
  171 | 
  172 | 	@Override
  173 | 	public String toString() {
  174 | 		return "Prompt{" + "messages=" + this.messages + ", modelOptions=" + this.chatOptions + '}';
  175 | 	}
  176 | 
  177 | 	@Override
  178 | 	public boolean equals(@Nullable Object o) {
  179 | 		if (this == o) {
  180 | 			return true;
  181 | 		}
  182 | 		if (!(o instanceof Prompt prompt)) {
  183 | 			return false;
  184 | 		}
  185 | 		return Objects.equals(this.messages, prompt.messages) && Objects.equals(this.chatOptions, prompt.chatOptions);
  186 | 	}
  187 | 
  188 | 	@Override
  189 | 	public int hashCode() {
  190 | 		return Objects.hash(this.messages, this.chatOptions);
  191 | 	}
  192 | 
  193 | 	public Prompt copy() {
  194 | 		return new Prompt(instructionsCopy(), this.chatOptions);
  195 | 	}
  196 | 
  197 | 	private List<Message> instructionsCopy() {
  198 | 		List<Message> messagesCopy = new ArrayList<>();
  199 | 		this.messages.forEach(message -> {
  200 | 			if (message instanceof UserMessage userMessage) {
  201 | 				messagesCopy.add(userMessage.copy());
  202 | 			}
  203 | 			else if (message instanceof SystemMessage systemMessage) {
  204 | 				messagesCopy.add(systemMessage.copy());
  205 | 			}
  206 | 			else if (message instanceof AssistantMessage assistantMessage) {
  207 | 				messagesCopy.add(assistantMessage.copy());
  208 | 			}
  209 | 			else if (message instanceof ToolResponseMessage toolResponseMessage) {
  210 | 				messagesCopy.add(ToolResponseMessage.builder()
  211 | 					.responses(new ArrayList<>(toolResponseMessage.getResponses()))
  212 | 					.metadata(new HashMap<>(toolResponseMessage.getMetadata()))
  213 | 					.build());
  214 | 			}
  215 | 			else {
  216 | 				throw new IllegalArgumentException("Unsupported message type: " + message.getClass().getName());
  217 | 			}
  218 | 		});
  219 | 
  220 | 		return messagesCopy;
  221 | 	}
  222 | 
  223 | 	/**
  224 | 	 * Augments the first system message in the prompt with the provided function. If no
  225 | 	 * system message is found, a new one is created with the provided text.
  226 | 	 * @return a new {@link Prompt} instance with the augmented system message.
  227 | 	 */
  228 | 	public Prompt augmentSystemMessage(Function<SystemMessage, SystemMessage> systemMessageAugmenter) {
  229 | 		var messagesCopy = new ArrayList<>(this.messages);
  230 | 		boolean found = false;
  231 | 		for (int i = 0; i < messagesCopy.size(); i++) {
  232 | 			Message message = messagesCopy.get(i);
  233 | 			if (message instanceof SystemMessage systemMessage) {
  234 | 				messagesCopy.set(i, systemMessageAugmenter.apply(systemMessage));
  235 | 				found = true;
  236 | 				break;
  237 | 			}
  238 | 		}
  239 | 		if (!found) {
  240 | 			// If no system message is found, create a new one with the provided text
  241 | 			// and add it as the first item in the list.
  242 | 			messagesCopy.add(0, systemMessageAugmenter.apply(new SystemMessage("")));
  243 | 		}
  244 | 		return new Prompt(messagesCopy, this.chatOptions);
  245 | 	}
  246 | 
  247 | 	/**
  248 | 	 * Augments the last system message in the prompt with the provided text. If no system
  249 | 	 * message is found, a new one is created with the provided text.
  250 | 	 * @return a new {@link Prompt} instance with the augmented system message.
  251 | 	 */
  252 | 	public Prompt augmentSystemMessage(String newSystemText) {
  253 | 		return augmentSystemMessage(systemMessage -> systemMessage.mutate().text(newSystemText).build());
  254 | 	}
  255 | 
  256 | 	/**
  257 | 	 * Augments the last user message in the prompt with the provided function. If no user
  258 | 	 * message is found, a new one is created with the provided text.
  259 | 	 * @return a new {@link Prompt} instance with the augmented user message.
  260 | 	 */
  261 | 	public Prompt augmentUserMessage(Function<UserMessage, UserMessage> userMessageAugmenter) {
  262 | 		var messagesCopy = new ArrayList<>(this.messages);
  263 | 		for (int i = messagesCopy.size() - 1; i >= 0; i--) {
  264 | 			Message message = messagesCopy.get(i);
  265 | 			if (message instanceof UserMessage userMessage) {
  266 | 				messagesCopy.set(i, userMessageAugmenter.apply(userMessage));
  267 | 				break;
  268 | 			}
  269 | 			if (i == 0) {
  270 | 				messagesCopy.add(userMessageAugmenter.apply(new UserMessage("")));
  271 | 			}
  272 | 		}
  273 | 
  274 | 		return new Prompt(messagesCopy, this.chatOptions);
  275 | 	}
  276 | 
  277 | 	/**
  278 | 	 * Augments the last user message in the prompt with the provided text. If no user
  279 | 	 * message is found, a new one is created with the provided text.
  280 | 	 * @return a new {@link Prompt} instance with the augmented user message.
  281 | 	 */
  282 | 	public Prompt augmentUserMessage(String newUserText) {
  283 | 		return augmentUserMessage(userMessage -> userMessage.mutate().text(newUserText).build());
  284 | 	}
  285 | 
  286 | 	public Builder mutate() {
  287 | 		Builder builder = new Builder().messages(instructionsCopy());
  288 | 		if (this.chatOptions != null) {
  289 | 			builder.chatOptions(this.chatOptions);
  290 | 		}
  291 | 		return builder;
  292 | 	}
  293 | 
  294 | 	public static Builder builder() {
  295 | 		return new Builder();
  296 | 	}
  297 | 
  298 | 	public static final class Builder {
  299 | 
  300 | 		private @Nullable List<Message> messages;
  301 | 
  302 | 		private @Nullable ChatOptions chatOptions;
  303 | 
  304 | 		public Builder content(@Nullable String content) {
  305 | 			this.messages = List.of(new UserMessage(content));
  306 | 			return this;
  307 | 		}
  308 | 
  309 | 		public Builder messages(Message @Nullable ... messages) {
  310 | 			if (messages != null) {
  311 | 				this.messages = Arrays.asList(messages);
  312 | 			}
  313 | 			return this;
  314 | 		}
  315 | 
  316 | 		public Builder messages(List<Message> messages) {
  317 | 			this.messages = messages;
  318 | 			return this;
  319 | 		}
  320 | 
  321 | 		public Builder chatOptions(ChatOptions chatOptions) {
  322 | 			this.chatOptions = chatOptions;
  323 | 			return this;
  324 | 		}
  325 | 
  326 | 		public Prompt build() {
  327 | 			Assert.state(this.messages != null, "either messages or content needs to be set");
  328 | 			return new Prompt(this.messages, this.chatOptions);
  329 | 		}
  330 | 
  331 | 	}
  332 | 
  333 | }
```
