<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/chat/messages/UserMessage.java -->
<!-- 正文引用：[[源码:UserMessage.java#L起-L止]] ；代码块头部：(src: UserMessage.java#L起-L止) -->

# UserMessage.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/chat/messages/UserMessage.java` |
| 行数 | 231 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:55:27.959Z |

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
   20 | import java.util.Arrays;
   21 | import java.util.Collection;
   22 | import java.util.HashMap;
   23 | import java.util.List;
   24 | import java.util.Map;
   25 | 
   26 | import org.jspecify.annotations.Nullable;
   27 | 
   28 | import org.springframework.ai.chat.messages.part.MediaPart;
   29 | import org.springframework.ai.chat.messages.part.MessagePart;
   30 | import org.springframework.ai.chat.messages.part.TextPart;
   31 | import org.springframework.ai.content.Media;
   32 | import org.springframework.ai.content.MediaContent;
   33 | import org.springframework.core.io.Resource;
   34 | import org.springframework.util.Assert;
   35 | import org.springframework.util.StringUtils;
   36 | 
   37 | /**
   38 |  * A message of the type 'user' passed as input Messages with the user role are from the
   39 |  * end-user or developer. They represent questions, prompts, or any input that you want
   40 |  * the generative to respond to.
   41 |  * <p>
   42 |  * The message content is an ordered list of {@link MessagePart}s, inherited from
   43 |  * {@link AbstractMessage} so that it is shared with {@link AssistantMessage}.
   44 |  * {@link #getMedia()} is a view over that list, selecting the {@link MediaPart}s in part
   45 |  * order.
   46 |  */
   47 | public class UserMessage extends AbstractMessage implements MediaContent {
   48 | 
   49 | 	/**
   50 | 	 * The media of this message, derived from its {@link MediaPart}s.
   51 | 	 * @deprecated since 2.1.0 in favor of {@link #getMedia()}; kept so existing
   52 | 	 * subclasses that read the field keep compiling. Will be removed in a future release.
   53 | 	 */
   54 | 	@Deprecated(since = "2.1.0", forRemoval = true)
   55 | 	protected final List<Media> media;
   56 | 
   57 | 	public UserMessage(@Nullable String textContent) {
   58 | 		this(textContent, new ArrayList<>(), Map.of());
   59 | 	}
   60 | 
   61 | 	public UserMessage(Resource resource) {
   62 | 		this(MessageUtils.readResource(resource));
   63 | 	}
   64 | 
   65 | 	private UserMessage(@Nullable String textContent, Collection<Media> media, Map<String, Object> metadata) {
   66 | 		this(legacyParts(textContent, media), metadata);
   67 | 	}
   68 | 
   69 | 	/**
   70 | 	 * Creates a message from an ordered list of parts. A message without a
   71 | 	 * {@link TextPart} (for example media only) has an empty text.
   72 | 	 * @param parts the parts, in order
   73 | 	 * @param metadata the message metadata
   74 | 	 * @since 2.1.0
   75 | 	 */
   76 | 	protected UserMessage(List<MessagePart> parts, Map<String, Object> metadata) {
   77 | 		super(MessageType.USER, List.copyOf(parts), metadata);
   78 | 		this.media = getParts().stream()
   79 | 			.filter(MediaPart.class::isInstance)
   80 | 			.map(MediaPart.class::cast)
   81 | 			.map(MediaPart::media)
   82 | 			.toList();
   83 | 	}
   84 | 
   85 | 	private static List<MessagePart> legacyParts(@Nullable String textContent, Collection<Media> media) {
   86 | 		Assert.notNull(media, "media cannot be null");
   87 | 		Assert.noNullElements(media, "media cannot have null elements");
   88 | 		List<MessagePart> parts = new ArrayList<>();
   89 | 		if (textContent != null) {
   90 | 			parts.add(TextPart.of(textContent));
   91 | 		}
   92 | 		for (Media medium : media) {
   93 | 			parts.add(MediaPart.of(medium));
   94 | 		}
   95 | 		return parts;
   96 | 	}
   97 | 
   98 | 	@Override
   99 | 	public String toString() {
  100 | 		return "UserMessage{" + "content='" + getText() + '\'' + ", parts=" + getParts() + ", metadata=" + this.metadata
  101 | 				+ ", messageType=" + this.messageType + '}';
  102 | 	}
  103 | 
  104 | 	@Override
  105 | 	public List<Media> getMedia() {
  106 | 		return this.media;
  107 | 	}
  108 | 
  109 | 	public UserMessage copy() {
  110 | 		return mutate().build();
  111 | 	}
  112 | 
  113 | 	/**
  114 | 	 * A builder pre-populated with this message's parts and metadata. Calling
  115 | 	 * {@link Builder#text(String)} or {@link Builder#media(List)} on it replaces the
  116 | 	 * corresponding parts, so rewriting the question keeps the media and vice versa.
  117 | 	 */
  118 | 	public Builder mutate() {
  119 | 		return new Builder().parts(getParts()).metadata(Map.copyOf(getMetadata()));
  120 | 	}
  121 | 
  122 | 	public static Builder builder() {
  123 | 		return new Builder();
  124 | 	}
  125 | 
  126 | 	/**
  127 | 	 * Builder for {@link UserMessage}.
  128 | 	 * <p>
  129 | 	 * Parts added through {@link #part(MessagePart)} and {@link #parts(List)} keep their
  130 | 	 * order. {@link #text(String)} and {@link #text(Resource)} replace the text parts:
  131 | 	 * the new text takes the place of the first text part, or comes first when there is
  132 | 	 * none. {@link #media(List)} replaces the media parts: the new media go where the
  133 | 	 * first media part was, or last when there is none. A builder without explicit parts
  134 | 	 * therefore produces the legacy order, text then media.
  135 | 	 */
  136 | 	public static final class Builder {
  137 | 
  138 | 		private final List<MessagePart> explicitParts = new ArrayList<>();
  139 | 
  140 | 		private @Nullable String textContent;
  141 | 
  142 | 		private boolean textSet;
  143 | 
  144 | 		private @Nullable Resource resource;
  145 | 
  146 | 		private @Nullable List<Media> media;
  147 | 
  148 | 		private Map<String, Object> metadata = new HashMap<>();
  149 | 
  150 | 		/**
  151 | 		 * Adds a part.
  152 | 		 * @param part the part
  153 | 		 * @return this builder
  154 | 		 * @since 2.1.0
  155 | 		 */
  156 | 		public Builder part(MessagePart part) {
  157 | 			Assert.notNull(part, "part cannot be null");
  158 | 			this.explicitParts.add(part);
  159 | 			return this;
  160 | 		}
  161 | 
  162 | 		/**
  163 | 		 * Adds parts, in order.
  164 | 		 * @param parts the parts
  165 | 		 * @return this builder
  166 | 		 * @since 2.1.0
  167 | 		 */
  168 | 		public Builder parts(List<MessagePart> parts) {
  169 | 			Assert.notNull(parts, "parts cannot be null");
  170 | 			Assert.noNullElements(parts, "parts cannot have null elements");
  171 | 			this.explicitParts.addAll(parts);
  172 | 			return this;
  173 | 		}
  174 | 
  175 | 		public Builder text(String textContent) {
  176 | 			this.textContent = textContent;
  177 | 			this.textSet = true;
  178 | 			return this;
  179 | 		}
  180 | 
  181 | 		public Builder text(Resource resource) {
  182 | 			this.resource = resource;
  183 | 			return this;
  184 | 		}
  185 | 
  186 | 		public Builder media(List<Media> media) {
  187 | 			this.media = media;
  188 | 			return this;
  189 | 		}
  190 | 
  191 | 		public Builder media(Media... media) {
  192 | 			this.media = Arrays.asList(media);
  193 | 			return this;
  194 | 		}
  195 | 
  196 | 		public Builder metadata(Map<String, Object> metadata) {
  197 | 			this.metadata = metadata;
  198 | 			return this;
  199 | 		}
  200 | 
  201 | 		public UserMessage build() {
  202 | 			if (StringUtils.hasText(this.textContent) && this.resource != null) {
  203 | 				throw new IllegalArgumentException("textContent and resource cannot be set at the same time");
  204 | 			}
  205 | 			String text = this.textContent;
  206 | 			if (this.resource != null) {
  207 | 				text = MessageUtils.readResource(this.resource);
  208 | 			}
  209 | 			if (this.explicitParts.isEmpty()) {
  210 | 				// Legacy path: text then media; a null text fails as it always has
  211 | 				return new UserMessage(text, this.media != null ? this.media : List.of(), this.metadata);
  212 | 			}
  213 | 			List<MessagePart> parts = new ArrayList<>(this.explicitParts);
  214 | 			if (this.textSet || this.resource != null) {
  215 | 				Assert.notNull(text, "Content must not be null for SYSTEM or USER messages");
  216 | 				replace(parts, TextPart.class, List.of(TextPart.of(text)), 0);
  217 | 			}
  218 | 			if (this.media != null) {
  219 | 				Assert.noNullElements(this.media, "media cannot have null elements");
  220 | 				List<MessagePart> mediaParts = this.media.stream()
  221 | 					.map(MediaPart::of)
  222 | 					.map(MessagePart.class::cast)
  223 | 					.toList();
  224 | 				replace(parts, MediaPart.class, mediaParts, parts.size());
  225 | 			}
  226 | 			return new UserMessage(parts, this.metadata);
  227 | 		}
  228 | 
  229 | 	}
  230 | 
  231 | }
```
