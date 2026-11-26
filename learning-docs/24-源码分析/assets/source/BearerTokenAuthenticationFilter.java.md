<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-oauth2-resource-server:7.2.0 | path: org/springframework/security/oauth2/server/resource/web/authentication/BearerTokenAuthenticationFilter.java -->
<!-- 正文引用：[[源码:BearerTokenAuthenticationFilter.java#L起-L止]] ；代码块头部：(src: BearerTokenAuthenticationFilter.java#L起-L止) -->

# BearerTokenAuthenticationFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-oauth2-resource-server:7.2.0` |
| 路径 | `org/springframework/security/oauth2/server/resource/web/authentication/BearerTokenAuthenticationFilter.java` |
| 行数 | 327 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-oauth2-resource-server/7.2.0/spring-security-oauth2-resource-server-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:01.570Z |

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
   17 | package org.springframework.security.oauth2.server.resource.web.authentication;
   18 | 
   19 | import java.io.IOException;
   20 | import java.lang.reflect.Method;
   21 | import java.util.Map;
   22 | import java.util.Set;
   23 | import java.util.stream.Collectors;
   24 | 
   25 | import jakarta.servlet.FilterChain;
   26 | import jakarta.servlet.ServletException;
   27 | import jakarta.servlet.http.HttpServletRequest;
   28 | import jakarta.servlet.http.HttpServletResponse;
   29 | 
   30 | import org.springframework.core.log.LogMessage;
   31 | import org.springframework.security.authentication.AuthenticationDetailsSource;
   32 | import org.springframework.security.authentication.AuthenticationManager;
   33 | import org.springframework.security.authentication.AuthenticationManagerResolver;
   34 | import org.springframework.security.core.Authentication;
   35 | import org.springframework.security.core.AuthenticationException;
   36 | import org.springframework.security.core.GrantedAuthority;
   37 | import org.springframework.security.core.context.SecurityContext;
   38 | import org.springframework.security.core.context.SecurityContextHolder;
   39 | import org.springframework.security.core.context.SecurityContextHolderStrategy;
   40 | import org.springframework.security.oauth2.core.ClaimAccessor;
   41 | import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
   42 | import org.springframework.security.oauth2.server.resource.BearerTokenError;
   43 | import org.springframework.security.oauth2.server.resource.BearerTokenErrors;
   44 | import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
   45 | import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
   46 | import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
   47 | import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
   48 | import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
   49 | import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
   50 | import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
   51 | import org.springframework.security.web.AuthenticationEntryPoint;
   52 | import org.springframework.security.web.authentication.AuthenticationConverter;
   53 | import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
   54 | import org.springframework.security.web.authentication.AuthenticationFailureHandler;
   55 | import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
   56 | import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
   57 | import org.springframework.security.web.context.SecurityContextRepository;
   58 | import org.springframework.util.Assert;
   59 | import org.springframework.util.CollectionUtils;
   60 | import org.springframework.util.StringUtils;
   61 | import org.springframework.web.filter.OncePerRequestFilter;
   62 | 
   63 | /**
   64 |  * Authenticates requests that contain an OAuth 2.0
   65 |  * <a href="https://tools.ietf.org/html/rfc6750#section-1.2" target="_blank">Bearer
   66 |  * Token</a>.
   67 |  *
   68 |  * This filter should be wired with an {@link AuthenticationManager} that can authenticate
   69 |  * a {@link BearerTokenAuthenticationToken}.
   70 |  *
   71 |  * @author Josh Cummings
   72 |  * @author Vedran Pavic
   73 |  * @author Joe Grandja
   74 |  * @author Jeongjin Kim
   75 |  * @since 5.1
   76 |  * @see <a href="https://tools.ietf.org/html/rfc6750" target="_blank">The OAuth 2.0
   77 |  * Authorization Framework: Bearer Token Usage</a>
   78 |  * @see JwtAuthenticationProvider
   79 |  */
   80 | public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {
   81 | 
   82 | 	private final AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver;
   83 | 
   84 | 	private final AuthenticationConverter authenticationConverter;
   85 | 
   86 | 	private SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
   87 | 		.getContextHolderStrategy();
   88 | 
   89 | 	private AuthenticationEntryPoint authenticationEntryPoint = new BearerTokenAuthenticationEntryPoint();
   90 | 
   91 | 	private AuthenticationFailureHandler authenticationFailureHandler = new AuthenticationEntryPointFailureHandler(
   92 | 			(request, response, exception) -> this.authenticationEntryPoint.commence(request, response, exception));
   93 | 
   94 | 	private SecurityContextRepository securityContextRepository = new RequestAttributeSecurityContextRepository();
   95 | 
   96 | 	/**
   97 | 	 * Construct a {@code BearerTokenAuthenticationFilter} using the provided
   98 | 	 * parameter(s).
   99 | 	 * @param authenticationManagerResolver
  100 | 	 */
  101 | 	public BearerTokenAuthenticationFilter(
  102 | 			AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver) {
  103 | 		this(authenticationManagerResolver, new BearerTokenAuthenticationConverter());
  104 | 	}
  105 | 
  106 | 	/**
  107 | 	 * Construct a {@code BearerTokenAuthenticationFilter} using the provided
  108 | 	 * parameter(s).
  109 | 	 * @param authenticationManager
  110 | 	 */
  111 | 	public BearerTokenAuthenticationFilter(AuthenticationManager authenticationManager) {
  112 | 		this(authenticationManager, new BearerTokenAuthenticationConverter());
  113 | 	}
  114 | 
  115 | 	/**
  116 | 	 * Construct this filter using the provided parameters.
  117 | 	 * @param authenticationManager the {@link AuthenticationManager} to use
  118 | 	 * @param authenticationConverter the {@link AuthenticationConverter} to use
  119 | 	 * @since 7.0
  120 | 	 * @see JwtAuthenticationProvider
  121 | 	 * @see OpaqueTokenAuthenticationProvider
  122 | 	 * @see BearerTokenAuthenticationConverter
  123 | 	 */
  124 | 	public BearerTokenAuthenticationFilter(AuthenticationManager authenticationManager,
  125 | 			AuthenticationConverter authenticationConverter) {
  126 | 		Assert.notNull(authenticationManager, "authenticationManager cannot be null");
  127 | 		Assert.notNull(authenticationConverter, "authenticationConverter cannot be null");
  128 | 		this.authenticationManagerResolver = (authentication) -> authenticationManager;
  129 | 		this.authenticationConverter = authenticationConverter;
  130 | 	}
  131 | 
  132 | 	/**
  133 | 	 * Construct this filter using the provided parameters.
  134 | 	 * @param authenticationManagerResolver the {@link AuthenticationManagerResolver} to
  135 | 	 * use
  136 | 	 * @param authenticationConverter the {@link AuthenticationConverter} to use
  137 | 	 * @since 7.0
  138 | 	 * @see JwtAuthenticationProvider
  139 | 	 * @see OpaqueTokenAuthenticationProvider
  140 | 	 * @see BearerTokenAuthenticationConverter
  141 | 	 */
  142 | 	public BearerTokenAuthenticationFilter(
  143 | 			AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver,
  144 | 			AuthenticationConverter authenticationConverter) {
  145 | 		Assert.notNull(authenticationManagerResolver, "authenticationManagerResolver cannot be null");
  146 | 		Assert.notNull(authenticationConverter, "authenticationConverter cannot be null");
  147 | 		this.authenticationManagerResolver = authenticationManagerResolver;
  148 | 		this.authenticationConverter = authenticationConverter;
  149 | 	}
  150 | 
  151 | 	/**
  152 | 	 * Extract any
  153 | 	 * <a href="https://tools.ietf.org/html/rfc6750#section-1.2" target="_blank">Bearer
  154 | 	 * Token</a> from the request and attempt an authentication.
  155 | 	 * @param request
  156 | 	 * @param response
  157 | 	 * @param filterChain
  158 | 	 * @throws ServletException
  159 | 	 * @throws IOException
  160 | 	 */
  161 | 	@Override
  162 | 	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
  163 | 			throws ServletException, IOException {
  164 | 		Authentication authenticationRequest;
  165 | 		try {
  166 | 			authenticationRequest = this.authenticationConverter.convert(request);
  167 | 		}
  168 | 		catch (OAuth2AuthenticationException invalid) {
  169 | 			this.logger.trace("Sending to authentication entry point since failed to resolve bearer token", invalid);
  170 | 			this.authenticationEntryPoint.commence(request, response, invalid);
  171 | 			return;
  172 | 		}
  173 | 
  174 | 		if (authenticationRequest == null) {
  175 | 			this.logger.trace("Did not process request since did not find bearer token");
  176 | 			filterChain.doFilter(request, response);
  177 | 			return;
  178 | 		}
  179 | 
  180 | 		try {
  181 | 			AuthenticationManager authenticationManager = this.authenticationManagerResolver.resolve(request);
  182 | 			Authentication authenticationResult = authenticationManager.authenticate(authenticationRequest);
  183 | 			if (isDPoPBoundAccessToken(authenticationResult)) {
  184 | 				// Prevent downgraded usage of DPoP-bound access tokens,
  185 | 				// by rejecting a DPoP-bound access token received as a bearer token.
  186 | 				BearerTokenError error = BearerTokenErrors.invalidToken("Invalid bearer token");
  187 | 				throw new OAuth2AuthenticationException(error);
  188 | 			}
  189 | 			Authentication current = this.securityContextHolderStrategy.getContext().getAuthentication();
  190 | 			if (current != null && current.isAuthenticated() && declaresToBuilder(authenticationResult)) {
  191 | 				authenticationResult = authenticationResult.toBuilder().authorities((a) -> {
  192 | 					Set<String> newAuthorities = a.stream()
  193 | 						.map(GrantedAuthority::getAuthority)
  194 | 						.collect(Collectors.toUnmodifiableSet());
  195 | 					for (GrantedAuthority currentAuthority : current.getAuthorities()) {
  196 | 						if (!newAuthorities.contains(currentAuthority.getAuthority())) {
  197 | 							a.add(currentAuthority);
  198 | 						}
  199 | 					}
  200 | 				}).build();
  201 | 			}
  202 | 			SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
  203 | 			context.setAuthentication(authenticationResult);
  204 | 			this.securityContextHolderStrategy.setContext(context);
  205 | 			this.securityContextRepository.saveContext(context, request, response);
  206 | 			if (this.logger.isDebugEnabled()) {
  207 | 				this.logger.debug(LogMessage.format("Set SecurityContextHolder to %s", authenticationResult));
  208 | 			}
  209 | 			filterChain.doFilter(request, response);
  210 | 		}
  211 | 		catch (AuthenticationException failed) {
  212 | 			this.securityContextHolderStrategy.clearContext();
  213 | 			this.logger.trace("Failed to process authentication request", failed);
  214 | 			this.authenticationFailureHandler.onAuthenticationFailure(request, response, failed);
  215 | 		}
  216 | 	}
  217 | 
  218 | 	private static boolean declaresToBuilder(Authentication authentication) {
  219 | 		for (Method method : authentication.getClass().getDeclaredMethods()) {
  220 | 			if (method.getName().equals("toBuilder") && method.getParameterTypes().length == 0) {
  221 | 				return true;
  222 | 			}
  223 | 		}
  224 | 		return false;
  225 | 	}
  226 | 
  227 | 	/**
  228 | 	 * Sets the {@link SecurityContextHolderStrategy} to use. The default action is to use
  229 | 	 * the {@link SecurityContextHolderStrategy} stored in {@link SecurityContextHolder}.
  230 | 	 *
  231 | 	 * @since 5.8
  232 | 	 */
  233 | 	public void setSecurityContextHolderStrategy(SecurityContextHolderStrategy securityContextHolderStrategy) {
  234 | 		Assert.notNull(securityContextHolderStrategy, "securityContextHolderStrategy cannot be null");
  235 | 		this.securityContextHolderStrategy = securityContextHolderStrategy;
  236 | 	}
  237 | 
  238 | 	/**
  239 | 	 * Sets the {@link SecurityContextRepository} to save the {@link SecurityContext} on
  240 | 	 * authentication success. The default action is not to save the
  241 | 	 * {@link SecurityContext}.
  242 | 	 * @param securityContextRepository the {@link SecurityContextRepository} to use.
  243 | 	 * Cannot be null.
  244 | 	 */
  245 | 	public void setSecurityContextRepository(SecurityContextRepository securityContextRepository) {
  246 | 		Assert.notNull(securityContextRepository, "securityContextRepository cannot be null");
  247 | 		this.securityContextRepository = securityContextRepository;
  248 | 	}
  249 | 
  250 | 	/**
  251 | 	 * Set the {@link BearerTokenResolver} to use. Defaults to
  252 | 	 * {@link DefaultBearerTokenResolver}.
  253 | 	 * @param bearerTokenResolver the {@code BearerTokenResolver} to use
  254 | 	 * @deprecated Please provide an {@link AuthenticationConverter} in the constructor
  255 | 	 * instead
  256 | 	 * @see BearerTokenAuthenticationConverter
  257 | 	 */
  258 | 	@Deprecated
  259 | 	public void setBearerTokenResolver(BearerTokenResolver bearerTokenResolver) {
  260 | 		Assert.notNull(bearerTokenResolver, "bearerTokenResolver cannot be null");
  261 | 		if (this.authenticationConverter instanceof BearerTokenAuthenticationConverter converter) {
  262 | 			converter.setBearerTokenResolver(bearerTokenResolver);
  263 | 		}
  264 | 		else {
  265 | 			throw new IllegalArgumentException(
  266 | 					"You cannot both specify an AuthenticationConverter and a BearerTokenResolver.");
  267 | 		}
  268 | 	}
  269 | 
  270 | 	/**
  271 | 	 * Set the {@link AuthenticationEntryPoint} to use. Defaults to
  272 | 	 * {@link BearerTokenAuthenticationEntryPoint}.
  273 | 	 * @param authenticationEntryPoint the {@code AuthenticationEntryPoint} to use
  274 | 	 */
  275 | 	public void setAuthenticationEntryPoint(final AuthenticationEntryPoint authenticationEntryPoint) {
  276 | 		Assert.notNull(authenticationEntryPoint, "authenticationEntryPoint cannot be null");
  277 | 		this.authenticationEntryPoint = authenticationEntryPoint;
  278 | 	}
  279 | 
  280 | 	/**
  281 | 	 * Set the {@link AuthenticationFailureHandler} to use. Default implementation invokes
  282 | 	 * {@link AuthenticationEntryPoint}.
  283 | 	 * @param authenticationFailureHandler the {@code AuthenticationFailureHandler} to use
  284 | 	 * @since 5.2
  285 | 	 */
  286 | 	public void setAuthenticationFailureHandler(final AuthenticationFailureHandler authenticationFailureHandler) {
  287 | 		Assert.notNull(authenticationFailureHandler, "authenticationFailureHandler cannot be null");
  288 | 		this.authenticationFailureHandler = authenticationFailureHandler;
  289 | 	}
  290 | 
  291 | 	/**
  292 | 	 * Set the {@link AuthenticationDetailsSource} to use. Defaults to
  293 | 	 * {@link WebAuthenticationDetailsSource}.
  294 | 	 * @param authenticationDetailsSource the {@code AuthenticationDetailsSource} to use
  295 | 	 * @since 5.5
  296 | 	 * @deprecated Please provide an {@link AuthenticationConverter} in the constructor
  297 | 	 * and set the {@link AuthenticationDetailsSource} there instead. For example, you can
  298 | 	 * use {@link BearerTokenAuthenticationConverter#setAuthenticationDetailsSource}
  299 | 	 * @see BearerTokenAuthenticationConverter
  300 | 	 */
  301 | 	@Deprecated
  302 | 	public void setAuthenticationDetailsSource(
  303 | 			AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource) {
  304 | 		Assert.notNull(authenticationDetailsSource, "authenticationDetailsSource cannot be null");
  305 | 		if (this.authenticationConverter instanceof BearerTokenAuthenticationConverter converter) {
  306 | 			converter.setAuthenticationDetailsSource(authenticationDetailsSource);
  307 | 		}
  308 | 		else {
  309 | 			throw new IllegalArgumentException(
  310 | 					"You cannot specify both an AuthenticationConverter and an AuthenticationDetailsSource");
  311 | 		}
  312 | 	}
  313 | 
  314 | 	private static boolean isDPoPBoundAccessToken(Authentication authentication) {
  315 | 		if (!(authentication instanceof AbstractOAuth2TokenAuthenticationToken<?> accessTokenAuthentication)) {
  316 | 			return false;
  317 | 		}
  318 | 		ClaimAccessor accessTokenClaims = accessTokenAuthentication::getTokenAttributes;
  319 | 		String jwkThumbprintClaim = null;
  320 | 		Map<String, Object> confirmationMethodClaim = accessTokenClaims.getClaimAsMap("cnf");
  321 | 		if (!CollectionUtils.isEmpty(confirmationMethodClaim) && confirmationMethodClaim.containsKey("jkt")) {
  322 | 			jwkThumbprintClaim = (String) confirmationMethodClaim.get("jkt");
  323 | 		}
  324 | 		return StringUtils.hasText(jwkThumbprintClaim);
  325 | 	}
  326 | 
  327 | }
```
