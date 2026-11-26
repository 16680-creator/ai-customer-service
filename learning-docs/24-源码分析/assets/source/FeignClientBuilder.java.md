<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/FeignClientBuilder.java -->
<!-- 正文引用：[[源码:FeignClientBuilder.java#L起-L止]] ；代码块头部：(src: FeignClientBuilder.java#L起-L止) -->

# FeignClientBuilder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/FeignClientBuilder.java` |
| 行数 | 130 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:15.543Z |

```java
    1 | /*
    2 |  * Copyright 2013-present the original author or authors.
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
   17 | package org.springframework.cloud.openfeign;
   18 | 
   19 | import feign.Feign;
   20 | 
   21 | import org.springframework.context.ApplicationContext;
   22 | 
   23 | /**
   24 |  * A builder for creating Feign clients without using the {@link FeignClient} annotation.
   25 |  * <p>
   26 |  * This builder builds the Feign client exactly like it would be created by using the
   27 |  * {@link FeignClient} annotation.
   28 |  *
   29 |  * @author Sven Döring
   30 |  * @author Matt King
   31 |  * @author Sam Kruglov
   32 |  * @author Olga Maciaszek-Sharma
   33 |  */
   34 | public class FeignClientBuilder {
   35 | 
   36 | 	private final ApplicationContext applicationContext;
   37 | 
   38 | 	public FeignClientBuilder(final ApplicationContext applicationContext) {
   39 | 		this.applicationContext = applicationContext;
   40 | 	}
   41 | 
   42 | 	public <T> Builder<T> forType(final Class<T> type, final String name) {
   43 | 		return new Builder<>(this.applicationContext, type, name);
   44 | 	}
   45 | 
   46 | 	public <T> Builder<T> forType(final Class<T> type, final FeignClientFactoryBean clientFactoryBean,
   47 | 			final String name) {
   48 | 		return new Builder<>(this.applicationContext, clientFactoryBean, type, name);
   49 | 	}
   50 | 
   51 | 	/**
   52 | 	 * Builder of feign targets.
   53 | 	 *
   54 | 	 * @param <T> type of target
   55 | 	 */
   56 | 	public static final class Builder<T> {
   57 | 
   58 | 		private final FeignClientFactoryBean feignClientFactoryBean;
   59 | 
   60 | 		private Builder(final ApplicationContext applicationContext, final Class<T> type, final String name) {
   61 | 			this(applicationContext, new FeignClientFactoryBean(), type, name);
   62 | 		}
   63 | 
   64 | 		private Builder(final ApplicationContext applicationContext, final FeignClientFactoryBean clientFactoryBean,
   65 | 				final Class<T> type, final String name) {
   66 | 			this.feignClientFactoryBean = clientFactoryBean;
   67 | 
   68 | 			this.feignClientFactoryBean.setApplicationContext(applicationContext);
   69 | 			this.feignClientFactoryBean.setType(type);
   70 | 			this.feignClientFactoryBean.setName(FeignClientsRegistrar.getName(name));
   71 | 			this.feignClientFactoryBean.setContextId(FeignClientsRegistrar.getName(name));
   72 | 			this.feignClientFactoryBean.setInheritParentContext(true);
   73 | 			// preset default values - these values resemble the default values on the
   74 | 			// FeignClient annotation
   75 | 			this.url("").path("").dismiss404(false);
   76 | 		}
   77 | 
   78 | 		public Builder<T> url(final String url) {
   79 | 			this.feignClientFactoryBean.setUrl(FeignClientsRegistrar.getUrl(url));
   80 | 			return this;
   81 | 		}
   82 | 
   83 | 		/**
   84 | 		 * Applies a {@link FeignBuilderCustomizer} to the underlying
   85 | 		 * {@link Feign.Builder}. May be called multiple times.
   86 | 		 * @param customizer applied in the same order as supplied here after applying
   87 | 		 * customizers found in the context.
   88 | 		 * @return the {@link Builder} with the customizer added
   89 | 		 */
   90 | 		public Builder<T> customize(final FeignBuilderCustomizer customizer) {
   91 | 			this.feignClientFactoryBean.addCustomizer(customizer);
   92 | 			return this;
   93 | 		}
   94 | 
   95 | 		public Builder<T> contextId(final String contextId) {
   96 | 			this.feignClientFactoryBean.setContextId(contextId);
   97 | 			return this;
   98 | 		}
   99 | 
  100 | 		public Builder<T> path(final String path) {
  101 | 			this.feignClientFactoryBean.setPath(FeignClientsRegistrar.getPath(path));
  102 | 			return this;
  103 | 		}
  104 | 
  105 | 		public Builder<T> dismiss404(final boolean dismiss404) {
  106 | 			this.feignClientFactoryBean.setDismiss404(dismiss404);
  107 | 			return this;
  108 | 		}
  109 | 
  110 | 		public Builder<T> inheritParentContext(final boolean inheritParentContext) {
  111 | 			this.feignClientFactoryBean.setInheritParentContext(inheritParentContext);
  112 | 			return this;
  113 | 		}
  114 | 
  115 | 		public Builder<T> fallback(final Class<? extends T> fallback) {
  116 | 			FeignClientsRegistrar.validateFallback(fallback);
  117 | 			this.feignClientFactoryBean.setFallback(fallback);
  118 | 			return this;
  119 | 		}
  120 | 
  121 | 		/**
  122 | 		 * @return the created Feign client
  123 | 		 */
  124 | 		public T build() {
  125 | 			return this.feignClientFactoryBean.getTarget();
  126 | 		}
  127 | 
  128 | 	}
  129 | 
  130 | }
```
