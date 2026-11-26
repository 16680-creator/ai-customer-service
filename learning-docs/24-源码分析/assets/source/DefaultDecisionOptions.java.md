<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.ai:spring-ai-model:2.1.0 | path: org/springframework/ai/decision/DefaultDecisionOptions.java -->
<!-- 正文引用：[[源码:DefaultDecisionOptions.java#L起-L止]] ；代码块头部：(src: DefaultDecisionOptions.java#L起-L止) -->

# DefaultDecisionOptions.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.ai:spring-ai-model:2.1.0` |
| 路径 | `org/springframework/ai/decision/DefaultDecisionOptions.java` |
| 行数 | 125 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/ai/spring-ai-model/2.1.0/spring-ai-model-2.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:39:21.600Z |

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
   17 | package org.springframework.ai.decision;
   18 | 
   19 | import java.util.Objects;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | /**
   24 |  * Default implementation of {@link DecisionOptions}.
   25 |  *
   26 |  * @author Thomas Vitale
   27 |  * @author Christian Tzolov
   28 |  * @since 2.1.0
   29 |  */
   30 | public class DefaultDecisionOptions implements DecisionOptions {
   31 | 
   32 | 	private final @Nullable String model;
   33 | 
   34 | 	protected DefaultDecisionOptions(@Nullable String model) {
   35 | 		this.model = model;
   36 | 	}
   37 | 
   38 | 	@Override
   39 | 	public @Nullable String getModel() {
   40 | 		return this.model;
   41 | 	}
   42 | 
   43 | 	@Override
   44 | 	public DecisionOptions.Builder<?> mutate() {
   45 | 		return DecisionOptions.builder().model(this.model);
   46 | 	}
   47 | 
   48 | 	@Override
   49 | 	public boolean equals(@Nullable Object o) {
   50 | 		if (this == o) {
   51 | 			return true;
   52 | 		}
   53 | 		if (o == null || getClass() != o.getClass()) {
   54 | 			return false;
   55 | 		}
   56 | 		DefaultDecisionOptions that = (DefaultDecisionOptions) o;
   57 | 		return Objects.equals(this.model, that.model);
   58 | 	}
   59 | 
   60 | 	@Override
   61 | 	public int hashCode() {
   62 | 		return Objects.hashCode(this.model);
   63 | 	}
   64 | 
   65 | 	@Override
   66 | 	public String toString() {
   67 | 		return "DefaultDecisionOptions{model='" + this.model + "'}";
   68 | 	}
   69 | 
   70 | 	/**
   71 | 	 * Default builder. Provider builders extend it with their own fields and override
   72 | 	 * {@link #combineWith(DecisionOptions.Builder)} to merge them.
   73 | 	 */
   74 | 	public static class Builder<B extends Builder<B>> implements DecisionOptions.Builder<B> {
   75 | 
   76 | 		protected @Nullable String model;
   77 | 
   78 | 		public Builder() {
   79 | 		}
   80 | 
   81 | 		@Override
   82 | 		@SuppressWarnings("unchecked")
   83 | 		public B clone() {
   84 | 			try {
   85 | 				return (B) super.clone();
   86 | 			}
   87 | 			catch (CloneNotSupportedException ex) {
   88 | 				throw new IllegalStateException(ex);
   89 | 			}
   90 | 		}
   91 | 
   92 | 		@SuppressWarnings("unchecked")
   93 | 		protected B self() {
   94 | 			return (B) this;
   95 | 		}
   96 | 
   97 | 		@Override
   98 | 		public B model(@Nullable String model) {
   99 | 			this.model = model;
  100 | 			return self();
  101 | 		}
  102 | 
  103 | 		/**
  104 | 		 * {@inheritDoc}
  105 | 		 * <p>
  106 | 		 * A builder of another implementation is read through the options it builds, so a
  107 | 		 * model its {@code build()} fills in counts as set.
  108 | 		 */
  109 | 		@Override
  110 | 		public B combineWith(DecisionOptions.Builder<?> other) {
  111 | 			String otherModel = other instanceof Builder<?> that ? that.model : other.build().getModel();
  112 | 			if (otherModel != null) {
  113 | 				this.model = otherModel;
  114 | 			}
  115 | 			return self();
  116 | 		}
  117 | 
  118 | 		@Override
  119 | 		public DecisionOptions build() {
  120 | 			return new DefaultDecisionOptions(this.model);
  121 | 		}
  122 | 
  123 | 	}
  124 | 
  125 | }
```
