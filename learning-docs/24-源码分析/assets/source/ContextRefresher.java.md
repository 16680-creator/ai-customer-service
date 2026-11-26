<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-context:5.1.0 | path: org/springframework/cloud/context/refresh/ContextRefresher.java -->
<!-- 正文引用：[[源码:ContextRefresher.java#L起-L止]] ；代码块头部：(src: ContextRefresher.java#L起-L止) -->

# ContextRefresher.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-context:5.1.0` |
| 路径 | `org/springframework/cloud/context/refresh/ContextRefresher.java` |
| 行数 | 233 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-context/5.1.0/spring-cloud-context-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:31:36.855Z |

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
   17 | package org.springframework.cloud.context.refresh;
   18 | 
   19 | import java.io.IOException;
   20 | import java.io.InputStream;
   21 | import java.util.ArrayList;
   22 | import java.util.Arrays;
   23 | import java.util.HashMap;
   24 | import java.util.HashSet;
   25 | import java.util.List;
   26 | import java.util.Map;
   27 | import java.util.Objects;
   28 | import java.util.Set;
   29 | 
   30 | import org.apache.commons.logging.Log;
   31 | import org.apache.commons.logging.LogFactory;
   32 | 
   33 | import org.springframework.boot.env.ConfigTreePropertySource;
   34 | import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
   35 | import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
   36 | import org.springframework.cloud.context.scope.refresh.RefreshScope;
   37 | import org.springframework.context.ConfigurableApplicationContext;
   38 | import org.springframework.context.annotation.Configuration;
   39 | import org.springframework.core.env.CommandLinePropertySource;
   40 | import org.springframework.core.env.CompositePropertySource;
   41 | import org.springframework.core.env.ConfigurableEnvironment;
   42 | import org.springframework.core.env.EnumerablePropertySource;
   43 | import org.springframework.core.env.MutablePropertySources;
   44 | import org.springframework.core.env.PropertySource;
   45 | import org.springframework.core.env.StandardEnvironment;
   46 | import org.springframework.util.CollectionUtils;
   47 | import org.springframework.web.context.support.StandardServletEnvironment;
   48 | 
   49 | /**
   50 |  * @author Dave Syer
   51 |  * @author Venil Noronha
   52 |  * @author Mikhail Polivakha
   53 |  * @author arimu1
   54 |  */
   55 | public abstract class ContextRefresher {
   56 | 
   57 | 	protected final Log logger = LogFactory.getLog(getClass());
   58 | 
   59 | 	protected static final String[] DEFAULT_PROPERTY_SOURCES = new String[] {
   60 | 			// order matters, if cli args aren't first, things get messy
   61 | 			CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME, "defaultProperties" };
   62 | 
   63 | 	protected Set<String> standardSources = new HashSet<>(
   64 | 			Arrays.asList(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
   65 | 					StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
   66 | 					StandardServletEnvironment.JNDI_PROPERTY_SOURCE_NAME,
   67 | 					StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME,
   68 | 					StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME, "configurationProperties"));
   69 | 
   70 | 	protected final List<String> additionalPropertySourcesToRetain;
   71 | 
   72 | 	private final ConfigurableApplicationContext context;
   73 | 
   74 | 	private final RefreshScope scope;
   75 | 
   76 | 	@Deprecated
   77 | 	protected ContextRefresher(ConfigurableApplicationContext context, RefreshScope scope) {
   78 | 		this(context, scope, new RefreshAutoConfiguration.RefreshProperties());
   79 | 	}
   80 | 
   81 | 	@SuppressWarnings("unchecked")
   82 | 	protected ContextRefresher(ConfigurableApplicationContext context, RefreshScope scope,
   83 | 			RefreshAutoConfiguration.RefreshProperties properties) {
   84 | 		this.context = context;
   85 | 		this.scope = scope;
   86 | 		additionalPropertySourcesToRetain = properties.getAdditionalPropertySourcesToRetain();
   87 | 	}
   88 | 
   89 | 	protected ConfigurableApplicationContext getContext() {
   90 | 		return this.context;
   91 | 	}
   92 | 
   93 | 	protected RefreshScope getScope() {
   94 | 		return this.scope;
   95 | 	}
   96 | 
   97 | 	public synchronized Set<String> refresh() {
   98 | 		Set<String> keys = refreshEnvironment();
   99 | 		this.scope.refreshAll();
  100 | 		return keys;
  101 | 	}
  102 | 
  103 | 	public synchronized Set<String> refreshEnvironment() {
  104 | 		Map<String, Object> before = getCurrentEnvironmentProperties();
  105 | 		updateEnvironment();
  106 | 		Set<String> keys = changes(before, getCurrentEnvironmentProperties()).keySet();
  107 | 		this.context.publishEvent(new EnvironmentChangeEvent(this.context, keys));
  108 | 		return keys;
  109 | 	}
  110 | 
  111 | 	private Map<String, Object> getCurrentEnvironmentProperties() {
  112 | 		return extract(this.context.getEnvironment().getPropertySources());
  113 | 	}
  114 | 
  115 | 	protected abstract void updateEnvironment();
  116 | 
  117 | 	// Don't use ConfigurableEnvironment.merge() in case there are clashes with property
  118 | 	// source names
  119 | 	protected StandardEnvironment copyEnvironment(ConfigurableEnvironment sourceEnv) {
  120 | 		StandardEnvironment environment = new StandardEnvironment();
  121 | 		MutablePropertySources capturedPropertySources = environment.getPropertySources();
  122 | 		// Only copy the default property source(s) and the profiles over from the main
  123 | 		// environment (everything else should be pristine, just like it was on startup).
  124 | 		List<String> propertySourcesToRetain = new ArrayList<>(Arrays.asList(DEFAULT_PROPERTY_SOURCES));
  125 | 		if (!CollectionUtils.isEmpty(additionalPropertySourcesToRetain)) {
  126 | 			propertySourcesToRetain.addAll(additionalPropertySourcesToRetain);
  127 | 		}
  128 | 
  129 | 		propertySourcesToRetain.stream().filter(s -> sourceEnv.getPropertySources().contains(s)).forEach(s -> {
  130 | 			if (capturedPropertySources.contains(s)) {
  131 | 				capturedPropertySources.replace(s, sourceEnv.getPropertySources().get(s));
  132 | 			}
  133 | 			else {
  134 | 				capturedPropertySources.addLast(sourceEnv.getPropertySources().get(s));
  135 | 			}
  136 | 		});
  137 | 
  138 | 		environment.setActiveProfiles(sourceEnv.getActiveProfiles());
  139 | 		environment.setDefaultProfiles(sourceEnv.getDefaultProfiles());
  140 | 		return environment;
  141 | 	}
  142 | 
  143 | 	private Map<String, Object> changes(Map<String, Object> before, Map<String, Object> after) {
  144 | 		Map<String, Object> result = new HashMap<>();
  145 | 		for (String key : before.keySet()) {
  146 | 			if (!after.containsKey(key)) {
  147 | 				result.put(key, null);
  148 | 			}
  149 | 			else if (!equal(before.get(key), after.get(key))) {
  150 | 				result.put(key, after.get(key));
  151 | 			}
  152 | 		}
  153 | 		for (String key : after.keySet()) {
  154 | 			if (!before.containsKey(key)) {
  155 | 				result.put(key, after.get(key));
  156 | 			}
  157 | 		}
  158 | 		return result;
  159 | 	}
  160 | 
  161 | 	private boolean equal(Object one, Object two) {
  162 | 		if (Objects.equals(one, two)) {
  163 | 			return true;
  164 | 		}
  165 | 		if (one instanceof ConfigTreePropertySource.Value oneValue
  166 | 				&& two instanceof ConfigTreePropertySource.Value twoValue) {
  167 | 			return configTreeValuesEqual(oneValue, twoValue);
  168 | 		}
  169 | 		return false;
  170 | 	}
  171 | 
  172 | 	private boolean configTreeValuesEqual(ConfigTreePropertySource.Value one, ConfigTreePropertySource.Value two) {
  173 | 		try {
  174 | 			return Arrays.equals(readAllBytes(one), readAllBytes(two));
  175 | 		}
  176 | 		catch (Exception ex) {
  177 | 			if (this.logger.isDebugEnabled()) {
  178 | 				this.logger.debug("Unable to compare config tree property values", ex);
  179 | 			}
  180 | 			return false;
  181 | 		}
  182 | 	}
  183 | 
  184 | 	private byte[] readAllBytes(ConfigTreePropertySource.Value value) throws IOException {
  185 | 		try (InputStream inputStream = value.getInputStream()) {
  186 | 			return inputStream.readAllBytes();
  187 | 		}
  188 | 	}
  189 | 
  190 | 	private Map<String, Object> extract(MutablePropertySources propertySources) {
  191 | 		Map<String, Object> result = new HashMap<>();
  192 | 		List<PropertySource<?>> sources = new ArrayList<>();
  193 | 		for (PropertySource<?> source : propertySources) {
  194 | 			sources.add(0, source);
  195 | 		}
  196 | 		for (PropertySource<?> source : sources) {
  197 | 			if (!this.standardSources.contains(source.getName())) {
  198 | 				extract(source, result);
  199 | 			}
  200 | 		}
  201 | 		return result;
  202 | 	}
  203 | 
  204 | 	private void extract(PropertySource<?> propertySource, Map<String, Object> result) {
  205 | 		if (propertySource instanceof CompositePropertySource cps) {
  206 | 			try {
  207 | 				List<PropertySource<?>> sources = new ArrayList<>();
  208 | 
  209 | 				for (PropertySource<?> source : cps.getPropertySources()) {
  210 | 					sources.add(0, source);
  211 | 				}
  212 | 
  213 | 				for (PropertySource<?> source : sources) {
  214 | 					extract(source, result);
  215 | 				}
  216 | 			}
  217 | 			catch (Exception e) {
  218 | 				return;
  219 | 			}
  220 | 		}
  221 | 		else if (propertySource instanceof EnumerablePropertySource<?> eps) {
  222 | 			for (String key : eps.getPropertyNames()) {
  223 | 				result.put(key, propertySource.getProperty(key));
  224 | 			}
  225 | 		}
  226 | 	}
  227 | 
  228 | 	@Configuration(proxyBeanMethods = false)
  229 | 	protected static class Empty {
  230 | 
  231 | 	}
  232 | 
  233 | }
```
