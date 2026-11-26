<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/messages/AssistantMessage.java -->
<!-- 正文引用：[[源码:AssistantMessage.java#L起-L止]] ；代码块头部：(src: AssistantMessage.java#L起-L止) -->

# AssistantMessage.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/messages/AssistantMessage.java` |
| 行数 | 362 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:05.182Z |

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
   22 | import java.util.stream.Stream;
   23 | 
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.ai.chat.messages.part.MediaPart;
   27 | import org.springframework.ai.chat.messages.part.MessagePart;
   28 | import org.springframework.ai.chat.messages.part.ReasoningPart;
   29 | import org.springframework.ai.chat.messages.part.StreamingParts;
   30 | import org.springframework.ai.chat.messages.part.TextPart;
   31 | import org.springframework.ai.chat.messages.part.ToolCallPart;
   32 | import org.springframework.ai.content.Media;
   33 | import org.springframework.ai.content.MediaContent;
   34 | import org.springframework.util.Assert;
   35 | 
   36 | /**
   37 |  * Lets the generative know the content was generated as a response to the user. This role
   38 |  * indicates messages that the generative has previously generated in the conversation. By
   39 |  * including assistant messages in the series, you provide context to the generative about
   40 |  * prior exchanges in the conversation.
   41 |  * <p>
   42 |  * The message content is an ordered list of {@link MessagePart}s, inherited from
   43 |  * {@link AbstractMessage} so that {@link UserMessage} can carry the same kind of content.
   44 |  * The text, tool calls, media and reasoning accessors are views over that list:
   45 |  * {@link #getText()} joins the {@link TextPart}s, {@link #getToolCalls()} selects the
   46 |  * {@link ToolCallPart}s except streamed tool-call argument deltas, {@link #getMedia()}
   47 |  * selects the {@link MediaPart}s and {@link #getReasoning()} selects the
   48 |  * {@link ReasoningPart}s, in part order. Because reasoning is a part like any other, a
   49 |  * message preserves the exact interleaving some providers stream between reasoning, tool
   50 |  * calls, text and media (for example a reasoning block immediately followed by the tool
   51 |  * call it justified). Messages built through the legacy constructor or builder setters
   52 |  * get their parts in the legacy order: text, tool calls, media.
   53 |  *
   54 |  * @author Mark Pollack
   55 |  * @author Christian Tzolov
   56 |  * @author Thomas Vitale
   57 |  * @author guan xu
   58 |  * @since 1.0.0
   59 |  */
   60 | public class AssistantMessage extends AbstractMessage implements MediaContent {
   61 | 
   62 | 	/**
   63 | 	 * The media of this message, derived from its {@link MediaPart}s.
   64 | 	 * @deprecated since 2.1.0 in favor of {@link #getMedia()}; kept so existing
   65 | 	 * subclasses that read the field keep compiling. Will be removed in a future release.
   66 | 	 */
   67 | 	@Deprecated(since = "2.1.0", forRemoval = true)
   68 | 	protected final List<Media> media;
   69 | 
   70 | 	public AssistantMessage(@Nullable String content) {
   71 | 		this(content, Map.of(), List.of(), List.of());
   72 | 	}
   73 | 
   74 | 	protected AssistantMessage(@Nullable String content, Map<String, Object> properties, List<ToolCall> toolCalls,
   75 | 			List<Media> media) {
   76 | 		this(legacyParts(content, toolCalls, media), properties);
   77 | 	}
   78 | 
   79 | 	/**
   80 | 	 * Creates a message from an ordered list of content parts, which may include
   81 | 	 * {@link ReasoningPart}s interleaved with the rest of the content.
   82 | 	 * @param parts the parts, in order
   83 | 	 * @param properties the message metadata
   84 | 	 * @since 2.1.0
   85 | 	 */
   86 | 	protected AssistantMessage(List<MessagePart> parts, Map<String, Object> properties) {
   87 | 		super(MessageType.ASSISTANT, parts, properties);
   88 | 		this.media = select(MediaPart.class).map(MediaPart::media).toList();
   89 | 	}
   90 | 
   91 | 	private static List<MessagePart> legacyParts(@Nullable String content, List<ToolCall> toolCalls,
   92 | 			List<Media> media) {
   93 | 		Assert.notNull(toolCalls, "Tool calls must not be null");
   94 | 		Assert.notNull(media, "Media must not be null");
   95 | 		List<MessagePart> parts = new ArrayList<>();
   96 | 		if (content != null) {
   97 | 			parts.add(TextPart.of(content));
   98 | 		}
   99 | 		for (ToolCall toolCall : toolCalls) {
  100 | 			parts.add(ToolCallPart.of(toolCall));
  101 | 		}
  102 | 		for (Media medium : media) {
  103 | 			parts.add(MediaPart.of(medium));
  104 | 		}
  105 | 		return parts;
  106 | 	}
  107 | 
  108 | 	/**
  109 | 	 * The reasoning parts of this message, in part order.
  110 | 	 * @return an unmodifiable list of reasoning parts
  111 | 	 * @since 2.1.0
  112 | 	 */
  113 | 	public List<ReasoningPart> getReasoning() {
  114 | 		return select(ReasoningPart.class).toList();
  115 | 	}
  116 | 
  117 | 	/**
  118 | 	 * The tool calls of this message, in part order. A streamed tool-call argument delta,
  119 | 	 * a {@link ToolCallPart} marked {@link StreamingParts#isPartial(MessagePart)
  120 | 	 * partial}, is not a tool call and is skipped.
  121 | 	 * @return an unmodifiable list of tool calls
  122 | 	 */
  123 | 	public List<ToolCall> getToolCalls() {
  124 | 		return completeToolCallParts().map(ToolCallPart::toolCall).toList();
  125 | 	}
  126 | 
  127 | 	/**
  128 | 	 * Whether this message holds a tool call. A streamed tool-call argument delta does
  129 | 	 * not count, see {@link #getToolCalls()}.
  130 | 	 * @return {@code true} if the message holds at least one tool call
  131 | 	 */
  132 | 	public boolean hasToolCalls() {
  133 | 		return completeToolCallParts().findAny().isPresent();
  134 | 	}
  135 | 
  136 | 	private Stream<ToolCallPart> completeToolCallParts() {
  137 | 		return select(ToolCallPart.class).filter(part -> !StreamingParts.isPartial(part));
  138 | 	}
  139 | 
  140 | 	@Override
  141 | 	public List<Media> getMedia() {
  142 | 		return this.media;
  143 | 	}
  144 | 
  145 | 	public AssistantMessage copy() {
  146 | 		return mutate().build();
  147 | 	}
  148 | 
  149 | 	/**
  150 | 	 * A builder pre-populated with this message's parts and metadata. Calling
  151 | 	 * {@link Builder#content(String)}, {@link Builder#toolCalls(List)} or
  152 | 	 * {@link Builder#media(List)} on it replaces the corresponding parts in place, so
  153 | 	 * rewriting the text keeps the reasoning and tool calls where they were and vice
  154 | 	 * versa.
  155 | 	 * @return the builder
  156 | 	 */
  157 | 	public Builder<?> mutate() {
  158 | 		return builder().parts(getParts()).properties(getMetadata());
  159 | 	}
  160 | 
  161 | 	@Override
  162 | 	public String toString() {
  163 | 		return "AssistantMessage [messageType=" + this.messageType + ", parts=" + getParts() + ", textContent="
  164 | 				+ getText() + ", metadata=" + this.metadata + "]";
  165 | 	}
  166 | 
  167 | 	public static Builder<?> builder() {
  168 | 		return new Builder<>();
  169 | 	}
  170 | 
  171 | 	public record ToolCall(String id, String type, String name, String arguments) {
  172 | 
  173 | 	}
  174 | 
  175 | 	/**
  176 | 	 * Builder for {@link AssistantMessage}.
  177 | 	 * <p>
  178 | 	 * Parts added through {@link #part(MessagePart)}, {@link #parts(List)} and
  179 | 	 * {@link #reasoning(ReasoningPart)} keep the order they were added in. The legacy
  180 | 	 * setters replace the parts of their type at {@link #build()} time:
  181 | 	 * {@link #content(String)} replaces the text parts, {@link #toolCalls(List)} the tool
  182 | 	 * call parts and {@link #media(List)} the media parts. The new parts take the place
  183 | 	 * of the first part of that type, or go where the legacy order has them when there is
  184 | 	 * none (text before tool calls and media, tool calls before media), and a
  185 | 	 * {@code null} content or an empty list removes the parts of that type. The setters
  186 | 	 * take effect at {@link #build()} time, so they also replace parts of their type
  187 | 	 * added after the setter was called. A builder without explicit parts therefore
  188 | 	 * produces the legacy order (text, tool calls, media) whatever the setter call order.
  189 | 	 *
  190 | 	 * @param <B> the concrete builder type, for subclass builders
  191 | 	 */
  192 | 	public static class Builder<B extends Builder<B>> {
  193 | 
  194 | 		protected final List<MessagePart> explicitParts = new ArrayList<>();
  195 | 
  196 | 		/**
  197 | 		 * The text set through {@link #content(String)}. A subclass builder must set it
  198 | 		 * through that method: on a builder that also holds parts, a value assigned to
  199 | 		 * the field directly is ignored.
  200 | 		 */
  201 | 		protected @Nullable String content;
  202 | 
  203 | 		protected Map<String, Object> properties = Map.of();
  204 | 
  205 | 		/**
  206 | 		 * The tool calls set through {@link #toolCalls(List)}. As for {@link #content}, a
  207 | 		 * subclass builder must set them through that method.
  208 | 		 */
  209 | 		protected List<ToolCall> toolCalls = List.of();
  210 | 
  211 | 		/**
  212 | 		 * The media set through {@link #media(List)}. As for {@link #content}, a subclass
  213 | 		 * builder must set them through that method.
  214 | 		 */
  215 | 		protected List<Media> media = List.of();
  216 | 
  217 | 		private boolean contentSet;
  218 | 
  219 | 		private boolean toolCallsSet;
  220 | 
  221 | 		private boolean mediaSet;
  222 | 
  223 | 		protected Builder() {
  224 | 		}
  225 | 
  226 | 		@SuppressWarnings("unchecked")
  227 | 		protected B self() {
  228 | 			return (B) this;
  229 | 		}
  230 | 
  231 | 		/**
  232 | 		 * Appends a content part.
  233 | 		 * @param part the part
  234 | 		 * @return this builder
  235 | 		 * @since 2.1.0
  236 | 		 */
  237 | 		public B part(MessagePart part) {
  238 | 			Assert.notNull(part, "Content part must not be null");
  239 | 			if (!supportsParts()) {
  240 | 				throw new UnsupportedOperationException(getClass().getName()
  241 | 						+ " overrides build() through the legacy constructor and would drop explicit parts;"
  242 | 						+ " override supportsParts() to return true and build from buildParts()");
  243 | 			}
  244 | 			this.explicitParts.add(part);
  245 | 			return self();
  246 | 		}
  247 | 
  248 | 		/**
  249 | 		 * Whether {@link #build()} of this builder honors {@link #part(MessagePart)},
  250 | 		 * {@link #parts(List)} and {@link #reasoning(ReasoningPart)}. The base builder
  251 | 		 * does. A subclass builder that overrides {@link #build()} must override this
  252 | 		 * method to return {@code true} and construct its message from
  253 | 		 * {@link #buildParts()}; otherwise {@link #part(MessagePart)} throws so parts are
  254 | 		 * never dropped silently.
  255 | 		 * @return whether explicit parts are supported
  256 | 		 * @since 2.1.0
  257 | 		 */
  258 | 		protected boolean supportsParts() {
  259 | 			return getClass() == Builder.class;
  260 | 		}
  261 | 
  262 | 		/**
  263 | 		 * The parts this builder would put in the message: the explicit parts, with the
  264 | 		 * text, tool call and media parts replaced by the corresponding legacy setters
  265 | 		 * that were called. A setter whose part type is not among the explicit parts puts
  266 | 		 * its parts where the legacy order has them: the text before the first tool call
  267 | 		 * or media part, the tool calls before the first media part, the media at the
  268 | 		 * end.
  269 | 		 * @return the parts, in message order
  270 | 		 * @since 2.1.0
  271 | 		 */
  272 | 		protected List<MessagePart> buildParts() {
  273 | 			if (this.explicitParts.isEmpty()) {
  274 | 				return legacyParts(this.content, this.toolCalls, this.media);
  275 | 			}
  276 | 			List<MessagePart> parts = new ArrayList<>(this.explicitParts);
  277 | 			if (this.contentSet) {
  278 | 				List<TextPart> textParts = (this.content != null) ? List.of(TextPart.of(this.content)) : List.of();
  279 | 				replace(parts, TextPart.class, textParts, firstIndexOf(parts, ToolCallPart.class, MediaPart.class));
  280 | 			}
  281 | 			if (this.toolCallsSet) {
  282 | 				Assert.notNull(this.toolCalls, "Tool calls must not be null");
  283 | 				List<ToolCallPart> toolCallParts = this.toolCalls.stream().map(ToolCallPart::of).toList();
  284 | 				replace(parts, ToolCallPart.class, toolCallParts, firstIndexOf(parts, MediaPart.class));
  285 | 			}
  286 | 			if (this.mediaSet) {
  287 | 				Assert.notNull(this.media, "Media must not be null");
  288 | 				List<MediaPart> mediaParts = this.media.stream().map(MediaPart::of).toList();
  289 | 				replace(parts, MediaPart.class, mediaParts, parts.size());
  290 | 			}
  291 | 			return parts;
  292 | 		}
  293 | 
  294 | 		/**
  295 | 		 * Appends content parts in order.
  296 | 		 * @param parts the parts
  297 | 		 * @return this builder
  298 | 		 * @since 2.1.0
  299 | 		 */
  300 | 		public B parts(List<MessagePart> parts) {
  301 | 			Assert.notNull(parts, "Content parts must not be null");
  302 | 			for (MessagePart part : parts) {
  303 | 				part(part);
  304 | 			}
  305 | 			return self();
  306 | 		}
  307 | 
  308 | 		/**
  309 | 		 * Appends a reasoning part. Equivalent to {@link #part(MessagePart)}, since
  310 | 		 * {@link ReasoningPart} is a {@link MessagePart}.
  311 | 		 * @param reasoning the reasoning part
  312 | 		 * @return this builder
  313 | 		 * @since 2.1.0
  314 | 		 */
  315 | 		public B reasoning(ReasoningPart reasoning) {
  316 | 			return part(reasoning);
  317 | 		}
  318 | 
  319 | 		/**
  320 | 		 * Appends reasoning parts in order.
  321 | 		 * @param reasoning the reasoning parts
  322 | 		 * @return this builder
  323 | 		 * @since 2.1.0
  324 | 		 */
  325 | 		public B reasoning(List<ReasoningPart> reasoning) {
  326 | 			Assert.notNull(reasoning, "Reasoning must not be null");
  327 | 			for (ReasoningPart part : reasoning) {
  328 | 				reasoning(part);
  329 | 			}
  330 | 			return self();
  331 | 		}
  332 | 
  333 | 		public B content(@Nullable String content) {
  334 | 			this.content = content;
  335 | 			this.contentSet = true;
  336 | 			return self();
  337 | 		}
  338 | 
  339 | 		public B properties(Map<String, Object> properties) {
  340 | 			this.properties = properties;
  341 | 			return self();
  342 | 		}
  343 | 
  344 | 		public B toolCalls(List<ToolCall> toolCalls) {
  345 | 			this.toolCalls = toolCalls;
  346 | 			this.toolCallsSet = true;
  347 | 			return self();
  348 | 		}
  349 | 
  350 | 		public B media(List<Media> media) {
  351 | 			this.media = media;
  352 | 			this.mediaSet = true;
  353 | 			return self();
  354 | 		}
  355 | 
  356 | 		public AssistantMessage build() {
  357 | 			return new AssistantMessage(buildParts(), this.properties);
  358 | 		}
  359 | 
  360 | 	}
  361 | 
  362 | }
```
