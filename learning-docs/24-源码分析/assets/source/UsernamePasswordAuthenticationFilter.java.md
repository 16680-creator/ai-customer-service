<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/authentication/UsernamePasswordAuthenticationFilter.java -->
<!-- 正文引用：[[源码:UsernamePasswordAuthenticationFilter.java#L起-L止]] ；代码块头部：(src: UsernamePasswordAuthenticationFilter.java#L起-L止) -->

# UsernamePasswordAuthenticationFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/authentication/UsernamePasswordAuthenticationFilter.java` |
| 行数 | 170 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:28.011Z |

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
   17 | package org.springframework.security.web.authentication;
   18 | 
   19 | import jakarta.servlet.http.HttpServletRequest;
   20 | import jakarta.servlet.http.HttpServletResponse;
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.http.HttpMethod;
   24 | import org.springframework.security.authentication.AuthenticationManager;
   25 | import org.springframework.security.authentication.AuthenticationServiceException;
   26 | import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
   27 | import org.springframework.security.core.Authentication;
   28 | import org.springframework.security.core.AuthenticationException;
   29 | import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
   30 | import org.springframework.security.web.util.matcher.RequestMatcher;
   31 | import org.springframework.util.Assert;
   32 | 
   33 | /**
   34 |  * Processes an authentication form submission. Called
   35 |  * {@code AuthenticationProcessingFilter} prior to Spring Security 3.0.
   36 |  * <p>
   37 |  * Login forms must present two parameters to this filter: a username and password. The
   38 |  * default parameter names to use are contained in the static fields
   39 |  * {@link #SPRING_SECURITY_FORM_USERNAME_KEY} and
   40 |  * {@link #SPRING_SECURITY_FORM_PASSWORD_KEY}. The parameter names can also be changed by
   41 |  * setting the {@code usernameParameter} and {@code passwordParameter} properties.
   42 |  * <p>
   43 |  * This filter by default responds to the URL {@code /login}.
   44 |  *
   45 |  * @author Ben Alex
   46 |  * @author Colin Sampaleanu
   47 |  * @author Luke Taylor
   48 |  * @since 3.0
   49 |  */
   50 | public class UsernamePasswordAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
   51 | 
   52 | 	public static final String SPRING_SECURITY_FORM_USERNAME_KEY = "username";
   53 | 
   54 | 	public static final String SPRING_SECURITY_FORM_PASSWORD_KEY = "password";
   55 | 
   56 | 	private static final RequestMatcher DEFAULT_PATH_REQUEST_MATCHER = PathPatternRequestMatcher.withDefaults()
   57 | 		.matcher(HttpMethod.POST, "/login");
   58 | 
   59 | 	private String usernameParameter = SPRING_SECURITY_FORM_USERNAME_KEY;
   60 | 
   61 | 	private String passwordParameter = SPRING_SECURITY_FORM_PASSWORD_KEY;
   62 | 
   63 | 	private boolean postOnly = true;
   64 | 
   65 | 	public UsernamePasswordAuthenticationFilter() {
   66 | 		super(DEFAULT_PATH_REQUEST_MATCHER);
   67 | 	}
   68 | 
   69 | 	public UsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager) {
   70 | 		super(DEFAULT_PATH_REQUEST_MATCHER, authenticationManager);
   71 | 	}
   72 | 
   73 | 	@Override
   74 | 	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
   75 | 			throws AuthenticationException {
   76 | 		if (this.postOnly && !request.getMethod().equals("POST")) {
   77 | 			throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
   78 | 		}
   79 | 		String username = obtainUsername(request);
   80 | 		username = (username != null) ? username.trim() : "";
   81 | 		String password = obtainPassword(request);
   82 | 		password = (password != null) ? password : "";
   83 | 		UsernamePasswordAuthenticationToken authRequest = UsernamePasswordAuthenticationToken.unauthenticated(username,
   84 | 				password);
   85 | 		// Allow subclasses to set the "details" property
   86 | 		setDetails(request, authRequest);
   87 | 		return this.getAuthenticationManager().authenticate(authRequest);
   88 | 	}
   89 | 
   90 | 	/**
   91 | 	 * Enables subclasses to override the composition of the password, such as by
   92 | 	 * including additional values and a separator.
   93 | 	 * <p>
   94 | 	 * This might be used for example if a postcode/zipcode was required in addition to
   95 | 	 * the password. A delimiter such as a pipe (|) should be used to separate the
   96 | 	 * password and extended value(s). The <code>AuthenticationDao</code> will need to
   97 | 	 * generate the expected password in a corresponding manner.
   98 | 	 * </p>
   99 | 	 * @param request so that request attributes can be retrieved
  100 | 	 * @return the password that will be presented in the <code>Authentication</code>
  101 | 	 * request token to the <code>AuthenticationManager</code>
  102 | 	 */
  103 | 	protected @Nullable String obtainPassword(HttpServletRequest request) {
  104 | 		return request.getParameter(this.passwordParameter);
  105 | 	}
  106 | 
  107 | 	/**
  108 | 	 * Enables subclasses to override the composition of the username, such as by
  109 | 	 * including additional values and a separator.
  110 | 	 * @param request so that request attributes can be retrieved
  111 | 	 * @return the username that will be presented in the <code>Authentication</code>
  112 | 	 * request token to the <code>AuthenticationManager</code>
  113 | 	 */
  114 | 	protected @Nullable String obtainUsername(HttpServletRequest request) {
  115 | 		return request.getParameter(this.usernameParameter);
  116 | 	}
  117 | 
  118 | 	/**
  119 | 	 * Provided so that subclasses may configure what is put into the authentication
  120 | 	 * request's details property.
  121 | 	 * @param request that an authentication request is being created for
  122 | 	 * @param authRequest the authentication request object that should have its details
  123 | 	 * set
  124 | 	 */
  125 | 	protected void setDetails(HttpServletRequest request, UsernamePasswordAuthenticationToken authRequest) {
  126 | 		authRequest.setDetails(this.authenticationDetailsSource.buildDetails(request));
  127 | 	}
  128 | 
  129 | 	/**
  130 | 	 * Sets the parameter name which will be used to obtain the username from the login
  131 | 	 * request.
  132 | 	 * @param usernameParameter the parameter name. Defaults to "username".
  133 | 	 */
  134 | 	public void setUsernameParameter(String usernameParameter) {
  135 | 		Assert.hasText(usernameParameter, "Username parameter must not be empty or null");
  136 | 		this.usernameParameter = usernameParameter;
  137 | 	}
  138 | 
  139 | 	/**
  140 | 	 * Sets the parameter name which will be used to obtain the password from the login
  141 | 	 * request.
  142 | 	 * @param passwordParameter the parameter name. Defaults to "password".
  143 | 	 */
  144 | 	public void setPasswordParameter(String passwordParameter) {
  145 | 		Assert.hasText(passwordParameter, "Password parameter must not be empty or null");
  146 | 		this.passwordParameter = passwordParameter;
  147 | 	}
  148 | 
  149 | 	/**
  150 | 	 * Defines whether only HTTP POST requests will be allowed by this filter. If set to
  151 | 	 * true, and an authentication request is received which is not a POST request, an
  152 | 	 * exception will be raised immediately and authentication will not be attempted. The
  153 | 	 * <tt>unsuccessfulAuthentication()</tt> method will be called as if handling a failed
  154 | 	 * authentication.
  155 | 	 * <p>
  156 | 	 * Defaults to <tt>true</tt> but may be overridden by subclasses.
  157 | 	 */
  158 | 	public void setPostOnly(boolean postOnly) {
  159 | 		this.postOnly = postOnly;
  160 | 	}
  161 | 
  162 | 	public final String getUsernameParameter() {
  163 | 		return this.usernameParameter;
  164 | 	}
  165 | 
  166 | 	public final String getPasswordParameter() {
  167 | 		return this.passwordParameter;
  168 | 	}
  169 | 
  170 | }
```
