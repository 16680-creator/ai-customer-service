<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/aot/AbstractAotProcessor.java -->
<!-- 正文引用：[[源码:AbstractAotProcessor.java#L起-L止]] ；代码块头部：(src: AbstractAotProcessor.java#L起-L止) -->

# AbstractAotProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/aot/AbstractAotProcessor.java` |
| 行数 | 285 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:27.915Z |

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
   17 | package org.springframework.context.aot;
   18 | 
   19 | import java.io.IOException;
   20 | import java.io.UncheckedIOException;
   21 | import java.nio.file.Path;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.aot.generate.FileSystemGeneratedFiles;
   26 | import org.springframework.aot.generate.GeneratedFiles.Kind;
   27 | import org.springframework.aot.hint.RuntimeHints;
   28 | import org.springframework.aot.nativex.FileNativeConfigurationWriter;
   29 | import org.springframework.util.Assert;
   30 | import org.springframework.util.FileSystemUtils;
   31 | 
   32 | /**
   33 |  * Abstract base class for filesystem-based ahead-of-time (AOT) processing.
   34 |  *
   35 |  * <p>Concrete implementations should override {@link #doProcess()} that kicks
   36 |  * off the optimization of the target, usually an application.
   37 |  *
   38 |  * @author Stephane Nicoll
   39 |  * @author Andy Wilkinson
   40 |  * @author Phillip Webb
   41 |  * @author Sam Brannen
   42 |  * @since 6.0
   43 |  * @param <T> the type of the processing result
   44 |  * @see FileSystemGeneratedFiles
   45 |  * @see FileNativeConfigurationWriter
   46 |  * @see org.springframework.context.aot.ContextAotProcessor
   47 |  * @see org.springframework.test.context.aot.TestAotProcessor
   48 |  */
   49 | public abstract class AbstractAotProcessor<T> {
   50 | 
   51 | 	/**
   52 | 	 * The name of a system property that is made available when the processor runs.
   53 | 	 * @since 6.2
   54 | 	 * @see #doProcess()
   55 | 	 */
   56 | 	public static final String AOT_PROCESSING = "spring.aot.processing";
   57 | 
   58 | 	private final Settings settings;
   59 | 
   60 | 
   61 | 	/**
   62 | 	 * Create a new processor instance with the supplied {@linkplain Settings settings}.
   63 | 	 * @see Settings#builder()
   64 | 	 */
   65 | 	protected AbstractAotProcessor(Settings settings) {
   66 | 		this.settings = settings;
   67 | 	}
   68 | 
   69 | 
   70 | 	/**
   71 | 	 * Get the {@linkplain Settings settings} for this AOT processor.
   72 | 	 */
   73 | 	protected Settings getSettings() {
   74 | 		return this.settings;
   75 | 	}
   76 | 
   77 | 	/**
   78 | 	 * Run AOT processing.
   79 | 	 * @return the result of the processing.
   80 | 	 */
   81 | 	public final T process() {
   82 | 		try {
   83 | 			System.setProperty(AOT_PROCESSING, "true");
   84 | 			return doProcess();
   85 | 		}
   86 | 		finally {
   87 | 			System.clearProperty(AOT_PROCESSING);
   88 | 		}
   89 | 	}
   90 | 
   91 | 	protected abstract T doProcess();
   92 | 
   93 | 	/**
   94 | 	 * Delete the source, resource, and class output directories.
   95 | 	 */
   96 | 	protected void deleteExistingOutput() {
   97 | 		deleteExistingOutput(getSettings().getSourceOutput(),
   98 | 				getSettings().getResourceOutput(), getSettings().getClassOutput());
   99 | 	}
  100 | 
  101 | 	private void deleteExistingOutput(Path... paths) {
  102 | 		for (Path path : paths) {
  103 | 			try {
  104 | 				FileSystemUtils.deleteRecursively(path);
  105 | 			}
  106 | 			catch (IOException ex) {
  107 | 				throw new UncheckedIOException("Failed to delete existing output in '" + path + "'", ex);
  108 | 			}
  109 | 		}
  110 | 	}
  111 | 
  112 | 	protected FileSystemGeneratedFiles createFileSystemGeneratedFiles() {
  113 | 		return new FileSystemGeneratedFiles(this::getRoot);
  114 | 	}
  115 | 
  116 | 	private Path getRoot(Kind kind) {
  117 | 		return switch (kind) {
  118 | 			case SOURCE -> getSettings().getSourceOutput();
  119 | 			case RESOURCE -> getSettings().getResourceOutput();
  120 | 			case CLASS -> getSettings().getClassOutput();
  121 | 		};
  122 | 	}
  123 | 
  124 | 	protected void writeHints(RuntimeHints hints) {
  125 | 		FileNativeConfigurationWriter writer = new FileNativeConfigurationWriter(
  126 | 				getSettings().getResourceOutput(), getSettings().getGroupId(), getSettings().getArtifactId());
  127 | 		writer.write(hints);
  128 | 	}
  129 | 
  130 | 
  131 | 	/**
  132 | 	 * Common settings for AOT processors.
  133 | 	 */
  134 | 	public static final class Settings {
  135 | 
  136 | 		private final Path sourceOutput;
  137 | 
  138 | 		private final Path resourceOutput;
  139 | 
  140 | 		private final Path classOutput;
  141 | 
  142 | 		private final String groupId;
  143 | 
  144 | 		private final String artifactId;
  145 | 
  146 | 		private Settings(Path sourceOutput, Path resourceOutput, Path classOutput, String groupId, String artifactId) {
  147 | 			this.sourceOutput = sourceOutput;
  148 | 			this.resourceOutput = resourceOutput;
  149 | 			this.classOutput = classOutput;
  150 | 			this.groupId = groupId;
  151 | 			this.artifactId = artifactId;
  152 | 		}
  153 | 
  154 | 		/**
  155 | 		 * Create a new {@link Builder} for {@link Settings}.
  156 | 		 */
  157 | 		public static Builder builder() {
  158 | 			return new Builder();
  159 | 		}
  160 | 
  161 | 		/**
  162 | 		 * Get the output directory for generated sources.
  163 | 		 */
  164 | 		public Path getSourceOutput() {
  165 | 			return this.sourceOutput;
  166 | 		}
  167 | 
  168 | 		/**
  169 | 		 * Get the output directory for generated resources.
  170 | 		 */
  171 | 		public Path getResourceOutput() {
  172 | 			return this.resourceOutput;
  173 | 		}
  174 | 
  175 | 		/**
  176 | 		 * Get the output directory for generated classes.
  177 | 		 */
  178 | 		public Path getClassOutput() {
  179 | 			return this.classOutput;
  180 | 		}
  181 | 
  182 | 		/**
  183 | 		 * Get the group ID of the application.
  184 | 		 */
  185 | 		public String getGroupId() {
  186 | 			return this.groupId;
  187 | 		}
  188 | 
  189 | 		/**
  190 | 		 * Get the artifact ID of the application.
  191 | 		 */
  192 | 		public String getArtifactId() {
  193 | 			return this.artifactId;
  194 | 		}
  195 | 
  196 | 
  197 | 		/**
  198 | 		 * Fluent builder API for {@link Settings}.
  199 | 		 */
  200 | 		public static final class Builder {
  201 | 
  202 | 			private @Nullable Path sourceOutput;
  203 | 
  204 | 			private @Nullable Path resourceOutput;
  205 | 
  206 | 			private @Nullable Path classOutput;
  207 | 
  208 | 			private @Nullable String groupId;
  209 | 
  210 | 			private @Nullable String artifactId;
  211 | 
  212 | 			private Builder() {
  213 | 				// internal constructor
  214 | 			}
  215 | 
  216 | 			/**
  217 | 			 * Set the output directory for generated sources.
  218 | 			 * @param sourceOutput the location of generated sources
  219 | 			 * @return this builder for method chaining
  220 | 			 */
  221 | 			public Builder sourceOutput(Path sourceOutput) {
  222 | 				this.sourceOutput = sourceOutput;
  223 | 				return this;
  224 | 			}
  225 | 
  226 | 			/**
  227 | 			 * Set the output directory for generated resources.
  228 | 			 * @param resourceOutput the location of generated resources
  229 | 			 * @return this builder for method chaining
  230 | 			 */
  231 | 			public Builder resourceOutput(Path resourceOutput) {
  232 | 				this.resourceOutput = resourceOutput;
  233 | 				return this;
  234 | 			}
  235 | 
  236 | 			/**
  237 | 			 * Set the output directory for generated classes.
  238 | 			 * @param classOutput the location of generated classes
  239 | 			 * @return this builder for method chaining
  240 | 			 */
  241 | 			public Builder classOutput(Path classOutput) {
  242 | 				this.classOutput = classOutput;
  243 | 				return this;
  244 | 			}
  245 | 
  246 | 			/**
  247 | 			 * Set the group ID of the application.
  248 | 			 * @param groupId the group ID of the application, used to locate
  249 | 			 * {@code native-image.properties}
  250 | 			 * @return this builder for method chaining
  251 | 			 */
  252 | 			public Builder groupId(String groupId) {
  253 | 				Assert.hasText(groupId, "'groupId' must not be empty");
  254 | 				this.groupId = groupId;
  255 | 				return this;
  256 | 			}
  257 | 
  258 | 			/**
  259 | 			 * Set the artifact ID of the application.
  260 | 			 * @param artifactId the artifact ID of the application, used to locate
  261 | 			 * {@code native-image.properties}
  262 | 			 * @return this builder for method chaining
  263 | 			 */
  264 | 			public Builder artifactId(String artifactId) {
  265 | 				Assert.hasText(artifactId, "'artifactId' must not be empty");
  266 | 				this.artifactId = artifactId;
  267 | 				return this;
  268 | 			}
  269 | 
  270 | 			/**
  271 | 			 * Build the {@link Settings} configured in this {@code Builder}.
  272 | 			 */
  273 | 			public Settings build() {
  274 | 				Assert.notNull(this.sourceOutput, "'sourceOutput' must not be null");
  275 | 				Assert.notNull(this.resourceOutput, "'resourceOutput' must not be null");
  276 | 				Assert.notNull(this.classOutput, "'classOutput' must not be null");
  277 | 				Assert.notNull(this.groupId, "'groupId' must not be null");
  278 | 				Assert.notNull(this.artifactId, "'artifactId' must not be null");
  279 | 				return new Settings(this.sourceOutput, this.resourceOutput, this.classOutput,
  280 | 						this.groupId, this.artifactId);
  281 | 			}
  282 | 		}
  283 | 	}
  284 | 
  285 | }
```
