<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-vector-store:2.1.0 | path: org/springframework/ai/vectorstore/AbstractVectorStoreBuilder.java -->
<!-- 正文引用：[[源码:AbstractVectorStoreBuilder.java#L起-L止]] ；代码块头部：(src: AbstractVectorStoreBuilder.java#L起-L止) -->

# AbstractVectorStoreBuilder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-vector-store:2.1.0` |
| 路径 | `org/springframework/ai/vectorstore/AbstractVectorStoreBuilder.java` |
| 行数 | 101 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-vector-store/2.1.0/spring-ai-vector-store-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:20.482Z |

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
   17 | package org.springframework.ai.vectorstore;
   18 | 
   19 | import io.micrometer.observation.ObservationRegistry;
   20 | import org.jspecify.annotations.Nullable;
   21 | 
   22 | import org.springframework.ai.embedding.BatchingStrategy;
   23 | import org.springframework.ai.embedding.EmbeddingModel;
   24 | import org.springframework.ai.embedding.TokenCountBatchingStrategy;
   25 | import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
   26 | import org.springframework.util.Assert;
   27 | 
   28 | /**
   29 |  * Abstract base builder implementing common builder functionality for
   30 |  * {@link VectorStore}. Provides default implementations for observation-related settings.
   31 |  *
   32 |  * @param <T> the concrete builder type, enabling method chaining with the correct return
   33 |  * type
   34 |  */
   35 | public abstract class AbstractVectorStoreBuilder<T extends AbstractVectorStoreBuilder<T>>
   36 | 		implements VectorStore.Builder<T> {
   37 | 
   38 | 	protected final EmbeddingModel embeddingModel;
   39 | 
   40 | 	protected ObservationRegistry observationRegistry = ObservationRegistry.NOOP;
   41 | 
   42 | 	protected @Nullable VectorStoreObservationConvention customObservationConvention;
   43 | 
   44 | 	protected BatchingStrategy batchingStrategy = new TokenCountBatchingStrategy();
   45 | 
   46 | 	public AbstractVectorStoreBuilder(EmbeddingModel embeddingModel) {
   47 | 		Assert.notNull(embeddingModel, "EmbeddingModel must be configured");
   48 | 		this.embeddingModel = embeddingModel;
   49 | 	}
   50 | 
   51 | 	public EmbeddingModel getEmbeddingModel() {
   52 | 		return this.embeddingModel;
   53 | 	}
   54 | 
   55 | 	public BatchingStrategy getBatchingStrategy() {
   56 | 		return this.batchingStrategy;
   57 | 	}
   58 | 
   59 | 	public ObservationRegistry getObservationRegistry() {
   60 | 		return this.observationRegistry;
   61 | 	}
   62 | 
   63 | 	public @Nullable VectorStoreObservationConvention getCustomObservationConvention() {
   64 | 		return this.customObservationConvention;
   65 | 	}
   66 | 
   67 | 	/**
   68 | 	 * Returns this builder cast to the concrete builder type. Used internally to enable
   69 | 	 * proper method chaining in subclasses.
   70 | 	 * @return this builder cast to the concrete type
   71 | 	 */
   72 | 	@SuppressWarnings("unchecked")
   73 | 	protected T self() {
   74 | 		return (T) this;
   75 | 	}
   76 | 
   77 | 	@Override
   78 | 	public T observationRegistry(ObservationRegistry observationRegistry) {
   79 | 		Assert.notNull(observationRegistry, "ObservationRegistry must not be null");
   80 | 		this.observationRegistry = observationRegistry;
   81 | 		return self();
   82 | 	}
   83 | 
   84 | 	@Override
   85 | 	public T customObservationConvention(@Nullable VectorStoreObservationConvention convention) {
   86 | 		this.customObservationConvention = convention;
   87 | 		return self();
   88 | 	}
   89 | 
   90 | 	/**
   91 | 	 * Sets the batching strategy.
   92 | 	 * @param batchingStrategy the strategy to use
   93 | 	 * @return the builder instance
   94 | 	 */
   95 | 	public T batchingStrategy(BatchingStrategy batchingStrategy) {
   96 | 		Assert.notNull(batchingStrategy, "BatchingStrategy must not be null");
   97 | 		this.batchingStrategy = batchingStrategy;
   98 | 		return self();
   99 | 	}
  100 | 
  101 | }
```
