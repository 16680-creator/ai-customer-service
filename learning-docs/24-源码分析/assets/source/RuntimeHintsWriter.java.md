<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-core:7.1.0 | path: org/springframework/aot/nativex/RuntimeHintsWriter.java -->
<!-- 正文引用：[[源码:RuntimeHintsWriter.java#L起-L止]] ；代码块头部：(src: RuntimeHintsWriter.java#L起-L止) -->

# RuntimeHintsWriter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-core:7.1.0` |
| 路径 | `org/springframework/aot/nativex/RuntimeHintsWriter.java` |
| 行数 | 59 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-core/7.1.0/spring-core-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:28.656Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
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
   17 | package org.springframework.aot.nativex;
   18 | 
   19 | import java.util.LinkedHashMap;
   20 | import java.util.List;
   21 | import java.util.Map;
   22 | 
   23 | import org.springframework.aot.hint.RuntimeHints;
   24 | import org.springframework.core.SpringVersion;
   25 | 
   26 | /**
   27 |  * Write a {@link RuntimeHints} instance to the JSON output expected by the
   28 |  * GraalVM {@code native-image} compiler, typically named {@code reachability-metadata.json}.
   29 |  *
   30 |  * @author Brian Clozel
   31 |  * @author Stephane Nicoll
   32 |  * @since 7.0
   33 |  * @see <a href="https://www.graalvm.org/jdk25/reference-manual/native-image/metadata">GraalVM Reachability Metadata</a>
   34 |  */
   35 | class RuntimeHintsWriter {
   36 | 
   37 | 	public void write(BasicJsonWriter writer, RuntimeHints hints) {
   38 | 		Map<String, Object> document = new LinkedHashMap<>();
   39 | 		String springVersion = SpringVersion.getVersion();
   40 | 		if (springVersion != null) {
   41 | 			document.put("comment", "Spring Framework " + springVersion);
   42 | 		}
   43 | 		List<Map<String, Object>> reflection = new ReflectionHintsAttributes().reflection(hints);
   44 | 		if (!reflection.isEmpty()) {
   45 | 			document.put("reflection", reflection);
   46 | 		}
   47 | 		List<Map<String, Object>> jni = new ReflectionHintsAttributes().jni(hints);
   48 | 		if (!jni.isEmpty()) {
   49 | 			document.put("jni", jni);
   50 | 		}
   51 | 		List<Map<String, Object>> resourceHints = new ResourceHintsAttributes().resources(hints.resources());
   52 | 		if (!resourceHints.isEmpty()) {
   53 | 			document.put("resources", resourceHints);
   54 | 		}
   55 | 
   56 | 		writer.writeObject(document);
   57 | 	}
   58 | 
   59 | }
```
