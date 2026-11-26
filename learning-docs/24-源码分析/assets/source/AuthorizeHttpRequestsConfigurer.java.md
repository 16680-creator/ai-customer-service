<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/web/configurers/AuthorizeHttpRequestsConfigurer.java -->
<!-- 正文引用：[[源码:AuthorizeHttpRequestsConfigurer.java#L起-L止]] ；代码块头部：(src: AuthorizeHttpRequestsConfigurer.java#L起-L止) -->

# AuthorizeHttpRequestsConfigurer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/web/configurers/AuthorizeHttpRequestsConfigurer.java` |
| 行数 | 452 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:40:13.195Z |

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
   19 | import java.util.List;
   20 | import java.util.function.Function;
   21 | 
   22 | import jakarta.servlet.http.HttpServletRequest;
   23 | 
   24 | import org.springframework.beans.factory.ObjectProvider;
   25 | import org.springframework.context.ApplicationContext;
   26 | import org.springframework.core.ResolvableType;
   27 | import org.springframework.security.access.hierarchicalroles.NullRoleHierarchy;
   28 | import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
   29 | import org.springframework.security.authorization.AuthorizationDecision;
   30 | import org.springframework.security.authorization.AuthorizationEventPublisher;
   31 | import org.springframework.security.authorization.AuthorizationManager;
   32 | import org.springframework.security.authorization.AuthorizationManagerFactory;
   33 | import org.springframework.security.authorization.AuthorizationManagers;
   34 | import org.springframework.security.authorization.DefaultAuthorizationManagerFactory;
   35 | import org.springframework.security.authorization.SpringAuthorizationEventPublisher;
   36 | import org.springframework.security.config.ObjectPostProcessor;
   37 | import org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry;
   38 | import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
   39 | import org.springframework.security.config.core.GrantedAuthorityDefaults;
   40 | import org.springframework.security.core.Authentication;
   41 | import org.springframework.security.web.access.intercept.AuthorizationFilter;
   42 | import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
   43 | import org.springframework.security.web.access.intercept.RequestMatcherDelegatingAuthorizationManager;
   44 | import org.springframework.security.web.util.matcher.RequestMatcher;
   45 | import org.springframework.security.web.util.matcher.RequestMatcherEntry;
   46 | import org.springframework.util.Assert;
   47 | 
   48 | /**
   49 |  * Adds a URL based authorization using {@link AuthorizationManager}.
   50 |  *
   51 |  * @param <H> the type of {@link HttpSecurityBuilder} that is being configured.
   52 |  * @author Evgeniy Cheban
   53 |  * @author Steve Riesenberg
   54 |  * @since 5.5
   55 |  */
   56 | public final class AuthorizeHttpRequestsConfigurer<H extends HttpSecurityBuilder<H>>
   57 | 		extends AbstractHttpConfigurer<AuthorizeHttpRequestsConfigurer<H>, H> {
   58 | 
   59 | 	private final AuthorizationManagerRequestMatcherRegistry registry;
   60 | 
   61 | 	private final AuthorizationEventPublisher publisher;
   62 | 
   63 | 	private final AuthorizationManagerFactory<? super RequestAuthorizationContext> authorizationManagerFactory;
   64 | 
   65 | 	private ObjectPostProcessor<AuthorizationManager<HttpServletRequest>> postProcessor = ObjectPostProcessor
   66 | 		.identity();
   67 | 
   68 | 	/**
   69 | 	 * Creates an instance.
   70 | 	 * @param context the {@link ApplicationContext} to use
   71 | 	 */
   72 | 	public AuthorizeHttpRequestsConfigurer(ApplicationContext context) {
   73 | 		this.registry = new AuthorizationManagerRequestMatcherRegistry(context);
   74 | 		if (context.getBeanNamesForType(AuthorizationEventPublisher.class).length > 0) {
   75 | 			this.publisher = context.getBean(AuthorizationEventPublisher.class);
   76 | 		}
   77 | 		else {
   78 | 			this.publisher = new SpringAuthorizationEventPublisher(context);
   79 | 		}
   80 | 		this.authorizationManagerFactory = getAuthorizationManagerFactory(context);
   81 | 		ResolvableType type = ResolvableType.forClassWithGenerics(ObjectPostProcessor.class,
   82 | 				ResolvableType.forClassWithGenerics(AuthorizationManager.class, HttpServletRequest.class));
   83 | 		ObjectProvider<ObjectPostProcessor<AuthorizationManager<HttpServletRequest>>> provider = context
   84 | 			.getBeanProvider(type);
   85 | 		provider.ifUnique((postProcessor) -> this.postProcessor = postProcessor);
   86 | 	}
   87 | 
   88 | 	private AuthorizationManagerFactory<? super RequestAuthorizationContext> getAuthorizationManagerFactory(
   89 | 			ApplicationContext context) {
   90 | 		ResolvableType authorizationManagerFactoryType = ResolvableType
   91 | 			.forClassWithGenerics(AuthorizationManagerFactory.class, RequestAuthorizationContext.class);
   92 | 
   93 | 		// Handle fallback to generic type
   94 | 		if (context.getBeanNamesForType(authorizationManagerFactoryType).length == 0) {
   95 | 			authorizationManagerFactoryType = ResolvableType.forClassWithGenerics(AuthorizationManagerFactory.class,
   96 | 					Object.class);
   97 | 		}
   98 | 
   99 | 		ObjectProvider<AuthorizationManagerFactory<RequestAuthorizationContext>> authorizationManagerFactoryProvider = context
  100 | 			.getBeanProvider(authorizationManagerFactoryType);
  101 | 
  102 | 		return authorizationManagerFactoryProvider.getIfAvailable(() -> {
  103 | 			RoleHierarchy roleHierarchy = context.getBeanProvider(RoleHierarchy.class)
  104 | 				.getIfAvailable(NullRoleHierarchy::new);
  105 | 			GrantedAuthorityDefaults grantedAuthorityDefaults = context.getBeanProvider(GrantedAuthorityDefaults.class)
  106 | 				.getIfAvailable();
  107 | 			String rolePrefix = (grantedAuthorityDefaults != null) ? grantedAuthorityDefaults.getRolePrefix() : "ROLE_";
  108 | 
  109 | 			DefaultAuthorizationManagerFactory<RequestAuthorizationContext> authorizationManagerFactory = new DefaultAuthorizationManagerFactory<>();
  110 | 			authorizationManagerFactory.setRoleHierarchy(roleHierarchy);
  111 | 			authorizationManagerFactory.setRolePrefix(rolePrefix);
  112 | 
  113 | 			return authorizationManagerFactory;
  114 | 		});
  115 | 	}
  116 | 
  117 | 	/**
  118 | 	 * The {@link AuthorizationManagerRequestMatcherRegistry} is what users will interact
  119 | 	 * with after applying the {@link AuthorizeHttpRequestsConfigurer}.
  120 | 	 * @return the {@link AuthorizationManagerRequestMatcherRegistry} for further
  121 | 	 * customizations
  122 | 	 */
  123 | 	public AuthorizationManagerRequestMatcherRegistry getRegistry() {
  124 | 		return this.registry;
  125 | 	}
  126 | 
  127 | 	@Override
  128 | 	public void configure(H http) {
  129 | 		AuthorizationManager<HttpServletRequest> authorizationManager = this.registry.createAuthorizationManager();
  130 | 		AuthorizationFilter authorizationFilter = new AuthorizationFilter(authorizationManager);
  131 | 		authorizationFilter.setAuthorizationEventPublisher(this.publisher);
  132 | 		authorizationFilter.setSecurityContextHolderStrategy(getSecurityContextHolderStrategy());
  133 | 		http.addFilter(postProcess(authorizationFilter));
  134 | 	}
  135 | 
  136 | 	private AuthorizationManagerRequestMatcherRegistry addMapping(List<? extends RequestMatcher> matchers,
  137 | 			AuthorizationManager<? super RequestAuthorizationContext> manager) {
  138 | 		for (RequestMatcher matcher : matchers) {
  139 | 			this.registry.addMapping(matcher, manager);
  140 | 		}
  141 | 		return this.registry;
  142 | 	}
  143 | 
  144 | 	AuthorizationManagerRequestMatcherRegistry addFirst(RequestMatcher matcher,
  145 | 			AuthorizationManager<RequestAuthorizationContext> manager) {
  146 | 		this.registry.addFirst(matcher, manager);
  147 | 		return this.registry;
  148 | 	}
  149 | 
  150 | 	/**
  151 | 	 * Registry for mapping a {@link RequestMatcher} to an {@link AuthorizationManager}.
  152 | 	 *
  153 | 	 * @author Evgeniy Cheban
  154 | 	 */
  155 | 	public final class AuthorizationManagerRequestMatcherRegistry
  156 | 			extends AbstractRequestMatcherRegistry<AuthorizedUrl> {
  157 | 
  158 | 		private final RequestMatcherDelegatingAuthorizationManager.Builder managerBuilder = RequestMatcherDelegatingAuthorizationManager
  159 | 			.builder();
  160 | 
  161 | 		private List<RequestMatcher> unmappedMatchers;
  162 | 
  163 | 		private int mappingCount;
  164 | 
  165 | 		private AuthorizationManagerRequestMatcherRegistry(ApplicationContext context) {
  166 | 			setApplicationContext(context);
  167 | 		}
  168 | 
  169 | 		private void addMapping(RequestMatcher matcher,
  170 | 				AuthorizationManager<? super RequestAuthorizationContext> manager) {
  171 | 			this.unmappedMatchers = null;
  172 | 			this.managerBuilder.add(matcher, manager);
  173 | 			this.mappingCount++;
  174 | 		}
  175 | 
  176 | 		private void addFirst(RequestMatcher matcher,
  177 | 				AuthorizationManager<? super RequestAuthorizationContext> manager) {
  178 | 			this.unmappedMatchers = null;
  179 | 			this.managerBuilder.mappings((m) -> m.add(0, new RequestMatcherEntry<>(matcher, manager)));
  180 | 			this.mappingCount++;
  181 | 		}
  182 | 
  183 | 		private AuthorizationManager<HttpServletRequest> createAuthorizationManager() {
  184 | 			Assert.state(this.unmappedMatchers == null,
  185 | 					() -> "An incomplete mapping was found for " + this.unmappedMatchers
  186 | 							+ ". Try completing it with something like requestUrls().<something>.hasRole('USER')");
  187 | 			Assert.state(this.mappingCount > 0,
  188 | 					"At least one mapping is required (for example, authorizeHttpRequests().anyRequest().authenticated())");
  189 | 			AuthorizationManager<HttpServletRequest> manager = postProcess(
  190 | 					(AuthorizationManager<HttpServletRequest>) this.managerBuilder.build());
  191 | 			return AuthorizeHttpRequestsConfigurer.this.postProcessor.postProcess(manager);
  192 | 		}
  193 | 
  194 | 		@Override
  195 | 		protected AuthorizedUrl chainRequestMatchers(List<RequestMatcher> requestMatchers) {
  196 | 			this.unmappedMatchers = requestMatchers;
  197 | 			return new AuthorizedUrl(requestMatchers, AuthorizeHttpRequestsConfigurer.this.authorizationManagerFactory);
  198 | 		}
  199 | 
  200 | 		/**
  201 | 		 * Adds an {@link ObjectPostProcessor} for this class.
  202 | 		 * @param objectPostProcessor the {@link ObjectPostProcessor} to use
  203 | 		 * @return the {@link AuthorizationManagerRequestMatcherRegistry} for further
  204 | 		 * customizations
  205 | 		 */
  206 | 		public AuthorizationManagerRequestMatcherRegistry withObjectPostProcessor(
  207 | 				ObjectPostProcessor<?> objectPostProcessor) {
  208 | 			addObjectPostProcessor(objectPostProcessor);
  209 | 			return this;
  210 | 		}
  211 | 
  212 | 	}
  213 | 
  214 | 	/**
  215 | 	 * An object that allows configuring the {@link AuthorizationManager} for
  216 | 	 * {@link RequestMatcher}s.
  217 | 	 *
  218 | 	 * @author Evgeniy Cheban
  219 | 	 * @author Josh Cummings
  220 | 	 */
  221 | 	public class AuthorizedUrl {
  222 | 
  223 | 		private final List<? extends RequestMatcher> matchers;
  224 | 
  225 | 		private AuthorizationManagerFactory<? super RequestAuthorizationContext> authorizationManagerFactory;
  226 | 
  227 | 		private boolean not;
  228 | 
  229 | 		/**
  230 | 		 * Creates an instance.
  231 | 		 * @param matchers the {@link RequestMatcher} instances to map
  232 | 		 * @param authorizationManagerFactory the {@link AuthorizationManagerFactory} for
  233 | 		 * creating instances of {@link AuthorizationManager}
  234 | 		 */
  235 | 		AuthorizedUrl(List<? extends RequestMatcher> matchers,
  236 | 				AuthorizationManagerFactory<? super RequestAuthorizationContext> authorizationManagerFactory) {
  237 | 			this.matchers = matchers;
  238 | 			this.authorizationManagerFactory = authorizationManagerFactory;
  239 | 		}
  240 | 
  241 | 		protected List<? extends RequestMatcher> getMatchers() {
  242 | 			return this.matchers;
  243 | 		}
  244 | 
  245 | 		void setAuthorizationManagerFactory(
  246 | 				AuthorizationManagerFactory<? super RequestAuthorizationContext> authorizationManagerFactory) {
  247 | 			this.authorizationManagerFactory = authorizationManagerFactory;
  248 | 		}
  249 | 
  250 | 		/**
  251 | 		 * Negates the following authorization rule.
  252 | 		 * @return the {@link AuthorizedUrl} for further customization
  253 | 		 * @since 6.3
  254 | 		 */
  255 | 		public AuthorizedUrl not() {
  256 | 			this.not = true;
  257 | 			return this;
  258 | 		}
  259 | 
  260 | 		/**
  261 | 		 * Specify that URLs are allowed by anyone.
  262 | 		 * @return the {@link AuthorizationManagerRequestMatcherRegistry} for further
  263 | 		 * customizations
  264 | 		 */
  265 | 		public AuthorizationManagerRequestMatcherRegistry permitAll() {
  266 | 			return access(this.authorizationManagerFactory.permitAll());
  267 | 		}
  268 | 
  269 | 		/**
  270 | 		 * Specify that URLs are not allowed by anyone.
  271 | 		 * @return the {@link AuthorizationManagerRequestMatcherRegistry} for further
  272 | 		 * customizations
  273 | 		 */
  274 | 		public AuthorizationManagerRequestMatcherRegistry denyAll() {
  275 | 			return access(this.authorizationManagerFactory.denyAll());
  276 | 		}
  277 | 
  278 | 		/**
  279 | 		 * Specifies a user requires a role.
  280 | 		 * @param role the role that should be required which is prepended with ROLE_
  281 | 		 * automatically (i.e. USER, ADMIN, etc). It should not start with ROLE_
  282 | 		 * @return {@link AuthorizationManagerRequestMatcherRegistry} for further
  283 | 		 * customizations
  284 | 		 */
  285 | 		public AuthorizationManagerRequestMatcherRegistry hasRole(String role) {
  286 | 			return access(this.authorizationManagerFactory.hasRole(role));
  287 | 		}
  288 | 
  289 | 		/**
  290 | 		 * Specifies that a user requires one of many roles.
  291 | 		 * @param roles the roles that the user should have at least one of (i.e. ADMIN,
  292 | 		 * USER, etc). Each role should not start with ROLE_ since it is automatically
  293 | 		 * prepended already
  294 | 		 * @return the {@link AuthorizationManagerRequestMatcherRegistry} for further
  295 | 		 * customizations
  296 | 		 */
  297 | 		public AuthorizationManagerRequestMatcherRegistry hasAnyRole(String... roles) {
  298 | 			return access(this.authorizationManagerFactory.hasAnyRole(roles));
  299 | 		}
  300 | 
  301 | 		/**
  302 | 		 * Specifies that a user requires all the provided roles.
  303 | 		 * @param roles the roles that the user should have (i.e. ADMIN, USER, etc). Each
  304 | 		 * role should not start with ROLE_ since it is automatically prepended already
  305 | 		 * @return the {@link AuthorizationManagerRequestMatcherRegistry} for further
  306 | 		 * customizations
  307 | 		 */
  308 | 		public AuthorizationManagerRequestMatcherRegistry hasAllRoles(String... roles) {
  309 | 			return access(this.authorizationManagerFactory.hasAllRoles(roles));
  310 | 		}
  311 | 
  312 | 		/**
  313 | 		 * Specifies a user requires an authority.
  314 | 		 * @param authority the authority that should be required
  315 | 		 * @return the {@link AuthorizationManagerRequestMatcherRegistry} for further
  316 | 		 * customizations
  317 | 		 */
  318 | 		public AuthorizationManagerRequestMatcherRegistry hasAuthority(String authority) {
  319 | 			return access(this.authorizationManagerFactory.hasAuthority(authority));
  320 | 		}
  321 | 
  322 | 		/**
  323 | 		 * Specifies that a user requires one of many authorities.
  324 | 		 * @param authorities the authorities that the user should have at least one of
  325 | 		 * (i.e. ROLE_USER, ROLE_ADMIN, etc)
  326 | 		 * @return the {@link AuthorizationManagerRequestMatcherRegistry} for further
  327 | 		 * customizations
  328 | 		 */
  329 | 		public AuthorizationManagerRequestMatcherRegistry hasAnyAuthority(String... authorities) {
  330 | 			return access(this.authorizationManagerFactory.hasAnyAuthority(authorities));
  331 | 		}
  332 | 
  333 | 		/**
  334 | 		 * Specifies that a user requires all the provided authorities.
  335 | 		 * @param authorities the authorities that the user should have (i.e. ROLE_USER,
  336 | 		 * ROLE_ADMIN, etc)
  337 | 		 * @return the {@link AuthorizationManagerRequestMatcherRegistry} for further
  338 | 		 * customizations
  339 | 		 */
  340 | 		public AuthorizationManagerRequestMatcherRegistry hasAllAuthorities(String... authorities) {
  341 | 			return access(this.authorizationManagerFactory.hasAllAuthorities(authorities));
  342 | 		}
  343 | 
  344 | 		/**
  345 | 		 * Specify that URLs are allowed by any authenticated user.
  346 | 		 * @return the {@link AuthorizationManagerRequestMatcherRegistry} for further
  347 | 		 * customizations
  348 | 		 */
  349 | 		public AuthorizationManagerRequestMatcherRegistry authenticated() {
  350 | 			return access(this.authorizationManagerFactory.authenticated());
  351 | 		}
  352 | 
  353 | 		/**
  354 | 		 * Specify that URLs are allowed by users who have authenticated and were not
  355 | 		 * "remembered".
  356 | 		 * @return the {@link AuthorizationManagerRequestMatcherRegistry} for further
  357 | 		 * customization
  358 | 		 * @since 5.8
  359 | 		 * @see RememberMeConfigurer
  360 | 		 */
  361 | 		public AuthorizationManagerRequestMatcherRegistry fullyAuthenticated() {
  362 | 			return access(this.authorizationManagerFactory.fullyAuthenticated());
  363 | 		}
  364 | 
  365 | 		/**
  366 | 		 * Specify that URLs are allowed by users that have been remembered.
  367 | 		 * @return the {@link AuthorizationManagerRequestMatcherRegistry} for further
  368 | 		 * customization
  369 | 		 * @since 5.8
  370 | 		 * @see RememberMeConfigurer
  371 | 		 */
  372 | 		public AuthorizationManagerRequestMatcherRegistry rememberMe() {
  373 | 			return access(this.authorizationManagerFactory.rememberMe());
  374 | 		}
  375 | 
  376 | 		/**
  377 | 		 * Specify that URLs are allowed by anonymous users.
  378 | 		 * @return the {@link AuthorizationManagerRequestMatcherRegistry} for further
  379 | 		 * customization
  380 | 		 * @since 5.8
  381 | 		 */
  382 | 		public AuthorizationManagerRequestMatcherRegistry anonymous() {
  383 | 			return access(this.authorizationManagerFactory.anonymous());
  384 | 		}
  385 | 
  386 | 		/**
  387 | 		 * Specify that a path variable in URL to be compared.
  388 | 		 *
  389 | 		 * <p>
  390 | 		 * For example, <pre>
  391 | 		 * requestMatchers("/user/{username}").hasVariable("username").equalTo(Authentication::getName)
  392 | 		 * </pre>
  393 | 		 * @param variable the variable in URL template to compare.
  394 | 		 * @return {@link AuthorizedUrlVariable} for further customization.
  395 | 		 * @since 6.3
  396 | 		 */
  397 | 		public AuthorizedUrlVariable hasVariable(String variable) {
  398 | 			return new AuthorizedUrlVariable(variable);
  399 | 		}
  400 | 
  401 | 		/**
  402 | 		 * Allows specifying a custom {@link AuthorizationManager}.
  403 | 		 * @param manager the {@link AuthorizationManager} to use
  404 | 		 * @return the {@link AuthorizationManagerRequestMatcherRegistry} for further
  405 | 		 * customizations
  406 | 		 */
  407 | 		public AuthorizationManagerRequestMatcherRegistry access(
  408 | 				AuthorizationManager<? super RequestAuthorizationContext> manager) {
  409 | 			Assert.notNull(manager, "manager cannot be null");
  410 | 			return (this.not)
  411 | 					? AuthorizeHttpRequestsConfigurer.this.addMapping(this.matchers, AuthorizationManagers.not(manager))
  412 | 					: AuthorizeHttpRequestsConfigurer.this.addMapping(this.matchers, manager);
  413 | 		}
  414 | 
  415 | 		/**
  416 | 		 * An object that allows configuring {@link RequestMatcher}s with URI path
  417 | 		 * variables.
  418 | 		 *
  419 | 		 * @author Taehong Kim
  420 | 		 * @since 6.3
  421 | 		 */
  422 | 		public final class AuthorizedUrlVariable {
  423 | 
  424 | 			private final String variable;
  425 | 
  426 | 			private AuthorizedUrlVariable(String variable) {
  427 | 				this.variable = variable;
  428 | 			}
  429 | 
  430 | 			/**
  431 | 			 * Compares the value of a path variable in the URI with an `Authentication`
  432 | 			 * attribute
  433 | 			 * <p>
  434 | 			 * For example, <pre>
  435 | 			 * requestMatchers("/user/{username}").hasVariable("username").equalTo(Authentication::getName));
  436 | 			 * </pre>.
  437 | 			 * @param function a function to get value from {@link Authentication}.
  438 | 			 * @return the {@link AuthorizationManagerRequestMatcherRegistry} for further
  439 | 			 * customization.
  440 | 			 */
  441 | 			public AuthorizationManagerRequestMatcherRegistry equalTo(Function<Authentication, String> function) {
  442 | 				return access((auth, requestContext) -> {
  443 | 					String value = requestContext.getVariables().get(this.variable);
  444 | 					return new AuthorizationDecision(function.apply(auth.get()).equals(value));
  445 | 				});
  446 | 			}
  447 | 
  448 | 		}
  449 | 
  450 | 	}
  451 | 
  452 | }
```
