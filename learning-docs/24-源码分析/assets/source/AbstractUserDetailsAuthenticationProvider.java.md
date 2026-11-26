<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authentication/dao/AbstractUserDetailsAuthenticationProvider.java -->
<!-- 正文引用：[[源码:AbstractUserDetailsAuthenticationProvider.java#L起-L止]] ；代码块头部：(src: AbstractUserDetailsAuthenticationProvider.java#L起-L止) -->

# AbstractUserDetailsAuthenticationProvider.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authentication/dao/AbstractUserDetailsAuthenticationProvider.java` |
| 行数 | 407 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:44.760Z |

```java
    1 | /*
    2 |  * Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
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
   17 | package org.springframework.security.authentication.dao;
   18 | 
   19 | import java.util.Collection;
   20 | import java.util.LinkedHashSet;
   21 | 
   22 | import org.apache.commons.logging.Log;
   23 | import org.apache.commons.logging.LogFactory;
   24 | 
   25 | import org.springframework.beans.factory.InitializingBean;
   26 | import org.springframework.context.MessageSource;
   27 | import org.springframework.context.MessageSourceAware;
   28 | import org.springframework.context.support.MessageSourceAccessor;
   29 | import org.springframework.core.log.LogMessage;
   30 | import org.springframework.security.authentication.AccountExpiredException;
   31 | import org.springframework.security.authentication.AuthenticationProvider;
   32 | import org.springframework.security.authentication.BadCredentialsException;
   33 | import org.springframework.security.authentication.CredentialsExpiredException;
   34 | import org.springframework.security.authentication.DisabledException;
   35 | import org.springframework.security.authentication.LockedException;
   36 | import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
   37 | import org.springframework.security.core.Authentication;
   38 | import org.springframework.security.core.AuthenticationException;
   39 | import org.springframework.security.core.GrantedAuthority;
   40 | import org.springframework.security.core.SpringSecurityMessageSource;
   41 | import org.springframework.security.core.authority.FactorGrantedAuthority;
   42 | import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
   43 | import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
   44 | import org.springframework.security.core.userdetails.UserCache;
   45 | import org.springframework.security.core.userdetails.UserDetails;
   46 | import org.springframework.security.core.userdetails.UserDetailsChecker;
   47 | import org.springframework.security.core.userdetails.UserDetailsService;
   48 | import org.springframework.security.core.userdetails.UsernameNotFoundException;
   49 | import org.springframework.security.core.userdetails.cache.NullUserCache;
   50 | import org.springframework.util.Assert;
   51 | 
   52 | /**
   53 |  * A base {@link AuthenticationProvider} that allows subclasses to override and work with
   54 |  * {@link org.springframework.security.core.userdetails.UserDetails} objects. The class is
   55 |  * designed to respond to {@link UsernamePasswordAuthenticationToken} authentication
   56 |  * requests.
   57 |  *
   58 |  * <p>
   59 |  * Upon successful validation, a <code>UsernamePasswordAuthenticationToken</code> will be
   60 |  * created and returned to the caller. The token will include as its principal either a
   61 |  * <code>String</code> representation of the username, or the {@link UserDetails} that was
   62 |  * returned from the authentication repository. Using <code>String</code> is appropriate
   63 |  * if a container adapter is being used, as it expects <code>String</code> representations
   64 |  * of the username. Using <code>UserDetails</code> is appropriate if you require access to
   65 |  * additional properties of the authenticated user, such as email addresses,
   66 |  * human-friendly names etc. As container adapters are not recommended to be used, and
   67 |  * <code>UserDetails</code> implementations provide additional flexibility, by default a
   68 |  * <code>UserDetails</code> is returned. To override this default, set the
   69 |  * {@link #setForcePrincipalAsString} to <code>true</code>.
   70 |  * <p>
   71 |  * Caching is handled by storing the <code>UserDetails</code> object being placed in the
   72 |  * {@link UserCache}. This ensures that subsequent requests with the same username can be
   73 |  * validated without needing to query the {@link UserDetailsService}. It should be noted
   74 |  * that if a user appears to present an incorrect password, the {@link UserDetailsService}
   75 |  * will be queried to confirm the most up-to-date password was used for comparison.
   76 |  * Caching is only likely to be required for stateless applications. In a normal web
   77 |  * application, for example, the <tt>SecurityContext</tt> is stored in the user's session
   78 |  * and the user isn't reauthenticated on each request. The default cache implementation is
   79 |  * therefore {@link NullUserCache}.
   80 |  *
   81 |  * @author Ben Alex
   82 |  */
   83 | public abstract class AbstractUserDetailsAuthenticationProvider
   84 | 		implements AuthenticationProvider, InitializingBean, MessageSourceAware {
   85 | 
   86 | 	protected final Log logger = LogFactory.getLog(getClass());
   87 | 
   88 | 	protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
   89 | 
   90 | 	private UserCache userCache = new NullUserCache();
   91 | 
   92 | 	private boolean forcePrincipalAsString = false;
   93 | 
   94 | 	protected boolean hideUserNotFoundExceptions = true;
   95 | 
   96 | 	private UserDetailsChecker preAuthenticationChecks = new DefaultPreAuthenticationChecks();
   97 | 
   98 | 	private UserDetailsChecker postAuthenticationChecks = new DefaultPostAuthenticationChecks();
   99 | 
  100 | 	private boolean alwaysPerformAdditionalChecksOnUser = true;
  101 | 
  102 | 	private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();
  103 | 
  104 | 	private static final String AUTHORITY = FactorGrantedAuthority.PASSWORD_AUTHORITY;
  105 | 
  106 | 	/**
  107 | 	 * Allows subclasses to perform any additional checks of a returned (or cached)
  108 | 	 * <code>UserDetails</code> for a given authentication request. Generally a subclass
  109 | 	 * will at least compare the {@link Authentication#getCredentials()} with a
  110 | 	 * {@link UserDetails#getPassword()}. If custom logic is needed to compare additional
  111 | 	 * properties of <code>UserDetails</code> and/or
  112 | 	 * <code>UsernamePasswordAuthenticationToken</code>, these should also appear in this
  113 | 	 * method.
  114 | 	 * @param userDetails as retrieved from the
  115 | 	 * {@link #retrieveUser(String, UsernamePasswordAuthenticationToken)} or
  116 | 	 * <code>UserCache</code>
  117 | 	 * @param authentication the current request that needs to be authenticated
  118 | 	 * @throws AuthenticationException AuthenticationException if the credentials could
  119 | 	 * not be validated (generally a <code>BadCredentialsException</code>, an
  120 | 	 * <code>AuthenticationServiceException</code>)
  121 | 	 */
  122 | 	protected abstract void additionalAuthenticationChecks(UserDetails userDetails,
  123 | 			UsernamePasswordAuthenticationToken authentication) throws AuthenticationException;
  124 | 
  125 | 	@Override
  126 | 	public final void afterPropertiesSet() {
  127 | 		Assert.notNull(this.userCache, "A user cache must be set");
  128 | 		Assert.notNull(this.messages, "A message source must be set");
  129 | 		Assert.notNull(this.preAuthenticationChecks, "A pre authentication checks must be set");
  130 | 		Assert.notNull(this.postAuthenticationChecks, "A post authentication checks must be set");
  131 | 		doAfterPropertiesSet();
  132 | 	}
  133 | 
  134 | 	@Override
  135 | 	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
  136 | 		Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication,
  137 | 				() -> this.messages.getMessage("AbstractUserDetailsAuthenticationProvider.onlySupports",
  138 | 						"Only UsernamePasswordAuthenticationToken is supported"));
  139 | 		String username = determineUsername(authentication);
  140 | 		boolean cacheWasUsed = true;
  141 | 		UserDetails user = this.userCache.getUserFromCache(username);
  142 | 		if (user == null) {
  143 | 			cacheWasUsed = false;
  144 | 			try {
  145 | 				user = retrieveUser(username, (UsernamePasswordAuthenticationToken) authentication);
  146 | 			}
  147 | 			catch (UsernameNotFoundException ex) {
  148 | 				this.logger.debug(LogMessage.format("Failed to find user '%s'", username));
  149 | 				String message = this.messages.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials",
  150 | 						"Bad credentials");
  151 | 				if (!this.hideUserNotFoundExceptions) {
  152 | 					throw ex;
  153 | 				}
  154 | 				throw new BadCredentialsException(message, ex);
  155 | 			}
  156 | 			Assert.notNull(user, "retrieveUser returned null - a violation of the interface contract");
  157 | 		}
  158 | 		try {
  159 | 			performPreCheck(user, (UsernamePasswordAuthenticationToken) authentication);
  160 | 		}
  161 | 		catch (AuthenticationException ex) {
  162 | 			if (!cacheWasUsed) {
  163 | 				throw ex;
  164 | 			}
  165 | 			// There was a problem, so try again after checking
  166 | 			// we're using latest data (i.e. not from the cache)
  167 | 			cacheWasUsed = false;
  168 | 			user = retrieveUser(username, (UsernamePasswordAuthenticationToken) authentication);
  169 | 			performPreCheck(user, (UsernamePasswordAuthenticationToken) authentication);
  170 | 		}
  171 | 		this.postAuthenticationChecks.check(user);
  172 | 		if (!cacheWasUsed) {
  173 | 			this.userCache.putUserInCache(user);
  174 | 		}
  175 | 		Object principalToReturn = user;
  176 | 		if (this.forcePrincipalAsString) {
  177 | 			principalToReturn = user.getUsername();
  178 | 		}
  179 | 		return createSuccessAuthentication(principalToReturn, authentication, user);
  180 | 	}
  181 | 
  182 | 	private void performPreCheck(UserDetails user, UsernamePasswordAuthenticationToken authentication) {
  183 | 		try {
  184 | 			this.preAuthenticationChecks.check(user);
  185 | 		}
  186 | 		catch (AuthenticationException ex) {
  187 | 			if (!this.alwaysPerformAdditionalChecksOnUser) {
  188 | 				throw ex;
  189 | 			}
  190 | 			try {
  191 | 				additionalAuthenticationChecks(user, authentication);
  192 | 			}
  193 | 			catch (AuthenticationException ignored) {
  194 | 				// preserve the original failed check
  195 | 			}
  196 | 			throw ex;
  197 | 		}
  198 | 		additionalAuthenticationChecks(user, authentication);
  199 | 	}
  200 | 
  201 | 	private String determineUsername(Authentication authentication) {
  202 | 		return (authentication.getPrincipal() == null) ? "NONE_PROVIDED" : authentication.getName();
  203 | 	}
  204 | 
  205 | 	/**
  206 | 	 * Creates a successful {@link Authentication} object.
  207 | 	 * <p>
  208 | 	 * Protected so subclasses can override.
  209 | 	 * </p>
  210 | 	 * <p>
  211 | 	 * Subclasses will usually store the original credentials the user supplied (not
  212 | 	 * salted or encoded passwords) in the returned <code>Authentication</code> object.
  213 | 	 * </p>
  214 | 	 * @param principal that should be the principal in the returned object (defined by
  215 | 	 * the {@link #isForcePrincipalAsString()} method)
  216 | 	 * @param authentication that was presented to the provider for validation
  217 | 	 * @param user that was loaded by the implementation
  218 | 	 * @return the successful authentication token
  219 | 	 */
  220 | 	protected Authentication createSuccessAuthentication(Object principal, Authentication authentication,
  221 | 			UserDetails user) {
  222 | 		// Ensure we return the original credentials the user supplied,
  223 | 		// so subsequent attempts are successful even with encoded passwords.
  224 | 		// Also ensure we return the original getDetails(), so that future
  225 | 		// authentication events after cache expiry contain the details
  226 | 		Collection<GrantedAuthority> authorities = new LinkedHashSet<>(
  227 | 				this.authoritiesMapper.mapAuthorities(user.getAuthorities()));
  228 | 		authorities.add(FactorGrantedAuthority.fromAuthority(AUTHORITY));
  229 | 		UsernamePasswordAuthenticationToken result = UsernamePasswordAuthenticationToken.authenticated(principal,
  230 | 				authentication.getCredentials(), authorities);
  231 | 		result.setDetails(authentication.getDetails());
  232 | 		this.logger.debug("Authenticated user");
  233 | 		return result;
  234 | 	}
  235 | 
  236 | 	protected void doAfterPropertiesSet() {
  237 | 	}
  238 | 
  239 | 	public UserCache getUserCache() {
  240 | 		return this.userCache;
  241 | 	}
  242 | 
  243 | 	public boolean isForcePrincipalAsString() {
  244 | 		return this.forcePrincipalAsString;
  245 | 	}
  246 | 
  247 | 	public boolean isHideUserNotFoundExceptions() {
  248 | 		return this.hideUserNotFoundExceptions;
  249 | 	}
  250 | 
  251 | 	/**
  252 | 	 * Allows subclasses to actually retrieve the <code>UserDetails</code> from an
  253 | 	 * implementation-specific location, with the option of throwing an
  254 | 	 * <code>AuthenticationException</code> immediately if the presented credentials are
  255 | 	 * incorrect (this is especially useful if it is necessary to bind to a resource as
  256 | 	 * the user in order to obtain or generate a <code>UserDetails</code>).
  257 | 	 * <p>
  258 | 	 * Subclasses are not required to perform any caching, as the
  259 | 	 * <code>AbstractUserDetailsAuthenticationProvider</code> will by default cache the
  260 | 	 * <code>UserDetails</code>. The caching of <code>UserDetails</code> does present
  261 | 	 * additional complexity as this means subsequent requests that rely on the cache will
  262 | 	 * need to still have their credentials validated, even if the correctness of
  263 | 	 * credentials was assured by subclasses adopting a binding-based strategy in this
  264 | 	 * method. Accordingly it is important that subclasses either disable caching (if they
  265 | 	 * want to ensure that this method is the only method that is capable of
  266 | 	 * authenticating a request, as no <code>UserDetails</code> will ever be cached) or
  267 | 	 * ensure subclasses implement
  268 | 	 * {@link #additionalAuthenticationChecks(UserDetails, UsernamePasswordAuthenticationToken)}
  269 | 	 * to compare the credentials of a cached <code>UserDetails</code> with subsequent
  270 | 	 * authentication requests.
  271 | 	 * </p>
  272 | 	 * <p>
  273 | 	 * Most of the time subclasses will not perform credentials inspection in this method,
  274 | 	 * instead performing it in
  275 | 	 * {@link #additionalAuthenticationChecks(UserDetails, UsernamePasswordAuthenticationToken)}
  276 | 	 * so that code related to credentials validation need not be duplicated across two
  277 | 	 * methods.
  278 | 	 * </p>
  279 | 	 * @param username The username to retrieve
  280 | 	 * @param authentication The authentication request, which subclasses <em>may</em>
  281 | 	 * need to perform a binding-based retrieval of the <code>UserDetails</code>
  282 | 	 * @return the user information (never <code>null</code> - instead an exception should
  283 | 	 * the thrown)
  284 | 	 * @throws AuthenticationException if the credentials could not be validated
  285 | 	 * (generally a <code>BadCredentialsException</code>, an
  286 | 	 * <code>AuthenticationServiceException</code> or
  287 | 	 * <code>UsernameNotFoundException</code>)
  288 | 	 */
  289 | 	protected abstract UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication)
  290 | 			throws AuthenticationException;
  291 | 
  292 | 	public void setForcePrincipalAsString(boolean forcePrincipalAsString) {
  293 | 		this.forcePrincipalAsString = forcePrincipalAsString;
  294 | 	}
  295 | 
  296 | 	/**
  297 | 	 * By default the <code>AbstractUserDetailsAuthenticationProvider</code> throws a
  298 | 	 * <code>BadCredentialsException</code> if a username is not found or the password is
  299 | 	 * incorrect. Setting this property to <code>false</code> will cause
  300 | 	 * <code>UsernameNotFoundException</code>s to be thrown instead for the former. Note
  301 | 	 * this is considered less secure than throwing <code>BadCredentialsException</code>
  302 | 	 * for both exceptions.
  303 | 	 * @param hideUserNotFoundExceptions set to <code>false</code> if you wish
  304 | 	 * <code>UsernameNotFoundException</code>s to be thrown instead of the non-specific
  305 | 	 * <code>BadCredentialsException</code> (defaults to <code>true</code>)
  306 | 	 */
  307 | 	public void setHideUserNotFoundExceptions(boolean hideUserNotFoundExceptions) {
  308 | 		this.hideUserNotFoundExceptions = hideUserNotFoundExceptions;
  309 | 	}
  310 | 
  311 | 	@Override
  312 | 	public void setMessageSource(MessageSource messageSource) {
  313 | 		this.messages = new MessageSourceAccessor(messageSource);
  314 | 	}
  315 | 
  316 | 	public void setUserCache(UserCache userCache) {
  317 | 		this.userCache = userCache;
  318 | 	}
  319 | 
  320 | 	@Override
  321 | 	public boolean supports(Class<?> authentication) {
  322 | 		return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
  323 | 	}
  324 | 
  325 | 	protected UserDetailsChecker getPreAuthenticationChecks() {
  326 | 		return this.preAuthenticationChecks;
  327 | 	}
  328 | 
  329 | 	/**
  330 | 	 * Sets the policy will be used to verify the status of the loaded
  331 | 	 * <tt>UserDetails</tt> <em>before</em> validation of the credentials takes place.
  332 | 	 * @param preAuthenticationChecks strategy to be invoked prior to authentication.
  333 | 	 */
  334 | 	public void setPreAuthenticationChecks(UserDetailsChecker preAuthenticationChecks) {
  335 | 		this.preAuthenticationChecks = preAuthenticationChecks;
  336 | 	}
  337 | 
  338 | 	protected UserDetailsChecker getPostAuthenticationChecks() {
  339 | 		return this.postAuthenticationChecks;
  340 | 	}
  341 | 
  342 | 	public void setPostAuthenticationChecks(UserDetailsChecker postAuthenticationChecks) {
  343 | 		this.postAuthenticationChecks = postAuthenticationChecks;
  344 | 	}
  345 | 
  346 | 	/**
  347 | 	 * Set whether to always perform the additional checks on the user, even if the
  348 | 	 * pre-authentication checks fail. This is useful to ensure that regardless of the
  349 | 	 * state of the user account, authentication takes the same amount of time to
  350 | 	 * complete.
  351 | 	 *
  352 | 	 * <p>
  353 | 	 * For applications that rely on the additional checks running only once should set
  354 | 	 * this value to {@code false}
  355 | 	 * @param alwaysPerformAdditionalChecksOnUser
  356 | 	 * @since 5.7.23
  357 | 	 */
  358 | 	public void setAlwaysPerformAdditionalChecksOnUser(boolean alwaysPerformAdditionalChecksOnUser) {
  359 | 		this.alwaysPerformAdditionalChecksOnUser = alwaysPerformAdditionalChecksOnUser;
  360 | 	}
  361 | 
  362 | 	public void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
  363 | 		this.authoritiesMapper = authoritiesMapper;
  364 | 	}
  365 | 
  366 | 	private class DefaultPreAuthenticationChecks implements UserDetailsChecker {
  367 | 
  368 | 		@Override
  369 | 		public void check(UserDetails user) {
  370 | 			if (!user.isAccountNonLocked()) {
  371 | 				AbstractUserDetailsAuthenticationProvider.this.logger
  372 | 					.debug("Failed to authenticate since user account is locked");
  373 | 				throw new LockedException(AbstractUserDetailsAuthenticationProvider.this.messages
  374 | 					.getMessage("AbstractUserDetailsAuthenticationProvider.locked", "User account is locked"));
  375 | 			}
  376 | 			if (!user.isEnabled()) {
  377 | 				AbstractUserDetailsAuthenticationProvider.this.logger
  378 | 					.debug("Failed to authenticate since user account is disabled");
  379 | 				throw new DisabledException(AbstractUserDetailsAuthenticationProvider.this.messages
  380 | 					.getMessage("AbstractUserDetailsAuthenticationProvider.disabled", "User is disabled"));
  381 | 			}
  382 | 			if (!user.isAccountNonExpired()) {
  383 | 				AbstractUserDetailsAuthenticationProvider.this.logger
  384 | 					.debug("Failed to authenticate since user account has expired");
  385 | 				throw new AccountExpiredException(AbstractUserDetailsAuthenticationProvider.this.messages
  386 | 					.getMessage("AbstractUserDetailsAuthenticationProvider.expired", "User account has expired"));
  387 | 			}
  388 | 		}
  389 | 
  390 | 	}
  391 | 
  392 | 	private class DefaultPostAuthenticationChecks implements UserDetailsChecker {
  393 | 
  394 | 		@Override
  395 | 		public void check(UserDetails user) {
  396 | 			if (!user.isCredentialsNonExpired()) {
  397 | 				AbstractUserDetailsAuthenticationProvider.this.logger
  398 | 					.debug("Failed to authenticate since user account credentials have expired");
  399 | 				throw new CredentialsExpiredException(AbstractUserDetailsAuthenticationProvider.this.messages
  400 | 					.getMessage("AbstractUserDetailsAuthenticationProvider.credentialsExpired",
  401 | 							"User credentials have expired"));
  402 | 			}
  403 | 		}
  404 | 
  405 | 	}
  406 | 
  407 | }
```
