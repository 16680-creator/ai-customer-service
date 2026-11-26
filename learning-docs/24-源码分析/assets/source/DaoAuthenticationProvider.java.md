<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-core:7.2.0 | path: org/springframework/security/authentication/dao/DaoAuthenticationProvider.java -->
<!-- 正文引用：[[源码:DaoAuthenticationProvider.java#L起-L止]] ；代码块头部：(src: DaoAuthenticationProvider.java#L起-L止) -->

# DaoAuthenticationProvider.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-core:7.2.0` |
| 路径 | `org/springframework/security/authentication/dao/DaoAuthenticationProvider.java` |
| 行数 | 197 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-core/7.2.0/spring-security-core-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:44.817Z |

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
   19 | import java.util.Objects;
   20 | import java.util.function.Supplier;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.security.authentication.AuthenticationProvider;
   25 | import org.springframework.security.authentication.BadCredentialsException;
   26 | import org.springframework.security.authentication.InternalAuthenticationServiceException;
   27 | import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
   28 | import org.springframework.security.authentication.password.CompromisedPasswordChecker;
   29 | import org.springframework.security.authentication.password.CompromisedPasswordException;
   30 | import org.springframework.security.core.Authentication;
   31 | import org.springframework.security.core.AuthenticationException;
   32 | import org.springframework.security.core.userdetails.UserDetails;
   33 | import org.springframework.security.core.userdetails.UserDetailsPasswordService;
   34 | import org.springframework.security.core.userdetails.UserDetailsService;
   35 | import org.springframework.security.core.userdetails.UsernameNotFoundException;
   36 | import org.springframework.security.crypto.factory.PasswordEncoderFactories;
   37 | import org.springframework.security.crypto.password.PasswordEncoder;
   38 | import org.springframework.util.Assert;
   39 | import org.springframework.util.function.SingletonSupplier;
   40 | 
   41 | /**
   42 |  * An {@link AuthenticationProvider} implementation that retrieves user details from a
   43 |  * {@link UserDetailsService}.
   44 |  *
   45 |  * @author Ben Alex
   46 |  * @author Rob Winch
   47 |  * @author Andrey Litvitski
   48 |  */
   49 | public class DaoAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {
   50 | 
   51 | 	/**
   52 | 	 * The plaintext password used to perform
   53 | 	 * {@link PasswordEncoder#matches(CharSequence, String)} on when the user is not found
   54 | 	 * to avoid SEC-2056.
   55 | 	 */
   56 | 	private static final String USER_NOT_FOUND_PASSWORD = "userNotFoundPassword";
   57 | 
   58 | 	private Supplier<PasswordEncoder> passwordEncoder = SingletonSupplier
   59 | 		.of(PasswordEncoderFactories::createDelegatingPasswordEncoder);
   60 | 
   61 | 	/**
   62 | 	 * The password used to perform {@link PasswordEncoder#matches(CharSequence, String)}
   63 | 	 * on when the user is not found to avoid SEC-2056. This is necessary, because some
   64 | 	 * {@link PasswordEncoder} implementations will short circuit if the password is not
   65 | 	 * in a valid format.
   66 | 	 */
   67 | 	private volatile @Nullable String userNotFoundEncodedPassword;
   68 | 
   69 | 	private final UserDetailsService userDetailsService;
   70 | 
   71 | 	private UserDetailsPasswordService userDetailsPasswordService = UserDetailsPasswordService.NOOP;
   72 | 
   73 | 	private @Nullable CompromisedPasswordChecker compromisedPasswordChecker;
   74 | 
   75 | 	public DaoAuthenticationProvider(UserDetailsService userDetailsService) {
   76 | 		Assert.notNull(userDetailsService, "userDetailsService cannot be null");
   77 | 		this.userDetailsService = userDetailsService;
   78 | 	}
   79 | 
   80 | 	@Override
   81 | 	protected void additionalAuthenticationChecks(UserDetails userDetails,
   82 | 			UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
   83 | 		if (authentication.getCredentials() == null) {
   84 | 			this.logger.debug("Failed to authenticate since no credentials provided");
   85 | 			throw new BadCredentialsException(this.messages
   86 | 				.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
   87 | 		}
   88 | 		String presentedPassword = authentication.getCredentials().toString();
   89 | 		if (!this.passwordEncoder.get().matches(presentedPassword, userDetails.getPassword())) {
   90 | 			this.logger.debug("Failed to authenticate since password does not match stored value");
   91 | 			throw new BadCredentialsException(this.messages
   92 | 				.getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
   93 | 		}
   94 | 	}
   95 | 
   96 | 	@Override
   97 | 	protected void doAfterPropertiesSet() {
   98 | 		Assert.notNull(this.userDetailsService, "A UserDetailsService must be set");
   99 | 	}
  100 | 
  101 | 	@Override
  102 | 	protected final UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication)
  103 | 			throws AuthenticationException {
  104 | 		prepareTimingAttackProtection();
  105 | 		try {
  106 | 			UserDetails loadedUser = this.getUserDetailsService().loadUserByUsername(username);
  107 | 			if (loadedUser == null) {
  108 | 				throw new InternalAuthenticationServiceException(
  109 | 						"UserDetailsService returned null, which is an interface contract violation");
  110 | 			}
  111 | 			return loadedUser;
  112 | 		}
  113 | 		catch (UsernameNotFoundException ex) {
  114 | 			mitigateAgainstTimingAttack(authentication);
  115 | 			throw ex;
  116 | 		}
  117 | 		catch (InternalAuthenticationServiceException ex) {
  118 | 			throw ex;
  119 | 		}
  120 | 		catch (Exception ex) {
  121 | 			throw new InternalAuthenticationServiceException(ex.getMessage(), ex);
  122 | 		}
  123 | 	}
  124 | 
  125 | 	@Override
  126 | 	protected Authentication createSuccessAuthentication(Object principal, Authentication authentication,
  127 | 			UserDetails user) {
  128 | 		Assert.notNull(authentication.getCredentials(), "Authentication.getCredentials() cannot be null");
  129 | 		String presentedPassword = authentication.getCredentials().toString();
  130 | 		boolean isPasswordCompromised = this.compromisedPasswordChecker != null
  131 | 				&& this.compromisedPasswordChecker.check(presentedPassword).isCompromised();
  132 | 		if (isPasswordCompromised) {
  133 | 			throw new CompromisedPasswordException("The provided password is compromised, please change your password");
  134 | 		}
  135 | 		String existingEncodedPassword = user.getPassword();
  136 | 		boolean upgradeEncoding = existingEncodedPassword != null
  137 | 				&& !Objects.equals(this.userDetailsPasswordService, UserDetailsPasswordService.NOOP)
  138 | 				&& this.passwordEncoder.get().upgradeEncoding(existingEncodedPassword);
  139 | 		if (upgradeEncoding) {
  140 | 			String newPassword = this.passwordEncoder.get().encode(presentedPassword);
  141 | 			user = this.userDetailsPasswordService.updatePassword(user, newPassword);
  142 | 		}
  143 | 		return super.createSuccessAuthentication(principal, authentication, user);
  144 | 	}
  145 | 
  146 | 	private void prepareTimingAttackProtection() {
  147 | 		if (this.userNotFoundEncodedPassword == null) {
  148 | 			this.userNotFoundEncodedPassword = this.passwordEncoder.get().encode(USER_NOT_FOUND_PASSWORD);
  149 | 		}
  150 | 	}
  151 | 
  152 | 	private void mitigateAgainstTimingAttack(UsernamePasswordAuthenticationToken authentication) {
  153 | 		if (authentication.getCredentials() != null) {
  154 | 			Assert.notNull(this.userNotFoundEncodedPassword, "userNotFoundEncodedPassword cannot be null");
  155 | 			String presentedPassword = authentication.getCredentials().toString();
  156 | 			this.passwordEncoder.get().matches(presentedPassword, this.userNotFoundEncodedPassword);
  157 | 		}
  158 | 	}
  159 | 
  160 | 	/**
  161 | 	 * Sets the PasswordEncoder instance to be used to encode and validate passwords. If
  162 | 	 * not set, the password will be compared using
  163 | 	 * {@link PasswordEncoderFactories#createDelegatingPasswordEncoder()}
  164 | 	 * @param passwordEncoder must be an instance of one of the {@code PasswordEncoder}
  165 | 	 * types.
  166 | 	 */
  167 | 	public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
  168 | 		Assert.notNull(passwordEncoder, "passwordEncoder cannot be null");
  169 | 		this.passwordEncoder = () -> passwordEncoder;
  170 | 		this.userNotFoundEncodedPassword = null;
  171 | 	}
  172 | 
  173 | 	protected PasswordEncoder getPasswordEncoder() {
  174 | 		return this.passwordEncoder.get();
  175 | 	}
  176 | 
  177 | 	protected UserDetailsService getUserDetailsService() {
  178 | 		return this.userDetailsService;
  179 | 	}
  180 | 
  181 | 	public void setUserDetailsPasswordService(UserDetailsPasswordService userDetailsPasswordService) {
  182 | 		Assert.notNull(userDetailsPasswordService, "userDetailsPasswordService cannot be null");
  183 | 		this.userDetailsPasswordService = userDetailsPasswordService;
  184 | 	}
  185 | 
  186 | 	/**
  187 | 	 * Sets the {@link CompromisedPasswordChecker} to be used before creating a successful
  188 | 	 * authentication. Defaults to {@code null}.
  189 | 	 * @param compromisedPasswordChecker the {@link CompromisedPasswordChecker} to use
  190 | 	 * @since 6.3
  191 | 	 */
  192 | 	public void setCompromisedPasswordChecker(CompromisedPasswordChecker compromisedPasswordChecker) {
  193 | 		Assert.notNull(compromisedPasswordChecker, "compromisedPasswordChecker cannot be null");
  194 | 		this.compromisedPasswordChecker = compromisedPasswordChecker;
  195 | 	}
  196 | 
  197 | }
```
