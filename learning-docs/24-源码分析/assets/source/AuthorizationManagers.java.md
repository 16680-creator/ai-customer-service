<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authorization/AuthorizationManagers.java -->
<!-- 正文引用：[[源码:AuthorizationManagers.java#L起-L止]] ；代码块头部：(src: AuthorizationManagers.java#L起-L止) -->

# AuthorizationManagers.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authorization/AuthorizationManagers.java` |
| 行数 | 193 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:40.153Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | import java.util.function.Supplier;
   22 | 
   23 | import org.jspecify.annotations.Nullable;
   24 | 
   25 | import org.springframework.security.core.Authentication;
   26 | 
   27 | /**
   28 |  * A factory class to create an {@link AuthorizationManager} instances.
   29 |  *
   30 |  * @author Evgeniy Cheban
   31 |  * @author Josh Cummings
   32 |  * @since 5.8
   33 |  */
   34 | public final class AuthorizationManagers {
   35 | 
   36 | 	/**
   37 | 	 * Creates an {@link AuthorizationManager} that grants access if at least one
   38 | 	 * {@link AuthorizationManager} granted or abstained, if <code>managers</code> are
   39 | 	 * empty then denied decision is returned.
   40 | 	 * @param <T> the type of object that is being authorized
   41 | 	 * @param managers the {@link AuthorizationManager}s to use
   42 | 	 * @return the {@link AuthorizationManager} to use
   43 | 	 */
   44 | 	@SafeVarargs
   45 | 	public static <T> AuthorizationManager<T> anyOf(AuthorizationManager<T>... managers) {
   46 | 		return anyOf(new AuthorizationDecision(false), managers);
   47 | 	}
   48 | 
   49 | 	/**
   50 | 	 * Creates an {@link AuthorizationManager} that grants access if at least one
   51 | 	 * {@link AuthorizationManager} granted, if <code>managers</code> are empty or
   52 | 	 * abstained, a default {@link AuthorizationDecision} is returned.
   53 | 	 * @param <T> the type of object that is being authorized
   54 | 	 * @param allAbstainDefaultDecision the default decision if all
   55 | 	 * {@link AuthorizationManager}s abstained
   56 | 	 * @param managers the {@link AuthorizationManager}s to use
   57 | 	 * @return the {@link AuthorizationManager} to use
   58 | 	 * @since 6.3
   59 | 	 */
   60 | 	@SafeVarargs
   61 | 	public static <T> AuthorizationManager<T> anyOf(AuthorizationDecision allAbstainDefaultDecision,
   62 | 			AuthorizationManager<T>... managers) {
   63 | 		return (AuthorizationManagerCheckAdapter<T>) (Supplier<? extends @Nullable Authentication> authentication,
   64 | 				T object) -> {
   65 | 			List<AuthorizationResult> results = new ArrayList<>();
   66 | 			for (AuthorizationManager<T> manager : managers) {
   67 | 				AuthorizationResult result = manager.authorize(authentication, object);
   68 | 				if (result == null) {
   69 | 					continue;
   70 | 				}
   71 | 				if (result.isGranted()) {
   72 | 					return result;
   73 | 				}
   74 | 				results.add(result);
   75 | 			}
   76 | 			if (results.isEmpty()) {
   77 | 				return allAbstainDefaultDecision;
   78 | 			}
   79 | 			return new CompositeAuthorizationDecision(false, results);
   80 | 		};
   81 | 	}
   82 | 
   83 | 	/**
   84 | 	 * Creates an {@link AuthorizationManager} that grants access if all
   85 | 	 * {@link AuthorizationManager}s granted or abstained, if <code>managers</code> are
   86 | 	 * empty then granted decision is returned.
   87 | 	 * @param <T> the type of object that is being authorized
   88 | 	 * @param managers the {@link AuthorizationManager}s to use
   89 | 	 * @return the {@link AuthorizationManager} to use
   90 | 	 */
   91 | 	@SafeVarargs
   92 | 	public static <T> AuthorizationManager<T> allOf(AuthorizationManager<T>... managers) {
   93 | 		return allOf(new AuthorizationDecision(true), managers);
   94 | 	}
   95 | 
   96 | 	/**
   97 | 	 * Creates an {@link AuthorizationManager} that grants access if all
   98 | 	 * {@link AuthorizationManager}s granted, if <code>managers</code> are empty or
   99 | 	 * abstained, a default {@link AuthorizationDecision} is returned.
  100 | 	 * @param <T> the type of object that is being authorized
  101 | 	 * @param allAbstainDefaultDecision the default decision if all
  102 | 	 * {@link AuthorizationManager}s abstained
  103 | 	 * @param managers the {@link AuthorizationManager}s to use
  104 | 	 * @return the {@link AuthorizationManager} to use
  105 | 	 * @since 6.3
  106 | 	 */
  107 | 	@SafeVarargs
  108 | 	public static <T> AuthorizationManager<T> allOf(AuthorizationDecision allAbstainDefaultDecision,
  109 | 			AuthorizationManager<T>... managers) {
  110 | 		return (AuthorizationManagerCheckAdapter<T>) (Supplier<? extends @Nullable Authentication> authentication,
  111 | 				T object) -> {
  112 | 			List<AuthorizationResult> results = new ArrayList<>();
  113 | 			for (AuthorizationManager<T> manager : managers) {
  114 | 				AuthorizationResult result = manager.authorize(authentication, object);
  115 | 				if (result == null) {
  116 | 					continue;
  117 | 				}
  118 | 				if (!result.isGranted()) {
  119 | 					return result;
  120 | 				}
  121 | 				results.add(result);
  122 | 			}
  123 | 			if (results.isEmpty()) {
  124 | 				return allAbstainDefaultDecision;
  125 | 			}
  126 | 			return new CompositeAuthorizationDecision(true, results);
  127 | 		};
  128 | 	}
  129 | 
  130 | 	/**
  131 | 	 * Creates an {@link AuthorizationManager} that reverses whatever decision the given
  132 | 	 * {@link AuthorizationManager} granted. If the given {@link AuthorizationManager}
  133 | 	 * abstains, then the returned manager also abstains.
  134 | 	 * @param <T> the type of object that is being authorized
  135 | 	 * @param manager the {@link AuthorizationManager} to reverse
  136 | 	 * @return the reversing {@link AuthorizationManager}
  137 | 	 * @since 6.3
  138 | 	 */
  139 | 	public static <T> AuthorizationManager<T> not(AuthorizationManager<T> manager) {
  140 | 		return (Supplier<? extends @Nullable Authentication> authentication, T object) -> {
  141 | 			AuthorizationResult result = manager.authorize(authentication, object);
  142 | 			if (result == null) {
  143 | 				return null;
  144 | 			}
  145 | 			return new NotAuthorizationDecision(result);
  146 | 		};
  147 | 	}
  148 | 
  149 | 	private AuthorizationManagers() {
  150 | 	}
  151 | 
  152 | 	@SuppressWarnings("serial")
  153 | 	private static final class CompositeAuthorizationDecision extends AuthorizationDecision {
  154 | 
  155 | 		private final List<AuthorizationResult> results;
  156 | 
  157 | 		private CompositeAuthorizationDecision(boolean granted, List<AuthorizationResult> results) {
  158 | 			super(granted);
  159 | 			this.results = results;
  160 | 		}
  161 | 
  162 | 		@Override
  163 | 		public String toString() {
  164 | 			return "CompositeAuthorizationDecision [results=" + this.results + ']';
  165 | 		}
  166 | 
  167 | 	}
  168 | 
  169 | 	@SuppressWarnings("serial")
  170 | 	private static final class NotAuthorizationDecision extends AuthorizationDecision {
  171 | 
  172 | 		private final AuthorizationResult result;
  173 | 
  174 | 		private NotAuthorizationDecision(AuthorizationResult result) {
  175 | 			super(!result.isGranted());
  176 | 			this.result = result;
  177 | 		}
  178 | 
  179 | 		@Override
  180 | 		public String toString() {
  181 | 			return "NotAuthorizationDecision [result=" + this.result + ']';
  182 | 		}
  183 | 
  184 | 	}
  185 | 
  186 | 	private interface AuthorizationManagerCheckAdapter<T> extends AuthorizationManager<T> {
  187 | 
  188 | 		@Override
  189 | 		AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication, T object);
  190 | 
  191 | 	}
  192 | 
  193 | }
```
