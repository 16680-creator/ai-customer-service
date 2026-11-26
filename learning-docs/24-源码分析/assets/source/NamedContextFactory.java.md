<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-context:5.1.0 | path: org/springframework/cloud/context/named/NamedContextFactory.java -->
<!-- 正文引用：[[源码:NamedContextFactory.java#L起-L止]] ；代码块头部：(src: NamedContextFactory.java#L起-L止) -->

# NamedContextFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-context:5.1.0` |
| 路径 | `org/springframework/cloud/context/named/NamedContextFactory.java` |
| 行数 | 276 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-context/5.1.0/spring-cloud-context-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:04.059Z |

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
   17 | package org.springframework.cloud.context.named;
   18 | 
   19 | import java.lang.annotation.Annotation;
   20 | import java.util.ArrayList;
   21 | import java.util.Collection;
   22 | import java.util.Collections;
   23 | import java.util.HashMap;
   24 | import java.util.HashSet;
   25 | import java.util.List;
   26 | import java.util.Map;
   27 | import java.util.Set;
   28 | import java.util.concurrent.ConcurrentHashMap;
   29 | 
   30 | import org.jspecify.annotations.Nullable;
   31 | 
   32 | import org.springframework.aot.AotDetector;
   33 | import org.springframework.beans.BeansException;
   34 | import org.springframework.beans.factory.BeanFactoryUtils;
   35 | import org.springframework.beans.factory.DisposableBean;
   36 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   37 | import org.springframework.beans.factory.ObjectProvider;
   38 | import org.springframework.beans.factory.support.DefaultListableBeanFactory;
   39 | import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
   40 | import org.springframework.context.ApplicationContext;
   41 | import org.springframework.context.ApplicationContextAware;
   42 | import org.springframework.context.ApplicationContextInitializer;
   43 | import org.springframework.context.ConfigurableApplicationContext;
   44 | import org.springframework.context.annotation.AnnotationConfigApplicationContext;
   45 | import org.springframework.context.annotation.AnnotationConfigRegistry;
   46 | import org.springframework.context.support.GenericApplicationContext;
   47 | import org.springframework.core.ResolvableType;
   48 | import org.springframework.core.env.MapPropertySource;
   49 | import org.springframework.util.Assert;
   50 | 
   51 | /**
   52 |  * Creates a set of child contexts that allows a set of Specifications to define the beans
   53 |  * in each child context. Ported from spring-cloud-netflix FeignClientFactory and
   54 |  * SpringClientFactory
   55 |  *
   56 |  * @param <C> specification
   57 |  * @author Spencer Gibb
   58 |  * @author Dave Syer
   59 |  * @author Tommy Karlsson
   60 |  * @author Olga Maciaszek-Sharma
   61 |  */
   62 | public abstract class NamedContextFactory<C extends NamedContextFactory.Specification>
   63 | 		implements DisposableBean, ApplicationContextAware {
   64 | 
   65 | 	private final Map<String, ApplicationContextInitializer<GenericApplicationContext>> applicationContextInitializers;
   66 | 
   67 | 	private final String propertySourceName;
   68 | 
   69 | 	private final String propertyName;
   70 | 
   71 | 	private final Map<String, GenericApplicationContext> contexts = new ConcurrentHashMap<>();
   72 | 
   73 | 	private final Map<String, C> configurations = new ConcurrentHashMap<>();
   74 | 
   75 | 	private ApplicationContext parent;
   76 | 
   77 | 	private final Class<?> defaultConfigType;
   78 | 
   79 | 	public NamedContextFactory(Class<?> defaultConfigType, String propertySourceName, String propertyName) {
   80 | 		this(defaultConfigType, propertySourceName, propertyName, new HashMap<>());
   81 | 	}
   82 | 
   83 | 	public NamedContextFactory(Class<?> defaultConfigType, String propertySourceName, String propertyName,
   84 | 			Map<String, ApplicationContextInitializer<GenericApplicationContext>> applicationContextInitializers) {
   85 | 		this.defaultConfigType = defaultConfigType;
   86 | 		this.propertySourceName = propertySourceName;
   87 | 		this.propertyName = propertyName;
   88 | 		this.applicationContextInitializers = applicationContextInitializers;
   89 | 	}
   90 | 
   91 | 	@Override
   92 | 	public void setApplicationContext(ApplicationContext parent) throws BeansException {
   93 | 		this.parent = parent;
   94 | 	}
   95 | 
   96 | 	public ApplicationContext getParent() {
   97 | 		return parent;
   98 | 	}
   99 | 
  100 | 	public void setConfigurations(List<C> configurations) {
  101 | 		for (C client : configurations) {
  102 | 			this.configurations.put(client.getName(), client);
  103 | 		}
  104 | 	}
  105 | 
  106 | 	public Set<String> getContextNames() {
  107 | 		return new HashSet<>(this.contexts.keySet());
  108 | 	}
  109 | 
  110 | 	@Override
  111 | 	public void destroy() {
  112 | 		Collection<GenericApplicationContext> values = this.contexts.values();
  113 | 		for (GenericApplicationContext context : values) {
  114 | 			// This can fail, but it never throws an exception (you see stack traces
  115 | 			// logged as WARN).
  116 | 			context.close();
  117 | 		}
  118 | 		this.contexts.clear();
  119 | 	}
  120 | 
  121 | 	protected GenericApplicationContext getContext(String name) {
  122 | 		if (!this.contexts.containsKey(name)) {
  123 | 			synchronized (this.contexts) {
  124 | 				if (!this.contexts.containsKey(name)) {
  125 | 					this.contexts.put(name, createContext(name));
  126 | 				}
  127 | 			}
  128 | 		}
  129 | 		return this.contexts.get(name);
  130 | 	}
  131 | 
  132 | 	public GenericApplicationContext createContext(String name) {
  133 | 		GenericApplicationContext context = buildContext(name);
  134 | 		// there's an AOT initializer for this context
  135 | 		if (applicationContextInitializers.get(name) != null) {
  136 | 			applicationContextInitializers.get(name).initialize(context);
  137 | 			context.refresh();
  138 | 			return context;
  139 | 		}
  140 | 		registerBeans(name, context);
  141 | 		context.refresh();
  142 | 		return context;
  143 | 	}
  144 | 
  145 | 	public void registerBeans(String name, GenericApplicationContext context) {
  146 | 		Assert.isInstanceOf(AnnotationConfigRegistry.class, context);
  147 | 		AnnotationConfigRegistry registry = (AnnotationConfigRegistry) context;
  148 | 		if (this.configurations.containsKey(name)) {
  149 | 			for (Class<?> configuration : this.configurations.get(name).getConfiguration()) {
  150 | 				registry.register(configuration);
  151 | 			}
  152 | 		}
  153 | 		for (Map.Entry<String, C> entry : this.configurations.entrySet()) {
  154 | 			if (entry.getKey().startsWith("default.")) {
  155 | 				for (Class<?> configuration : entry.getValue().getConfiguration()) {
  156 | 					registry.register(configuration);
  157 | 				}
  158 | 			}
  159 | 		}
  160 | 		registry.register(PropertyPlaceholderAutoConfiguration.class, this.defaultConfigType);
  161 | 	}
  162 | 
  163 | 	public GenericApplicationContext buildContext(String name) {
  164 | 		// https://github.com/spring-cloud/spring-cloud-netflix/issues/3101
  165 | 		// https://github.com/spring-cloud/spring-cloud-openfeign/issues/475
  166 | 		ClassLoader classLoader = getClass().getClassLoader();
  167 | 		GenericApplicationContext context;
  168 | 		if (this.parent != null) {
  169 | 			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
  170 | 			if (parent instanceof ConfigurableApplicationContext) {
  171 | 				beanFactory.setBeanClassLoader(
  172 | 						((ConfigurableApplicationContext) parent).getBeanFactory().getBeanClassLoader());
  173 | 			}
  174 | 			else {
  175 | 				beanFactory.setBeanClassLoader(classLoader);
  176 | 			}
  177 | 			context = AotDetector.useGeneratedArtifacts() ? new GenericApplicationContext(beanFactory)
  178 | 					: new AnnotationConfigApplicationContext(beanFactory);
  179 | 		}
  180 | 		else {
  181 | 			context = AotDetector.useGeneratedArtifacts() ? new GenericApplicationContext()
  182 | 					: new AnnotationConfigApplicationContext();
  183 | 		}
  184 | 		context.setClassLoader(classLoader);
  185 | 		context.getEnvironment()
  186 | 			.getPropertySources()
  187 | 			.addFirst(
  188 | 					new MapPropertySource(this.propertySourceName, Collections.singletonMap(this.propertyName, name)));
  189 | 		if (this.parent != null) {
  190 | 			// Uses Environment from parent as well as beans
  191 | 			context.setParent(this.parent);
  192 | 		}
  193 | 		context.setDisplayName(generateDisplayName(name));
  194 | 		return context;
  195 | 	}
  196 | 
  197 | 	protected String generateDisplayName(String name) {
  198 | 		return this.getClass().getSimpleName() + "-" + name;
  199 | 	}
  200 | 
  201 | 	public <T> @Nullable T getInstance(String name, Class<T> type) {
  202 | 		GenericApplicationContext context = getContext(name);
  203 | 		try {
  204 | 			return context.getBean(type);
  205 | 		}
  206 | 		catch (NoSuchBeanDefinitionException e) {
  207 | 			// ignore
  208 | 		}
  209 | 		return null;
  210 | 	}
  211 | 
  212 | 	public <T> ObjectProvider<T> getLazyProvider(String name, Class<T> type) {
  213 | 		return new ClientFactoryObjectProvider<>(this, name, type);
  214 | 	}
  215 | 
  216 | 	public <T> ObjectProvider<T> getProvider(String name, Class<T> type) {
  217 | 		GenericApplicationContext context = getContext(name);
  218 | 		return context.getBeanProvider(type);
  219 | 	}
  220 | 
  221 | 	public <T> T getInstance(String name, Class<?> clazz, Class<?>... generics) {
  222 | 		ResolvableType type = ResolvableType.forClassWithGenerics(clazz, generics);
  223 | 		return getInstance(name, type);
  224 | 	}
  225 | 
  226 | 	@SuppressWarnings("unchecked")
  227 | 	public <T> T getInstance(String name, ResolvableType type) {
  228 | 		GenericApplicationContext context = getContext(name);
  229 | 		String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context, type);
  230 | 		for (String beanName : beanNames) {
  231 | 			if (context.isTypeMatch(beanName, type)) {
  232 | 				return (T) context.getBean(beanName);
  233 | 			}
  234 | 		}
  235 | 		return null;
  236 | 	}
  237 | 
  238 | 	@SuppressWarnings("unchecked")
  239 | 	public <T> T getAnnotatedInstance(String name, ResolvableType type, Class<? extends Annotation> annotationType) {
  240 | 		GenericApplicationContext context = getContext(name);
  241 | 		String[] beanNames = BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors(context, annotationType);
  242 | 
  243 | 		List<T> beans = new ArrayList<>();
  244 | 		for (String beanName : beanNames) {
  245 | 			if (context.isTypeMatch(beanName, type)) {
  246 | 				beans.add((T) context.getBean(beanName));
  247 | 			}
  248 | 		}
  249 | 		if (beans.size() > 1) {
  250 | 			throw new IllegalStateException("Only one annotated bean for type expected.");
  251 | 		}
  252 | 		return beans.isEmpty() ? null : beans.get(0);
  253 | 	}
  254 | 
  255 | 	public <T> Map<String, T> getInstances(String name, Class<T> type) {
  256 | 		GenericApplicationContext context = getContext(name);
  257 | 
  258 | 		return BeanFactoryUtils.beansOfTypeIncludingAncestors(context, type);
  259 | 	}
  260 | 
  261 | 	public Map<String, C> getConfigurations() {
  262 | 		return configurations;
  263 | 	}
  264 | 
  265 | 	/**
  266 | 	 * Specification with name and configuration.
  267 | 	 */
  268 | 	public interface Specification {
  269 | 
  270 | 		String getName();
  271 | 
  272 | 		Class<?>[] getConfiguration();
  273 | 
  274 | 	}
  275 | 
  276 | }
```
