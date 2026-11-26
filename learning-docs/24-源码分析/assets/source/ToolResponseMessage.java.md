<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/messages/ToolResponseMessage.java -->
<!-- 正文引用：[[源码:ToolResponseMessage.java#L起-L止]] ；代码块头部：(src: ToolResponseMessage.java#L起-L止) -->

# ToolResponseMessage.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/messages/ToolResponseMessage.java` |
| 行数 | 220 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:05.091Z |

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
   17 | package org.springframework.ai.chat.messages;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | import java.util.Map;
   22 | 
   23 | import org.springframework.ai.chat.messages.part.MessagePart;
   24 | import org.springframework.ai.chat.messages.part.ToolResultPart;
   25 | import org.springframework.util.Assert;
   26 | 
   27 | /**
   28 |  * The ToolResponseMessage class represents a message with a function content in a chat
   29 |  * application.
   30 |  * <p>
   31 |  * The message content is an ordered list of {@link MessagePart}s, one
   32 |  * {@link ToolResultPart} per tool result, so a result can carry more than a string, for
   33 |  * example media or an error flag. {@link #getResponses()} is a view over the parts that
   34 |  * keeps only the text of each result. Messages built through the legacy constructor or
   35 |  * the {@link Builder#responses(List)} setter get one text-only result part per response,
   36 |  * so existing code keeps working unchanged.
   37 |  *
   38 |  * @author Christian Tzolov
   39 |  * @author Eric Bottard
   40 |  * @since 1.0.0
   41 |  */
   42 | public class ToolResponseMessage extends AbstractMessage {
   43 | 
   44 | 	/**
   45 | 	 * The responses of this message, derived from its {@link ToolResultPart}s.
   46 | 	 * @deprecated since 2.1.0 in favor of {@link #getResponses()}; kept so existing
   47 | 	 * subclasses that read the field keep compiling. Will be removed in a future release.
   48 | 	 */
   49 | 	@Deprecated(since = "2.1.0", forRemoval = true)
   50 | 	protected final List<ToolResponse> responses;
   51 | 
   52 | 	protected ToolResponseMessage(List<ToolResponse> responses, Map<String, Object> metadata) {
   53 | 		this(metadata, legacyParts(responses));
   54 | 	}
   55 | 
   56 | 	/**
   57 | 	 * Creates a message from an ordered list of parts. The metadata comes first because
   58 | 	 * the legacy constructor already takes a list and a map in the other order and the
   59 | 	 * two would otherwise have the same erasure.
   60 | 	 * @param metadata the message metadata
   61 | 	 * @param parts the parts, in order
   62 | 	 * @since 2.1.0
   63 | 	 */
   64 | 	protected ToolResponseMessage(Map<String, Object> metadata, List<MessagePart> parts) {
   65 | 		super(MessageType.TOOL, parts, metadata);
   66 | 		this.responses = select(ToolResultPart.class).map(ToolResultPart::toToolResponse).toList();
   67 | 	}
   68 | 
   69 | 	private static List<MessagePart> legacyParts(List<ToolResponse> responses) {
   70 | 		Assert.notNull(responses, "responses must not be null");
   71 | 		List<MessagePart> parts = new ArrayList<>(responses.size());
   72 | 		for (ToolResponse response : responses) {
   73 | 			parts.add(ToolResultPart.of(response));
   74 | 		}
   75 | 		return parts;
   76 | 	}
   77 | 
   78 | 	public static Builder builder() {
   79 | 		return new Builder();
   80 | 	}
   81 | 
   82 | 	/**
   83 | 	 * The tool results of this message, in part order.
   84 | 	 * @return an unmodifiable list of result parts
   85 | 	 * @since 2.1.0
   86 | 	 */
   87 | 	public List<ToolResultPart> getResults() {
   88 | 		return select(ToolResultPart.class).toList();
   89 | 	}
   90 | 
   91 | 	/**
   92 | 	 * The tool responses of this message: the text of each {@link ToolResultPart}, in
   93 | 	 * part order. Content a string cannot carry, such as media, is not included; read
   94 | 	 * {@link #getResults()} for it.
   95 | 	 * @return an unmodifiable list of responses
   96 | 	 */
   97 | 	public List<ToolResponse> getResponses() {
   98 | 		return this.responses;
   99 | 	}
  100 | 
  101 | 	/**
  102 | 	 * Tool response messages never had a {@code null} text, even with no responses at
  103 | 	 * all; only {@link AbstractMessage}'s generic rule for an empty parts list would say
  104 | 	 * otherwise.
  105 | 	 */
  106 | 	@Override
  107 | 	public String getText() {
  108 | 		String text = super.getText();
  109 | 		return (text != null) ? text : "";
  110 | 	}
  111 | 
  112 | 	/**
  113 | 	 * A builder pre-populated with this message's parts and metadata. Calling
  114 | 	 * {@link Builder#responses(List)} on it replaces the result parts in place.
  115 | 	 * @return the builder
  116 | 	 * @since 2.1.0
  117 | 	 */
  118 | 	public Builder mutate() {
  119 | 		return new Builder().parts(getParts()).metadata(Map.copyOf(getMetadata()));
  120 | 	}
  121 | 
  122 | 	@Override
  123 | 	public String toString() {
  124 | 		return "ToolResponseMessage{" + "parts=" + getParts() + ", messageType=" + this.messageType + ", metadata="
  125 | 				+ this.metadata + '}';
  126 | 	}
  127 | 
  128 | 	/**
  129 | 	 * A tool response.
  130 | 	 *
  131 | 	 * @param id the id of the tool call this response answers
  132 | 	 * @param name the tool name
  133 | 	 * @param responseData the response text
  134 | 	 */
  135 | 	public record ToolResponse(String id, String name, String responseData) {
  136 | 
  137 | 	}
  138 | 
  139 | 	/**
  140 | 	 * Builder for {@link ToolResponseMessage}.
  141 | 	 * <p>
  142 | 	 * Parts added through {@link #result(ToolResultPart)}, {@link #results(List)} and
  143 | 	 * {@link #parts(List)} keep their order. The legacy {@link #responses(List)} setter
  144 | 	 * replaces the {@link ToolResultPart}s at {@link #build()} with one text-only result
  145 | 	 * part per response: the new results take the place of the first result part, or are
  146 | 	 * appended when there is none.
  147 | 	 */
  148 | 	public static final class Builder {
  149 | 
  150 | 		private final List<MessagePart> explicitParts = new ArrayList<>();
  151 | 
  152 | 		private List<ToolResponse> responses = List.of();
  153 | 
  154 | 		private boolean responsesSet;
  155 | 
  156 | 		private Map<String, Object> metadata = Map.of();
  157 | 
  158 | 		private Builder() {
  159 | 		}
  160 | 
  161 | 		/**
  162 | 		 * Adds a tool result.
  163 | 		 * @param result the result part
  164 | 		 * @return this builder
  165 | 		 * @since 2.1.0
  166 | 		 */
  167 | 		public Builder result(ToolResultPart result) {
  168 | 			Assert.notNull(result, "result must not be null");
  169 | 			this.explicitParts.add(result);
  170 | 			return this;
  171 | 		}
  172 | 
  173 | 		/**
  174 | 		 * Adds tool results, in order.
  175 | 		 * @param results the result parts
  176 | 		 * @return this builder
  177 | 		 * @since 2.1.0
  178 | 		 */
  179 | 		public Builder results(List<ToolResultPart> results) {
  180 | 			Assert.notNull(results, "results must not be null");
  181 | 			Assert.noNullElements(results, "results must not contain null elements");
  182 | 			this.explicitParts.addAll(results);
  183 | 			return this;
  184 | 		}
  185 | 
  186 | 		/**
  187 | 		 * Adds parts, in order, for example when rebuilding a persisted message.
  188 | 		 * @param parts the parts
  189 | 		 * @return this builder
  190 | 		 * @since 2.1.0
  191 | 		 */
  192 | 		public Builder parts(List<MessagePart> parts) {
  193 | 			Assert.notNull(parts, "parts must not be null");
  194 | 			Assert.noNullElements(parts, "parts must not contain null elements");
  195 | 			this.explicitParts.addAll(parts);
  196 | 			return this;
  197 | 		}
  198 | 
  199 | 		public Builder responses(List<ToolResponse> responses) {
  200 | 			this.responses = responses;
  201 | 			this.responsesSet = true;
  202 | 			return this;
  203 | 		}
  204 | 
  205 | 		public Builder metadata(Map<String, Object> metadata) {
  206 | 			this.metadata = metadata;
  207 | 			return this;
  208 | 		}
  209 | 
  210 | 		public ToolResponseMessage build() {
  211 | 			List<MessagePart> parts = new ArrayList<>(this.explicitParts);
  212 | 			if (this.responsesSet) {
  213 | 				replace(parts, ToolResultPart.class, legacyParts(this.responses), parts.size());
  214 | 			}
  215 | 			return new ToolResponseMessage(this.metadata, parts);
  216 | 		}
  217 | 
  218 | 	}
  219 | 
  220 | }
```
