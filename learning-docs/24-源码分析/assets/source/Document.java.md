<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-commons:2.1.0 | path: org/springframework/ai/document/Document.java -->
<!-- 正文引用：[[源码:Document.java#L起-L止]] ；代码块头部：(src: Document.java#L起-L止) -->

# Document.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-commons:2.1.0` |
| 路径 | `org/springframework/ai/document/Document.java` |
| 行数 | 536 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-commons/2.1.0/spring-ai-commons-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:47.383Z |

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
   17 | package org.springframework.ai.document;
   18 | 
   19 | import java.util.HashMap;
   20 | import java.util.Map;
   21 | import java.util.Objects;
   22 | 
   23 | import com.fasterxml.jackson.annotation.JsonAlias;
   24 | import com.fasterxml.jackson.annotation.JsonIgnore;
   25 | import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
   26 | import org.jspecify.annotations.Nullable;
   27 | import tools.jackson.databind.annotation.JsonDeserialize;
   28 | import tools.jackson.databind.annotation.JsonPOJOBuilder;
   29 | 
   30 | import org.springframework.ai.content.Media;
   31 | import org.springframework.ai.document.id.IdGenerator;
   32 | import org.springframework.ai.document.id.RandomIdGenerator;
   33 | import org.springframework.util.Assert;
   34 | import org.springframework.util.StringUtils;
   35 | 
   36 | /**
   37 |  * A document is a container for the content and metadata of a document. It also contains
   38 |  * the document's unique ID.
   39 |  *
   40 |  * <p>
   41 |  * A {@code Document} holds either text content or media content, but not both. Text
   42 |  * content is accessed via {@link #getText()}, and media content via {@link #getMedia()}.
   43 |  * Use {@link #isText()} to determine which type is present.
   44 |  *
   45 |  * <p>
   46 |  * Documents are intended to carry data through Spring AI's ETL pipeline — from ingestion
   47 |  * through transformation to storage in a vector store.
   48 |  *
   49 |  * <h2>JSON serialization and deserialization</h2>
   50 |  * <p>
   51 |  * {@code Document} is fully Jackson-serializable. The serialized JSON uses the following
   52 |  * field names:
   53 |  * <ul>
   54 |  * <li>{@code id} — the unique document identifier
   55 |  * <li>{@code text} — the text content (null for media documents)
   56 |  * <li>{@code media} — the media content (null for text documents)
   57 |  * <li>{@code metadata} — the metadata map
   58 |  * <li>{@code score} — the relevance score, may be null
   59 |  * </ul>
   60 |  *
   61 |  * <p>
   62 |  * Deserialization is handled via {@link Builder}. JSON round-trips are fully supported: a
   63 |  * serialized {@code Document} can be deserialized back to an equal instance.
   64 |  *
   65 |  * <p>
   66 |  * Example round-trip: <pre>{@code
   67 |  * ObjectMapper mapper = JacksonUtils.getDefaultJsonMapper();
   68 |  * Document original = Document.builder()
   69 |  *     .id("doc-1")
   70 |  *     .text("hello world")
   71 |  *     .metadata("source", "example")
   72 |  *     .score(0.95)
   73 |  *     .build();
   74 |  *
   75 |  * String json = mapper.writeValueAsString(original);
   76 |  * // {"id":"doc-1","text":"hello world","media":null,"metadata":{"source":"example"},"score":0.95}
   77 |  *
   78 |  * Document restored = mapper.readValue(json, Document.class);
   79 |  * // restored.equals(original) == true
   80 |  * }</pre>
   81 |  *
   82 |  * <h2>Creating documents</h2>
   83 |  * <p>
   84 |  * Example of creating a text document: <pre>{@code
   85 |  * // Using constructor
   86 |  * Document textDoc = new Document("Sample text content", Map.of("source", "user-input"));
   87 |  *
   88 |  * // Using builder (preferred)
   89 |  * Document textDoc = Document.builder()
   90 |  *     .text("Sample text content")
   91 |  *     .metadata("source", "user-input")
   92 |  *     .build();
   93 |  * }</pre>
   94 |  *
   95 |  * <p>
   96 |  * Example of creating a media document: <pre>{@code
   97 |  * // Using constructor
   98 |  * Media imageContent = new Media(MediaType.IMAGE_PNG, new byte[] {...});
   99 |  * Document mediaDoc = new Document(imageContent, Map.of("filename", "sample.png"));
  100 |  *
  101 |  * // Using builder (preferred)
  102 |  * Document mediaDoc = Document.builder()
  103 |  *     .media(new Media(MediaType.IMAGE_PNG, new byte[] {...}))
  104 |  *     .metadata("filename", "sample.png")
  105 |  *     .build();
  106 |  * }</pre>
  107 |  *
  108 |  * <p>
  109 |  * Example of checking content type and accessing content: <pre>{@code
  110 |  * if (document.isText()) {
  111 |  *     String text = document.getText();
  112 |  *     // Process text content
  113 |  * } else {
  114 |  *     Media media = document.getMedia();
  115 |  *     // Process media content
  116 |  * }
  117 |  * }</pre>
  118 |  */
  119 | @JsonDeserialize(builder = Document.Builder.class)
  120 | public class Document {
  121 | 
  122 | 	public static final ContentFormatter DEFAULT_CONTENT_FORMATTER = DefaultContentFormatter.defaultConfig();
  123 | 
  124 | 	/**
  125 | 	 * Unique ID
  126 | 	 */
  127 | 	private final String id;
  128 | 
  129 | 	/**
  130 | 	 * Document string content.
  131 | 	 */
  132 | 	private final @Nullable String text;
  133 | 
  134 | 	/**
  135 | 	 * Document media content
  136 | 	 */
  137 | 	private final @Nullable Media media;
  138 | 
  139 | 	/**
  140 | 	 * Metadata for the document. It should not be nested and values should be restricted
  141 | 	 * to string, int, float, boolean for simple use with Vector Dbs.
  142 | 	 */
  143 | 	private final Map<String, Object> metadata;
  144 | 
  145 | 	/**
  146 | 	 * A numeric score associated with this document that can represent various types of
  147 | 	 * relevance measures.
  148 | 	 * <p>
  149 | 	 * Common uses include:
  150 | 	 * <ul>
  151 | 	 * <li>Measure of similarity between the embedding value of the document's text/media
  152 | 	 * and a query vector, where higher scores indicate greater similarity (opposite of
  153 | 	 * distance measure)
  154 | 	 * <li>Text relevancy rankings from retrieval systems
  155 | 	 * <li>Custom relevancy metrics from RAG patterns
  156 | 	 * </ul>
  157 | 	 * <p>
  158 | 	 * Higher values typically indicate greater relevance or similarity.
  159 | 	 */
  160 | 	private final @Nullable Double score;
  161 | 
  162 | 	/**
  163 | 	 * Mutable, ephemeral, content to text formatter. Defaults to Document text.
  164 | 	 */
  165 | 	@JsonIgnore
  166 | 	private ContentFormatter contentFormatter = DEFAULT_CONTENT_FORMATTER;
  167 | 
  168 | 	/**
  169 | 	 * Creates a text document with a generated ID and empty metadata.
  170 | 	 * @param text the text content of the document
  171 | 	 */
  172 | 	public Document(@Nullable String text) {
  173 | 		this(text, new HashMap<>());
  174 | 	}
  175 | 
  176 | 	/**
  177 | 	 * Creates a text document with a generated ID and the given metadata.
  178 | 	 * @param text the text content of the document
  179 | 	 * @param metadata the metadata map; must not be null and must not contain null keys
  180 | 	 * or values
  181 | 	 */
  182 | 	public Document(@Nullable String text, Map<String, Object> metadata) {
  183 | 		this(new RandomIdGenerator().generateId(), text, null, metadata, null);
  184 | 	}
  185 | 
  186 | 	/**
  187 | 	 * Creates a text document with an explicit ID and the given metadata.
  188 | 	 * @param id the unique document identifier; must not be null or empty
  189 | 	 * @param text the text content of the document
  190 | 	 * @param metadata the metadata map; must not be null and must not contain null keys
  191 | 	 * or values
  192 | 	 */
  193 | 	public Document(String id, @Nullable String text, Map<String, Object> metadata) {
  194 | 		this(id, text, null, metadata, null);
  195 | 	}
  196 | 
  197 | 	/**
  198 | 	 * Creates a media document with a generated ID and the given metadata.
  199 | 	 * @param media the media content of the document
  200 | 	 * @param metadata the metadata map; must not be null and must not contain null keys
  201 | 	 * or values
  202 | 	 */
  203 | 	public Document(@Nullable Media media, Map<String, Object> metadata) {
  204 | 		this(new RandomIdGenerator().generateId(), null, media, metadata, null);
  205 | 	}
  206 | 
  207 | 	/**
  208 | 	 * Creates a media document with an explicit ID and the given metadata.
  209 | 	 * @param id the unique document identifier; must not be null or empty
  210 | 	 * @param media the media content of the document
  211 | 	 * @param metadata the metadata map; must not be null and must not contain null keys
  212 | 	 * or values
  213 | 	 */
  214 | 	public Document(String id, @Nullable Media media, Map<String, Object> metadata) {
  215 | 		this(id, null, media, metadata, null);
  216 | 	}
  217 | 
  218 | 	private Document(String id, @Nullable String text, @Nullable Media media, Map<String, Object> metadata,
  219 | 			@Nullable Double score) {
  220 | 		Assert.hasText(id, "id cannot be null or empty");
  221 | 		Assert.notNull(metadata, "metadata cannot be null");
  222 | 		Assert.noNullElements(metadata.keySet(), "metadata cannot have null keys");
  223 | 		Assert.noNullElements(metadata.values(), "metadata cannot have null values");
  224 | 		Assert.isTrue(text != null ^ media != null, "exactly one of text or media must be specified");
  225 | 
  226 | 		this.id = id;
  227 | 		this.text = text;
  228 | 		this.media = media;
  229 | 		this.metadata = new HashMap<>(metadata);
  230 | 		this.score = score;
  231 | 	}
  232 | 
  233 | 	public static Builder builder() {
  234 | 		return new Builder();
  235 | 	}
  236 | 
  237 | 	/**
  238 | 	 * Returns the unique identifier for this document.
  239 | 	 * <p>
  240 | 	 * This ID is either explicitly provided during document creation or generated using
  241 | 	 * the configured {@link IdGenerator} (defaults to {@link RandomIdGenerator}).
  242 | 	 * @return the unique identifier of this document
  243 | 	 * @see RandomIdGenerator
  244 | 	 */
  245 | 	public String getId() {
  246 | 		return this.id;
  247 | 	}
  248 | 
  249 | 	/**
  250 | 	 * Returns the document's text content, if any.
  251 | 	 * @return the text content if {@link #isText()} is true, null otherwise
  252 | 	 * @see #isText()
  253 | 	 * @see #getMedia()
  254 | 	 */
  255 | 	public @Nullable String getText() {
  256 | 		return this.text;
  257 | 	}
  258 | 
  259 | 	/**
  260 | 	 * Determines whether this document contains text or media content.
  261 | 	 * @return true if this document contains text content (accessible via
  262 | 	 * {@link #getText()}), false if it contains media content (accessible via
  263 | 	 * {@link #getMedia()})
  264 | 	 */
  265 | 	public boolean isText() {
  266 | 		return this.text != null;
  267 | 	}
  268 | 
  269 | 	/**
  270 | 	 * Returns the document's media content, if any.
  271 | 	 * @return the media content if {@link #isText()} is false, null otherwise
  272 | 	 * @see #isText()
  273 | 	 * @see #getText()
  274 | 	 */
  275 | 	public @Nullable Media getMedia() {
  276 | 		return this.media;
  277 | 	}
  278 | 
  279 | 	/**
  280 | 	 * Returns the formatted content of this document using {@link MetadataMode#ALL},
  281 | 	 * including both text and all metadata entries.
  282 | 	 * <p>
  283 | 	 * This method is excluded from JSON serialization as it is a computed, ephemeral
  284 | 	 * representation.
  285 | 	 * @return the formatted content string
  286 | 	 * @see #getFormattedContent(MetadataMode)
  287 | 	 */
  288 | 	@JsonIgnore
  289 | 	public String getFormattedContent() {
  290 | 		return this.getFormattedContent(MetadataMode.ALL);
  291 | 	}
  292 | 
  293 | 	/**
  294 | 	 * Returns the formatted content of this document using the given
  295 | 	 * {@link MetadataMode}.
  296 | 	 * @param metadataMode controls which metadata entries are included in the output;
  297 | 	 * must not be null
  298 | 	 * @return the formatted content string
  299 | 	 */
  300 | 	public String getFormattedContent(MetadataMode metadataMode) {
  301 | 		Assert.notNull(metadataMode, "Metadata mode must not be null");
  302 | 		return this.contentFormatter.format(this, metadataMode);
  303 | 	}
  304 | 
  305 | 	/**
  306 | 	 * Helper content extractor that uses and external {@link ContentFormatter}.
  307 | 	 */
  308 | 	public String getFormattedContent(ContentFormatter formatter, MetadataMode metadataMode) {
  309 | 		Assert.notNull(formatter, "formatter must not be null");
  310 | 		Assert.notNull(metadataMode, "Metadata mode must not be null");
  311 | 		return formatter.format(this, metadataMode);
  312 | 	}
  313 | 
  314 | 	/**
  315 | 	 * Returns the metadata associated with this document.
  316 | 	 * <p>
  317 | 	 * The metadata values are restricted to simple types (string, int, float, boolean)
  318 | 	 * for compatibility with Vector Databases.
  319 | 	 * @return the metadata map
  320 | 	 */
  321 | 	public Map<String, Object> getMetadata() {
  322 | 		return this.metadata;
  323 | 	}
  324 | 
  325 | 	/**
  326 | 	 * Returns the relevance score associated with this document, if any.
  327 | 	 * <p>
  328 | 	 * The score is typically assigned during retrieval and is not set at document
  329 | 	 * creation time. Use {@link Builder#score(Double)} or {@link #mutate()} to produce a
  330 | 	 * new document with a score.
  331 | 	 * @return the relevance score, or null if none has been assigned
  332 | 	 */
  333 | 	public @Nullable Double getScore() {
  334 | 		return this.score;
  335 | 	}
  336 | 
  337 | 	/**
  338 | 	 * Returns the content formatter associated with this document.
  339 | 	 * @return the current ContentFormatter instance used for formatting the document
  340 | 	 * content.
  341 | 	 */
  342 | 	public ContentFormatter getContentFormatter() {
  343 | 		return this.contentFormatter;
  344 | 	}
  345 | 
  346 | 	/**
  347 | 	 * Replace the document's {@link ContentFormatter}.
  348 | 	 * @param contentFormatter new formatter to use.
  349 | 	 */
  350 | 	public void setContentFormatter(ContentFormatter contentFormatter) {
  351 | 		this.contentFormatter = contentFormatter;
  352 | 	}
  353 | 
  354 | 	/**
  355 | 	 * Returns a new {@link Builder} pre-populated with all fields of this document,
  356 | 	 * allowing selective modification without altering the original.
  357 | 	 * @return a builder initialised from this document's state
  358 | 	 */
  359 | 	public Builder mutate() {
  360 | 		return new Builder().id(this.id).text(this.text).media(this.media).metadata(this.metadata).score(this.score);
  361 | 	}
  362 | 
  363 | 	@Override
  364 | 	public boolean equals(@Nullable Object o) {
  365 | 		if (o == null || this.getClass() != o.getClass()) {
  366 | 			return false;
  367 | 		}
  368 | 		Document document = (Document) o;
  369 | 		return Objects.equals(this.id, document.id) && Objects.equals(this.text, document.text)
  370 | 				&& Objects.equals(this.media, document.media) && Objects.equals(this.metadata, document.metadata)
  371 | 				&& Objects.equals(this.score, document.score);
  372 | 	}
  373 | 
  374 | 	@Override
  375 | 	public int hashCode() {
  376 | 		return Objects.hash(this.id, this.text, this.media, this.metadata, this.score);
  377 | 	}
  378 | 
  379 | 	@Override
  380 | 	public String toString() {
  381 | 		return "Document{" + "id='" + this.id + '\'' + ", text='" + this.text + '\'' + ", media='" + this.media + '\''
  382 | 				+ ", metadata=" + this.metadata + ", score=" + this.score + '}';
  383 | 	}
  384 | 
  385 | 	/**
  386 | 	 * Builder for {@link Document}.
  387 | 	 *
  388 | 	 * <p>
  389 | 	 * Exactly one of {@link #text(String)} or {@link #media(Media)} must be set before
  390 | 	 * calling {@link #build()}. If no {@link #id(String)} is provided, one is generated
  391 | 	 * by the configured {@link IdGenerator}.
  392 | 	 *
  393 | 	 * <h3>JSON deserialization</h3>
  394 | 	 * <p>
  395 | 	 * This builder is used by Jackson to deserialize {@link Document} instances
  396 | 	 * (configured via {@code @JsonDeserialize(builder = Builder.class)}). Jackson maps
  397 | 	 * JSON field names to builder methods by name: {@code "id"} → {@link #id},
  398 | 	 * {@code "text"} → {@link #text}, {@code "metadata"} → {@link #metadata(Map)},
  399 | 	 * {@code "score"} → {@link #score}.
  400 | 	 */
  401 | 	@JsonPOJOBuilder(withPrefix = "")
  402 | 	@JsonIgnoreProperties(ignoreUnknown = true)
  403 | 	public static final class Builder {
  404 | 
  405 | 		private @Nullable String id;
  406 | 
  407 | 		private @Nullable String text;
  408 | 
  409 | 		private @Nullable Media media;
  410 | 
  411 | 		private Map<String, Object> metadata = new HashMap<>();
  412 | 
  413 | 		private @Nullable Double score;
  414 | 
  415 | 		private IdGenerator idGenerator = new RandomIdGenerator();
  416 | 
  417 | 		/**
  418 | 		 * Sets the {@link IdGenerator} used to generate the document ID when none is
  419 | 		 * provided explicitly. Defaults to {@link RandomIdGenerator}.
  420 | 		 * @param idGenerator the ID generator to use; must not be null
  421 | 		 * @return the builder instance
  422 | 		 */
  423 | 		public Builder idGenerator(IdGenerator idGenerator) {
  424 | 			Assert.notNull(idGenerator, "idGenerator cannot be null");
  425 | 			this.idGenerator = idGenerator;
  426 | 			return this;
  427 | 		}
  428 | 
  429 | 		/**
  430 | 		 * Sets an explicit ID for the document. If not called, an ID is generated
  431 | 		 * automatically by the configured {@link IdGenerator} at {@link #build()} time.
  432 | 		 * @param id the unique document identifier; must not be null or empty
  433 | 		 * @return the builder instance
  434 | 		 */
  435 | 		public Builder id(String id) {
  436 | 			Assert.hasText(id, "id cannot be null or empty");
  437 | 			this.id = id;
  438 | 			return this;
  439 | 		}
  440 | 
  441 | 		/**
  442 | 		 * Sets the text content of the document.
  443 | 		 * <p>
  444 | 		 * Either text or media content must be set before building the document, but not
  445 | 		 * both.
  446 | 		 * @param text the text content
  447 | 		 * @return the builder instance
  448 | 		 * @see #media(Media)
  449 | 		 */
  450 | 		@JsonAlias("content")
  451 | 		public Builder text(@Nullable String text) {
  452 | 			this.text = text;
  453 | 			return this;
  454 | 		}
  455 | 
  456 | 		/**
  457 | 		 * Sets the media content of the document.
  458 | 		 * <p>
  459 | 		 * Either text or media content must be set before building the document, but not
  460 | 		 * both.
  461 | 		 * @param media the media content
  462 | 		 * @return the builder instance
  463 | 		 * @see #text(String)
  464 | 		 */
  465 | 		public Builder media(@Nullable Media media) {
  466 | 			this.media = media;
  467 | 			return this;
  468 | 		}
  469 | 
  470 | 		/**
  471 | 		 * Replaces the metadata map with the given map.
  472 | 		 * @param metadata the metadata map; must not be null and must not contain null
  473 | 		 * keys or values
  474 | 		 * @return the builder instance
  475 | 		 */
  476 | 		public Builder metadata(Map<String, Object> metadata) {
  477 | 			Assert.notNull(metadata, "metadata cannot be null");
  478 | 			this.metadata = metadata;
  479 | 			return this;
  480 | 		}
  481 | 
  482 | 		/**
  483 | 		 * Adds a single key-value pair to the metadata map.
  484 | 		 * @param key the metadata key; must not be null
  485 | 		 * @param value the metadata value; must not be null
  486 | 		 * @return the builder instance
  487 | 		 */
  488 | 		public Builder metadata(String key, Object value) {
  489 | 			Assert.notNull(key, "metadata key cannot be null");
  490 | 			Assert.notNull(value, "metadata value cannot be null");
  491 | 			this.metadata.put(key, value);
  492 | 			return this;
  493 | 		}
  494 | 
  495 | 		/**
  496 | 		 * Sets a score value for this document.
  497 | 		 * <p>
  498 | 		 * Common uses include:
  499 | 		 * <ul>
  500 | 		 * <li>Measure of similarity between the embedding value of the document's
  501 | 		 * text/media and a query vector, where higher scores indicate greater similarity
  502 | 		 * (opposite of distance measure)
  503 | 		 * <li>Text relevancy rankings from retrieval systems
  504 | 		 * <li>Custom relevancy metrics from RAG patterns
  505 | 		 * </ul>
  506 | 		 * <p>
  507 | 		 * Higher values typically indicate greater relevance or similarity.
  508 | 		 * @param score the document score, may be null
  509 | 		 * @return the builder instance
  510 | 		 */
  511 | 		public Builder score(@Nullable Double score) {
  512 | 			this.score = score;
  513 | 			return this;
  514 | 		}
  515 | 
  516 | 		/**
  517 | 		 * Builds the {@link Document}.
  518 | 		 * <p>
  519 | 		 * If no {@link #id(String)} was set, an ID is generated from the text content and
  520 | 		 * metadata using the configured {@link IdGenerator}. Exactly one of
  521 | 		 * {@link #text(String)} or {@link #media(Media)} must have been set.
  522 | 		 * @return a new {@link Document}
  523 | 		 * @throws IllegalArgumentException if both or neither of text and media are set,
  524 | 		 * or if the metadata contains null keys or values
  525 | 		 */
  526 | 		public Document build() {
  527 | 			if (!StringUtils.hasText(this.id)) {
  528 | 				var text = this.text != null ? this.text : "";
  529 | 				this.id = this.idGenerator.generateId(text, this.metadata);
  530 | 			}
  531 | 			return new Document(this.id, this.text, this.media, this.metadata, this.score);
  532 | 		}
  533 | 
  534 | 	}
  535 | 
  536 | }
```
