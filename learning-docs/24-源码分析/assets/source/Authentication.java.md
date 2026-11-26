<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/core/Authentication.java -->
<!-- 正文引用：[[源码:Authentication.java#L起-L止]] ；代码块头部：(src: Authentication.java#L起-L止) -->

# Authentication.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/core/Authentication.java` |
| 行数 | 246 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-26T06:37:39.956Z |

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
   17 | package org.springframework.security.core;
   18 | 
   19 | import java.io.Serializable;
   20 | import java.security.Principal;
   21 | import java.util.Collection;
   22 | import java.util.function.Consumer;
   23 | 
   24 | import org.jspecify.annotations.Nullable;
   25 | 
   26 | import org.springframework.security.authentication.AuthenticationManager;
   27 | import org.springframework.security.core.context.SecurityContextHolder;
   28 | 
   29 | /**
   30 |  * Represents the token for an authentication request or for an authenticated principal
   31 |  * once the request has been processed by the
   32 |  * {@link AuthenticationManager#authenticate(Authentication)} method.
   33 |  * <p>
   34 |  * Once the request has been authenticated, the <tt>Authentication</tt> will usually be
   35 |  * stored in a thread-local <tt>SecurityContext</tt> managed by the
   36 |  * {@link SecurityContextHolder} by the authentication mechanism which is being used. An
   37 |  * explicit authentication can be achieved, without using one of Spring Security's
   38 |  * authentication mechanisms, by creating an <tt>Authentication</tt> instance and using
   39 |  * the code:
   40 |  *
   41 |  * <pre>
   42 |  * SecurityContext context = SecurityContextHolder.createEmptyContext();
   43 |  * context.setAuthentication(anAuthentication);
   44 |  * SecurityContextHolder.setContext(context);
   45 |  * </pre>
   46 |  *
   47 |  * Note that unless the <tt>Authentication</tt> has the <tt>authenticated</tt> property
   48 |  * set to <tt>true</tt>, it will still be authenticated by any security interceptor (for
   49 |  * method or web invocations) which encounters it.
   50 |  * <p>
   51 |  * In most cases, the framework transparently takes care of managing the security context
   52 |  * and authentication objects for you.
   53 |  *
   54 |  * @author Ben Alex
   55 |  */
   56 | public interface Authentication extends Principal, Serializable {
   57 | 
   58 | 	/**
   59 | 	 * Set by an <code>AuthenticationManager</code> to indicate the authorities that the
   60 | 	 * principal has been granted. Note that classes should not rely on this value as
   61 | 	 * being valid unless it has been set by a trusted <code>AuthenticationManager</code>.
   62 | 	 * <p>
   63 | 	 * Implementations should ensure that modifications to the returned collection array
   64 | 	 * do not affect the state of the Authentication object, or use an unmodifiable
   65 | 	 * instance.
   66 | 	 * </p>
   67 | 	 * @return the authorities granted to the principal, or an empty collection if the
   68 | 	 * token has not been authenticated. Never null.
   69 | 	 */
   70 | 	Collection<? extends GrantedAuthority> getAuthorities();
   71 | 
   72 | 	/**
   73 | 	 * The credentials that prove the principal is correct. This is usually a password,
   74 | 	 * but could be anything relevant to the <code>AuthenticationManager</code>. Callers
   75 | 	 * are expected to populate the credentials.
   76 | 	 * @return the credentials that prove the identity of the <code>Principal</code>
   77 | 	 */
   78 | 	@Nullable Object getCredentials();
   79 | 
   80 | 	/**
   81 | 	 * Stores additional details about the authentication request. These might be an IP
   82 | 	 * address, certificate serial number etc.
   83 | 	 * @return additional details about the authentication request, or <code>null</code>
   84 | 	 * if not used
   85 | 	 */
   86 | 	@Nullable Object getDetails();
   87 | 
   88 | 	/**
   89 | 	 * The identity of the principal being authenticated. In the case of an authentication
   90 | 	 * request with username and password, this would be the username. Callers are
   91 | 	 * expected to populate the principal for an authentication request.
   92 | 	 * <p>
   93 | 	 * The <tt>AuthenticationManager</tt> implementation will often return an
   94 | 	 * <tt>Authentication</tt> containing richer information as the principal for use by
   95 | 	 * the application. Many of the authentication providers will create a
   96 | 	 * {@code UserDetails} object as the principal.
   97 | 	 * @return the <code>Principal</code> being authenticated or the authenticated
   98 | 	 * principal after authentication.
   99 | 	 */
  100 | 	@Nullable Object getPrincipal();
  101 | 
  102 | 	/**
  103 | 	 * Used to indicate to {@code AbstractSecurityInterceptor} whether it should present
  104 | 	 * the authentication token to the <code>AuthenticationManager</code>. Typically an
  105 | 	 * <code>AuthenticationManager</code> (or, more often, one of its
  106 | 	 * <code>AuthenticationProvider</code>s) will return an immutable authentication token
  107 | 	 * after successful authentication, in which case that token can safely return
  108 | 	 * <code>true</code> to this method. Returning <code>true</code> will improve
  109 | 	 * performance, as calling the <code>AuthenticationManager</code> for every request
  110 | 	 * will no longer be necessary.
  111 | 	 * <p>
  112 | 	 * For security reasons, implementations of this interface should be very careful
  113 | 	 * about returning <code>true</code> from this method unless they are either
  114 | 	 * immutable, or have some way of ensuring the properties have not been changed since
  115 | 	 * original creation.
  116 | 	 * @return true if the token has been authenticated and the
  117 | 	 * <code>AbstractSecurityInterceptor</code> does not need to present the token to the
  118 | 	 * <code>AuthenticationManager</code> again for re-authentication.
  119 | 	 */
  120 | 	boolean isAuthenticated();
  121 | 
  122 | 	/**
  123 | 	 * See {@link #isAuthenticated()} for a full description.
  124 | 	 * <p>
  125 | 	 * Implementations should <b>always</b> allow this method to be called with a
  126 | 	 * <code>false</code> parameter, as this is used by various classes to specify the
  127 | 	 * authentication token should not be trusted. If an implementation wishes to reject
  128 | 	 * an invocation with a <code>true</code> parameter (which would indicate the
  129 | 	 * authentication token is trusted - a potential security risk) the implementation
  130 | 	 * should throw an {@link IllegalArgumentException}.
  131 | 	 * @param isAuthenticated <code>true</code> if the token should be trusted (which may
  132 | 	 * result in an exception) or <code>false</code> if the token should not be trusted
  133 | 	 * @throws IllegalArgumentException if an attempt to make the authentication token
  134 | 	 * trusted (by passing <code>true</code> as the argument) is rejected due to the
  135 | 	 * implementation being immutable or implementing its own alternative approach to
  136 | 	 * {@link #isAuthenticated()}
  137 | 	 */
  138 | 	void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException;
  139 | 
  140 | 	/**
  141 | 	 * Return an {@link Builder} based on this instance. By default, returns a builder
  142 | 	 * that builds a {@link SimpleAuthentication}.
  143 | 	 * <p>
  144 | 	 * Although a {@code default} method, all {@link Authentication} implementations
  145 | 	 * should implement this. The reason is to ensure that the {@link Authentication} type
  146 | 	 * is preserved when {@link Builder#build} is invoked. This is especially important in
  147 | 	 * the event that your authentication implementation contains custom fields.
  148 | 	 * </p>
  149 | 	 * <p>
  150 | 	 * This isn't strictly necessary since it is recommended that applications code to the
  151 | 	 * {@link Authentication} interface and that custom information is often contained in
  152 | 	 * the {@link Authentication#getPrincipal} value.
  153 | 	 * </p>
  154 | 	 * @return an {@link Builder} for building a new {@link Authentication} based on this
  155 | 	 * instance
  156 | 	 * @since 7.0
  157 | 	 */
  158 | 	default Builder<?> toBuilder() {
  159 | 		return new SimpleAuthentication.Builder(this);
  160 | 	}
  161 | 
  162 | 	/**
  163 | 	 * A builder based on a given {@link Authentication} instance.
  164 | 	 *
  165 | 	 * @author Josh Cummings
  166 | 	 * @since 7.0
  167 | 	 */
  168 | 	interface Builder<B extends Builder<B>> {
  169 | 
  170 | 		/**
  171 | 		 * Mutate the authorities with this {@link Consumer}.
  172 | 		 * <p>
  173 | 		 * Note that since a non-empty set of authorities implies an
  174 | 		 * {@link Authentication} is authenticated, this method also marks the
  175 | 		 * authentication as {@link #authenticated} by default.
  176 | 		 * </p>
  177 | 		 * @param authorities a consumer that receives the full set of authorities
  178 | 		 * @return the {@link Builder} for additional configuration
  179 | 		 * @see Authentication#getAuthorities
  180 | 		 */
  181 | 		B authorities(Consumer<Collection<GrantedAuthority>> authorities);
  182 | 
  183 | 		/**
  184 | 		 * Use this credential.
  185 | 		 * <p>
  186 | 		 * Note that since some credentials are insecure to store, this method is
  187 | 		 * implemented as unsupported by default. Only implement or use this method if you
  188 | 		 * support secure storage of the credential or if your implementation also
  189 | 		 * implements {@link CredentialsContainer} and the credentials are thereby erased.
  190 | 		 * </p>
  191 | 		 * @param credentials the credentials to use
  192 | 		 * @return the {@link Builder} for additional configuration
  193 | 		 * @see Authentication#getCredentials
  194 | 		 */
  195 | 		default B credentials(@Nullable Object credentials) {
  196 | 			throw new UnsupportedOperationException(
  197 | 					String.format("%s does not store credentials", this.getClass().getSimpleName()));
  198 | 		}
  199 | 
  200 | 		/**
  201 | 		 * Use this details object.
  202 | 		 * <p>
  203 | 		 * Implementations may choose to use these {@code details} in combination with any
  204 | 		 * principal from the pre-existing {@link Authentication} instance.
  205 | 		 * </p>
  206 | 		 * @param details the details to use
  207 | 		 * @return the {@link Builder} for additional configuration
  208 | 		 * @see Authentication#getDetails
  209 | 		 */
  210 | 		B details(@Nullable Object details);
  211 | 
  212 | 		/**
  213 | 		 * Use this principal.
  214 | 		 * <p>
  215 | 		 * Note that in many cases, the principal is strongly-typed. Implementations may
  216 | 		 * choose to do a type check and are not necessarily expected to allow any object
  217 | 		 * as a principal.
  218 | 		 * </p>
  219 | 		 * <p>
  220 | 		 * Implementations may choose to use this {@code principal} in combination with
  221 | 		 * any principal from the pre-existing {@link Authentication} instance.
  222 | 		 * </p>
  223 | 		 * @param principal the principal to use
  224 | 		 * @return the {@link Builder} for additional configuration
  225 | 		 * @see Authentication#getPrincipal
  226 | 		 */
  227 | 		B principal(@Nullable Object principal);
  228 | 
  229 | 		/**
  230 | 		 * Mark this authentication as authenticated or not.
  231 | 		 * @param authenticated whether this is an authenticated {@link Authentication}
  232 | 		 * instance
  233 | 		 * @return the {@link Builder} for additional configuration
  234 | 		 * @see Authentication#isAuthenticated
  235 | 		 */
  236 | 		B authenticated(boolean authenticated);
  237 | 
  238 | 		/**
  239 | 		 * Build an {@link Authentication} instance.
  240 | 		 * @return the {@link Authentication} instance
  241 | 		 */
  242 | 		Authentication build();
  243 | 
  244 | 	}
  245 | 
  246 | }
```
