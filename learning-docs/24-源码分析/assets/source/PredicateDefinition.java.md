<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0 | path: org/springframework/cloud/gateway/handler/predicate/PredicateDefinition.java -->
<!-- 正文引用：[[源码:PredicateDefinition.java#L起-L止]] ；代码块头部：(src: PredicateDefinition.java#L起-L止) -->

# PredicateDefinition.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-gateway-server-webflux:5.1.0` |
| 路径 | `org/springframework/cloud/gateway/handler/predicate/PredicateDefinition.java` |
| 行数 | 105 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-gateway-server-webflux/5.1.0/spring-cloud-gateway-server-webflux-5.1.0-sources.jar |
| 抓取时间 | 2026-11-26T07:50:18.865Z |

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
   17 | package org.springframework.cloud.gateway.handler.predicate;
   18 | 
   19 | import java.util.LinkedHashMap;
   20 | import java.util.Map;
   21 | import java.util.Objects;
   22 | 
   23 | import jakarta.validation.ValidationException;
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.cloud.gateway.support.NameUtils;
   27 | import org.springframework.validation.annotation.Validated;
   28 | 
   29 | import static org.springframework.util.StringUtils.tokenizeToStringArray;
   30 | 
   31 | /**
   32 |  * @author Spencer Gibb
   33 |  */
   34 | @Validated
   35 | public class PredicateDefinition {
   36 | 
   37 | 	private @Nullable String name;
   38 | 
   39 | 	private Map<String, String> args = new LinkedHashMap<>();
   40 | 
   41 | 	public PredicateDefinition() {
   42 | 	}
   43 | 
   44 | 	public PredicateDefinition(String text) {
   45 | 		int eqIdx = text.indexOf('=');
   46 | 		if (eqIdx <= 0) {
   47 | 			throw new ValidationException(
   48 | 					"Unable to parse PredicateDefinition text '" + text + "'" + ", must be of the form name=value");
   49 | 		}
   50 | 		setName(text.substring(0, eqIdx));
   51 | 
   52 | 		String[] args = tokenizeToStringArray(text.substring(eqIdx + 1), ",");
   53 | 
   54 | 		for (int i = 0; i < args.length; i++) {
   55 | 			this.args.put(NameUtils.generateName(i), args[i]);
   56 | 		}
   57 | 	}
   58 | 
   59 | 	public @Nullable String getName() {
   60 | 		return name;
   61 | 	}
   62 | 
   63 | 	public void setName(String name) {
   64 | 		this.name = name;
   65 | 	}
   66 | 
   67 | 	public Map<String, String> getArgs() {
   68 | 		return args;
   69 | 	}
   70 | 
   71 | 	public void setArgs(Map<String, String> args) {
   72 | 		this.args = args;
   73 | 	}
   74 | 
   75 | 	public void addArg(String key, String value) {
   76 | 		this.args.put(key, value);
   77 | 	}
   78 | 
   79 | 	@Override
   80 | 	public boolean equals(Object o) {
   81 | 		if (this == o) {
   82 | 			return true;
   83 | 		}
   84 | 		if (o == null || getClass() != o.getClass()) {
   85 | 			return false;
   86 | 		}
   87 | 		PredicateDefinition that = (PredicateDefinition) o;
   88 | 		return Objects.equals(name, that.name) && Objects.equals(args, that.args);
   89 | 	}
   90 | 
   91 | 	@Override
   92 | 	public int hashCode() {
   93 | 		return Objects.hash(name, args);
   94 | 	}
   95 | 
   96 | 	@Override
   97 | 	public String toString() {
   98 | 		final StringBuilder sb = new StringBuilder("PredicateDefinition{");
   99 | 		sb.append("name='").append(name).append('\'');
  100 | 		sb.append(", args=").append(args);
  101 | 		sb.append('}');
  102 | 		return sb.toString();
  103 | 	}
  104 | 
  105 | }
```
