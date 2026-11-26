<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-web:7.2.0 | path: org/springframework/security/web/authentication/DelegatingAuthenticationEntryPoint.java -->
<!-- 正文引用：[[源码:DelegatingAuthenticationEntryPoint.java#L起-L止]] ；代码块头部：(src: DelegatingAuthenticationEntryPoint.java#L起-L止) -->

# DelegatingAuthenticationEntryPoint.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-web:7.2.0` |
| 路径 | `org/springframework/security/web/authentication/DelegatingAuthenticationEntryPoint.java` |
| 行数 | 234 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-web/7.2.0/spring-security-web-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:28:01.272Z |

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
   17 | package org.springframework.security.web.authentication;
   18 | 
   19 | import java.io.IOException;
   20 | import java.util.ArrayList;
   21 | import java.util.Arrays;
   22 | import java.util.LinkedHashMap;
   23 | import java.util.List;
   24 | import java.util.stream.Collectors;
   25 | 
   26 | import jakarta.servlet.ServletException;
   27 | import jakarta.servlet.http.HttpServletRequest;
   28 | import jakarta.servlet.http.HttpServletResponse;
   29 | import org.apache.commons.logging.Log;
   30 | import org.apache.commons.logging.LogFactory;
   31 | import org.jspecify.annotations.Nullable;
   32 | 
   33 | import org.springframework.beans.factory.InitializingBean;
   34 | import org.springframework.core.log.LogMessage;
   35 | import org.springframework.security.core.AuthenticationException;
   36 | import org.springframework.security.web.AuthenticationEntryPoint;
   37 | import org.springframework.security.web.util.matcher.ELRequestMatcher;
   38 | import org.springframework.security.web.util.matcher.RequestMatcher;
   39 | import org.springframework.security.web.util.matcher.RequestMatcherEditor;
   40 | import org.springframework.security.web.util.matcher.RequestMatcherEntry;
   41 | import org.springframework.util.Assert;
   42 | 
   43 | /**
   44 |  * An {@code AuthenticationEntryPoint} which selects a concrete
   45 |  * {@code AuthenticationEntryPoint} based on a {@link RequestMatcher} evaluation.
   46 |  *
   47 |  * <p>
   48 |  * A configuration might look like this:
   49 |  * </p>
   50 |  *
   51 |  * <pre>
   52 |  * &lt;bean id=&quot;daep&quot; class=&quot;org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint&quot;&gt;
   53 |  *     &lt;constructor-arg&gt;
   54 |  *         &lt;map&gt;
   55 |  *             &lt;entry key=&quot;hasIpAddress('192.168.1.0/24') and hasHeader('User-Agent','Mozilla')&quot; value-ref=&quot;firstAEP&quot; /&gt;
   56 |  *             &lt;entry key=&quot;hasHeader('User-Agent','MSIE')&quot; value-ref=&quot;secondAEP&quot; /&gt;
   57 |  *         &lt;/map&gt;
   58 |  *     &lt;/constructor-arg&gt;
   59 |  *     &lt;property name=&quot;defaultEntryPoint&quot; ref=&quot;defaultAEP&quot;/&gt;
   60 |  * &lt;/bean&gt;
   61 |  * </pre>
   62 |  *
   63 |  * This example uses the {@link RequestMatcherEditor} which creates a
   64 |  * {@link ELRequestMatcher} instances for the map keys.
   65 |  *
   66 |  * @author Mike Wiesner
   67 |  * @since 3.0.2
   68 |  */
   69 | public class DelegatingAuthenticationEntryPoint implements AuthenticationEntryPoint, InitializingBean {
   70 | 
   71 | 	private static final Log logger = LogFactory.getLog(DelegatingAuthenticationEntryPoint.class);
   72 | 
   73 | 	private final List<RequestMatcherEntry<AuthenticationEntryPoint>> entryPoints;
   74 | 
   75 | 	@SuppressWarnings("NullAway.Init")
   76 | 	private AuthenticationEntryPoint defaultEntryPoint;
   77 | 
   78 | 	/**
   79 | 	 * Creates a new instance with the provided mappings.
   80 | 	 * @param entryPoints the mapping of {@link RequestMatcher} to
   81 | 	 * {@link AuthenticationEntryPoint}. Cannot be null or empty.
   82 | 	 * @param defaultEntryPoint the default {@link AuthenticationEntryPoint}. Cannot be
   83 | 	 * null.
   84 | 	 */
   85 | 	public DelegatingAuthenticationEntryPoint(AuthenticationEntryPoint defaultEntryPoint,
   86 | 			RequestMatcherEntry<AuthenticationEntryPoint>... entryPoints) {
   87 | 		Assert.notEmpty(entryPoints, "entryPoints cannot be empty");
   88 | 		Assert.notNull(defaultEntryPoint, "defaultEntryPoint cannot be null");
   89 | 		this.entryPoints = Arrays.asList(entryPoints);
   90 | 		this.defaultEntryPoint = defaultEntryPoint;
   91 | 	}
   92 | 
   93 | 	/**
   94 | 	 * Creates a new instance with the provided mappings.
   95 | 	 * @param defaultEntryPoint the default {@link AuthenticationEntryPoint}. Cannot be
   96 | 	 * null.
   97 | 	 * @param entryPoints the mapping of {@link RequestMatcher} to
   98 | 	 * {@link AuthenticationEntryPoint}. Cannot be null or empty.
   99 | 	 */
  100 | 	public DelegatingAuthenticationEntryPoint(AuthenticationEntryPoint defaultEntryPoint,
  101 | 			List<RequestMatcherEntry<AuthenticationEntryPoint>> entryPoints) {
  102 | 		Assert.notEmpty(entryPoints, "entryPoints cannot be empty");
  103 | 		Assert.notNull(defaultEntryPoint, "defaultEntryPoint cannot be null");
  104 | 		this.entryPoints = entryPoints;
  105 | 		this.defaultEntryPoint = defaultEntryPoint;
  106 | 	}
  107 | 
  108 | 	/**
  109 | 	 * Creates a new instance.
  110 | 	 * @param entryPoints
  111 | 	 * @deprecated Use
  112 | 	 * {@link #DelegatingAuthenticationEntryPoint(AuthenticationEntryPoint, List)}
  113 | 	 */
  114 | 	@Deprecated(forRemoval = true)
  115 | 	public DelegatingAuthenticationEntryPoint(LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints) {
  116 | 		this.entryPoints = entryPoints.entrySet()
  117 | 			.stream()
  118 | 			.map((e) -> new RequestMatcherEntry<>(e.getKey(), e.getValue()))
  119 | 			.collect(Collectors.toList());
  120 | 	}
  121 | 
  122 | 	@Override
  123 | 	public void commence(HttpServletRequest request, HttpServletResponse response,
  124 | 			AuthenticationException authException) throws IOException, ServletException {
  125 | 		for (RequestMatcherEntry<AuthenticationEntryPoint> entry : this.entryPoints) {
  126 | 			RequestMatcher requestMatcher = entry.getRequestMatcher();
  127 | 			logger.debug(LogMessage.format("Trying to match using %s", requestMatcher));
  128 | 			if (requestMatcher.matches(request)) {
  129 | 				AuthenticationEntryPoint entryPoint = entry.getEntry();
  130 | 				logger.debug(LogMessage.format("Match found! Executing %s", entryPoint));
  131 | 				entryPoint.commence(request, response, authException);
  132 | 				return;
  133 | 			}
  134 | 		}
  135 | 		logger.debug(LogMessage.format("No match found. Using default entry point %s", this.defaultEntryPoint));
  136 | 		// No EntryPoint matched, use defaultEntryPoint
  137 | 		this.defaultEntryPoint.commence(request, response, authException);
  138 | 	}
  139 | 
  140 | 	/**
  141 | 	 * EntryPoint which is used when no RequestMatcher returned true.
  142 | 	 * @deprecated Use
  143 | 	 * {@link #DelegatingAuthenticationEntryPoint(AuthenticationEntryPoint, List)}
  144 | 	 */
  145 | 	@Deprecated(forRemoval = true)
  146 | 	public void setDefaultEntryPoint(AuthenticationEntryPoint defaultEntryPoint) {
  147 | 		this.defaultEntryPoint = defaultEntryPoint;
  148 | 	}
  149 | 
  150 | 	@Override
  151 | 	public void afterPropertiesSet() {
  152 | 		Assert.notEmpty(this.entryPoints, "entryPoints must be specified");
  153 | 		Assert.notNull(this.defaultEntryPoint, "defaultEntryPoint must be specified");
  154 | 	}
  155 | 
  156 | 	/**
  157 | 	 * Creates a new {@link Builder}.
  158 | 	 * @return the new {@link Builder}
  159 | 	 */
  160 | 	public static Builder builder() {
  161 | 		return new Builder();
  162 | 	}
  163 | 
  164 | 	/**
  165 | 	 * Used to build a new instance of {@link DelegatingAuthenticationEntryPoint}.
  166 | 	 *
  167 | 	 * @author Rob Winch
  168 | 	 * @since 7.0
  169 | 	 */
  170 | 	public static final class Builder {
  171 | 
  172 | 		private @Nullable AuthenticationEntryPoint defaultEntryPoint;
  173 | 
  174 | 		private List<RequestMatcherEntry<AuthenticationEntryPoint>> entryPoints = new ArrayList<RequestMatcherEntry<AuthenticationEntryPoint>>();
  175 | 
  176 | 		/**
  177 | 		 * Set the default {@link AuthenticationEntryPoint} if none match. The default is
  178 | 		 * to use the first {@link AuthenticationEntryPoint} added in
  179 | 		 * {@link #addEntryPointFor(AuthenticationEntryPoint, RequestMatcher)}.
  180 | 		 * @param defaultEntryPoint the default {@link AuthenticationEntryPoint} to use.
  181 | 		 * @return the {@link Builder} for further customization.
  182 | 		 */
  183 | 		public Builder defaultEntryPoint(@Nullable AuthenticationEntryPoint defaultEntryPoint) {
  184 | 			this.defaultEntryPoint = defaultEntryPoint;
  185 | 			return this;
  186 | 		}
  187 | 
  188 | 		/**
  189 | 		 * Adds an {@link AuthenticationEntryPoint} for the provided
  190 | 		 * {@link RequestMatcher}.
  191 | 		 * @param entryPoint the {@link AuthenticationEntryPoint} to use. Cannot be null.
  192 | 		 * @param requestMatcher the {@link RequestMatcher} to use. Cannot be null.
  193 | 		 * @return the {@link Builder} for further customization.
  194 | 		 */
  195 | 		public Builder addEntryPointFor(AuthenticationEntryPoint entryPoint, RequestMatcher requestMatcher) {
  196 | 			Assert.notNull(entryPoint, "entryPoint cannot be null");
  197 | 			Assert.notNull(requestMatcher, "requestMatcher cannot be null");
  198 | 			this.entryPoints.add(new RequestMatcherEntry<>(requestMatcher, entryPoint));
  199 | 			return this;
  200 | 		}
  201 | 
  202 | 		/**
  203 | 		 * Builds the {@link AuthenticationEntryPoint}. If the
  204 | 		 * {@link #defaultEntryPoint(AuthenticationEntryPoint)} is not set, then the first
  205 | 		 * {@link #addEntryPointFor(AuthenticationEntryPoint, RequestMatcher)} is used as
  206 | 		 * the default. If the {@link #defaultEntryPoint(AuthenticationEntryPoint)} is not
  207 | 		 * set and there is only a single
  208 | 		 * {@link #addEntryPointFor(AuthenticationEntryPoint, RequestMatcher)}, then the
  209 | 		 * {@link AuthenticationEntryPoint} is returned rather than wrapping it in
  210 | 		 * {@link DelegatingAuthenticationEntryPoint}.
  211 | 		 * @return the {@link AuthenticationEntryPoint} to use.
  212 | 		 */
  213 | 		public AuthenticationEntryPoint build() {
  214 | 			AuthenticationEntryPoint defaultEntryPoint = this.defaultEntryPoint;
  215 | 			if (defaultEntryPoint == null) {
  216 | 				Assert.state(!this.entryPoints.isEmpty(), "entryPoints cannot be empty if defaultEntryPoint is null");
  217 | 				AuthenticationEntryPoint firstAuthenticationEntryPoint = this.entryPoints.get(0).getEntry();
  218 | 				if (this.entryPoints.size() == 1) {
  219 | 					return firstAuthenticationEntryPoint;
  220 | 				}
  221 | 				defaultEntryPoint = firstAuthenticationEntryPoint;
  222 | 			}
  223 | 			else if (this.entryPoints.isEmpty()) {
  224 | 				return defaultEntryPoint;
  225 | 			}
  226 | 			return new DelegatingAuthenticationEntryPoint(defaultEntryPoint, this.entryPoints);
  227 | 		}
  228 | 
  229 | 		private Builder() {
  230 | 		}
  231 | 
  232 | 	}
  233 | 
  234 | }
```
