<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-commons:2.1.0 | path: org/springframework/ai/transformer/splitter/TokenTextSplitter.java -->
<!-- 正文引用：[[源码:TokenTextSplitter.java#L起-L止]] ；代码块头部：(src: TokenTextSplitter.java#L起-L止) -->

# TokenTextSplitter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-commons:2.1.0` |
| 路径 | `org/springframework/ai/transformer/splitter/TokenTextSplitter.java` |
| 行数 | 372 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-commons/2.1.0/spring-ai-commons-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:47.556Z |

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
   17 | package org.springframework.ai.transformer.splitter;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | 
   22 | import com.knuddels.jtokkit.Encodings;
   23 | import com.knuddels.jtokkit.api.Encoding;
   24 | import com.knuddels.jtokkit.api.EncodingRegistry;
   25 | import com.knuddels.jtokkit.api.EncodingType;
   26 | import com.knuddels.jtokkit.api.IntArrayList;
   27 | 
   28 | import org.springframework.util.Assert;
   29 | 
   30 | /**
   31 |  * A {@link TextSplitter} that splits text into chunks of a target size in tokens.
   32 |  *
   33 |  * @author Raphael Yu
   34 |  * @author Christian Tzolov
   35 |  * @author Ricken Bazolo
   36 |  * @author Seunghwan Jung
   37 |  * @author Jemin Huh
   38 |  */
   39 | public class TokenTextSplitter extends TextSplitter {
   40 | 
   41 | 	private static final int DEFAULT_CHUNK_SIZE = 800;
   42 | 
   43 | 	private static final int DEFAULT_CHUNK_OVERLAP = 0;
   44 | 
   45 | 	private static final int MIN_CHUNK_SIZE_CHARS = 350;
   46 | 
   47 | 	private static final int MIN_CHUNK_LENGTH_TO_EMBED = 5;
   48 | 
   49 | 	private static final int MAX_NUM_CHUNKS = 10000;
   50 | 
   51 | 	private static final boolean KEEP_SEPARATOR = true;
   52 | 
   53 | 	private static final List<Character> DEFAULT_PUNCTUATION_MARKS = List.of('.', '?', '!', '\n');
   54 | 
   55 | 	private static final EncodingType DEFAULT_ENCODING_TYPE = EncodingType.CL100K_BASE;
   56 | 
   57 | 	private final EncodingRegistry registry = Encodings.newLazyEncodingRegistry();
   58 | 
   59 | 	private final Encoding encoding;
   60 | 
   61 | 	// The target size of each text chunk in tokens
   62 | 	private final int chunkSize;
   63 | 
   64 | 	// The overlap size of each text chunk in tokens
   65 | 	private final int chunkOverlap;
   66 | 
   67 | 	// The minimum size of each text chunk in characters
   68 | 	private final int minChunkSizeChars;
   69 | 
   70 | 	// Discard chunks shorter than this
   71 | 	private final int minChunkLengthToEmbed;
   72 | 
   73 | 	// The maximum number of chunks to generate from a text
   74 | 	private final int maxNumChunks;
   75 | 
   76 | 	private final boolean keepSeparator;
   77 | 
   78 | 	private final List<Character> punctuationMarks;
   79 | 
   80 | 	/**
   81 | 	 * @deprecated since 2.0.0-M3, use {@link #builder()} instead.
   82 | 	 */
   83 | 	@Deprecated(since = "2.0.0-M3", forRemoval = true)
   84 | 	@SuppressWarnings("deprecation")
   85 | 	public TokenTextSplitter() {
   86 | 		this(DEFAULT_ENCODING_TYPE, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP, MIN_CHUNK_SIZE_CHARS,
   87 | 				MIN_CHUNK_LENGTH_TO_EMBED, MAX_NUM_CHUNKS, KEEP_SEPARATOR, DEFAULT_PUNCTUATION_MARKS);
   88 | 	}
   89 | 
   90 | 	/**
   91 | 	 * @deprecated since 2.0.0-M3, use {@link #builder()} instead.
   92 | 	 */
   93 | 	@Deprecated(since = "2.0.0-M3", forRemoval = true)
   94 | 	public TokenTextSplitter(boolean keepSeparator) {
   95 | 		this(DEFAULT_ENCODING_TYPE, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP, MIN_CHUNK_SIZE_CHARS,
   96 | 				MIN_CHUNK_LENGTH_TO_EMBED, MAX_NUM_CHUNKS, keepSeparator, DEFAULT_PUNCTUATION_MARKS);
   97 | 	}
   98 | 
   99 | 	/**
  100 | 	 * @deprecated since 2.0.0-M3, use {@link #builder()} instead.
  101 | 	 */
  102 | 	@Deprecated(since = "2.0.0-M3", forRemoval = true)
  103 | 	public TokenTextSplitter(EncodingType encodingType) {
  104 | 		this(encodingType, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP, MIN_CHUNK_SIZE_CHARS, MIN_CHUNK_LENGTH_TO_EMBED,
  105 | 				MAX_NUM_CHUNKS, KEEP_SEPARATOR, DEFAULT_PUNCTUATION_MARKS);
  106 | 	}
  107 | 
  108 | 	/**
  109 | 	 * @deprecated since 2.0.0-M3, use {@link #builder()} instead.
  110 | 	 */
  111 | 	@Deprecated(since = "2.0.0-M3", forRemoval = true)
  112 | 	public TokenTextSplitter(EncodingType encodingType, boolean keepSeparator) {
  113 | 		this(encodingType, DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP, MIN_CHUNK_SIZE_CHARS, MIN_CHUNK_LENGTH_TO_EMBED,
  114 | 				MAX_NUM_CHUNKS, keepSeparator, DEFAULT_PUNCTUATION_MARKS);
  115 | 	}
  116 | 
  117 | 	/**
  118 | 	 * @deprecated since 2.0.0-M3, use {@link #builder()} instead.
  119 | 	 */
  120 | 	@Deprecated(since = "2.0.0-M3", forRemoval = true)
  121 | 	public TokenTextSplitter(int chunkSize, int minChunkSizeChars, int minChunkLengthToEmbed, int maxNumChunks,
  122 | 			boolean keepSeparator, List<Character> punctuationMarks) {
  123 | 		this(DEFAULT_ENCODING_TYPE, chunkSize, DEFAULT_CHUNK_OVERLAP, minChunkSizeChars, minChunkLengthToEmbed,
  124 | 				maxNumChunks, keepSeparator, punctuationMarks);
  125 | 	}
  126 | 
  127 | 	private TokenTextSplitter(EncodingType encodingType, int chunkSize, int chunkOverlap, int minChunkSizeChars,
  128 | 			int minChunkLengthToEmbed, int maxNumChunks, boolean keepSeparator, List<Character> punctuationMarks) {
  129 | 		Assert.notNull(encodingType, "encodingType must not be null");
  130 | 		Assert.notEmpty(punctuationMarks, "punctuationMarks must not be empty");
  131 | 		Assert.isTrue(chunkSize > 0, "chunkSize must be greater than zero");
  132 | 		Assert.isTrue(chunkOverlap >= 0, "chunk overlap must not be negative");
  133 | 		Assert.isTrue(chunkOverlap < chunkSize, "chunk overlap must be less than chunk size");
  134 | 		Assert.isTrue(maxNumChunks > 0, "maxNumChunks must be greater than zero");
  135 | 		Assert.isTrue(minChunkSizeChars >= 0, "minChunkSizeChars must not be negative");
  136 | 		Assert.isTrue(minChunkLengthToEmbed >= 0, "minChunkLengthToEmbed must not be negative");
  137 | 		this.encoding = this.registry.getEncoding(encodingType);
  138 | 		this.chunkSize = chunkSize;
  139 | 		this.chunkOverlap = chunkOverlap;
  140 | 		this.minChunkSizeChars = minChunkSizeChars;
  141 | 		this.minChunkLengthToEmbed = minChunkLengthToEmbed;
  142 | 		this.maxNumChunks = maxNumChunks;
  143 | 		this.keepSeparator = keepSeparator;
  144 | 		this.punctuationMarks = punctuationMarks;
  145 | 	}
  146 | 
  147 | 	public static Builder builder() {
  148 | 		return new Builder();
  149 | 	}
  150 | 
  151 | 	@Override
  152 | 	protected List<String> splitText(String text) {
  153 | 		return doSplit(text, this.chunkSize);
  154 | 	}
  155 | 
  156 | 	/**
  157 | 	 * Splits text into chunks based on token count, using the overlap configured on this
  158 | 	 * splitter.
  159 | 	 * <p>
  160 | 	 * Subclasses that override this method replace the whole algorithm, so overlap does
  161 | 	 * not apply to them unless they call {@link #doSplit(String, int, int)}.
  162 | 	 * @param text the text to split
  163 | 	 * @param chunkSize the target chunk size in tokens
  164 | 	 * @return list of text chunks
  165 | 	 */
  166 | 	protected List<String> doSplit(String text, int chunkSize) {
  167 | 		return doSplit(text, chunkSize, this.chunkOverlap);
  168 | 	}
  169 | 
  170 | 	/**
  171 | 	 * Splits text into chunks based on token count.
  172 | 	 * <p>
  173 | 	 * Punctuation-based splitting only applies when the token count exceeds the chunk
  174 | 	 * size ({@code tokens.size() > chunkSize}). Text that exactly matches or is smaller
  175 | 	 * than the chunk size is returned as a single chunk without punctuation-based
  176 | 	 * truncation.
  177 | 	 * @param text the text to split
  178 | 	 * @param chunkSize the target chunk size in tokens
  179 | 	 * @param chunkOverlap the number of tokens each chunk shares with the one before it
  180 | 	 * @return list of text chunks
  181 | 	 * @since 2.1.0
  182 | 	 */
  183 | 	protected List<String> doSplit(String text, int chunkSize, int chunkOverlap) {
  184 | 		if (text == null || text.trim().isEmpty()) {
  185 | 			return new ArrayList<>();
  186 | 		}
  187 | 
  188 | 		List<Integer> tokens = getEncodedTokens(text);
  189 | 
  190 | 		// If text is smaller than chunk size, return as a single chunk
  191 | 		if (tokens.size() <= chunkSize) {
  192 | 			String processedText = this.keepSeparator ? text.trim() : text.replace(System.lineSeparator(), " ").trim();
  193 | 
  194 | 			if (processedText.length() > this.minChunkLengthToEmbed) {
  195 | 				return List.of(processedText);
  196 | 			}
  197 | 			return new ArrayList<>();
  198 | 		}
  199 | 		List<String> chunks = new ArrayList<>();
  200 | 		int position = 0;
  201 | 		int num_chunks = 0;
  202 | 
  203 | 		while (position < tokens.size() && num_chunks < this.maxNumChunks) {
  204 | 			int chunkEnd = Math.min(position + chunkSize, tokens.size());
  205 | 
  206 | 			// Extract tokens for this chunk
  207 | 			List<Integer> chunkTokens = tokens.subList(position, chunkEnd);
  208 | 			String chunkText = decodeTokens(chunkTokens);
  209 | 
  210 | 			// Skip the chunk if it is empty or whitespace
  211 | 			if (chunkText.trim().isEmpty()) {
  212 | 				position = chunkEnd;
  213 | 				continue;
  214 | 			}
  215 | 
  216 | 			// Apply sentence boundary optimization, but only while more text remains than
  217 | 			// fits in this chunk, so the last chunk is never cut at punctuation
  218 | 			String optimizedText = (chunkEnd < tokens.size()) ? optimizeChunkBoundary(chunkText) : chunkText;
  219 | 			int optimizedTokenCount = getEncodedTokens(optimizedText).size();
  220 | 
  221 | 			// Use optimized chunk
  222 | 			String finalChunkText = optimizedText;
  223 | 			int finalChunkTokenCount = optimizedTokenCount;
  224 | 
  225 | 			// Advance past this chunk, stepping back chunkOverlap tokens so the next
  226 | 			// chunk repeats them, but always move forward by at least one token
  227 | 			int advance = Math.max(1, finalChunkTokenCount - chunkOverlap);
  228 | 
  229 | 			// Once a chunk reaches the end of the text, or only whitespace follows it,
  230 | 			// stop instead of stepping back, so no trailing chunk repeats text that was
  231 | 			// already emitted. Only a short remainder is checked for whitespace.
  232 | 			int chunkEndPosition = position + finalChunkTokenCount;
  233 | 			int remainingTokens = tokens.size() - chunkEndPosition;
  234 | 			boolean reachedEnd = remainingTokens <= 0 || (remainingTokens <= chunkSize
  235 | 					&& decodeTokens(tokens.subList(chunkEndPosition, tokens.size())).isBlank());
  236 | 			position = reachedEnd ? tokens.size() : position + advance;
  237 | 
  238 | 			// Format according to keepSeparator setting
  239 | 			String formattedChunk = this.keepSeparator ? finalChunkText.trim()
  240 | 					: finalChunkText.replace(System.lineSeparator(), " ").trim();
  241 | 
  242 | 			// Add chunk if it meets minimum length
  243 | 			if (formattedChunk.length() > this.minChunkLengthToEmbed) {
  244 | 				chunks.add(formattedChunk);
  245 | 			}
  246 | 
  247 | 			// Count every chunk toward maxNumChunks, including ones dropped as too short
  248 | 			num_chunks++;
  249 | 		}
  250 | 
  251 | 		// Handle the remaining tokens
  252 | 		if (position < tokens.size()) {
  253 | 			String remaining_text = decodeTokens(tokens.subList(position, tokens.size()))
  254 | 				.replace(System.lineSeparator(), " ")
  255 | 				.trim();
  256 | 			if (remaining_text.length() > this.minChunkLengthToEmbed) {
  257 | 				chunks.add(remaining_text);
  258 | 			}
  259 | 		}
  260 | 
  261 | 		return chunks;
  262 | 	}
  263 | 
  264 | 	private String optimizeChunkBoundary(String chunkText) {
  265 | 		if (chunkText.length() <= this.minChunkSizeChars) {
  266 | 			return chunkText;
  267 | 		}
  268 | 
  269 | 		// Find the last configured punctuation mark in the chunk
  270 | 		int lastPunctuation = getLastPunctuationIndex(chunkText);
  271 | 
  272 | 		// Cut after it, keeping the punctuation, unless that would leave the chunk too
  273 | 		// short
  274 | 		if (lastPunctuation != -1 && lastPunctuation > this.minChunkSizeChars) {
  275 | 			return chunkText.substring(0, lastPunctuation + 1);
  276 | 		}
  277 | 
  278 | 		// Otherwise return the original chunk
  279 | 		return chunkText;
  280 | 	}
  281 | 
  282 | 	protected int getLastPunctuationIndex(String chunkText) {
  283 | 		// find the max index of any punctuation mark
  284 | 		int maxLastPunctuation = -1;
  285 | 		for (Character punctuationMark : this.punctuationMarks) {
  286 | 			int lastPunctuation = chunkText.lastIndexOf(punctuationMark);
  287 | 			maxLastPunctuation = Math.max(maxLastPunctuation, lastPunctuation);
  288 | 		}
  289 | 		return maxLastPunctuation;
  290 | 	}
  291 | 
  292 | 	private List<Integer> getEncodedTokens(String text) {
  293 | 		Assert.notNull(text, "Text must not be null");
  294 | 		return this.encoding.encode(text).boxed();
  295 | 	}
  296 | 
  297 | 	private String decodeTokens(List<Integer> tokens) {
  298 | 		Assert.notNull(tokens, "Tokens must not be null");
  299 | 		var tokensIntArray = new IntArrayList(tokens.size());
  300 | 		tokens.forEach(tokensIntArray::add);
  301 | 		return this.encoding.decode(tokensIntArray);
  302 | 	}
  303 | 
  304 | 	public static final class Builder {
  305 | 
  306 | 		private EncodingType encodingType = DEFAULT_ENCODING_TYPE;
  307 | 
  308 | 		private int chunkSize = DEFAULT_CHUNK_SIZE;
  309 | 
  310 | 		private int chunkOverlap = DEFAULT_CHUNK_OVERLAP;
  311 | 
  312 | 		private int minChunkSizeChars = MIN_CHUNK_SIZE_CHARS;
  313 | 
  314 | 		private int minChunkLengthToEmbed = MIN_CHUNK_LENGTH_TO_EMBED;
  315 | 
  316 | 		private int maxNumChunks = MAX_NUM_CHUNKS;
  317 | 
  318 | 		private boolean keepSeparator = KEEP_SEPARATOR;
  319 | 
  320 | 		private List<Character> punctuationMarks = DEFAULT_PUNCTUATION_MARKS;
  321 | 
  322 | 		private Builder() {
  323 | 		}
  324 | 
  325 | 		public Builder withEncodingType(EncodingType encodingType) {
  326 | 			this.encodingType = encodingType;
  327 | 			return this;
  328 | 		}
  329 | 
  330 | 		public Builder withChunkSize(int chunkSize) {
  331 | 			this.chunkSize = chunkSize;
  332 | 			return this;
  333 | 		}
  334 | 
  335 | 		public Builder withChunkOverlap(int chunkOverlap) {
  336 | 			this.chunkOverlap = chunkOverlap;
  337 | 			return this;
  338 | 		}
  339 | 
  340 | 		public Builder withMinChunkSizeChars(int minChunkSizeChars) {
  341 | 			this.minChunkSizeChars = minChunkSizeChars;
  342 | 			return this;
  343 | 		}
  344 | 
  345 | 		public Builder withMinChunkLengthToEmbed(int minChunkLengthToEmbed) {
  346 | 			this.minChunkLengthToEmbed = minChunkLengthToEmbed;
  347 | 			return this;
  348 | 		}
  349 | 
  350 | 		public Builder withMaxNumChunks(int maxNumChunks) {
  351 | 			this.maxNumChunks = maxNumChunks;
  352 | 			return this;
  353 | 		}
  354 | 
  355 | 		public Builder withKeepSeparator(boolean keepSeparator) {
  356 | 			this.keepSeparator = keepSeparator;
  357 | 			return this;
  358 | 		}
  359 | 
  360 | 		public Builder withPunctuationMarks(List<Character> punctuationMarks) {
  361 | 			this.punctuationMarks = punctuationMarks;
  362 | 			return this;
  363 | 		}
  364 | 
  365 | 		public TokenTextSplitter build() {
  366 | 			return new TokenTextSplitter(this.encodingType, this.chunkSize, this.chunkOverlap, this.minChunkSizeChars,
  367 | 					this.minChunkLengthToEmbed, this.maxNumChunks, this.keepSeparator, this.punctuationMarks);
  368 | 		}
  369 | 
  370 | 	}
  371 | 
  372 | }
```
