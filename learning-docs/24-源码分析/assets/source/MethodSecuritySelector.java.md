<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/method/configuration/MethodSecuritySelector.java -->
<!-- 正文引用：[[源码:MethodSecuritySelector.java#L起-L止]] ；代码块头部：(src: MethodSecuritySelector.java#L起-L止) -->

# MethodSecuritySelector.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/method/configuration/MethodSecuritySelector.java` |
| 行数 | 104 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:41.246Z |

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
   17 | package org.springframework.security.config.annotation.method.configuration;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Arrays;
   21 | import java.util.List;
   22 | 
   23 | import org.springframework.context.annotation.AdviceMode;
   24 | import org.springframework.context.annotation.AdviceModeImportSelector;
   25 | import org.springframework.context.annotation.AutoProxyRegistrar;
   26 | import org.springframework.context.annotation.ImportSelector;
   27 | import org.springframework.core.type.AnnotationMetadata;
   28 | import org.springframework.lang.NonNull;
   29 | import org.springframework.util.ClassUtils;
   30 | 
   31 | /**
   32 |  * Dynamically determines which imports to include using the {@link EnableMethodSecurity}
   33 |  * annotation.
   34 |  *
   35 |  * @author Evgeniy Cheban
   36 |  * @author Josh Cummings
   37 |  * @since 5.6
   38 |  */
   39 | final class MethodSecuritySelector implements ImportSelector {
   40 | 
   41 | 	private static final boolean isDataPresent = ClassUtils
   42 | 		.isPresent("org.springframework.security.data.aot.hint.AuthorizeReturnObjectDataHintsRegistrar", null);
   43 | 
   44 | 	private static final boolean isWebPresent = ClassUtils
   45 | 		.isPresent("org.springframework.web.servlet.DispatcherServlet", null)
   46 | 			&& ClassUtils.isPresent("org.springframework.security.web.util.ThrowableAnalyzer", null);
   47 | 
   48 | 	private static final boolean isObservabilityPresent = ClassUtils
   49 | 		.isPresent("io.micrometer.observation.ObservationRegistry", null);
   50 | 
   51 | 	private final ImportSelector autoProxy = new AutoProxyRegistrarSelector();
   52 | 
   53 | 	@Override
   54 | 	public String[] selectImports(@NonNull AnnotationMetadata importMetadata) {
   55 | 		if (!importMetadata.hasAnnotation(EnableMethodSecurity.class.getName())
   56 | 				&& !importMetadata.hasMetaAnnotation(EnableMethodSecurity.class.getName())) {
   57 | 			return new String[0];
   58 | 		}
   59 | 		EnableMethodSecurity annotation = importMetadata.getAnnotations().get(EnableMethodSecurity.class).synthesize();
   60 | 		List<String> imports = new ArrayList<>(Arrays.asList(this.autoProxy.selectImports(importMetadata)));
   61 | 		if (annotation.prePostEnabled()) {
   62 | 			imports.add(PrePostMethodSecurityConfiguration.class.getName());
   63 | 		}
   64 | 		if (annotation.securedEnabled()) {
   65 | 			imports.add(SecuredMethodSecurityConfiguration.class.getName());
   66 | 		}
   67 | 		if (annotation.jsr250Enabled()) {
   68 | 			imports.add(Jsr250MethodSecurityConfiguration.class.getName());
   69 | 		}
   70 | 		imports.add(AuthorizationProxyConfiguration.class.getName());
   71 | 		if (isDataPresent) {
   72 | 			imports.add(AuthorizationProxyDataConfiguration.class.getName());
   73 | 		}
   74 | 		if (isWebPresent) {
   75 | 			imports.add(AuthorizationProxyWebConfiguration.class.getName());
   76 | 		}
   77 | 		if (isObservabilityPresent) {
   78 | 			imports.add(MethodObservationConfiguration.class.getName());
   79 | 		}
   80 | 		return imports.toArray(new String[0]);
   81 | 	}
   82 | 
   83 | 	private static final class AutoProxyRegistrarSelector extends AdviceModeImportSelector<EnableMethodSecurity> {
   84 | 
   85 | 		private static final String[] IMPORTS = new String[] { AutoProxyRegistrar.class.getName(),
   86 | 				MethodSecurityAdvisorRegistrar.class.getName() };
   87 | 
   88 | 		private static final String[] ASPECTJ_IMPORTS = new String[] {
   89 | 				MethodSecurityAspectJAutoProxyRegistrar.class.getName() };
   90 | 
   91 | 		@Override
   92 | 		protected String[] selectImports(@NonNull AdviceMode adviceMode) {
   93 | 			if (adviceMode == AdviceMode.PROXY) {
   94 | 				return IMPORTS;
   95 | 			}
   96 | 			if (adviceMode == AdviceMode.ASPECTJ) {
   97 | 				return ASPECTJ_IMPORTS;
   98 | 			}
   99 | 			throw new IllegalStateException("AdviceMode '" + adviceMode + "' is not supported");
  100 | 		}
  101 | 
  102 | 	}
  103 | 
  104 | }
```
