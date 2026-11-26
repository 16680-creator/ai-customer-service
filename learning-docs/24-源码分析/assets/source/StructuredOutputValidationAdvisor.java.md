<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-client-chat:2.1.0 | path: org/springframework/ai/chat/client/advisor/StructuredOutputValidationAdvisor.java -->
<!-- 正文引用：[[源码:StructuredOutputValidationAdvisor.java#L起-L止]] ；代码块头部：(src: StructuredOutputValidationAdvisor.java#L起-L止) -->

# StructuredOutputValidationAdvisor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-client-chat:2.1.0` |
| 路径 | `org/springframework/ai/chat/client/advisor/StructuredOutputValidationAdvisor.java` |
| 行数 | 405 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-client-chat/2.1.0/spring-ai-client-chat-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:55:28.020Z |

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
   19 | import java.lang.reflect.Type;
   20 | import java.util.List;
   21 | import java.util.Objects;
   22 | import java.util.stream.Collectors;
   23 | 
   24 | import com.networknt.schema.Error;
   25 | import com.networknt.schema.Schema;
   26 | import com.networknt.schema.SchemaRegistry;
   27 | import com.networknt.schema.SpecificationVersion;
   28 | import org.apache.commons.logging.Log;
   29 | import org.apache.commons.logging.LogFactory;
   30 | import org.jspecify.annotations.Nullable;
   31 | import reactor.core.publisher.Flux;
   32 | import tools.jackson.core.JacksonException;
   33 | import tools.jackson.core.type.TypeReference;
   34 | import tools.jackson.databind.JsonNode;
   35 | import tools.jackson.databind.json.JsonMapper;
   36 | 
   37 | import org.springframework.ai.chat.client.ChatClientRequest;
   38 | import org.springframework.ai.chat.client.ChatClientResponse;
   39 | import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
   40 | import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
   41 | import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
   42 | import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
   43 | import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
   44 | import org.springframework.ai.chat.metadata.FinishReasonCategory;
   45 | import org.springframework.ai.chat.model.ChatResponse;
   46 | import org.springframework.ai.chat.model.Generation;
   47 | import org.springframework.ai.chat.model.GuardrailRefusal;
   48 | import org.springframework.ai.chat.prompt.Prompt;
   49 | import org.springframework.ai.util.JacksonUtils;
   50 | import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
   51 | import org.springframework.core.ParameterizedTypeReference;
   52 | import org.springframework.util.Assert;
   53 | import org.springframework.util.StringUtils;
   54 | 
   55 | /**
   56 |  * Advisor that validates the structured JSON output of a chat client response against a
   57 |  * JSON schema derived from the configured output type or a pre-supplied schema string.
   58 |  * <p>
   59 |  * When validation fails, the advisor appends the validation error to the user message and
   60 |  * re-invokes the model, repeating up to {@code maxRepeatAttempts} times.
   61 |  * <p>
   62 |  * A response that is not the model's output is returned as it is, without validation or
   63 |  * another attempt: a tool call, and the refusal of a guardrail (see
   64 |  * {@link GuardrailRefusal}). A response that a safety filter or a refusal of the model
   65 |  * stopped is validated, and returned without another attempt when it is not valid.
   66 |  * <p>
   67 |  * Streaming responses are not supported.
   68 |  *
   69 |  * @author Christian Tzolov
   70 |  * @author Jewoo Shin
   71 |  */
   72 | public final class StructuredOutputValidationAdvisor implements CallAdvisor, StreamAdvisor {
   73 | 
   74 | 	private static final Log logger = LogFactory.getLog(StructuredOutputValidationAdvisor.class);
   75 | 
   76 | 	private final int advisorOrder;
   77 | 
   78 | 	private final Schema jsonSchema;
   79 | 
   80 | 	private final JsonMapper jsonMapper;
   81 | 
   82 | 	private final int maxRepeatAttempts;
   83 | 
   84 | 	private StructuredOutputValidationAdvisor(int advisorOrder, String outputJsonSchema, int maxRepeatAttempts,
   85 | 			JsonMapper jsonMapper) {
   86 | 		Assert.notNull(advisorOrder, "advisorOrder must not be null");
   87 | 		Assert.notNull(outputJsonSchema, "outputJsonSchema must not be null");
   88 | 		Assert.isTrue(advisorOrder > BaseAdvisor.HIGHEST_PRECEDENCE && advisorOrder < BaseAdvisor.LOWEST_PRECEDENCE,
   89 | 				"advisorOrder must be between HIGHEST_PRECEDENCE and LOWEST_PRECEDENCE");
   90 | 		Assert.isTrue(maxRepeatAttempts >= 0, "repeatAttempts must be greater than or equal to 0");
   91 | 		Assert.notNull(jsonMapper, "jsonMapper must not be null");
   92 | 
   93 | 		this.advisorOrder = advisorOrder;
   94 | 		this.jsonMapper = jsonMapper;
   95 | 
   96 | 		if (logger.isDebugEnabled()) {
   97 | 			logger.debug("Generated JSON Schema:\n" + outputJsonSchema);
   98 | 		}
   99 | 
  100 | 		JsonNode schemaNode;
  101 | 		try {
  102 | 			schemaNode = jsonMapper.readTree(outputJsonSchema);
  103 | 		}
  104 | 		catch (Exception e) {
  105 | 			throw new IllegalArgumentException("Failed to parse JSON schema", e);
  106 | 		}
  107 | 
  108 | 		SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
  109 | 		this.jsonSchema = schemaRegistry.getSchema(schemaNode);
  110 | 
  111 | 		this.maxRepeatAttempts = maxRepeatAttempts;
  112 | 	}
  113 | 
  114 | 	@SuppressWarnings("null")
  115 | 	@Override
  116 | 	public String getName() {
  117 | 		return "Structured Output Validation Advisor";
  118 | 	}
  119 | 
  120 | 	@Override
  121 | 	public int getOrder() {
  122 | 		return this.advisorOrder;
  123 | 	}
  124 | 
  125 | 	@SuppressWarnings("null")
  126 | 	@Override
  127 | 	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
  128 | 		Assert.notNull(callAdvisorChain, "callAdvisorChain must not be null");
  129 | 		Assert.notNull(chatClientRequest, "chatClientRequest must not be null");
  130 | 
  131 | 		ChatClientResponse chatClientResponse = null;
  132 | 
  133 | 		boolean isValidationSuccess = false;
  134 | 
  135 | 		var processedChatClientRequest = chatClientRequest;
  136 | 
  137 | 		UsageAccumulator usageAccumulator = new UsageAccumulator();
  138 | 
  139 | 		for (var currentAttemptNumber = 1 + this.maxRepeatAttempts; currentAttemptNumber > 0
  140 | 				&& !isValidationSuccess; currentAttemptNumber--) {
  141 | 			// Next Call
  142 | 			chatClientResponse = callAdvisorChain.copy(this).nextCall(processedChatClientRequest);
  143 | 
  144 | 			// After Call
  145 | 			ChatResponse chatResponse = chatClientResponse.chatResponse();
  146 | 			usageAccumulator.addRoundResponse(chatResponse);
  147 | 
  148 | 			if (GuardrailRefusal.isRefusal(chatResponse)) {
  149 | 				// A guardrail refused the turn. Its refusal is not the model's output, so
  150 | 				// there is nothing to validate, and asking again would be refused again.
  151 | 				break;
  152 | 			}
  153 | 
  154 | 			// We should not validate tool call requests, only the content of the final
  155 | 			// response.
  156 | 			if (chatResponse == null || !chatResponse.hasToolCalls()) {
  157 | 				SchemaValidation validationResponse = validateOutputSchema(chatClientResponse,
  158 | 						currentAttemptNumber - 1);
  159 | 
  160 | 				isValidationSuccess = validationResponse.success();
  161 | 
  162 | 				if (!isValidationSuccess && isContentFiltered(chatResponse)) {
  163 | 					// A safety filter or a refusal of the model stopped the response.
  164 | 					// The validation error is not why it is invalid, so asking again
  165 | 					// with that error would only repeat the stop.
  166 | 					break;
  167 | 				}
  168 | 
  169 | 				if (!isValidationSuccess) {
  170 | 
  171 | 					// Add the validation error message to the next user message
  172 | 					// to let the LLM fix its output.
  173 | 					// Note: We could also consider adding the previous invalid output.
  174 | 					// However, this might lead to confusion and more complex prompts.
  175 | 					// Instead, we rely on the LLM to generate a new output based on the
  176 | 					// validation error.
  177 | 					if (logger.isWarnEnabled()) {
  178 | 						logger.warn("JSON validation failed: " + validationResponse);
  179 | 					}
  180 | 
  181 | 					String validationErrorMessage = "Output JSON validation failed because of: "
  182 | 							+ validationResponse.errorMessage();
  183 | 
  184 | 					Prompt augmentedPrompt = chatClientRequest.prompt()
  185 | 						.augmentUserMessage(userMessage -> userMessage.mutate()
  186 | 							.text(userMessage.getText() + System.lineSeparator() + validationErrorMessage)
  187 | 							.build());
  188 | 
  189 | 					processedChatClientRequest = chatClientRequest.mutate().prompt(augmentedPrompt).build();
  190 | 				}
  191 | 				else if (logger.isDebugEnabled()) {
  192 | 					logger.debug("JSON validation succeeded");
  193 | 				}
  194 | 			}
  195 | 			else {
  196 | 				// A tool call round has nothing to validate. Return it to the caller
  197 | 				// (e.g. the tool calling advisor) instead of repeating the same request.
  198 | 				break;
  199 | 			}
  200 | 		}
  201 | 
  202 | 		return usageAccumulator.applyAccumulatedUsage(Objects.requireNonNull(chatClientResponse));
  203 | 	}
  204 | 
  205 | 	/**
  206 | 	 * Whether a safety filter or a refusal of the model stopped the generation that is
  207 | 	 * validated, which is the first.
  208 | 	 */
  209 | 	private static boolean isContentFiltered(@Nullable ChatResponse chatResponse) {
  210 | 		Generation result = (chatResponse != null) ? chatResponse.getResult() : null;
  211 | 		return result != null && result.getMetadata().getFinishReasonCategory() == FinishReasonCategory.FILTERED;
  212 | 	}
  213 | 
  214 | 	private SchemaValidation validateOutputSchema(ChatClientResponse chatClientResponse, int leftAttemptsCounter) {
  215 | 
  216 | 		if (chatClientResponse.chatResponse() == null || chatClientResponse.chatResponse().getResult() == null
  217 | 				|| chatClientResponse.chatResponse().getResult().getOutput().getText() == null) {
  218 | 
  219 | 			logger.warn("ChatClientResponse is missing required json output for validation.");
  220 | 			return SchemaValidation.failed("Missing required json output for validation.");
  221 | 		}
  222 | 
  223 | 		// TODO: should we consider validation for multiple results?
  224 | 		String json = chatClientResponse.chatResponse().getResult().getOutput().getText();
  225 | 
  226 | 		if (logger.isDebugEnabled()) {
  227 | 			logger.debug("Validating JSON output against schema. Attempts left: " + leftAttemptsCounter);
  228 | 		}
  229 | 
  230 | 		return validateJsonText(json);
  231 | 	}
  232 | 
  233 | 	private SchemaValidation validateJsonText(String json) {
  234 | 		if (json.isBlank()) {
  235 | 			return SchemaValidation.failed("Empty JSON output for validation.");
  236 | 		}
  237 | 		try {
  238 | 			JsonNode instance = this.jsonMapper.readTree(json);
  239 | 			List<Error> errors = this.jsonSchema.validate(instance);
  240 | 			if (errors.isEmpty()) {
  241 | 				return SchemaValidation.passed();
  242 | 			}
  243 | 			String message = errors.stream().map(Error::getMessage).collect(Collectors.joining("; "));
  244 | 			return SchemaValidation.failed(message);
  245 | 		}
  246 | 		catch (JacksonException e) {
  247 | 			return SchemaValidation.failed("Invalid JSON: " + e.getOriginalMessage());
  248 | 		}
  249 | 	}
  250 | 
  251 | 	@SuppressWarnings("null")
  252 | 	@Override
  253 | 	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
  254 | 			StreamAdvisorChain streamAdvisorChain) {
  255 | 
  256 | 		return Flux.error(new UnsupportedOperationException(
  257 | 				"The Structured Output Validation Advisor does not support streaming."));
  258 | 	}
  259 | 
  260 | 	/**
  261 | 	 * Returns a new {@link Builder}.
  262 | 	 * @return a new builder instance
  263 | 	 */
  264 | 	public static Builder builder() {
  265 | 		return new Builder();
  266 | 	}
  267 | 
  268 | 	/**
  269 | 	 * Builder for {@link StructuredOutputValidationAdvisor}.
  270 | 	 */
  271 | 	public final static class Builder {
  272 | 
  273 | 		private int advisorOrder = BaseAdvisor.LOWEST_PRECEDENCE - 2000;
  274 | 
  275 | 		private @Nullable Type outputType;
  276 | 
  277 | 		private int maxRepeatAttempts = 3;
  278 | 
  279 | 		private JsonMapper jsonMapper = JacksonUtils.getDefaultJsonMapper();
  280 | 
  281 | 		private @Nullable String outputJsonSchema;
  282 | 
  283 | 		private Builder() {
  284 | 		}
  285 | 
  286 | 		/**
  287 | 		 * Sets the advisor order. Must be strictly between
  288 | 		 * {@link BaseAdvisor#HIGHEST_PRECEDENCE} and
  289 | 		 * {@link BaseAdvisor#LOWEST_PRECEDENCE}. Defaults to
  290 | 		 * {@code LOWEST_PRECEDENCE - 2000}.
  291 | 		 * @param advisorOrder the advisor order
  292 | 		 * @return this builder
  293 | 		 */
  294 | 		public Builder advisorOrder(int advisorOrder) {
  295 | 			this.advisorOrder = advisorOrder;
  296 | 			return this;
  297 | 		}
  298 | 
  299 | 		/**
  300 | 		 * Sets the expected output type; the JSON schema is derived automatically.
  301 | 		 * Mutually exclusive with {@link #outputJsonSchema(String)}.
  302 | 		 * @param outputType the expected output type
  303 | 		 * @return this builder
  304 | 		 */
  305 | 		public Builder outputType(Type outputType) {
  306 | 			this.outputType = outputType;
  307 | 			return this;
  308 | 		}
  309 | 
  310 | 		/**
  311 | 		 * Sets the expected output type; the JSON schema is derived automatically.
  312 | 		 * Mutually exclusive with {@link #outputJsonSchema(String)}.
  313 | 		 * @param <T> the type parameter
  314 | 		 * @param outputType the expected output type
  315 | 		 * @return this builder
  316 | 		 */
  317 | 		public <T> Builder outputType(TypeReference<T> outputType) {
  318 | 			this.outputType = outputType.getType();
  319 | 			return this;
  320 | 		}
  321 | 
  322 | 		/**
  323 | 		 * Sets the expected output type; the JSON schema is derived automatically.
  324 | 		 * Mutually exclusive with {@link #outputJsonSchema(String)}.
  325 | 		 * @param <T> the type parameter
  326 | 		 * @param outputType the expected output type
  327 | 		 * @return this builder
  328 | 		 */
  329 | 		public <T> Builder outputType(ParameterizedTypeReference<T> outputType) {
  330 | 			this.outputType = outputType.getType();
  331 | 			return this;
  332 | 		}
  333 | 
  334 | 		/**
  335 | 		 * Sets a pre-generated JSON schema string to validate against. Mutually exclusive
  336 | 		 * with the {@code outputType} methods.
  337 | 		 * @param outputJsonSchema the JSON schema as a string
  338 | 		 * @return this builder
  339 | 		 */
  340 | 		public Builder outputJsonSchema(String outputJsonSchema) {
  341 | 			this.outputJsonSchema = outputJsonSchema;
  342 | 			return this;
  343 | 		}
  344 | 
  345 | 		/**
  346 | 		 * Sets the maximum number of retry attempts after a validation failure. Zero
  347 | 		 * means no retries; the model is called exactly once. Defaults to 3.
  348 | 		 * @param repeatAttempts the number of retry attempts, must be &gt;= 0
  349 | 		 * @return this builder
  350 | 		 */
  351 | 		public Builder maxRepeatAttempts(int repeatAttempts) {
  352 | 			this.maxRepeatAttempts = repeatAttempts;
  353 | 			return this;
  354 | 		}
  355 | 
  356 | 		/**
  357 | 		 * Sets the {@link JsonMapper} used for JSON parsing and validation. Defaults to
  358 | 		 * {@link JacksonUtils#getDefaultJsonMapper()}.
  359 | 		 * @param jsonMapper the JSON mapper
  360 | 		 * @return this builder
  361 | 		 */
  362 | 		public Builder jsonMapper(JsonMapper jsonMapper) {
  363 | 			this.jsonMapper = jsonMapper;
  364 | 			return this;
  365 | 		}
  366 | 
  367 | 		/**
  368 | 		 * Builds the StructuredOutputValidationAdvisor.
  369 | 		 * @return a new StructuredOutputValidationAdvisor instance
  370 | 		 * @throws IllegalArgumentException if neither outputType nor outputJsonSchema is
  371 | 		 * set, or if both are set
  372 | 		 */
  373 | 		public StructuredOutputValidationAdvisor build() {
  374 | 
  375 | 			if (StringUtils.hasText(this.outputJsonSchema) && this.outputType != null) {
  376 | 				throw new IllegalArgumentException("Only outputType or outputJsonSchema can be set, not both.");
  377 | 			}
  378 | 
  379 | 			if (!StringUtils.hasText(this.outputJsonSchema) && this.outputType == null) {
  380 | 				throw new IllegalArgumentException("Either outputType or outputJsonSchema must be set.");
  381 | 			}
  382 | 
  383 | 			if (this.outputType != null) {
  384 | 				this.outputJsonSchema = JsonSchemaGenerator.generateForType(this.outputType);
  385 | 			}
  386 | 
  387 | 			return new StructuredOutputValidationAdvisor(this.advisorOrder,
  388 | 					Objects.requireNonNull(this.outputJsonSchema), this.maxRepeatAttempts, this.jsonMapper);
  389 | 		}
  390 | 
  391 | 	}
  392 | 
  393 | 	private record SchemaValidation(boolean success, String errorMessage) {
  394 | 
  395 | 		private static SchemaValidation passed() {
  396 | 			return new SchemaValidation(true, "");
  397 | 		}
  398 | 
  399 | 		private static SchemaValidation failed(String errorMessage) {
  400 | 			return new SchemaValidation(false, errorMessage);
  401 | 		}
  402 | 
  403 | 	}
  404 | 
  405 | }
```
