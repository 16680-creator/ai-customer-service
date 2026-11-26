<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/tool/execution/DefaultToolCallResultConverter.java -->
<!-- 正文引用：[[源码:DefaultToolCallResultConverter.java#L起-L止]] ；代码块头部：(src: DefaultToolCallResultConverter.java#L起-L止) -->

# DefaultToolCallResultConverter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/tool/execution/DefaultToolCallResultConverter.java` |
| 行数 | 69 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:27.296Z |

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
   17 | package org.springframework.ai.tool.execution;
   18 | 
   19 | import java.awt.image.RenderedImage;
   20 | import java.io.ByteArrayOutputStream;
   21 | import java.io.IOException;
   22 | import java.lang.reflect.Type;
   23 | import java.util.Base64;
   24 | import java.util.Map;
   25 | 
   26 | import javax.imageio.ImageIO;
   27 | 
   28 | import org.apache.commons.logging.Log;
   29 | import org.apache.commons.logging.LogFactory;
   30 | import org.jspecify.annotations.Nullable;
   31 | 
   32 | import org.springframework.ai.util.JsonHelper;
   33 | 
   34 | /**
   35 |  * A default implementation of {@link ToolCallResultConverter}.
   36 |  *
   37 |  * @author Thomas Vitale
   38 |  * @since 1.0.0
   39 |  */
   40 | public final class DefaultToolCallResultConverter implements ToolCallResultConverter {
   41 | 
   42 | 	private static final JsonHelper jsonHelper = new JsonHelper();
   43 | 
   44 | 	private static final Log logger = LogFactory.getLog(DefaultToolCallResultConverter.class);
   45 | 
   46 | 	@Override
   47 | 	public String convert(@Nullable Object result, @Nullable Type returnType) {
   48 | 		if (returnType == Void.TYPE) {
   49 | 			logger.debug("The tool has no return type. Converting to conventional response.");
   50 | 			return jsonHelper.toJson("Done");
   51 | 		}
   52 | 		if (result instanceof RenderedImage) {
   53 | 			final var buf = new ByteArrayOutputStream(1024 * 4);
   54 | 			try {
   55 | 				ImageIO.write((RenderedImage) result, "PNG", buf);
   56 | 			}
   57 | 			catch (IOException e) {
   58 | 				return "Failed to convert tool result to a base64 image: " + e.getMessage();
   59 | 			}
   60 | 			final var imgB64 = Base64.getEncoder().encodeToString(buf.toByteArray());
   61 | 			return jsonHelper.toJson(Map.of("mimeType", "image/png", "data", imgB64));
   62 | 		}
   63 | 		else {
   64 | 			logger.debug("Converting tool result to JSON.");
   65 | 			return jsonHelper.toJson(result, true);
   66 | 		}
   67 | 	}
   68 | 
   69 | }
```
