<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/AuthenticatedAuthorizationManager.java -->
<!-- 正文引用：[[源码:AuthenticatedAuthorizationManager.java#L起-L止]] ；代码块头部：(src: AuthenticatedAuthorizationManager.java#L起-L止) -->

# AuthenticatedAuthorizationManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/AuthenticatedAuthorizationManager.java` |
| 行数 | 170 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:38:06.898Z |

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
   17 | package org.springframework.security.authorization;
   18 | 
   19 | import java.util.function.Supplier;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.security.authentication.AuthenticationTrustResolver;
   24 | import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
   25 | import org.springframework.security.core.Authentication;
   26 | import org.springframework.util.Assert;
   27 | 
   28 | /**
   29 |  * An {@link AuthorizationManager} that determines if the current user is authenticated.
   30 |  *
   31 |  * @param <T> the type of object authorization is being performed against. This does not.
   32 |  * @author Evgeniy Cheban
   33 |  * @since 5.5
   34 |  */
   35 | public final class AuthenticatedAuthorizationManager<T> implements AuthorizationManager<T> {
   36 | 
   37 | 	private final AbstractAuthorizationStrategy authorizationStrategy;
   38 | 
   39 | 	/**
   40 | 	 * Creates an instance that determines if the current user is authenticated, this is
   41 | 	 * the same as calling {@link #authenticated()} factory method.
   42 | 	 *
   43 | 	 * @since 5.8
   44 | 	 * @see #authenticated()
   45 | 	 * @see #fullyAuthenticated()
   46 | 	 * @see #rememberMe()
   47 | 	 * @see #anonymous()
   48 | 	 */
   49 | 	public AuthenticatedAuthorizationManager() {
   50 | 		this(new AuthenticatedAuthorizationStrategy());
   51 | 	}
   52 | 
   53 | 	private AuthenticatedAuthorizationManager(AbstractAuthorizationStrategy authorizationStrategy) {
   54 | 		this.authorizationStrategy = authorizationStrategy;
   55 | 	}
   56 | 
   57 | 	/**
   58 | 	 * Sets the {@link AuthenticationTrustResolver} to be used. Default is
   59 | 	 * {@link AuthenticationTrustResolverImpl}. Cannot be null.
   60 | 	 * @param trustResolver the {@link AuthenticationTrustResolver} to use
   61 | 	 * @since 5.8
   62 | 	 */
   63 | 	public void setTrustResolver(AuthenticationTrustResolver trustResolver) {
   64 | 		this.authorizationStrategy.setTrustResolver(trustResolver);
   65 | 	}
   66 | 
   67 | 	/**
   68 | 	 * Creates an instance of {@link AuthenticatedAuthorizationManager}.
   69 | 	 * @param <T> the type of object being authorized
   70 | 	 * @return the new instance
   71 | 	 */
   72 | 	public static <T> AuthenticatedAuthorizationManager<T> authenticated() {
   73 | 		return new AuthenticatedAuthorizationManager<>();
   74 | 	}
   75 | 
   76 | 	/**
   77 | 	 * Creates an instance of {@link AuthenticatedAuthorizationManager} that determines if
   78 | 	 * the {@link Authentication} is authenticated without using remember me.
   79 | 	 * @param <T> the type of object being authorized
   80 | 	 * @return the new instance
   81 | 	 * @since 5.8
   82 | 	 */
   83 | 	public static <T> AuthenticatedAuthorizationManager<T> fullyAuthenticated() {
   84 | 		return new AuthenticatedAuthorizationManager<>(new FullyAuthenticatedAuthorizationStrategy());
   85 | 	}
   86 | 
   87 | 	/**
   88 | 	 * Creates an instance of {@link AuthenticatedAuthorizationManager} that determines if
   89 | 	 * the {@link Authentication} is authenticated using remember me.
   90 | 	 * @param <T> the type of object being authorized
   91 | 	 * @return the new instance
   92 | 	 * @since 5.8
   93 | 	 */
   94 | 	public static <T> AuthenticatedAuthorizationManager<T> rememberMe() {
   95 | 		return new AuthenticatedAuthorizationManager<>(new RememberMeAuthorizationStrategy());
   96 | 	}
   97 | 
   98 | 	/**
   99 | 	 * Creates an instance of {@link AuthenticatedAuthorizationManager} that determines if
  100 | 	 * the {@link Authentication} is anonymous.
  101 | 	 * @param <T> the type of object being authorized
  102 | 	 * @return the new instance
  103 | 	 * @since 5.8
  104 | 	 */
  105 | 	public static <T> AuthenticatedAuthorizationManager<T> anonymous() {
  106 | 		return new AuthenticatedAuthorizationManager<>(new AnonymousAuthorizationStrategy());
  107 | 	}
  108 | 
  109 | 	/**
  110 | 	 * Determines if the current user is authorized according to the given strategy.
  111 | 	 * @param authentication the {@link Supplier} of the {@link Authentication} to check
  112 | 	 * @param object the {@link T} object to check
  113 | 	 * @return an {@link AuthorizationDecision}
  114 | 	 */
  115 | 	@Override
  116 | 	public AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication, T object) {
  117 | 		boolean granted = this.authorizationStrategy.isGranted(authentication.get());
  118 | 		return new AuthorizationDecision(granted);
  119 | 	}
  120 | 
  121 | 	private abstract static class AbstractAuthorizationStrategy {
  122 | 
  123 | 		AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();
  124 | 
  125 | 		private void setTrustResolver(AuthenticationTrustResolver trustResolver) {
  126 | 			Assert.notNull(trustResolver, "trustResolver cannot be null");
  127 | 			this.trustResolver = trustResolver;
  128 | 		}
  129 | 
  130 | 		abstract boolean isGranted(Authentication authentication);
  131 | 
  132 | 	}
  133 | 
  134 | 	private static class AuthenticatedAuthorizationStrategy extends AbstractAuthorizationStrategy {
  135 | 
  136 | 		@Override
  137 | 		boolean isGranted(Authentication authentication) {
  138 | 			return this.trustResolver.isAuthenticated(authentication);
  139 | 		}
  140 | 
  141 | 	}
  142 | 
  143 | 	private static final class FullyAuthenticatedAuthorizationStrategy extends AuthenticatedAuthorizationStrategy {
  144 | 
  145 | 		@Override
  146 | 		boolean isGranted(Authentication authentication) {
  147 | 			return this.trustResolver.isFullyAuthenticated(authentication);
  148 | 		}
  149 | 
  150 | 	}
  151 | 
  152 | 	private static final class AnonymousAuthorizationStrategy extends AbstractAuthorizationStrategy {
  153 | 
  154 | 		@Override
  155 | 		boolean isGranted(Authentication authentication) {
  156 | 			return this.trustResolver.isAnonymous(authentication);
  157 | 		}
  158 | 
  159 | 	}
  160 | 
  161 | 	private static final class RememberMeAuthorizationStrategy extends AbstractAuthorizationStrategy {
  162 | 
  163 | 		@Override
  164 | 		boolean isGranted(Authentication authentication) {
  165 | 			return this.trustResolver.isRememberMe(authentication);
  166 | 		}
  167 | 
  168 | 	}
  169 | 
  170 | }
```
