<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-core:7.1.0 | path: org/springframework/core/env/AbstractEnvironment.java -->
<!-- 正文引用：[[源码:AbstractEnvironment.java#L起-L止]] ；代码块头部：(src: AbstractEnvironment.java#L起-L止) -->

# AbstractEnvironment.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-core:7.1.0` |
| 路径 | `org/springframework/core/env/AbstractEnvironment.java` |
| 行数 | 597 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-core/7.1.0/spring-core-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:12:41.341Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
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
   17 | package org.springframework.core.env;
   18 | 
   19 | import java.util.Arrays;
   20 | import java.util.Collections;
   21 | import java.util.LinkedHashSet;
   22 | import java.util.Map;
   23 | import java.util.Set;
   24 | 
   25 | import org.apache.commons.logging.Log;
   26 | import org.apache.commons.logging.LogFactory;
   27 | import org.jspecify.annotations.Nullable;
   28 | 
   29 | import org.springframework.core.SpringProperties;
   30 | import org.springframework.core.convert.support.ConfigurableConversionService;
   31 | import org.springframework.util.Assert;
   32 | import org.springframework.util.ObjectUtils;
   33 | import org.springframework.util.StringUtils;
   34 | 
   35 | /**
   36 |  * Abstract base class for {@link Environment} implementations. Supports the notion of
   37 |  * reserved default profile names and enables specifying active and default profiles
   38 |  * through the {@link #ACTIVE_PROFILES_PROPERTY_NAME} and
   39 |  * {@link #DEFAULT_PROFILES_PROPERTY_NAME} properties.
   40 |  *
   41 |  * <p>Concrete subclasses differ primarily on which {@link PropertySource} objects they
   42 |  * add by default. {@code AbstractEnvironment} adds none. Subclasses should contribute
   43 |  * property sources through the protected {@link #customizePropertySources(MutablePropertySources)}
   44 |  * hook, while clients should customize using {@link ConfigurableEnvironment#getPropertySources()}
   45 |  * and work against the {@link MutablePropertySources} API.
   46 |  * See {@link ConfigurableEnvironment} javadoc for usage examples.
   47 |  *
   48 |  * @author Chris Beams
   49 |  * @author Juergen Hoeller
   50 |  * @author Phillip Webb
   51 |  * @since 3.1
   52 |  * @see ConfigurableEnvironment
   53 |  * @see StandardEnvironment
   54 |  */
   55 | public abstract class AbstractEnvironment implements ConfigurableEnvironment {
   56 | 
   57 | 	/**
   58 | 	 * System property that instructs Spring to ignore system environment variables,
   59 | 	 * i.e. to never attempt to retrieve such a variable via {@link System#getenv()}.
   60 | 	 * <p>The default is "false", falling back to system environment variable checks if a
   61 | 	 * Spring environment property (for example, a placeholder in a configuration String) isn't
   62 | 	 * resolvable otherwise. Consider switching this flag to "true" if you experience
   63 | 	 * log warnings from {@code getenv} calls coming from Spring.
   64 | 	 * @see #suppressGetenvAccess()
   65 | 	 */
   66 | 	public static final String IGNORE_GETENV_PROPERTY_NAME = "spring.getenv.ignore";
   67 | 
   68 | 	/**
   69 | 	 * Name of the property to specify active profiles: {@value}.
   70 | 	 * <p>The value may be comma delimited.
   71 | 	 * <p>Note that certain shell environments such as Bash disallow the use of the period
   72 | 	 * character in variable names. Assuming that Spring's {@link SystemEnvironmentPropertySource}
   73 | 	 * is in use, this property may be specified as an environment variable named
   74 | 	 * {@code SPRING_PROFILES_ACTIVE}.
   75 | 	 * @see ConfigurableEnvironment#setActiveProfiles
   76 | 	 */
   77 | 	public static final String ACTIVE_PROFILES_PROPERTY_NAME = "spring.profiles.active";
   78 | 
   79 | 	/**
   80 | 	 * Name of the property to specify profiles that are active by default: {@value}.
   81 | 	 * <p>The value may be comma delimited.
   82 | 	 * <p>Note that certain shell environments such as Bash disallow the use of the period
   83 | 	 * character in variable names. Assuming that Spring's {@link SystemEnvironmentPropertySource}
   84 | 	 * is in use, this property may be specified as an environment variable named
   85 | 	 * {@code SPRING_PROFILES_DEFAULT}.
   86 | 	 * @see ConfigurableEnvironment#setDefaultProfiles
   87 | 	 */
   88 | 	public static final String DEFAULT_PROFILES_PROPERTY_NAME = "spring.profiles.default";
   89 | 
   90 | 	/**
   91 | 	 * Name of the reserved default profile name: {@value}.
   92 | 	 * <p>If no default profile names are explicitly set and no active profile names
   93 | 	 * are explicitly set, this profile will automatically be activated by default.
   94 | 	 * @see #getReservedDefaultProfiles
   95 | 	 * @see ConfigurableEnvironment#setDefaultProfiles
   96 | 	 * @see ConfigurableEnvironment#setActiveProfiles
   97 | 	 * @see AbstractEnvironment#DEFAULT_PROFILES_PROPERTY_NAME
   98 | 	 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
   99 | 	 */
  100 | 	public static final String RESERVED_DEFAULT_PROFILE_NAME = "default";
  101 | 
  102 | 
  103 | 	protected final Log logger = LogFactory.getLog(getClass());
  104 | 
  105 | 	private final Set<String> activeProfiles = new LinkedHashSet<>();
  106 | 
  107 | 	private final Set<String> defaultProfiles = new LinkedHashSet<>(getReservedDefaultProfiles());
  108 | 
  109 | 	private final MutablePropertySources propertySources;
  110 | 
  111 | 	private final ConfigurablePropertyResolver propertyResolver;
  112 | 
  113 | 
  114 | 	/**
  115 | 	 * Create a new {@code Environment} instance, calling back to
  116 | 	 * {@link #customizePropertySources(MutablePropertySources)} during construction to
  117 | 	 * allow subclasses to contribute or manipulate {@link PropertySource} instances as
  118 | 	 * appropriate.
  119 | 	 * @see #customizePropertySources(MutablePropertySources)
  120 | 	 */
  121 | 	public AbstractEnvironment() {
  122 | 		this(new MutablePropertySources());
  123 | 	}
  124 | 
  125 | 	/**
  126 | 	 * Create a new {@code Environment} instance with a specific
  127 | 	 * {@link MutablePropertySources} instance, calling back to
  128 | 	 * {@link #customizePropertySources(MutablePropertySources)} during
  129 | 	 * construction to allow subclasses to contribute or manipulate
  130 | 	 * {@link PropertySource} instances as appropriate.
  131 | 	 * @param propertySources property sources to use
  132 | 	 * @since 5.3.4
  133 | 	 * @see #customizePropertySources(MutablePropertySources)
  134 | 	 */
  135 | 	protected AbstractEnvironment(MutablePropertySources propertySources) {
  136 | 		this.propertySources = propertySources;
  137 | 		this.propertyResolver = createPropertyResolver(propertySources);
  138 | 		customizePropertySources(propertySources);
  139 | 	}
  140 | 
  141 | 
  142 | 	/**
  143 | 	 * Factory method used to create the {@link ConfigurablePropertyResolver}
  144 | 	 * used by this {@code Environment}.
  145 | 	 * @since 5.3.4
  146 | 	 * @see #getPropertyResolver()
  147 | 	 */
  148 | 	protected ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources) {
  149 | 		return new PropertySourcesPropertyResolver(propertySources);
  150 | 	}
  151 | 
  152 | 	/**
  153 | 	 * Return the {@link ConfigurablePropertyResolver} used by the {@code Environment}.
  154 | 	 * @since 5.3.4
  155 | 	 * @see #createPropertyResolver(MutablePropertySources)
  156 | 	 */
  157 | 	protected final ConfigurablePropertyResolver getPropertyResolver() {
  158 | 		return this.propertyResolver;
  159 | 	}
  160 | 
  161 | 	/**
  162 | 	 * Customize the set of {@link PropertySource} objects to be searched by this
  163 | 	 * {@code Environment} during calls to {@link #getProperty(String)} and related
  164 | 	 * methods.
  165 | 	 *
  166 | 	 * <p>Subclasses that override this method are encouraged to add property
  167 | 	 * sources using {@link MutablePropertySources#addLast(PropertySource)} such that
  168 | 	 * further subclasses may call {@code super.customizePropertySources()} with
  169 | 	 * predictable results. For example:
  170 | 	 *
  171 | 	 * <pre class="code">
  172 | 	 * public class Level1Environment extends AbstractEnvironment {
  173 | 	 *     &#064;Override
  174 | 	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
  175 | 	 *         super.customizePropertySources(propertySources); // no-op from base class
  176 | 	 *         propertySources.addLast(new PropertySourceA(...));
  177 | 	 *         propertySources.addLast(new PropertySourceB(...));
  178 | 	 *     }
  179 | 	 * }
  180 | 	 *
  181 | 	 * public class Level2Environment extends Level1Environment {
  182 | 	 *     &#064;Override
  183 | 	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
  184 | 	 *         super.customizePropertySources(propertySources); // add all from superclass
  185 | 	 *         propertySources.addLast(new PropertySourceC(...));
  186 | 	 *         propertySources.addLast(new PropertySourceD(...));
  187 | 	 *     }
  188 | 	 * }
  189 | 	 * </pre>
  190 | 	 *
  191 | 	 * <p>In this arrangement, properties will be resolved against sources A, B, C, D in that
  192 | 	 * order. That is to say that property source "A" has precedence over property source
  193 | 	 * "D". If the {@code Level2Environment} subclass wished to give property sources C
  194 | 	 * and D higher precedence than A and B, it could simply call
  195 | 	 * {@code super.customizePropertySources} after, rather than before adding its own:
  196 | 	 *
  197 | 	 * <pre class="code">
  198 | 	 * public class Level2Environment extends Level1Environment {
  199 | 	 *     &#064;Override
  200 | 	 *     protected void customizePropertySources(MutablePropertySources propertySources) {
  201 | 	 *         propertySources.addLast(new PropertySourceC(...));
  202 | 	 *         propertySources.addLast(new PropertySourceD(...));
  203 | 	 *         super.customizePropertySources(propertySources); // add all from superclass
  204 | 	 *     }
  205 | 	 * }
  206 | 	 * </pre>
  207 | 	 *
  208 | 	 * <p>The search order is now C, D, A, B as desired.
  209 | 	 *
  210 | 	 * <p>Beyond these recommendations, subclasses may use any of the {@code add*},
  211 | 	 * {@code remove}, or {@code replace} methods exposed by {@link MutablePropertySources}
  212 | 	 * in order to create the exact arrangement of property sources desired.
  213 | 	 *
  214 | 	 * <p>The base implementation registers no property sources.
  215 | 	 *
  216 | 	 * <p>Note that clients of any {@link ConfigurableEnvironment} may further customize
  217 | 	 * property sources via the {@link #getPropertySources()} accessor, typically within
  218 | 	 * an {@link org.springframework.context.ApplicationContextInitializer
  219 | 	 * ApplicationContextInitializer}. For example:
  220 | 	 *
  221 | 	 * <pre class="code">
  222 | 	 * ConfigurableEnvironment env = new StandardEnvironment();
  223 | 	 * env.getPropertySources().addLast(new PropertySourceX(...));
  224 | 	 * </pre>
  225 | 	 *
  226 | 	 * <h2>A warning about instance variable access</h2>
  227 | 	 * <p>Instance variables declared in subclasses and having default initial values should
  228 | 	 * <em>not</em> be accessed from within this method. Due to Java object creation
  229 | 	 * lifecycle constraints, any initial value will not yet be assigned when this
  230 | 	 * callback is invoked by the {@link #AbstractEnvironment()} constructor, which may
  231 | 	 * lead to a {@code NullPointerException} or other problems. If you need to access
  232 | 	 * default values of instance variables, leave this method as a no-op and perform
  233 | 	 * property source manipulation and instance variable access directly within the
  234 | 	 * subclass constructor. Note that <em>assigning</em> values to instance variables is
  235 | 	 * not problematic; it is only attempting to read default values that must be avoided.
  236 | 	 * @see MutablePropertySources
  237 | 	 * @see PropertySourcesPropertyResolver
  238 | 	 * @see org.springframework.context.ApplicationContextInitializer
  239 | 	 */
  240 | 	protected void customizePropertySources(MutablePropertySources propertySources) {
  241 | 	}
  242 | 
  243 | 	/**
  244 | 	 * Return the set of reserved default profile names. This implementation returns
  245 | 	 * {@value #RESERVED_DEFAULT_PROFILE_NAME}. Subclasses may override in order to
  246 | 	 * customize the set of reserved names.
  247 | 	 * @see #RESERVED_DEFAULT_PROFILE_NAME
  248 | 	 * @see #doGetDefaultProfiles()
  249 | 	 */
  250 | 	protected Set<String> getReservedDefaultProfiles() {
  251 | 		return Collections.singleton(RESERVED_DEFAULT_PROFILE_NAME);
  252 | 	}
  253 | 
  254 | 
  255 | 	//---------------------------------------------------------------------
  256 | 	// Implementation of ConfigurableEnvironment interface
  257 | 	//---------------------------------------------------------------------
  258 | 
  259 | 	@Override
  260 | 	public String[] getActiveProfiles() {
  261 | 		return StringUtils.toStringArray(doGetActiveProfiles());
  262 | 	}
  263 | 
  264 | 	/**
  265 | 	 * Return the set of active profiles as explicitly set through
  266 | 	 * {@link #setActiveProfiles} or if the current set of active profiles
  267 | 	 * is empty, check for the presence of {@link #doGetActiveProfilesProperty()}
  268 | 	 * and assign its value to the set of active profiles.
  269 | 	 * @see #getActiveProfiles()
  270 | 	 * @see #doGetActiveProfilesProperty()
  271 | 	 */
  272 | 	protected Set<String> doGetActiveProfiles() {
  273 | 		synchronized (this.activeProfiles) {
  274 | 			if (this.activeProfiles.isEmpty()) {
  275 | 				String profiles = doGetActiveProfilesProperty();
  276 | 				if (StringUtils.hasText(profiles)) {
  277 | 					setActiveProfiles(StringUtils.commaDelimitedListToStringArray(
  278 | 							StringUtils.trimAllWhitespace(profiles)));
  279 | 				}
  280 | 			}
  281 | 			return this.activeProfiles;
  282 | 		}
  283 | 	}
  284 | 
  285 | 	/**
  286 | 	 * Return the property value for the active profiles.
  287 | 	 * @since 5.3.4
  288 | 	 * @see #ACTIVE_PROFILES_PROPERTY_NAME
  289 | 	 */
  290 | 	protected @Nullable String doGetActiveProfilesProperty() {
  291 | 		return getProperty(ACTIVE_PROFILES_PROPERTY_NAME);
  292 | 	}
  293 | 
  294 | 	@Override
  295 | 	public void setActiveProfiles(String... profiles) {
  296 | 		Assert.notNull(profiles, "Profile array must not be null");
  297 | 		if (logger.isDebugEnabled()) {
  298 | 			logger.debug("Activating profiles " + Arrays.toString(profiles));
  299 | 		}
  300 | 		synchronized (this.activeProfiles) {
  301 | 			this.activeProfiles.clear();
  302 | 			for (String profile : profiles) {
  303 | 				validateProfile(profile);
  304 | 				this.activeProfiles.add(profile);
  305 | 			}
  306 | 		}
  307 | 	}
  308 | 
  309 | 	@Override
  310 | 	public void addActiveProfile(String profile) {
  311 | 		if (logger.isDebugEnabled()) {
  312 | 			logger.debug("Activating profile '" + profile + "'");
  313 | 		}
  314 | 		validateProfile(profile);
  315 | 		doGetActiveProfiles();
  316 | 		synchronized (this.activeProfiles) {
  317 | 			this.activeProfiles.add(profile);
  318 | 		}
  319 | 	}
  320 | 
  321 | 	@Override
  322 | 	public String[] getDefaultProfiles() {
  323 | 		return StringUtils.toStringArray(doGetDefaultProfiles());
  324 | 	}
  325 | 
  326 | 	/**
  327 | 	 * Return the set of default profiles explicitly set via
  328 | 	 * {@link #setDefaultProfiles(String...)}, or if the current set of default profiles
  329 | 	 * consists only of {@linkplain #getReservedDefaultProfiles() reserved default
  330 | 	 * profiles}, then check for the presence of {@link #doGetDefaultProfilesProperty()}
  331 | 	 * and assign its value (if any) to the set of default profiles.
  332 | 	 * @see #AbstractEnvironment()
  333 | 	 * @see #getDefaultProfiles()
  334 | 	 * @see #getReservedDefaultProfiles()
  335 | 	 * @see #doGetDefaultProfilesProperty()
  336 | 	 */
  337 | 	protected Set<String> doGetDefaultProfiles() {
  338 | 		synchronized (this.defaultProfiles) {
  339 | 			if (this.defaultProfiles.equals(getReservedDefaultProfiles())) {
  340 | 				String profiles = doGetDefaultProfilesProperty();
  341 | 				if (StringUtils.hasText(profiles)) {
  342 | 					setDefaultProfiles(StringUtils.commaDelimitedListToStringArray(
  343 | 							StringUtils.trimAllWhitespace(profiles)));
  344 | 				}
  345 | 			}
  346 | 			return this.defaultProfiles;
  347 | 		}
  348 | 	}
  349 | 
  350 | 	/**
  351 | 	 * Return the property value for the default profiles.
  352 | 	 * @since 5.3.4
  353 | 	 * @see #DEFAULT_PROFILES_PROPERTY_NAME
  354 | 	 */
  355 | 	protected @Nullable String doGetDefaultProfilesProperty() {
  356 | 		return getProperty(DEFAULT_PROFILES_PROPERTY_NAME);
  357 | 	}
  358 | 
  359 | 	/**
  360 | 	 * Specify the set of profiles to be made active by default if no other profiles
  361 | 	 * are explicitly made active through {@link #setActiveProfiles}.
  362 | 	 * <p>Calling this method removes overrides any reserved default profiles
  363 | 	 * that may have been added during construction of the environment.
  364 | 	 * @see #AbstractEnvironment()
  365 | 	 * @see #getReservedDefaultProfiles()
  366 | 	 */
  367 | 	@Override
  368 | 	public void setDefaultProfiles(String... profiles) {
  369 | 		Assert.notNull(profiles, "Profile array must not be null");
  370 | 		synchronized (this.defaultProfiles) {
  371 | 			this.defaultProfiles.clear();
  372 | 			for (String profile : profiles) {
  373 | 				validateProfile(profile);
  374 | 				this.defaultProfiles.add(profile);
  375 | 			}
  376 | 		}
  377 | 	}
  378 | 
  379 | 	@Override
  380 | 	@Deprecated(since = "5.1")
  381 | 	public boolean acceptsProfiles(String... profiles) {
  382 | 		Assert.notEmpty(profiles, "Must specify at least one profile");
  383 | 		for (String profile : profiles) {
  384 | 			if (StringUtils.hasLength(profile) && profile.charAt(0) == '!') {
  385 | 				if (!isProfileActive(profile.substring(1))) {
  386 | 					return true;
  387 | 				}
  388 | 			}
  389 | 			else if (isProfileActive(profile)) {
  390 | 				return true;
  391 | 			}
  392 | 		}
  393 | 		return false;
  394 | 	}
  395 | 
  396 | 	@Override
  397 | 	public boolean acceptsProfiles(Profiles profiles) {
  398 | 		Assert.notNull(profiles, "Profiles must not be null");
  399 | 		return profiles.matches(this::isProfileActive);
  400 | 	}
  401 | 
  402 | 	/**
  403 | 	 * Return whether the given profile is active, or if active profiles are empty
  404 | 	 * whether the profile should be active by default.
  405 | 	 * @throws IllegalArgumentException per {@link #validateProfile(String)}
  406 | 	 */
  407 | 	protected boolean isProfileActive(String profile) {
  408 | 		validateProfile(profile);
  409 | 		Set<String> currentActiveProfiles = doGetActiveProfiles();
  410 | 		return (currentActiveProfiles.contains(profile) ||
  411 | 				(currentActiveProfiles.isEmpty() && doGetDefaultProfiles().contains(profile)));
  412 | 	}
  413 | 
  414 | 	/**
  415 | 	 * Validate the given profile, called internally prior to adding to the set of
  416 | 	 * active or default profiles.
  417 | 	 * <p>Subclasses may override to impose further restrictions on profile syntax.
  418 | 	 * @throws IllegalArgumentException if the profile is null, empty, whitespace-only or
  419 | 	 * begins with the profile NOT operator (!)
  420 | 	 * @see #acceptsProfiles
  421 | 	 * @see #addActiveProfile
  422 | 	 * @see #setDefaultProfiles
  423 | 	 */
  424 | 	protected void validateProfile(String profile) {
  425 | 		if (!StringUtils.hasText(profile)) {
  426 | 			throw new IllegalArgumentException("Invalid profile [" + profile + "]: must contain text");
  427 | 		}
  428 | 		if (profile.charAt(0) == '!') {
  429 | 			throw new IllegalArgumentException("Invalid profile [" + profile + "]: must not begin with ! operator");
  430 | 		}
  431 | 	}
  432 | 
  433 | 	@Override
  434 | 	public MutablePropertySources getPropertySources() {
  435 | 		return this.propertySources;
  436 | 	}
  437 | 
  438 | 	@Override
  439 | 	@SuppressWarnings({"rawtypes", "unchecked"})
  440 | 	public Map<String, Object> getSystemProperties() {
  441 | 		return (Map) System.getProperties();
  442 | 	}
  443 | 
  444 | 	@Override
  445 | 	@SuppressWarnings({"rawtypes", "unchecked"})
  446 | 	public Map<String, Object> getSystemEnvironment() {
  447 | 		if (suppressGetenvAccess()) {
  448 | 			return Collections.emptyMap();
  449 | 		}
  450 | 		return (Map) System.getenv();
  451 | 	}
  452 | 
  453 | 	/**
  454 | 	 * Determine whether to suppress {@link System#getenv()}/{@link System#getenv(String)}
  455 | 	 * access for the purposes of {@link #getSystemEnvironment()}.
  456 | 	 * <p>If this method returns {@code true}, an empty dummy Map will be used instead
  457 | 	 * of the regular system environment Map, never even trying to call {@code getenv}
  458 | 	 * and therefore avoiding security manager warnings (if any).
  459 | 	 * <p>The default implementation checks for the "spring.getenv.ignore" system property,
  460 | 	 * returning {@code true} if its value equals "true" in any case.
  461 | 	 * @see #IGNORE_GETENV_PROPERTY_NAME
  462 | 	 * @see SpringProperties#getFlag
  463 | 	 */
  464 | 	protected boolean suppressGetenvAccess() {
  465 | 		return SpringProperties.getFlag(IGNORE_GETENV_PROPERTY_NAME);
  466 | 	}
  467 | 
  468 | 	@Override
  469 | 	public void merge(ConfigurableEnvironment parent) {
  470 | 		for (PropertySource<?> ps : parent.getPropertySources()) {
  471 | 			if (!this.propertySources.contains(ps.getName())) {
  472 | 				this.propertySources.addLast(ps);
  473 | 			}
  474 | 		}
  475 | 		String[] parentActiveProfiles = parent.getActiveProfiles();
  476 | 		if (!ObjectUtils.isEmpty(parentActiveProfiles)) {
  477 | 			synchronized (this.activeProfiles) {
  478 | 				Collections.addAll(this.activeProfiles, parentActiveProfiles);
  479 | 			}
  480 | 		}
  481 | 		String[] parentDefaultProfiles = parent.getDefaultProfiles();
  482 | 		if (!ObjectUtils.isEmpty(parentDefaultProfiles)) {
  483 | 			synchronized (this.defaultProfiles) {
  484 | 				this.defaultProfiles.remove(RESERVED_DEFAULT_PROFILE_NAME);
  485 | 				Collections.addAll(this.defaultProfiles, parentDefaultProfiles);
  486 | 			}
  487 | 		}
  488 | 	}
  489 | 
  490 | 
  491 | 	//---------------------------------------------------------------------
  492 | 	// Implementation of ConfigurablePropertyResolver interface
  493 | 	//---------------------------------------------------------------------
  494 | 
  495 | 	@Override
  496 | 	public ConfigurableConversionService getConversionService() {
  497 | 		return this.propertyResolver.getConversionService();
  498 | 	}
  499 | 
  500 | 	@Override
  501 | 	public void setConversionService(ConfigurableConversionService conversionService) {
  502 | 		this.propertyResolver.setConversionService(conversionService);
  503 | 	}
  504 | 
  505 | 	@Override
  506 | 	public void setPlaceholderPrefix(String placeholderPrefix) {
  507 | 		this.propertyResolver.setPlaceholderPrefix(placeholderPrefix);
  508 | 	}
  509 | 
  510 | 	@Override
  511 | 	public void setPlaceholderSuffix(String placeholderSuffix) {
  512 | 		this.propertyResolver.setPlaceholderSuffix(placeholderSuffix);
  513 | 	}
  514 | 
  515 | 	@Override
  516 | 	public void setValueSeparator(@Nullable String valueSeparator) {
  517 | 		this.propertyResolver.setValueSeparator(valueSeparator);
  518 | 	}
  519 | 
  520 | 	@Override
  521 | 	public void setEscapeCharacter(@Nullable Character escapeCharacter) {
  522 | 		this.propertyResolver.setEscapeCharacter(escapeCharacter);
  523 | 	}
  524 | 
  525 | 	@Override
  526 | 	public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
  527 | 		this.propertyResolver.setIgnoreUnresolvableNestedPlaceholders(ignoreUnresolvableNestedPlaceholders);
  528 | 	}
  529 | 
  530 | 	@Override
  531 | 	public void setRequiredProperties(String... requiredProperties) {
  532 | 		this.propertyResolver.setRequiredProperties(requiredProperties);
  533 | 	}
  534 | 
  535 | 	@Override
  536 | 	public void validateRequiredProperties() throws MissingRequiredPropertiesException {
  537 | 		this.propertyResolver.validateRequiredProperties();
  538 | 	}
  539 | 
  540 | 
  541 | 	//---------------------------------------------------------------------
  542 | 	// Implementation of PropertyResolver interface
  543 | 	//---------------------------------------------------------------------
  544 | 
  545 | 	@Override
  546 | 	public boolean containsProperty(String key) {
  547 | 		return this.propertyResolver.containsProperty(key);
  548 | 	}
  549 | 
  550 | 	@Override
  551 | 	public @Nullable String getProperty(String key) {
  552 | 		return this.propertyResolver.getProperty(key);
  553 | 	}
  554 | 
  555 | 	@Override
  556 | 	public String getProperty(String key, String defaultValue) {
  557 | 		return this.propertyResolver.getProperty(key, defaultValue);
  558 | 	}
  559 | 
  560 | 	@Override
  561 | 	public <T> @Nullable T getProperty(String key, Class<T> targetType) {
  562 | 		return this.propertyResolver.getProperty(key, targetType);
  563 | 	}
  564 | 
  565 | 	@Override
  566 | 	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
  567 | 		return this.propertyResolver.getProperty(key, targetType, defaultValue);
  568 | 	}
  569 | 
  570 | 	@Override
  571 | 	public String getRequiredProperty(String key) throws IllegalStateException {
  572 | 		return this.propertyResolver.getRequiredProperty(key);
  573 | 	}
  574 | 
  575 | 	@Override
  576 | 	public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
  577 | 		return this.propertyResolver.getRequiredProperty(key, targetType);
  578 | 	}
  579 | 
  580 | 	@Override
  581 | 	public String resolvePlaceholders(String text) {
  582 | 		return this.propertyResolver.resolvePlaceholders(text);
  583 | 	}
  584 | 
  585 | 	@Override
  586 | 	public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
  587 | 		return this.propertyResolver.resolveRequiredPlaceholders(text);
  588 | 	}
  589 | 
  590 | 
  591 | 	@Override
  592 | 	public String toString() {
  593 | 		return getClass().getSimpleName() + " {activeProfiles=" + this.activeProfiles +
  594 | 				", defaultProfiles=" + this.defaultProfiles + ", propertySources=" + this.propertySources + "}";
  595 | 	}
  596 | 
  597 | }
```
