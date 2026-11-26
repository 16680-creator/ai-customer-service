<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/DefaultSecurityFilterChain.java -->
<!-- 正文引用：[[源码:DefaultSecurityFilterChain.java#L起-L止]] ；代码块头部：(src: DefaultSecurityFilterChain.java#L起-L止) -->

# DefaultSecurityFilterChain.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/DefaultSecurityFilterChain.java` |
| 行数 | 129 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:18.091Z |

```java
    1 | /*
    2 |  * Copyright 2004-present the original author or authors.
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
   17 | package org.springframework.security.web;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Arrays;
   21 | import java.util.List;
   22 | 
   23 | import jakarta.servlet.Filter;
   24 | import jakarta.servlet.http.HttpServletRequest;
   25 | import org.apache.commons.logging.Log;
   26 | import org.apache.commons.logging.LogFactory;
   27 | import org.jspecify.annotations.NonNull;
   28 | import org.jspecify.annotations.Nullable;
   29 | 
   30 | import org.springframework.beans.BeansException;
   31 | import org.springframework.beans.factory.BeanFactory;
   32 | import org.springframework.beans.factory.BeanFactoryAware;
   33 | import org.springframework.beans.factory.BeanNameAware;
   34 | import org.springframework.beans.factory.config.BeanDefinition;
   35 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   36 | import org.springframework.core.log.LogMessage;
   37 | import org.springframework.security.web.util.matcher.RequestMatcher;
   38 | import org.springframework.util.StringUtils;
   39 | 
   40 | /**
   41 |  * Standard implementation of {@code SecurityFilterChain}.
   42 |  *
   43 |  * @author Luke Taylor
   44 |  * @author Jinwoo Bae
   45 |  * @since 3.1
   46 |  */
   47 | public final class DefaultSecurityFilterChain implements SecurityFilterChain, BeanNameAware, BeanFactoryAware {
   48 | 
   49 | 	private static final Log logger = LogFactory.getLog(DefaultSecurityFilterChain.class);
   50 | 
   51 | 	private final RequestMatcher requestMatcher;
   52 | 
   53 | 	private final List<Filter> filters;
   54 | 
   55 | 	private @Nullable String beanName;
   56 | 
   57 | 	private @Nullable ConfigurableListableBeanFactory beanFactory;
   58 | 
   59 | 	public DefaultSecurityFilterChain(RequestMatcher requestMatcher, Filter... filters) {
   60 | 		this(requestMatcher, Arrays.asList(filters));
   61 | 	}
   62 | 
   63 | 	public DefaultSecurityFilterChain(RequestMatcher requestMatcher, List<Filter> filters) {
   64 | 		if (filters.isEmpty()) {
   65 | 			logger.debug(LogMessage.format("Will not secure %s", requestMatcher));
   66 | 		}
   67 | 		else {
   68 | 			List<String> filterNames = new ArrayList<>();
   69 | 			for (Filter filter : filters) {
   70 | 				filterNames.add(filter.getClass().getSimpleName());
   71 | 			}
   72 | 			String names = StringUtils.collectionToDelimitedString(filterNames, ", ");
   73 | 			logger.debug(LogMessage.format("Will secure %s with filters: %s", requestMatcher, names));
   74 | 		}
   75 | 		this.requestMatcher = requestMatcher;
   76 | 		this.filters = new ArrayList<>(filters);
   77 | 	}
   78 | 
   79 | 	public RequestMatcher getRequestMatcher() {
   80 | 		return this.requestMatcher;
   81 | 	}
   82 | 
   83 | 	@Override
   84 | 	public List<Filter> getFilters() {
   85 | 		return this.filters;
   86 | 	}
   87 | 
   88 | 	@Override
   89 | 	public boolean matches(HttpServletRequest request) {
   90 | 		return this.requestMatcher.matches(request);
   91 | 	}
   92 | 
   93 | 	@Override
   94 | 	public String toString() {
   95 | 		List<String> filterNames = new ArrayList<>();
   96 | 		for (Filter filter : this.filters) {
   97 | 			String name = filter.getClass().getSimpleName();
   98 | 			if (name.endsWith("Filter")) {
   99 | 				name = name.substring(0, name.length() - "Filter".length());
  100 | 			}
  101 | 			filterNames.add(name);
  102 | 		}
  103 | 		String declaration = this.getClass().getSimpleName();
  104 | 		if (this.beanName != null) {
  105 | 			declaration += " defined as '" + this.beanName + "'";
  106 | 			if (this.beanFactory != null) {
  107 | 				BeanDefinition bd = this.beanFactory.getBeanDefinition(this.beanName);
  108 | 				String description = bd.getResourceDescription();
  109 | 				if (description != null) {
  110 | 					declaration += " in [" + description + "]";
  111 | 				}
  112 | 			}
  113 | 		}
  114 | 		return declaration + " matching [" + this.requestMatcher + "] and having filters " + filterNames;
  115 | 	}
  116 | 
  117 | 	@Override
  118 | 	public void setBeanName(@NonNull String name) {
  119 | 		this.beanName = name;
  120 | 	}
  121 | 
  122 | 	@Override
  123 | 	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
  124 | 		if (beanFactory instanceof ConfigurableListableBeanFactory listable) {
  125 | 			this.beanFactory = listable;
  126 | 		}
  127 | 	}
  128 | 
  129 | }
```
