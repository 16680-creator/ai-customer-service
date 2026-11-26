<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/converter/BeanOutputConverter.java -->
<!-- 正文引用：[[源码:BeanOutputConverter.java#L起-L止]] ；代码块头部：(src: BeanOutputConverter.java#L起-L止) -->

# BeanOutputConverter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/converter/BeanOutputConverter.java` |
| 行数 | 245 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:04.774Z |

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
   17 | package org.springframework.ai.converter;
   18 | 
   19 | import java.lang.reflect.Type;
   20 | import java.util.Map;
   21 | import java.util.Objects;
   22 | 
   23 | import org.apache.commons.logging.Log;
   24 | import org.apache.commons.logging.LogFactory;
   25 | import org.jspecify.annotations.Nullable;
   26 | import tools.jackson.core.JacksonException;
   27 | import tools.jackson.databind.DeserializationFeature;
   28 | import tools.jackson.databind.json.JsonMapper;
   29 | 
   30 | import org.springframework.ai.util.JacksonUtils;
   31 | import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
   32 | import org.springframework.core.ParameterizedTypeReference;
   33 | 
   34 | /**
   35 |  * An implementation of {@link StructuredOutputConverter} that transforms the LLM output
   36 |  * to a specific object type using JSON schema. This converter works by generating a JSON
   37 |  * schema based on a given Java class or parameterized type reference, which is then used
   38 |  * to validate and transform the LLM output into the desired type.
   39 |  *
   40 |  * @param <T> The target type to which the output will be converted.
   41 |  * @author Mark Pollack
   42 |  * @author Christian Tzolov
   43 |  * @author Sebastian Ullrich
   44 |  * @author Kirk Lund
   45 |  * @author Josh Long
   46 |  * @author Sebastien Deleuze
   47 |  * @author Soby Chacko
   48 |  * @author Thomas Vitale
   49 |  * @author liugddx
   50 |  */
   51 | public class BeanOutputConverter<T> implements StructuredOutputConverter<T> {
   52 | 
   53 | 	private final Log logger = LogFactory.getLog(BeanOutputConverter.class);
   54 | 
   55 | 	/**
   56 | 	 * The target class type reference to which the output will be converted.
   57 | 	 */
   58 | 	private final Type type;
   59 | 
   60 | 	/** The JSON mapper used for deserialization and other JSON operations. */
   61 | 	private final JsonMapper jsonMapper;
   62 | 
   63 | 	/** Holds the generated JSON schema for the target type. */
   64 | 	private final String jsonSchema;
   65 | 
   66 | 	/** The text cleaner used to preprocess LLM responses before parsing. */
   67 | 	private final ResponseTextCleaner textCleaner;
   68 | 
   69 | 	/**
   70 | 	 * Constructor to initialize with the target type's class.
   71 | 	 * @param clazz The target type's class.
   72 | 	 */
   73 | 	public BeanOutputConverter(Class<T> clazz) {
   74 | 		this(clazz, null, null);
   75 | 	}
   76 | 
   77 | 	/**
   78 | 	 * Constructor to initialize with the target type's class, a custom JSON mapper, and a
   79 | 	 * line endings normalizer to ensure consistent line endings on any platform.
   80 | 	 * @param clazz The target type's class.
   81 | 	 * @param jsonMapper Custom JSON mapper for JSON operations. endings.
   82 | 	 */
   83 | 	public BeanOutputConverter(Class<T> clazz, @Nullable JsonMapper jsonMapper) {
   84 | 		this(clazz, jsonMapper, null);
   85 | 	}
   86 | 
   87 | 	/**
   88 | 	 * Constructor to initialize with the target type's class, a custom JSON mapper, and a
   89 | 	 * custom text cleaner.
   90 | 	 * @param clazz The target type's class.
   91 | 	 * @param jsonMapper Custom JSON mapper for JSON operations.
   92 | 	 * @param textCleaner Custom text cleaner for preprocessing responses.
   93 | 	 */
   94 | 	public BeanOutputConverter(Class<T> clazz, @Nullable JsonMapper jsonMapper,
   95 | 			@Nullable ResponseTextCleaner textCleaner) {
   96 | 		this(ParameterizedTypeReference.forType(clazz), jsonMapper, textCleaner);
   97 | 	}
   98 | 
   99 | 	/**
  100 | 	 * Constructor to initialize with the target class type reference.
  101 | 	 * @param typeRef The target class type reference.
  102 | 	 */
  103 | 	public BeanOutputConverter(ParameterizedTypeReference<T> typeRef) {
  104 | 		this(typeRef, null, null);
  105 | 	}
  106 | 
  107 | 	/**
  108 | 	 * Constructor to initialize with the target class type reference, a custom JSON
  109 | 	 * mapper, and a line endings normalizer to ensure consistent line endings on any
  110 | 	 * platform.
  111 | 	 * @param typeRef The target class type reference.
  112 | 	 * @param jsonMapper Custom JSON mapper for JSON operations. endings.
  113 | 	 */
  114 | 	public BeanOutputConverter(ParameterizedTypeReference<T> typeRef, @Nullable JsonMapper jsonMapper) {
  115 | 		this(typeRef, jsonMapper, null);
  116 | 	}
  117 | 
  118 | 	/**
  119 | 	 * Constructor to initialize with the target class type reference, a custom JSON
  120 | 	 * mapper, and a custom text cleaner.
  121 | 	 * @param typeRef The target class type reference.
  122 | 	 * @param jsonMapper Custom JSON mapper for JSON operations.
  123 | 	 * @param textCleaner Custom text cleaner for preprocessing responses.
  124 | 	 */
  125 | 	public BeanOutputConverter(ParameterizedTypeReference<T> typeRef, @Nullable JsonMapper jsonMapper,
  126 | 			@Nullable ResponseTextCleaner textCleaner) {
  127 | 		this(typeRef.getType(), jsonMapper, textCleaner);
  128 | 	}
  129 | 
  130 | 	/**
  131 | 	 * Constructor to initialize with the target class type reference, a custom JSON
  132 | 	 * mapper, and a line endings normalizer to ensure consistent line endings on any
  133 | 	 * platform.
  134 | 	 * @param type The target class type.
  135 | 	 * @param jsonMapper Custom JSON mapper for JSON operations. endings.
  136 | 	 * @param textCleaner Custom text cleaner for preprocessing responses.
  137 | 	 */
  138 | 	private BeanOutputConverter(Type type, @Nullable JsonMapper jsonMapper, @Nullable ResponseTextCleaner textCleaner) {
  139 | 		Objects.requireNonNull(type, "Type cannot be null;");
  140 | 		this.type = type;
  141 | 		this.jsonMapper = jsonMapper != null ? jsonMapper : getJsonMapper();
  142 | 		this.textCleaner = textCleaner != null ? textCleaner : createDefaultTextCleaner();
  143 | 		this.jsonSchema = Objects.requireNonNull(generateSchema(), "JSON schema cannot be null");
  144 | 	}
  145 | 
  146 | 	/**
  147 | 	 * Creates the default text cleaner that handles common response formats from various
  148 | 	 * AI models.
  149 | 	 * <p>
  150 | 	 * The default cleaner includes:
  151 | 	 * <ul>
  152 | 	 * <li>{@link ThinkingTagCleaner} - Removes thinking tags from models like Amazon Nova
  153 | 	 * and Qwen. For models that don't generate thinking tags, this has minimal
  154 | 	 * performance impact due to fast-path optimization.</li>
  155 | 	 * <li>{@link MarkdownCodeBlockCleaner} - Removes markdown code block formatting.</li>
  156 | 	 * <li>{@link WhitespaceCleaner} - Trims whitespace.</li>
  157 | 	 * </ul>
  158 | 	 * <p>
  159 | 	 * To customize the cleaning behavior, provide a custom {@link ResponseTextCleaner}
  160 | 	 * via the constructor.
  161 | 	 * @return a composite text cleaner with default cleaning strategies
  162 | 	 */
  163 | 	private static ResponseTextCleaner createDefaultTextCleaner() {
  164 | 		return CompositeResponseTextCleaner.builder()
  165 | 			.addCleaner(new WhitespaceCleaner())
  166 | 			.addCleaner(new ThinkingTagCleaner())
  167 | 			.addCleaner(new MarkdownCodeBlockCleaner())
  168 | 			.addCleaner(new WhitespaceCleaner()) // Final trim after all cleanups
  169 | 			.build();
  170 | 	}
  171 | 
  172 | 	/**
  173 | 	 * Generates the JSON schema for the target type.
  174 | 	 * <p>
  175 | 	 * This method can be overridden in subclasses to customize the JSON schema generation
  176 | 	 * logic.
  177 | 	 * @return the generated JSON schema
  178 | 	 */
  179 | 	protected String generateSchema() {
  180 | 		return JsonSchemaGenerator.generateForType(this.type);
  181 | 	}
  182 | 
  183 | 	/**
  184 | 	 * Parses the given text to transform it to the desired target type.
  185 | 	 * @param text The LLM output in string format.
  186 | 	 * @return The parsed output in the desired target type.
  187 | 	 */
  188 | 	@SuppressWarnings("unchecked")
  189 | 	@Override
  190 | 	public T convert(String text) {
  191 | 		// Clean the text using the configured text cleaner
  192 | 		text = this.textCleaner.clean(text);
  193 | 
  194 | 		return (T) this.jsonMapper.readValue(text, this.jsonMapper.constructType(this.type));
  195 | 	}
  196 | 
  197 | 	/**
  198 | 	 * Configures and returns a JSON mapper for JSON operations.
  199 | 	 * @return Configured JSON mapper.
  200 | 	 */
  201 | 	protected JsonMapper getJsonMapper() {
  202 | 		return JsonMapper.builder()
  203 | 			.addModules(JacksonUtils.instantiateAvailableModules())
  204 | 			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
  205 | 			.build();
  206 | 	}
  207 | 
  208 | 	/**
  209 | 	 * Provides the expected format of the response, instructing that it should adhere to
  210 | 	 * the generated JSON schema.
  211 | 	 * @return The instruction format string.
  212 | 	 */
  213 | 	@Override
  214 | 	public String getFormat() {
  215 | 		String template = """
  216 | 				Your response should be in JSON format.
  217 | 				Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.
  218 | 				Do not include markdown code blocks in your response.
  219 | 				Remove the ```json markdown from the output.
  220 | 				Here is the JSON Schema instance your output must adhere to:
  221 | 				```%s```
  222 | 				""";
  223 | 		return String.format(template, this.jsonSchema);
  224 | 	}
  225 | 
  226 | 	/**
  227 | 	 * Provides the generated JSON schema for the target type.
  228 | 	 * @return The generated JSON schema.
  229 | 	 */
  230 | 	@Override
  231 | 	public String getJsonSchema() {
  232 | 		return this.jsonSchema;
  233 | 	}
  234 | 
  235 | 	public Map<String, Object> getJsonSchemaMap() {
  236 | 		try {
  237 | 			return this.jsonMapper.readValue(this.jsonSchema, Map.class);
  238 | 		}
  239 | 		catch (JacksonException ex) {
  240 | 			logger.error("Could not parse the JSON Schema to a Map object", ex);
  241 | 			throw new IllegalStateException(ex);
  242 | 		}
  243 | 	}
  244 | 
  245 | }
```
