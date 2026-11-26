<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-core:7.1.0 | path: org/springframework/aot/nativex/NativeConfigurationWriter.java -->
<!-- 正文引用：[[源码:NativeConfigurationWriter.java#L起-L止]] ；代码块头部：(src: NativeConfigurationWriter.java#L起-L止) -->

# NativeConfigurationWriter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-core:7.1.0` |
| 路径 | `org/springframework/aot/nativex/NativeConfigurationWriter.java` |
| 行数 | 69 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-core/7.1.0/spring-core-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:32:36.845Z |

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
   19 | import java.util.function.Consumer;
   20 | 
   21 | import org.springframework.aot.hint.RuntimeHints;
   22 | 
   23 | /**
   24 |  * Write {@link RuntimeHints} as GraalVM native configuration.
   25 |  *
   26 |  * @author Sebastien Deleuze
   27 |  * @author Stephane Nicoll
   28 |  * @author Janne Valkealahti
   29 |  * @author Brian Clozel
   30 |  * @since 6.0
   31 |  * @see <a href="https://www.graalvm.org/jdk23/reference-manual/native-image/overview/BuildConfiguration/">Native Image Build Configuration</a>
   32 |  */
   33 | public abstract class NativeConfigurationWriter {
   34 | 
   35 | 	/**
   36 | 	 * Write the GraalVM native configuration from the provided hints.
   37 | 	 * @param hints the hints to handle
   38 | 	 */
   39 | 	public void write(RuntimeHints hints) {
   40 | 		if (hasAnyHint(hints)) {
   41 | 			writeTo("reachability-metadata.json",
   42 | 					writer -> new RuntimeHintsWriter().write(writer, hints));
   43 | 		}
   44 | 	}
   45 | 
   46 | 	private boolean hasAnyHint(RuntimeHints hints) {
   47 | 		return (hints.proxies().jdkProxyHints().findAny().isPresent() ||
   48 | 				hints.reflection().typeHints().findAny().isPresent() ||
   49 | 				hints.reflection().lambdaHints().findAny().isPresent() ||
   50 | 				hints.resources().resourcePatternHints().findAny().isPresent() ||
   51 | 				hints.resources().resourceBundleHints().findAny().isPresent() ||
   52 | 				hints.jni().typeHints().findAny().isPresent() ||
   53 | 				hasAnyDeprecatedHint(hints));
   54 | 	}
   55 | 
   56 | 	@SuppressWarnings("removal")
   57 | 	private boolean hasAnyDeprecatedHint(RuntimeHints hints) {
   58 | 		return hints.serialization().javaSerializationHints().findAny().isPresent();
   59 | 	}
   60 | 
   61 | 	/**
   62 | 	 * Write the specified GraalVM native configuration file, using the
   63 | 	 * provided {@link BasicJsonWriter}.
   64 | 	 * @param fileName the name of the file
   65 | 	 * @param writer a consumer for the writer to use
   66 | 	 */
   67 | 	protected abstract void writeTo(String fileName, Consumer<BasicJsonWriter> writer);
   68 | 
   69 | }
```
