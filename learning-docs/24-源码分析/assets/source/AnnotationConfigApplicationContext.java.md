<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-context:7.1.0 | path: org/springframework/context/annotation/AnnotationConfigApplicationContext.java -->
<!-- 正文引用：[[源码:AnnotationConfigApplicationContext.java#L起-L止]] ；代码块头部：(src: AnnotationConfigApplicationContext.java#L起-L止) -->

# AnnotationConfigApplicationContext.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-context:7.1.0` |
| 路径 | `org/springframework/context/annotation/AnnotationConfigApplicationContext.java` |
| 行数 | 206 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-context/7.1.0/spring-context-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:56:19.111Z |

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
   19 | import java.util.Arrays;
   20 | import java.util.function.Supplier;
   21 | 
   22 | import org.jspecify.annotations.Nullable;
   23 | 
   24 | import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
   25 | import org.springframework.beans.factory.support.BeanNameGenerator;
   26 | import org.springframework.beans.factory.support.DefaultListableBeanFactory;
   27 | import org.springframework.context.support.GenericApplicationContext;
   28 | import org.springframework.core.env.ConfigurableEnvironment;
   29 | import org.springframework.core.metrics.StartupStep;
   30 | import org.springframework.util.Assert;
   31 | 
   32 | /**
   33 |  * Standalone application context, accepting <em>component classes</em> as input &mdash;
   34 |  * in particular {@link Configuration @Configuration}-annotated classes, but also plain
   35 |  * {@link org.springframework.stereotype.Component @Component} types and JSR-330 compliant
   36 |  * classes using {@code jakarta.inject} annotations.
   37 |  *
   38 |  * <p>Allows for registering classes one by one using {@link #register(Class...)}
   39 |  * as well as for classpath scanning using {@link #scan(String...)}.
   40 |  *
   41 |  * <p>In case of multiple {@code @Configuration} classes, {@link Bean @Bean} methods
   42 |  * defined in later classes will override those defined in earlier classes. This can
   43 |  * be leveraged to deliberately override certain bean definitions via an extra
   44 |  * {@code @Configuration} class.
   45 |  *
   46 |  * <p>See {@link Configuration @Configuration}'s javadoc for usage examples.
   47 |  *
   48 |  * @author Juergen Hoeller
   49 |  * @author Chris Beams
   50 |  * @since 3.0
   51 |  * @see #register
   52 |  * @see #scan
   53 |  * @see AnnotatedBeanDefinitionReader
   54 |  * @see ClassPathBeanDefinitionScanner
   55 |  * @see org.springframework.context.support.GenericXmlApplicationContext
   56 |  */
   57 | public class AnnotationConfigApplicationContext extends GenericApplicationContext implements AnnotationConfigRegistry {
   58 | 
   59 | 	private final AnnotatedBeanDefinitionReader reader;
   60 | 
   61 | 	private final ClassPathBeanDefinitionScanner scanner;
   62 | 
   63 | 
   64 | 	/**
   65 | 	 * Create a new AnnotationConfigApplicationContext that needs to be populated
   66 | 	 * through {@link #register} calls and then manually {@linkplain #refresh refreshed}.
   67 | 	 */
   68 | 	public AnnotationConfigApplicationContext() {
   69 | 		this.reader = new AnnotatedBeanDefinitionReader(this);
   70 | 		this.scanner = new ClassPathBeanDefinitionScanner(this);
   71 | 	}
   72 | 
   73 | 	/**
   74 | 	 * Create a new AnnotationConfigApplicationContext with the given DefaultListableBeanFactory.
   75 | 	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
   76 | 	 */
   77 | 	public AnnotationConfigApplicationContext(DefaultListableBeanFactory beanFactory) {
   78 | 		super(beanFactory);
   79 | 		this.reader = new AnnotatedBeanDefinitionReader(this);
   80 | 		this.scanner = new ClassPathBeanDefinitionScanner(this);
   81 | 	}
   82 | 
   83 | 	/**
   84 | 	 * Create a new AnnotationConfigApplicationContext, deriving bean definitions
   85 | 	 * from the given component classes and automatically refreshing the context.
   86 | 	 * @param componentClasses one or more component classes &mdash; for example,
   87 | 	 * {@link Configuration @Configuration} classes
   88 | 	 */
   89 | 	public AnnotationConfigApplicationContext(Class<?>... componentClasses) {
   90 | 		this();
   91 | 		register(componentClasses);
   92 | 		refresh();
   93 | 	}
   94 | 
   95 | 	/**
   96 | 	 * Create a new AnnotationConfigApplicationContext, scanning for components
   97 | 	 * in the given packages, registering bean definitions for those components,
   98 | 	 * and automatically refreshing the context.
   99 | 	 * @param basePackages the packages to scan for component classes
  100 | 	 */
  101 | 	public AnnotationConfigApplicationContext(String... basePackages) {
  102 | 		this();
  103 | 		scan(basePackages);
  104 | 		refresh();
  105 | 	}
  106 | 
  107 | 
  108 | 	/**
  109 | 	 * Propagate the given custom {@code Environment} to the underlying
  110 | 	 * {@link AnnotatedBeanDefinitionReader} and {@link ClassPathBeanDefinitionScanner}.
  111 | 	 */
  112 | 	@Override
  113 | 	public void setEnvironment(ConfigurableEnvironment environment) {
  114 | 		super.setEnvironment(environment);
  115 | 		this.reader.setEnvironment(environment);
  116 | 		this.scanner.setEnvironment(environment);
  117 | 	}
  118 | 
  119 | 	/**
  120 | 	 * Provide a custom {@link BeanNameGenerator} for use with {@link AnnotatedBeanDefinitionReader}
  121 | 	 * and/or {@link ClassPathBeanDefinitionScanner}.
  122 | 	 * <p>Default is {@code AnnotationBeanNameGenerator}.
  123 | 	 * <p>When processing {@link Configuration @Configuration} classes, a
  124 | 	 * {@link ConfigurationBeanNameGenerator} (such as
  125 | 	 * {@link FullyQualifiedConfigurationBeanNameGenerator}) also determines the
  126 | 	 * default names for {@link Bean @Bean} methods without an explicit {@code name}
  127 | 	 * attribute.
  128 | 	 * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
  129 | 	 * and/or {@link #scan(String...)}.
  130 | 	 * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
  131 | 	 * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
  132 | 	 * @see AnnotationBeanNameGenerator
  133 | 	 * @see FullyQualifiedAnnotationBeanNameGenerator
  134 | 	 * @see FullyQualifiedConfigurationBeanNameGenerator
  135 | 	 */
  136 | 	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
  137 | 		this.reader.setBeanNameGenerator(beanNameGenerator);
  138 | 		this.scanner.setBeanNameGenerator(beanNameGenerator);
  139 | 		getBeanFactory().registerSingleton(
  140 | 				AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, beanNameGenerator);
  141 | 	}
  142 | 
  143 | 	/**
  144 | 	 * Set the {@link ScopeMetadataResolver} to use for registered component classes.
  145 | 	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
  146 | 	 * <p>Any call to this method must occur prior to calls to {@link #register(Class...)}
  147 | 	 * and/or {@link #scan(String...)}.
  148 | 	 */
  149 | 	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
  150 | 		this.reader.setScopeMetadataResolver(scopeMetadataResolver);
  151 | 		this.scanner.setScopeMetadataResolver(scopeMetadataResolver);
  152 | 	}
  153 | 
  154 | 
  155 | 	//---------------------------------------------------------------------
  156 | 	// Implementation of AnnotationConfigRegistry
  157 | 	//---------------------------------------------------------------------
  158 | 
  159 | 	/**
  160 | 	 * Register one or more component classes to be processed.
  161 | 	 * <p>Note that {@link #refresh()} must be called in order for the context
  162 | 	 * to fully process the new classes.
  163 | 	 * @param componentClasses one or more component classes &mdash; for example,
  164 | 	 * {@link Configuration @Configuration} classes
  165 | 	 * @see #scan(String...)
  166 | 	 * @see #refresh()
  167 | 	 */
  168 | 	@Override
  169 | 	public void register(Class<?>... componentClasses) {
  170 | 		Assert.notEmpty(componentClasses, "At least one component class must be specified");
  171 | 		StartupStep registerComponentClass = getApplicationStartup().start("spring.context.component-classes.register")
  172 | 				.tag("classes", () -> Arrays.toString(componentClasses));
  173 | 		this.reader.register(componentClasses);
  174 | 		registerComponentClass.end();
  175 | 	}
  176 | 
  177 | 	/**
  178 | 	 * Perform a scan within the specified base packages.
  179 | 	 * <p>Note that {@link #refresh()} must be called in order for the context
  180 | 	 * to fully process the new classes.
  181 | 	 * @param basePackages the packages to scan for component classes
  182 | 	 * @see #register(Class...)
  183 | 	 * @see #refresh()
  184 | 	 */
  185 | 	@Override
  186 | 	public void scan(String... basePackages) {
  187 | 		Assert.notEmpty(basePackages, "At least one base package must be specified");
  188 | 		StartupStep scanPackages = getApplicationStartup().start("spring.context.base-packages.scan")
  189 | 				.tag("packages", () -> Arrays.toString(basePackages));
  190 | 		this.scanner.scan(basePackages);
  191 | 		scanPackages.end();
  192 | 	}
  193 | 
  194 | 
  195 | 	//---------------------------------------------------------------------
  196 | 	// Adapt superclass registerBean calls to AnnotatedBeanDefinitionReader
  197 | 	//---------------------------------------------------------------------
  198 | 
  199 | 	@Override
  200 | 	public <T> void registerBean(@Nullable String beanName, Class<T> beanClass,
  201 | 			@Nullable Supplier<T> supplier, BeanDefinitionCustomizer... customizers) {
  202 | 
  203 | 		this.reader.registerBean(beanClass, beanName, supplier, customizers);
  204 | 	}
  205 | 
  206 | }
```
