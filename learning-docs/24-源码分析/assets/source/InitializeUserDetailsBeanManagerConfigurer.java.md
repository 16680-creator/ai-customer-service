<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/authentication/configuration/InitializeUserDetailsBeanManagerConfigurer.java -->
<!-- 正文引用：[[源码:InitializeUserDetailsBeanManagerConfigurer.java#L起-L止]] ；代码块头部：(src: InitializeUserDetailsBeanManagerConfigurer.java#L起-L止) -->

# InitializeUserDetailsBeanManagerConfigurer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/authentication/configuration/InitializeUserDetailsBeanManagerConfigurer.java` |
| 行数 | 127 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:38:37.307Z |

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
   17 | package org.springframework.security.config.annotation.authentication.configuration;
   18 | 
   19 | import java.util.Arrays;
   20 | 
   21 | import org.apache.commons.logging.Log;
   22 | import org.apache.commons.logging.LogFactory;
   23 | 
   24 | import org.springframework.context.ApplicationContext;
   25 | import org.springframework.core.Ordered;
   26 | import org.springframework.core.annotation.Order;
   27 | import org.springframework.core.log.LogMessage;
   28 | import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
   29 | import org.springframework.security.authentication.password.CompromisedPasswordChecker;
   30 | import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
   31 | import org.springframework.security.core.userdetails.UserDetailsPasswordService;
   32 | import org.springframework.security.core.userdetails.UserDetailsService;
   33 | import org.springframework.security.crypto.password.PasswordEncoder;
   34 | 
   35 | /**
   36 |  * Lazily initializes the global authentication with a {@link UserDetailsService} if it is
   37 |  * not yet configured and there is only a single Bean of that type. Optionally, if a
   38 |  * {@link PasswordEncoder} is defined will wire this up too.
   39 |  *
   40 |  * @author Rob Winch
   41 |  * @author Ngoc Nhan
   42 |  * @since 4.1
   43 |  */
   44 | @Order(InitializeUserDetailsBeanManagerConfigurer.DEFAULT_ORDER)
   45 | class InitializeUserDetailsBeanManagerConfigurer extends GlobalAuthenticationConfigurerAdapter {
   46 | 
   47 | 	static final int DEFAULT_ORDER = Ordered.LOWEST_PRECEDENCE - 5000;
   48 | 
   49 | 	private final ApplicationContext context;
   50 | 
   51 | 	/**
   52 | 	 * Creates a new instance using the given {@link ApplicationContext}.
   53 | 	 * @param context
   54 | 	 */
   55 | 	InitializeUserDetailsBeanManagerConfigurer(ApplicationContext context) {
   56 | 		this.context = context;
   57 | 	}
   58 | 
   59 | 	@Override
   60 | 	public void init(AuthenticationManagerBuilder auth) {
   61 | 		auth.apply(new InitializeUserDetailsManagerConfigurer());
   62 | 	}
   63 | 
   64 | 	class InitializeUserDetailsManagerConfigurer extends GlobalAuthenticationConfigurerAdapter {
   65 | 
   66 | 		private final Log logger = LogFactory.getLog(getClass());
   67 | 
   68 | 		@Override
   69 | 		public void configure(AuthenticationManagerBuilder auth) {
   70 | 			String[] beanNames = InitializeUserDetailsBeanManagerConfigurer.this.context
   71 | 				.getBeanNamesForType(UserDetailsService.class);
   72 | 			if (auth.isConfigured()) {
   73 | 				if (beanNames.length > 0) {
   74 | 					this.logger.warn("Global AuthenticationManager configured with an AuthenticationProvider bean. "
   75 | 							+ "UserDetailsService beans will not be used by Spring Security for automatically configuring username/password login. "
   76 | 							+ "Consider removing the AuthenticationProvider bean. "
   77 | 							+ "Alternatively, consider using the UserDetailsService in a manually instantiated DaoAuthenticationProvider. "
   78 | 							+ "If the current configuration is intentional, to turn off this warning, "
   79 | 							+ "increase the logging level of 'org.springframework.security.config.annotation.authentication.configuration.InitializeUserDetailsBeanManagerConfigurer' to ERROR");
   80 | 				}
   81 | 				return;
   82 | 			}
   83 | 
   84 | 			if (beanNames.length == 0) {
   85 | 				return;
   86 | 			}
   87 | 			else if (beanNames.length > 1) {
   88 | 				this.logger.warn(LogMessage.format("Found %s UserDetailsService beans, with names %s. "
   89 | 						+ "Global Authentication Manager will not use a UserDetailsService for username/password login. "
   90 | 						+ "Consider publishing a single UserDetailsService bean.", beanNames.length,
   91 | 						Arrays.toString(beanNames)));
   92 | 				return;
   93 | 			}
   94 | 			UserDetailsService userDetailsService = InitializeUserDetailsBeanManagerConfigurer.this.context
   95 | 				.getBean(beanNames[0], UserDetailsService.class);
   96 | 			PasswordEncoder passwordEncoder = getBeanOrNull(PasswordEncoder.class);
   97 | 			UserDetailsPasswordService passwordManager = getBeanOrNull(UserDetailsPasswordService.class);
   98 | 			CompromisedPasswordChecker passwordChecker = getBeanOrNull(CompromisedPasswordChecker.class);
   99 | 			DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
  100 | 			if (passwordEncoder != null) {
  101 | 				provider.setPasswordEncoder(passwordEncoder);
  102 | 			}
  103 | 			if (passwordManager != null) {
  104 | 				provider.setUserDetailsPasswordService(passwordManager);
  105 | 			}
  106 | 			if (passwordChecker != null) {
  107 | 				provider.setCompromisedPasswordChecker(passwordChecker);
  108 | 			}
  109 | 			provider.afterPropertiesSet();
  110 | 			auth.authenticationProvider(provider);
  111 | 			this.logger.info(LogMessage.format(
  112 | 					"Global AuthenticationManager configured with UserDetailsService bean with name %s", beanNames[0]));
  113 | 		}
  114 | 
  115 | 		/**
  116 | 		 * Returns a bean of the requested class if there's just a single registered
  117 | 		 * component, null otherwise.
  118 | 		 * @return a bean of the requested class if there's just a single registered
  119 | 		 * component, null otherwise.
  120 | 		 */
  121 | 		private <T> T getBeanOrNull(Class<T> type) {
  122 | 			return InitializeUserDetailsBeanManagerConfigurer.this.context.getBeanProvider(type).getIfUnique();
  123 | 		}
  124 | 
  125 | 	}
  126 | 
  127 | }
```
