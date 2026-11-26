<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis-spring:3.0.5 | path: org/mybatis/spring/mapper/MapperScannerConfigurer.java -->
<!-- 正文引用：[[源码:MapperScannerConfigurer.java#L起-L止]] ；代码块头部：(src: MapperScannerConfigurer.java#L起-L止) -->

# MapperScannerConfigurer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis-spring:3.0.5` |
| 路径 | `org/mybatis/spring/mapper/MapperScannerConfigurer.java` |
| 行数 | 542 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis-spring/3.0.5/mybatis-spring-3.0.5-sources.jar |
| 抓取时间 | 2026-11-25T15:01:25.722Z |

```java
    1 | /*
    2 |  * Copyright 2010-2024 the original author or authors.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *    https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | package org.mybatis.spring.mapper;
   17 | 
   18 | import static org.springframework.util.Assert.notNull;
   19 | 
   20 | import java.lang.annotation.Annotation;
   21 | import java.util.ArrayList;
   22 | import java.util.List;
   23 | import java.util.Map;
   24 | import java.util.Optional;
   25 | import java.util.regex.Pattern;
   26 | 
   27 | import org.apache.ibatis.session.SqlSessionFactory;
   28 | import org.mybatis.spring.SqlSessionTemplate;
   29 | import org.springframework.beans.BeanUtils;
   30 | import org.springframework.beans.PropertyValues;
   31 | import org.springframework.beans.factory.BeanNameAware;
   32 | import org.springframework.beans.factory.InitializingBean;
   33 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   34 | import org.springframework.beans.factory.config.PropertyResourceConfigurer;
   35 | import org.springframework.beans.factory.config.TypedStringValue;
   36 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   37 | import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
   38 | import org.springframework.beans.factory.support.BeanNameGenerator;
   39 | import org.springframework.beans.factory.support.DefaultListableBeanFactory;
   40 | import org.springframework.context.ApplicationContext;
   41 | import org.springframework.context.ApplicationContextAware;
   42 | import org.springframework.context.ConfigurableApplicationContext;
   43 | import org.springframework.core.env.Environment;
   44 | import org.springframework.core.type.filter.AnnotationTypeFilter;
   45 | import org.springframework.core.type.filter.AspectJTypeFilter;
   46 | import org.springframework.core.type.filter.AssignableTypeFilter;
   47 | import org.springframework.core.type.filter.RegexPatternTypeFilter;
   48 | import org.springframework.core.type.filter.TypeFilter;
   49 | import org.springframework.lang.Nullable;
   50 | import org.springframework.util.ClassUtils;
   51 | import org.springframework.util.StringUtils;
   52 | 
   53 | /**
   54 |  * BeanDefinitionRegistryPostProcessor that searches recursively starting from a base package for interfaces and
   55 |  * registers them as {@code MapperFactoryBean}. Note that only interfaces with at least one method will be registered;
   56 |  * concrete classes will be ignored.
   57 |  * <p>
   58 |  * This class was a {code BeanFactoryPostProcessor} until 1.0.1 version. It changed to
   59 |  * {@code BeanDefinitionRegistryPostProcessor} in 1.0.2. See https://jira.springsource.org/browse/SPR-8269 for the
   60 |  * details.
   61 |  * <p>
   62 |  * The {@code basePackage} property can contain more than one package name, separated by either commas or semicolons.
   63 |  * <p>
   64 |  * This class supports filtering the mappers created by either specifying a marker interface or an annotation. The
   65 |  * {@code annotationClass} property specifies an annotation to search for. The {@code markerInterface} property
   66 |  * specifies a parent interface to search for. If both properties are specified, mappers are added for interfaces that
   67 |  * match <em>either</em> criteria. By default, these two properties are null, so all interfaces in the given
   68 |  * {@code basePackage} are added as mappers.
   69 |  * <p>
   70 |  * This configurer enables autowire for all the beans that it creates so that they are automatically autowired with the
   71 |  * proper {@code SqlSessionFactory} or {@code SqlSessionTemplate}. If there is more than one {@code SqlSessionFactory}
   72 |  * in the application, however, autowiring cannot be used. In this case you must explicitly specify either an
   73 |  * {@code SqlSessionFactory} or an {@code SqlSessionTemplate} to use via the <em>bean name</em> properties. Bean names
   74 |  * are used rather than actual objects because Spring does not initialize property placeholders until after this class
   75 |  * is processed.
   76 |  * <p>
   77 |  * Passing in an actual object which may require placeholders (i.e. DB user password) will fail. Using bean names defers
   78 |  * actual object creation until later in the startup process, after all placeholder substitution is completed. However,
   79 |  * note that this configurer does support property placeholders of its <em>own</em> properties. The
   80 |  * <code>basePackage</code> and bean name properties all support <code>${property}</code> style substitution.
   81 |  * <p>
   82 |  * Configuration sample:
   83 |  *
   84 |  * <pre class="code">
   85 |  * {@code
   86 |  *   <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
   87 |  *       <property name="basePackage" value="org.mybatis.spring.sample.mapper" />
   88 |  *       <!-- optional unless there are multiple session factories defined -->
   89 |  *       <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory" />
   90 |  *   </bean>
   91 |  * }
   92 |  * </pre>
   93 |  *
   94 |  * @author Hunter Presnall
   95 |  * @author Eduardo Macarron
   96 |  *
   97 |  * @see MapperFactoryBean
   98 |  * @see ClassPathMapperScanner
   99 |  */
  100 | public class MapperScannerConfigurer
  101 |     implements BeanDefinitionRegistryPostProcessor, InitializingBean, ApplicationContextAware, BeanNameAware {
  102 | 
  103 |   private String basePackage;
  104 | 
  105 |   private boolean addToConfig = true;
  106 | 
  107 |   private String lazyInitialization;
  108 | 
  109 |   private SqlSessionFactory sqlSessionFactory;
  110 | 
  111 |   private SqlSessionTemplate sqlSessionTemplate;
  112 | 
  113 |   private String sqlSessionFactoryBeanName;
  114 | 
  115 |   private String sqlSessionTemplateBeanName;
  116 | 
  117 |   private Class<? extends Annotation> annotationClass;
  118 | 
  119 |   private Class<?> markerInterface;
  120 | 
  121 |   private List<TypeFilter> excludeFilters;
  122 | 
  123 |   private List<Map<String, String>> rawExcludeFilters;
  124 | 
  125 |   private Class<? extends MapperFactoryBean> mapperFactoryBeanClass;
  126 | 
  127 |   private ApplicationContext applicationContext;
  128 | 
  129 |   private String beanName;
  130 | 
  131 |   private boolean processPropertyPlaceHolders;
  132 | 
  133 |   private BeanNameGenerator nameGenerator;
  134 | 
  135 |   private String defaultScope;
  136 | 
  137 |   /**
  138 |    * This property lets you set the base package for your mapper interface files.
  139 |    * <p>
  140 |    * You can set more than one package by using a semicolon or comma as a separator.
  141 |    * <p>
  142 |    * Mappers will be searched for recursively starting in the specified package(s).
  143 |    *
  144 |    * @param basePackage
  145 |    *          base package name
  146 |    */
  147 |   public void setBasePackage(String basePackage) {
  148 |     this.basePackage = basePackage;
  149 |   }
  150 | 
  151 |   /**
  152 |    * Same as {@code MapperFactoryBean#setAddToConfig(boolean)}.
  153 |    *
  154 |    * @param addToConfig
  155 |    *          a flag that whether add mapper to MyBatis or not
  156 |    *
  157 |    * @see MapperFactoryBean#setAddToConfig(boolean)
  158 |    */
  159 |   public void setAddToConfig(boolean addToConfig) {
  160 |     this.addToConfig = addToConfig;
  161 |   }
  162 | 
  163 |   /**
  164 |    * Set whether enable lazy initialization for mapper bean.
  165 |    * <p>
  166 |    * Default is {@code false}.
  167 |    * </p>
  168 |    *
  169 |    * @param lazyInitialization
  170 |    *          Set the @{code true} to enable
  171 |    *
  172 |    * @since 2.0.2
  173 |    */
  174 |   public void setLazyInitialization(String lazyInitialization) {
  175 |     this.lazyInitialization = lazyInitialization;
  176 |   }
  177 | 
  178 |   /**
  179 |    * This property specifies the annotation that the scanner will search for.
  180 |    * <p>
  181 |    * The scanner will register all interfaces in the base package that also have the specified annotation.
  182 |    * <p>
  183 |    * Note this can be combined with markerInterface.
  184 |    *
  185 |    * @param annotationClass
  186 |    *          annotation class
  187 |    */
  188 |   public void setAnnotationClass(Class<? extends Annotation> annotationClass) {
  189 |     this.annotationClass = annotationClass;
  190 |   }
  191 | 
  192 |   /**
  193 |    * This property specifies the parent that the scanner will search for.
  194 |    * <p>
  195 |    * The scanner will register all interfaces in the base package that also have the specified interface class as a
  196 |    * parent.
  197 |    * <p>
  198 |    * Note this can be combined with annotationClass.
  199 |    *
  200 |    * @param superClass
  201 |    *          parent class
  202 |    */
  203 |   public void setMarkerInterface(Class<?> superClass) {
  204 |     this.markerInterface = superClass;
  205 |   }
  206 | 
  207 |   /**
  208 |    * Specifies which types are not eligible for the mapper scanner.
  209 |    * <p>
  210 |    * The scanner will exclude types that define with excludeFilters.
  211 |    *
  212 |    * @since 3.0.4
  213 |    *
  214 |    * @param excludeFilters
  215 |    *          list of TypeFilter
  216 |    */
  217 |   public void setExcludeFilters(List<TypeFilter> excludeFilters) {
  218 |     this.excludeFilters = excludeFilters;
  219 |   }
  220 | 
  221 |   /**
  222 |    * In order to support process PropertyPlaceHolders.
  223 |    * <p>
  224 |    * After parsed, it will be added to excludeFilters.
  225 |    *
  226 |    * @since 3.0.4
  227 |    *
  228 |    * @param rawExcludeFilters
  229 |    *          list of rawExcludeFilter
  230 |    */
  231 |   public void setRawExcludeFilters(List<Map<String, String>> rawExcludeFilters) {
  232 |     this.rawExcludeFilters = rawExcludeFilters;
  233 |   }
  234 | 
  235 |   /**
  236 |    * Specifies which {@code SqlSessionTemplate} to use in the case that there is more than one in the spring context.
  237 |    * Usually this is only needed when you have more than one datasource.
  238 |    * <p>
  239 |    *
  240 |    * @deprecated Use {@link #setSqlSessionTemplateBeanName(String)} instead
  241 |    *
  242 |    * @param sqlSessionTemplate
  243 |    *          a template of SqlSession
  244 |    */
  245 |   @Deprecated
  246 |   public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
  247 |     this.sqlSessionTemplate = sqlSessionTemplate;
  248 |   }
  249 | 
  250 |   /**
  251 |    * Specifies which {@code SqlSessionTemplate} to use in the case that there is more than one in the spring context.
  252 |    * Usually this is only needed when you have more than one datasource.
  253 |    * <p>
  254 |    * Note bean names are used, not bean references. This is because the scanner loads early during the start process and
  255 |    * it is too early to build mybatis object instances.
  256 |    *
  257 |    * @since 1.1.0
  258 |    *
  259 |    * @param sqlSessionTemplateName
  260 |    *          Bean name of the {@code SqlSessionTemplate}
  261 |    */
  262 |   public void setSqlSessionTemplateBeanName(String sqlSessionTemplateName) {
  263 |     this.sqlSessionTemplateBeanName = sqlSessionTemplateName;
  264 |   }
  265 | 
  266 |   /**
  267 |    * Specifies which {@code SqlSessionFactory} to use in the case that there is more than one in the spring context.
  268 |    * Usually this is only needed when you have more than one datasource.
  269 |    * <p>
  270 |    *
  271 |    * @deprecated Use {@link #setSqlSessionFactoryBeanName(String)} instead.
  272 |    *
  273 |    * @param sqlSessionFactory
  274 |    *          a factory of SqlSession
  275 |    */
  276 |   @Deprecated
  277 |   public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
  278 |     this.sqlSessionFactory = sqlSessionFactory;
  279 |   }
  280 | 
  281 |   /**
  282 |    * Specifies which {@code SqlSessionFactory} to use in the case that there is more than one in the spring context.
  283 |    * Usually this is only needed when you have more than one datasource.
  284 |    * <p>
  285 |    * Note bean names are used, not bean references. This is because the scanner loads early during the start process and
  286 |    * it is too early to build mybatis object instances.
  287 |    *
  288 |    * @since 1.1.0
  289 |    *
  290 |    * @param sqlSessionFactoryName
  291 |    *          Bean name of the {@code SqlSessionFactory}
  292 |    */
  293 |   public void setSqlSessionFactoryBeanName(String sqlSessionFactoryName) {
  294 |     this.sqlSessionFactoryBeanName = sqlSessionFactoryName;
  295 |   }
  296 | 
  297 |   /**
  298 |    * Specifies a flag that whether execute a property placeholder processing or not.
  299 |    * <p>
  300 |    * The default is {@literal false}. This means that a property placeholder processing does not execute.
  301 |    *
  302 |    * @since 1.1.1
  303 |    *
  304 |    * @param processPropertyPlaceHolders
  305 |    *          a flag that whether execute a property placeholder processing or not
  306 |    */
  307 |   public void setProcessPropertyPlaceHolders(boolean processPropertyPlaceHolders) {
  308 |     this.processPropertyPlaceHolders = processPropertyPlaceHolders;
  309 |   }
  310 | 
  311 |   /**
  312 |    * The class of the {@link MapperFactoryBean} to return a mybatis proxy as spring bean.
  313 |    *
  314 |    * @param mapperFactoryBeanClass
  315 |    *          The class of the MapperFactoryBean
  316 |    *
  317 |    * @since 2.0.1
  318 |    */
  319 |   public void setMapperFactoryBeanClass(Class<? extends MapperFactoryBean> mapperFactoryBeanClass) {
  320 |     this.mapperFactoryBeanClass = mapperFactoryBeanClass;
  321 |   }
  322 | 
  323 |   @Override
  324 |   public void setApplicationContext(ApplicationContext applicationContext) {
  325 |     this.applicationContext = applicationContext;
  326 |   }
  327 | 
  328 |   @Override
  329 |   public void setBeanName(String name) {
  330 |     this.beanName = name;
  331 |   }
  332 | 
  333 |   /**
  334 |    * Gets beanNameGenerator to be used while running the scanner.
  335 |    *
  336 |    * @return the beanNameGenerator BeanNameGenerator that has been configured
  337 |    *
  338 |    * @since 1.2.0
  339 |    */
  340 |   public BeanNameGenerator getNameGenerator() {
  341 |     return nameGenerator;
  342 |   }
  343 | 
  344 |   /**
  345 |    * Sets beanNameGenerator to be used while running the scanner.
  346 |    *
  347 |    * @param nameGenerator
  348 |    *          the beanNameGenerator to set
  349 |    *
  350 |    * @since 1.2.0
  351 |    */
  352 |   public void setNameGenerator(BeanNameGenerator nameGenerator) {
  353 |     this.nameGenerator = nameGenerator;
  354 |   }
  355 | 
  356 |   /**
  357 |    * Sets the default scope of scanned mappers.
  358 |    * <p>
  359 |    * Default is {@code null} (equiv to singleton).
  360 |    * </p>
  361 |    *
  362 |    * @param defaultScope
  363 |    *          the default scope
  364 |    *
  365 |    * @since 2.0.6
  366 |    */
  367 |   public void setDefaultScope(String defaultScope) {
  368 |     this.defaultScope = defaultScope;
  369 |   }
  370 | 
  371 |   @Override
  372 |   public void afterPropertiesSet() throws Exception {
  373 |     notNull(this.basePackage, "Property 'basePackage' is required");
  374 |   }
  375 | 
  376 |   @Override
  377 |   public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
  378 |     // left intentionally blank
  379 |   }
  380 | 
  381 |   @Override
  382 |   public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
  383 |     if (this.processPropertyPlaceHolders) {
  384 |       processPropertyPlaceHolders();
  385 |     }
  386 | 
  387 |     var scanner = new ClassPathMapperScanner(registry, getEnvironment());
  388 |     scanner.setAddToConfig(this.addToConfig);
  389 |     scanner.setAnnotationClass(this.annotationClass);
  390 |     scanner.setMarkerInterface(this.markerInterface);
  391 |     scanner.setExcludeFilters(this.excludeFilters = mergeExcludeFilters());
  392 |     scanner.setSqlSessionFactory(this.sqlSessionFactory);
  393 |     scanner.setSqlSessionTemplate(this.sqlSessionTemplate);
  394 |     scanner.setSqlSessionFactoryBeanName(this.sqlSessionFactoryBeanName);
  395 |     scanner.setSqlSessionTemplateBeanName(this.sqlSessionTemplateBeanName);
  396 |     scanner.setResourceLoader(this.applicationContext);
  397 |     scanner.setBeanNameGenerator(this.nameGenerator);
  398 |     scanner.setMapperFactoryBeanClass(this.mapperFactoryBeanClass);
  399 |     if (StringUtils.hasText(lazyInitialization)) {
  400 |       scanner.setLazyInitialization(Boolean.parseBoolean(lazyInitialization));
  401 |     }
  402 |     if (StringUtils.hasText(defaultScope)) {
  403 |       scanner.setDefaultScope(defaultScope);
  404 |     }
  405 |     scanner.registerFilters();
  406 |     scanner.scan(
  407 |         StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
  408 |   }
  409 | 
  410 |   /*
  411 |    * BeanDefinitionRegistries are called early in application startup, before BeanFactoryPostProcessors. This means that
  412 |    * PropertyResourceConfigurers will not have been loaded and any property substitution of this class' properties will
  413 |    * fail. To avoid this, find any PropertyResourceConfigurers defined in the context and run them on this class' bean
  414 |    * definition. Then update the values.
  415 |    */
  416 |   private void processPropertyPlaceHolders() {
  417 |     Map<String, PropertyResourceConfigurer> prcs = applicationContext.getBeansOfType(PropertyResourceConfigurer.class,
  418 |         false, false);
  419 | 
  420 |     if (!prcs.isEmpty() && applicationContext instanceof ConfigurableApplicationContext) {
  421 |       var mapperScannerBean = ((ConfigurableApplicationContext) applicationContext).getBeanFactory()
  422 |           .getBeanDefinition(beanName);
  423 | 
  424 |       // PropertyResourceConfigurer does not expose any methods to explicitly perform
  425 |       // property placeholder substitution. Instead, create a BeanFactory that just
  426 |       // contains this mapper scanner and post process the factory.
  427 |       var factory = new DefaultListableBeanFactory();
  428 |       factory.registerBeanDefinition(beanName, mapperScannerBean);
  429 | 
  430 |       for (PropertyResourceConfigurer prc : prcs.values()) {
  431 |         prc.postProcessBeanFactory(factory);
  432 |       }
  433 | 
  434 |       PropertyValues values = mapperScannerBean.getPropertyValues();
  435 | 
  436 |       this.basePackage = getPropertyValue("basePackage", values);
  437 |       this.sqlSessionFactoryBeanName = getPropertyValue("sqlSessionFactoryBeanName", values);
  438 |       this.sqlSessionTemplateBeanName = getPropertyValue("sqlSessionTemplateBeanName", values);
  439 |       this.lazyInitialization = getPropertyValue("lazyInitialization", values);
  440 |       this.defaultScope = getPropertyValue("defaultScope", values);
  441 |       this.rawExcludeFilters = getPropertyValueForTypeFilter("rawExcludeFilters", values);
  442 |     }
  443 |     this.basePackage = Optional.ofNullable(this.basePackage).map(getEnvironment()::resolvePlaceholders).orElse(null);
  444 |     this.sqlSessionFactoryBeanName = Optional.ofNullable(this.sqlSessionFactoryBeanName)
  445 |         .map(getEnvironment()::resolvePlaceholders).orElse(null);
  446 |     this.sqlSessionTemplateBeanName = Optional.ofNullable(this.sqlSessionTemplateBeanName)
  447 |         .map(getEnvironment()::resolvePlaceholders).orElse(null);
  448 |     this.lazyInitialization = Optional.ofNullable(this.lazyInitialization).map(getEnvironment()::resolvePlaceholders)
  449 |         .orElse(null);
  450 |     this.defaultScope = Optional.ofNullable(this.defaultScope).map(getEnvironment()::resolvePlaceholders).orElse(null);
  451 |   }
  452 | 
  453 |   private Environment getEnvironment() {
  454 |     return this.applicationContext.getEnvironment();
  455 |   }
  456 | 
  457 |   private String getPropertyValue(String propertyName, PropertyValues values) {
  458 |     var property = values.getPropertyValue(propertyName);
  459 | 
  460 |     if (property == null) {
  461 |       return null;
  462 |     }
  463 | 
  464 |     var value = property.getValue();
  465 | 
  466 |     if (value == null) {
  467 |       return null;
  468 |     }
  469 |     if (value instanceof String) {
  470 |       return value.toString();
  471 |     }
  472 |     if (value instanceof TypedStringValue) {
  473 |       return ((TypedStringValue) value).getValue();
  474 |     }
  475 |     return null;
  476 |   }
  477 | 
  478 |   @SuppressWarnings("unchecked")
  479 |   private List<Map<String, String>> getPropertyValueForTypeFilter(String propertyName, PropertyValues values) {
  480 |     var property = values.getPropertyValue(propertyName);
  481 |     Object value;
  482 |     if (property == null || (value = property.getValue()) == null || !(value instanceof List<?>)) {
  483 |       return null;
  484 |     }
  485 |     return (List<Map<String, String>>) value;
  486 |   }
  487 | 
  488 |   private List<TypeFilter> mergeExcludeFilters() {
  489 |     List<TypeFilter> typeFilters = new ArrayList<>();
  490 |     if (this.rawExcludeFilters == null || this.rawExcludeFilters.isEmpty()) {
  491 |       return this.excludeFilters;
  492 |     }
  493 |     if (this.excludeFilters != null && !this.excludeFilters.isEmpty()) {
  494 |       typeFilters.addAll(this.excludeFilters);
  495 |     }
  496 |     try {
  497 |       for (Map<String, String> typeFilter : this.rawExcludeFilters) {
  498 |         typeFilters.add(
  499 |             createTypeFilter(typeFilter.get("type"), typeFilter.get("expression"), this.getClass().getClassLoader()));
  500 |       }
  501 |     } catch (ClassNotFoundException exception) {
  502 |       throw new RuntimeException("ClassNotFoundException occur when to load the Specified excludeFilter classes.",
  503 |           exception);
  504 |     }
  505 |     return typeFilters;
  506 |   }
  507 | 
  508 |   @SuppressWarnings("unchecked")
  509 |   private TypeFilter createTypeFilter(String filterType, String expression, @Nullable ClassLoader classLoader)
  510 |       throws ClassNotFoundException {
  511 | 
  512 |     if (this.processPropertyPlaceHolders) {
  513 |       expression = this.getEnvironment().resolvePlaceholders(expression);
  514 |     }
  515 | 
  516 |     switch (filterType) {
  517 |       case "annotation":
  518 |         Class<?> filterAnno = ClassUtils.forName(expression, classLoader);
  519 |         if (!Annotation.class.isAssignableFrom(filterAnno)) {
  520 |           throw new IllegalArgumentException(
  521 |               "Class is not assignable to [" + Annotation.class.getName() + "]: " + expression);
  522 |         }
  523 |         return new AnnotationTypeFilter((Class<Annotation>) filterAnno);
  524 |       case "custom":
  525 |         Class<?> filterClass = ClassUtils.forName(expression, classLoader);
  526 |         if (!TypeFilter.class.isAssignableFrom(filterClass)) {
  527 |           throw new IllegalArgumentException(
  528 |               "Class is not assignable to [" + TypeFilter.class.getName() + "]: " + expression);
  529 |         }
  530 |         return (TypeFilter) BeanUtils.instantiateClass(filterClass);
  531 |       case "assignable":
  532 |         return new AssignableTypeFilter(ClassUtils.forName(expression, classLoader));
  533 |       case "regex":
  534 |         return new RegexPatternTypeFilter(Pattern.compile(expression));
  535 |       case "aspectj":
  536 |         return new AspectJTypeFilter(expression, classLoader);
  537 |       default:
  538 |         throw new IllegalArgumentException("Unsupported filter type: " + filterType);
  539 |     }
  540 |   }
  541 | 
  542 | }
```
