<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/web/configurers/ExceptionHandlingConfigurer.java -->
<!-- 正文引用：[[源码:ExceptionHandlingConfigurer.java#L起-L止]] ；代码块头部：(src: ExceptionHandlingConfigurer.java#L起-L止) -->

# ExceptionHandlingConfigurer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/web/configurers/ExceptionHandlingConfigurer.java` |
| 行数 | 318 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:55:53.148Z |

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
   19 | import java.util.LinkedHashMap;
   20 | import java.util.function.Consumer;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.security.config.Customizer;
   25 | import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
   26 | import org.springframework.security.config.annotation.web.builders.HttpSecurity;
   27 | import org.springframework.security.core.GrantedAuthority;
   28 | import org.springframework.security.web.AuthenticationEntryPoint;
   29 | import org.springframework.security.web.access.AccessDeniedHandler;
   30 | import org.springframework.security.web.access.AccessDeniedHandlerImpl;
   31 | import org.springframework.security.web.access.DelegatingMissingAuthorityAccessDeniedHandler;
   32 | import org.springframework.security.web.access.ExceptionTranslationFilter;
   33 | import org.springframework.security.web.access.RequestMatcherDelegatingAccessDeniedHandler;
   34 | import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
   35 | import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
   36 | import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
   37 | import org.springframework.security.web.savedrequest.RequestCache;
   38 | import org.springframework.security.web.util.matcher.RequestMatcher;
   39 | 
   40 | /**
   41 |  * Adds exception handling for Spring Security related exceptions to an application. All
   42 |  * properties have reasonable defaults, so no additional configuration is required other
   43 |  * than applying this
   44 |  * {@link org.springframework.security.config.annotation.SecurityConfigurer}.
   45 |  *
   46 |  * <h2>Security Filters</h2>
   47 |  *
   48 |  * The following Filters are populated
   49 |  *
   50 |  * <ul>
   51 |  * <li>{@link ExceptionTranslationFilter}</li>
   52 |  * </ul>
   53 |  *
   54 |  * <h2>Shared Objects Created</h2>
   55 |  *
   56 |  * No shared objects are created.
   57 |  *
   58 |  * <h2>Shared Objects Used</h2>
   59 |  *
   60 |  * The following shared objects are used:
   61 |  *
   62 |  * <ul>
   63 |  * <li>If no explicit {@link RequestCache}, is provided a {@link RequestCache} shared
   64 |  * object is used to replay the request after authentication is successful</li>
   65 |  * <li>{@link AuthenticationEntryPoint} - see
   66 |  * {@link #authenticationEntryPoint(AuthenticationEntryPoint)}</li>
   67 |  * </ul>
   68 |  *
   69 |  * @author Rob Winch
   70 |  * @since 3.2
   71 |  */
   72 | public final class ExceptionHandlingConfigurer<H extends HttpSecurityBuilder<H>>
   73 | 		extends AbstractHttpConfigurer<ExceptionHandlingConfigurer<H>, H> {
   74 | 
   75 | 	private AuthenticationEntryPoint authenticationEntryPoint;
   76 | 
   77 | 	private AccessDeniedHandler accessDeniedHandler;
   78 | 
   79 | 	private DelegatingAuthenticationEntryPoint.@Nullable Builder defaultEntryPoint;
   80 | 
   81 | 	private LinkedHashMap<RequestMatcher, AccessDeniedHandler> defaultDeniedHandlerMappings = new LinkedHashMap<>();
   82 | 
   83 | 	private DelegatingMissingAuthorityAccessDeniedHandler.@Nullable Builder missingAuthoritiesHandlerBuilder;
   84 | 
   85 | 	/**
   86 | 	 * Creates a new instance.
   87 | 	 * @see HttpSecurity#exceptionHandling(Customizer)
   88 | 	 */
   89 | 	public ExceptionHandlingConfigurer() {
   90 | 	}
   91 | 
   92 | 	/**
   93 | 	 * Shortcut to specify the {@link AccessDeniedHandler} to be used is a specific error
   94 | 	 * page.
   95 | 	 * @param accessDeniedUrl the URL to the access denied page (i.e. /errors/401)
   96 | 	 * @return the {@link ExceptionHandlingConfigurer} for further customization
   97 | 	 * @see AccessDeniedHandlerImpl
   98 | 	 * @see #accessDeniedHandler(org.springframework.security.web.access.AccessDeniedHandler)
   99 | 	 */
  100 | 	public ExceptionHandlingConfigurer<H> accessDeniedPage(String accessDeniedUrl) {
  101 | 		AccessDeniedHandlerImpl accessDeniedHandler = new AccessDeniedHandlerImpl();
  102 | 		accessDeniedHandler.setErrorPage(accessDeniedUrl);
  103 | 		return accessDeniedHandler(accessDeniedHandler);
  104 | 	}
  105 | 
  106 | 	/**
  107 | 	 * Specifies the {@link AccessDeniedHandler} to be used.
  108 | 	 * @param accessDeniedHandler the {@link AccessDeniedHandler} to be used
  109 | 	 * @return the {@link ExceptionHandlingConfigurer} for further customization
  110 | 	 */
  111 | 	public ExceptionHandlingConfigurer<H> accessDeniedHandler(AccessDeniedHandler accessDeniedHandler) {
  112 | 		this.accessDeniedHandler = accessDeniedHandler;
  113 | 		return this;
  114 | 	}
  115 | 
  116 | 	/**
  117 | 	 * Sets a default {@link AccessDeniedHandler} to be used which prefers being invoked
  118 | 	 * for the provided {@link RequestMatcher}. If only a single default
  119 | 	 * {@link AccessDeniedHandler} is specified, it will be what is used for the default
  120 | 	 * {@link AccessDeniedHandler}. If multiple default {@link AccessDeniedHandler}
  121 | 	 * instances are configured, then a
  122 | 	 * {@link RequestMatcherDelegatingAccessDeniedHandler} will be used.
  123 | 	 * @param deniedHandler the {@link AccessDeniedHandler} to use
  124 | 	 * @param preferredMatcher the {@link RequestMatcher} for this default
  125 | 	 * {@link AccessDeniedHandler}
  126 | 	 * @return the {@link ExceptionHandlingConfigurer} for further customizations
  127 | 	 * @since 5.1
  128 | 	 */
  129 | 	public ExceptionHandlingConfigurer<H> defaultAccessDeniedHandlerFor(AccessDeniedHandler deniedHandler,
  130 | 			RequestMatcher preferredMatcher) {
  131 | 		this.defaultDeniedHandlerMappings.put(preferredMatcher, deniedHandler);
  132 | 		return this;
  133 | 	}
  134 | 
  135 | 	/**
  136 | 	 * Sets a default {@link AuthenticationEntryPoint} to be used which prefers being
  137 | 	 * invoked for the provided missing {@link GrantedAuthority}.
  138 | 	 * @param entryPoint the {@link AuthenticationEntryPoint} to use for the given
  139 | 	 * {@code authority}
  140 | 	 * @param authority the authority
  141 | 	 * @return the {@link ExceptionHandlingConfigurer} for further customizations
  142 | 	 * @since 7.0
  143 | 	 */
  144 | 	public ExceptionHandlingConfigurer<H> defaultDeniedHandlerForMissingAuthority(AuthenticationEntryPoint entryPoint,
  145 | 			String authority) {
  146 | 		if (this.missingAuthoritiesHandlerBuilder == null) {
  147 | 			this.missingAuthoritiesHandlerBuilder = DelegatingMissingAuthorityAccessDeniedHandler.builder();
  148 | 		}
  149 | 		this.missingAuthoritiesHandlerBuilder.addEntryPointFor(entryPoint, authority);
  150 | 		return this;
  151 | 	}
  152 | 
  153 | 	/**
  154 | 	 * Sets a default {@link AuthenticationEntryPoint} to be used which prefers being
  155 | 	 * invoked for the provided missing {@link GrantedAuthority}.
  156 | 	 * @param entryPoint a consumer of a
  157 | 	 * {@link DelegatingAuthenticationEntryPoint.Builder} to use for the given
  158 | 	 * {@code authority}
  159 | 	 * @param authority the authority
  160 | 	 * @return the {@link ExceptionHandlingConfigurer} for further customizations
  161 | 	 * @since 7.0
  162 | 	 */
  163 | 	public ExceptionHandlingConfigurer<H> defaultDeniedHandlerForMissingAuthority(
  164 | 			Consumer<DelegatingAuthenticationEntryPoint.Builder> entryPoint, String authority) {
  165 | 		if (this.missingAuthoritiesHandlerBuilder == null) {
  166 | 			this.missingAuthoritiesHandlerBuilder = DelegatingMissingAuthorityAccessDeniedHandler.builder();
  167 | 		}
  168 | 		this.missingAuthoritiesHandlerBuilder.addEntryPointFor(entryPoint, authority);
  169 | 		return this;
  170 | 	}
  171 | 
  172 | 	/**
  173 | 	 * Sets the {@link AuthenticationEntryPoint} to be used.
  174 | 	 *
  175 | 	 * <p>
  176 | 	 * If no {@link #authenticationEntryPoint(AuthenticationEntryPoint)} is specified,
  177 | 	 * then
  178 | 	 * {@link #defaultAuthenticationEntryPointFor(AuthenticationEntryPoint, RequestMatcher)}
  179 | 	 * will be used. The first {@link AuthenticationEntryPoint} will be used as the
  180 | 	 * default if no matches were found.
  181 | 	 * </p>
  182 | 	 *
  183 | 	 * <p>
  184 | 	 * If that is not provided defaults to {@link Http403ForbiddenEntryPoint}.
  185 | 	 * </p>
  186 | 	 * @param authenticationEntryPoint the {@link AuthenticationEntryPoint} to use
  187 | 	 * @return the {@link ExceptionHandlingConfigurer} for further customizations
  188 | 	 */
  189 | 	public ExceptionHandlingConfigurer<H> authenticationEntryPoint(AuthenticationEntryPoint authenticationEntryPoint) {
  190 | 		this.authenticationEntryPoint = authenticationEntryPoint;
  191 | 		return this;
  192 | 	}
  193 | 
  194 | 	/**
  195 | 	 * Sets a default {@link AuthenticationEntryPoint} to be used which prefers being
  196 | 	 * invoked for the provided {@link RequestMatcher}. If only a single default
  197 | 	 * {@link AuthenticationEntryPoint} is specified, it will be what is used for the
  198 | 	 * default {@link AuthenticationEntryPoint}. If multiple default
  199 | 	 * {@link AuthenticationEntryPoint} instances are configured, then a
  200 | 	 * {@link DelegatingAuthenticationEntryPoint} will be used.
  201 | 	 * @param entryPoint the {@link AuthenticationEntryPoint} to use
  202 | 	 * @param preferredMatcher the {@link RequestMatcher} for this default
  203 | 	 * {@link AuthenticationEntryPoint}
  204 | 	 * @return the {@link ExceptionHandlingConfigurer} for further customizations
  205 | 	 */
  206 | 	public ExceptionHandlingConfigurer<H> defaultAuthenticationEntryPointFor(AuthenticationEntryPoint entryPoint,
  207 | 			RequestMatcher preferredMatcher) {
  208 | 		if (this.defaultEntryPoint == null) {
  209 | 			this.defaultEntryPoint = DelegatingAuthenticationEntryPoint.builder();
  210 | 		}
  211 | 		this.defaultEntryPoint.addEntryPointFor(entryPoint, preferredMatcher);
  212 | 		return this;
  213 | 	}
  214 | 
  215 | 	/**
  216 | 	 * Gets any explicitly configured {@link AuthenticationEntryPoint}.
  217 | 	 * @return
  218 | 	 */
  219 | 	AuthenticationEntryPoint getAuthenticationEntryPoint() {
  220 | 		return this.authenticationEntryPoint;
  221 | 	}
  222 | 
  223 | 	/**
  224 | 	 * Gets the {@link AccessDeniedHandler} that is configured.
  225 | 	 * @return the {@link AccessDeniedHandler}
  226 | 	 */
  227 | 	AccessDeniedHandler getAccessDeniedHandler() {
  228 | 		return this.accessDeniedHandler;
  229 | 	}
  230 | 
  231 | 	@Override
  232 | 	public void configure(H http) {
  233 | 		AuthenticationEntryPoint entryPoint = getAuthenticationEntryPoint(http);
  234 | 		ExceptionTranslationFilter exceptionTranslationFilter = new ExceptionTranslationFilter(entryPoint,
  235 | 				getRequestCache(http));
  236 | 		AccessDeniedHandler deniedHandler = getAccessDeniedHandler(http);
  237 | 		exceptionTranslationFilter.setAccessDeniedHandler(deniedHandler);
  238 | 		exceptionTranslationFilter.setSecurityContextHolderStrategy(getSecurityContextHolderStrategy());
  239 | 		exceptionTranslationFilter = postProcess(exceptionTranslationFilter);
  240 | 		http.addFilter(exceptionTranslationFilter);
  241 | 	}
  242 | 
  243 | 	/**
  244 | 	 * Gets the {@link AccessDeniedHandler} according to the rules specified by
  245 | 	 * {@link #accessDeniedHandler(AccessDeniedHandler)}.
  246 | 	 * @param http the {@link HttpSecurity} used to look up shared
  247 | 	 * {@link AccessDeniedHandler}
  248 | 	 * @return the {@link AccessDeniedHandler} to use
  249 | 	 */
  250 | 	AccessDeniedHandler getAccessDeniedHandler(H http) {
  251 | 		AccessDeniedHandler deniedHandler = this.accessDeniedHandler;
  252 | 		if (deniedHandler == null) {
  253 | 			deniedHandler = createDefaultDeniedHandler(http);
  254 | 		}
  255 | 		return deniedHandler;
  256 | 	}
  257 | 
  258 | 	/**
  259 | 	 * Gets the {@link AuthenticationEntryPoint} according to the rules specified by
  260 | 	 * {@link #authenticationEntryPoint(AuthenticationEntryPoint)}.
  261 | 	 * @param http the {@link HttpSecurity} used to look up shared
  262 | 	 * {@link AuthenticationEntryPoint}
  263 | 	 * @return the {@link AuthenticationEntryPoint} to use
  264 | 	 */
  265 | 	AuthenticationEntryPoint getAuthenticationEntryPoint(H http) {
  266 | 		AuthenticationEntryPoint entryPoint = this.authenticationEntryPoint;
  267 | 		if (entryPoint == null) {
  268 | 			entryPoint = createDefaultEntryPoint(http);
  269 | 		}
  270 | 		return entryPoint;
  271 | 	}
  272 | 
  273 | 	private AccessDeniedHandler createDefaultDeniedHandler(H http) {
  274 | 		AccessDeniedHandler defaults = createDefaultAccessDeniedHandler(http);
  275 | 		if (this.missingAuthoritiesHandlerBuilder == null) {
  276 | 			return defaults;
  277 | 		}
  278 | 		DelegatingMissingAuthorityAccessDeniedHandler deniedHandler = this.missingAuthoritiesHandlerBuilder.build();
  279 | 		deniedHandler.setRequestCache(getRequestCache(http));
  280 | 		deniedHandler.setDefaultAccessDeniedHandler(defaults);
  281 | 		return deniedHandler;
  282 | 	}
  283 | 
  284 | 	private AccessDeniedHandler createDefaultAccessDeniedHandler(H http) {
  285 | 		if (this.defaultDeniedHandlerMappings.isEmpty()) {
  286 | 			return new AccessDeniedHandlerImpl();
  287 | 		}
  288 | 		if (this.defaultDeniedHandlerMappings.size() == 1) {
  289 | 			return this.defaultDeniedHandlerMappings.values().iterator().next();
  290 | 		}
  291 | 		return new RequestMatcherDelegatingAccessDeniedHandler(this.defaultDeniedHandlerMappings,
  292 | 				new AccessDeniedHandlerImpl());
  293 | 	}
  294 | 
  295 | 	private AuthenticationEntryPoint createDefaultEntryPoint(H http) {
  296 | 		if (this.defaultEntryPoint == null) {
  297 | 			return new Http403ForbiddenEntryPoint();
  298 | 		}
  299 | 		return this.defaultEntryPoint.build();
  300 | 	}
  301 | 
  302 | 	/**
  303 | 	 * Gets the {@link RequestCache} to use. If one is defined using
  304 | 	 * {@link #requestCache(org.springframework.security.web.savedrequest.RequestCache)},
  305 | 	 * then it is used. Otherwise, an attempt to find a {@link RequestCache} shared object
  306 | 	 * is made. If that fails, an {@link HttpSessionRequestCache} is used
  307 | 	 * @param http the {@link HttpSecurity} to attempt to fined the shared object
  308 | 	 * @return the {@link RequestCache} to use
  309 | 	 */
  310 | 	private RequestCache getRequestCache(H http) {
  311 | 		RequestCache result = http.getSharedObject(RequestCache.class);
  312 | 		if (result != null) {
  313 | 			return result;
  314 | 		}
  315 | 		return new HttpSessionRequestCache();
  316 | 	}
  317 | 
  318 | }
```
