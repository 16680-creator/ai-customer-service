<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/annotation/ConfigurationClass.java -->
<!-- 正文引用：[[源码:ConfigurationClass.java#L起-L止]] ；代码块头部：(src: ConfigurationClass.java#L起-L止) -->

# ConfigurationClass.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/annotation/ConfigurationClass.java` |
| 行数 | 314 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:21.167Z |

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
   17 | package org.springframework.context.annotation;
   18 | 
   19 | import java.util.HashSet;
   20 | import java.util.LinkedHashMap;
   21 | import java.util.LinkedHashSet;
   22 | import java.util.Map;
   23 | import java.util.Set;
   24 | 
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.beans.factory.BeanRegistrar;
   28 | import org.springframework.beans.factory.parsing.Location;
   29 | import org.springframework.beans.factory.parsing.Problem;
   30 | import org.springframework.beans.factory.parsing.ProblemReporter;
   31 | import org.springframework.beans.factory.support.BeanDefinitionReader;
   32 | import org.springframework.core.io.DescriptiveResource;
   33 | import org.springframework.core.io.Resource;
   34 | import org.springframework.core.type.AnnotationMetadata;
   35 | import org.springframework.core.type.MethodMetadata;
   36 | import org.springframework.core.type.classreading.MetadataReader;
   37 | import org.springframework.util.Assert;
   38 | import org.springframework.util.ClassUtils;
   39 | import org.springframework.util.LinkedMultiValueMap;
   40 | import org.springframework.util.MultiValueMap;
   41 | 
   42 | /**
   43 |  * Represents a user-defined {@link Configuration @Configuration} class.
   44 |  * <p>Includes a set of {@link Bean} methods, including all such methods
   45 |  * defined in the ancestry of the class, in a 'flattened-out' manner.
   46 |  *
   47 |  * @author Chris Beams
   48 |  * @author Juergen Hoeller
   49 |  * @author Phillip Webb
   50 |  * @since 3.0
   51 |  * @see BeanMethod
   52 |  * @see ConfigurationClassParser
   53 |  */
   54 | final class ConfigurationClass {
   55 | 
   56 | 	private final AnnotationMetadata metadata;
   57 | 
   58 | 	private final Resource resource;
   59 | 
   60 | 	private @Nullable String beanName;
   61 | 
   62 | 	private boolean scanned = false;
   63 | 
   64 | 	private final Set<ConfigurationClass> importedBy = new LinkedHashSet<>(1);
   65 | 
   66 | 	private final Set<BeanMethod> beanMethods = new LinkedHashSet<>();
   67 | 
   68 | 	private final Map<String, Class<? extends BeanDefinitionReader>> importedResources =
   69 | 			new LinkedHashMap<>();
   70 | 
   71 | 	private final MultiValueMap<String, BeanRegistrar> beanRegistrars = new LinkedMultiValueMap<>();
   72 | 
   73 | 	private final Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> importBeanDefinitionRegistrars =
   74 | 			new LinkedHashMap<>();
   75 | 
   76 | 	final Set<String> skippedBeanMethods = new HashSet<>();
   77 | 
   78 | 
   79 | 	/**
   80 | 	 * Create a new {@link ConfigurationClass} with the given name.
   81 | 	 * @param metadataReader reader used to parse the underlying {@link Class}
   82 | 	 * @param beanName must not be {@code null}
   83 | 	 */
   84 | 	ConfigurationClass(MetadataReader metadataReader, String beanName) {
   85 | 		Assert.notNull(beanName, "Bean name must not be null");
   86 | 		this.metadata = metadataReader.getAnnotationMetadata();
   87 | 		this.resource = metadataReader.getResource();
   88 | 		this.beanName = beanName;
   89 | 	}
   90 | 
   91 | 	/**
   92 | 	 * Create a new {@link ConfigurationClass} representing a class that was imported
   93 | 	 * using the {@link Import} annotation or automatically processed as a nested
   94 | 	 * configuration class (if importedBy is not {@code null}).
   95 | 	 * @param metadataReader reader used to parse the underlying {@link Class}
   96 | 	 * @param importedBy the configuration class importing this one
   97 | 	 * @since 3.1.1
   98 | 	 */
   99 | 	ConfigurationClass(MetadataReader metadataReader, ConfigurationClass importedBy) {
  100 | 		this.metadata = metadataReader.getAnnotationMetadata();
  101 | 		this.resource = metadataReader.getResource();
  102 | 		this.importedBy.add(importedBy);
  103 | 	}
  104 | 
  105 | 	/**
  106 | 	 * Create a new {@link ConfigurationClass} with the given name.
  107 | 	 * @param clazz the underlying {@link Class} to represent
  108 | 	 * @param beanName name of the {@code @Configuration} class bean
  109 | 	 */
  110 | 	ConfigurationClass(Class<?> clazz, String beanName) {
  111 | 		Assert.notNull(beanName, "Bean name must not be null");
  112 | 		this.metadata = AnnotationMetadata.introspect(clazz);
  113 | 		this.resource = new DescriptiveResource(clazz.getName());
  114 | 		this.beanName = beanName;
  115 | 	}
  116 | 
  117 | 	/**
  118 | 	 * Create a new {@link ConfigurationClass} representing a class that was imported
  119 | 	 * using the {@link Import} annotation or automatically processed as a nested
  120 | 	 * configuration class (if imported is {@code true}).
  121 | 	 * @param clazz the underlying {@link Class} to represent
  122 | 	 * @param importedBy the configuration class importing this one
  123 | 	 * @since 3.1.1
  124 | 	 */
  125 | 	ConfigurationClass(Class<?> clazz, ConfigurationClass importedBy) {
  126 | 		this.metadata = AnnotationMetadata.introspect(clazz);
  127 | 		this.resource = new DescriptiveResource(clazz.getName());
  128 | 		this.importedBy.add(importedBy);
  129 | 	}
  130 | 
  131 | 	/**
  132 | 	 * Create a new {@link ConfigurationClass} with the given name.
  133 | 	 * @param metadata the metadata for the underlying class to represent
  134 | 	 * @param beanName name of the {@code @Configuration} class bean
  135 | 	 * @param scanned whether the underlying class has been registered through a scan
  136 | 	 * @since 6.2
  137 | 	 */
  138 | 	ConfigurationClass(AnnotationMetadata metadata, String beanName, boolean scanned) {
  139 | 		Assert.notNull(beanName, "Bean name must not be null");
  140 | 		this.metadata = metadata;
  141 | 		this.resource = new DescriptiveResource(metadata.getClassName());
  142 | 		this.beanName = beanName;
  143 | 		this.scanned = scanned;
  144 | 	}
  145 | 
  146 | 
  147 | 	AnnotationMetadata getMetadata() {
  148 | 		return this.metadata;
  149 | 	}
  150 | 
  151 | 	Resource getResource() {
  152 | 		return this.resource;
  153 | 	}
  154 | 
  155 | 	String getSimpleName() {
  156 | 		return ClassUtils.getShortName(getMetadata().getClassName());
  157 | 	}
  158 | 
  159 | 	void setBeanName(@Nullable String beanName) {
  160 | 		this.beanName = beanName;
  161 | 	}
  162 | 
  163 | 	@Nullable String getBeanName() {
  164 | 		return this.beanName;
  165 | 	}
  166 | 
  167 | 	/**
  168 | 	 * Return whether this configuration class has been registered through a scan.
  169 | 	 * @since 6.2
  170 | 	 */
  171 | 	boolean isScanned() {
  172 | 		return this.scanned;
  173 | 	}
  174 | 
  175 | 	/**
  176 | 	 * Return whether this configuration class was registered via @{@link Import} or
  177 | 	 * automatically registered due to being nested within another configuration class.
  178 | 	 * @since 3.1.1
  179 | 	 * @see #getImportedBy()
  180 | 	 */
  181 | 	boolean isImported() {
  182 | 		return !this.importedBy.isEmpty();
  183 | 	}
  184 | 
  185 | 	/**
  186 | 	 * Merge the imported-by declarations from the given configuration class into this one.
  187 | 	 * @since 4.0.5
  188 | 	 */
  189 | 	void mergeImportedBy(ConfigurationClass otherConfigClass) {
  190 | 		this.importedBy.addAll(otherConfigClass.importedBy);
  191 | 	}
  192 | 
  193 | 	/**
  194 | 	 * Return the configuration classes that imported this class,
  195 | 	 * or an empty Set if this configuration was not imported.
  196 | 	 * @since 4.0.5
  197 | 	 * @see #isImported()
  198 | 	 */
  199 | 	Set<ConfigurationClass> getImportedBy() {
  200 | 		return this.importedBy;
  201 | 	}
  202 | 
  203 | 	void addBeanMethod(BeanMethod method) {
  204 | 		this.beanMethods.add(method);
  205 | 	}
  206 | 
  207 | 	Set<BeanMethod> getBeanMethods() {
  208 | 		return this.beanMethods;
  209 | 	}
  210 | 
  211 | 	boolean hasNonStaticBeanMethods() {
  212 | 		for (BeanMethod beanMethod : this.beanMethods) {
  213 | 			if (!beanMethod.getMetadata().isStatic()) {
  214 | 				return true;
  215 | 			}
  216 | 		}
  217 | 		return false;
  218 | 	}
  219 | 
  220 | 	void addImportedResource(String importedResource, Class<? extends BeanDefinitionReader> readerClass) {
  221 | 		this.importedResources.put(importedResource, readerClass);
  222 | 	}
  223 | 
  224 | 	Map<String, Class<? extends BeanDefinitionReader>> getImportedResources() {
  225 | 		return this.importedResources;
  226 | 	}
  227 | 
  228 | 	void addBeanRegistrar(String sourceClassName, BeanRegistrar beanRegistrar) {
  229 | 		this.beanRegistrars.add(sourceClassName, beanRegistrar);
  230 | 	}
  231 | 
  232 | 	public MultiValueMap<String, BeanRegistrar> getBeanRegistrars() {
  233 | 		return this.beanRegistrars;
  234 | 	}
  235 | 
  236 | 	void addImportBeanDefinitionRegistrar(ImportBeanDefinitionRegistrar registrar, AnnotationMetadata importingClassMetadata) {
  237 | 		this.importBeanDefinitionRegistrars.put(registrar, importingClassMetadata);
  238 | 	}
  239 | 
  240 | 	Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> getImportBeanDefinitionRegistrars() {
  241 | 		return this.importBeanDefinitionRegistrars;
  242 | 	}
  243 | 
  244 | 	@SuppressWarnings("NullAway") // Reflection
  245 | 	void validate(ProblemReporter problemReporter) {
  246 | 		Map<String, @Nullable Object> attributes = this.metadata.getAnnotationAttributes(Configuration.class.getName());
  247 | 
  248 | 		// A configuration class may not be final (CGLIB limitation) unless it does not have to proxy bean methods
  249 | 		if (attributes != null && (Boolean) attributes.get("proxyBeanMethods") && hasNonStaticBeanMethods() &&
  250 | 				this.metadata.isFinal()) {
  251 | 			problemReporter.error(new FinalConfigurationProblem());
  252 | 		}
  253 | 
  254 | 		for (BeanMethod beanMethod : this.beanMethods) {
  255 | 			beanMethod.validate(problemReporter);
  256 | 		}
  257 | 
  258 | 		// A configuration class may not contain overloaded bean methods unless it declares enforceUniqueMethods=false
  259 | 		if (attributes != null && (Boolean) attributes.get("enforceUniqueMethods")) {
  260 | 			Map<String, MethodMetadata> beanMethodsByName = new LinkedHashMap<>();
  261 | 			for (BeanMethod beanMethod : this.beanMethods) {
  262 | 				MethodMetadata current = beanMethod.getMetadata();
  263 | 				MethodMetadata existing = beanMethodsByName.put(current.getMethodName(), current);
  264 | 				if (existing != null && existing.getDeclaringClassName().equals(current.getDeclaringClassName())) {
  265 | 					problemReporter.error(new BeanMethodOverloadingProblem(existing.getMethodName()));
  266 | 				}
  267 | 			}
  268 | 		}
  269 | 	}
  270 | 
  271 | 	@Override
  272 | 	public boolean equals(@Nullable Object other) {
  273 | 		return (this == other || (other instanceof ConfigurationClass that &&
  274 | 				getMetadata().getClassName().equals(that.getMetadata().getClassName())));
  275 | 	}
  276 | 
  277 | 	@Override
  278 | 	public int hashCode() {
  279 | 		return getMetadata().getClassName().hashCode();
  280 | 	}
  281 | 
  282 | 	@Override
  283 | 	public String toString() {
  284 | 		return "ConfigurationClass: beanName '" + this.beanName + "', " + this.resource;
  285 | 	}
  286 | 
  287 | 
  288 | 	/**
  289 | 	 * Configuration classes must be non-final to accommodate CGLIB subclassing.
  290 | 	 */
  291 | 	private class FinalConfigurationProblem extends Problem {
  292 | 
  293 | 		FinalConfigurationProblem() {
  294 | 			super(String.format("@Configuration class '%s' may not be final. Remove the final modifier to continue.",
  295 | 					getSimpleName()), new Location(getResource(), getMetadata()));
  296 | 		}
  297 | 	}
  298 | 
  299 | 
  300 | 	/**
  301 | 	 * Configuration classes are not allowed to contain overloaded bean methods
  302 | 	 * by default (as of 6.0).
  303 | 	 */
  304 | 	private class BeanMethodOverloadingProblem extends Problem {
  305 | 
  306 | 		BeanMethodOverloadingProblem(String methodName) {
  307 | 			super(String.format("@Configuration class '%s' contains overloaded @Bean methods with name '%s'. Use " +
  308 | 							"unique method names for separate bean definitions (with individual conditions etc) " +
  309 | 							"or switch '@Configuration.enforceUniqueMethods' to 'false'.",
  310 | 					getSimpleName(), methodName), new Location(getResource(), getMetadata()));
  311 | 		}
  312 | 	}
  313 | 
  314 | }
```
