<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/support/NameUtils.java -->
<!-- 正文引用：[[源码:NameUtils.java#L起-L止]] ；代码块头部：(src: NameUtils.java#L起-L止) -->

# NameUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/support/NameUtils.java` |
| 行数 | 95 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:26.426Z |

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
   17 | package org.springframework.cloud.gateway.support;
   18 | 
   19 | import java.util.Locale;
   20 | import java.util.regex.Matcher;
   21 | import java.util.regex.Pattern;
   22 | 
   23 | import org.springframework.cloud.gateway.filter.GlobalFilter;
   24 | import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
   25 | import org.springframework.cloud.gateway.handler.predicate.RoutePredicateFactory;
   26 | 
   27 | /**
   28 |  * @author Spencer Gibb
   29 |  */
   30 | public final class NameUtils {
   31 | 
   32 | 	private NameUtils() {
   33 | 		throw new AssertionError("Must not instantiate utility class.");
   34 | 	}
   35 | 
   36 | 	/**
   37 | 	 * Generated name prefix.
   38 | 	 */
   39 | 	public static final String GENERATED_NAME_PREFIX = "_genkey_";
   40 | 
   41 | 	private static final Pattern NAME_PATTERN = Pattern.compile("([A-Z][a-z0-9]+)");
   42 | 
   43 | 	public static String generateName(int i) {
   44 | 		return GENERATED_NAME_PREFIX + i;
   45 | 	}
   46 | 
   47 | 	public static String normalizeRoutePredicateName(Class<? extends RoutePredicateFactory> clazz) {
   48 | 		return removeGarbage(clazz.getSimpleName().replace(RoutePredicateFactory.class.getSimpleName(), ""));
   49 | 	}
   50 | 
   51 | 	public static String normalizeRoutePredicateNameAsProperty(Class<? extends RoutePredicateFactory> clazz) {
   52 | 		return normalizeToCanonicalPropertyFormat(normalizeRoutePredicateName(clazz));
   53 | 	}
   54 | 
   55 | 	public static String normalizeFilterFactoryName(Class<? extends GatewayFilterFactory> clazz) {
   56 | 		return removeGarbage(clazz.getSimpleName().replace(GatewayFilterFactory.class.getSimpleName(), ""));
   57 | 	}
   58 | 
   59 | 	public static String normalizeGlobalFilterName(Class<? extends GlobalFilter> clazz) {
   60 | 		return removeGarbage(clazz.getSimpleName().replace(GlobalFilter.class.getSimpleName(), "")).replace("Filter",
   61 | 				"");
   62 | 	}
   63 | 
   64 | 	public static String normalizeFilterFactoryNameAsProperty(Class<? extends GatewayFilterFactory> clazz) {
   65 | 		return normalizeToCanonicalPropertyFormat(normalizeFilterFactoryName(clazz));
   66 | 	}
   67 | 
   68 | 	public static String normalizeGlobalFilterNameAsProperty(Class<? extends GlobalFilter> filterClass) {
   69 | 		return normalizeToCanonicalPropertyFormat(normalizeGlobalFilterName(filterClass));
   70 | 	}
   71 | 
   72 | 	public static String normalizeToCanonicalPropertyFormat(String name) {
   73 | 		Matcher matcher = NAME_PATTERN.matcher(name);
   74 | 		StringBuffer stringBuffer = new StringBuffer();
   75 | 		while (matcher.find()) {
   76 | 			if (stringBuffer.length() != 0) {
   77 | 				matcher.appendReplacement(stringBuffer, "-" + matcher.group(1));
   78 | 			}
   79 | 			else {
   80 | 				matcher.appendReplacement(stringBuffer, matcher.group(1));
   81 | 			}
   82 | 		}
   83 | 		return stringBuffer.toString().toLowerCase(Locale.ROOT);
   84 | 	}
   85 | 
   86 | 	private static String removeGarbage(String s) {
   87 | 		int garbageIdx = s.indexOf("$Mockito");
   88 | 		if (garbageIdx > 0) {
   89 | 			return s.substring(0, garbageIdx);
   90 | 		}
   91 | 
   92 | 		return s;
   93 | 	}
   94 | 
   95 | }
```
