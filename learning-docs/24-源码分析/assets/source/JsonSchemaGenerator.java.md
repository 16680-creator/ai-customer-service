<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/util/json/schema/JsonSchemaGenerator.java -->
<!-- 正文引用：[[源码:JsonSchemaGenerator.java#L起-L止]] ；代码块头部：(src: JsonSchemaGenerator.java#L起-L止) -->

# JsonSchemaGenerator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/util/json/schema/JsonSchemaGenerator.java` |
| 行数 | 362 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:24.715Z |

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
   17 | package org.springframework.ai.util.json.schema;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | import java.lang.reflect.Parameter;
   21 | import java.lang.reflect.Type;
   22 | import java.util.ArrayList;
   23 | import java.util.List;
   24 | import java.util.Locale;
   25 | import java.util.stream.Stream;
   26 | 
   27 | import com.fasterxml.jackson.annotation.JsonProperty;
   28 | import com.fasterxml.jackson.annotation.JsonPropertyDescription;
   29 | import com.github.victools.jsonschema.generator.Module;
   30 | import com.github.victools.jsonschema.generator.Option;
   31 | import com.github.victools.jsonschema.generator.OptionPreset;
   32 | import com.github.victools.jsonschema.generator.SchemaGenerator;
   33 | import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
   34 | import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
   35 | import com.github.victools.jsonschema.generator.SchemaVersion;
   36 | import com.github.victools.jsonschema.module.jackson.JacksonOption;
   37 | import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;
   38 | import com.github.victools.jsonschema.module.swagger2.Swagger2Module;
   39 | import io.swagger.v3.oas.annotations.media.Schema;
   40 | import org.jspecify.annotations.Nullable;
   41 | import tools.jackson.databind.JsonNode;
   42 | import tools.jackson.databind.node.ObjectNode;
   43 | 
   44 | import org.springframework.ai.chat.model.ToolContext;
   45 | import org.springframework.ai.model.KotlinModule;
   46 | import org.springframework.ai.tool.annotation.ToolParam;
   47 | import org.springframework.ai.util.JacksonUtils;
   48 | import org.springframework.core.KotlinDetector;
   49 | import org.springframework.core.Nullness;
   50 | import org.springframework.util.Assert;
   51 | import org.springframework.util.ClassUtils;
   52 | import org.springframework.util.StringUtils;
   53 | 
   54 | /**
   55 |  * Utilities to generate JSON Schemas from Java types and method signatures. It's designed
   56 |  * to work well in the context of tool calling and structured outputs, aiming at ensuring
   57 |  * consistency and robustness across different model providers.
   58 |  * <p>
   59 |  * Metadata such as descriptions and required properties can be specified using one of the
   60 |  * following supported annotations:
   61 |  * <p>
   62 |  * <ul>
   63 |  * <li>{@code @ToolParam(required = ..., description = ...)}</li>
   64 |  * <li>{@code @JsonProperty(required = ...)}</li>
   65 |  * <li>{@code @JsonClassDescription(...)}</li>
   66 |  * <li>{@code @JsonPropertyDescription(...)}</li>
   67 |  * <li>{@code @Schema(required = ..., description = ...)}</li>
   68 |  * <li>{@code @Nullable}</li>
   69 |  * </ul>
   70 |  * <p>
   71 |  * If none of these annotations are present, the default behavior is to consider the
   72 |  * property as required and not to include a description.
   73 |  * <p>
   74 |  *
   75 |  * @author Thomas Vitale
   76 |  * @author Sebastien Deleuze
   77 |  * @since 1.0.0
   78 |  */
   79 | public final class JsonSchemaGenerator {
   80 | 
   81 | 	/**
   82 | 	 * To ensure consistency and robustness across different model providers, all
   83 | 	 * properties in the JSON Schema are considered required by default. This behavior can
   84 | 	 * be overridden by setting the {@link ToolParam#required()},
   85 | 	 * {@link JsonProperty#required()}, or {@link Schema#requiredMode()}} annotation.
   86 | 	 */
   87 | 	private static final boolean PROPERTY_REQUIRED_BY_DEFAULT = true;
   88 | 
   89 | 	private static final SchemaGenerator typeSchemaGenerator;
   90 | 
   91 | 	private static final SchemaGenerator subtypeSchemaGenerator;
   92 | 
   93 | 	/*
   94 | 	 * Initialize JSON Schema generators.
   95 | 	 */
   96 | 	static {
   97 | 		Module jacksonModule = new JacksonSchemaModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED,
   98 | 				JacksonOption.RESPECT_JSONPROPERTY_ORDER);
   99 | 		Module openApiModule = new Swagger2Module();
  100 | 		Module springAiSchemaModule = PROPERTY_REQUIRED_BY_DEFAULT ? new SpringAiSchemaModule()
  101 | 				: new SpringAiSchemaModule(SpringAiSchemaModule.Option.PROPERTY_REQUIRED_FALSE_BY_DEFAULT);
  102 | 
  103 | 		SchemaGeneratorConfigBuilder schemaGeneratorConfigBuilder = new SchemaGeneratorConfigBuilder(
  104 | 				SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON)
  105 | 			.with(jacksonModule)
  106 | 			.with(openApiModule)
  107 | 			.with(springAiSchemaModule)
  108 | 			.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
  109 | 			.with(Option.PLAIN_DEFINITION_KEYS);
  110 | 
  111 | 		if (KotlinDetector.isKotlinReflectPresent()) {
  112 | 			schemaGeneratorConfigBuilder.with(new KotlinModule());
  113 | 		}
  114 | 
  115 | 		SchemaGeneratorConfig typeSchemaGeneratorConfig = schemaGeneratorConfigBuilder.build();
  116 | 		typeSchemaGenerator = new SchemaGenerator(typeSchemaGeneratorConfig);
  117 | 
  118 | 		SchemaGeneratorConfig subtypeSchemaGeneratorConfig = schemaGeneratorConfigBuilder
  119 | 			.without(Option.SCHEMA_VERSION_INDICATOR)
  120 | 			.build();
  121 | 		subtypeSchemaGenerator = new SchemaGenerator(subtypeSchemaGeneratorConfig);
  122 | 	}
  123 | 
  124 | 	private JsonSchemaGenerator() {
  125 | 	}
  126 | 
  127 | 	private static ObjectNode generateSchema(SchemaGenerator generator, Type type) {
  128 | 		synchronized (generator) {
  129 | 			return generator.generateSchema(type);
  130 | 		}
  131 | 	}
  132 | 
  133 | 	/**
  134 | 	 * Generate a JSON Schema for a method's input parameters.
  135 | 	 */
  136 | 	public static String generateForMethodInput(Method method, SchemaOption... schemaOptions) {
  137 | 		ObjectNode schema = JacksonUtils.getDefaultJsonMapper().createObjectNode();
  138 | 		schema.put("$schema", SchemaVersion.DRAFT_2020_12.getIdentifier());
  139 | 		schema.put("type", "object");
  140 | 		ObjectNode defs = schema.putObject("$defs");
  141 | 
  142 | 		ObjectNode properties = schema.putObject("properties");
  143 | 		List<String> required = new ArrayList<>();
  144 | 
  145 | 		for (int i = 0; i < method.getParameterCount(); i++) {
  146 | 			String parameterName = method.getParameters()[i].getName();
  147 | 			Type parameterType = method.getGenericParameterTypes()[i];
  148 | 			if (parameterType instanceof Class<?> parameterClass
  149 | 					&& ClassUtils.isAssignable(ToolContext.class, parameterClass)) {
  150 | 				// A ToolContext method parameter is not included in the JSON Schema
  151 | 				// generation.
  152 | 				// It's a special type used by Spring AI to pass contextual data to tools
  153 | 				// outside the model interaction flow.
  154 | 				continue;
  155 | 			}
  156 | 			// A Kotlin suspend function carries a synthetic trailing Continuation
  157 | 			// parameter that is not part of the tool contract and must not appear in
  158 | 			// the generated schema.
  159 | 			if (KotlinDetector.isSuspendingFunction(method) && i == method.getParameterCount() - 1) {
  160 | 				continue;
  161 | 			}
  162 | 			if (isMethodParameterRequired(method, i)) {
  163 | 				required.add(parameterName);
  164 | 			}
  165 | 			ObjectNode parameterNode = generateSchema(subtypeSchemaGenerator, parameterType);
  166 | 			// victools generates self-contained schemas where $defs and the $ref
  167 | 			// pointers into them are rooted at the sub-schema. Inlining the
  168 | 			// sub-schema under properties.<paramName> re-parents existing
  169 | 			// "#/$defs/<Name>" refs to the outer root, leaving them unresolvable.
  170 | 			// Hoist $defs to the outer root so those refs resolve again.
  171 | 			JsonSchemaUtils.hoistDefsToRoot(schema, parameterNode);
  172 | 			// Remove OpenAPI format as some LLMs (like Mistral) don't handle them.
  173 | 			parameterNode.remove("format");
  174 | 			String parameterDescription = getMethodParameterDescription(method, i);
  175 | 			if (StringUtils.hasText(parameterDescription)) {
  176 | 				parameterNode.put("description", parameterDescription);
  177 | 			}
  178 | 			properties.set(parameterName, parameterNode);
  179 | 		}
  180 | 
  181 | 		if (defs.isEmpty()) {
  182 | 			schema.remove("$defs");
  183 | 		}
  184 | 
  185 | 		var requiredArray = schema.putArray("required");
  186 | 		required.forEach(requiredArray::add);
  187 | 
  188 | 		processSchemaOptions(schemaOptions, schema);
  189 | 
  190 | 		return schema.toPrettyString();
  191 | 	}
  192 | 
  193 | 	/**
  194 | 	 * Generate a JSON Schema for a class type.
  195 | 	 */
  196 | 	public static String generateForType(Type type, SchemaOption... schemaOptions) {
  197 | 		Assert.notNull(type, "type cannot be null");
  198 | 		ObjectNode schema = generateSchema(typeSchemaGenerator, type);
  199 | 		if ((type == Void.class) && !schema.has("properties")) {
  200 | 			schema.putObject("properties");
  201 | 		}
  202 | 		processSchemaOptions(schemaOptions, schema);
  203 | 		return schema.toPrettyString();
  204 | 	}
  205 | 
  206 | 	private static void processSchemaOptions(SchemaOption[] schemaOptions, ObjectNode schema) {
  207 | 		if (Stream.of(schemaOptions)
  208 | 			.noneMatch(option -> option == SchemaOption.ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT)) {
  209 | 			forbidAdditionalProperties(schema);
  210 | 		}
  211 | 		if (Stream.of(schemaOptions).anyMatch(option -> option == SchemaOption.UPPER_CASE_TYPE_VALUES)) {
  212 | 			convertTypeValuesToUpperCase(schema);
  213 | 		}
  214 | 	}
  215 | 
  216 | 	/**
  217 | 	 * Determines whether a property is required based on the presence of a series of *
  218 | 	 * annotations.
  219 | 	 *
  220 | 	 * <p>
  221 | 	 * <ul>
  222 | 	 * <li>{@code @ToolParam(required = ...)}</li>
  223 | 	 * <li>{@code @JsonProperty(required = ...)}</li>
  224 | 	 * <li>{@code @Schema(required = ...)}</li>
  225 | 	 * <li>{@code @Nullable}</li>
  226 | 	 * </ul>
  227 | 	 * <p>
  228 | 	 *
  229 | 	 * If none of these annotations are present, the default behavior is to consider the *
  230 | 	 * property as required.
  231 | 	 */
  232 | 	private static boolean isMethodParameterRequired(Method method, int index) {
  233 | 		Parameter parameter = method.getParameters()[index];
  234 | 
  235 | 		var toolParamAnnotation = parameter.getAnnotation(ToolParam.class);
  236 | 		if (toolParamAnnotation != null) {
  237 | 			return toolParamAnnotation.required();
  238 | 		}
  239 | 
  240 | 		var propertyAnnotation = parameter.getAnnotation(JsonProperty.class);
  241 | 		if (propertyAnnotation != null) {
  242 | 			return propertyAnnotation.required();
  243 | 		}
  244 | 
  245 | 		var schemaAnnotation = parameter.getAnnotation(Schema.class);
  246 | 		if (schemaAnnotation != null) {
  247 | 			return schemaAnnotation.requiredMode() == Schema.RequiredMode.REQUIRED
  248 | 					|| schemaAnnotation.requiredMode() == Schema.RequiredMode.AUTO || schemaAnnotation.required();
  249 | 		}
  250 | 
  251 | 		Nullness nullness = Nullness.forParameter(parameter);
  252 | 		if (nullness == Nullness.NULLABLE) {
  253 | 			return false;
  254 | 		}
  255 | 
  256 | 		return PROPERTY_REQUIRED_BY_DEFAULT;
  257 | 	}
  258 | 
  259 | 	/**
  260 | 	 * Determines a property description based on the presence of a series of annotations.
  261 | 	 *
  262 | 	 * <p>
  263 | 	 * <ul>
  264 | 	 * <li>{@code @ToolParam(description = ...)}</li>
  265 | 	 * <li>{@code @JsonPropertyDescription(...)}</li>
  266 | 	 * <li>{@code @Schema(description = ...)}</li>
  267 | 	 * </ul>
  268 | 	 * <p>
  269 | 	 */
  270 | 	private static @Nullable String getMethodParameterDescription(Method method, int index) {
  271 | 		Parameter parameter = method.getParameters()[index];
  272 | 
  273 | 		var toolParamAnnotation = parameter.getAnnotation(ToolParam.class);
  274 | 		if (toolParamAnnotation != null && StringUtils.hasText(toolParamAnnotation.description())) {
  275 | 			return toolParamAnnotation.description();
  276 | 		}
  277 | 
  278 | 		var jacksonAnnotation = parameter.getAnnotation(JsonPropertyDescription.class);
  279 | 		if (jacksonAnnotation != null && StringUtils.hasText(jacksonAnnotation.value())) {
  280 | 			return jacksonAnnotation.value();
  281 | 		}
  282 | 
  283 | 		var schemaAnnotation = parameter.getAnnotation(Schema.class);
  284 | 		if (schemaAnnotation != null && StringUtils.hasText(schemaAnnotation.description())) {
  285 | 			return schemaAnnotation.description();
  286 | 		}
  287 | 
  288 | 		return null;
  289 | 	}
  290 | 
  291 | 	/**
  292 | 	 * Recursively adds {@code "additionalProperties": false} to all object schemas (nodes
  293 | 	 * with a {@code "properties"} key) that do not already define
  294 | 	 * {@code "additionalProperties"}. The guard preserves {@code Map<K,V>} schemas where
  295 | 	 * {@code "additionalProperties"} is a type reference rather than a boolean.
  296 | 	 */
  297 | 	private static void forbidAdditionalProperties(ObjectNode node) {
  298 | 		if (node.has("properties") && !node.has("additionalProperties")) {
  299 | 			node.put("additionalProperties", false);
  300 | 		}
  301 | 		node.properties().forEach(entry -> {
  302 | 			JsonNode value = entry.getValue();
  303 | 			if (value.isObject()) {
  304 | 				forbidAdditionalProperties((ObjectNode) value);
  305 | 			}
  306 | 			else if (value.isArray()) {
  307 | 				value.forEach(element -> {
  308 | 					if (element.isObject()) {
  309 | 						forbidAdditionalProperties((ObjectNode) element);
  310 | 					}
  311 | 				});
  312 | 			}
  313 | 		});
  314 | 	}
  315 | 
  316 | 	public static void convertTypeValuesToUpperCase(ObjectNode node) {
  317 | 		if (node.isObject()) {
  318 | 			node.properties().forEach(entry -> {
  319 | 				JsonNode value = entry.getValue();
  320 | 				if (value.isObject()) {
  321 | 					convertTypeValuesToUpperCase((ObjectNode) value);
  322 | 				}
  323 | 				else if (value.isArray()) {
  324 | 					value.forEach(element -> {
  325 | 						if (element.isObject() || element.isArray()) {
  326 | 							convertTypeValuesToUpperCase((ObjectNode) element);
  327 | 						}
  328 | 					});
  329 | 				}
  330 | 				else if (value.isString() && entry.getKey().equals("type")) {
  331 | 					String oldValue = node.get("type").asString();
  332 | 					node.put("type", oldValue.toUpperCase(Locale.ROOT));
  333 | 				}
  334 | 			});
  335 | 		}
  336 | 		else if (node.isArray()) {
  337 | 			node.forEach(element -> {
  338 | 				if (element.isObject() || element.isArray()) {
  339 | 					convertTypeValuesToUpperCase((ObjectNode) element);
  340 | 				}
  341 | 			});
  342 | 		}
  343 | 	}
  344 | 
  345 | 	/**
  346 | 	 * Options for generating JSON Schemas.
  347 | 	 */
  348 | 	public enum SchemaOption {
  349 | 
  350 | 		/**
  351 | 		 * Allow an object to contain additional key/values not defined in the schema.
  352 | 		 */
  353 | 		ALLOW_ADDITIONAL_PROPERTIES_BY_DEFAULT,
  354 | 
  355 | 		/**
  356 | 		 * Convert all "type" values to upper case.
  357 | 		 */
  358 | 		UPPER_CASE_TYPE_VALUES
  359 | 
  360 | 	}
  361 | 
  362 | }
```
