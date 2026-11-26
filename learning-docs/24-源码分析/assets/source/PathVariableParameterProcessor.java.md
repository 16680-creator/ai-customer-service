<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-openfeign-core:5.1.0 | path: org/springframework/cloud/openfeign/annotation/PathVariableParameterProcessor.java -->
<!-- 正文引用：[[源码:PathVariableParameterProcessor.java#L起-L止]] ；代码块头部：(src: PathVariableParameterProcessor.java#L起-L止) -->

# PathVariableParameterProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-openfeign-core:5.1.0` |
| 路径 | `org/springframework/cloud/openfeign/annotation/PathVariableParameterProcessor.java` |
| 行数 | 78 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-openfeign-core/5.1.0/spring-cloud-openfeign-core-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:26.351Z |

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
   17 | package org.springframework.cloud.openfeign.annotation;
   18 | 
   19 | import java.lang.annotation.Annotation;
   20 | import java.lang.reflect.Method;
   21 | import java.util.Collection;
   22 | import java.util.Map;
   23 | import java.util.regex.Pattern;
   24 | 
   25 | import feign.MethodMetadata;
   26 | 
   27 | import org.springframework.cloud.openfeign.AnnotatedParameterProcessor;
   28 | import org.springframework.web.bind.annotation.PathVariable;
   29 | 
   30 | import static feign.Util.checkState;
   31 | import static feign.Util.emptyToNull;
   32 | 
   33 | /**
   34 |  * {@link PathVariable} parameter processor.
   35 |  *
   36 |  * @author Jakub Narloch
   37 |  * @author Abhijit Sarkar
   38 |  * @author Yanming Zhou
   39 |  * @see AnnotatedParameterProcessor
   40 |  */
   41 | public class PathVariableParameterProcessor implements AnnotatedParameterProcessor {
   42 | 
   43 | 	private static final Class<PathVariable> ANNOTATION = PathVariable.class;
   44 | 
   45 | 	@Override
   46 | 	public Class<? extends Annotation> getAnnotationType() {
   47 | 		return ANNOTATION;
   48 | 	}
   49 | 
   50 | 	@Override
   51 | 	public boolean processArgument(AnnotatedParameterContext context, Annotation annotation, Method method) {
   52 | 		String name = ANNOTATION.cast(annotation).value();
   53 | 		checkState(emptyToNull(name) != null, "PathVariable annotation was empty on param %s.",
   54 | 				context.getParameterIndex());
   55 | 		context.setParameterName(name);
   56 | 
   57 | 		MethodMetadata data = context.getMethodMetadata();
   58 | 		Pattern varNamePattern = Pattern.compile("\\{" + Pattern.quote(name) + "(:[^}]+)?\\}");
   59 | 		if (!varNamePattern.matcher(data.template().url()).find()
   60 | 				&& !matchesMapValues(data.template().queries(), varNamePattern)
   61 | 				&& !matchesMapValues(data.template().headers(), varNamePattern)) {
   62 | 			data.formParams().add(name);
   63 | 		}
   64 | 		return true;
   65 | 	}
   66 | 
   67 | 	private boolean matchesMapValues(Map<String, Collection<String>> map, Pattern pattern) {
   68 | 		for (Collection<String> entry : map.values()) {
   69 | 			for (String value : entry) {
   70 | 				if (pattern.matcher(value).find()) {
   71 | 					return true;
   72 | 				}
   73 | 			}
   74 | 		}
   75 | 		return false;
   76 | 	}
   77 | 
   78 | }
```
