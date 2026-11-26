<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/firewall/StrictHttpFirewall.java -->
<!-- 正文引用：[[源码:StrictHttpFirewall.java#L起-L止]] ；代码块头部：(src: StrictHttpFirewall.java#L起-L止) -->

# StrictHttpFirewall.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/firewall/StrictHttpFirewall.java` |
| 行数 | 868 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T17:02:22.864Z |

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
   17 | package org.springframework.security.web.firewall;
   18 | 
   19 | import java.util.Arrays;
   20 | import java.util.Collection;
   21 | import java.util.Collections;
   22 | import java.util.Enumeration;
   23 | import java.util.HashSet;
   24 | import java.util.List;
   25 | import java.util.Map;
   26 | import java.util.Set;
   27 | import java.util.function.Predicate;
   28 | import java.util.regex.Pattern;
   29 | 
   30 | import jakarta.servlet.http.HttpServletRequest;
   31 | import jakarta.servlet.http.HttpServletResponse;
   32 | 
   33 | import org.springframework.http.HttpMethod;
   34 | import org.springframework.util.Assert;
   35 | 
   36 | /**
   37 |  * <p>
   38 |  * A strict implementation of {@link HttpFirewall} that rejects any suspicious requests
   39 |  * with a {@link RequestRejectedException}.
   40 |  * </p>
   41 |  * <p>
   42 |  * The following rules are applied to the firewall:
   43 |  * </p>
   44 |  * <ul>
   45 |  * <li>Rejects HTTP methods that are not allowed. This specified to block
   46 |  * <a href="https://www.owasp.org/index.php/Test_HTTP_Methods_(OTG-CONFIG-006)">HTTP Verb
   47 |  * tampering and XST attacks</a>. See {@link #setAllowedHttpMethods(Collection)}</li>
   48 |  * <li>Rejects URLs that are not normalized to avoid bypassing security constraints. There
   49 |  * is no way to disable this as it is considered extremely risky to disable this
   50 |  * constraint. A few options to allow this behavior is to normalize the request prior to
   51 |  * the firewall or using {@link DefaultHttpFirewall} instead. Please keep in mind that
   52 |  * normalizing the request is fragile and why requests are rejected rather than
   53 |  * normalized.</li>
   54 |  * <li>Rejects URLs that contain characters that are not printable ASCII characters. There
   55 |  * is no way to disable this as it is considered extremely risky to disable this
   56 |  * constraint.</li>
   57 |  * <li>Rejects URLs that contain semicolons. See {@link #setAllowSemicolon(boolean)}</li>
   58 |  * <li>Rejects URLs that contain a URL encoded slash. See
   59 |  * {@link #setAllowUrlEncodedSlash(boolean)}</li>
   60 |  * <li>Rejects URLs that contain a backslash. See {@link #setAllowBackSlash(boolean)}</li>
   61 |  * <li>Rejects URLs that contain a null character. See {@link #setAllowNull(boolean)}</li>
   62 |  * <li>Rejects URLs that contain a URL encoded percent. See
   63 |  * {@link #setAllowUrlEncodedPercent(boolean)}</li>
   64 |  * <li>Rejects hosts that are not allowed. See {@link #setAllowedHostnames(Predicate)}
   65 |  * </li>
   66 |  * <li>Reject headers names that are not allowed. See
   67 |  * {@link #setAllowedHeaderNames(Predicate)}</li>
   68 |  * <li>Reject headers values that are not allowed. See
   69 |  * {@link #setAllowedHeaderValues(Predicate)}</li>
   70 |  * <li>Reject parameter names that are not allowed. See
   71 |  * {@link #setAllowedParameterNames(Predicate)}</li>
   72 |  * <li>Reject parameter values that are not allowed. See
   73 |  * {@link #setAllowedParameterValues(Predicate)}</li>
   74 |  * </ul>
   75 |  *
   76 |  * @author Rob Winch
   77 |  * @author Eddú Meléndez
   78 |  * @author Jinwoo Bae
   79 |  * @since 4.2.4
   80 |  * @see DefaultHttpFirewall
   81 |  */
   82 | public class StrictHttpFirewall implements HttpFirewall {
   83 | 
   84 | 	/**
   85 | 	 * Used to specify to {@link #setAllowedHttpMethods(Collection)} that any HTTP method
   86 | 	 * should be allowed.
   87 | 	 */
   88 | 	private static final Set<String> ALLOW_ANY_HTTP_METHOD = Collections.emptySet();
   89 | 
   90 | 	private static final String ENCODED_PERCENT = "%25";
   91 | 
   92 | 	private static final String PERCENT = "%";
   93 | 
   94 | 	private static final List<String> FORBIDDEN_ENCODED_PERIOD = Collections
   95 | 		.unmodifiableList(Arrays.asList("%2e", "%2E"));
   96 | 
   97 | 	private static final List<String> FORBIDDEN_SEMICOLON = Collections
   98 | 		.unmodifiableList(Arrays.asList(";", "%3b", "%3B"));
   99 | 
  100 | 	private static final List<String> FORBIDDEN_FORWARDSLASH = Collections
  101 | 		.unmodifiableList(Arrays.asList("%2f", "%2F"));
  102 | 
  103 | 	private static final List<String> FORBIDDEN_DOUBLE_FORWARDSLASH = Collections
  104 | 		.unmodifiableList(Arrays.asList("//", "%2f%2f", "%2f%2F", "%2F%2f", "%2F%2F"));
  105 | 
  106 | 	private static final List<String> FORBIDDEN_BACKSLASH = Collections
  107 | 		.unmodifiableList(Arrays.asList("\\", "%5c", "%5C"));
  108 | 
  109 | 	private static final List<String> FORBIDDEN_NULL = Collections.unmodifiableList(Arrays.asList("\0", "%00"));
  110 | 
  111 | 	private static final List<String> FORBIDDEN_LF = Collections.unmodifiableList(Arrays.asList("\n", "%0a", "%0A"));
  112 | 
  113 | 	private static final List<String> FORBIDDEN_CR = Collections.unmodifiableList(Arrays.asList("\r", "%0d", "%0D"));
  114 | 
  115 | 	private static final List<String> FORBIDDEN_LINE_SEPARATOR = Collections.unmodifiableList(Arrays.asList("\u2028"));
  116 | 
  117 | 	private static final List<String> FORBIDDEN_PARAGRAPH_SEPARATOR = Collections
  118 | 		.unmodifiableList(Arrays.asList("\u2029"));
  119 | 
  120 | 	private Set<String> encodedUrlBlocklist = new HashSet<>();
  121 | 
  122 | 	private Set<String> decodedUrlBlocklist = new HashSet<>();
  123 | 
  124 | 	private Set<String> allowedHttpMethods = createDefaultAllowedHttpMethods();
  125 | 
  126 | 	private Predicate<String> allowedHostnames = (hostname) -> true;
  127 | 
  128 | 	private static final Pattern ASSIGNED_AND_NOT_ISO_CONTROL_PATTERN = Pattern
  129 | 		.compile("[\\p{IsAssigned}&&[^\\p{IsControl}]]*");
  130 | 
  131 | 	private static final Predicate<String> ASSIGNED_AND_NOT_ISO_CONTROL_PREDICATE = (
  132 | 			s) -> ASSIGNED_AND_NOT_ISO_CONTROL_PATTERN.matcher(s).matches();
  133 | 
  134 | 	private static final Pattern HEADER_VALUE_PATTERN = Pattern.compile("[\\p{IsAssigned}&&[[^\\p{IsControl}]||\\t]]*");
  135 | 
  136 | 	private static final Predicate<String> HEADER_VALUE_PREDICATE = (s) -> HEADER_VALUE_PATTERN.matcher(s).matches();
  137 | 
  138 | 	private Predicate<String> allowedHeaderNames = ALLOWED_HEADER_NAMES;
  139 | 
  140 | 	public static final Predicate<String> ALLOWED_HEADER_NAMES = ASSIGNED_AND_NOT_ISO_CONTROL_PREDICATE;
  141 | 
  142 | 	private Predicate<String> allowedHeaderValues = ALLOWED_HEADER_VALUES;
  143 | 
  144 | 	public static final Predicate<String> ALLOWED_HEADER_VALUES = HEADER_VALUE_PREDICATE;
  145 | 
  146 | 	private Predicate<String> allowedParameterNames = ALLOWED_PARAMETER_NAMES;
  147 | 
  148 | 	public static final Predicate<String> ALLOWED_PARAMETER_NAMES = ASSIGNED_AND_NOT_ISO_CONTROL_PREDICATE;
  149 | 
  150 | 	private Predicate<String> allowedParameterValues = ALLOWED_PARAMETER_VALUES;
  151 | 
  152 | 	public static final Predicate<String> ALLOWED_PARAMETER_VALUES = (value) -> true;
  153 | 
  154 | 	public StrictHttpFirewall() {
  155 | 		urlBlocklistsAddAll(FORBIDDEN_SEMICOLON);
  156 | 		urlBlocklistsAddAll(FORBIDDEN_FORWARDSLASH);
  157 | 		urlBlocklistsAddAll(FORBIDDEN_DOUBLE_FORWARDSLASH);
  158 | 		urlBlocklistsAddAll(FORBIDDEN_BACKSLASH);
  159 | 		urlBlocklistsAddAll(FORBIDDEN_NULL);
  160 | 		urlBlocklistsAddAll(FORBIDDEN_LF);
  161 | 		urlBlocklistsAddAll(FORBIDDEN_CR);
  162 | 
  163 | 		this.encodedUrlBlocklist.add(ENCODED_PERCENT);
  164 | 		this.encodedUrlBlocklist.addAll(FORBIDDEN_ENCODED_PERIOD);
  165 | 		this.decodedUrlBlocklist.add(PERCENT);
  166 | 		this.decodedUrlBlocklist.addAll(FORBIDDEN_LINE_SEPARATOR);
  167 | 		this.decodedUrlBlocklist.addAll(FORBIDDEN_PARAGRAPH_SEPARATOR);
  168 | 	}
  169 | 
  170 | 	/**
  171 | 	 * Sets if any HTTP method is allowed. If this set to true, then no validation on the
  172 | 	 * HTTP method will be performed. This can open the application up to
  173 | 	 * <a href="https://www.owasp.org/index.php/Test_HTTP_Methods_(OTG-CONFIG-006)"> HTTP
  174 | 	 * Verb tampering and XST attacks</a>
  175 | 	 * @param unsafeAllowAnyHttpMethod if true, disables HTTP method validation, else
  176 | 	 * resets back to the defaults. Default is false.
  177 | 	 * @since 5.1
  178 | 	 * @see #setAllowedHttpMethods(Collection)
  179 | 	 */
  180 | 	public void setUnsafeAllowAnyHttpMethod(boolean unsafeAllowAnyHttpMethod) {
  181 | 		this.allowedHttpMethods = unsafeAllowAnyHttpMethod ? ALLOW_ANY_HTTP_METHOD : createDefaultAllowedHttpMethods();
  182 | 	}
  183 | 
  184 | 	/**
  185 | 	 * <p>
  186 | 	 * Determines which HTTP methods should be allowed. The default is to allow "DELETE",
  187 | 	 * "GET", "HEAD", "OPTIONS", "PATCH", "POST", and "PUT".
  188 | 	 * </p>
  189 | 	 * @param allowedHttpMethods the case-sensitive collection of HTTP methods that are
  190 | 	 * allowed.
  191 | 	 * @since 5.1
  192 | 	 * @see #setUnsafeAllowAnyHttpMethod(boolean)
  193 | 	 */
  194 | 	public void setAllowedHttpMethods(Collection<String> allowedHttpMethods) {
  195 | 		Assert.notNull(allowedHttpMethods, "allowedHttpMethods cannot be null");
  196 | 		this.allowedHttpMethods = (allowedHttpMethods != ALLOW_ANY_HTTP_METHOD) ? new HashSet<>(allowedHttpMethods)
  197 | 				: ALLOW_ANY_HTTP_METHOD;
  198 | 	}
  199 | 
  200 | 	/**
  201 | 	 * <p>
  202 | 	 * Determines if semicolon is allowed in the URL (i.e. matrix variables). The default
  203 | 	 * is to disable this behavior because it is a common way of attempting to perform
  204 | 	 * <a href="https://www.owasp.org/index.php/Reflected_File_Download">Reflected File
  205 | 	 * Download Attacks</a>. It is also the source of many exploits which bypass URL based
  206 | 	 * security.
  207 | 	 * </p>
  208 | 	 * <p>
  209 | 	 * For example, the following CVEs are a subset of the issues related to ambiguities
  210 | 	 * in the Servlet Specification on how to treat semicolons that led to CVEs:
  211 | 	 * </p>
  212 | 	 * <ul>
  213 | 	 * <li><a href="https://pivotal.io/security/cve-2016-5007">cve-2016-5007</a></li>
  214 | 	 * <li><a href="https://pivotal.io/security/cve-2016-9879">cve-2016-9879</a></li>
  215 | 	 * <li><a href="https://pivotal.io/security/cve-2018-1199">cve-2018-1199</a></li>
  216 | 	 * </ul>
  217 | 	 *
  218 | 	 * <p>
  219 | 	 * If you are wanting to allow semicolons, please reconsider as it is a very common
  220 | 	 * source of security bypasses. A few common reasons users want semicolons and
  221 | 	 * alternatives are listed below:
  222 | 	 * </p>
  223 | 	 * <ul>
  224 | 	 * <li>Including the JSESSIONID in the path - You should not include session id (or
  225 | 	 * any sensitive information) in a URL as it can lead to leaking. Instead use Cookies.
  226 | 	 * </li>
  227 | 	 * <li>Matrix Variables - Users wanting to leverage Matrix Variables should consider
  228 | 	 * using HTTP parameters instead.</li>
  229 | 	 * </ul>
  230 | 	 * @param allowSemicolon should semicolons be allowed in the URL. Default is false
  231 | 	 */
  232 | 	public void setAllowSemicolon(boolean allowSemicolon) {
  233 | 		if (allowSemicolon) {
  234 | 			urlBlocklistsRemoveAll(FORBIDDEN_SEMICOLON);
  235 | 		}
  236 | 		else {
  237 | 			urlBlocklistsAddAll(FORBIDDEN_SEMICOLON);
  238 | 		}
  239 | 	}
  240 | 
  241 | 	/**
  242 | 	 * <p>
  243 | 	 * Determines if a slash "/" that is URL encoded "%2F" should be allowed in the path
  244 | 	 * or not. The default is to not allow this behavior because it is a common way to
  245 | 	 * bypass URL based security.
  246 | 	 * </p>
  247 | 	 * <p>
  248 | 	 * For example, due to ambiguities in the servlet specification, the value is not
  249 | 	 * parsed consistently which results in different values in {@code HttpServletRequest}
  250 | 	 * path related values which allow bypassing certain security constraints.
  251 | 	 * </p>
  252 | 	 * @param allowUrlEncodedSlash should a slash "/" that is URL encoded "%2F" be allowed
  253 | 	 * in the path or not. Default is false.
  254 | 	 */
  255 | 	public void setAllowUrlEncodedSlash(boolean allowUrlEncodedSlash) {
  256 | 		if (allowUrlEncodedSlash) {
  257 | 			urlBlocklistsRemoveAll(FORBIDDEN_FORWARDSLASH);
  258 | 		}
  259 | 		else {
  260 | 			urlBlocklistsAddAll(FORBIDDEN_FORWARDSLASH);
  261 | 		}
  262 | 	}
  263 | 
  264 | 	/**
  265 | 	 * <p>
  266 | 	 * Determines if double slash "//" that is URL encoded "%2F%2F" should be allowed in
  267 | 	 * the path or not. The default is to not allow.
  268 | 	 * </p>
  269 | 	 * @param allowUrlEncodedDoubleSlash should a slash "//" that is URL encoded "%2F%2F"
  270 | 	 * be allowed in the path or not. Default is false.
  271 | 	 */
  272 | 	public void setAllowUrlEncodedDoubleSlash(boolean allowUrlEncodedDoubleSlash) {
  273 | 		if (allowUrlEncodedDoubleSlash) {
  274 | 			urlBlocklistsRemoveAll(FORBIDDEN_DOUBLE_FORWARDSLASH);
  275 | 		}
  276 | 		else {
  277 | 			urlBlocklistsAddAll(FORBIDDEN_DOUBLE_FORWARDSLASH);
  278 | 		}
  279 | 	}
  280 | 
  281 | 	/**
  282 | 	 * <p>
  283 | 	 * Determines if a period "." that is URL encoded "%2E" should be allowed in the path
  284 | 	 * or not. The default is to not allow this behavior because it is a frequent source
  285 | 	 * of security exploits.
  286 | 	 * </p>
  287 | 	 * <p>
  288 | 	 * For example, due to ambiguities in the servlet specification a URL encoded period
  289 | 	 * might lead to bypassing security constraints through a directory traversal attack.
  290 | 	 * This is because the path is not parsed consistently which results in different
  291 | 	 * values in {@code HttpServletRequest} path related values which allow bypassing
  292 | 	 * certain security constraints.
  293 | 	 * </p>
  294 | 	 * @param allowUrlEncodedPeriod should a period "." that is URL encoded "%2E" be
  295 | 	 * allowed in the path or not. Default is false.
  296 | 	 */
  297 | 	public void setAllowUrlEncodedPeriod(boolean allowUrlEncodedPeriod) {
  298 | 		if (allowUrlEncodedPeriod) {
  299 | 			this.encodedUrlBlocklist.removeAll(FORBIDDEN_ENCODED_PERIOD);
  300 | 		}
  301 | 		else {
  302 | 			this.encodedUrlBlocklist.addAll(FORBIDDEN_ENCODED_PERIOD);
  303 | 		}
  304 | 	}
  305 | 
  306 | 	/**
  307 | 	 * <p>
  308 | 	 * Determines if a backslash "\" or a URL encoded backslash "%5C" should be allowed in
  309 | 	 * the path or not. The default is not to allow this behavior because it is a frequent
  310 | 	 * source of security exploits.
  311 | 	 * </p>
  312 | 	 * <p>
  313 | 	 * For example, due to ambiguities in the servlet specification a URL encoded period
  314 | 	 * might lead to bypassing security constraints through a directory traversal attack.
  315 | 	 * This is because the path is not parsed consistently which results in different
  316 | 	 * values in {@code HttpServletRequest} path related values which allow bypassing
  317 | 	 * certain security constraints.
  318 | 	 * </p>
  319 | 	 * @param allowBackSlash a backslash "\" or a URL encoded backslash "%5C" be allowed
  320 | 	 * in the path or not. Default is false
  321 | 	 */
  322 | 	public void setAllowBackSlash(boolean allowBackSlash) {
  323 | 		if (allowBackSlash) {
  324 | 			urlBlocklistsRemoveAll(FORBIDDEN_BACKSLASH);
  325 | 		}
  326 | 		else {
  327 | 			urlBlocklistsAddAll(FORBIDDEN_BACKSLASH);
  328 | 		}
  329 | 	}
  330 | 
  331 | 	/**
  332 | 	 * <p>
  333 | 	 * Determines if a null "\0" or a URL encoded nul "%00" should be allowed in the path
  334 | 	 * or not. The default is not to allow this behavior because it is a frequent source
  335 | 	 * of security exploits.
  336 | 	 * </p>
  337 | 	 * @param allowNull a null "\0" or a URL encoded null "%00" be allowed in the path or
  338 | 	 * not. Default is false
  339 | 	 * @since 5.4
  340 | 	 */
  341 | 	public void setAllowNull(boolean allowNull) {
  342 | 		if (allowNull) {
  343 | 			urlBlocklistsRemoveAll(FORBIDDEN_NULL);
  344 | 		}
  345 | 		else {
  346 | 			urlBlocklistsAddAll(FORBIDDEN_NULL);
  347 | 		}
  348 | 	}
  349 | 
  350 | 	/**
  351 | 	 * <p>
  352 | 	 * Determines if a percent "%" that is URL encoded "%25" should be allowed in the path
  353 | 	 * or not. The default is not to allow this behavior because it is a frequent source
  354 | 	 * of security exploits.
  355 | 	 * </p>
  356 | 	 * <p>
  357 | 	 * For example, this can lead to exploits that involve double URL encoding that lead
  358 | 	 * to bypassing security constraints.
  359 | 	 * </p>
  360 | 	 * @param allowUrlEncodedPercent if a percent "%" that is URL encoded "%25" should be
  361 | 	 * allowed in the path or not. Default is false
  362 | 	 */
  363 | 	public void setAllowUrlEncodedPercent(boolean allowUrlEncodedPercent) {
  364 | 		if (allowUrlEncodedPercent) {
  365 | 			this.encodedUrlBlocklist.remove(ENCODED_PERCENT);
  366 | 			this.decodedUrlBlocklist.remove(PERCENT);
  367 | 		}
  368 | 		else {
  369 | 			this.encodedUrlBlocklist.add(ENCODED_PERCENT);
  370 | 			this.decodedUrlBlocklist.add(PERCENT);
  371 | 		}
  372 | 	}
  373 | 
  374 | 	/**
  375 | 	 * Determines if a URL encoded Carriage Return is allowed in the path or not. The
  376 | 	 * default is not to allow this behavior because it is a frequent source of security
  377 | 	 * exploits.
  378 | 	 * @param allowUrlEncodedCarriageReturn if URL encoded Carriage Return is allowed in
  379 | 	 * the URL or not. Default is false.
  380 | 	 */
  381 | 	public void setAllowUrlEncodedCarriageReturn(boolean allowUrlEncodedCarriageReturn) {
  382 | 		if (allowUrlEncodedCarriageReturn) {
  383 | 			urlBlocklistsRemoveAll(FORBIDDEN_CR);
  384 | 		}
  385 | 		else {
  386 | 			urlBlocklistsAddAll(FORBIDDEN_CR);
  387 | 		}
  388 | 	}
  389 | 
  390 | 	/**
  391 | 	 * Determines if a URL encoded Line Feed is allowed in the path or not. The default is
  392 | 	 * not to allow this behavior because it is a frequent source of security exploits.
  393 | 	 * @param allowUrlEncodedLineFeed if URL encoded Line Feed is allowed in the URL or
  394 | 	 * not. Default is false.
  395 | 	 */
  396 | 	public void setAllowUrlEncodedLineFeed(boolean allowUrlEncodedLineFeed) {
  397 | 		if (allowUrlEncodedLineFeed) {
  398 | 			urlBlocklistsRemoveAll(FORBIDDEN_LF);
  399 | 		}
  400 | 		else {
  401 | 			urlBlocklistsAddAll(FORBIDDEN_LF);
  402 | 		}
  403 | 	}
  404 | 
  405 | 	/**
  406 | 	 * Determines if a URL encoded paragraph separator is allowed in the path or not. The
  407 | 	 * default is not to allow this behavior because it is a frequent source of security
  408 | 	 * exploits.
  409 | 	 * @param allowUrlEncodedParagraphSeparator if URL encoded paragraph separator is
  410 | 	 * allowed in the URL or not. Default is false.
  411 | 	 */
  412 | 	public void setAllowUrlEncodedParagraphSeparator(boolean allowUrlEncodedParagraphSeparator) {
  413 | 		if (allowUrlEncodedParagraphSeparator) {
  414 | 			this.decodedUrlBlocklist.removeAll(FORBIDDEN_PARAGRAPH_SEPARATOR);
  415 | 		}
  416 | 		else {
  417 | 			this.decodedUrlBlocklist.addAll(FORBIDDEN_PARAGRAPH_SEPARATOR);
  418 | 		}
  419 | 	}
  420 | 
  421 | 	/**
  422 | 	 * Determines if a URL encoded line separator is allowed in the path or not. The
  423 | 	 * default is not to allow this behavior because it is a frequent source of security
  424 | 	 * exploits.
  425 | 	 * @param allowUrlEncodedLineSeparator if URL encoded line separator is allowed in the
  426 | 	 * URL or not. Default is false.
  427 | 	 */
  428 | 	public void setAllowUrlEncodedLineSeparator(boolean allowUrlEncodedLineSeparator) {
  429 | 		if (allowUrlEncodedLineSeparator) {
  430 | 			this.decodedUrlBlocklist.removeAll(FORBIDDEN_LINE_SEPARATOR);
  431 | 		}
  432 | 		else {
  433 | 			this.decodedUrlBlocklist.addAll(FORBIDDEN_LINE_SEPARATOR);
  434 | 		}
  435 | 	}
  436 | 
  437 | 	/**
  438 | 	 * <p>
  439 | 	 * Determines which header names should be allowed. The default is to reject header
  440 | 	 * names that contain ISO control characters and characters that are not defined.
  441 | 	 * </p>
  442 | 	 * @param allowedHeaderNames the predicate for testing header names
  443 | 	 * @since 5.4
  444 | 	 * @see Character#isISOControl(int)
  445 | 	 * @see Character#isDefined(int)
  446 | 	 */
  447 | 	public void setAllowedHeaderNames(Predicate<String> allowedHeaderNames) {
  448 | 		Assert.notNull(allowedHeaderNames, "allowedHeaderNames cannot be null");
  449 | 		this.allowedHeaderNames = allowedHeaderNames;
  450 | 	}
  451 | 
  452 | 	/**
  453 | 	 * <p>
  454 | 	 * Determines which header values should be allowed. The default is to reject header
  455 | 	 * values that contain ISO control characters and characters that are not defined.
  456 | 	 * </p>
  457 | 	 * @param allowedHeaderValues the predicate for testing hostnames
  458 | 	 * @since 5.4
  459 | 	 * @see Character#isISOControl(int)
  460 | 	 * @see Character#isDefined(int)
  461 | 	 */
  462 | 	public void setAllowedHeaderValues(Predicate<String> allowedHeaderValues) {
  463 | 		Assert.notNull(allowedHeaderValues, "allowedHeaderValues cannot be null");
  464 | 		this.allowedHeaderValues = allowedHeaderValues;
  465 | 	}
  466 | 
  467 | 	/**
  468 | 	 * Determines which parameter names should be allowed. The default is to reject header
  469 | 	 * names that contain ISO control characters and characters that are not defined.
  470 | 	 * @param allowedParameterNames the predicate for testing parameter names
  471 | 	 * @since 5.4
  472 | 	 * @see Character#isISOControl(int)
  473 | 	 * @see Character#isDefined(int)
  474 | 	 */
  475 | 	public void setAllowedParameterNames(Predicate<String> allowedParameterNames) {
  476 | 		Assert.notNull(allowedParameterNames, "allowedParameterNames cannot be null");
  477 | 		this.allowedParameterNames = allowedParameterNames;
  478 | 	}
  479 | 
  480 | 	/**
  481 | 	 * <p>
  482 | 	 * Determines which parameter values should be allowed. The default is to allow any
  483 | 	 * parameter value.
  484 | 	 * </p>
  485 | 	 * @param allowedParameterValues the predicate for testing parameter values
  486 | 	 * @since 5.4
  487 | 	 */
  488 | 	public void setAllowedParameterValues(Predicate<String> allowedParameterValues) {
  489 | 		Assert.notNull(allowedParameterValues, "allowedParameterValues cannot be null");
  490 | 		this.allowedParameterValues = allowedParameterValues;
  491 | 	}
  492 | 
  493 | 	/**
  494 | 	 * <p>
  495 | 	 * Determines which hostnames should be allowed. The default is to allow any hostname.
  496 | 	 * </p>
  497 | 	 * @param allowedHostnames the predicate for testing hostnames
  498 | 	 * @since 5.2
  499 | 	 */
  500 | 	public void setAllowedHostnames(Predicate<String> allowedHostnames) {
  501 | 		Assert.notNull(allowedHostnames, "allowedHostnames cannot be null");
  502 | 		this.allowedHostnames = allowedHostnames;
  503 | 	}
  504 | 
  505 | 	private void urlBlocklistsAddAll(Collection<String> values) {
  506 | 		this.encodedUrlBlocklist.addAll(values);
  507 | 		this.decodedUrlBlocklist.addAll(values);
  508 | 	}
  509 | 
  510 | 	private void urlBlocklistsRemoveAll(Collection<String> values) {
  511 | 		this.encodedUrlBlocklist.removeAll(values);
  512 | 		this.decodedUrlBlocklist.removeAll(values);
  513 | 	}
  514 | 
  515 | 	@Override
  516 | 	public FirewalledRequest getFirewalledRequest(HttpServletRequest request) throws RequestRejectedException {
  517 | 		rejectForbiddenHttpMethod(request);
  518 | 		rejectedBlocklistedUrls(request);
  519 | 		rejectedUntrustedHosts(request);
  520 | 		if (!isNormalized(request)) {
  521 | 			throw new RequestRejectedException("The request was rejected because the URL was not normalized.");
  522 | 		}
  523 | 		rejectNonPrintableAsciiCharactersInFieldName(request.getRequestURI(), "requestURI");
  524 | 		return new StrictFirewalledRequest(request);
  525 | 	}
  526 | 
  527 | 	private void rejectNonPrintableAsciiCharactersInFieldName(String toCheck, String propertyName) {
  528 | 		if (!containsOnlyPrintableAsciiCharacters(toCheck)) {
  529 | 			throw new RequestRejectedException(String
  530 | 				.format("The %s was rejected because it can only contain printable ASCII characters.", propertyName));
  531 | 		}
  532 | 	}
  533 | 
  534 | 	private void rejectForbiddenHttpMethod(HttpServletRequest request) {
  535 | 		if (this.allowedHttpMethods == ALLOW_ANY_HTTP_METHOD) {
  536 | 			return;
  537 | 		}
  538 | 		if (!this.allowedHttpMethods.contains(request.getMethod())) {
  539 | 			throw new RequestRejectedException(
  540 | 					"The request was rejected because the HTTP method \"" + request.getMethod()
  541 | 							+ "\" was not included within the list of allowed HTTP methods " + this.allowedHttpMethods);
  542 | 		}
  543 | 	}
  544 | 
  545 | 	private void rejectedBlocklistedUrls(HttpServletRequest request) {
  546 | 		for (String forbidden : this.encodedUrlBlocklist) {
  547 | 			if (encodedUrlContains(request, forbidden)) {
  548 | 				throw new RequestRejectedException(
  549 | 						"The request was rejected because the URL contained a potentially malicious String \""
  550 | 								+ forbidden + "\"");
  551 | 			}
  552 | 		}
  553 | 		for (String forbidden : this.decodedUrlBlocklist) {
  554 | 			if (decodedUrlContains(request, forbidden)) {
  555 | 				throw new RequestRejectedException(
  556 | 						"The request was rejected because the URL contained a potentially malicious String \""
  557 | 								+ forbidden + "\"");
  558 | 			}
  559 | 		}
  560 | 	}
  561 | 
  562 | 	private void rejectedUntrustedHosts(HttpServletRequest request) {
  563 | 		String serverName = request.getServerName();
  564 | 		if (serverName != null && !this.allowedHostnames.test(serverName)) {
  565 | 			throw new RequestRejectedException(
  566 | 					"The request was rejected because the domain " + serverName + " is untrusted.");
  567 | 		}
  568 | 	}
  569 | 
  570 | 	@Override
  571 | 	public HttpServletResponse getFirewalledResponse(HttpServletResponse response) {
  572 | 		return new FirewalledResponse(response);
  573 | 	}
  574 | 
  575 | 	private static Set<String> createDefaultAllowedHttpMethods() {
  576 | 		Set<String> result = new HashSet<>();
  577 | 		result.add(HttpMethod.DELETE.name());
  578 | 		result.add(HttpMethod.GET.name());
  579 | 		result.add(HttpMethod.HEAD.name());
  580 | 		result.add(HttpMethod.OPTIONS.name());
  581 | 		result.add(HttpMethod.PATCH.name());
  582 | 		result.add(HttpMethod.POST.name());
  583 | 		result.add(HttpMethod.PUT.name());
  584 | 		return result;
  585 | 	}
  586 | 
  587 | 	private static boolean isNormalized(HttpServletRequest request) {
  588 | 		if (!isNormalized(request.getRequestURI())) {
  589 | 			return false;
  590 | 		}
  591 | 		if (!isNormalized(request.getContextPath())) {
  592 | 			return false;
  593 | 		}
  594 | 		if (!isNormalized(request.getServletPath())) {
  595 | 			return false;
  596 | 		}
  597 | 		if (!isNormalized(request.getPathInfo())) {
  598 | 			return false;
  599 | 		}
  600 | 		return true;
  601 | 	}
  602 | 
  603 | 	private static boolean encodedUrlContains(HttpServletRequest request, String value) {
  604 | 		if (valueContains(request.getContextPath(), value)) {
  605 | 			return true;
  606 | 		}
  607 | 		return valueContains(request.getRequestURI(), value);
  608 | 	}
  609 | 
  610 | 	private static boolean decodedUrlContains(HttpServletRequest request, String value) {
  611 | 		if (valueContains(request.getServletPath(), value)) {
  612 | 			return true;
  613 | 		}
  614 | 		return valueContains(request.getPathInfo(), value);
  615 | 	}
  616 | 
  617 | 	private static boolean containsOnlyPrintableAsciiCharacters(String uri) {
  618 | 		if (uri == null) {
  619 | 			return true;
  620 | 		}
  621 | 		int length = uri.length();
  622 | 		for (int i = 0; i < length; i++) {
  623 | 			char ch = uri.charAt(i);
  624 | 			if (ch < '\u0020' || ch > '\u007e') {
  625 | 				return false;
  626 | 			}
  627 | 		}
  628 | 		return true;
  629 | 	}
  630 | 
  631 | 	private static boolean valueContains(String value, String contains) {
  632 | 		return value != null && value.contains(contains);
  633 | 	}
  634 | 
  635 | 	/**
  636 | 	 * Checks whether a path is normalized (doesn't contain path traversal sequences like
  637 | 	 * "./", "/../" or "/.")
  638 | 	 * @param path the path to test
  639 | 	 * @return true if the path doesn't contain any path-traversal character sequences.
  640 | 	 */
  641 | 	private static boolean isNormalized(String path) {
  642 | 		if (path == null) {
  643 | 			return true;
  644 | 		}
  645 | 		for (int i = path.length(); i > 0;) {
  646 | 			int slashIndex = path.lastIndexOf('/', i - 1);
  647 | 			int gap = i - slashIndex;
  648 | 			if (gap == 2 && path.charAt(slashIndex + 1) == '.') {
  649 | 				return false; // ".", "/./" or "/."
  650 | 			}
  651 | 			if (gap == 3 && path.charAt(slashIndex + 1) == '.' && path.charAt(slashIndex + 2) == '.') {
  652 | 				return false;
  653 | 			}
  654 | 			i = slashIndex;
  655 | 		}
  656 | 		return true;
  657 | 	}
  658 | 
  659 | 	/**
  660 | 	 * Provides the existing encoded url blocklist which can add/remove entries from.
  661 | 	 * @return the existing encoded url blocklist, never null
  662 | 	 */
  663 | 	public Set<String> getEncodedUrlBlocklist() {
  664 | 		return this.encodedUrlBlocklist;
  665 | 	}
  666 | 
  667 | 	/**
  668 | 	 * Provides the existing decoded url blocklist which can add/remove entries from.
  669 | 	 * @return the existing decoded url blocklist, never null
  670 | 	 */
  671 | 	public Set<String> getDecodedUrlBlocklist() {
  672 | 		return this.decodedUrlBlocklist;
  673 | 	}
  674 | 
  675 | 	/**
  676 | 	 * Provides the existing encoded url blocklist which can add/remove entries from.
  677 | 	 * @return the existing encoded url blocklist, never null
  678 | 	 * @deprecated Use {@link #getEncodedUrlBlocklist()} instead
  679 | 	 */
  680 | 	@Deprecated
  681 | 	public Set<String> getEncodedUrlBlacklist() {
  682 | 		return getEncodedUrlBlocklist();
  683 | 	}
  684 | 
  685 | 	/**
  686 | 	 * Provides the existing decoded url blocklist which can add/remove entries from.
  687 | 	 * @return the existing decoded url blocklist, never null
  688 | 	 *
  689 | 	 */
  690 | 	public Set<String> getDecodedUrlBlacklist() {
  691 | 		return getDecodedUrlBlocklist();
  692 | 	}
  693 | 
  694 | 	/**
  695 | 	 * Strict {@link FirewalledRequest}.
  696 | 	 */
  697 | 	private class StrictFirewalledRequest extends FirewalledRequest {
  698 | 
  699 | 		StrictFirewalledRequest(HttpServletRequest request) {
  700 | 			super(request);
  701 | 		}
  702 | 
  703 | 		@Override
  704 | 		public long getDateHeader(String name) {
  705 | 			if (name != null) {
  706 | 				validateAllowedHeaderName(name);
  707 | 			}
  708 | 			return super.getDateHeader(name);
  709 | 		}
  710 | 
  711 | 		@Override
  712 | 		public int getIntHeader(String name) {
  713 | 			if (name != null) {
  714 | 				validateAllowedHeaderName(name);
  715 | 			}
  716 | 			return super.getIntHeader(name);
  717 | 		}
  718 | 
  719 | 		@Override
  720 | 		public String getHeader(String name) {
  721 | 			if (name != null) {
  722 | 				validateAllowedHeaderName(name);
  723 | 			}
  724 | 			String value = super.getHeader(name);
  725 | 			if (value != null) {
  726 | 				validateAllowedHeaderValue(name, value);
  727 | 			}
  728 | 			return value;
  729 | 		}
  730 | 
  731 | 		@Override
  732 | 		public Enumeration<String> getHeaders(String name) {
  733 | 			if (name != null) {
  734 | 				validateAllowedHeaderName(name);
  735 | 			}
  736 | 			Enumeration<String> headers = super.getHeaders(name);
  737 | 			return new Enumeration<>() {
  738 | 
  739 | 				@Override
  740 | 				public boolean hasMoreElements() {
  741 | 					return headers.hasMoreElements();
  742 | 				}
  743 | 
  744 | 				@Override
  745 | 				public String nextElement() {
  746 | 					String value = headers.nextElement();
  747 | 					validateAllowedHeaderValue(name, value);
  748 | 					return value;
  749 | 				}
  750 | 
  751 | 			};
  752 | 		}
  753 | 
  754 | 		@Override
  755 | 		public Enumeration<String> getHeaderNames() {
  756 | 			Enumeration<String> names = super.getHeaderNames();
  757 | 			return new Enumeration<>() {
  758 | 
  759 | 				@Override
  760 | 				public boolean hasMoreElements() {
  761 | 					return names.hasMoreElements();
  762 | 				}
  763 | 
  764 | 				@Override
  765 | 				public String nextElement() {
  766 | 					String headerNames = names.nextElement();
  767 | 					validateAllowedHeaderName(headerNames);
  768 | 					return headerNames;
  769 | 				}
  770 | 
  771 | 			};
  772 | 		}
  773 | 
  774 | 		@Override
  775 | 		public String getParameter(String name) {
  776 | 			if (name != null) {
  777 | 				validateAllowedParameterName(name);
  778 | 			}
  779 | 			String value = super.getParameter(name);
  780 | 			if (value != null) {
  781 | 				validateAllowedParameterValue(name, value);
  782 | 			}
  783 | 			return value;
  784 | 		}
  785 | 
  786 | 		@Override
  787 | 		public Map<String, String[]> getParameterMap() {
  788 | 			Map<String, String[]> parameterMap = super.getParameterMap();
  789 | 			for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
  790 | 				String name = entry.getKey();
  791 | 				String[] values = entry.getValue();
  792 | 				validateAllowedParameterName(name);
  793 | 				for (String value : values) {
  794 | 					validateAllowedParameterValue(name, value);
  795 | 				}
  796 | 			}
  797 | 			return parameterMap;
  798 | 		}
  799 | 
  800 | 		@Override
  801 | 		public Enumeration<String> getParameterNames() {
  802 | 			Enumeration<String> parameterNames = super.getParameterNames();
  803 | 			return new Enumeration<>() {
  804 | 
  805 | 				@Override
  806 | 				public boolean hasMoreElements() {
  807 | 					return parameterNames.hasMoreElements();
  808 | 				}
  809 | 
  810 | 				@Override
  811 | 				public String nextElement() {
  812 | 					String name = parameterNames.nextElement();
  813 | 					validateAllowedParameterName(name);
  814 | 					return name;
  815 | 				}
  816 | 
  817 | 			};
  818 | 		}
  819 | 
  820 | 		@Override
  821 | 		public String[] getParameterValues(String name) {
  822 | 			if (name != null) {
  823 | 				validateAllowedParameterName(name);
  824 | 			}
  825 | 			String[] values = super.getParameterValues(name);
  826 | 			if (values != null) {
  827 | 				for (String value : values) {
  828 | 					validateAllowedParameterValue(name, value);
  829 | 				}
  830 | 			}
  831 | 			return values;
  832 | 		}
  833 | 
  834 | 		private void validateAllowedHeaderName(String headerNames) {
  835 | 			if (!StrictHttpFirewall.this.allowedHeaderNames.test(headerNames)) {
  836 | 				throw new RequestRejectedException(
  837 | 						"The request was rejected because the header name \"" + headerNames + "\" is not allowed.");
  838 | 			}
  839 | 		}
  840 | 
  841 | 		private void validateAllowedHeaderValue(String name, String value) {
  842 | 			if (!StrictHttpFirewall.this.allowedHeaderValues.test(value)) {
  843 | 				throw new RequestRejectedException("The request was rejected because the header: \"" + name
  844 | 						+ " \" has a value \"" + value + "\" that is not allowed.");
  845 | 			}
  846 | 		}
  847 | 
  848 | 		private void validateAllowedParameterName(String name) {
  849 | 			if (!StrictHttpFirewall.this.allowedParameterNames.test(name)) {
  850 | 				throw new RequestRejectedException(
  851 | 						"The request was rejected because the parameter name \"" + name + "\" is not allowed.");
  852 | 			}
  853 | 		}
  854 | 
  855 | 		private void validateAllowedParameterValue(String name, String value) {
  856 | 			if (!StrictHttpFirewall.this.allowedParameterValues.test(value)) {
  857 | 				throw new RequestRejectedException("The request was rejected because the parameter: \"" + name
  858 | 						+ " \" has a value \"" + value + "\" that is not allowed.");
  859 | 			}
  860 | 		}
  861 | 
  862 | 		@Override
  863 | 		public void reset() {
  864 | 		}
  865 | 
  866 | 	};
  867 | 
  868 | }
```
