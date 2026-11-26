<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/embedding/EmbeddingModel.java -->
<!-- 正文引用：[[源码:EmbeddingModel.java#L起-L止]] ；代码块头部：(src: EmbeddingModel.java#L起-L止) -->

# EmbeddingModel.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/embedding/EmbeddingModel.java` |
| 行数 | 141 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:04.855Z |

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
   17 | package org.springframework.ai.embedding;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.ai.document.Document;
   25 | import org.springframework.ai.model.Model;
   26 | import org.springframework.util.Assert;
   27 | 
   28 | /**
   29 |  * EmbeddingModel is a generic interface for embedding models.
   30 |  *
   31 |  * @author Mark Pollack
   32 |  * @author Christian Tzolov
   33 |  * @author Josh Long
   34 |  * @author Soby Chacko
   35 |  * @author Jihoon Kim
   36 |  * @since 1.0.0
   37 |  *
   38 |  */
   39 | public interface EmbeddingModel extends Model<EmbeddingRequest, EmbeddingResponse> {
   40 | 
   41 | 	@Override
   42 | 	EmbeddingResponse call(EmbeddingRequest request);
   43 | 
   44 | 	/**
   45 | 	 * Embeds the given text into a vector.
   46 | 	 * @param text the text to embed.
   47 | 	 * @return the embedded vector.
   48 | 	 */
   49 | 	default float[] embed(String text) {
   50 | 		Assert.notNull(text, "Text must not be null");
   51 | 		List<float[]> response = this.embed(List.of(text));
   52 | 		return response.iterator().next();
   53 | 	}
   54 | 
   55 | 	/**
   56 | 	 * Embeds the given document's content into a vector.
   57 | 	 * @param document the document to embed.
   58 | 	 * @return the embedded vector.
   59 | 	 */
   60 | 	float[] embed(Document document);
   61 | 
   62 | 	/**
   63 | 	 * Extracts the text content from a {@link Document} to be used for embedding. By
   64 | 	 * default, returns {@link Document#getText()}. Implementations that support
   65 | 	 * {@link org.springframework.ai.document.MetadataMode} should override this method to
   66 | 	 * return
   67 | 	 * {@link Document#getFormattedContent(org.springframework.ai.document.MetadataMode)}
   68 | 	 * with the appropriate metadata mode, so that metadata is included in the text sent
   69 | 	 * to the embedding API.
   70 | 	 * @param document the document to extract embedding content from.
   71 | 	 * @return the text content to embed.
   72 | 	 */
   73 | 	default @Nullable String getEmbeddingContent(Document document) {
   74 | 		Assert.notNull(document, "Document must not be null");
   75 | 		return document.getText();
   76 | 	}
   77 | 
   78 | 	/**
   79 | 	 * Embeds a batch of texts into vectors.
   80 | 	 * @param texts list of texts to embed.
   81 | 	 * @return list of embedded vectors.
   82 | 	 */
   83 | 	default List<float[]> embed(List<String> texts) {
   84 | 		Assert.notNull(texts, "Texts must not be null");
   85 | 		return this.call(new EmbeddingRequest(texts, EmbeddingOptions.builder().build()))
   86 | 			.getResults()
   87 | 			.stream()
   88 | 			.map(Embedding::getOutput)
   89 | 			.toList();
   90 | 	}
   91 | 
   92 | 	/**
   93 | 	 * Embeds a batch of {@link Document}s into vectors based on a
   94 | 	 * {@link BatchingStrategy}.
   95 | 	 * @param documents list of {@link Document}s.
   96 | 	 * @param options {@link EmbeddingOptions}.
   97 | 	 * @param batchingStrategy {@link BatchingStrategy}.
   98 | 	 * @return a list of float[] that represents the vectors for the incoming
   99 | 	 * {@link Document}s. The returned list is expected to be in the same order of the
  100 | 	 * {@link Document} list.
  101 | 	 */
  102 | 	default List<float[]> embed(List<Document> documents, @Nullable EmbeddingOptions options,
  103 | 			BatchingStrategy batchingStrategy) {
  104 | 		Assert.notNull(documents, "Documents must not be null");
  105 | 		List<float[]> embeddings = new ArrayList<>(documents.size());
  106 | 		List<List<Document>> batch = batchingStrategy.batch(documents);
  107 | 		for (List<Document> subBatch : batch) {
  108 | 			List<String> texts = subBatch.stream().map(this::getEmbeddingContent).toList();
  109 | 			EmbeddingRequest request = new EmbeddingRequest(texts, options);
  110 | 			EmbeddingResponse response = this.call(request);
  111 | 			for (int i = 0; i < subBatch.size(); i++) {
  112 | 				embeddings.add(response.getResults().get(i).getOutput());
  113 | 			}
  114 | 		}
  115 | 		Assert.isTrue(embeddings.size() == documents.size(),
  116 | 				"Embeddings must have the same number as that of the documents");
  117 | 		return embeddings;
  118 | 	}
  119 | 
  120 | 	/**
  121 | 	 * Embeds a batch of texts into vectors and returns the {@link EmbeddingResponse}.
  122 | 	 * @param texts list of texts to embed.
  123 | 	 * @return the embedding response.
  124 | 	 */
  125 | 	default EmbeddingResponse embedForResponse(List<String> texts) {
  126 | 		Assert.notNull(texts, "Texts must not be null");
  127 | 		return this.call(new EmbeddingRequest(texts, EmbeddingOptions.builder().build()));
  128 | 	}
  129 | 
  130 | 	/**
  131 | 	 * Get the number of dimensions of the embedded vectors. Note that by default, this
  132 | 	 * method will call the remote Embedding endpoint to get the dimensions of the
  133 | 	 * embedded vectors. If the dimensions are known ahead of time, it is recommended to
  134 | 	 * override this method.
  135 | 	 * @return the number of dimensions of the embedded vectors.
  136 | 	 */
  137 | 	default int dimensions() {
  138 | 		return embed("Test String").length;
  139 | 	}
  140 | 
  141 | }
```
