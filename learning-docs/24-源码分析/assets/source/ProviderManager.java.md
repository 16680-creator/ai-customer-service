<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authentication/ProviderManager.java -->
<!-- 正文引用：[[源码:ProviderManager.java#L起-L止]] ；代码块头部：(src: ProviderManager.java#L起-L止) -->

# ProviderManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authentication/ProviderManager.java` |
| 行数 | 328 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:44.636Z |

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
   17 | package org.springframework.security.authentication;
   18 | 
   19 | import java.util.Arrays;
   20 | import java.util.Collections;
   21 | import java.util.List;
   22 | 
   23 | import org.apache.commons.logging.Log;
   24 | import org.apache.commons.logging.LogFactory;
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.beans.factory.InitializingBean;
   28 | import org.springframework.context.MessageSource;
   29 | import org.springframework.context.MessageSourceAware;
   30 | import org.springframework.context.support.MessageSourceAccessor;
   31 | import org.springframework.core.log.LogMessage;
   32 | import org.springframework.security.core.Authentication;
   33 | import org.springframework.security.core.AuthenticationException;
   34 | import org.springframework.security.core.CredentialsContainer;
   35 | import org.springframework.security.core.SpringSecurityMessageSource;
   36 | import org.springframework.util.Assert;
   37 | 
   38 | /**
   39 |  * Iterates an {@link Authentication} request through a list of
   40 |  * {@link AuthenticationProvider}s.
   41 |  *
   42 |  * <p>
   43 |  * <tt>AuthenticationProvider</tt>s are usually tried in order until one provides a
   44 |  * non-null response. A non-null response indicates the provider had authority to decide
   45 |  * on the authentication request and no further providers are tried. If a subsequent
   46 |  * provider successfully authenticates the request, the earlier authentication exception
   47 |  * is disregarded and the successful authentication will be used. If no subsequent
   48 |  * provider provides a non-null response, or a new <code>AuthenticationException</code>,
   49 |  * the last <code>AuthenticationException</code> received will be used. If no provider
   50 |  * returns a non-null response, or indicates it can even process an
   51 |  * <code>Authentication</code>, the <code>ProviderManager</code> will throw a
   52 |  * <code>ProviderNotFoundException</code>. A parent {@code AuthenticationManager} can also
   53 |  * be set, and this will also be tried if none of the configured providers can perform the
   54 |  * authentication. This is intended to support namespace configuration options though and
   55 |  * is not a feature that should normally be required.
   56 |  * <p>
   57 |  * The exception to this process is when a provider throws an
   58 |  * {@link AccountStatusException}, in which case no further providers in the list will be
   59 |  * queried.
   60 |  *
   61 |  * Post-authentication, the credentials will be cleared from the returned
   62 |  * {@code Authentication} object, if it implements the {@link CredentialsContainer}
   63 |  * interface. This behaviour can be controlled by modifying the
   64 |  * {@link #setEraseCredentialsAfterAuthentication(boolean)
   65 |  * eraseCredentialsAfterAuthentication} property.
   66 |  *
   67 |  * <h2>Event Publishing</h2>
   68 |  * <p>
   69 |  * Authentication event publishing is delegated to the configured
   70 |  * {@link AuthenticationEventPublisher} which defaults to a null implementation which
   71 |  * doesn't publish events, so if you are configuring the bean yourself you must inject a
   72 |  * publisher bean if you want to receive events. The standard implementation is
   73 |  * {@link DefaultAuthenticationEventPublisher} which maps common exceptions to events (in
   74 |  * the case of authentication failure) and publishes an
   75 |  * {@link org.springframework.security.authentication.event.AuthenticationSuccessEvent
   76 |  * AuthenticationSuccessEvent} if authentication succeeds. If you are using the namespace
   77 |  * then an instance of this bean will be used automatically by the <tt>&lt;http&gt;</tt>
   78 |  * configuration, so you will receive events from the web part of your application
   79 |  * automatically.
   80 |  * <p>
   81 |  * Note that the implementation also publishes authentication failure events when it
   82 |  * obtains an authentication result (or an exception) from the "parent"
   83 |  * {@code AuthenticationManager} if one has been set. So in this situation, the parent
   84 |  * should not generally be configured to publish events or there will be duplicates.
   85 |  *
   86 |  * @author Ben Alex
   87 |  * @author Luke Taylor
   88 |  * @see DefaultAuthenticationEventPublisher
   89 |  */
   90 | public class ProviderManager implements AuthenticationManager, MessageSourceAware, InitializingBean {
   91 | 
   92 | 	private static final Log logger = LogFactory.getLog(ProviderManager.class);
   93 | 
   94 | 	private AuthenticationEventPublisher eventPublisher = new NullEventPublisher();
   95 | 
   96 | 	private List<AuthenticationProvider> providers = Collections.emptyList();
   97 | 
   98 | 	protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
   99 | 
  100 | 	private @Nullable AuthenticationManager parent;
  101 | 
  102 | 	private boolean eraseCredentialsAfterAuthentication = true;
  103 | 
  104 | 	/**
  105 | 	 * Construct a {@link ProviderManager} using the given
  106 | 	 * {@link AuthenticationProvider}s.
  107 | 	 * @param providers the {@link AuthenticationProvider}s to use
  108 | 	 */
  109 | 	public ProviderManager(AuthenticationProvider... providers) {
  110 | 		this(Arrays.asList(providers), null);
  111 | 	}
  112 | 
  113 | 	/**
  114 | 	 * Construct a {@link ProviderManager} using the given
  115 | 	 * {@link AuthenticationProvider}s.
  116 | 	 * @param providers the {@link AuthenticationProvider}s to use
  117 | 	 */
  118 | 	public ProviderManager(List<AuthenticationProvider> providers) {
  119 | 		this(providers, null);
  120 | 	}
  121 | 
  122 | 	/**
  123 | 	 * Construct a {@link ProviderManager} using the provided parameters.
  124 | 	 * @param providers the {@link AuthenticationProvider}s to use
  125 | 	 * @param parent a parent {@link AuthenticationManager} to fall back to
  126 | 	 */
  127 | 	public ProviderManager(List<AuthenticationProvider> providers, @Nullable AuthenticationManager parent) {
  128 | 		Assert.notNull(providers, "providers list cannot be null");
  129 | 		this.providers = providers;
  130 | 		this.parent = parent;
  131 | 		checkState();
  132 | 	}
  133 | 
  134 | 	@Override
  135 | 	public void afterPropertiesSet() {
  136 | 		checkState();
  137 | 	}
  138 | 
  139 | 	private void checkState() {
  140 | 		Assert.isTrue(this.parent != null || !this.providers.isEmpty(),
  141 | 				"A parent AuthenticationManager or a list of AuthenticationProviders is required");
  142 | 		Assert.noNullElements(this.providers, "providers list cannot contain null values");
  143 | 	}
  144 | 
  145 | 	/**
  146 | 	 * Attempts to authenticate the passed {@link Authentication} object.
  147 | 	 * <p>
  148 | 	 * The list of {@link AuthenticationProvider}s will be successively tried until an
  149 | 	 * <code>AuthenticationProvider</code> indicates it is capable of authenticating the
  150 | 	 * type of <code>Authentication</code> object passed. Authentication will then be
  151 | 	 * attempted with that <code>AuthenticationProvider</code>.
  152 | 	 * <p>
  153 | 	 * If more than one <code>AuthenticationProvider</code> supports the passed
  154 | 	 * <code>Authentication</code> object, the first one able to successfully authenticate
  155 | 	 * the <code>Authentication</code> object determines the <code>result</code>,
  156 | 	 * overriding any possible <code>AuthenticationException</code> thrown by earlier
  157 | 	 * supporting <code>AuthenticationProvider</code>s. On successful authentication, no
  158 | 	 * subsequent <code>AuthenticationProvider</code>s will be tried. If authentication
  159 | 	 * was not successful by any supporting <code>AuthenticationProvider</code> the last
  160 | 	 * thrown <code>AuthenticationException</code> will be rethrown.
  161 | 	 * @param authentication the authentication request object.
  162 | 	 * @return a fully authenticated object including credentials.
  163 | 	 * @throws AuthenticationException if authentication fails.
  164 | 	 */
  165 | 	@Override
  166 | 	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
  167 | 		Class<? extends Authentication> toTest = authentication.getClass();
  168 | 		AuthenticationException lastException = null;
  169 | 		AuthenticationException parentException = null;
  170 | 		Authentication result = null;
  171 | 		Authentication parentResult = null;
  172 | 		int currentPosition = 0;
  173 | 		int size = this.providers.size();
  174 | 		for (AuthenticationProvider provider : getProviders()) {
  175 | 			if (!provider.supports(toTest)) {
  176 | 				continue;
  177 | 			}
  178 | 			if (logger.isTraceEnabled()) {
  179 | 				logger.trace(LogMessage.format("Authenticating request with %s (%d/%d)",
  180 | 						provider.getClass().getSimpleName(), ++currentPosition, size));
  181 | 			}
  182 | 			try {
  183 | 				result = provider.authenticate(authentication);
  184 | 				if (result != null) {
  185 | 					copyDetails(authentication, result);
  186 | 					break;
  187 | 				}
  188 | 			}
  189 | 			catch (AccountStatusException ex) {
  190 | 				prepareException(ex, authentication);
  191 | 				logger.debug(LogMessage.format("Authentication failed for user '%s' since their account status is %s",
  192 | 						authentication.getName(), ex.getMessage()), ex);
  193 | 				// SEC-546: Avoid polling additional providers if auth failure is due to
  194 | 				// invalid account status
  195 | 				throw ex;
  196 | 			}
  197 | 			catch (InternalAuthenticationServiceException ex) {
  198 | 				prepareException(ex, authentication);
  199 | 				logger.debug(LogMessage.format("Authentication service failed internally for user '%s'",
  200 | 						authentication.getName()), ex);
  201 | 				// SEC-546: Avoid polling additional providers if auth failure is due to
  202 | 				// invalid account status
  203 | 				throw ex;
  204 | 			}
  205 | 			catch (AuthenticationException ex) {
  206 | 				ex.setAuthenticationRequest(authentication);
  207 | 				logger.debug(LogMessage.format("Authentication failed with provider %s since %s",
  208 | 						provider.getClass().getSimpleName(), ex.getMessage()));
  209 | 				lastException = ex;
  210 | 			}
  211 | 		}
  212 | 		if (result == null && this.parent != null) {
  213 | 			// Allow the parent to try.
  214 | 			try {
  215 | 				parentResult = this.parent.authenticate(authentication);
  216 | 				result = parentResult;
  217 | 			}
  218 | 			catch (ProviderNotFoundException ex) {
  219 | 				// ignore as we will throw below if no other exception occurred prior to
  220 | 				// calling parent and the parent
  221 | 				// may throw ProviderNotFound even though a provider in the child already
  222 | 				// handled the request
  223 | 			}
  224 | 			catch (AuthenticationException ex) {
  225 | 				parentException = ex;
  226 | 				lastException = ex;
  227 | 			}
  228 | 		}
  229 | 		if (result != null) {
  230 | 			if (this.eraseCredentialsAfterAuthentication && (result instanceof CredentialsContainer)) {
  231 | 				// Authentication is complete. Remove credentials and other secret data
  232 | 				// from authentication
  233 | 				((CredentialsContainer) result).eraseCredentials();
  234 | 			}
  235 | 			// If the parent AuthenticationManager was attempted and successful then it
  236 | 			// will publish an AuthenticationSuccessEvent
  237 | 			// This check prevents a duplicate AuthenticationSuccessEvent if the parent
  238 | 			// AuthenticationManager already published it
  239 | 			if (parentResult == null) {
  240 | 				this.eventPublisher.publishAuthenticationSuccess(result);
  241 | 			}
  242 | 
  243 | 			return result;
  244 | 		}
  245 | 
  246 | 		// Parent was null, or didn't authenticate (or throw an exception).
  247 | 		if (lastException == null) {
  248 | 			lastException = new ProviderNotFoundException(this.messages.getMessage("ProviderManager.providerNotFound",
  249 | 					new Object[] { toTest.getName() }, "No AuthenticationProvider found for {0}"));
  250 | 		}
  251 | 		// If the parent AuthenticationManager was attempted and failed then it will
  252 | 		// publish an AbstractAuthenticationFailureEvent
  253 | 		// This check prevents a duplicate AbstractAuthenticationFailureEvent if the
  254 | 		// parent AuthenticationManager already published it
  255 | 		if (parentException == null) {
  256 | 			prepareException(lastException, authentication);
  257 | 		}
  258 | 
  259 | 		// Ensure this message is not logged when authentication is attempted by
  260 | 		// the parent provider
  261 | 		if (this.parent != null) {
  262 | 			logger.debug("Denying authentication since all attempted providers failed");
  263 | 		}
  264 | 
  265 | 		throw lastException;
  266 | 	}
  267 | 
  268 | 	@SuppressWarnings("deprecation")
  269 | 	private void prepareException(AuthenticationException ex, Authentication auth) {
  270 | 		ex.setAuthenticationRequest(auth);
  271 | 		this.eventPublisher.publishAuthenticationFailure(ex, auth);
  272 | 	}
  273 | 
  274 | 	/**
  275 | 	 * Copies the authentication details from a source Authentication object to a
  276 | 	 * destination one, provided the latter does not already have one set.
  277 | 	 * @param source source authentication
  278 | 	 * @param dest the destination authentication object
  279 | 	 */
  280 | 	private void copyDetails(Authentication source, Authentication dest) {
  281 | 		if ((dest instanceof AbstractAuthenticationToken token) && (dest.getDetails() == null)) {
  282 | 			token.setDetails(source.getDetails());
  283 | 		}
  284 | 	}
  285 | 
  286 | 	public List<AuthenticationProvider> getProviders() {
  287 | 		return this.providers;
  288 | 	}
  289 | 
  290 | 	@Override
  291 | 	public void setMessageSource(MessageSource messageSource) {
  292 | 		this.messages = new MessageSourceAccessor(messageSource);
  293 | 	}
  294 | 
  295 | 	public void setAuthenticationEventPublisher(AuthenticationEventPublisher eventPublisher) {
  296 | 		Assert.notNull(eventPublisher, "AuthenticationEventPublisher cannot be null");
  297 | 		this.eventPublisher = eventPublisher;
  298 | 	}
  299 | 
  300 | 	/**
  301 | 	 * If set to, a resulting {@code Authentication} which implements the
  302 | 	 * {@code CredentialsContainer} interface will have its
  303 | 	 * {@link CredentialsContainer#eraseCredentials() eraseCredentials} method called
  304 | 	 * before it is returned from the {@code authenticate()} method.
  305 | 	 * @param eraseSecretData set to {@literal false} to retain the credentials data in
  306 | 	 * memory. Defaults to {@literal true}.
  307 | 	 */
  308 | 	public void setEraseCredentialsAfterAuthentication(boolean eraseSecretData) {
  309 | 		this.eraseCredentialsAfterAuthentication = eraseSecretData;
  310 | 	}
  311 | 
  312 | 	public boolean isEraseCredentialsAfterAuthentication() {
  313 | 		return this.eraseCredentialsAfterAuthentication;
  314 | 	}
  315 | 
  316 | 	private static final class NullEventPublisher implements AuthenticationEventPublisher {
  317 | 
  318 | 		@Override
  319 | 		public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {
  320 | 		}
  321 | 
  322 | 		@Override
  323 | 		public void publishAuthenticationSuccess(Authentication authentication) {
  324 | 		}
  325 | 
  326 | 	}
  327 | 
  328 | }
```
