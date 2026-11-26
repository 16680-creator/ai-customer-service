<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-config:7.2.0 | path: org/springframework/security/config/annotation/web/builders/HttpSecurity.java -->
<!-- 正文引用：[[源码:HttpSecurity.java#L起-L止]] ；代码块头部：(src: HttpSecurity.java#L起-L止) -->

# HttpSecurity.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-config:7.2.0` |
| 路径 | `org/springframework/security/config/annotation/web/builders/HttpSecurity.java` |
| 行数 | 2154 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-config/7.2.0/spring-security-config-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:52.308Z |

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
   17 | package org.springframework.security.config.annotation.web.builders;
   18 | 
   19 | import java.io.IOException;
   20 | import java.util.ArrayList;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | 
   24 | import jakarta.servlet.Filter;
   25 | import jakarta.servlet.FilterChain;
   26 | import jakarta.servlet.ServletException;
   27 | import jakarta.servlet.ServletRequest;
   28 | import jakarta.servlet.ServletResponse;
   29 | import jakarta.servlet.http.HttpServletRequest;
   30 | 
   31 | import org.springframework.beans.factory.ObjectProvider;
   32 | import org.springframework.context.ApplicationContext;
   33 | import org.springframework.core.OrderComparator;
   34 | import org.springframework.core.Ordered;
   35 | import org.springframework.core.ResolvableType;
   36 | import org.springframework.security.authentication.AuthenticationManager;
   37 | import org.springframework.security.authentication.AuthenticationProvider;
   38 | import org.springframework.security.config.Customizer;
   39 | import org.springframework.security.config.ObjectPostProcessor;
   40 | import org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder;
   41 | import org.springframework.security.config.annotation.SecurityBuilder;
   42 | import org.springframework.security.config.annotation.SecurityConfigurer;
   43 | import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
   44 | import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
   45 | import org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry;
   46 | import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
   47 | import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
   48 | import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
   49 | import org.springframework.security.config.annotation.web.configurers.AnonymousConfigurer;
   50 | import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
   51 | import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry;
   52 | import org.springframework.security.config.annotation.web.configurers.ChannelSecurityConfigurer;
   53 | import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
   54 | import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
   55 | import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
   56 | import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
   57 | import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
   58 | import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
   59 | import org.springframework.security.config.annotation.web.configurers.HttpsRedirectConfigurer;
   60 | import org.springframework.security.config.annotation.web.configurers.JeeConfigurer;
   61 | import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
   62 | import org.springframework.security.config.annotation.web.configurers.PasswordManagementConfigurer;
   63 | import org.springframework.security.config.annotation.web.configurers.PortMapperConfigurer;
   64 | import org.springframework.security.config.annotation.web.configurers.RememberMeConfigurer;
   65 | import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
   66 | import org.springframework.security.config.annotation.web.configurers.SecurityContextConfigurer;
   67 | import org.springframework.security.config.annotation.web.configurers.ServletApiConfigurer;
   68 | import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
   69 | import org.springframework.security.config.annotation.web.configurers.WebAuthnConfigurer;
   70 | import org.springframework.security.config.annotation.web.configurers.X509Configurer;
   71 | import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2ClientConfigurer;
   72 | import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
   73 | import org.springframework.security.config.annotation.web.configurers.oauth2.client.OidcLogoutConfigurer;
   74 | import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
   75 | import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
   76 | import org.springframework.security.config.annotation.web.configurers.ott.OneTimeTokenLoginConfigurer;
   77 | import org.springframework.security.config.annotation.web.configurers.saml2.Saml2LoginConfigurer;
   78 | import org.springframework.security.config.annotation.web.configurers.saml2.Saml2LogoutConfigurer;
   79 | import org.springframework.security.config.annotation.web.configurers.saml2.Saml2MetadataConfigurer;
   80 | import org.springframework.security.core.Authentication;
   81 | import org.springframework.security.core.context.SecurityContext;
   82 | import org.springframework.security.core.context.SecurityContextHolder;
   83 | import org.springframework.security.core.userdetails.UserDetailsService;
   84 | import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
   85 | import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
   86 | import org.springframework.security.web.DefaultSecurityFilterChain;
   87 | import org.springframework.security.web.PortMapper;
   88 | import org.springframework.security.web.PortMapperImpl;
   89 | import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
   90 | import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
   91 | import org.springframework.security.web.context.SecurityContextRepository;
   92 | import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
   93 | import org.springframework.security.web.session.HttpSessionEventPublisher;
   94 | import org.springframework.security.web.util.matcher.AnyRequestMatcher;
   95 | import org.springframework.security.web.util.matcher.OrRequestMatcher;
   96 | import org.springframework.security.web.util.matcher.RequestMatcher;
   97 | import org.springframework.util.Assert;
   98 | import org.springframework.web.cors.CorsConfiguration;
   99 | import org.springframework.web.filter.CorsFilter;
  100 | 
  101 | /**
  102 |  * A {@link HttpSecurity} is similar to Spring Security's XML &lt;http&gt; element in the
  103 |  * namespace configuration. It allows configuring web based security for specific http
  104 |  * requests. By default it will be applied to all requests, but can be restricted using
  105 |  * {@link #authorizeHttpRequests(Customizer)} or other similar methods.
  106 |  *
  107 |  * <h2>Example Usage</h2>
  108 |  *
  109 |  * The most basic form based configuration can be seen below. The configuration will
  110 |  * require that any URL that is requested will require a User with the role "ROLE_USER".
  111 |  * It also defines an in memory authentication scheme with a user that has the username
  112 |  * "user", the password "password", and the role "ROLE_USER". For additional examples,
  113 |  * refer to the Java Doc of individual methods on {@link HttpSecurity}.
  114 |  *
  115 |  * <pre>
  116 |  * &#064;Configuration
  117 |  * &#064;EnableWebSecurity
  118 |  * public class FormLoginSecurityConfig {
  119 |  *
  120 |  * 	&#064;Bean
  121 |  * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  122 |  * 		http
  123 |  * 			.authorizeHttpRequests((authorize) -&gt; authorize
  124 |  * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
  125 |  * 			)
  126 |  * 			.formLogin(withDefaults());
  127 |  * 		return http.build();
  128 |  * 	}
  129 |  *
  130 |  * 	&#064;Bean
  131 |  * 	public UserDetailsService userDetailsService() {
  132 |  * 		UserDetails user = User.withDefaultPasswordEncoder()
  133 |  * 			.username(&quot;user&quot;)
  134 |  * 			.password(&quot;password&quot;)
  135 |  * 			.roles(&quot;USER&quot;)
  136 |  * 			.build();
  137 |  * 		return new InMemoryUserDetailsManager(user);
  138 |  * 	}
  139 |  * }
  140 |  * </pre>
  141 |  *
  142 |  * @author Rob Winch
  143 |  * @author Joe Grandja
  144 |  * @author Ngoc Nhan
  145 |  * @since 3.2
  146 |  * @see EnableWebSecurity
  147 |  */
  148 | public final class HttpSecurity extends AbstractConfiguredSecurityBuilder<DefaultSecurityFilterChain, HttpSecurity>
  149 | 		implements SecurityBuilder<DefaultSecurityFilterChain>, HttpSecurityBuilder<HttpSecurity> {
  150 | 
  151 | 	private final RequestMatcherConfigurer requestMatcherConfigurer;
  152 | 
  153 | 	private List<OrderedFilter> filters = new ArrayList<>();
  154 | 
  155 | 	private RequestMatcher requestMatcher = AnyRequestMatcher.INSTANCE;
  156 | 
  157 | 	private FilterOrderRegistration filterOrders = new FilterOrderRegistration();
  158 | 
  159 | 	private AuthenticationManager authenticationManager;
  160 | 
  161 | 	/**
  162 | 	 * Creates a new instance.
  163 | 	 * @param objectPostProcessor the {@link ObjectPostProcessor} that should be used
  164 | 	 * @param authenticationBuilder the {@link AuthenticationManagerBuilder} to use for
  165 | 	 * additional updates
  166 | 	 * @param sharedObjects the shared Objects to initialize the {@link HttpSecurity} with
  167 | 	 * @see WebSecurityConfiguration
  168 | 	 */
  169 | 	@SuppressWarnings("unchecked")
  170 | 	public HttpSecurity(ObjectPostProcessor<Object> objectPostProcessor,
  171 | 			AuthenticationManagerBuilder authenticationBuilder, Map<Class<?>, Object> sharedObjects) {
  172 | 		super(objectPostProcessor);
  173 | 		Assert.notNull(authenticationBuilder, "authenticationBuilder cannot be null");
  174 | 		setSharedObject(AuthenticationManagerBuilder.class, authenticationBuilder);
  175 | 		for (Map.Entry<Class<?>, Object> entry : sharedObjects.entrySet()) {
  176 | 			setSharedObject((Class<Object>) entry.getKey(), entry.getValue());
  177 | 		}
  178 | 		ApplicationContext context = (ApplicationContext) sharedObjects.get(ApplicationContext.class);
  179 | 		this.requestMatcherConfigurer = new RequestMatcherConfigurer(context);
  180 | 	}
  181 | 
  182 | 	private ApplicationContext getContext() {
  183 | 		return getSharedObject(ApplicationContext.class);
  184 | 	}
  185 | 
  186 | 	/**
  187 | 	 * Adds the Security headers to the response. This is activated by default when using
  188 | 	 * {@link EnableWebSecurity}.
  189 | 	 *
  190 | 	 * <h2>Example Configurations</h2>
  191 | 	 *
  192 | 	 * Accepting the default provided by {@link EnableWebSecurity} or only invoking
  193 | 	 * {@link #headers(Customizer)} without invoking additional methods on it, is the
  194 | 	 * equivalent of:
  195 | 	 *
  196 | 	 * <pre>
  197 | 	 * &#064;Configuration
  198 | 	 * &#064;EnableWebSecurity
  199 | 	 * public class CsrfSecurityConfig {
  200 | 	 *
  201 | 	 *	&#064;Bean
  202 | 	 *	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  203 | 	 *		http
  204 | 	 *			.headers((headers) -&gt;
  205 | 	 *				headers
  206 | 	 *					.contentTypeOptions(withDefaults())
  207 | 	 *					.xssProtection(withDefaults())
  208 | 	 *					.cacheControl(withDefaults())
  209 | 	 *					.httpStrictTransportSecurity(withDefaults())
  210 | 	 *					.frameOptions(withDefaults()
  211 | 	 *			);
  212 | 	 *		return http.build();
  213 | 	 *	}
  214 | 	 * }
  215 | 	 * </pre>
  216 | 	 *
  217 | 	 * You can disable the headers using the following:
  218 | 	 *
  219 | 	 * <pre>
  220 | 	 * &#064;Configuration
  221 | 	 * &#064;EnableWebSecurity
  222 | 	 * public class CsrfSecurityConfig {
  223 | 	 *
  224 | 	 *	&#064;Bean
  225 | 	 *	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  226 | 	 * 		http
  227 | 	 * 			.headers((headers) -&gt; headers.disable());
  228 | 	 *		return http.build();
  229 | 	 *	}
  230 | 	 * }
  231 | 	 * </pre>
  232 | 	 *
  233 | 	 * You can enable only a few of the headers by first invoking
  234 | 	 * {@link HeadersConfigurer#defaultsDisabled()} and then invoking the appropriate
  235 | 	 * methods on the {@link #headers(Customizer)} result. For example, the following will
  236 | 	 * enable {@link HeadersConfigurer#cacheControl(Customizer)} and
  237 | 	 * {@link HeadersConfigurer#frameOptions(Customizer)} only.
  238 | 	 *
  239 | 	 * <pre>
  240 | 	 * &#064;Configuration
  241 | 	 * &#064;EnableWebSecurity
  242 | 	 * public class CsrfSecurityConfig {
  243 | 	 *
  244 | 	 *	&#064;Bean
  245 | 	 *	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  246 | 	 *		http
  247 | 	 *			.headers((headers) -&gt;
  248 | 	 *				headers
  249 | 	 *			 		.defaultsDisabled()
  250 | 	 *			 		.cacheControl(withDefaults())
  251 | 	 *			 		.frameOptions(withDefaults())
  252 | 	 *			);
  253 | 	 *		return http.build();
  254 | 	 * 	}
  255 | 	 * }
  256 | 	 * </pre>
  257 | 	 *
  258 | 	 * You can also choose to keep the defaults but explicitly disable a subset of
  259 | 	 * headers. For example, the following will enable all the default headers except
  260 | 	 * {@link HeadersConfigurer#frameOptions(Customizer)}.
  261 | 	 *
  262 | 	 * <pre>
  263 | 	 * &#064;Configuration
  264 | 	 * &#064;EnableWebSecurity
  265 | 	 * public class CsrfSecurityConfig {
  266 | 	 *
  267 | 	 * 	&#064;Bean
  268 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  269 | 	 *  	http
  270 | 	 *  		.headers((headers) -&gt;
  271 | 	 *  			headers
  272 | 	 *  				.frameOptions((frameOptions) -&gt; frameOptions.disable())
  273 | 	 *  		);
  274 | 	 * 		return http.build();
  275 | 	 * 	}
  276 | 	 * }
  277 | 	 * </pre>
  278 | 	 * @param headersCustomizer the {@link Customizer} to provide more options for the
  279 | 	 * {@link HeadersConfigurer}
  280 | 	 * @return the {@link HttpSecurity} for further customizations @
  281 | 	 */
  282 | 	public HttpSecurity headers(Customizer<HeadersConfigurer<HttpSecurity>> headersCustomizer) {
  283 | 		headersCustomizer.customize(getOrApply(new HeadersConfigurer<>()));
  284 | 		return HttpSecurity.this;
  285 | 	}
  286 | 
  287 | 	/**
  288 | 	 * Adds a {@link CorsFilter} to be used. If a bean by the name of corsFilter is
  289 | 	 * provided, that {@link CorsFilter} is used. Else if corsConfigurationSource is
  290 | 	 * defined, then that {@link CorsConfiguration} is used. You can enable CORS using:
  291 | 	 *
  292 | 	 * <pre>
  293 | 	 * &#064;Configuration
  294 | 	 * &#064;EnableWebSecurity
  295 | 	 * public class CorsSecurityConfig {
  296 | 	 *
  297 | 	 * 	&#064;Bean
  298 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  299 | 	 * 		http
  300 | 	 * 			.cors(withDefaults());
  301 | 	 * 		return http.build();
  302 | 	 * 	}
  303 | 	 * }
  304 | 	 * </pre>
  305 | 	 * @param corsCustomizer the {@link Customizer} to provide more options for the
  306 | 	 * {@link CorsConfigurer}
  307 | 	 * @return the {@link HttpSecurity} for further customizations @
  308 | 	 */
  309 | 	public HttpSecurity cors(Customizer<CorsConfigurer<HttpSecurity>> corsCustomizer) {
  310 | 		corsCustomizer.customize(getOrApply(new CorsConfigurer<>()));
  311 | 		return HttpSecurity.this;
  312 | 	}
  313 | 
  314 | 	/**
  315 | 	 * Allows configuring of Session Management.
  316 | 	 *
  317 | 	 * <h2>Example Configuration</h2>
  318 | 	 *
  319 | 	 * The following configuration demonstrates how to enforce that only a single instance
  320 | 	 * of a user is authenticated at a time. If a user authenticates with the username
  321 | 	 * "user" without logging out and an attempt to authenticate with "user" is made the
  322 | 	 * first session will be forcibly terminated and sent to the "/login?expired" URL.
  323 | 	 *
  324 | 	 * <pre>
  325 | 	 * &#064;Configuration
  326 | 	 * &#064;EnableWebSecurity
  327 | 	 * public class SessionManagementSecurityConfig {
  328 | 	 *
  329 | 	 * 	&#064;Bean
  330 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  331 | 	 * 		http
  332 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
  333 | 	 * 				authorizeHttpRequests
  334 | 	 * 					.anyRequest().hasRole(&quot;USER&quot;)
  335 | 	 * 			)
  336 | 	 * 			.formLogin((formLogin) -&gt;
  337 | 	 * 				formLogin
  338 | 	 * 					.permitAll()
  339 | 	 * 			)
  340 | 	 * 			.sessionManagement((sessionManagement) -&gt;
  341 | 	 * 				sessionManagement
  342 | 	 * 					.sessionConcurrency((sessionConcurrency) -&gt;
  343 | 	 * 						sessionConcurrency
  344 | 	 * 							.maximumSessions(1)
  345 | 	 * 							.expiredUrl(&quot;/login?expired&quot;)
  346 | 	 * 					)
  347 | 	 * 			);
  348 | 	 * 		return http.build();
  349 | 	 * 	}
  350 | 	 *
  351 | 	 * 	&#064;Bean
  352 | 	 * 	public UserDetailsService userDetailsService() {
  353 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
  354 | 	 * 			.username(&quot;user&quot;)
  355 | 	 * 			.password(&quot;password&quot;)
  356 | 	 * 			.roles(&quot;USER&quot;)
  357 | 	 * 			.build();
  358 | 	 * 		return new InMemoryUserDetailsManager(user);
  359 | 	 * 	}
  360 | 	 * }
  361 | 	 * </pre>
  362 | 	 *
  363 | 	 * When using {@link SessionManagementConfigurer#maximumSessions(int)}, do not forget
  364 | 	 * to configure {@link HttpSessionEventPublisher} for the application to ensure that
  365 | 	 * expired sessions are cleaned up.
  366 | 	 *
  367 | 	 * In a web.xml this can be configured using the following:
  368 | 	 *
  369 | 	 * <pre>
  370 | 	 * &lt;listener&gt;
  371 | 	 *      &lt;listener-class&gt;org.springframework.security.web.session.HttpSessionEventPublisher&lt;/listener-class&gt;
  372 | 	 * &lt;/listener&gt;
  373 | 	 * </pre>
  374 | 	 *
  375 | 	 * Alternatively,
  376 | 	 * {@link AbstractSecurityWebApplicationInitializer#enableHttpSessionEventPublisher()}
  377 | 	 * could return true.
  378 | 	 * @param sessionManagementCustomizer the {@link Customizer} to provide more options
  379 | 	 * for the {@link SessionManagementConfigurer}
  380 | 	 * @return the {@link HttpSecurity} for further customizations @
  381 | 	 */
  382 | 	public HttpSecurity sessionManagement(
  383 | 			Customizer<SessionManagementConfigurer<HttpSecurity>> sessionManagementCustomizer) {
  384 | 		sessionManagementCustomizer.customize(getOrApply(new SessionManagementConfigurer<>()));
  385 | 		return HttpSecurity.this;
  386 | 	}
  387 | 
  388 | 	/**
  389 | 	 * Allows configuring a {@link PortMapper} that is available from
  390 | 	 * {@link HttpSecurity#getSharedObject(Class)}. Other provided
  391 | 	 * {@link SecurityConfigurer} objects use this configured {@link PortMapper} as a
  392 | 	 * default {@link PortMapper} when redirecting from HTTP to HTTPS or from HTTPS to
  393 | 	 * HTTP (for example when used in combination with
  394 | 	 * {@link #requiresChannel(Customizer)} )}. By default Spring Security uses a
  395 | 	 * {@link PortMapperImpl} which maps the HTTP port 8080 to the HTTPS port 8443 and the
  396 | 	 * HTTP port of 80 to the HTTPS port of 443.
  397 | 	 *
  398 | 	 * <h2>Example Configuration</h2>
  399 | 	 *
  400 | 	 * The following configuration will ensure that redirects within Spring Security from
  401 | 	 * HTTP of a port of 9090 will redirect to HTTPS port of 9443 and the HTTP port of 80
  402 | 	 * to the HTTPS port of 443.
  403 | 	 *
  404 | 	 * <pre>
  405 | 	 * &#064;Configuration
  406 | 	 * &#064;EnableWebSecurity
  407 | 	 * public class PortMapperSecurityConfig {
  408 | 	 *
  409 | 	 * 	&#064;Bean
  410 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  411 | 	 * 		http
  412 | 	 * 			.requiresChannel((requiresChannel) -&gt;
  413 | 	 * 				requiresChannel
  414 | 	 * 					.anyRequest().requiresSecure()
  415 | 	 * 			)
  416 | 	 * 			.portMapper((portMapper) -&gt;
  417 | 	 * 				portMapper
  418 | 	 * 					.http(9090).mapsTo(9443)
  419 | 	 * 					.http(80).mapsTo(443)
  420 | 	 * 			);
  421 | 	 * 		return http.build();
  422 | 	 * 	}
  423 | 	 *
  424 | 	 * 	&#064;Bean
  425 | 	 * 	public UserDetailsService userDetailsService() {
  426 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
  427 | 	 * 			.username(&quot;user&quot;)
  428 | 	 * 			.password(&quot;password&quot;)
  429 | 	 * 			.roles(&quot;USER&quot;)
  430 | 	 * 			.build();
  431 | 	 * 		return new InMemoryUserDetailsManager(user);
  432 | 	 * 	}
  433 | 	 * }
  434 | 	 * </pre>
  435 | 	 * @param portMapperCustomizer the {@link Customizer} to provide more options for the
  436 | 	 * {@link PortMapperConfigurer}
  437 | 	 * @return the {@link HttpSecurity} for further customizations
  438 | 	 * @ @see #requiresChannel(Customizer)
  439 | 	 */
  440 | 	public HttpSecurity portMapper(Customizer<PortMapperConfigurer<HttpSecurity>> portMapperCustomizer) {
  441 | 		portMapperCustomizer.customize(getOrApply(new PortMapperConfigurer<>()));
  442 | 		return HttpSecurity.this;
  443 | 	}
  444 | 
  445 | 	/**
  446 | 	 * Configures container based pre authentication. In this case, authentication is
  447 | 	 * managed by the Servlet Container.
  448 | 	 *
  449 | 	 * <h2>Example Configuration</h2>
  450 | 	 *
  451 | 	 * The following configuration will use the principal found on the
  452 | 	 * {@link HttpServletRequest} and if the user is in the role "ROLE_USER" or
  453 | 	 * "ROLE_ADMIN" will add that to the resulting {@link Authentication}.
  454 | 	 *
  455 | 	 * <pre>
  456 | 	 * &#064;Configuration
  457 | 	 * &#064;EnableWebSecurity
  458 | 	 * public class JeeSecurityConfig {
  459 | 	 *
  460 | 	 * 	&#064;Bean
  461 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  462 | 	 * 		http
  463 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
  464 | 	 * 				authorizeHttpRequests
  465 | 	 * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
  466 | 	 * 			)
  467 | 	 * 			.jee((jee) -&gt;
  468 | 	 * 				jee
  469 | 	 * 					.mappableRoles(&quot;USER&quot;, &quot;ADMIN&quot;)
  470 | 	 * 			);
  471 | 	 * 		return http.build();
  472 | 	 * 	}
  473 | 	 * }
  474 | 	 * </pre>
  475 | 	 *
  476 | 	 * Developers wishing to use pre authentication with the container will need to ensure
  477 | 	 * their web.xml configures the security constraints. For example, the web.xml (there
  478 | 	 * is no equivalent Java based configuration supported by the Servlet specification)
  479 | 	 * might look like:
  480 | 	 *
  481 | 	 * <pre>
  482 | 	 * &lt;login-config&gt;
  483 | 	 *     &lt;auth-method&gt;FORM&lt;/auth-method&gt;
  484 | 	 *     &lt;form-login-config&gt;
  485 | 	 *         &lt;form-login-page&gt;/login&lt;/form-login-page&gt;
  486 | 	 *         &lt;form-error-page&gt;/login?error&lt;/form-error-page&gt;
  487 | 	 *     &lt;/form-login-config&gt;
  488 | 	 * &lt;/login-config&gt;
  489 | 	 *
  490 | 	 * &lt;security-role&gt;
  491 | 	 *     &lt;role-name&gt;ROLE_USER&lt;/role-name&gt;
  492 | 	 * &lt;/security-role&gt;
  493 | 	 * &lt;security-constraint&gt;
  494 | 	 *     &lt;web-resource-collection&gt;
  495 | 	 *     &lt;web-resource-name&gt;Public&lt;/web-resource-name&gt;
  496 | 	 *         &lt;description&gt;Matches unconstrained pages&lt;/description&gt;
  497 | 	 *         &lt;url-pattern&gt;/login&lt;/url-pattern&gt;
  498 | 	 *         &lt;url-pattern&gt;/logout&lt;/url-pattern&gt;
  499 | 	 *         &lt;url-pattern&gt;/resources/*&lt;/url-pattern&gt;
  500 | 	 *     &lt;/web-resource-collection&gt;
  501 | 	 * &lt;/security-constraint&gt;
  502 | 	 * &lt;security-constraint&gt;
  503 | 	 *     &lt;web-resource-collection&gt;
  504 | 	 *         &lt;web-resource-name&gt;Secured Areas&lt;/web-resource-name&gt;
  505 | 	 *         &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
  506 | 	 *     &lt;/web-resource-collection&gt;
  507 | 	 *     &lt;auth-constraint&gt;
  508 | 	 *         &lt;role-name&gt;ROLE_USER&lt;/role-name&gt;
  509 | 	 *     &lt;/auth-constraint&gt;
  510 | 	 * &lt;/security-constraint&gt;
  511 | 	 * </pre>
  512 | 	 *
  513 | 	 * Last you will need to configure your container to contain the user with the correct
  514 | 	 * roles. This configuration is specific to the Servlet Container, so consult your
  515 | 	 * Servlet Container's documentation.
  516 | 	 * @param jeeCustomizer the {@link Customizer} to provide more options for the
  517 | 	 * {@link JeeConfigurer}
  518 | 	 * @return the {@link HttpSecurity} for further customizations @
  519 | 	 */
  520 | 	public HttpSecurity jee(Customizer<JeeConfigurer<HttpSecurity>> jeeCustomizer) {
  521 | 		jeeCustomizer.customize(getOrApply(new JeeConfigurer<>()));
  522 | 		return HttpSecurity.this;
  523 | 	}
  524 | 
  525 | 	/**
  526 | 	 * Configures X509 based pre authentication.
  527 | 	 *
  528 | 	 * <h2>Example Configuration</h2>
  529 | 	 *
  530 | 	 * The following configuration will attempt to extract the username from the X509
  531 | 	 * certificate. Remember that the Servlet Container will need to be configured to
  532 | 	 * request client certificates in order for this to work.
  533 | 	 *
  534 | 	 * <pre>
  535 | 	 * &#064;Configuration
  536 | 	 * &#064;EnableWebSecurity
  537 | 	 * public class X509SecurityConfig {
  538 | 	 *
  539 | 	 * 	&#064;Bean
  540 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  541 | 	 * 		http
  542 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
  543 | 	 * 				authorizeHttpRequests
  544 | 	 * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
  545 | 	 * 			)
  546 | 	 * 			.x509(withDefaults());
  547 | 	 * 		return http.build();
  548 | 	 * 	}
  549 | 	 * }
  550 | 	 * </pre>
  551 | 	 * @param x509Customizer the {@link Customizer} to provide more options for the
  552 | 	 * {@link X509Configurer}
  553 | 	 * @return the {@link HttpSecurity} for further customizations @
  554 | 	 */
  555 | 	public HttpSecurity x509(Customizer<X509Configurer<HttpSecurity>> x509Customizer) {
  556 | 		x509Customizer.customize(getOrApply(new X509Configurer<>()));
  557 | 		return HttpSecurity.this;
  558 | 	}
  559 | 
  560 | 	/**
  561 | 	 * Allows configuring of Remember Me authentication.
  562 | 	 *
  563 | 	 * <h2>Example Configuration</h2>
  564 | 	 *
  565 | 	 * The following configuration demonstrates how to allow token based remember me
  566 | 	 * authentication. Upon authenticating if the HTTP parameter named "remember-me"
  567 | 	 * exists, then the user will be remembered even after their
  568 | 	 * {@link jakarta.servlet.http.HttpSession} expires.
  569 | 	 *
  570 | 	 * <pre>
  571 | 	 * &#064;Configuration
  572 | 	 * &#064;EnableWebSecurity
  573 | 	 * public class RememberMeSecurityConfig {
  574 | 	 *
  575 | 	 * 	&#064;Bean
  576 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  577 | 	 * 		http
  578 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
  579 | 	 * 				authorizeHttpRequests
  580 | 	 * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
  581 | 	 * 			)
  582 | 	 * 			.formLogin(withDefaults())
  583 | 	 * 			.rememberMe(withDefaults());
  584 | 	 * 		return http.build();
  585 | 	 * 	}
  586 | 	 *
  587 | 	 * 	&#064;Bean
  588 | 	 * 	public UserDetailsService userDetailsService() {
  589 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
  590 | 	 * 			.username(&quot;user&quot;)
  591 | 	 * 			.password(&quot;password&quot;)
  592 | 	 * 			.roles(&quot;USER&quot;)
  593 | 	 * 			.build();
  594 | 	 * 		return new InMemoryUserDetailsManager(user);
  595 | 	 * 	}
  596 | 	 * }
  597 | 	 * </pre>
  598 | 	 * @param rememberMeCustomizer the {@link Customizer} to provide more options for the
  599 | 	 * {@link RememberMeConfigurer}
  600 | 	 * @return the {@link HttpSecurity} for further customizations @
  601 | 	 */
  602 | 	public HttpSecurity rememberMe(Customizer<RememberMeConfigurer<HttpSecurity>> rememberMeCustomizer) {
  603 | 		rememberMeCustomizer.customize(getOrApply(new RememberMeConfigurer<>()));
  604 | 		return HttpSecurity.this;
  605 | 	}
  606 | 
  607 | 	/**
  608 | 	 * Allows restricting access based upon the {@link HttpServletRequest} using
  609 | 	 * {@link RequestMatcher} implementations (i.e. via URL patterns).
  610 | 	 *
  611 | 	 * <h2>Example Configurations</h2>
  612 | 	 *
  613 | 	 * The most basic example is to configure all URLs to require the role "ROLE_USER".
  614 | 	 * The configuration below requires authentication to every URL and will grant access
  615 | 	 * to both the user "admin" and "user".
  616 | 	 *
  617 | 	 * <pre>
  618 | 	 * &#064;Configuration
  619 | 	 * &#064;EnableWebSecurity
  620 | 	 * public class AuthorizeUrlsSecurityConfig {
  621 | 	 *
  622 | 	 * 	&#064;Bean
  623 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  624 | 	 * 		http
  625 | 	 * 			.authorizeHttpRequests((authorize) -&gt; authorize
  626 | 	 * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
  627 | 	 * 			)
  628 | 	 * 			.formLogin(withDefaults());
  629 | 	 * 		return http.build();
  630 | 	 * 	}
  631 | 	 *
  632 | 	 * 	&#064;Bean
  633 | 	 * 	public UserDetailsService userDetailsService() {
  634 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
  635 | 	 * 			.username(&quot;user&quot;)
  636 | 	 * 			.password(&quot;password&quot;)
  637 | 	 * 			.roles(&quot;USER&quot;)
  638 | 	 * 			.build();
  639 | 	 * 		UserDetails admin = User.withDefaultPasswordEncoder()
  640 | 	 * 			.username(&quot;admin&quot;)
  641 | 	 * 			.password(&quot;password&quot;)
  642 | 	 * 			.roles(&quot;ADMIN&quot;, &quot;USER&quot;)
  643 | 	 * 			.build();
  644 | 	 * 		return new InMemoryUserDetailsManager(user, admin);
  645 | 	 * 	}
  646 | 	 * }
  647 | 	 * </pre>
  648 | 	 *
  649 | 	 * We can also configure multiple URLs. The configuration below requires
  650 | 	 * authentication to every URL and will grant access to URLs starting with /admin/ to
  651 | 	 * only the "admin" user. All other URLs either user can access.
  652 | 	 *
  653 | 	 * <pre>
  654 | 	 * &#064;Configuration
  655 | 	 * &#064;EnableWebSecurity
  656 | 	 * public class AuthorizeUrlsSecurityConfig {
  657 | 	 *
  658 | 	 * 	&#064;Bean
  659 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  660 | 	 * 		http
  661 | 	 * 			.authorizeHttpRequests((authorize) -&gt; authorize
  662 | 	 * 					.requestMatchers(&quot;/admin/**&quot;).hasRole(&quot;ADMIN&quot;)
  663 | 	 * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
  664 | 	 * 			)
  665 | 	 * 			.formLogin(withDefaults());
  666 | 	 * 		return http.build();
  667 | 	 * 	}
  668 | 	 *
  669 | 	 * 	&#064;Bean
  670 | 	 * 	public UserDetailsService userDetailsService() {
  671 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
  672 | 	 * 			.username(&quot;user&quot;)
  673 | 	 * 			.password(&quot;password&quot;)
  674 | 	 * 			.roles(&quot;USER&quot;)
  675 | 	 * 			.build();
  676 | 	 * 		UserDetails admin = User.withDefaultPasswordEncoder()
  677 | 	 * 			.username(&quot;admin&quot;)
  678 | 	 * 			.password(&quot;password&quot;)
  679 | 	 * 			.roles(&quot;ADMIN&quot;, &quot;USER&quot;)
  680 | 	 * 			.build();
  681 | 	 * 		return new InMemoryUserDetailsManager(user, admin);
  682 | 	 * 	}
  683 | 	 * }
  684 | 	 * </pre>
  685 | 	 *
  686 | 	 * Note that the matchers are considered in order. Therefore, the following is invalid
  687 | 	 * because the first matcher matches every request and will never get to the second
  688 | 	 * mapping:
  689 | 	 *
  690 | 	 * <pre>
  691 | 	 * &#064;Configuration
  692 | 	 * &#064;EnableWebSecurity
  693 | 	 * public class AuthorizeUrlsSecurityConfig {
  694 | 	 *
  695 | 	 * 	&#064;Bean
  696 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  697 | 	 * 		http
  698 | 	 * 		 	.authorizeHttpRequests((authorize) -&gt; authorize
  699 | 	 * 			 		.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
  700 | 	 * 			 		.requestMatchers(&quot;/admin/**&quot;).hasRole(&quot;ADMIN&quot;)
  701 | 	 * 		 	);
  702 | 	 * 		return http.build();
  703 | 	 * 	}
  704 | 	 * }
  705 | 	 * </pre>
  706 | 	 * @param authorizeHttpRequestsCustomizer the {@link Customizer} to provide more
  707 | 	 * options for the {@link AuthorizationManagerRequestMatcherRegistry}
  708 | 	 * @return the {@link HttpSecurity} for further customizations
  709 | 	 * @ @since 5.5
  710 | 	 */
  711 | 	public HttpSecurity authorizeHttpRequests(
  712 | 			Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> authorizeHttpRequestsCustomizer) {
  713 | 		ApplicationContext context = getContext();
  714 | 		authorizeHttpRequestsCustomizer
  715 | 			.customize(getOrApply(new AuthorizeHttpRequestsConfigurer<>(context)).getRegistry());
  716 | 		return HttpSecurity.this;
  717 | 	}
  718 | 
  719 | 	/**
  720 | 	 * Allows configuring the Request Cache. For example, a protected page (/protected)
  721 | 	 * may be requested prior to authentication. The application will redirect the user to
  722 | 	 * a login page. After authentication, Spring Security will redirect the user to the
  723 | 	 * originally requested protected page (/protected). This is automatically applied
  724 | 	 * when using {@link EnableWebSecurity}.
  725 | 	 *
  726 | 	 * <h2>Example Custom Configuration</h2>
  727 | 	 *
  728 | 	 * The following example demonstrates how to disable request caching.
  729 | 	 *
  730 | 	 * <pre>
  731 | 	 * &#064;Configuration
  732 | 	 * &#064;EnableWebSecurity
  733 | 	 * public class RequestCacheDisabledSecurityConfig {
  734 | 	 *
  735 | 	 * 	&#064;Bean
  736 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  737 | 	 * 		http
  738 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
  739 | 	 * 				authorizeHttpRequests
  740 | 	 * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
  741 | 	 * 			)
  742 | 	 * 			.requestCache((requestCache) -&gt;
  743 | 	 * 				requestCache.disable()
  744 | 	 * 			);
  745 | 	 * 		return http.build();
  746 | 	 * 	}
  747 | 	 * }
  748 | 	 * </pre>
  749 | 	 * @param requestCacheCustomizer the {@link Customizer} to provide more options for
  750 | 	 * the {@link RequestCacheConfigurer}
  751 | 	 * @return the {@link HttpSecurity} for further customizations @
  752 | 	 */
  753 | 	public HttpSecurity requestCache(Customizer<RequestCacheConfigurer<HttpSecurity>> requestCacheCustomizer) {
  754 | 		requestCacheCustomizer.customize(getOrApply(new RequestCacheConfigurer<>()));
  755 | 		return HttpSecurity.this;
  756 | 	}
  757 | 
  758 | 	/**
  759 | 	 * Allows configuring exception handling. This is automatically applied when using
  760 | 	 * {@link EnableWebSecurity}.
  761 | 	 *
  762 | 	 * <h2>Example Custom Configuration</h2>
  763 | 	 *
  764 | 	 * The following customization will ensure that users who are denied access are
  765 | 	 * forwarded to the page "/errors/access-denied".
  766 | 	 *
  767 | 	 * <pre>
  768 | 	 * &#064;Configuration
  769 | 	 * &#064;EnableWebSecurity
  770 | 	 * public class ExceptionHandlingSecurityConfig {
  771 | 	 *
  772 | 	 * 	&#064;Bean
  773 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  774 | 	 * 		http
  775 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
  776 | 	 * 				authorizeHttpRequests
  777 | 	 * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
  778 | 	 * 			)
  779 | 	 * 			// sample exception handling customization
  780 | 	 * 			.exceptionHandling((exceptionHandling) -&gt;
  781 | 	 * 				exceptionHandling
  782 | 	 * 					.accessDeniedPage(&quot;/errors/access-denied&quot;)
  783 | 	 * 			);
  784 | 	 * 		return http.build();
  785 | 	 * 	}
  786 | 	 * }
  787 | 	 * </pre>
  788 | 	 * @param exceptionHandlingCustomizer the {@link Customizer} to provide more options
  789 | 	 * for the {@link ExceptionHandlingConfigurer}
  790 | 	 * @return the {@link HttpSecurity} for further customizations @
  791 | 	 */
  792 | 	public HttpSecurity exceptionHandling(
  793 | 			Customizer<ExceptionHandlingConfigurer<HttpSecurity>> exceptionHandlingCustomizer) {
  794 | 		exceptionHandlingCustomizer.customize(getOrApply(new ExceptionHandlingConfigurer<>()));
  795 | 		return HttpSecurity.this;
  796 | 	}
  797 | 
  798 | 	/**
  799 | 	 * Sets up management of the {@link SecurityContext} on the
  800 | 	 * {@link SecurityContextHolder} between {@link HttpServletRequest}'s. This is
  801 | 	 * automatically applied when using {@link EnableWebSecurity}.
  802 | 	 *
  803 | 	 * The following customization specifies the shared {@link SecurityContextRepository}
  804 | 	 *
  805 | 	 * <pre>
  806 | 	 * &#064;Configuration
  807 | 	 * &#064;EnableWebSecurity
  808 | 	 * public class SecurityContextSecurityConfig {
  809 | 	 *
  810 | 	 * 	&#064;Bean
  811 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  812 | 	 * 		http
  813 | 	 * 			.securityContext((securityContext) -&gt;
  814 | 	 * 				securityContext
  815 | 	 * 					.securityContextRepository(SCR)
  816 | 	 * 			);
  817 | 	 * 		return http.build();
  818 | 	 * 	}
  819 | 	 * }
  820 | 	 * </pre>
  821 | 	 * @param securityContextCustomizer the {@link Customizer} to provide more options for
  822 | 	 * the {@link SecurityContextConfigurer}
  823 | 	 * @return the {@link HttpSecurity} for further customizations @
  824 | 	 */
  825 | 	public HttpSecurity securityContext(Customizer<SecurityContextConfigurer<HttpSecurity>> securityContextCustomizer) {
  826 | 		securityContextCustomizer.customize(getOrApply(new SecurityContextConfigurer<>()));
  827 | 		return HttpSecurity.this;
  828 | 	}
  829 | 
  830 | 	/**
  831 | 	 * Integrates the {@link HttpServletRequest} methods with the values found on the
  832 | 	 * {@link SecurityContext}. This is automatically applied when using
  833 | 	 * {@link EnableWebSecurity}. You can disable it using:
  834 | 	 *
  835 | 	 * <pre>
  836 | 	 * &#064;Configuration
  837 | 	 * &#064;EnableWebSecurity
  838 | 	 * public class ServletApiSecurityConfig {
  839 | 	 *
  840 | 	 * 	&#064;Bean
  841 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  842 | 	 * 		http
  843 | 	 * 			.servletApi((servletApi) -&gt;
  844 | 	 * 				servletApi.disable()
  845 | 	 * 			);
  846 | 	 * 		return http.build();
  847 | 	 * 	}
  848 | 	 * }
  849 | 	 * </pre>
  850 | 	 * @param servletApiCustomizer the {@link Customizer} to provide more options for the
  851 | 	 * {@link ServletApiConfigurer}
  852 | 	 * @return the {@link HttpSecurity} for further customizations @
  853 | 	 */
  854 | 	public HttpSecurity servletApi(Customizer<ServletApiConfigurer<HttpSecurity>> servletApiCustomizer) {
  855 | 		servletApiCustomizer.customize(getOrApply(new ServletApiConfigurer<>()));
  856 | 		return HttpSecurity.this;
  857 | 	}
  858 | 
  859 | 	/**
  860 | 	 * Enables CSRF protection. This is activated by default when using
  861 | 	 * {@link EnableWebSecurity}. You can disable it using:
  862 | 	 *
  863 | 	 * <pre>
  864 | 	 * &#064;Configuration
  865 | 	 * &#064;EnableWebSecurity
  866 | 	 * public class CsrfSecurityConfig {
  867 | 	 *
  868 | 	 * 	&#064;Bean
  869 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  870 | 	 * 		http
  871 | 	 * 			.csrf((csrf) -&gt; csrf.disable());
  872 | 	 * 		return http.build();
  873 | 	 * 	}
  874 | 	 * }
  875 | 	 * </pre>
  876 | 	 * @param csrfCustomizer the {@link Customizer} to provide more options for the
  877 | 	 * {@link CsrfConfigurer}
  878 | 	 * @return the {@link HttpSecurity} for further customizations @
  879 | 	 */
  880 | 	public HttpSecurity csrf(Customizer<CsrfConfigurer<HttpSecurity>> csrfCustomizer) {
  881 | 		ApplicationContext context = getContext();
  882 | 		csrfCustomizer.customize(getOrApply(new CsrfConfigurer<>(context)));
  883 | 		return HttpSecurity.this;
  884 | 	}
  885 | 
  886 | 	/**
  887 | 	 * Provides logout support. This is automatically applied when using
  888 | 	 * {@link EnableWebSecurity}. The default is that accessing the URL "/logout" will log
  889 | 	 * the user out by invalidating the HTTP Session, cleaning up any
  890 | 	 * {@link #rememberMe(Customizer)} authentication that was configured, clearing the
  891 | 	 * {@link SecurityContextHolder}, and then redirect to "/login?success".
  892 | 	 *
  893 | 	 * <h2>Example Custom Configuration</h2>
  894 | 	 *
  895 | 	 * The following customization to log out when the URL "/custom-logout" is invoked.
  896 | 	 * Log out will remove the cookie named "remove", not invalidate the HttpSession,
  897 | 	 * clear the SecurityContextHolder, and upon completion redirect to "/logout-success".
  898 | 	 *
  899 | 	 * <pre>
  900 | 	 * &#064;Configuration
  901 | 	 * &#064;EnableWebSecurity
  902 | 	 * public class LogoutSecurityConfig {
  903 | 	 *
  904 | 	 * 	&#064;Bean
  905 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  906 | 	 * 		http
  907 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
  908 | 	 * 				authorizeHttpRequests
  909 | 	 * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
  910 | 	 * 			)
  911 | 	 * 			.formLogin(withDefaults())
  912 | 	 * 			// sample logout customization
  913 | 	 * 			.logout((logout) -&gt;
  914 | 	 * 				logout.deleteCookies(&quot;remove&quot;)
  915 | 	 * 					.invalidateHttpSession(false)
  916 | 	 * 					.logoutUrl(&quot;/custom-logout&quot;)
  917 | 	 * 					.logoutSuccessUrl(&quot;/logout-success&quot;)
  918 | 	 * 			);
  919 | 	 * 		return http.build();
  920 | 	 * 	}
  921 | 	 *
  922 | 	 * 	&#064;Bean
  923 | 	 * 	public UserDetailsService userDetailsService() {
  924 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
  925 | 	 * 			.username(&quot;user&quot;)
  926 | 	 * 			.password(&quot;password&quot;)
  927 | 	 * 			.roles(&quot;USER&quot;)
  928 | 	 * 			.build();
  929 | 	 * 		return new InMemoryUserDetailsManager(user);
  930 | 	 * 	}
  931 | 	 * }
  932 | 	 * </pre>
  933 | 	 * @param logoutCustomizer the {@link Customizer} to provide more options for the
  934 | 	 * {@link LogoutConfigurer}
  935 | 	 * @return the {@link HttpSecurity} for further customizations @
  936 | 	 */
  937 | 	public HttpSecurity logout(Customizer<LogoutConfigurer<HttpSecurity>> logoutCustomizer) {
  938 | 		logoutCustomizer.customize(getOrApply(new LogoutConfigurer<>()));
  939 | 		return HttpSecurity.this;
  940 | 	}
  941 | 
  942 | 	/**
  943 | 	 * Allows configuring how an anonymous user is represented. This is automatically
  944 | 	 * applied when used in conjunction with {@link EnableWebSecurity}. By default
  945 | 	 * anonymous users will be represented with an
  946 | 	 * {@link org.springframework.security.authentication.AnonymousAuthenticationToken}
  947 | 	 * and contain the role "ROLE_ANONYMOUS".
  948 | 	 *
  949 | 	 * <h2>Example Configuration</h2>
  950 | 	 *
  951 | 	 * The following configuration demonstrates how to specify that anonymous users should
  952 | 	 * contain the role "ROLE_ANON" instead.
  953 | 	 *
  954 | 	 * <pre>
  955 | 	 * &#064;Configuration
  956 | 	 * &#064;EnableWebSecurity
  957 | 	 * public class AnonymousSecurityConfig {
  958 | 	 *
  959 | 	 * 	&#064;Bean
  960 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  961 | 	 * 		http
  962 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
  963 | 	 * 				authorizeHttpRequests
  964 | 	 * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
  965 | 	 * 			)
  966 | 	 * 			.formLogin(withDefaults())
  967 | 	 * 			// sample anonymous customization
  968 | 	 * 			.anonymous((anonymous) -&gt;
  969 | 	 * 				anonymous
  970 | 	 * 					.authorities(&quot;ROLE_ANON&quot;)
  971 | 	 * 			);
  972 | 	 * 		return http.build();
  973 | 	 * 	}
  974 | 	 *
  975 | 	 * 	&#064;Bean
  976 | 	 * 	public UserDetailsService userDetailsService() {
  977 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
  978 | 	 * 			.username(&quot;user&quot;)
  979 | 	 * 			.password(&quot;password&quot;)
  980 | 	 * 			.roles(&quot;USER&quot;)
  981 | 	 * 			.build();
  982 | 	 * 		return new InMemoryUserDetailsManager(user);
  983 | 	 * 	}
  984 | 	 * }
  985 | 	 * </pre>
  986 | 	 *
  987 | 	 * The following demonstrates how to represent anonymous users as null. Note that this
  988 | 	 * can cause {@link NullPointerException} in code that assumes anonymous
  989 | 	 * authentication is enabled.
  990 | 	 *
  991 | 	 * <pre>
  992 | 	 * &#064;Configuration
  993 | 	 * &#064;EnableWebSecurity
  994 | 	 * public class AnonymousSecurityConfig {
  995 | 	 *
  996 | 	 * 	&#064;Bean
  997 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
  998 | 	 * 		http
  999 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
 1000 | 	 * 				authorizeHttpRequests
 1001 | 	 * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
 1002 | 	 * 			)
 1003 | 	 * 			.formLogin(withDefaults())
 1004 | 	 * 			// sample anonymous customization
 1005 | 	 * 			.anonymous((anonymous) -&gt;
 1006 | 	 * 				anonymous.disable()
 1007 | 	 * 			);
 1008 | 	 * 		return http.build();
 1009 | 	 * 	}
 1010 | 	 *
 1011 | 	 * 	&#064;Bean
 1012 | 	 * 	public UserDetailsService userDetailsService() {
 1013 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
 1014 | 	 * 			.username(&quot;user&quot;)
 1015 | 	 * 			.password(&quot;password&quot;)
 1016 | 	 * 			.roles(&quot;USER&quot;)
 1017 | 	 * 			.build();
 1018 | 	 * 		return new InMemoryUserDetailsManager(user);
 1019 | 	 * 	}
 1020 | 	 * }
 1021 | 	 * </pre>
 1022 | 	 * @param anonymousCustomizer the {@link Customizer} to provide more options for the
 1023 | 	 * {@link AnonymousConfigurer}
 1024 | 	 * @return the {@link HttpSecurity} for further customizations @
 1025 | 	 */
 1026 | 	public HttpSecurity anonymous(Customizer<AnonymousConfigurer<HttpSecurity>> anonymousCustomizer) {
 1027 | 		anonymousCustomizer.customize(getOrApply(new AnonymousConfigurer<>()));
 1028 | 		return HttpSecurity.this;
 1029 | 	}
 1030 | 
 1031 | 	/**
 1032 | 	 * Specifies to support form based authentication. If
 1033 | 	 * {@link FormLoginConfigurer#loginPage(String)} is not specified a default login page
 1034 | 	 * will be generated.
 1035 | 	 *
 1036 | 	 * <h2>Example Configurations</h2>
 1037 | 	 *
 1038 | 	 * The most basic configuration defaults to automatically generating a login page at
 1039 | 	 * the URL "/login", redirecting to "/login?error" for authentication failure. The
 1040 | 	 * details of the login page can be found on
 1041 | 	 * {@link FormLoginConfigurer#loginPage(String)}
 1042 | 	 *
 1043 | 	 * <pre>
 1044 | 	 * &#064;Configuration
 1045 | 	 * &#064;EnableWebSecurity
 1046 | 	 * public class FormLoginSecurityConfig {
 1047 | 	 *
 1048 | 	 * 	&#064;Bean
 1049 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
 1050 | 	 * 		http
 1051 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
 1052 | 	 * 				authorizeHttpRequests
 1053 | 	 * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
 1054 | 	 * 			)
 1055 | 	 * 			.formLogin(withDefaults());
 1056 | 	 * 		return http.build();
 1057 | 	 * 	}
 1058 | 	 *
 1059 | 	 * 	&#064;Bean
 1060 | 	 * 	public UserDetailsService userDetailsService() {
 1061 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
 1062 | 	 * 			.username(&quot;user&quot;)
 1063 | 	 * 			.password(&quot;password&quot;)
 1064 | 	 * 			.roles(&quot;USER&quot;)
 1065 | 	 * 			.build();
 1066 | 	 * 		return new InMemoryUserDetailsManager(user);
 1067 | 	 * 	}
 1068 | 	 * }
 1069 | 	 * </pre>
 1070 | 	 *
 1071 | 	 * The configuration below demonstrates customizing the defaults.
 1072 | 	 *
 1073 | 	 * <pre>
 1074 | 	 * &#064;Configuration
 1075 | 	 * &#064;EnableWebSecurity
 1076 | 	 * public class FormLoginSecurityConfig {
 1077 | 	 *
 1078 | 	 * 	&#064;Bean
 1079 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
 1080 | 	 * 		http
 1081 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
 1082 | 	 * 				authorizeHttpRequests
 1083 | 	 * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
 1084 | 	 * 			)
 1085 | 	 * 			.formLogin((formLogin) -&gt;
 1086 | 	 * 				formLogin
 1087 | 	 * 					.usernameParameter(&quot;username&quot;)
 1088 | 	 * 					.passwordParameter(&quot;password&quot;)
 1089 | 	 * 					.loginPage(&quot;/authentication/login&quot;)
 1090 | 	 * 					.failureUrl(&quot;/authentication/login?failed&quot;)
 1091 | 	 * 					.loginProcessingUrl(&quot;/authentication/login/process&quot;)
 1092 | 	 * 			);
 1093 | 	 * 		return http.build();
 1094 | 	 * 	}
 1095 | 	 *
 1096 | 	 * 	&#064;Bean
 1097 | 	 * 	public UserDetailsService userDetailsService() {
 1098 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
 1099 | 	 * 			.username(&quot;user&quot;)
 1100 | 	 * 			.password(&quot;password&quot;)
 1101 | 	 * 			.roles(&quot;USER&quot;)
 1102 | 	 * 			.build();
 1103 | 	 * 		return new InMemoryUserDetailsManager(user);
 1104 | 	 * 	}
 1105 | 	 * }
 1106 | 	 * </pre>
 1107 | 	 * @param formLoginCustomizer the {@link Customizer} to provide more options for the
 1108 | 	 * {@link FormLoginConfigurer}
 1109 | 	 * @return the {@link HttpSecurity} for further customizations
 1110 | 	 * @ @see FormLoginConfigurer#loginPage(String)
 1111 | 	 */
 1112 | 	public HttpSecurity formLogin(Customizer<FormLoginConfigurer<HttpSecurity>> formLoginCustomizer) {
 1113 | 		formLoginCustomizer.customize(getOrApply(new FormLoginConfigurer<>()));
 1114 | 		return HttpSecurity.this;
 1115 | 	}
 1116 | 
 1117 | 	/**
 1118 | 	 * Configures authentication support using an SAML 2.0 Service Provider. <br>
 1119 | 	 * <br>
 1120 | 	 *
 1121 | 	 * The &quot;authentication flow&quot; is implemented using the <b>Web Browser SSO
 1122 | 	 * Profile, using POST and REDIRECT bindings</b>, as documented in the
 1123 | 	 * <a target="_blank" href="https://docs.oasis-open.org/security/saml/">SAML V2.0
 1124 | 	 * Core,Profiles and Bindings</a> specifications. <br>
 1125 | 	 * <br>
 1126 | 	 *
 1127 | 	 * As a prerequisite to using this feature, is that you have a SAML v2.0 Identity
 1128 | 	 * Provider to provide an assertion. The representation of the Service Provider, the
 1129 | 	 * relying party, and the remote Identity Provider, the asserting party is contained
 1130 | 	 * within {@link RelyingPartyRegistration}. <br>
 1131 | 	 * <br>
 1132 | 	 *
 1133 | 	 * {@link RelyingPartyRegistration}(s) are composed within a
 1134 | 	 * {@link RelyingPartyRegistrationRepository}, which is <b>required</b> and must be
 1135 | 	 * registered with the {@link ApplicationContext} or configured via
 1136 | 	 * <code>saml2Login().relyingPartyRegistrationRepository(..)</code>. <br>
 1137 | 	 * <br>
 1138 | 	 *
 1139 | 	 * The default configuration provides an auto-generated login page at
 1140 | 	 * <code>&quot;/login&quot;</code> and redirects to
 1141 | 	 * <code>&quot;/login?error&quot;</code> when an authentication error occurs. The
 1142 | 	 * login page will display each of the identity providers with a link that is capable
 1143 | 	 * of initiating the &quot;authentication flow&quot;. <br>
 1144 | 	 * <br>
 1145 | 	 *
 1146 | 	 * <p>
 1147 | 	 * <h2>Example Configuration</h2>
 1148 | 	 *
 1149 | 	 * The following example shows the minimal configuration required, using SimpleSamlPhp
 1150 | 	 * as the Authentication Provider.
 1151 | 	 *
 1152 | 	 * <pre>
 1153 | 	 * &#064;Configuration
 1154 | 	 * &#064;EnableWebSecurity
 1155 | 	 * public class Saml2LoginSecurityConfig {
 1156 | 	 *
 1157 | 	 * 	&#064;Bean
 1158 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
 1159 | 	 * 		http
 1160 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
 1161 | 	 * 				authorizeHttpRequests
 1162 | 	 * 					.anyRequest().authenticated()
 1163 | 	 * 			)
 1164 | 	 * 			.saml2Login(withDefaults());
 1165 | 	 * 		return http.build();
 1166 | 	 * 	}
 1167 | 	 *
 1168 | 	 *	&#064;Bean
 1169 | 	 *	public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
 1170 | 	 *		return new InMemoryRelyingPartyRegistrationRepository(this.getSaml2RelyingPartyRegistration());
 1171 | 	 *	}
 1172 | 	 *
 1173 | 	 * 	private RelyingPartyRegistration getSaml2RelyingPartyRegistration() {
 1174 | 	 * 		//remote IDP entity ID
 1175 | 	 * 		String idpEntityId = "https://simplesaml-for-spring-saml.apps.pcfone.io/saml2/idp/metadata.php";
 1176 | 	 * 		//remote WebSSO Endpoint - Where to Send AuthNRequests to
 1177 | 	 * 		String webSsoEndpoint = "https://simplesaml-for-spring-saml.apps.pcfone.io/saml2/idp/SSOService.php";
 1178 | 	 * 		//local registration ID
 1179 | 	 * 		String registrationId = "simplesamlphp";
 1180 | 	 * 		//local entity ID - autogenerated based on URL
 1181 | 	 * 		String localEntityIdTemplate = "{baseUrl}/saml2/service-provider-metadata/{registrationId}";
 1182 | 	 * 		//local signing (and decryption key)
 1183 | 	 * 		Saml2X509Credential signingCredential = getSigningCredential();
 1184 | 	 * 		//IDP certificate for verification of incoming messages
 1185 | 	 * 		Saml2X509Credential idpVerificationCertificate = getVerificationCertificate();
 1186 | 	 * 		return RelyingPartyRegistration.withRegistrationId(registrationId)
 1187 | 	 * 				.remoteIdpEntityId(idpEntityId)
 1188 | 	 * 				.idpWebSsoUrl(webSsoEndpoint)
 1189 | 	 * 				.credential(signingCredential)
 1190 | 	 * 				.credential(idpVerificationCertificate)
 1191 | 	 * 				.localEntityIdTemplate(localEntityIdTemplate)
 1192 | 	 * 				.build();
 1193 | 	 * 	}
 1194 | 	 * }
 1195 | 	 * </pre>
 1196 | 	 *
 1197 | 	 * <p>
 1198 | 	 * @param saml2LoginCustomizer the {@link Customizer} to provide more options for the
 1199 | 	 * {@link Saml2LoginConfigurer}
 1200 | 	 * @return the {@link HttpSecurity} for further customizations
 1201 | 	 * @ @since 5.2
 1202 | 	 */
 1203 | 	public HttpSecurity saml2Login(Customizer<Saml2LoginConfigurer<HttpSecurity>> saml2LoginCustomizer) {
 1204 | 		saml2LoginCustomizer.customize(getOrApply(new Saml2LoginConfigurer<>()));
 1205 | 		return HttpSecurity.this;
 1206 | 	}
 1207 | 
 1208 | 	/**
 1209 | 	 * Configures logout support for an SAML 2.0 Relying Party. <br>
 1210 | 	 * <br>
 1211 | 	 *
 1212 | 	 * Implements the <b>Single Logout Profile, using POST and REDIRECT bindings</b>, as
 1213 | 	 * documented in the
 1214 | 	 * <a target="_blank" href="https://docs.oasis-open.org/security/saml/">SAML V2.0
 1215 | 	 * Core, Profiles and Bindings</a> specifications. <br>
 1216 | 	 * <br>
 1217 | 	 *
 1218 | 	 * As a prerequisite to using this feature, is that you have a SAML v2.0 Asserting
 1219 | 	 * Party to sent a logout request to. The representation of the relying party and the
 1220 | 	 * asserting party is contained within {@link RelyingPartyRegistration}. <br>
 1221 | 	 * <br>
 1222 | 	 *
 1223 | 	 * {@link RelyingPartyRegistration}(s) are composed within a
 1224 | 	 * {@link RelyingPartyRegistrationRepository}, which is <b>required</b> and must be
 1225 | 	 * registered with the {@link ApplicationContext} or configured via
 1226 | 	 * {@link #saml2Login(Customizer)}.<br>
 1227 | 	 * <br>
 1228 | 	 *
 1229 | 	 * The default configuration provides an auto-generated logout endpoint at
 1230 | 	 * <code>&quot;/logout&quot;</code> and redirects to <code>/login?logout</code> when
 1231 | 	 * logout completes. <br>
 1232 | 	 * <br>
 1233 | 	 *
 1234 | 	 * <p>
 1235 | 	 * <h2>Example Configuration</h2>
 1236 | 	 *
 1237 | 	 * The following example shows the minimal configuration required, using a
 1238 | 	 * hypothetical asserting party.
 1239 | 	 *
 1240 | 	 * <pre>
 1241 | 	 *	&#064;EnableWebSecurity
 1242 | 	 *	&#064;Configuration
 1243 | 	 *	public class Saml2LogoutSecurityConfig {
 1244 | 	 *		&#064;Bean
 1245 | 	 *		public SecurityFilterChain web(HttpSecurity http)  {
 1246 | 	 *			http
 1247 | 	 *				.authorizeHttpRequests((authorize) -&gt; authorize
 1248 | 	 *					.anyRequest().authenticated()
 1249 | 	 *				)
 1250 | 	 *				.saml2Login(withDefaults())
 1251 | 	 *				.saml2Logout(withDefaults());
 1252 | 	 *			return http.build();
 1253 | 	 *		}
 1254 | 	 *
 1255 | 	 *		&#064;Bean
 1256 | 	 *		public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
 1257 | 	 *			RelyingPartyRegistration registration = RelyingPartyRegistrations
 1258 | 	 *					.withMetadataLocation("https://ap.example.org/metadata")
 1259 | 	 *					.registrationId("simple")
 1260 | 	 *					.build();
 1261 | 	 *			return new InMemoryRelyingPartyRegistrationRepository(registration);
 1262 | 	 *		}
 1263 | 	 *	}
 1264 | 	 * </pre>
 1265 | 	 *
 1266 | 	 * <p>
 1267 | 	 * @return the {@link HttpSecurity} for further customizations
 1268 | 	 * @ @since 5.6
 1269 | 	 */
 1270 | 	public HttpSecurity saml2Logout(Customizer<Saml2LogoutConfigurer<HttpSecurity>> saml2LogoutCustomizer) {
 1271 | 		saml2LogoutCustomizer.customize(getOrApply(new Saml2LogoutConfigurer<>(getContext())));
 1272 | 		return HttpSecurity.this;
 1273 | 	}
 1274 | 
 1275 | 	/**
 1276 | 	 * Configures a SAML 2.0 metadata endpoint that presents relying party configurations
 1277 | 	 * in an {@code <md:EntityDescriptor>} payload.
 1278 | 	 *
 1279 | 	 * <p>
 1280 | 	 * By default, the endpoints are {@code /saml2/metadata} and
 1281 | 	 * {@code /saml2/metadata/{registrationId}} though note that also
 1282 | 	 * {@code /saml2/service-provider-metadata/{registrationId}} is recognized for
 1283 | 	 * backward compatibility purposes.
 1284 | 	 *
 1285 | 	 * <p>
 1286 | 	 * <h2>Example Configuration</h2>
 1287 | 	 *
 1288 | 	 * The following example shows the minimal configuration required, using a
 1289 | 	 * hypothetical asserting party.
 1290 | 	 *
 1291 | 	 * <pre>
 1292 | 	 *	&#064;EnableWebSecurity
 1293 | 	 *	&#064;Configuration
 1294 | 	 *	public class Saml2LogoutSecurityConfig {
 1295 | 	 *		&#064;Bean
 1296 | 	 *		public SecurityFilterChain web(HttpSecurity http)  {
 1297 | 	 *			http
 1298 | 	 *				.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
 1299 | 	 *				.saml2Metadata(Customizer.withDefaults());
 1300 | 	 *			return http.build();
 1301 | 	 *		}
 1302 | 	 *
 1303 | 	 *		&#064;Bean
 1304 | 	 *		public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
 1305 | 	 *			RelyingPartyRegistration registration = RelyingPartyRegistrations
 1306 | 	 *					.withMetadataLocation("https://ap.example.org/metadata")
 1307 | 	 *					.registrationId("simple")
 1308 | 	 *					.build();
 1309 | 	 *			return new InMemoryRelyingPartyRegistrationRepository(registration);
 1310 | 	 *		}
 1311 | 	 *	}
 1312 | 	 * </pre>
 1313 | 	 * @param saml2MetadataConfigurer the {@link Customizer} to provide more options for
 1314 | 	 * the {@link Saml2MetadataConfigurer}
 1315 | 	 * @return the {@link HttpSecurity} for further customizations
 1316 | 	 * @ @since 6.1
 1317 | 	 */
 1318 | 	public HttpSecurity saml2Metadata(Customizer<Saml2MetadataConfigurer<HttpSecurity>> saml2MetadataConfigurer) {
 1319 | 		saml2MetadataConfigurer.customize(getOrApply(new Saml2MetadataConfigurer<>(getContext())));
 1320 | 		return HttpSecurity.this;
 1321 | 	}
 1322 | 
 1323 | 	/**
 1324 | 	 * Configures authentication support using an OAuth 2.0 and/or OpenID Connect 1.0
 1325 | 	 * Provider. <br>
 1326 | 	 * <br>
 1327 | 	 *
 1328 | 	 * The &quot;authentication flow&quot; is implemented using the <b>Authorization Code
 1329 | 	 * Grant</b>, as specified in the
 1330 | 	 * <a target="_blank" href="https://tools.ietf.org/html/rfc6749#section-4.1">OAuth 2.0
 1331 | 	 * Authorization Framework</a> and <a target="_blank" href=
 1332 | 	 * "https://openid.net/specs/openid-connect-core-1_0.html#CodeFlowAuth">OpenID Connect
 1333 | 	 * Core 1.0</a> specification. <br>
 1334 | 	 * <br>
 1335 | 	 *
 1336 | 	 * As a prerequisite to using this feature, you must register a client with a
 1337 | 	 * provider. The client registration information may than be used for configuring a
 1338 | 	 * {@link org.springframework.security.oauth2.client.registration.ClientRegistration}
 1339 | 	 * using a
 1340 | 	 * {@link org.springframework.security.oauth2.client.registration.ClientRegistration.Builder}.
 1341 | 	 * <br>
 1342 | 	 * <br>
 1343 | 	 *
 1344 | 	 * {@link org.springframework.security.oauth2.client.registration.ClientRegistration}(s)
 1345 | 	 * are composed within a
 1346 | 	 * {@link org.springframework.security.oauth2.client.registration.ClientRegistrationRepository},
 1347 | 	 * which is <b>required</b> and must be registered with the {@link ApplicationContext}
 1348 | 	 * or configured via <code>oauth2Login().clientRegistrationRepository(..)</code>. <br>
 1349 | 	 * <br>
 1350 | 	 *
 1351 | 	 * The default configuration provides an auto-generated login page at
 1352 | 	 * <code>&quot;/login&quot;</code> and redirects to
 1353 | 	 * <code>&quot;/login?error&quot;</code> when an authentication error occurs. The
 1354 | 	 * login page will display each of the clients with a link that is capable of
 1355 | 	 * initiating the &quot;authentication flow&quot;. <br>
 1356 | 	 * <br>
 1357 | 	 *
 1358 | 	 * <p>
 1359 | 	 * <h2>Example Configuration</h2>
 1360 | 	 *
 1361 | 	 * The following example shows the minimal configuration required, using Google as the
 1362 | 	 * Authentication Provider.
 1363 | 	 *
 1364 | 	 * <pre>
 1365 | 	 * &#064;Configuration
 1366 | 	 * &#064;EnableWebSecurity
 1367 | 	 * public class OAuth2LoginSecurityConfig {
 1368 | 	 *
 1369 | 	 * 	&#064;Bean
 1370 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
 1371 | 	 * 		http
 1372 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
 1373 | 	 * 				authorizeHttpRequests
 1374 | 	 * 					.anyRequest().authenticated()
 1375 | 	 * 			)
 1376 | 	 * 			.oauth2Login(withDefaults());
 1377 | 	 * 		return http.build();
 1378 | 	 * 	}
 1379 | 	 *
 1380 | 	 *	&#064;Bean
 1381 | 	 *	public ClientRegistrationRepository clientRegistrationRepository() {
 1382 | 	 *		return new InMemoryClientRegistrationRepository(this.googleClientRegistration());
 1383 | 	 *	}
 1384 | 	 *
 1385 | 	 * 	private ClientRegistration googleClientRegistration() {
 1386 | 	 * 		return ClientRegistration.withRegistrationId("google")
 1387 | 	 * 			.clientId("google-client-id")
 1388 | 	 * 			.clientSecret("google-client-secret")
 1389 | 	 * 			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
 1390 | 	 * 			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
 1391 | 	 * 			.redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
 1392 | 	 * 			.scope("openid", "profile", "email", "address", "phone")
 1393 | 	 * 			.authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
 1394 | 	 * 			.tokenUri("https://www.googleapis.com/oauth2/v4/token")
 1395 | 	 * 			.userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
 1396 | 	 * 			.userNameAttributeName(IdTokenClaimNames.SUB)
 1397 | 	 * 			.jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
 1398 | 	 * 			.clientName("Google")
 1399 | 	 * 			.build();
 1400 | 	 *	}
 1401 | 	 * }
 1402 | 	 * </pre>
 1403 | 	 *
 1404 | 	 * <p>
 1405 | 	 * For more advanced configuration, see {@link OAuth2LoginConfigurer} for available
 1406 | 	 * options to customize the defaults.
 1407 | 	 * @param oauth2LoginCustomizer the {@link Customizer} to provide more options for the
 1408 | 	 * {@link OAuth2LoginConfigurer}
 1409 | 	 * @return the {@link HttpSecurity} for further customizations
 1410 | 	 * @ @see
 1411 | 	 * <a target="_blank" href= "https://tools.ietf.org/html/rfc6749#section-4.1">Section
 1412 | 	 * 4.1 Authorization Code Grant</a>
 1413 | 	 * @see <a target="_blank" href=
 1414 | 	 * "https://openid.net/specs/openid-connect-core-1_0.html#CodeFlowAuth">Section 3.1
 1415 | 	 * Authorization Code Flow</a>
 1416 | 	 * @see org.springframework.security.oauth2.client.registration.ClientRegistration
 1417 | 	 * @see org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
 1418 | 	 */
 1419 | 	public HttpSecurity oauth2Login(Customizer<OAuth2LoginConfigurer<HttpSecurity>> oauth2LoginCustomizer) {
 1420 | 		oauth2LoginCustomizer.customize(getOrApply(new OAuth2LoginConfigurer<>()));
 1421 | 		return HttpSecurity.this;
 1422 | 	}
 1423 | 
 1424 | 	public OidcLogoutConfigurer<HttpSecurity> oidcLogout() {
 1425 | 		return getOrApply(new OidcLogoutConfigurer<>());
 1426 | 	}
 1427 | 
 1428 | 	public HttpSecurity oidcLogout(Customizer<OidcLogoutConfigurer<HttpSecurity>> oidcLogoutCustomizer) {
 1429 | 		oidcLogoutCustomizer.customize(getOrApply(new OidcLogoutConfigurer<>()));
 1430 | 		return HttpSecurity.this;
 1431 | 	}
 1432 | 
 1433 | 	/**
 1434 | 	 * Configures OAuth 2.0 Client support.
 1435 | 	 *
 1436 | 	 * <h2>Example Configuration</h2>
 1437 | 	 *
 1438 | 	 * The following example demonstrates how to enable OAuth 2.0 Client support for all
 1439 | 	 * endpoints.
 1440 | 	 *
 1441 | 	 * <pre>
 1442 | 	 * &#064;Configuration
 1443 | 	 * &#064;EnableWebSecurity
 1444 | 	 * public class OAuth2ClientSecurityConfig {
 1445 | 	 *
 1446 | 	 * 	&#064;Bean
 1447 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
 1448 | 	 * 		http
 1449 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
 1450 | 	 * 				authorizeHttpRequests
 1451 | 	 * 					.anyRequest().authenticated()
 1452 | 	 * 			)
 1453 | 	 * 			.oauth2Client(withDefaults());
 1454 | 	 * 		return http.build();
 1455 | 	 * 	}
 1456 | 	 * }
 1457 | 	 * </pre>
 1458 | 	 * @param oauth2ClientCustomizer the {@link Customizer} to provide more options for
 1459 | 	 * the {@link OAuth2ClientConfigurer}
 1460 | 	 * @return the {@link HttpSecurity} for further customizations
 1461 | 	 * @ @see
 1462 | 	 * <a target="_blank" href= "https://tools.ietf.org/html/rfc6749#section-1.1">OAuth
 1463 | 	 * 2.0 Authorization Framework</a>
 1464 | 	 */
 1465 | 	public HttpSecurity oauth2Client(Customizer<OAuth2ClientConfigurer<HttpSecurity>> oauth2ClientCustomizer) {
 1466 | 		oauth2ClientCustomizer.customize(getOrApply(new OAuth2ClientConfigurer<>()));
 1467 | 		return HttpSecurity.this;
 1468 | 	}
 1469 | 
 1470 | 	/**
 1471 | 	 * Configures OAuth 2.0 Resource Server support.
 1472 | 	 *
 1473 | 	 * <h2>Example Configuration</h2>
 1474 | 	 *
 1475 | 	 * The following example demonstrates how to configure a custom JWT authentication
 1476 | 	 * converter.
 1477 | 	 *
 1478 | 	 * <pre>
 1479 | 	 * &#064;Configuration
 1480 | 	 * &#064;EnableWebSecurity
 1481 | 	 * public class OAuth2ResourceServerSecurityConfig {
 1482 | 	 *
 1483 | 	 * 	&#064;Bean
 1484 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
 1485 | 	 * 		http
 1486 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
 1487 | 	 * 				authorizeHttpRequests
 1488 | 	 * 					.anyRequest().authenticated()
 1489 | 	 * 			)
 1490 | 	 * 			.oauth2ResourceServer((oauth2ResourceServer) -&gt;
 1491 | 	 * 				oauth2ResourceServer
 1492 | 	 * 					.jwt((jwt) -&gt;
 1493 | 	 * 						jwt
 1494 | 	 * 							.decoder(jwtDecoder())
 1495 | 	 * 					)
 1496 | 	 * 			);
 1497 | 	 * 		return http.build();
 1498 | 	 * 	}
 1499 | 	 *
 1500 | 	 * 	&#064;Bean
 1501 | 	 * 	public JwtDecoder jwtDecoder() {
 1502 | 	 * 		return NimbusJwtDecoder.withPublicKey(this.key).build();
 1503 | 	 * 	}
 1504 | 	 * }
 1505 | 	 * </pre>
 1506 | 	 * @param oauth2ResourceServerCustomizer the {@link Customizer} to provide more
 1507 | 	 * options for the {@link OAuth2ResourceServerConfigurer}
 1508 | 	 * @return the {@link HttpSecurity} for further customizations
 1509 | 	 * @ @see
 1510 | 	 * <a target="_blank" href= "https://tools.ietf.org/html/rfc6749#section-1.1">OAuth
 1511 | 	 * 2.0 Authorization Framework</a>
 1512 | 	 */
 1513 | 	public HttpSecurity oauth2ResourceServer(
 1514 | 			Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> oauth2ResourceServerCustomizer) {
 1515 | 		OAuth2ResourceServerConfigurer<HttpSecurity> configurer = getOrApply(
 1516 | 				new OAuth2ResourceServerConfigurer<>(getContext()));
 1517 | 		this.postProcess(configurer);
 1518 | 		oauth2ResourceServerCustomizer.customize(configurer);
 1519 | 		return HttpSecurity.this;
 1520 | 	}
 1521 | 
 1522 | 	/**
 1523 | 	 * Configures OAuth 2.1 Authorization Server support.
 1524 | 	 * @param oauth2AuthorizationServerCustomizer the {@link Customizer} providing access
 1525 | 	 * to the {@link OAuth2AuthorizationServerConfigurer} for further customizations
 1526 | 	 * @return the {@link HttpSecurity} for further customizations
 1527 | 	 * @ @since 7.0
 1528 | 	 * @see <a target="_blank" href=
 1529 | 	 * "https://www.ietf.org/archive/id/draft-ietf-oauth-v2-1-13.html">OAuth 2.1
 1530 | 	 * Authorization Framework</a>
 1531 | 	 */
 1532 | 	public HttpSecurity oauth2AuthorizationServer(
 1533 | 			Customizer<OAuth2AuthorizationServerConfigurer> oauth2AuthorizationServerCustomizer) {
 1534 | 		oauth2AuthorizationServerCustomizer.customize(getOrApply(new OAuth2AuthorizationServerConfigurer()));
 1535 | 		return HttpSecurity.this;
 1536 | 	}
 1537 | 
 1538 | 	/**
 1539 | 	 * Configures One-Time Token Login Support.
 1540 | 	 *
 1541 | 	 * <h2>Example Configuration</h2>
 1542 | 	 *
 1543 | 	 * <pre>
 1544 | 	 * &#064;Configuration
 1545 | 	 * &#064;EnableWebSecurity
 1546 | 	 * public class SecurityConfig {
 1547 | 	 *
 1548 | 	 * 	&#064;Bean
 1549 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
 1550 | 	 * 		http
 1551 | 	 * 			.authorizeHttpRequests((authorize) -&gt; authorize
 1552 | 	 * 					.anyRequest().authenticated()
 1553 | 	 * 			)
 1554 | 	 * 			.oneTimeTokenLogin(Customizer.withDefaults());
 1555 | 	 * 		return http.build();
 1556 | 	 * 	}
 1557 | 	 *
 1558 | 	 * 	&#064;Bean
 1559 | 	 * 	public OneTimeTokenGenerationSuccessHandler oneTimeTokenGenerationSuccessHandler() {
 1560 | 	 * 		return new MyMagicLinkOneTimeTokenGenerationSuccessHandler();
 1561 | 	 * 	}
 1562 | 	 *
 1563 | 	 * }
 1564 | 	 * </pre>
 1565 | 	 * @param oneTimeTokenLoginConfigurerCustomizer the {@link Customizer} to provide more
 1566 | 	 * options for the {@link OneTimeTokenLoginConfigurer}
 1567 | 	 * @return the {@link HttpSecurity} for further customizations @
 1568 | 	 */
 1569 | 	public HttpSecurity oneTimeTokenLogin(
 1570 | 			Customizer<OneTimeTokenLoginConfigurer<HttpSecurity>> oneTimeTokenLoginConfigurerCustomizer) {
 1571 | 		oneTimeTokenLoginConfigurerCustomizer.customize(getOrApply(new OneTimeTokenLoginConfigurer<>(getContext())));
 1572 | 		return HttpSecurity.this;
 1573 | 	}
 1574 | 
 1575 | 	/**
 1576 | 	 * Configures channel security. In order for this configuration to be useful at least
 1577 | 	 * one mapping to a required channel must be provided.
 1578 | 	 *
 1579 | 	 * <h2>Example Configuration</h2>
 1580 | 	 *
 1581 | 	 * The example below demonstrates how to require HTTPs for every request. Only
 1582 | 	 * requiring HTTPS for some requests is supported, but not recommended since an
 1583 | 	 * application that allows for HTTP introduces many security vulnerabilities. For one
 1584 | 	 * such example, read about
 1585 | 	 * <a href="https://en.wikipedia.org/wiki/Firesheep">Firesheep</a>.
 1586 | 	 *
 1587 | 	 * <pre>
 1588 | 	 * &#064;Configuration
 1589 | 	 * &#064;EnableWebSecurity
 1590 | 	 * public class ChannelSecurityConfig {
 1591 | 	 *
 1592 | 	 * 	&#064;Bean
 1593 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
 1594 | 	 * 		http
 1595 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
 1596 | 	 * 				authorizeHttpRequests
 1597 | 	 * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
 1598 | 	 * 			)
 1599 | 	 * 			.formLogin(withDefaults())
 1600 | 	 * 			.requiresChannel((requiresChannel) -&gt;
 1601 | 	 * 				requiresChannel
 1602 | 	 * 					.anyRequest().requiresSecure()
 1603 | 	 * 			);
 1604 | 	 * 		return http.build();
 1605 | 	 * 	}
 1606 | 	 *
 1607 | 	 * 	&#064;Bean
 1608 | 	 * 	public UserDetailsService userDetailsService() {
 1609 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
 1610 | 	 * 			.username(&quot;user&quot;)
 1611 | 	 * 			.password(&quot;password&quot;)
 1612 | 	 * 			.roles(&quot;USER&quot;)
 1613 | 	 * 			.build();
 1614 | 	 * 		return new InMemoryUserDetailsManager(user);
 1615 | 	 * 	}
 1616 | 	 * }
 1617 | 	 * </pre>
 1618 | 	 * @param requiresChannelCustomizer the {@link Customizer} to provide more options for
 1619 | 	 * the {@link ChannelSecurityConfigurer.ChannelRequestMatcherRegistry}
 1620 | 	 * @return the {@link HttpSecurity} for further customizations
 1621 | 	 * @deprecated Use {@link #redirectToHttps}
 1622 | 	 */
 1623 | 	@Deprecated
 1624 | 	public HttpSecurity requiresChannel(
 1625 | 			Customizer<ChannelSecurityConfigurer<HttpSecurity>.ChannelRequestMatcherRegistry> requiresChannelCustomizer) {
 1626 | 		ApplicationContext context = getContext();
 1627 | 		requiresChannelCustomizer.customize(getOrApply(new ChannelSecurityConfigurer<>(context)).getRegistry());
 1628 | 		return HttpSecurity.this;
 1629 | 	}
 1630 | 
 1631 | 	/**
 1632 | 	 * Configures channel security. In order for this configuration to be useful at least
 1633 | 	 * one mapping to a required channel must be provided.
 1634 | 	 *
 1635 | 	 * <h2>Example Configuration</h2>
 1636 | 	 *
 1637 | 	 * The example below demonstrates how to require HTTPS for every request. Only
 1638 | 	 * requiring HTTPS for some requests is supported, for example if you need to
 1639 | 	 * differentiate between local and production deployments.
 1640 | 	 *
 1641 | 	 * <pre>
 1642 | 	 * &#064;Configuration
 1643 | 	 * &#064;EnableWebSecurity
 1644 | 	 * public class RequireHttpsConfig {
 1645 | 	 *
 1646 | 	 * 	&#064;Bean
 1647 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
 1648 | 	 * 		http
 1649 | 	 * 			.authorizeHttpRequests((authorize) -&gt; authorize
 1650 | 	 * 				anyRequest().authenticated()
 1651 | 	 * 			)
 1652 | 	 * 			.formLogin(withDefaults())
 1653 | 	 * 			.redirectToHttps(withDefaults());
 1654 | 	 * 		return http.build();
 1655 | 	 * 	}
 1656 | 	 *
 1657 | 	 * 	&#064;Bean
 1658 | 	 * 	public UserDetailsService userDetailsService() {
 1659 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
 1660 | 	 * 			.username(&quot;user&quot;)
 1661 | 	 * 			.password(&quot;password&quot;)
 1662 | 	 * 			.roles(&quot;USER&quot;)
 1663 | 	 * 			.build();
 1664 | 	 * 		return new InMemoryUserDetailsManager(user);
 1665 | 	 * 	}
 1666 | 	 * }
 1667 | 	 * </pre>
 1668 | 	 * @param httpsRedirectConfigurerCustomizer the {@link Customizer} to provide more
 1669 | 	 * options for the {@link HttpsRedirectConfigurer}
 1670 | 	 * @return the {@link HttpSecurity} for further customizations
 1671 | 	 */
 1672 | 	public HttpSecurity redirectToHttps(
 1673 | 			Customizer<HttpsRedirectConfigurer<HttpSecurity>> httpsRedirectConfigurerCustomizer) {
 1674 | 		httpsRedirectConfigurerCustomizer.customize(getOrApply(new HttpsRedirectConfigurer<>()));
 1675 | 		return HttpSecurity.this;
 1676 | 	}
 1677 | 
 1678 | 	/**
 1679 | 	 * Configures HTTP Basic authentication.
 1680 | 	 *
 1681 | 	 * <h2>Example Configuration</h2>
 1682 | 	 *
 1683 | 	 * The example below demonstrates how to configure HTTP Basic authentication for an
 1684 | 	 * application. The default realm is "Realm", but can be customized using
 1685 | 	 * {@link HttpBasicConfigurer#realmName(String)}.
 1686 | 	 *
 1687 | 	 * <pre>
 1688 | 	 * &#064;Configuration
 1689 | 	 * &#064;EnableWebSecurity
 1690 | 	 * public class HttpBasicSecurityConfig {
 1691 | 	 *
 1692 | 	 * 	&#064;Bean
 1693 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
 1694 | 	 * 		http
 1695 | 	 * 			.authorizeHttpRequests((authorizeHttpRequests) -&gt;
 1696 | 	 * 				authorizeHttpRequests
 1697 | 	 * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
 1698 | 	 * 			)
 1699 | 	 * 			.httpBasic(withDefaults());
 1700 | 	 * 		return http.build();
 1701 | 	 * 	}
 1702 | 	 *
 1703 | 	 * 	&#064;Bean
 1704 | 	 * 	public UserDetailsService userDetailsService() {
 1705 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
 1706 | 	 * 			.username(&quot;user&quot;)
 1707 | 	 * 			.password(&quot;password&quot;)
 1708 | 	 * 			.roles(&quot;USER&quot;)
 1709 | 	 * 			.build();
 1710 | 	 * 		return new InMemoryUserDetailsManager(user);
 1711 | 	 * 	}
 1712 | 	 * }
 1713 | 	 * </pre>
 1714 | 	 * @param httpBasicCustomizer the {@link Customizer} to provide more options for the
 1715 | 	 * {@link HttpBasicConfigurer}
 1716 | 	 * @return the {@link HttpSecurity} for further customizations @
 1717 | 	 */
 1718 | 	public HttpSecurity httpBasic(Customizer<HttpBasicConfigurer<HttpSecurity>> httpBasicCustomizer) {
 1719 | 		httpBasicCustomizer.customize(getOrApply(new HttpBasicConfigurer<>()));
 1720 | 		return HttpSecurity.this;
 1721 | 	}
 1722 | 
 1723 | 	/**
 1724 | 	 * Adds support for the password management.
 1725 | 	 *
 1726 | 	 * <h2>Example Configuration</h2> The example below demonstrates how to configure
 1727 | 	 * password management for an application. The default change password page is
 1728 | 	 * "/change-password", but can be customized using
 1729 | 	 * {@link PasswordManagementConfigurer#changePasswordPage(String)}.
 1730 | 	 *
 1731 | 	 * <pre>
 1732 | 	 * &#064;Configuration
 1733 | 	 * &#064;EnableWebSecurity
 1734 | 	 * public class PasswordManagementSecurityConfig {
 1735 | 	 *
 1736 | 	 * 	&#064;Bean
 1737 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
 1738 | 	 * 		http
 1739 | 	 * 			.authorizeHttpRequests(authorizeHttpRequests -&gt;
 1740 | 	 * 				authorizeHttpRequests
 1741 | 	 * 					.requestMatchers(&quot;/**&quot;).hasRole(&quot;USER&quot;)
 1742 | 	 * 			)
 1743 | 	 * 			.passwordManagement(passwordManagement -&gt;
 1744 | 	 * 				passwordManagement
 1745 | 	 * 					.changePasswordPage(&quot;/custom-change-password-page&quot;)
 1746 | 	 * 			);
 1747 | 	 * 		return http.build();
 1748 | 	 * 	}
 1749 | 	 * }
 1750 | 	 * </pre>
 1751 | 	 * @param passwordManagementCustomizer the {@link Customizer} to provide more options
 1752 | 	 * for the {@link PasswordManagementConfigurer}
 1753 | 	 * @return the {@link HttpSecurity} for further customizations
 1754 | 	 * @ @since 5.6
 1755 | 	 */
 1756 | 	public HttpSecurity passwordManagement(
 1757 | 			Customizer<PasswordManagementConfigurer<HttpSecurity>> passwordManagementCustomizer) {
 1758 | 		passwordManagementCustomizer.customize(getOrApply(new PasswordManagementConfigurer<>()));
 1759 | 		return HttpSecurity.this;
 1760 | 	}
 1761 | 
 1762 | 	/**
 1763 | 	 * Configure the default {@link AuthenticationManager}.
 1764 | 	 * @param authenticationManager the {@link AuthenticationManager} to use
 1765 | 	 * @return the {@link HttpSecurity} for further customizations
 1766 | 	 * @since 5.6
 1767 | 	 */
 1768 | 	public HttpSecurity authenticationManager(AuthenticationManager authenticationManager) {
 1769 | 		Assert.notNull(authenticationManager, "authenticationManager cannot be null");
 1770 | 		this.authenticationManager = authenticationManager;
 1771 | 		return HttpSecurity.this;
 1772 | 	}
 1773 | 
 1774 | 	@Override
 1775 | 	public <C> void setSharedObject(Class<C> sharedType, C object) {
 1776 | 		super.setSharedObject(sharedType, object);
 1777 | 	}
 1778 | 
 1779 | 	@Override
 1780 | 	protected void beforeConfigure() {
 1781 | 		if (this.authenticationManager != null) {
 1782 | 			setSharedObject(AuthenticationManager.class, this.authenticationManager);
 1783 | 		}
 1784 | 		else {
 1785 | 			ObjectPostProcessor<AuthenticationManager> postProcessor = getAuthenticationManagerPostProcessor();
 1786 | 			AuthenticationManager manager = getAuthenticationRegistry().build();
 1787 | 			if (manager != null) {
 1788 | 				setSharedObject(AuthenticationManager.class, postProcessor.postProcess(manager));
 1789 | 			}
 1790 | 		}
 1791 | 	}
 1792 | 
 1793 | 	@SuppressWarnings("unchecked")
 1794 | 	@Override
 1795 | 	protected DefaultSecurityFilterChain performBuild() {
 1796 | 		this.filters.sort(OrderComparator.INSTANCE);
 1797 | 		List<Filter> sortedFilters = new ArrayList<>(this.filters.size());
 1798 | 		for (Filter filter : this.filters) {
 1799 | 			sortedFilters.add(((OrderedFilter) filter).filter);
 1800 | 		}
 1801 | 		return new DefaultSecurityFilterChain(this.requestMatcher, sortedFilters);
 1802 | 	}
 1803 | 
 1804 | 	@Override
 1805 | 	public HttpSecurity authenticationProvider(AuthenticationProvider authenticationProvider) {
 1806 | 		getAuthenticationRegistry().authenticationProvider(authenticationProvider);
 1807 | 		return this;
 1808 | 	}
 1809 | 
 1810 | 	@Override
 1811 | 	public HttpSecurity userDetailsService(UserDetailsService userDetailsService) {
 1812 | 		getAuthenticationRegistry().userDetailsService(userDetailsService);
 1813 | 		return this;
 1814 | 	}
 1815 | 
 1816 | 	private AuthenticationManagerBuilder getAuthenticationRegistry() {
 1817 | 		return getSharedObject(AuthenticationManagerBuilder.class);
 1818 | 	}
 1819 | 
 1820 | 	@Override
 1821 | 	public HttpSecurity addFilterAfter(Filter filter, Class<? extends Filter> afterFilter) {
 1822 | 		return addFilterAtOffsetOf(filter, 1, afterFilter);
 1823 | 	}
 1824 | 
 1825 | 	@Override
 1826 | 	public HttpSecurity addFilterBefore(Filter filter, Class<? extends Filter> beforeFilter) {
 1827 | 		return addFilterAtOffsetOf(filter, -1, beforeFilter);
 1828 | 	}
 1829 | 
 1830 | 	private HttpSecurity addFilterAtOffsetOf(Filter filter, int offset, Class<? extends Filter> registeredFilter) {
 1831 | 		Integer registeredFilterOrder = this.filterOrders.getOrder(registeredFilter);
 1832 | 		if (registeredFilterOrder == null) {
 1833 | 			throw new IllegalArgumentException(
 1834 | 					"The Filter class " + registeredFilter.getName() + " does not have a registered order");
 1835 | 		}
 1836 | 		int order = registeredFilterOrder + offset;
 1837 | 		this.filters.add(new OrderedFilter(filter, order));
 1838 | 		this.filterOrders.put(filter.getClass(), order);
 1839 | 		return this;
 1840 | 	}
 1841 | 
 1842 | 	@Override
 1843 | 	public HttpSecurity addFilter(Filter filter) {
 1844 | 		Integer order = this.filterOrders.getOrder(filter.getClass());
 1845 | 		if (order == null) {
 1846 | 			throw new IllegalArgumentException("The Filter class " + filter.getClass().getName()
 1847 | 					+ " does not have a registered order and cannot be added without a specified order. Consider using addFilterBefore or addFilterAfter instead.");
 1848 | 		}
 1849 | 		this.filters.add(new OrderedFilter(filter, order));
 1850 | 		return this;
 1851 | 	}
 1852 | 
 1853 | 	/**
 1854 | 	 * Adds the Filter at the location of the specified Filter class. For example, if you
 1855 | 	 * want the filter CustomFilter to be registered in the same position as
 1856 | 	 * {@link UsernamePasswordAuthenticationFilter}, you can invoke:
 1857 | 	 *
 1858 | 	 * <pre>
 1859 | 	 * addFilterAt(new CustomFilter(), UsernamePasswordAuthenticationFilter.class)
 1860 | 	 * </pre>
 1861 | 	 *
 1862 | 	 * Registration of multiple Filters in the same location means their ordering is not
 1863 | 	 * deterministic. More concretely, registering multiple Filters in the same location
 1864 | 	 * does not override existing Filters. Instead, do not register Filters you do not
 1865 | 	 * want to use.
 1866 | 	 * @param filter the Filter to register
 1867 | 	 * @param atFilter the location of another {@link Filter} that is already registered
 1868 | 	 * (i.e. known) with Spring Security.
 1869 | 	 * @return the {@link HttpSecurity} for further customizations
 1870 | 	 */
 1871 | 	public HttpSecurity addFilterAt(Filter filter, Class<? extends Filter> atFilter) {
 1872 | 		return addFilterAtOffsetOf(filter, 0, atFilter);
 1873 | 	}
 1874 | 
 1875 | 	/**
 1876 | 	 * Allows specifying which {@link HttpServletRequest} instances this
 1877 | 	 * {@link HttpSecurity} will be invoked on. This method allows for easily invoking the
 1878 | 	 * {@link HttpSecurity} for multiple different {@link RequestMatcher} instances. If
 1879 | 	 * only a single {@link RequestMatcher} is necessary consider using
 1880 | 	 * {@link #securityMatcher(String...)}, or {@link #securityMatcher(RequestMatcher)}.
 1881 | 	 *
 1882 | 	 * <p>
 1883 | 	 * Invoking {@link #securityMatchers(Customizer)} will not override previous
 1884 | 	 * invocations of {@link #securityMatchers(Customizer)}
 1885 | 	 * {@link #securityMatcher(String...)} and {@link #securityMatcher(RequestMatcher)}
 1886 | 	 * </p>
 1887 | 	 *
 1888 | 	 * <h3>Example Configurations</h3>
 1889 | 	 *
 1890 | 	 * The following configuration enables the {@link HttpSecurity} for URLs that begin
 1891 | 	 * with "/api/" or "/oauth/".
 1892 | 	 *
 1893 | 	 * <pre>
 1894 | 	 * &#064;Configuration
 1895 | 	 * &#064;EnableWebSecurity
 1896 | 	 * public class RequestMatchersSecurityConfig {
 1897 | 	 *
 1898 | 	 * 	&#064;Bean
 1899 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
 1900 | 	 * 		http
 1901 | 	 * 			.securityMatchers((matchers) -&gt; matchers
 1902 | 	 * 				.requestMatchers(&quot;/api/**&quot;, &quot;/oauth/**&quot;)
 1903 | 	 * 			)
 1904 | 	 * 			.authorizeHttpRequests((authorize) -&gt; authorize
 1905 | 	 * 				.anyRequest().hasRole(&quot;USER&quot;)
 1906 | 	 * 			)
 1907 | 	 * 			.httpBasic(withDefaults());
 1908 | 	 * 		return http.build();
 1909 | 	 * 	}
 1910 | 	 *
 1911 | 	 * 	&#064;Bean
 1912 | 	 * 	public UserDetailsService userDetailsService() {
 1913 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
 1914 | 	 * 			.username(&quot;user&quot;)
 1915 | 	 * 			.password(&quot;password&quot;)
 1916 | 	 * 			.roles(&quot;USER&quot;)
 1917 | 	 * 			.build();
 1918 | 	 * 		return new InMemoryUserDetailsManager(user);
 1919 | 	 * 	}
 1920 | 	 * }
 1921 | 	 * </pre>
 1922 | 	 *
 1923 | 	 * The configuration below is the same as the previous configuration.
 1924 | 	 *
 1925 | 	 * <pre>
 1926 | 	 * &#064;Configuration
 1927 | 	 * &#064;EnableWebSecurity
 1928 | 	 * public class RequestMatchersSecurityConfig {
 1929 | 	 *
 1930 | 	 * 	&#064;Bean
 1931 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
 1932 | 	 * 		http
 1933 | 	 * 			.securityMatchers((matchers) -&gt; matchers
 1934 | 	 * 				.requestMatchers(&quot;/api/**&quot;)
 1935 | 	 * 				.requestMatchers(&quot;/oauth/**&quot;)
 1936 | 	 * 			)
 1937 | 	 * 			.authorizeHttpRequests((authorize) -&gt; authorize
 1938 | 	 * 				.anyRequest().hasRole(&quot;USER&quot;)
 1939 | 	 * 			)
 1940 | 	 * 			.httpBasic(withDefaults());
 1941 | 	 * 		return http.build();
 1942 | 	 * 	}
 1943 | 	 *
 1944 | 	 * 	&#064;Bean
 1945 | 	 * 	public UserDetailsService userDetailsService() {
 1946 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
 1947 | 	 * 			.username(&quot;user&quot;)
 1948 | 	 * 			.password(&quot;password&quot;)
 1949 | 	 * 			.roles(&quot;USER&quot;)
 1950 | 	 * 			.build();
 1951 | 	 * 		return new InMemoryUserDetailsManager(user);
 1952 | 	 * 	}
 1953 | 	 * }
 1954 | 	 * </pre>
 1955 | 	 *
 1956 | 	 * The configuration below is also the same as the above configuration.
 1957 | 	 *
 1958 | 	 * <pre>
 1959 | 	 * &#064;Configuration
 1960 | 	 * &#064;EnableWebSecurity
 1961 | 	 * public class RequestMatchersSecurityConfig {
 1962 | 	 *
 1963 | 	 * 	&#064;Bean
 1964 | 	 * 	public SecurityFilterChain securityFilterChain(HttpSecurity http)  {
 1965 | 	 * 		http
 1966 | 	 * 			.securityMatchers((matchers) -&gt; matchers
 1967 | 	 * 				.requestMatchers(&quot;/api/**&quot;)
 1968 | 	 * 			)
 1969 | 	 *			.securityMatchers((matchers) -&gt; matchers
 1970 | 	 *				.requestMatchers(&quot;/oauth/**&quot;)
 1971 | 	 * 			)
 1972 | 	 * 			.authorizeHttpRequests((authorize) -&gt; authorize
 1973 | 	 * 				.anyRequest().hasRole(&quot;USER&quot;)
 1974 | 	 * 			)
 1975 | 	 * 			.httpBasic(withDefaults());
 1976 | 	 * 		return http.build();
 1977 | 	 * 	}
 1978 | 	 *
 1979 | 	 * 	&#064;Bean
 1980 | 	 * 	public UserDetailsService userDetailsService() {
 1981 | 	 * 		UserDetails user = User.withDefaultPasswordEncoder()
 1982 | 	 * 			.username(&quot;user&quot;)
 1983 | 	 * 			.password(&quot;password&quot;)
 1984 | 	 * 			.roles(&quot;USER&quot;)
 1985 | 	 * 			.build();
 1986 | 	 * 		return new InMemoryUserDetailsManager(user);
 1987 | 	 * 	}
 1988 | 	 * }
 1989 | 	 * </pre>
 1990 | 	 * @param requestMatcherCustomizer the {@link Customizer} to provide more options for
 1991 | 	 * the {@link RequestMatcherConfigurer}
 1992 | 	 * @return the {@link HttpSecurity} for further customizations
 1993 | 	 */
 1994 | 	public HttpSecurity securityMatchers(Customizer<RequestMatcherConfigurer> requestMatcherCustomizer) {
 1995 | 		requestMatcherCustomizer.customize(this.requestMatcherConfigurer);
 1996 | 		return HttpSecurity.this;
 1997 | 	}
 1998 | 
 1999 | 	/**
 2000 | 	 * Allows configuring the {@link HttpSecurity} to only be invoked when matching the
 2001 | 	 * provided {@link RequestMatcher}. If more advanced configuration is necessary,
 2002 | 	 * consider using {@link #securityMatchers(Customizer)} ()}.
 2003 | 	 *
 2004 | 	 * <p>
 2005 | 	 * Invoking {@link #securityMatcher(RequestMatcher)} will override previous
 2006 | 	 * invocations of {@link #securityMatcher(RequestMatcher)},
 2007 | 	 * {@link #securityMatcher(String...)} and {@link #securityMatchers(Customizer)}
 2008 | 	 * </p>
 2009 | 	 * @param requestMatcher the {@link RequestMatcher} to use, for example,
 2010 | 	 * {@code PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/admin/**")}
 2011 | 	 * @return the {@link HttpSecurity} for further customizations
 2012 | 	 * @see #securityMatcher(String...)
 2013 | 	 */
 2014 | 	public HttpSecurity securityMatcher(RequestMatcher requestMatcher) {
 2015 | 		this.requestMatcher = requestMatcher;
 2016 | 		return this;
 2017 | 	}
 2018 | 
 2019 | 	/**
 2020 | 	 * Allows configuring the {@link HttpSecurity} to only be invoked when matching the
 2021 | 	 * provided set of {@code patterns}. See
 2022 | 	 * {@link org.springframework.web.util.pattern.PathPattern} for matching rules
 2023 | 	 *
 2024 | 	 * <p>
 2025 | 	 * Invoking {@link #securityMatcher(String...)} will override previous invocations of
 2026 | 	 * {@link #securityMatcher(String...)}, {@link #securityMatcher(RequestMatcher)} and
 2027 | 	 * {@link #securityMatchers(Customizer)}.
 2028 | 	 * </p>
 2029 | 	 * @param patterns the pattern to match on (i.e. "/admin/**")
 2030 | 	 * @return the {@link HttpSecurity} for further customizations
 2031 | 	 * @see org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
 2032 | 	 * @see org.springframework.web.util.pattern.PathPattern
 2033 | 	 */
 2034 | 	public HttpSecurity securityMatcher(String... patterns) {
 2035 | 		List<RequestMatcher> matchers = new ArrayList<>();
 2036 | 		ApplicationContext context = getSharedObject(ApplicationContext.class);
 2037 | 		PathPatternRequestMatcher.Builder builder = context.getBeanProvider(PathPatternRequestMatcher.Builder.class)
 2038 | 			.getIfUnique(() -> getSharedObject(PathPatternRequestMatcher.Builder.class));
 2039 | 		for (String pattern : patterns) {
 2040 | 			matchers.add(builder.matcher(pattern));
 2041 | 		}
 2042 | 		this.requestMatcher = new OrRequestMatcher(matchers);
 2043 | 		return this;
 2044 | 	}
 2045 | 
 2046 | 	/**
 2047 | 	 * Specifies webAuthn/passkeys based authentication.
 2048 | 	 *
 2049 | 	 * <pre>
 2050 | 	 * 	&#064;Bean
 2051 | 	 * 	SecurityFilterChain securityFilterChain(HttpSecurity http)  {
 2052 | 	 * 		http
 2053 | 	 * 			// ...
 2054 | 	 * 			.webAuthn((webAuthn) -&gt; webAuthn
 2055 | 	 * 				.rpId("example.com")
 2056 | 	 * 				.allowedOrigins("https://example.com")
 2057 | 	 * 			);
 2058 | 	 * 		return http.build();
 2059 | 	 * 	}
 2060 | 	 * </pre>
 2061 | 	 * @param webAuthn the customizer to apply
 2062 | 	 * @return the {@link HttpSecurity} for further customizations @
 2063 | 	 */
 2064 | 	public HttpSecurity webAuthn(Customizer<WebAuthnConfigurer<HttpSecurity>> webAuthn) {
 2065 | 		webAuthn.customize(getOrApply(new WebAuthnConfigurer<>()));
 2066 | 		return HttpSecurity.this;
 2067 | 	}
 2068 | 
 2069 | 	/**
 2070 | 	 * If the {@link SecurityConfigurer} has already been specified get the original,
 2071 | 	 * otherwise apply the new {@link SecurityConfigurerAdapter}.
 2072 | 	 * @param configurer the {@link SecurityConfigurer} to apply if one is not found for
 2073 | 	 * this {@link SecurityConfigurer} class.
 2074 | 	 * @return the current {@link SecurityConfigurer} for the configurer passed in @
 2075 | 	 */
 2076 | 	@SuppressWarnings("unchecked")
 2077 | 	private <C extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity>> C getOrApply(C configurer) {
 2078 | 		C existingConfig = (C) getConfigurer(configurer.getClass());
 2079 | 		if (existingConfig != null) {
 2080 | 			return existingConfig;
 2081 | 		}
 2082 | 		with(configurer);
 2083 | 		return configurer;
 2084 | 	}
 2085 | 
 2086 | 	private ObjectPostProcessor<AuthenticationManager> getAuthenticationManagerPostProcessor() {
 2087 | 		ApplicationContext context = getContext();
 2088 | 		ResolvableType type = ResolvableType.forClassWithGenerics(ObjectPostProcessor.class,
 2089 | 				AuthenticationManager.class);
 2090 | 		ObjectProvider<ObjectPostProcessor<AuthenticationManager>> manager = context.getBeanProvider(type);
 2091 | 		return manager.getIfUnique(ObjectPostProcessor::identity);
 2092 | 	}
 2093 | 
 2094 | 	/**
 2095 | 	 * Allows mapping HTTP requests that this {@link HttpSecurity} will be used for.
 2096 | 	 *
 2097 | 	 * @author Rob Winch
 2098 | 	 * @since 3.2
 2099 | 	 */
 2100 | 	public class RequestMatcherConfigurer extends AbstractRequestMatcherRegistry<RequestMatcherConfigurer> {
 2101 | 
 2102 | 		protected List<RequestMatcher> matchers = new ArrayList<>();
 2103 | 
 2104 | 		RequestMatcherConfigurer(ApplicationContext context) {
 2105 | 			setApplicationContext(context);
 2106 | 		}
 2107 | 
 2108 | 		@Override
 2109 | 		protected RequestMatcherConfigurer chainRequestMatchers(List<RequestMatcher> requestMatchers) {
 2110 | 			setMatchers(requestMatchers);
 2111 | 			return this;
 2112 | 		}
 2113 | 
 2114 | 		private void setMatchers(List<? extends RequestMatcher> requestMatchers) {
 2115 | 			this.matchers.addAll(requestMatchers);
 2116 | 			securityMatcher(new OrRequestMatcher(this.matchers));
 2117 | 		}
 2118 | 
 2119 | 	}
 2120 | 
 2121 | 	/**
 2122 | 	 * A Filter that implements Ordered to be sorted. After sorting occurs, the original
 2123 | 	 * filter is what is used by FilterChainProxy
 2124 | 	 */
 2125 | 	private static final class OrderedFilter implements Ordered, Filter {
 2126 | 
 2127 | 		private final Filter filter;
 2128 | 
 2129 | 		private final int order;
 2130 | 
 2131 | 		private OrderedFilter(Filter filter, int order) {
 2132 | 			this.filter = filter;
 2133 | 			this.order = order;
 2134 | 		}
 2135 | 
 2136 | 		@Override
 2137 | 		public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
 2138 | 				throws IOException, ServletException {
 2139 | 			this.filter.doFilter(servletRequest, servletResponse, filterChain);
 2140 | 		}
 2141 | 
 2142 | 		@Override
 2143 | 		public int getOrder() {
 2144 | 			return this.order;
 2145 | 		}
 2146 | 
 2147 | 		@Override
 2148 | 		public String toString() {
 2149 | 			return "OrderedFilter{" + "filter=" + this.filter + ", order=" + this.order + '}';
 2150 | 		}
 2151 | 
 2152 | 	}
 2153 | 
 2154 | }
```
