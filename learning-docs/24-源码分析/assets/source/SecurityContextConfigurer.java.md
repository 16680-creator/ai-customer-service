<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/web/configurers/SecurityContextConfigurer.java -->
<!-- 正文引用：[[源码:SecurityContextConfigurer.java#L起-L止]] ；代码块头部：(src: SecurityContextConfigurer.java#L起-L止) -->

# SecurityContextConfigurer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/web/configurers/SecurityContextConfigurer.java` |
| 行数 | 134 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:55:53.279Z |

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
   17 | package org.springframework.security.config.annotation.web.configurers;
   18 | 
   19 | import org.springframework.security.config.Customizer;
   20 | import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
   21 | import org.springframework.security.config.annotation.web.builders.HttpSecurity;
   22 | import org.springframework.security.config.http.SessionCreationPolicy;
   23 | import org.springframework.security.core.context.SecurityContext;
   24 | import org.springframework.security.core.context.SecurityContextHolder;
   25 | import org.springframework.security.web.context.DelegatingSecurityContextRepository;
   26 | import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
   27 | import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
   28 | import org.springframework.security.web.context.SecurityContextHolderFilter;
   29 | import org.springframework.security.web.context.SecurityContextPersistenceFilter;
   30 | import org.springframework.security.web.context.SecurityContextRepository;
   31 | import org.springframework.security.web.session.ForceEagerSessionCreationFilter;
   32 | 
   33 | /**
   34 |  * Allows persisting and restoring of the {@link SecurityContext} found on the
   35 |  * {@link SecurityContextHolder} for each request by configuring the
   36 |  * {@link SecurityContextPersistenceFilter}. All properties have reasonable defaults, so
   37 |  * no additional configuration is required other than applying this
   38 |  * {@link org.springframework.security.config.annotation.SecurityConfigurer}.
   39 |  *
   40 |  * <h2>Security Filters</h2>
   41 |  *
   42 |  * The following Filters are populated
   43 |  *
   44 |  * <ul>
   45 |  * <li>{@link SecurityContextPersistenceFilter}</li>
   46 |  * </ul>
   47 |  *
   48 |  * <h2>Shared Objects Created</h2>
   49 |  *
   50 |  * No shared objects are created.
   51 |  *
   52 |  * <h2>Shared Objects Used</h2>
   53 |  *
   54 |  * The following shared objects are used:
   55 |  *
   56 |  * <ul>
   57 |  * <li>If {@link SessionManagementConfigurer}, is provided and set to always, then the
   58 |  * {@link SecurityContextPersistenceFilter#setForceEagerSessionCreation(boolean)} will be
   59 |  * set to true.</li>
   60 |  * <li>{@link SecurityContextRepository} must be set and is used on
   61 |  * {@link SecurityContextPersistenceFilter}.</li>
   62 |  * </ul>
   63 |  *
   64 |  * @author Rob Winch
   65 |  * @since 3.2
   66 |  */
   67 | public final class SecurityContextConfigurer<H extends HttpSecurityBuilder<H>>
   68 | 		extends AbstractHttpConfigurer<SecurityContextConfigurer<H>, H> {
   69 | 
   70 | 	private boolean requireExplicitSave = true;
   71 | 
   72 | 	/**
   73 | 	 * Creates a new instance.
   74 | 	 * @see HttpSecurity#securityContext(Customizer)
   75 | 	 */
   76 | 	public SecurityContextConfigurer() {
   77 | 	}
   78 | 
   79 | 	/**
   80 | 	 * Specifies the shared {@link SecurityContextRepository} that is to be used.
   81 | 	 * @param securityContextRepository the {@link SecurityContextRepository} to use
   82 | 	 * @return the {@link HttpSecurity} for further customizations
   83 | 	 */
   84 | 	public SecurityContextConfigurer<H> securityContextRepository(SecurityContextRepository securityContextRepository) {
   85 | 		getBuilder().setSharedObject(SecurityContextRepository.class, securityContextRepository);
   86 | 		return this;
   87 | 	}
   88 | 
   89 | 	public SecurityContextConfigurer<H> requireExplicitSave(boolean requireExplicitSave) {
   90 | 		this.requireExplicitSave = requireExplicitSave;
   91 | 		return this;
   92 | 	}
   93 | 
   94 | 	boolean isRequireExplicitSave() {
   95 | 		return this.requireExplicitSave;
   96 | 	}
   97 | 
   98 | 	SecurityContextRepository getSecurityContextRepository() {
   99 | 		SecurityContextRepository securityContextRepository = getBuilder()
  100 | 			.getSharedObject(SecurityContextRepository.class);
  101 | 		if (securityContextRepository == null) {
  102 | 			securityContextRepository = new DelegatingSecurityContextRepository(
  103 | 					new RequestAttributeSecurityContextRepository(), new HttpSessionSecurityContextRepository());
  104 | 		}
  105 | 		return securityContextRepository;
  106 | 	}
  107 | 
  108 | 	@Override
  109 | 	@SuppressWarnings("unchecked")
  110 | 	public void configure(H http) {
  111 | 		SecurityContextRepository securityContextRepository = getSecurityContextRepository();
  112 | 		if (this.requireExplicitSave) {
  113 | 			SecurityContextHolderFilter securityContextHolderFilter = postProcess(
  114 | 					new SecurityContextHolderFilter(securityContextRepository));
  115 | 			securityContextHolderFilter.setSecurityContextHolderStrategy(getSecurityContextHolderStrategy());
  116 | 			http.addFilter(securityContextHolderFilter);
  117 | 		}
  118 | 		else {
  119 | 			SecurityContextPersistenceFilter securityContextFilter = new SecurityContextPersistenceFilter(
  120 | 					securityContextRepository);
  121 | 			securityContextFilter.setSecurityContextHolderStrategy(getSecurityContextHolderStrategy());
  122 | 			SessionManagementConfigurer<?> sessionManagement = http.getConfigurer(SessionManagementConfigurer.class);
  123 | 			SessionCreationPolicy sessionCreationPolicy = (sessionManagement != null)
  124 | 					? sessionManagement.getSessionCreationPolicy() : null;
  125 | 			if (SessionCreationPolicy.ALWAYS == sessionCreationPolicy) {
  126 | 				securityContextFilter.setForceEagerSessionCreation(true);
  127 | 				http.addFilter(postProcess(new ForceEagerSessionCreationFilter()));
  128 | 			}
  129 | 			securityContextFilter = postProcess(securityContextFilter);
  130 | 			http.addFilter(securityContextFilter);
  131 | 		}
  132 | 	}
  133 | 
  134 | }
```
