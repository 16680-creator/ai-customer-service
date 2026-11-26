<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis-spring:3.0.5 | path: org/mybatis/spring/mapper/ClassPathMapperScanner.java -->
<!-- 正文引用：[[源码:ClassPathMapperScanner.java#L起-L止]] ；代码块头部：(src: ClassPathMapperScanner.java#L起-L止) -->

# ClassPathMapperScanner.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis-spring:3.0.5` |
| 路径 | `org/mybatis/spring/mapper/ClassPathMapperScanner.java` |
| 行数 | 372 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis-spring/3.0.5/mybatis-spring-3.0.5-sources.jar |
| 抓取时间 | 2026-11-25T15:01:25.468Z |

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
   18 | import java.lang.annotation.Annotation;
   19 | import java.util.Arrays;
   20 | import java.util.List;
   21 | import java.util.Optional;
   22 | import java.util.Set;
   23 | 
   24 | import org.apache.ibatis.io.Resources;
   25 | import org.apache.ibatis.session.SqlSessionFactory;
   26 | import org.mybatis.logging.Logger;
   27 | import org.mybatis.logging.LoggerFactory;
   28 | import org.mybatis.spring.SqlSessionTemplate;
   29 | import org.springframework.aop.scope.ScopedProxyFactoryBean;
   30 | import org.springframework.aop.scope.ScopedProxyUtils;
   31 | import org.springframework.aot.AotDetector;
   32 | import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
   33 | import org.springframework.beans.factory.config.BeanDefinition;
   34 | import org.springframework.beans.factory.config.BeanDefinitionHolder;
   35 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   36 | import org.springframework.beans.factory.config.RuntimeBeanReference;
   37 | import org.springframework.beans.factory.support.AbstractBeanDefinition;
   38 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   39 | import org.springframework.beans.factory.support.RootBeanDefinition;
   40 | import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
   41 | import org.springframework.core.NativeDetector;
   42 | import org.springframework.core.env.Environment;
   43 | import org.springframework.core.type.filter.AnnotationTypeFilter;
   44 | import org.springframework.core.type.filter.AssignableTypeFilter;
   45 | import org.springframework.core.type.filter.TypeFilter;
   46 | import org.springframework.util.StringUtils;
   47 | 
   48 | /**
   49 |  * A {@link ClassPathBeanDefinitionScanner} that registers Mappers by {@code basePackage}, {@code annotationClass}, or
   50 |  * {@code markerInterface}. If an {@code annotationClass} and/or {@code markerInterface} is specified, only the
   51 |  * specified types will be searched (searching for all interfaces will be disabled).
   52 |  * <p>
   53 |  * This functionality was previously a private class of {@link MapperScannerConfigurer}, but was broken out in version
   54 |  * 1.2.0.
   55 |  *
   56 |  * @author Hunter Presnall
   57 |  * @author Eduardo Macarron
   58 |  *
   59 |  * @see MapperFactoryBean
   60 |  *
   61 |  * @since 1.2.0
   62 |  */
   63 | public class ClassPathMapperScanner extends ClassPathBeanDefinitionScanner {
   64 | 
   65 |   private static final Logger LOGGER = LoggerFactory.getLogger(ClassPathMapperScanner.class);
   66 | 
   67 |   // Copy of FactoryBean#OBJECT_TYPE_ATTRIBUTE which was added in Spring 5.2
   68 |   static final String FACTORY_BEAN_OBJECT_TYPE = "factoryBeanObjectType";
   69 | 
   70 |   private boolean addToConfig = true;
   71 | 
   72 |   private boolean lazyInitialization;
   73 | 
   74 |   private boolean printWarnLogIfNotFoundMappers = true;
   75 | 
   76 |   private SqlSessionFactory sqlSessionFactory;
   77 | 
   78 |   private SqlSessionTemplate sqlSessionTemplate;
   79 | 
   80 |   private String sqlSessionTemplateBeanName;
   81 | 
   82 |   private String sqlSessionFactoryBeanName;
   83 | 
   84 |   private Class<? extends Annotation> annotationClass;
   85 | 
   86 |   private Class<?> markerInterface;
   87 | 
   88 |   private Class<? extends MapperFactoryBean> mapperFactoryBeanClass = MapperFactoryBean.class;
   89 | 
   90 |   private String defaultScope;
   91 |   private List<TypeFilter> excludeFilters;
   92 | 
   93 |   public ClassPathMapperScanner(BeanDefinitionRegistry registry, Environment environment) {
   94 |     super(registry, false, environment);
   95 |     setIncludeAnnotationConfig(!AotDetector.useGeneratedArtifacts());
   96 |     setPrintWarnLogIfNotFoundMappers(!NativeDetector.inNativeImage());
   97 |   }
   98 | 
   99 |   /**
  100 |    * @deprecated Please use the {@link #ClassPathMapperScanner(BeanDefinitionRegistry, Environment)}.
  101 |    */
  102 |   @Deprecated(since = "3.0.4", forRemoval = true)
  103 |   public ClassPathMapperScanner(BeanDefinitionRegistry registry) {
  104 |     super(registry, false);
  105 |     setIncludeAnnotationConfig(!AotDetector.useGeneratedArtifacts());
  106 |     setPrintWarnLogIfNotFoundMappers(!NativeDetector.inNativeImage());
  107 |   }
  108 | 
  109 |   public void setAddToConfig(boolean addToConfig) {
  110 |     this.addToConfig = addToConfig;
  111 |   }
  112 | 
  113 |   public void setAnnotationClass(Class<? extends Annotation> annotationClass) {
  114 |     this.annotationClass = annotationClass;
  115 |   }
  116 | 
  117 |   /**
  118 |    * Set whether enable lazy initialization for mapper bean.
  119 |    * <p>
  120 |    * Default is {@code false}.
  121 |    * </p>
  122 |    *
  123 |    * @param lazyInitialization
  124 |    *          Set the @{code true} to enable
  125 |    *
  126 |    * @since 2.0.2
  127 |    */
  128 |   public void setLazyInitialization(boolean lazyInitialization) {
  129 |     this.lazyInitialization = lazyInitialization;
  130 |   }
  131 | 
  132 |   /**
  133 |    * Set whether print warning log if not found mappers that matches conditions.
  134 |    * <p>
  135 |    * Default is {@code true}. But {@code false} when running in native image.
  136 |    * </p>
  137 |    *
  138 |    * @param printWarnLogIfNotFoundMappers
  139 |    *          Set the @{code true} to print
  140 |    *
  141 |    * @since 3.0.1
  142 |    */
  143 |   public void setPrintWarnLogIfNotFoundMappers(boolean printWarnLogIfNotFoundMappers) {
  144 |     this.printWarnLogIfNotFoundMappers = printWarnLogIfNotFoundMappers;
  145 |   }
  146 | 
  147 |   public void setMarkerInterface(Class<?> markerInterface) {
  148 |     this.markerInterface = markerInterface;
  149 |   }
  150 | 
  151 |   public void setExcludeFilters(List<TypeFilter> excludeFilters) {
  152 |     this.excludeFilters = excludeFilters;
  153 |   }
  154 | 
  155 |   public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
  156 |     this.sqlSessionFactory = sqlSessionFactory;
  157 |   }
  158 | 
  159 |   public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
  160 |     this.sqlSessionTemplate = sqlSessionTemplate;
  161 |   }
  162 | 
  163 |   public void setSqlSessionTemplateBeanName(String sqlSessionTemplateBeanName) {
  164 |     this.sqlSessionTemplateBeanName = sqlSessionTemplateBeanName;
  165 |   }
  166 | 
  167 |   public void setSqlSessionFactoryBeanName(String sqlSessionFactoryBeanName) {
  168 |     this.sqlSessionFactoryBeanName = sqlSessionFactoryBeanName;
  169 |   }
  170 | 
  171 |   /**
  172 |    * @deprecated Since 2.0.1, Please use the {@link #setMapperFactoryBeanClass(Class)}.
  173 |    */
  174 |   @Deprecated
  175 |   public void setMapperFactoryBean(MapperFactoryBean<?> mapperFactoryBean) {
  176 |     this.mapperFactoryBeanClass = mapperFactoryBean == null ? MapperFactoryBean.class : mapperFactoryBean.getClass();
  177 |   }
  178 | 
  179 |   /**
  180 |    * Set the {@code MapperFactoryBean} class.
  181 |    *
  182 |    * @param mapperFactoryBeanClass
  183 |    *          the {@code MapperFactoryBean} class
  184 |    *
  185 |    * @since 2.0.1
  186 |    */
  187 |   public void setMapperFactoryBeanClass(Class<? extends MapperFactoryBean> mapperFactoryBeanClass) {
  188 |     this.mapperFactoryBeanClass = mapperFactoryBeanClass == null ? MapperFactoryBean.class : mapperFactoryBeanClass;
  189 |   }
  190 | 
  191 |   /**
  192 |    * Set the default scope of scanned mappers.
  193 |    * <p>
  194 |    * Default is {@code null} (equiv to singleton).
  195 |    * </p>
  196 |    *
  197 |    * @param defaultScope
  198 |    *          the scope
  199 |    *
  200 |    * @since 2.0.6
  201 |    */
  202 |   public void setDefaultScope(String defaultScope) {
  203 |     this.defaultScope = defaultScope;
  204 |   }
  205 | 
  206 |   /**
  207 |    * Configures parent scanner to search for the right interfaces. It can search for all interfaces or just for those
  208 |    * that extends a markerInterface or/and those annotated with the annotationClass
  209 |    */
  210 |   public void registerFilters() {
  211 |     var acceptAllInterfaces = true;
  212 | 
  213 |     // if specified, use the given annotation and / or marker interface
  214 |     if (this.annotationClass != null) {
  215 |       addIncludeFilter(new AnnotationTypeFilter(this.annotationClass));
  216 |       acceptAllInterfaces = false;
  217 |     }
  218 | 
  219 |     // override AssignableTypeFilter to ignore matches on the actual marker interface
  220 |     if (this.markerInterface != null) {
  221 |       addIncludeFilter(new AssignableTypeFilter(this.markerInterface) {
  222 |         @Override
  223 |         protected boolean matchClassName(String className) {
  224 |           return false;
  225 |         }
  226 |       });
  227 |       acceptAllInterfaces = false;
  228 |     }
  229 | 
  230 |     if (acceptAllInterfaces) {
  231 |       // default include filter that accepts all classes
  232 |       addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
  233 |     }
  234 | 
  235 |     // exclude package-info.java
  236 |     addExcludeFilter((metadataReader, metadataReaderFactory) -> {
  237 |       var className = metadataReader.getClassMetadata().getClassName();
  238 |       return className.endsWith("package-info");
  239 |     });
  240 | 
  241 |     // exclude types declared by MapperScan.excludeFilters
  242 |     if (excludeFilters != null && excludeFilters.size() > 0) {
  243 |       for (TypeFilter excludeFilter : excludeFilters) {
  244 |         addExcludeFilter(excludeFilter);
  245 |       }
  246 |     }
  247 |   }
  248 | 
  249 |   /**
  250 |    * Calls the parent search that will search and register all the candidates. Then the registered objects are post
  251 |    * processed to set them as MapperFactoryBeans
  252 |    */
  253 |   @Override
  254 |   public Set<BeanDefinitionHolder> doScan(String... basePackages) {
  255 |     var beanDefinitions = super.doScan(basePackages);
  256 | 
  257 |     if (beanDefinitions.isEmpty()) {
  258 |       if (printWarnLogIfNotFoundMappers) {
  259 |         LOGGER.warn(() -> "No MyBatis mapper was found in '" + Arrays.toString(basePackages)
  260 |             + "' package. Please check your configuration.");
  261 |       }
  262 |     } else {
  263 |       processBeanDefinitions(beanDefinitions);
  264 |     }
  265 | 
  266 |     return beanDefinitions;
  267 |   }
  268 | 
  269 |   private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
  270 |     AbstractBeanDefinition definition;
  271 |     var registry = getRegistry();
  272 |     for (BeanDefinitionHolder holder : beanDefinitions) {
  273 |       definition = (AbstractBeanDefinition) holder.getBeanDefinition();
  274 |       var scopedProxy = false;
  275 |       if (ScopedProxyFactoryBean.class.getName().equals(definition.getBeanClassName())) {
  276 |         definition = (AbstractBeanDefinition) Optional
  277 |             .ofNullable(((RootBeanDefinition) definition).getDecoratedDefinition())
  278 |             .map(BeanDefinitionHolder::getBeanDefinition).orElseThrow(() -> new IllegalStateException(
  279 |                 "The target bean definition of scoped proxy bean not found. Root bean definition[" + holder + "]"));
  280 |         scopedProxy = true;
  281 |       }
  282 |       var beanClassName = definition.getBeanClassName();
  283 |       LOGGER.debug(() -> "Creating MapperFactoryBean with name '" + holder.getBeanName() + "' and '" + beanClassName
  284 |           + "' mapperInterface");
  285 | 
  286 |       // the mapper interface is the original class of the bean
  287 |       // but, the actual class of the bean is MapperFactoryBean
  288 |       definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName); // issue #59
  289 |       try {
  290 |         Class<?> beanClass = Resources.classForName(beanClassName);
  291 |         // Attribute for MockitoPostProcessor
  292 |         // https://github.com/mybatis/spring-boot-starter/issues/475
  293 |         definition.setAttribute(FACTORY_BEAN_OBJECT_TYPE, beanClass);
  294 |         // for spring-native
  295 |         definition.getPropertyValues().add("mapperInterface", beanClass);
  296 |       } catch (ClassNotFoundException ignore) {
  297 |         // ignore
  298 |       }
  299 | 
  300 |       definition.setBeanClass(this.mapperFactoryBeanClass);
  301 | 
  302 |       definition.getPropertyValues().add("addToConfig", this.addToConfig);
  303 | 
  304 |       var explicitFactoryUsed = false;
  305 |       if (StringUtils.hasText(this.sqlSessionFactoryBeanName)) {
  306 |         definition.getPropertyValues().add("sqlSessionFactory",
  307 |             new RuntimeBeanReference(this.sqlSessionFactoryBeanName));
  308 |         explicitFactoryUsed = true;
  309 |       } else if (this.sqlSessionFactory != null) {
  310 |         definition.getPropertyValues().add("sqlSessionFactory", this.sqlSessionFactory);
  311 |         explicitFactoryUsed = true;
  312 |       }
  313 | 
  314 |       if (StringUtils.hasText(this.sqlSessionTemplateBeanName)) {
  315 |         if (explicitFactoryUsed) {
  316 |           LOGGER.warn(
  317 |               () -> "Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
  318 |         }
  319 |         definition.getPropertyValues().add("sqlSessionTemplate",
  320 |             new RuntimeBeanReference(this.sqlSessionTemplateBeanName));
  321 |         explicitFactoryUsed = true;
  322 |       } else if (this.sqlSessionTemplate != null) {
  323 |         if (explicitFactoryUsed) {
  324 |           LOGGER.warn(
  325 |               () -> "Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
  326 |         }
  327 |         definition.getPropertyValues().add("sqlSessionTemplate", this.sqlSessionTemplate);
  328 |         explicitFactoryUsed = true;
  329 |       }
  330 | 
  331 |       if (!explicitFactoryUsed) {
  332 |         LOGGER.debug(() -> "Enabling autowire by type for MapperFactoryBean with name '" + holder.getBeanName() + "'.");
  333 |         definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
  334 |       }
  335 | 
  336 |       definition.setLazyInit(lazyInitialization);
  337 | 
  338 |       if (scopedProxy) {
  339 |         continue;
  340 |       }
  341 | 
  342 |       if (ConfigurableBeanFactory.SCOPE_SINGLETON.equals(definition.getScope()) && defaultScope != null) {
  343 |         definition.setScope(defaultScope);
  344 |       }
  345 | 
  346 |       if (!definition.isSingleton()) {
  347 |         var proxyHolder = ScopedProxyUtils.createScopedProxy(holder, registry, true);
  348 |         if (registry.containsBeanDefinition(proxyHolder.getBeanName())) {
  349 |           registry.removeBeanDefinition(proxyHolder.getBeanName());
  350 |         }
  351 |         registry.registerBeanDefinition(proxyHolder.getBeanName(), proxyHolder.getBeanDefinition());
  352 |       }
  353 | 
  354 |     }
  355 |   }
  356 | 
  357 |   @Override
  358 |   protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
  359 |     return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
  360 |   }
  361 | 
  362 |   @Override
  363 |   protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) {
  364 |     if (super.checkCandidate(beanName, beanDefinition)) {
  365 |       return true;
  366 |     }
  367 |     LOGGER.warn(() -> "Skipping MapperFactoryBean with name '" + beanName + "' and '"
  368 |         + beanDefinition.getBeanClassName() + "' mapperInterface" + ". Bean already defined with the same name!");
  369 |     return false;
  370 |   }
  371 | 
  372 | }
```
