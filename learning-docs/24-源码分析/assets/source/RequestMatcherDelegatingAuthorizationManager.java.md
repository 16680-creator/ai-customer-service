<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/access/intercept/RequestMatcherDelegatingAuthorizationManager.java -->
<!-- 正文引用：[[源码:RequestMatcherDelegatingAuthorizationManager.java#L起-L止]] ；代码块头部：(src: RequestMatcherDelegatingAuthorizationManager.java#L起-L止) -->

# RequestMatcherDelegatingAuthorizationManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/access/intercept/RequestMatcherDelegatingAuthorizationManager.java` |
| 行数 | 292 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:28.556Z |

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
   17 | package org.springframework.security.web.access.intercept;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | import java.util.function.Consumer;
   22 | import java.util.function.Supplier;
   23 | 
   24 | import jakarta.servlet.http.HttpServletRequest;
   25 | import org.apache.commons.logging.Log;
   26 | import org.apache.commons.logging.LogFactory;
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.core.log.LogMessage;
   30 | import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
   31 | import org.springframework.security.authorization.AuthorityAuthorizationManager;
   32 | import org.springframework.security.authorization.AuthorizationDecision;
   33 | import org.springframework.security.authorization.AuthorizationManager;
   34 | import org.springframework.security.authorization.AuthorizationResult;
   35 | import org.springframework.security.authorization.SingleResultAuthorizationManager;
   36 | import org.springframework.security.core.Authentication;
   37 | import org.springframework.security.web.util.UrlUtils;
   38 | import org.springframework.security.web.util.matcher.AnyRequestMatcher;
   39 | import org.springframework.security.web.util.matcher.RequestMatcher;
   40 | import org.springframework.security.web.util.matcher.RequestMatcher.MatchResult;
   41 | import org.springframework.security.web.util.matcher.RequestMatcherEntry;
   42 | import org.springframework.util.Assert;
   43 | 
   44 | /**
   45 |  * An {@link AuthorizationManager} which delegates to a specific
   46 |  * {@link AuthorizationManager} based on a {@link RequestMatcher} evaluation.
   47 |  *
   48 |  * @author Evgeniy Cheban
   49 |  * @author Parikshit Dutta
   50 |  * @since 5.5
   51 |  */
   52 | public final class RequestMatcherDelegatingAuthorizationManager implements AuthorizationManager<HttpServletRequest> {
   53 | 
   54 | 	private static final AuthorizationDecision DENY = new AuthorizationDecision(false);
   55 | 
   56 | 	private final Log logger = LogFactory.getLog(getClass());
   57 | 
   58 | 	private final List<RequestMatcherEntry<AuthorizationManager<? super RequestAuthorizationContext>>> mappings;
   59 | 
   60 | 	private RequestMatcherDelegatingAuthorizationManager(
   61 | 			List<RequestMatcherEntry<AuthorizationManager<? super RequestAuthorizationContext>>> mappings) {
   62 | 		Assert.notEmpty(mappings, "mappings cannot be empty");
   63 | 		this.mappings = mappings;
   64 | 	}
   65 | 
   66 | 	@Override
   67 | 	public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication,
   68 | 			HttpServletRequest request) {
   69 | 		if (this.logger.isTraceEnabled()) {
   70 | 			this.logger.trace(LogMessage.format("Authorizing %s", requestLine(request)));
   71 | 		}
   72 | 		for (RequestMatcherEntry<AuthorizationManager<? super RequestAuthorizationContext>> mapping : this.mappings) {
   73 | 
   74 | 			RequestMatcher matcher = mapping.getRequestMatcher();
   75 | 			MatchResult matchResult = matcher.matcher(request);
   76 | 			if (matchResult.isMatch()) {
   77 | 				AuthorizationManager<? super RequestAuthorizationContext> manager = mapping.getEntry();
   78 | 				if (this.logger.isTraceEnabled()) {
   79 | 					this.logger.trace(
   80 | 							LogMessage.format("Checking authorization on %s using %s", requestLine(request), manager));
   81 | 				}
   82 | 				return manager.authorize(authentication,
   83 | 						new RequestAuthorizationContext(request, matchResult.getVariables()));
   84 | 			}
   85 | 		}
   86 | 		if (this.logger.isTraceEnabled()) {
   87 | 			this.logger.trace(LogMessage.of(() -> "Denying request since did not find matching RequestMatcher"));
   88 | 		}
   89 | 		return DENY;
   90 | 	}
   91 | 
   92 | 	private static String requestLine(HttpServletRequest request) {
   93 | 		return request.getMethod() + " " + UrlUtils.buildRequestUrl(request);
   94 | 	}
   95 | 
   96 | 	/**
   97 | 	 * Creates a builder for {@link RequestMatcherDelegatingAuthorizationManager}.
   98 | 	 * @return the new {@link Builder} instance
   99 | 	 */
  100 | 	public static Builder builder() {
  101 | 		return new Builder();
  102 | 	}
  103 | 
  104 | 	/**
  105 | 	 * A builder for {@link RequestMatcherDelegatingAuthorizationManager}.
  106 | 	 */
  107 | 	public static final class Builder {
  108 | 
  109 | 		private boolean anyRequestConfigured;
  110 | 
  111 | 		private final List<RequestMatcherEntry<AuthorizationManager<? super RequestAuthorizationContext>>> mappings = new ArrayList<>();
  112 | 
  113 | 		/**
  114 | 		 * Maps a {@link RequestMatcher} to an {@link AuthorizationManager}.
  115 | 		 * @param matcher the {@link RequestMatcher} to use
  116 | 		 * @param manager the {@link AuthorizationManager} to use
  117 | 		 * @return the {@link Builder} for further customizations
  118 | 		 */
  119 | 		public Builder add(RequestMatcher matcher, AuthorizationManager<? super RequestAuthorizationContext> manager) {
  120 | 			Assert.state(!this.anyRequestConfigured, "Can't add mappings after anyRequest");
  121 | 			Assert.notNull(matcher, "matcher cannot be null");
  122 | 			Assert.notNull(manager, "manager cannot be null");
  123 | 			this.mappings.add(new RequestMatcherEntry<>(matcher, manager));
  124 | 			return this;
  125 | 		}
  126 | 
  127 | 		/**
  128 | 		 * Allows to configure the {@link RequestMatcher} to {@link AuthorizationManager}
  129 | 		 * mappings.
  130 | 		 * @param mappingsConsumer used to configure the {@link RequestMatcher} to
  131 | 		 * {@link AuthorizationManager} mappings.
  132 | 		 * @return the {@link Builder} for further customizations
  133 | 		 * @since 5.7
  134 | 		 */
  135 | 		public Builder mappings(
  136 | 				Consumer<List<RequestMatcherEntry<AuthorizationManager<? super RequestAuthorizationContext>>>> mappingsConsumer) {
  137 | 			Assert.state(!this.anyRequestConfigured, "Can't configure mappings after anyRequest");
  138 | 			Assert.notNull(mappingsConsumer, "mappingsConsumer cannot be null");
  139 | 			mappingsConsumer.accept(this.mappings);
  140 | 			return this;
  141 | 		}
  142 | 
  143 | 		/**
  144 | 		 * Maps any request.
  145 | 		 * @return the {@link AuthorizedUrl} for further customizations
  146 | 		 * @since 6.2
  147 | 		 */
  148 | 		public AuthorizedUrl anyRequest() {
  149 | 			Assert.state(!this.anyRequestConfigured, "Can't configure anyRequest after itself");
  150 | 			this.anyRequestConfigured = true;
  151 | 			return new AuthorizedUrl(AnyRequestMatcher.INSTANCE);
  152 | 		}
  153 | 
  154 | 		/**
  155 | 		 * Maps {@link RequestMatcher}s to {@link AuthorizationManager}.
  156 | 		 * @param matchers the {@link RequestMatcher}s to map
  157 | 		 * @return the {@link AuthorizedUrl} for further customizations
  158 | 		 * @since 6.2
  159 | 		 */
  160 | 		public AuthorizedUrl requestMatchers(RequestMatcher... matchers) {
  161 | 			Assert.state(!this.anyRequestConfigured, "Can't configure requestMatchers after anyRequest");
  162 | 			return new AuthorizedUrl(matchers);
  163 | 		}
  164 | 
  165 | 		/**
  166 | 		 * Creates a {@link RequestMatcherDelegatingAuthorizationManager} instance.
  167 | 		 * @return the {@link RequestMatcherDelegatingAuthorizationManager} instance
  168 | 		 */
  169 | 		public RequestMatcherDelegatingAuthorizationManager build() {
  170 | 			return new RequestMatcherDelegatingAuthorizationManager(this.mappings);
  171 | 		}
  172 | 
  173 | 		/**
  174 | 		 * An object that allows configuring the {@link AuthorizationManager} for
  175 | 		 * {@link RequestMatcher}s.
  176 | 		 *
  177 | 		 * @author Evgeniy Cheban
  178 | 		 * @since 6.2
  179 | 		 */
  180 | 		public final class AuthorizedUrl {
  181 | 
  182 | 			private final List<RequestMatcher> matchers;
  183 | 
  184 | 			private AuthorizedUrl(RequestMatcher... matchers) {
  185 | 				this(List.of(matchers));
  186 | 			}
  187 | 
  188 | 			private AuthorizedUrl(List<RequestMatcher> matchers) {
  189 | 				this.matchers = matchers;
  190 | 			}
  191 | 
  192 | 			/**
  193 | 			 * Specify that URLs are allowed by anyone.
  194 | 			 * @return the {@link Builder} for further customizations
  195 | 			 */
  196 | 			public Builder permitAll() {
  197 | 				return access(SingleResultAuthorizationManager.permitAll());
  198 | 			}
  199 | 
  200 | 			/**
  201 | 			 * Specify that URLs are not allowed by anyone.
  202 | 			 * @return the {@link Builder} for further customizations
  203 | 			 */
  204 | 			public Builder denyAll() {
  205 | 				return access(SingleResultAuthorizationManager.denyAll());
  206 | 			}
  207 | 
  208 | 			/**
  209 | 			 * Specify that URLs are allowed by any authenticated user.
  210 | 			 * @return the {@link Builder} for further customizations
  211 | 			 */
  212 | 			public Builder authenticated() {
  213 | 				return access(AuthenticatedAuthorizationManager.authenticated());
  214 | 			}
  215 | 
  216 | 			/**
  217 | 			 * Specify that URLs are allowed by users who have authenticated and were not
  218 | 			 * "remembered".
  219 | 			 * @return the {@link Builder} for further customization
  220 | 			 */
  221 | 			public Builder fullyAuthenticated() {
  222 | 				return access(AuthenticatedAuthorizationManager.fullyAuthenticated());
  223 | 			}
  224 | 
  225 | 			/**
  226 | 			 * Specify that URLs are allowed by users that have been remembered.
  227 | 			 * @return the {@link Builder} for further customization
  228 | 			 */
  229 | 			public Builder rememberMe() {
  230 | 				return access(AuthenticatedAuthorizationManager.rememberMe());
  231 | 			}
  232 | 
  233 | 			/**
  234 | 			 * Specify that URLs are allowed by anonymous users.
  235 | 			 * @return the {@link Builder} for further customization
  236 | 			 */
  237 | 			public Builder anonymous() {
  238 | 				return access(AuthenticatedAuthorizationManager.anonymous());
  239 | 			}
  240 | 
  241 | 			/**
  242 | 			 * Specifies a user requires a role.
  243 | 			 * @param role the role that should be required which is prepended with ROLE_
  244 | 			 * automatically (i.e. USER, ADMIN, etc). It should not start with ROLE_
  245 | 			 * @return {@link Builder} for further customizations
  246 | 			 */
  247 | 			public Builder hasRole(String role) {
  248 | 				return access(AuthorityAuthorizationManager.hasRole(role));
  249 | 			}
  250 | 
  251 | 			/**
  252 | 			 * Specifies that a user requires one of many roles.
  253 | 			 * @param roles the roles that the user should have at least one of (i.e.
  254 | 			 * ADMIN, USER, etc). Each role should not start with ROLE_ since it is
  255 | 			 * automatically prepended already
  256 | 			 * @return the {@link Builder} for further customizations
  257 | 			 */
  258 | 			public Builder hasAnyRole(String... roles) {
  259 | 				return access(AuthorityAuthorizationManager.hasAnyRole(roles));
  260 | 			}
  261 | 
  262 | 			/**
  263 | 			 * Specifies a user requires an authority.
  264 | 			 * @param authority the authority that should be required
  265 | 			 * @return the {@link Builder} for further customizations
  266 | 			 */
  267 | 			public Builder hasAuthority(String authority) {
  268 | 				return access(AuthorityAuthorizationManager.hasAuthority(authority));
  269 | 			}
  270 | 
  271 | 			/**
  272 | 			 * Specifies that a user requires one of many authorities.
  273 | 			 * @param authorities the authorities that the user should have at least one
  274 | 			 * of (i.e. ROLE_USER, ROLE_ADMIN, etc)
  275 | 			 * @return the {@link Builder} for further customizations
  276 | 			 */
  277 | 			public Builder hasAnyAuthority(String... authorities) {
  278 | 				return access(AuthorityAuthorizationManager.hasAnyAuthority(authorities));
  279 | 			}
  280 | 
  281 | 			private Builder access(AuthorizationManager<RequestAuthorizationContext> manager) {
  282 | 				for (RequestMatcher matcher : this.matchers) {
  283 | 					Builder.this.mappings.add(new RequestMatcherEntry<>(matcher, manager));
  284 | 				}
  285 | 				return Builder.this;
  286 | 			}
  287 | 
  288 | 		}
  289 | 
  290 | 	}
  291 | 
  292 | }
```
