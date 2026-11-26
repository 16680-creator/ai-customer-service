<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-core:7.1.0 | path: org/springframework/aot/nativex/FileNativeConfigurationWriter.java -->
<!-- 正文引用：[[源码:FileNativeConfigurationWriter.java#L起-L止]] ；代码块头部：(src: FileNativeConfigurationWriter.java#L起-L止) -->

# FileNativeConfigurationWriter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-core:7.1.0` |
| 路径 | `org/springframework/aot/nativex/FileNativeConfigurationWriter.java` |
| 行数 | 87 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-core/7.1.0/spring-core-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:28.845Z |

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
   19 | import java.io.File;
   20 | import java.io.FileWriter;
   21 | import java.io.IOException;
   22 | import java.io.Writer;
   23 | import java.nio.charset.StandardCharsets;
   24 | import java.nio.file.Path;
   25 | import java.util.function.Consumer;
   26 | 
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | /**
   30 |  * A {@link NativeConfigurationWriter} implementation that writes the
   31 |  * configuration to disk.
   32 |  *
   33 |  * @author Sebastien Deleuze
   34 |  * @author Stephane Nicoll
   35 |  * @since 6.0
   36 |  * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
   37 |  */
   38 | public class FileNativeConfigurationWriter extends NativeConfigurationWriter {
   39 | 
   40 | 	private final Path basePath;
   41 | 
   42 | 	private final @Nullable String groupId;
   43 | 
   44 | 	private final @Nullable String artifactId;
   45 | 
   46 | 	public FileNativeConfigurationWriter(Path basePath) {
   47 | 		this(basePath, null, null);
   48 | 	}
   49 | 
   50 | 	public FileNativeConfigurationWriter(Path basePath, @Nullable String groupId, @Nullable String artifactId) {
   51 | 		this.basePath = basePath;
   52 | 		if ((groupId == null && artifactId != null) || (groupId != null && artifactId == null)) {
   53 | 			throw new IllegalArgumentException("groupId and artifactId must be both null or both non-null");
   54 | 		}
   55 | 		this.groupId = groupId;
   56 | 		this.artifactId = artifactId;
   57 | 	}
   58 | 
   59 | 	@Override
   60 | 	protected void writeTo(String fileName, Consumer<BasicJsonWriter> writer) {
   61 | 		try {
   62 | 			File file = createIfNecessary(fileName);
   63 | 			try (FileWriter out = new FileWriter(file, StandardCharsets.UTF_8)) {
   64 | 				writer.accept(createJsonWriter(out));
   65 | 			}
   66 | 		}
   67 | 		catch (IOException ex) {
   68 | 			throw new IllegalStateException("Failed to write native configuration for " + fileName, ex);
   69 | 		}
   70 | 	}
   71 | 
   72 | 	private File createIfNecessary(String filename) throws IOException {
   73 | 		Path outputDirectory = this.basePath.resolve("META-INF").resolve("native-image");
   74 | 		if (this.groupId != null && this.artifactId != null) {
   75 | 			outputDirectory = outputDirectory.resolve(this.groupId).resolve(this.artifactId);
   76 | 		}
   77 | 		outputDirectory.toFile().mkdirs();
   78 | 		File file = outputDirectory.resolve(filename).toFile();
   79 | 		file.createNewFile();
   80 | 		return file;
   81 | 	}
   82 | 
   83 | 	private BasicJsonWriter createJsonWriter(Writer out) {
   84 | 		return new BasicJsonWriter(out);
   85 | 	}
   86 | 
   87 | }
```
