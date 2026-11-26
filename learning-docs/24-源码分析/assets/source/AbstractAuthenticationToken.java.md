<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authentication/AbstractAuthenticationToken.java -->
<!-- 正文引用：[[源码:AbstractAuthenticationToken.java#L起-L止]] ；代码块头部：(src: AbstractAuthenticationToken.java#L起-L止) -->

# AbstractAuthenticationToken.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authentication/AbstractAuthenticationToken.java` |
| 行数 | 244 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:44.696Z |

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
   17 | package org.springframework.security.authentication;
   18 | 
   19 | import java.io.Serial;
   20 | import java.security.Principal;
   21 | import java.util.ArrayList;
   22 | import java.util.Collection;
   23 | import java.util.Collections;
   24 | import java.util.LinkedHashSet;
   25 | import java.util.function.Consumer;
   26 | 
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.security.core.AuthenticatedPrincipal;
   30 | import org.springframework.security.core.Authentication;
   31 | import org.springframework.security.core.CredentialsContainer;
   32 | import org.springframework.security.core.GrantedAuthority;
   33 | import org.springframework.security.core.authority.AuthorityUtils;
   34 | import org.springframework.security.core.userdetails.UserDetails;
   35 | import org.springframework.util.Assert;
   36 | 
   37 | /**
   38 |  * Base class for <code>Authentication</code> objects.
   39 |  * <p>
   40 |  * Implementations which use this class should be immutable.
   41 |  *
   42 |  * @author Ben Alex
   43 |  * @author Luke Taylor
   44 |  */
   45 | public abstract class AbstractAuthenticationToken implements Authentication, CredentialsContainer {
   46 | 
   47 | 	@Serial
   48 | 	private static final long serialVersionUID = -3194696462184782834L;
   49 | 
   50 | 	private final Collection<GrantedAuthority> authorities;
   51 | 
   52 | 	private @Nullable Object details;
   53 | 
   54 | 	private boolean authenticated = false;
   55 | 
   56 | 	/**
   57 | 	 * Creates a token with the supplied array of authorities.
   58 | 	 * @param authorities the collection of <tt>GrantedAuthority</tt>s for the principal
   59 | 	 * represented by this authentication object.
   60 | 	 */
   61 | 	public AbstractAuthenticationToken(@Nullable Collection<? extends GrantedAuthority> authorities) {
   62 | 		if (authorities == null) {
   63 | 			this.authorities = AuthorityUtils.NO_AUTHORITIES;
   64 | 			return;
   65 | 		}
   66 | 		for (GrantedAuthority a : authorities) {
   67 | 			Assert.notNull(a, "Authorities collection cannot contain any null elements");
   68 | 		}
   69 | 		this.authorities = Collections.unmodifiableList(new ArrayList<>(authorities));
   70 | 	}
   71 | 
   72 | 	protected AbstractAuthenticationToken(AbstractAuthenticationBuilder<?> builder) {
   73 | 		this(builder.authorities);
   74 | 		this.authenticated = builder.authenticated;
   75 | 		this.details = builder.details;
   76 | 	}
   77 | 
   78 | 	@Override
   79 | 	public Collection<GrantedAuthority> getAuthorities() {
   80 | 		return this.authorities;
   81 | 	}
   82 | 
   83 | 	@Override
   84 | 	public String getName() {
   85 | 		if (this.getPrincipal() instanceof UserDetails userDetails) {
   86 | 			return userDetails.getUsername();
   87 | 		}
   88 | 		if (this.getPrincipal() instanceof AuthenticatedPrincipal authenticatedPrincipal) {
   89 | 			return authenticatedPrincipal.getName();
   90 | 		}
   91 | 		if (this.getPrincipal() instanceof Principal principal) {
   92 | 			return principal.getName();
   93 | 		}
   94 | 		return (this.getPrincipal() == null) ? "" : this.getPrincipal().toString();
   95 | 	}
   96 | 
   97 | 	@Override
   98 | 	public boolean isAuthenticated() {
   99 | 		return this.authenticated;
  100 | 	}
  101 | 
  102 | 	@Override
  103 | 	public void setAuthenticated(boolean authenticated) {
  104 | 		this.authenticated = authenticated;
  105 | 	}
  106 | 
  107 | 	@Override
  108 | 	public @Nullable Object getDetails() {
  109 | 		return this.details;
  110 | 	}
  111 | 
  112 | 	public void setDetails(@Nullable Object details) {
  113 | 		this.details = details;
  114 | 	}
  115 | 
  116 | 	/**
  117 | 	 * Checks the {@code credentials}, {@code principal} and {@code details} objects,
  118 | 	 * invoking the {@code eraseCredentials} method on any which implement
  119 | 	 * {@link CredentialsContainer}.
  120 | 	 */
  121 | 	@Override
  122 | 	public void eraseCredentials() {
  123 | 		eraseSecret(getCredentials());
  124 | 		eraseSecret(getPrincipal());
  125 | 		eraseSecret(this.details);
  126 | 	}
  127 | 
  128 | 	private void eraseSecret(@Nullable Object secret) {
  129 | 		if (secret instanceof CredentialsContainer container) {
  130 | 			container.eraseCredentials();
  131 | 		}
  132 | 	}
  133 | 
  134 | 	@Override
  135 | 	public boolean equals(@Nullable Object obj) {
  136 | 		if (!(obj instanceof AbstractAuthenticationToken test)) {
  137 | 			return false;
  138 | 		}
  139 | 		if (!this.authorities.equals(test.authorities)) {
  140 | 			return false;
  141 | 		}
  142 | 		if ((this.details == null) && (test.getDetails() != null)) {
  143 | 			return false;
  144 | 		}
  145 | 		if ((this.details != null) && (test.getDetails() == null)) {
  146 | 			return false;
  147 | 		}
  148 | 		if ((this.details != null) && (!this.details.equals(test.getDetails()))) {
  149 | 			return false;
  150 | 		}
  151 | 		if ((this.getCredentials() == null) && (test.getCredentials() != null)) {
  152 | 			return false;
  153 | 		}
  154 | 		if ((this.getCredentials() != null) && !this.getCredentials().equals(test.getCredentials())) {
  155 | 			return false;
  156 | 		}
  157 | 		if (this.getPrincipal() == null && test.getPrincipal() != null) {
  158 | 			return false;
  159 | 		}
  160 | 		if (this.getPrincipal() != null && !this.getPrincipal().equals(test.getPrincipal())) {
  161 | 			return false;
  162 | 		}
  163 | 		return this.isAuthenticated() == test.isAuthenticated();
  164 | 	}
  165 | 
  166 | 	@Override
  167 | 	public int hashCode() {
  168 | 		int code = 31;
  169 | 		for (GrantedAuthority authority : this.authorities) {
  170 | 			code ^= authority.hashCode();
  171 | 		}
  172 | 		if (this.getPrincipal() != null) {
  173 | 			code ^= this.getPrincipal().hashCode();
  174 | 		}
  175 | 		if (this.getCredentials() != null) {
  176 | 			code ^= this.getCredentials().hashCode();
  177 | 		}
  178 | 		if (this.getDetails() != null) {
  179 | 			code ^= this.getDetails().hashCode();
  180 | 		}
  181 | 		if (this.isAuthenticated()) {
  182 | 			code ^= -37;
  183 | 		}
  184 | 		return code;
  185 | 	}
  186 | 
  187 | 	@Override
  188 | 	public String toString() {
  189 | 		StringBuilder sb = new StringBuilder();
  190 | 		sb.append(getClass().getSimpleName()).append(" [");
  191 | 		sb.append("Principal=").append(getPrincipal()).append(", ");
  192 | 		sb.append("Credentials=[PROTECTED], ");
  193 | 		sb.append("Authenticated=").append(isAuthenticated()).append(", ");
  194 | 		sb.append("Details=").append(getDetails()).append(", ");
  195 | 		sb.append("Granted Authorities=").append(this.authorities);
  196 | 		sb.append("]");
  197 | 		return sb.toString();
  198 | 	}
  199 | 
  200 | 	/**
  201 | 	 * A common abstract implementation of {@link Authentication.Builder}. It implements
  202 | 	 * the builder methods that correspond to the {@link Authentication} methods that
  203 | 	 * {@link AbstractAuthenticationToken} implements
  204 | 	 *
  205 | 	 * @param <B>
  206 | 	 * @since 7.0
  207 | 	 */
  208 | 	protected abstract static class AbstractAuthenticationBuilder<B extends AbstractAuthenticationBuilder<B>>
  209 | 			implements Authentication.Builder<B> {
  210 | 
  211 | 		private boolean authenticated;
  212 | 
  213 | 		private @Nullable Object details;
  214 | 
  215 | 		private final Collection<GrantedAuthority> authorities;
  216 | 
  217 | 		protected AbstractAuthenticationBuilder(AbstractAuthenticationToken token) {
  218 | 			this.authorities = new LinkedHashSet<>(token.getAuthorities());
  219 | 			this.authenticated = token.isAuthenticated();
  220 | 			this.details = token.getDetails();
  221 | 		}
  222 | 
  223 | 		@Override
  224 | 		public B authenticated(boolean authenticated) {
  225 | 			this.authenticated = authenticated;
  226 | 			return (B) this;
  227 | 		}
  228 | 
  229 | 		@Override
  230 | 		public B details(@Nullable Object details) {
  231 | 			this.details = details;
  232 | 			return (B) this;
  233 | 		}
  234 | 
  235 | 		@Override
  236 | 		public B authorities(Consumer<Collection<GrantedAuthority>> authorities) {
  237 | 			authorities.accept(this.authorities);
  238 | 			this.authenticated = true;
  239 | 			return (B) this;
  240 | 		}
  241 | 
  242 | 	}
  243 | 
  244 | }
```
