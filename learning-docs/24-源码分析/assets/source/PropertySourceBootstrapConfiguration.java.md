<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-context:5.1.0 | path: org/springframework/cloud/bootstrap/config/PropertySourceBootstrapConfiguration.java -->
<!-- 正文引用：[[源码:PropertySourceBootstrapConfiguration.java#L起-L止]] ；代码块头部：(src: PropertySourceBootstrapConfiguration.java#L起-L止) -->

# PropertySourceBootstrapConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-context:5.1.0` |
| 路径 | `org/springframework/cloud/bootstrap/config/PropertySourceBootstrapConfiguration.java` |
| 行数 | 338 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-context/5.1.0/spring-cloud-context-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:53.859Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
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
   17 | package org.springframework.cloud.bootstrap.config;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Arrays;
   21 | import java.util.Collection;
   22 | import java.util.Collections;
   23 | import java.util.List;
   24 | import java.util.Map;
   25 | import java.util.Set;
   26 | import java.util.TreeSet;
   27 | import java.util.stream.Collectors;
   28 | 
   29 | import org.apache.commons.logging.Log;
   30 | import org.apache.commons.logging.LogFactory;
   31 | 
   32 | import org.springframework.beans.factory.annotation.Autowired;
   33 | import org.springframework.boot.context.config.Profiles;
   34 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   35 | import org.springframework.boot.context.properties.bind.Bindable;
   36 | import org.springframework.boot.context.properties.bind.Binder;
   37 | import org.springframework.boot.logging.LogFile;
   38 | import org.springframework.boot.logging.LoggingInitializationContext;
   39 | import org.springframework.boot.logging.LoggingSystem;
   40 | import org.springframework.cloud.bootstrap.BootstrapApplicationListener;
   41 | import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
   42 | import org.springframework.cloud.logging.LoggingRebinder;
   43 | import org.springframework.context.ApplicationContextInitializer;
   44 | import org.springframework.context.ApplicationListener;
   45 | import org.springframework.context.ConfigurableApplicationContext;
   46 | import org.springframework.context.annotation.Configuration;
   47 | import org.springframework.context.event.ContextRefreshedEvent;
   48 | import org.springframework.core.Ordered;
   49 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   50 | import org.springframework.core.env.AbstractEnvironment;
   51 | import org.springframework.core.env.CompositePropertySource;
   52 | import org.springframework.core.env.ConfigurableEnvironment;
   53 | import org.springframework.core.env.EnumerablePropertySource;
   54 | import org.springframework.core.env.Environment;
   55 | import org.springframework.core.env.MutablePropertySources;
   56 | import org.springframework.core.env.PropertySource;
   57 | import org.springframework.util.StringUtils;
   58 | 
   59 | import static org.springframework.cloud.bootstrap.encrypt.AbstractEnvironmentDecrypt.DECRYPTED_PROPERTY_SOURCE_NAME;
   60 | import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
   61 | 
   62 | /**
   63 |  * @author Dave Syer
   64 |  *
   65 |  */
   66 | @Configuration(proxyBeanMethods = false)
   67 | @EnableConfigurationProperties(PropertySourceBootstrapProperties.class)
   68 | public class PropertySourceBootstrapConfiguration implements ApplicationListener<ContextRefreshedEvent>,
   69 | 		ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {
   70 | 
   71 | 	/**
   72 | 	 * Bootstrap property source name.
   73 | 	 */
   74 | 	public static final String BOOTSTRAP_PROPERTY_SOURCE_NAME = BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME
   75 | 			+ "Properties";
   76 | 
   77 | 	private static Log logger = LogFactory.getLog(PropertySourceBootstrapConfiguration.class);
   78 | 
   79 | 	private int order = Ordered.HIGHEST_PRECEDENCE + 10;
   80 | 
   81 | 	@Autowired(required = false)
   82 | 	private List<PropertySourceLocator> propertySourceLocators = new ArrayList<>();
   83 | 
   84 | 	@Autowired
   85 | 	private PropertySourceBootstrapProperties bootstrapProperties;
   86 | 
   87 | 	@Override
   88 | 	public int getOrder() {
   89 | 		return this.order;
   90 | 	}
   91 | 
   92 | 	public void setPropertySourceLocators(Collection<PropertySourceLocator> propertySourceLocators) {
   93 | 		this.propertySourceLocators = new ArrayList<>(propertySourceLocators);
   94 | 	}
   95 | 
   96 | 	/*
   97 | 	 * The ApplicationListener is called when the main application context is initialized.
   98 | 	 * This will be called after the ApplicationListener ContextRefreshedEvent is fired
   99 | 	 * during the bootstrap phase. This method is also what added PropertySources prior to
  100 | 	 * Spring Cloud 2021.0.7, this is why it will be called when
  101 | 	 * spring.cloud.config.initialize-on-context-refresh is false. When
  102 | 	 * spring.cloud.config.initialize-on-context-refresh is true this method provides a
  103 | 	 * "second fetch" of configuration data to fetch any additional configuration data
  104 | 	 * from profiles that have been activated.
  105 | 	 */
  106 | 	@Override
  107 | 	public void initialize(ConfigurableApplicationContext applicationContext) {
  108 | 		if (!bootstrapProperties.isInitializeOnContextRefresh() || !applicationContext.getEnvironment()
  109 | 			.getPropertySources()
  110 | 			.contains(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
  111 | 			doInitialize(applicationContext);
  112 | 		}
  113 | 	}
  114 | 
  115 | 	private void doInitialize(ConfigurableApplicationContext applicationContext) {
  116 | 		List<PropertySource<?>> composite = new ArrayList<>();
  117 | 		AnnotationAwareOrderComparator.sort(this.propertySourceLocators);
  118 | 		boolean empty = true;
  119 | 		ConfigurableEnvironment environment = applicationContext.getEnvironment();
  120 | 		for (PropertySourceLocator locator : this.propertySourceLocators) {
  121 | 			Collection<PropertySource<?>> source = locator.locateCollection(environment);
  122 | 			if (source == null || source.size() == 0) {
  123 | 				continue;
  124 | 			}
  125 | 			List<PropertySource<?>> sourceList = new ArrayList<>();
  126 | 			for (PropertySource<?> p : source) {
  127 | 				if (p instanceof EnumerablePropertySource<?> enumerable) {
  128 | 					sourceList.add(new BootstrapPropertySource<>(enumerable));
  129 | 				}
  130 | 				else {
  131 | 					sourceList.add(new SimpleBootstrapPropertySource(p));
  132 | 				}
  133 | 			}
  134 | 			logger.info("Located property source: " + sourceList);
  135 | 			composite.addAll(sourceList);
  136 | 			empty = false;
  137 | 		}
  138 | 		if (!empty) {
  139 | 			MutablePropertySources propertySources = environment.getPropertySources();
  140 | 			String logConfig = environment.resolvePlaceholders("${logging.config:}");
  141 | 			LogFile logFile = LogFile.get(environment);
  142 | 			for (PropertySource<?> p : environment.getPropertySources()) {
  143 | 				if (p.getName().startsWith(BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
  144 | 					propertySources.remove(p.getName());
  145 | 				}
  146 | 			}
  147 | 			insertPropertySources(propertySources, composite);
  148 | 			reinitializeLoggingSystem(environment);
  149 | 			setLogLevels(applicationContext, environment);
  150 | 			handleProfiles(environment);
  151 | 		}
  152 | 	}
  153 | 
  154 | 	private void reinitializeLoggingSystem(ConfigurableEnvironment environment) {
  155 | 		Map<String, Object> props = Binder.get(environment)
  156 | 			.bind("logging", Bindable.mapOf(String.class, Object.class))
  157 | 			.orElseGet(Collections::emptyMap);
  158 | 		if (!props.isEmpty()) {
  159 | 			String logConfig = environment.resolvePlaceholders("${logging.config:}");
  160 | 			LogFile logFile = LogFile.get(environment);
  161 | 			LoggingSystem system = LoggingSystem.get(LoggingSystem.class.getClassLoader());
  162 | 			try {
  163 | 				// Three step initialization that accounts for the clean up of the logging
  164 | 				// context before initialization. Spring Boot doesn't initialize a logging
  165 | 				// system that hasn't had this sequence applied (since 1.4.1).
  166 | 				system.cleanUp();
  167 | 				system.beforeInitialize();
  168 | 				system.initialize(new LoggingInitializationContext(environment), logConfig, logFile);
  169 | 			}
  170 | 			catch (Exception ex) {
  171 | 				PropertySourceBootstrapConfiguration.logger.warn("Error opening logging config file " + logConfig, ex);
  172 | 			}
  173 | 		}
  174 | 	}
  175 | 
  176 | 	private void setLogLevels(ConfigurableApplicationContext applicationContext, ConfigurableEnvironment environment) {
  177 | 		LoggingRebinder rebinder = new LoggingRebinder();
  178 | 		rebinder.setEnvironment(environment);
  179 | 		// We can't fire the event in the ApplicationContext here (too early), but we can
  180 | 		// create our own listener and poke it (it doesn't need the key changes)
  181 | 		rebinder.onApplicationEvent(new EnvironmentChangeEvent(applicationContext, Collections.emptySet()));
  182 | 	}
  183 | 
  184 | 	private void insertPropertySources(MutablePropertySources propertySources, List<PropertySource<?>> composite) {
  185 | 		MutablePropertySources incoming = new MutablePropertySources();
  186 | 		List<PropertySource<?>> reversedComposite = new ArrayList<>(composite);
  187 | 		// Reverse the list so that when we call addFirst below we are maintaining the
  188 | 		// same order of PropertySources
  189 | 		// Wherever we call addLast we can use the order in the List since the first item
  190 | 		// will end up before the rest
  191 | 		Collections.reverse(reversedComposite);
  192 | 		for (PropertySource<?> p : reversedComposite) {
  193 | 			incoming.addFirst(p);
  194 | 		}
  195 | 		PropertySourceBootstrapProperties remoteProperties = new PropertySourceBootstrapProperties();
  196 | 		Binder.get(environment(incoming)).bind("spring.cloud.config", Bindable.ofInstance(remoteProperties));
  197 | 		if (!remoteProperties.isAllowOverride()
  198 | 				|| (!remoteProperties.isOverrideNone() && remoteProperties.isOverrideSystemProperties())) {
  199 | 			for (PropertySource<?> p : reversedComposite) {
  200 | 				if (propertySources.contains(DECRYPTED_PROPERTY_SOURCE_NAME)) {
  201 | 					propertySources.addAfter(DECRYPTED_PROPERTY_SOURCE_NAME, p);
  202 | 				}
  203 | 				else {
  204 | 					propertySources.addFirst(p);
  205 | 				}
  206 | 			}
  207 | 			return;
  208 | 		}
  209 | 		if (remoteProperties.isOverrideNone()) {
  210 | 			for (PropertySource<?> p : composite) {
  211 | 				propertySources.addLast(p);
  212 | 			}
  213 | 			return;
  214 | 		}
  215 | 		if (propertySources.contains(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
  216 | 			if (!remoteProperties.isOverrideSystemProperties()) {
  217 | 				for (PropertySource<?> p : reversedComposite) {
  218 | 					propertySources.addAfter(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, p);
  219 | 				}
  220 | 			}
  221 | 			else {
  222 | 				for (PropertySource<?> p : composite) {
  223 | 					propertySources.addBefore(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, p);
  224 | 				}
  225 | 			}
  226 | 		}
  227 | 		else {
  228 | 			for (PropertySource<?> p : composite) {
  229 | 				propertySources.addLast(p);
  230 | 			}
  231 | 		}
  232 | 	}
  233 | 
  234 | 	private Environment environment(MutablePropertySources incoming) {
  235 | 		ConfigurableEnvironment environment = new AbstractEnvironment() {
  236 | 		};
  237 | 		for (PropertySource<?> source : incoming) {
  238 | 			environment.getPropertySources().addLast(source);
  239 | 		}
  240 | 		return environment;
  241 | 	}
  242 | 
  243 | 	private void handleProfiles(ConfigurableEnvironment environment) {
  244 | 		if (bootstrapProperties.isInitializeOnContextRefresh() && !environment.getPropertySources()
  245 | 			.contains(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
  246 | 			// In the case that spring.cloud.config.initialize-on-context-refresh is true
  247 | 			// this method will
  248 | 			// be called during the bootstrap phase and the main application startup. We
  249 | 			// only manipulate the environment profiles in the bootstrap phase as we are
  250 | 			// fetching
  251 | 			// any additional profile specific configuration when this method would be
  252 | 			// called during the
  253 | 			// main application startup, and it is not valid to activate profiles in
  254 | 			// profile specific
  255 | 			// configuration properties, so we should not run this method then.
  256 | 			return;
  257 | 		}
  258 | 		Set<String> includeProfiles = new TreeSet<>();
  259 | 		List<String> activeProfiles = new ArrayList<>();
  260 | 
  261 | 		for (PropertySource<?> propertySource : environment.getPropertySources()) {
  262 | 			addIncludedProfilesTo(includeProfiles, propertySource, environment);
  263 | 			addActiveProfilesTo(activeProfiles, propertySource, environment);
  264 | 		}
  265 | 
  266 | 		// If it's already accepted we assume the order was set intentionally
  267 | 		includeProfiles.removeAll(activeProfiles);
  268 | 		// Prepend each added profile (last wins in a property key clash)
  269 | 		for (String profile : includeProfiles) {
  270 | 			activeProfiles.add(0, profile);
  271 | 		}
  272 | 		List<String> activeProfilesFromEnvironment = Arrays.stream(environment.getActiveProfiles())
  273 | 			.collect(Collectors.toList());
  274 | 		if (!activeProfiles.containsAll(activeProfilesFromEnvironment)) {
  275 | 			activeProfiles.addAll(activeProfilesFromEnvironment);
  276 | 
  277 | 		}
  278 | 		environment.setActiveProfiles(activeProfiles.toArray(new String[activeProfiles.size()]));
  279 | 	}
  280 | 
  281 | 	private Set<String> addIncludedProfilesTo(Set<String> profiles, PropertySource<?> propertySource,
  282 | 			ConfigurableEnvironment environment) {
  283 | 		return addProfilesTo(profiles, propertySource, Profiles.INCLUDE_PROFILES_PROPERTY_NAME, environment);
  284 | 	}
  285 | 
  286 | 	private List<String> addActiveProfilesTo(List<String> profiles, PropertySource<?> propertySource,
  287 | 			ConfigurableEnvironment environment) {
  288 | 		// According to Spring Boot, "spring.profiles.active" should have priority,
  289 | 		// only value from property source with the highest priority wins.
  290 | 		// Once settled, ignore others
  291 | 		if (!profiles.isEmpty()) {
  292 | 			return profiles;
  293 | 		}
  294 | 		return addProfilesTo(profiles, propertySource, AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, environment);
  295 | 	}
  296 | 
  297 | 	private <T extends Collection<String>> T addProfilesTo(T profiles, PropertySource<?> propertySource,
  298 | 			String property, ConfigurableEnvironment environment) {
  299 | 		if (propertySource instanceof CompositePropertySource compositePropertySource) {
  300 | 			for (PropertySource<?> nestedPropertySource : compositePropertySource.getPropertySources()) {
  301 | 				addProfilesTo(profiles, nestedPropertySource, property, environment);
  302 | 			}
  303 | 		}
  304 | 		else {
  305 | 			Collections.addAll(profiles, getProfilesForValue(propertySource.getProperty(property), environment));
  306 | 		}
  307 | 		return profiles;
  308 | 	}
  309 | 
  310 | 	private String[] getProfilesForValue(Object property, ConfigurableEnvironment environment) {
  311 | 		final String value = (property == null ? null : property.toString());
  312 | 		return property == null ? new String[0] : resolvePlaceholdersInProfiles(value, environment);
  313 | 	}
  314 | 
  315 | 	private String[] resolvePlaceholdersInProfiles(String profiles, ConfigurableEnvironment environment) {
  316 | 		return Arrays.stream(StringUtils.tokenizeToStringArray(profiles, ","))
  317 | 			.map(environment::resolvePlaceholders)
  318 | 			.toArray(String[]::new);
  319 | 	}
  320 | 
  321 | 	/*
  322 | 	 * The ContextRefreshedEvent gets called at the end of the boostrap phase after config
  323 | 	 * data is loaded during bootstrap. This will run and do an "initial fetch" of
  324 | 	 * configuration data during bootstrap but before the main application context starts.
  325 | 	 */
  326 | 	@Override
  327 | 	public void onApplicationEvent(ContextRefreshedEvent event) {
  328 | 		if (bootstrapProperties.isInitializeOnContextRefresh()
  329 | 				&& event.getApplicationContext() instanceof ConfigurableApplicationContext) {
  330 | 			if (((ConfigurableApplicationContext) event.getApplicationContext()).getEnvironment()
  331 | 				.getPropertySources()
  332 | 				.contains(BootstrapApplicationListener.BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
  333 | 				doInitialize((ConfigurableApplicationContext) event.getApplicationContext());
  334 | 			}
  335 | 		}
  336 | 	}
  337 | 
  338 | }
```
