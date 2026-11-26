<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/augment/ToolInputSchemaAugmenter.java -->
<!-- 正文引用：[[源码:ToolInputSchemaAugmenter.java#L起-L止]] ；代码块头部：(src: ToolInputSchemaAugmenter.java#L起-L止) -->

# ToolInputSchemaAugmenter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/augment/ToolInputSchemaAugmenter.java` |
| 行数 | 141 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:21.706Z |

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
   17 | package org.springframework.ai.tool.augment;
   18 | 
   19 | import java.lang.reflect.Type;
   20 | import java.util.Arrays;
   21 | import java.util.List;
   22 | 
   23 | import tools.jackson.databind.node.ArrayNode;
   24 | import tools.jackson.databind.node.ObjectNode;
   25 | 
   26 | import org.springframework.ai.tool.annotation.ToolParam;
   27 | import org.springframework.ai.util.JacksonUtils;
   28 | import org.springframework.ai.util.json.schema.JsonSchemaUtils;
   29 | 
   30 | /**
   31 |  * This utility provides functionality to augment a JSON Schema with additional fields
   32 |  * based on a provided Record type. It uses a JSON Schema Generator to generate the schema
   33 |  * for the Record's fields and integrates them into an existing JSON Schema. The augmented
   34 |  * schema can then be used to re-define the tool inputs for tool calling.
   35 |  *
   36 |  * @author Christian Tzolov
   37 |  */
   38 | public final class ToolInputSchemaAugmenter {
   39 | 
   40 | 	private ToolInputSchemaAugmenter() {
   41 | 	}
   42 | 
   43 | 	/**
   44 | 	 * Extracts the tool argument types from a record class annotated with
   45 | 	 * {@link ToolParam}. It retrieves the field names, types, descriptions, and required
   46 | 	 * status from the record components.
   47 | 	 * @param recordClass The record class to extract argument types from.
   48 | 	 * @return A list of {@link AugmentedArgumentType} representing the tool input
   49 | 	 * argument types.
   50 | 	 */
   51 | 	public static <T extends Record> List<AugmentedArgumentType> toAugmentedArgumentTypes(Class<T> recordClass) {
   52 | 		try {
   53 | 
   54 | 			return Arrays.stream(recordClass.getRecordComponents()).map(c -> {
   55 | 				// Get the annotation from the corresponding field, not the record
   56 | 				// component
   57 | 				ToolParam toolParam = null;
   58 | 				try {
   59 | 					var field = recordClass.getDeclaredField(c.getName());
   60 | 					toolParam = field.getAnnotation(ToolParam.class);
   61 | 				}
   62 | 				catch (NoSuchFieldException e) {
   63 | 					// Field not found, toolParam remains null
   64 | 				}
   65 | 
   66 | 				return new AugmentedArgumentType(c.getName(), c.getGenericType(),
   67 | 						toolParam != null ? toolParam.description() : "no description",
   68 | 						toolParam != null ? toolParam.required() : false);
   69 | 			}).toList();
   70 | 
   71 | 		}
   72 | 		catch (Exception e) {
   73 | 			throw new RuntimeException("Failed to extract record field types", e);
   74 | 		}
   75 | 	}
   76 | 
   77 | 	public static String augmentToolInputSchema(String jsonSchemaString, String propertyName, Type propertyType,
   78 | 			String description, boolean required) {
   79 | 
   80 | 		return augmentToolInputSchema(jsonSchemaString,
   81 | 				List.of(new AugmentedArgumentType(propertyName, propertyType, description, required)));
   82 | 	}
   83 | 
   84 | 	public static String augmentToolInputSchema(String jsonSchemaString, List<AugmentedArgumentType> argumentType) {
   85 | 
   86 | 		try {
   87 | 
   88 | 			ObjectNode schemaObjectNode = (ObjectNode) JacksonUtils.getDefaultJsonMapper().readTree(jsonSchemaString);
   89 | 
   90 | 			// Handle properties
   91 | 			ObjectNode propertiesNode;
   92 | 			if (schemaObjectNode.has("properties")) {
   93 | 				propertiesNode = (ObjectNode) schemaObjectNode.get("properties");
   94 | 			}
   95 | 			else {
   96 | 				propertiesNode = JacksonUtils.getDefaultJsonMapper().createObjectNode();
   97 | 				schemaObjectNode.set("properties", propertiesNode);
   98 | 			}
   99 | 
  100 | 			for (AugmentedArgumentType argument : argumentType) {
  101 | 
  102 | 				ObjectNode parameterNode = JsonSchemaUtils.getJsonSchema(argument.type());
  103 | 
  104 | 				if (argument.description() != null && !argument.description().isEmpty()) {
  105 | 					parameterNode.put("description", argument.description());
  106 | 				}
  107 | 				propertiesNode.set(argument.name(), parameterNode);
  108 | 
  109 | 				if (argument.required()) {
  110 | 
  111 | 					ArrayNode requiredArray;
  112 | 					if (schemaObjectNode.has("required")) {
  113 | 						requiredArray = (ArrayNode) schemaObjectNode.get("required");
  114 | 					}
  115 | 					else {
  116 | 						requiredArray = JacksonUtils.getDefaultJsonMapper().createArrayNode();
  117 | 						schemaObjectNode.set("required", requiredArray);
  118 | 					}
  119 | 					requiredArray.add(argument.name());
  120 | 
  121 | 				}
  122 | 			}
  123 | 
  124 | 			return JacksonUtils.getDefaultJsonMapper()
  125 | 				.writerWithDefaultPrettyPrinter()
  126 | 				.writeValueAsString(schemaObjectNode);
  127 | 
  128 | 		}
  129 | 		catch (Exception e) {
  130 | 			throw new RuntimeException("Failed to parse JSON Schema", e);
  131 | 		}
  132 | 	}
  133 | 
  134 | 	/**
  135 | 	 * Represents an extended argument type with additional metadata such as description
  136 | 	 * and required status.
  137 | 	 */
  138 | 	public record AugmentedArgumentType(String name, Type type, String description, boolean required) {
  139 | 	}
  140 | 
  141 | }
```
