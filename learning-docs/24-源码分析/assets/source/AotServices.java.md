<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-beans:7.1.0 | path: org/springframework/beans/factory/aot/AotServices.java -->
<!-- 正文引用：[[源码:AotServices.java#L起-L止]] ；代码块头部：(src: AotServices.java#L起-L止) -->

# AotServices.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-beans:7.1.0` |
| 路径 | `org/springframework/beans/factory/aot/AotServices.java` |
| 行数 | 238 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-beans/7.1.0/spring-beans-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:29.461Z |

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
   17 | package org.springframework.beans.factory.aot;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.Collection;
   21 | import java.util.Collections;
   22 | import java.util.IdentityHashMap;
   23 | import java.util.Iterator;
   24 | import java.util.List;
   25 | import java.util.Map;
   26 | import java.util.stream.Stream;
   27 | 
   28 | import org.jspecify.annotations.Nullable;
   29 | 
   30 | import org.springframework.beans.factory.BeanFactory;
   31 | import org.springframework.beans.factory.BeanFactoryUtils;
   32 | import org.springframework.beans.factory.ListableBeanFactory;
   33 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   34 | import org.springframework.core.annotation.AnnotationAwareOrderComparator;
   35 | import org.springframework.core.io.support.SpringFactoriesLoader;
   36 | import org.springframework.util.Assert;
   37 | import org.springframework.util.ObjectUtils;
   38 | 
   39 | /**
   40 |  * A collection of AOT services that can be {@link Loader loaded} from
   41 |  * a {@link SpringFactoriesLoader} or obtained from a {@link ListableBeanFactory}.
   42 |  *
   43 |  * @author Phillip Webb
   44 |  * @since 6.0
   45 |  * @param <T> the service type
   46 |  */
   47 | public final class AotServices<T> implements Iterable<T> {
   48 | 
   49 | 	/**
   50 | 	 * The location to look for AOT factories.
   51 | 	 */
   52 | 	public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring/aot.factories";
   53 | 
   54 | 	private final List<T> services;
   55 | 
   56 | 	private final Map<String, T> beans;
   57 | 
   58 | 	private final Map<T, Source> sources;
   59 | 
   60 | 
   61 | 	private AotServices(List<T> loaded, Map<String, T> beans) {
   62 | 		this.services = collectServices(loaded, beans);
   63 | 		this.sources = collectSources(loaded, beans.values());
   64 | 		this.beans = beans;
   65 | 	}
   66 | 
   67 | 	private List<T> collectServices(List<T> loaded, Map<String, T> beans) {
   68 | 		List<T> services = new ArrayList<>();
   69 | 		services.addAll(beans.values());
   70 | 		services.addAll(loaded);
   71 | 		AnnotationAwareOrderComparator.sort(services);
   72 | 		return Collections.unmodifiableList(services);
   73 | 	}
   74 | 
   75 | 	private Map<T, Source> collectSources(Collection<T> loaded, Collection<T> beans) {
   76 | 		Map<T, Source> sources = new IdentityHashMap<>();
   77 | 		loaded.forEach(service -> sources.put(service, Source.SPRING_FACTORIES_LOADER));
   78 | 		beans.forEach(service -> sources.put(service, Source.BEAN_FACTORY));
   79 | 		return Collections.unmodifiableMap(sources);
   80 | 	}
   81 | 
   82 | 	/**
   83 | 	 * Create a new {@link Loader} that will obtain AOT services from
   84 | 	 * {@value #FACTORIES_RESOURCE_LOCATION}.
   85 | 	 * @return a new {@link Loader} instance
   86 | 	 */
   87 | 	public static Loader factories() {
   88 | 		return factories((ClassLoader) null);
   89 | 	}
   90 | 
   91 | 	/**
   92 | 	 * Create a new {@link Loader} that will obtain AOT services from
   93 | 	 * {@value #FACTORIES_RESOURCE_LOCATION}.
   94 | 	 * @param classLoader the class loader used to load the factories resource
   95 | 	 * @return a new {@link Loader} instance
   96 | 	 */
   97 | 	public static Loader factories(@Nullable ClassLoader classLoader) {
   98 | 		return factories(getSpringFactoriesLoader(classLoader));
   99 | 	}
  100 | 
  101 | 	/**
  102 | 	 * Create a new {@link Loader} that will obtain AOT services from the given
  103 | 	 * {@link SpringFactoriesLoader}.
  104 | 	 * @param springFactoriesLoader the spring factories loader
  105 | 	 * @return a new {@link Loader} instance
  106 | 	 */
  107 | 	public static Loader factories(SpringFactoriesLoader springFactoriesLoader) {
  108 | 		Assert.notNull(springFactoriesLoader, "'springFactoriesLoader' must not be null");
  109 | 		return new Loader(springFactoriesLoader, null);
  110 | 	}
  111 | 
  112 | 	/**
  113 | 	 * Create a new {@link Loader} that will obtain AOT services from
  114 | 	 * {@value #FACTORIES_RESOURCE_LOCATION} as well as the given
  115 | 	 * {@link ListableBeanFactory}.
  116 | 	 * @param beanFactory the bean factory
  117 | 	 * @return a new {@link Loader} instance
  118 | 	 */
  119 | 	public static Loader factoriesAndBeans(ListableBeanFactory beanFactory) {
  120 | 		ClassLoader classLoader = (beanFactory instanceof ConfigurableBeanFactory configurableBeanFactory ?
  121 | 				configurableBeanFactory.getBeanClassLoader() : null);
  122 | 		return factoriesAndBeans(getSpringFactoriesLoader(classLoader), beanFactory);
  123 | 	}
  124 | 
  125 | 	/**
  126 | 	 * Create a new {@link Loader} that will obtain AOT services from the given
  127 | 	 * {@link SpringFactoriesLoader} and {@link ListableBeanFactory}.
  128 | 	 * @param springFactoriesLoader the spring factories loader
  129 | 	 * @param beanFactory the bean factory
  130 | 	 * @return a new {@link Loader} instance
  131 | 	 */
  132 | 	public static Loader factoriesAndBeans(SpringFactoriesLoader springFactoriesLoader, ListableBeanFactory beanFactory) {
  133 | 		Assert.notNull(beanFactory, "'beanFactory' must not be null");
  134 | 		Assert.notNull(springFactoriesLoader, "'springFactoriesLoader' must not be null");
  135 | 		return new Loader(springFactoriesLoader, beanFactory);
  136 | 	}
  137 | 
  138 | 	private static SpringFactoriesLoader getSpringFactoriesLoader(
  139 | 			@Nullable ClassLoader classLoader) {
  140 | 		return SpringFactoriesLoader.forResourceLocation(FACTORIES_RESOURCE_LOCATION,
  141 | 				classLoader);
  142 | 	}
  143 | 
  144 | 	@Override
  145 | 	public Iterator<T> iterator() {
  146 | 		return this.services.iterator();
  147 | 	}
  148 | 
  149 | 	/**
  150 | 	 * Return a {@link Stream} of the AOT services.
  151 | 	 * @return a stream of the services
  152 | 	 */
  153 | 	public Stream<T> stream() {
  154 | 		return this.services.stream();
  155 | 	}
  156 | 
  157 | 	/**
  158 | 	 * Return the AOT services as a {@link List}.
  159 | 	 * @return a list of the services
  160 | 	 */
  161 | 	public List<T> asList() {
  162 | 		return this.services;
  163 | 	}
  164 | 
  165 | 	/**
  166 | 	 * Find the AOT service that was loaded for the given bean name.
  167 | 	 * @param beanName the bean name
  168 | 	 * @return the AOT service or {@code null}
  169 | 	 */
  170 | 	public @Nullable T findByBeanName(String beanName) {
  171 | 		return this.beans.get(beanName);
  172 | 	}
  173 | 
  174 | 	/**
  175 | 	 * Get the source of the given service.
  176 | 	 * @param service the service instance
  177 | 	 * @return the source of the service
  178 | 	 */
  179 | 	public Source getSource(T service) {
  180 | 		Source source = this.sources.get(service);
  181 | 		Assert.state(source != null,
  182 | 				() -> "Unable to find service " + ObjectUtils.identityToString(source));
  183 | 		return source;
  184 | 	}
  185 | 
  186 | 
  187 | 	/**
  188 | 	 * Loader class used to actually load the services.
  189 | 	 */
  190 | 	public static class Loader {
  191 | 
  192 | 		private final SpringFactoriesLoader springFactoriesLoader;
  193 | 
  194 | 		private final @Nullable ListableBeanFactory beanFactory;
  195 | 
  196 | 
  197 | 		Loader(SpringFactoriesLoader springFactoriesLoader, @Nullable ListableBeanFactory beanFactory) {
  198 | 			this.springFactoriesLoader = springFactoriesLoader;
  199 | 			this.beanFactory = beanFactory;
  200 | 		}
  201 | 
  202 | 
  203 | 		/**
  204 | 		 * Load all AOT services of the given type.
  205 | 		 * @param <T> the service type
  206 | 		 * @param type the service type
  207 | 		 * @return a new {@link AotServices} instance
  208 | 		 */
  209 | 		public <T> AotServices<T> load(Class<T> type) {
  210 | 			return new AotServices<>(this.springFactoriesLoader.load(type), loadBeans(type));
  211 | 		}
  212 | 
  213 | 		private <T> Map<String, T> loadBeans(Class<T> type) {
  214 | 			return (this.beanFactory != null ?
  215 | 					BeanFactoryUtils.beansOfTypeIncludingAncestors(this.beanFactory, type, true, false) :
  216 | 					Collections.emptyMap());
  217 | 		}
  218 | 
  219 | 	}
  220 | 
  221 | 	/**
  222 | 	 * Sources from which services were obtained.
  223 | 	 */
  224 | 	public enum Source {
  225 | 
  226 | 		/**
  227 | 		 * An AOT service loaded from {@link SpringFactoriesLoader}.
  228 | 		 */
  229 | 		SPRING_FACTORIES_LOADER,
  230 | 
  231 | 		/**
  232 | 		 * An AOT service loaded from a {@link BeanFactory}.
  233 | 		 */
  234 | 		BEAN_FACTORY
  235 | 
  236 | 	}
  237 | 
  238 | }
```
