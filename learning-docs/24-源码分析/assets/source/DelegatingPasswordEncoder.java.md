<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.security:spring-security-crypto:7.2.0 | path: org/springframework/security/crypto/password/DelegatingPasswordEncoder.java -->
<!-- 正文引用：[[源码:DelegatingPasswordEncoder.java#L起-L止]] ；代码块头部：(src: DelegatingPasswordEncoder.java#L起-L止) -->

# DelegatingPasswordEncoder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.security:spring-security-crypto:7.2.0` |
| 路径 | `org/springframework/security/crypto/password/DelegatingPasswordEncoder.java` |
| 行数 | 313 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/security/spring-security-crypto/7.2.0/spring-security-crypto-7.2.0-sources.jar |
| 抓取时间 | 2026-11-25T16:27:44.939Z |

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
   17 | package org.springframework.security.crypto.password;
   18 | 
   19 | import java.util.HashMap;
   20 | import java.util.Map;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | /**
   25 |  * A password encoder that delegates to another PasswordEncoder based upon a prefixed
   26 |  * identifier.
   27 |  *
   28 |  * <h2>Constructing an instance</h2>
   29 |  *
   30 |  * You can easily construct an instance using
   31 |  * {@link org.springframework.security.crypto.factory.PasswordEncoderFactories}.
   32 |  * Alternatively, you may create your own custom instance. For example:
   33 |  *
   34 |  * <pre>
   35 |  * String idForEncode = "bcrypt";
   36 |  * Map&lt;String,PasswordEncoder&gt; encoders = new HashMap&lt;&gt;();
   37 |  * encoders.put(idForEncode, new BCryptPasswordEncoder());
   38 |  * encoders.put("noop", NoOpPasswordEncoder.getInstance());
   39 |  * encoders.put("pbkdf2", new Pbkdf2PasswordEncoder());
   40 |  * encoders.put("scrypt", new SCryptPasswordEncoder());
   41 |  * encoders.put("sha256", new StandardPasswordEncoder());
   42 |  *
   43 |  * PasswordEncoder passwordEncoder = new DelegatingPasswordEncoder(idForEncode, encoders);
   44 |  * </pre>
   45 |  *
   46 |  *
   47 |  * <h2>Password Storage Format</h2>
   48 |  *
   49 |  * The general format for a password is:
   50 |  *
   51 |  * <pre>
   52 |  * {id}encodedPassword
   53 |  * </pre>
   54 |  *
   55 |  * Such that "id" is an identifier used to look up which {@link PasswordEncoder} should be
   56 |  * used and "encodedPassword" is the original encoded password for the selected
   57 |  * {@link PasswordEncoder}. The "id" must be at the beginning of the password, start with
   58 |  * "{" (id prefix) and end with "}" (id suffix). Both id prefix and id suffix can be
   59 |  * customized via {@link #DelegatingPasswordEncoder(String, Map, String, String)}. If the
   60 |  * "id" cannot be found, the "id" will be null.
   61 |  *
   62 |  * For example, the following might be a list of passwords encoded using different "id".
   63 |  * All of the original passwords are "password".
   64 |  *
   65 |  * <pre>
   66 |  * {bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
   67 |  * {noop}password
   68 |  * {pbkdf2}5d923b44a6d129f3ddf3e3c8d29412723dcbde72445e8ef6bf3b508fbf17fa4ed4d6b99ca763d8dc
   69 |  * {scrypt}$e0801$8bWJaSu2IKSn9Z9kM+TPXfOc/9bdYSrN1oD9qfVThWEwdRTnO7re7Ei+fUZRJ68k9lTyuTeUp4of4g24hHnazw==$OAOec05+bXxvuu/1qZ6NUR+xQYvYv7BeL1QxwRpY5Pc=
   70 |  * {sha256}97cde38028ad898ebc02e690819fa220e88c62e0699403e94fff291cfffaf8410849f27605abcbc0
   71 |  * </pre>
   72 |  *
   73 |  * For the DelegatingPasswordEncoder that we constructed above:
   74 |  *
   75 |  * <ol>
   76 |  * <li>The first password would have a {@code PasswordEncoder} id of "bcrypt" and
   77 |  * encodedPassword of "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG". When
   78 |  * matching it would delegate to
   79 |  * {@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder}</li>
   80 |  * <li>The second password would have a {@code PasswordEncoder} id of "noop" and
   81 |  * encodedPassword of "password". When matching it would delegate to
   82 |  * {@link NoOpPasswordEncoder}</li>
   83 |  * <li>The third password would have a {@code PasswordEncoder} id of "pbkdf2" and
   84 |  * encodedPassword of
   85 |  * "5d923b44a6d129f3ddf3e3c8d29412723dcbde72445e8ef6bf3b508fbf17fa4ed4d6b99ca763d8dc".
   86 |  * When matching it would delegate to {@link Pbkdf2PasswordEncoder}</li>
   87 |  * <li>The fourth password would have a {@code PasswordEncoder} id of "scrypt" and
   88 |  * encodedPassword of
   89 |  * "$e0801$8bWJaSu2IKSn9Z9kM+TPXfOc/9bdYSrN1oD9qfVThWEwdRTnO7re7Ei+fUZRJ68k9lTyuTeUp4of4g24hHnazw==$OAOec05+bXxvuu/1qZ6NUR+xQYvYv7BeL1QxwRpY5Pc="
   90 |  * When matching it would delegate to
   91 |  * {@link org.springframework.security.crypto.scrypt.SCryptPasswordEncoder}</li>
   92 |  * <li>The final password would have a {@code PasswordEncoder} id of "sha256" and
   93 |  * encodedPassword of
   94 |  * "97cde38028ad898ebc02e690819fa220e88c62e0699403e94fff291cfffaf8410849f27605abcbc0".
   95 |  * When matching it would delegate to {@link StandardPasswordEncoder}</li>
   96 |  * </ol>
   97 |  *
   98 |  * <h2>Password Encoding</h2>
   99 |  *
  100 |  * The {@code idForEncode} passed into the constructor determines which
  101 |  * {@link PasswordEncoder} will be used for encoding passwords. In the
  102 |  * {@code DelegatingPasswordEncoder} we constructed above, that means that the result of
  103 |  * encoding "password" would be delegated to {@code BCryptPasswordEncoder} and be prefixed
  104 |  * with "{bcrypt}". The end result would look like:
  105 |  *
  106 |  * <pre>
  107 |  * {bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
  108 |  * </pre>
  109 |  *
  110 |  * <h2>Password Matching</h2>
  111 |  *
  112 |  * Matching is done based upon the "id" and the mapping of the "id" to the
  113 |  * {@link PasswordEncoder} provided in the constructor. Our example in "Password Storage
  114 |  * Format" provides a working example of how this is done.
  115 |  *
  116 |  * By default the result of invoking {@link #matches(CharSequence, String)} with a
  117 |  * password with an "id" that is not mapped (including a null id) will result in an
  118 |  * {@link IllegalArgumentException}. This behavior can be customized using
  119 |  * {@link #setDefaultPasswordEncoderForMatches(PasswordEncoder)}.
  120 |  *
  121 |  * @author Rob Winch
  122 |  * @author Michael Simons
  123 |  * @author heowc
  124 |  * @author Jihoon Cha
  125 |  * @since 5.0
  126 |  * @see org.springframework.security.crypto.factory.PasswordEncoderFactories
  127 |  */
  128 | public class DelegatingPasswordEncoder extends AbstractValidatingPasswordEncoder {
  129 | 
  130 | 	private static final String DEFAULT_ID_PREFIX = "{";
  131 | 
  132 | 	private static final String DEFAULT_ID_SUFFIX = "}";
  133 | 
  134 | 	private static final String NO_PASSWORD_ENCODER_MAPPED = "There is no password encoder mapped for the id '%s'. "
  135 | 			+ "Check your configuration to ensure it matches one of the registered encoders.";
  136 | 
  137 | 	private static final String NO_PASSWORD_ENCODER_PREFIX = "Given that there is no default password encoder configured, each password must have a password encoding prefix. "
  138 | 			+ "Please either prefix this password with '{noop}' or set a default password encoder in `DelegatingPasswordEncoder`.";
  139 | 
  140 | 	private static final String MALFORMED_PASSWORD_ENCODER_PREFIX = "The name of the password encoder is improperly "
  141 | 			+ "formatted or incomplete. The format should be '%sENCODER%spassword'.";
  142 | 
  143 | 	private final String idPrefix;
  144 | 
  145 | 	private final String idSuffix;
  146 | 
  147 | 	private final String idForEncode;
  148 | 
  149 | 	private final PasswordEncoder passwordEncoderForEncode;
  150 | 
  151 | 	private final Map<@Nullable String, PasswordEncoder> idToPasswordEncoder;
  152 | 
  153 | 	private PasswordEncoder defaultPasswordEncoderForMatches = new UnmappedIdPasswordEncoder();
  154 | 
  155 | 	/**
  156 | 	 * Creates a new instance.
  157 | 	 * @param idForEncode the id used to lookup which {@link PasswordEncoder} should be
  158 | 	 * used for {@link #encode(CharSequence)}
  159 | 	 * @param idToPasswordEncoder a Map of id to {@link PasswordEncoder} used to determine
  160 | 	 * which {@link PasswordEncoder} should be used for
  161 | 	 * {@link #matches(CharSequence, String)}
  162 | 	 */
  163 | 	public DelegatingPasswordEncoder(String idForEncode, Map<String, PasswordEncoder> idToPasswordEncoder) {
  164 | 		this(idForEncode, idToPasswordEncoder, DEFAULT_ID_PREFIX, DEFAULT_ID_SUFFIX);
  165 | 	}
  166 | 
  167 | 	/**
  168 | 	 * Creates a new instance.
  169 | 	 * @param idForEncode the id used to lookup which {@link PasswordEncoder} should be
  170 | 	 * used for {@link #encode(CharSequence)}
  171 | 	 * @param idToPasswordEncoder a Map of id to {@link PasswordEncoder} used to determine
  172 | 	 * which {@link PasswordEncoder} should be used for
  173 | 	 * @param idPrefix the prefix that denotes the start of the id in the encoded results
  174 | 	 * @param idSuffix the suffix that denotes the end of an id in the encoded results
  175 | 	 * {@link #matches(CharSequence, String)}
  176 | 	 */
  177 | 	public DelegatingPasswordEncoder(String idForEncode, Map<String, PasswordEncoder> idToPasswordEncoder,
  178 | 			String idPrefix, String idSuffix) {
  179 | 		if (idForEncode == null) {
  180 | 			throw new IllegalArgumentException("idForEncode cannot be null");
  181 | 		}
  182 | 		if (idPrefix == null) {
  183 | 			throw new IllegalArgumentException("prefix cannot be null");
  184 | 		}
  185 | 		if (idSuffix == null || idSuffix.isEmpty()) {
  186 | 			throw new IllegalArgumentException("suffix cannot be empty");
  187 | 		}
  188 | 		if (idPrefix.contains(idSuffix)) {
  189 | 			throw new IllegalArgumentException("idPrefix " + idPrefix + " cannot contain idSuffix " + idSuffix);
  190 | 		}
  191 | 
  192 | 		if (!idToPasswordEncoder.containsKey(idForEncode)) {
  193 | 			throw new IllegalArgumentException(
  194 | 					"idForEncode " + idForEncode + "is not found in idToPasswordEncoder " + idToPasswordEncoder);
  195 | 		}
  196 | 		for (String id : idToPasswordEncoder.keySet()) {
  197 | 			if (id == null) {
  198 | 				continue;
  199 | 			}
  200 | 			if (!idPrefix.isEmpty() && id.contains(idPrefix)) {
  201 | 				throw new IllegalArgumentException("id " + id + " cannot contain " + idPrefix);
  202 | 			}
  203 | 			if (id.contains(idSuffix)) {
  204 | 				throw new IllegalArgumentException("id " + id + " cannot contain " + idSuffix);
  205 | 			}
  206 | 		}
  207 | 		this.idForEncode = idForEncode;
  208 | 		this.passwordEncoderForEncode = idToPasswordEncoder.get(idForEncode);
  209 | 		this.idToPasswordEncoder = new HashMap<>(idToPasswordEncoder);
  210 | 		this.idPrefix = idPrefix;
  211 | 		this.idSuffix = idSuffix;
  212 | 	}
  213 | 
  214 | 	/**
  215 | 	 * Sets the {@link PasswordEncoder} to delegate to for
  216 | 	 * {@link #matches(CharSequence, String)} if the id is not mapped to a
  217 | 	 * {@link PasswordEncoder}.
  218 | 	 *
  219 | 	 * <p>
  220 | 	 * The encodedPassword provided will be the full password passed in including the
  221 | 	 * {"id"} portion.* For example, if the password of "{notmapped}foobar" was used, the
  222 | 	 * "id" would be "notmapped" and the encodedPassword passed into the
  223 | 	 * {@link PasswordEncoder} would be "{notmapped}foobar".
  224 | 	 * </p>
  225 | 	 * @param defaultPasswordEncoderForMatches the encoder to use. The default is to throw
  226 | 	 * an {@link IllegalArgumentException}
  227 | 	 */
  228 | 	public void setDefaultPasswordEncoderForMatches(PasswordEncoder defaultPasswordEncoderForMatches) {
  229 | 		if (defaultPasswordEncoderForMatches == null) {
  230 | 			throw new IllegalArgumentException("defaultPasswordEncoderForMatches cannot be null");
  231 | 		}
  232 | 		this.defaultPasswordEncoderForMatches = defaultPasswordEncoderForMatches;
  233 | 	}
  234 | 
  235 | 	@Override
  236 | 	protected String encodeNonNullPassword(String rawPassword) {
  237 | 		return this.idPrefix + this.idForEncode + this.idSuffix + this.passwordEncoderForEncode.encode(rawPassword);
  238 | 	}
  239 | 
  240 | 	@Override
  241 | 	protected boolean matchesNonNull(String rawPassword, String prefixEncodedPassword) {
  242 | 		String id = extractId(prefixEncodedPassword);
  243 | 		PasswordEncoder delegate = this.idToPasswordEncoder.get(id);
  244 | 		if (delegate == null) {
  245 | 			return this.defaultPasswordEncoderForMatches.matches(rawPassword, prefixEncodedPassword);
  246 | 		}
  247 | 		String encodedPassword = extractEncodedPassword(prefixEncodedPassword);
  248 | 		return delegate.matches(rawPassword, encodedPassword);
  249 | 	}
  250 | 
  251 | 	private @Nullable String extractId(@Nullable String prefixEncodedPassword) {
  252 | 		if (prefixEncodedPassword == null) {
  253 | 			return null;
  254 | 		}
  255 | 		int start = prefixEncodedPassword.indexOf(this.idPrefix);
  256 | 		if (start != 0) {
  257 | 			return null;
  258 | 		}
  259 | 		int end = prefixEncodedPassword.indexOf(this.idSuffix, start);
  260 | 		if (end < 0) {
  261 | 			return null;
  262 | 		}
  263 | 		return prefixEncodedPassword.substring(start + this.idPrefix.length(), end);
  264 | 	}
  265 | 
  266 | 	@Override
  267 | 	protected boolean upgradeEncodingNonNull(String prefixEncodedPassword) {
  268 | 		String id = extractId(prefixEncodedPassword);
  269 | 		if (!this.idForEncode.equalsIgnoreCase(id)) {
  270 | 			return true;
  271 | 		}
  272 | 		else {
  273 | 			String encodedPassword = extractEncodedPassword(prefixEncodedPassword);
  274 | 			return this.passwordEncoderForEncode.upgradeEncoding(encodedPassword);
  275 | 		}
  276 | 	}
  277 | 
  278 | 	private String extractEncodedPassword(String prefixEncodedPassword) {
  279 | 		int start = prefixEncodedPassword.indexOf(this.idSuffix);
  280 | 		return prefixEncodedPassword.substring(start + this.idSuffix.length());
  281 | 	}
  282 | 
  283 | 	/**
  284 | 	 * Default {@link PasswordEncoder} that throws an exception telling that a suitable
  285 | 	 * {@link PasswordEncoder} for the id could not be found.
  286 | 	 */
  287 | 	private class UnmappedIdPasswordEncoder extends AbstractValidatingPasswordEncoder {
  288 | 
  289 | 		@Override
  290 | 		protected String encodeNonNullPassword(String rawPassword) {
  291 | 			throw new UnsupportedOperationException("encode is not supported");
  292 | 		}
  293 | 
  294 | 		@Override
  295 | 		protected boolean matchesNonNull(String rawPassword, String prefixEncodedPassword) {
  296 | 			String id = extractId(prefixEncodedPassword);
  297 | 			if (id != null && !id.isBlank()) {
  298 | 				throw new IllegalArgumentException(String.format(NO_PASSWORD_ENCODER_MAPPED, id));
  299 | 			}
  300 | 			if (prefixEncodedPassword != null && !prefixEncodedPassword.isBlank()) {
  301 | 				int start = prefixEncodedPassword.indexOf(DelegatingPasswordEncoder.this.idPrefix);
  302 | 				int end = prefixEncodedPassword.indexOf(DelegatingPasswordEncoder.this.idSuffix, start);
  303 | 				if (start < 0 && end < 0) {
  304 | 					throw new IllegalArgumentException(NO_PASSWORD_ENCODER_PREFIX);
  305 | 				}
  306 | 			}
  307 | 			throw new IllegalArgumentException(String.format(MALFORMED_PASSWORD_ENCODER_PREFIX,
  308 | 					DelegatingPasswordEncoder.this.idPrefix, DelegatingPasswordEncoder.this.idSuffix));
  309 | 		}
  310 | 
  311 | 	}
  312 | 
  313 | }
```
