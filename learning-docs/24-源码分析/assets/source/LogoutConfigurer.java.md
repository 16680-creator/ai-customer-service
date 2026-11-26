<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/web/configurers/LogoutConfigurer.java -->
<!-- 正文引用：[[源码:LogoutConfigurer.java#L起-L止]] ；代码块头部：(src: LogoutConfigurer.java#L起-L止) -->

# LogoutConfigurer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/web/configurers/LogoutConfigurer.java` |
| 行数 | 376 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:40:13.727Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.LinkedHashMap;
   21 | import java.util.List;
   22 | 
   23 | import jakarta.servlet.http.HttpSession;
   24 | 
   25 | import org.springframework.http.HttpMethod;
   26 | import org.springframework.security.config.Customizer;
   27 | import org.springframework.security.config.annotation.SecurityConfigurer;
   28 | import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
   29 | import org.springframework.security.config.annotation.web.builders.HttpSecurity;
   30 | import org.springframework.security.core.Authentication;
   31 | import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
   32 | import org.springframework.security.web.authentication.logout.DelegatingLogoutSuccessHandler;
   33 | import org.springframework.security.web.authentication.logout.LogoutFilter;
   34 | import org.springframework.security.web.authentication.logout.LogoutHandler;
   35 | import org.springframework.security.web.authentication.logout.LogoutSuccessEventPublishingLogoutHandler;
   36 | import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
   37 | import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
   38 | import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
   39 | import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
   40 | import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
   41 | import org.springframework.security.web.context.SecurityContextRepository;
   42 | import org.springframework.security.web.util.matcher.OrRequestMatcher;
   43 | import org.springframework.security.web.util.matcher.RequestMatcher;
   44 | import org.springframework.util.Assert;
   45 | 
   46 | /**
   47 |  * Adds logout support. Other {@link SecurityConfigurer} instances may invoke
   48 |  * {@link #addLogoutHandler(LogoutHandler)} in the
   49 |  * {@link SecurityConfigurer#init(org.springframework.security.config.annotation.SecurityBuilder)}
   50 |  * phase.
   51 |  *
   52 |  * <h2>Security Filters</h2>
   53 |  *
   54 |  * The following Filters are populated
   55 |  *
   56 |  * <ul>
   57 |  * <li>{@link LogoutFilter}</li>
   58 |  * </ul>
   59 |  *
   60 |  * <h2>Shared Objects Created</h2>
   61 |  *
   62 |  * No shared Objects are created
   63 |  *
   64 |  * <h2>Shared Objects Used</h2>
   65 |  *
   66 |  * No shared objects are used.
   67 |  *
   68 |  * @author Rob Winch
   69 |  * @author Onur Kagan Ozcan
   70 |  * @since 3.2
   71 |  * @see RememberMeConfigurer
   72 |  */
   73 | public final class LogoutConfigurer<H extends HttpSecurityBuilder<H>>
   74 | 		extends AbstractHttpConfigurer<LogoutConfigurer<H>, H> {
   75 | 
   76 | 	private List<LogoutHandler> logoutHandlers = new ArrayList<>();
   77 | 
   78 | 	private SecurityContextLogoutHandler contextLogoutHandler = new SecurityContextLogoutHandler();
   79 | 
   80 | 	private String logoutSuccessUrl = "/login?logout";
   81 | 
   82 | 	private LogoutSuccessHandler logoutSuccessHandler;
   83 | 
   84 | 	private String logoutUrl = "/logout";
   85 | 
   86 | 	private RequestMatcher logoutRequestMatcher;
   87 | 
   88 | 	private boolean permitAll;
   89 | 
   90 | 	private boolean customLogoutSuccess;
   91 | 
   92 | 	private LinkedHashMap<RequestMatcher, LogoutSuccessHandler> defaultLogoutSuccessHandlerMappings = new LinkedHashMap<>();
   93 | 
   94 | 	/**
   95 | 	 * Creates a new instance.
   96 | 	 * @see HttpSecurity#logout(Customizer)
   97 | 	 */
   98 | 	public LogoutConfigurer() {
   99 | 	}
  100 | 
  101 | 	/**
  102 | 	 * Adds a {@link LogoutHandler}. {@link SecurityContextLogoutHandler} and
  103 | 	 * {@link LogoutSuccessEventPublishingLogoutHandler} are added as last
  104 | 	 * {@link LogoutHandler} instances by default.
  105 | 	 * @param logoutHandler the {@link LogoutHandler} to add
  106 | 	 * @return the {@link LogoutConfigurer} for further customization
  107 | 	 */
  108 | 	public LogoutConfigurer<H> addLogoutHandler(LogoutHandler logoutHandler) {
  109 | 		Assert.notNull(logoutHandler, "logoutHandler cannot be null");
  110 | 		this.logoutHandlers.add(logoutHandler);
  111 | 		return this;
  112 | 	}
  113 | 
  114 | 	/**
  115 | 	 * Specifies if {@link SecurityContextLogoutHandler} should clear the
  116 | 	 * {@link Authentication} at the time of logout.
  117 | 	 * @param clearAuthentication true {@link SecurityContextLogoutHandler} should clear
  118 | 	 * the {@link Authentication} (default), or false otherwise.
  119 | 	 * @return the {@link LogoutConfigurer} for further customization
  120 | 	 */
  121 | 	public LogoutConfigurer<H> clearAuthentication(boolean clearAuthentication) {
  122 | 		this.contextLogoutHandler.setClearAuthentication(clearAuthentication);
  123 | 		return this;
  124 | 	}
  125 | 
  126 | 	/**
  127 | 	 * Configures {@link SecurityContextLogoutHandler} to invalidate the
  128 | 	 * {@link HttpSession} at the time of logout.
  129 | 	 * @param invalidateHttpSession true if the {@link HttpSession} should be invalidated
  130 | 	 * (default), or false otherwise.
  131 | 	 * @return the {@link LogoutConfigurer} for further customization
  132 | 	 */
  133 | 	public LogoutConfigurer<H> invalidateHttpSession(boolean invalidateHttpSession) {
  134 | 		this.contextLogoutHandler.setInvalidateHttpSession(invalidateHttpSession);
  135 | 		return this;
  136 | 	}
  137 | 
  138 | 	/**
  139 | 	 * The URL that triggers log out to occur (default is "/logout"). If CSRF protection
  140 | 	 * is enabled (default), then the request must also be a POST. This means that by
  141 | 	 * default POST "/logout" is required to trigger a log out. If CSRF protection is
  142 | 	 * disabled, then any HTTP method is allowed.
  143 | 	 *
  144 | 	 * <p>
  145 | 	 * It is considered best practice to use an HTTP POST on any action that changes state
  146 | 	 * (i.e. log out) to protect against
  147 | 	 * <a href="https://en.wikipedia.org/wiki/Cross-site_request_forgery">CSRF
  148 | 	 * attacks</a>. If you really want to use an HTTP GET, you can use
  149 | 	 * <code>logoutRequestMatcher(PathPatternRequestMatcher.pathPattern(HttpMethod.GEt, logoutUrl));</code>
  150 | 	 * </p>
  151 | 	 * @param logoutUrl the URL that will invoke logout.
  152 | 	 * @return the {@link LogoutConfigurer} for further customization
  153 | 	 * @see #logoutRequestMatcher(RequestMatcher)
  154 | 	 * @see HttpSecurity#csrf(Customizer)
  155 | 	 */
  156 | 	public LogoutConfigurer<H> logoutUrl(String logoutUrl) {
  157 | 		this.logoutRequestMatcher = null;
  158 | 		this.logoutUrl = logoutUrl;
  159 | 		return this;
  160 | 	}
  161 | 
  162 | 	/**
  163 | 	 * The RequestMatcher that triggers log out to occur. In most circumstances users will
  164 | 	 * use {@link #logoutUrl(String)} which helps enforce good practices.
  165 | 	 * @param logoutRequestMatcher the RequestMatcher used to determine if logout should
  166 | 	 * occur.
  167 | 	 * @return the {@link LogoutConfigurer} for further customization
  168 | 	 * @see #logoutUrl(String)
  169 | 	 */
  170 | 	public LogoutConfigurer<H> logoutRequestMatcher(RequestMatcher logoutRequestMatcher) {
  171 | 		this.logoutRequestMatcher = logoutRequestMatcher;
  172 | 		return this;
  173 | 	}
  174 | 
  175 | 	/**
  176 | 	 * The URL to redirect to after logout has occurred. The default is "/login?logout".
  177 | 	 * This is a shortcut for invoking {@link #logoutSuccessHandler(LogoutSuccessHandler)}
  178 | 	 * with a {@link SimpleUrlLogoutSuccessHandler}.
  179 | 	 * @param logoutSuccessUrl the URL to redirect to after logout occurred
  180 | 	 * @return the {@link LogoutConfigurer} for further customization
  181 | 	 */
  182 | 	public LogoutConfigurer<H> logoutSuccessUrl(String logoutSuccessUrl) {
  183 | 		this.customLogoutSuccess = true;
  184 | 		this.logoutSuccessUrl = logoutSuccessUrl;
  185 | 		return this;
  186 | 	}
  187 | 
  188 | 	/**
  189 | 	 * A shortcut for {@link #permitAll(boolean)} with <code>true</code> as an argument.
  190 | 	 * @return the {@link LogoutConfigurer} for further customizations
  191 | 	 */
  192 | 	public LogoutConfigurer<H> permitAll() {
  193 | 		return permitAll(true);
  194 | 	}
  195 | 
  196 | 	/**
  197 | 	 * Allows specifying the names of cookies to be removed on logout success. This is a
  198 | 	 * shortcut to easily invoke {@link #addLogoutHandler(LogoutHandler)} with a
  199 | 	 * {@link CookieClearingLogoutHandler}.
  200 | 	 * @param cookieNamesToClear the names of cookies to be removed on logout success.
  201 | 	 * @return the {@link LogoutConfigurer} for further customization
  202 | 	 */
  203 | 	public LogoutConfigurer<H> deleteCookies(String... cookieNamesToClear) {
  204 | 		return addLogoutHandler(new CookieClearingLogoutHandler(cookieNamesToClear));
  205 | 	}
  206 | 
  207 | 	/**
  208 | 	 * Sets the {@link LogoutSuccessHandler} to use. If this is specified,
  209 | 	 * {@link #logoutSuccessUrl(String)} is ignored.
  210 | 	 * @param logoutSuccessHandler the {@link LogoutSuccessHandler} to use after a user
  211 | 	 * has been logged out.
  212 | 	 * @return the {@link LogoutConfigurer} for further customizations
  213 | 	 */
  214 | 	public LogoutConfigurer<H> logoutSuccessHandler(LogoutSuccessHandler logoutSuccessHandler) {
  215 | 		this.logoutSuccessUrl = null;
  216 | 		this.customLogoutSuccess = true;
  217 | 		this.logoutSuccessHandler = logoutSuccessHandler;
  218 | 		return this;
  219 | 	}
  220 | 
  221 | 	/**
  222 | 	 * Sets a default {@link LogoutSuccessHandler} to be used which prefers being invoked
  223 | 	 * for the provided {@link RequestMatcher}. If no {@link LogoutSuccessHandler} is
  224 | 	 * specified a {@link SimpleUrlLogoutSuccessHandler} will be used. If any default
  225 | 	 * {@link LogoutSuccessHandler} instances are configured, then a
  226 | 	 * {@link DelegatingLogoutSuccessHandler} will be used that defaults to a
  227 | 	 * {@link SimpleUrlLogoutSuccessHandler}.
  228 | 	 * @param handler the {@link LogoutSuccessHandler} to use
  229 | 	 * @param preferredMatcher the {@link RequestMatcher} for this default
  230 | 	 * {@link LogoutSuccessHandler}
  231 | 	 * @return the {@link LogoutConfigurer} for further customizations
  232 | 	 */
  233 | 	public LogoutConfigurer<H> defaultLogoutSuccessHandlerFor(LogoutSuccessHandler handler,
  234 | 			RequestMatcher preferredMatcher) {
  235 | 		Assert.notNull(handler, "handler cannot be null");
  236 | 		Assert.notNull(preferredMatcher, "preferredMatcher cannot be null");
  237 | 		this.defaultLogoutSuccessHandlerMappings.put(preferredMatcher, handler);
  238 | 		return this;
  239 | 	}
  240 | 
  241 | 	/**
  242 | 	 * Grants access to the {@link #logoutSuccessUrl(String)} and the
  243 | 	 * {@link #logoutUrl(String)} for every user.
  244 | 	 * @param permitAll if true grants access, else nothing is done
  245 | 	 * @return the {@link LogoutConfigurer} for further customization.
  246 | 	 */
  247 | 	public LogoutConfigurer<H> permitAll(boolean permitAll) {
  248 | 		this.permitAll = permitAll;
  249 | 		return this;
  250 | 	}
  251 | 
  252 | 	/**
  253 | 	 * Gets the {@link LogoutSuccessHandler} if not null, otherwise creates a new
  254 | 	 * {@link SimpleUrlLogoutSuccessHandler} using the {@link #logoutSuccessUrl(String)}.
  255 | 	 * @return the {@link LogoutSuccessHandler} to use
  256 | 	 */
  257 | 	public LogoutSuccessHandler getLogoutSuccessHandler() {
  258 | 		LogoutSuccessHandler handler = this.logoutSuccessHandler;
  259 | 		if (handler == null) {
  260 | 			handler = createDefaultSuccessHandler();
  261 | 			this.logoutSuccessHandler = handler;
  262 | 		}
  263 | 		return handler;
  264 | 	}
  265 | 
  266 | 	private LogoutSuccessHandler createDefaultSuccessHandler() {
  267 | 		SimpleUrlLogoutSuccessHandler urlLogoutHandler = new SimpleUrlLogoutSuccessHandler();
  268 | 		urlLogoutHandler.setDefaultTargetUrl(this.logoutSuccessUrl);
  269 | 		if (this.defaultLogoutSuccessHandlerMappings.isEmpty()) {
  270 | 			return urlLogoutHandler;
  271 | 		}
  272 | 		DelegatingLogoutSuccessHandler successHandler = new DelegatingLogoutSuccessHandler(
  273 | 				this.defaultLogoutSuccessHandlerMappings);
  274 | 		successHandler.setDefaultLogoutSuccessHandler(urlLogoutHandler);
  275 | 		return successHandler;
  276 | 	}
  277 | 
  278 | 	@Override
  279 | 	public void init(H http) {
  280 | 		if (this.permitAll) {
  281 | 			PermitAllSupport.permitAll(http, this.logoutSuccessUrl);
  282 | 			PermitAllSupport.permitAll(http, this.getLogoutRequestMatcher(http));
  283 | 		}
  284 | 		DefaultLoginPageGeneratingFilter loginPageGeneratingFilter = http
  285 | 			.getSharedObject(DefaultLoginPageGeneratingFilter.class);
  286 | 		if (loginPageGeneratingFilter != null && !isCustomLogoutSuccess()) {
  287 | 			loginPageGeneratingFilter.setLogoutSuccessUrl(getLogoutSuccessUrl());
  288 | 		}
  289 | 	}
  290 | 
  291 | 	@Override
  292 | 	public void configure(H http) {
  293 | 		LogoutFilter logoutFilter = createLogoutFilter(http);
  294 | 		http.addFilter(logoutFilter);
  295 | 	}
  296 | 
  297 | 	/**
  298 | 	 * Returns true if the logout success has been customized via
  299 | 	 * {@link #logoutSuccessUrl(String)} or
  300 | 	 * {@link #logoutSuccessHandler(LogoutSuccessHandler)}.
  301 | 	 * @return true if logout success handling has been customized, else false
  302 | 	 */
  303 | 	boolean isCustomLogoutSuccess() {
  304 | 		return this.customLogoutSuccess;
  305 | 	}
  306 | 
  307 | 	/**
  308 | 	 * Gets the logoutSuccessUrl or null if a
  309 | 	 * {@link #logoutSuccessHandler(LogoutSuccessHandler)} was configured.
  310 | 	 * @return the logoutSuccessUrl
  311 | 	 */
  312 | 	private String getLogoutSuccessUrl() {
  313 | 		return this.logoutSuccessUrl;
  314 | 	}
  315 | 
  316 | 	/**
  317 | 	 * Gets the {@link LogoutHandler} instances that will be used.
  318 | 	 * @return the {@link LogoutHandler} instances. Cannot be null.
  319 | 	 */
  320 | 	public List<LogoutHandler> getLogoutHandlers() {
  321 | 		return this.logoutHandlers;
  322 | 	}
  323 | 
  324 | 	/**
  325 | 	 * Creates the {@link LogoutFilter} using the {@link LogoutHandler} instances, the
  326 | 	 * {@link #logoutSuccessHandler(LogoutSuccessHandler)} and the
  327 | 	 * {@link #logoutUrl(String)}.
  328 | 	 * @param http the builder to use
  329 | 	 * @return the {@link LogoutFilter} to use.
  330 | 	 */
  331 | 	private LogoutFilter createLogoutFilter(H http) {
  332 | 		this.contextLogoutHandler.setSecurityContextHolderStrategy(getSecurityContextHolderStrategy());
  333 | 		this.contextLogoutHandler.setSecurityContextRepository(getSecurityContextRepository(http));
  334 | 		this.logoutHandlers.add(this.contextLogoutHandler);
  335 | 		this.logoutHandlers.add(postProcess(new LogoutSuccessEventPublishingLogoutHandler()));
  336 | 		LogoutHandler[] handlers = this.logoutHandlers.toArray(new LogoutHandler[0]);
  337 | 		LogoutFilter result = new LogoutFilter(getLogoutSuccessHandler(), handlers);
  338 | 		result.setSecurityContextHolderStrategy(getSecurityContextHolderStrategy());
  339 | 		result.setLogoutRequestMatcher(getLogoutRequestMatcher(http));
  340 | 		result = postProcess(result);
  341 | 		return result;
  342 | 	}
  343 | 
  344 | 	private SecurityContextRepository getSecurityContextRepository(H http) {
  345 | 		SecurityContextRepository securityContextRepository = http.getSharedObject(SecurityContextRepository.class);
  346 | 		if (securityContextRepository == null) {
  347 | 			securityContextRepository = new HttpSessionSecurityContextRepository();
  348 | 		}
  349 | 		return securityContextRepository;
  350 | 	}
  351 | 
  352 | 	private RequestMatcher getLogoutRequestMatcher(H http) {
  353 | 		if (this.logoutRequestMatcher != null) {
  354 | 			return this.logoutRequestMatcher;
  355 | 		}
  356 | 		this.logoutRequestMatcher = createLogoutRequestMatcher(http);
  357 | 		return this.logoutRequestMatcher;
  358 | 	}
  359 | 
  360 | 	@SuppressWarnings("unchecked")
  361 | 	private RequestMatcher createLogoutRequestMatcher(H http) {
  362 | 		RequestMatcher post = createLogoutRequestMatcher("POST");
  363 | 		if (http.getConfigurer(CsrfConfigurer.class) != null) {
  364 | 			return post;
  365 | 		}
  366 | 		RequestMatcher get = createLogoutRequestMatcher("GET");
  367 | 		RequestMatcher put = createLogoutRequestMatcher("PUT");
  368 | 		RequestMatcher delete = createLogoutRequestMatcher("DELETE");
  369 | 		return new OrRequestMatcher(get, post, put, delete);
  370 | 	}
  371 | 
  372 | 	private RequestMatcher createLogoutRequestMatcher(String httpMethod) {
  373 | 		return getRequestMatcherBuilder().matcher(HttpMethod.valueOf(httpMethod), this.logoutUrl);
  374 | 	}
  375 | 
  376 | }
```
