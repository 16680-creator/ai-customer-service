<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-actuator:4.2.0 | path: org/springframework/boot/actuate/endpoint/annotation/OperationReflectiveProcessor.java -->
<!-- 正文引用：[[源码:OperationReflectiveProcessor.java#L起-L止]] ；代码块头部：(src: OperationReflectiveProcessor.java#L起-L止) -->

# OperationReflectiveProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-actuator:4.2.0` |
| 路径 | `org/springframework/boot/actuate/endpoint/annotation/OperationReflectiveProcessor.java` |
| 行数 | 52 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-actuator/4.2.0/spring-boot-actuator-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:01.300Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.actuate.endpoint.annotation;
    6 | 
    7 | import java.lang.reflect.Method;
    8 | import java.lang.reflect.Type;
    9 | 
   10 | import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
   11 | import org.springframework.aot.hint.ReflectionHints;
   12 | import org.springframework.aot.hint.annotation.ReflectiveProcessor;
   13 | import org.springframework.aot.hint.annotation.SimpleReflectiveProcessor;
   14 | import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
   15 | import org.springframework.core.ResolvableType;
   16 | import org.springframework.core.io.Resource;
   17 | 
   18 | /**
   19 |  * {@link ReflectiveProcessor} that registers the annotated operation method and its
   20 |  * return type for reflection.
   21 |  *
   22 |  * @author Moritz Halbritter
   23 |  * @author Stephane Nicoll
   24 |  */
   25 | class OperationReflectiveProcessor extends SimpleReflectiveProcessor {
   26 | 
   27 | 	private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();
   28 | 
   29 | 	@Override
   30 | 	protected void registerMethodHint(ReflectionHints hints, Method method) {
   31 | 		super.registerMethodHint(hints, method);
   32 | 		Type returnType = extractReturnType(method);
   33 | 		if (returnType != null) {
   34 | 			registerReflectionHints(hints, returnType);
   35 | 		}
   36 | 	}
   37 | 
   38 | 	private Type extractReturnType(Method method) {
   39 | 		ResolvableType returnType = ResolvableType.forMethodReturnType(method);
   40 | 		if (!WebEndpointResponse.class.isAssignableFrom(method.getReturnType())) {
   41 | 			return returnType.getType();
   42 | 		}
   43 | 		return returnType.as(WebEndpointResponse.class).getGeneric(0).getType();
   44 | 	}
   45 | 
   46 | 	private void registerReflectionHints(ReflectionHints hints, Type type) {
   47 | 		if (!type.equals(Resource.class)) {
   48 | 			this.bindingRegistrar.registerReflectionHints(hints, type);
   49 | 		}
   50 | 	}
   51 | 
   52 | }
```
