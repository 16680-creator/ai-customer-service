<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis-spring:3.0.5 | path: org/mybatis/spring/annotation/MapperScannerRegistrar.java -->
<!-- 正文引用：[[源码:MapperScannerRegistrar.java#L起-L止]] ；代码块头部：(src: MapperScannerRegistrar.java#L起-L止) -->

# MapperScannerRegistrar.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis-spring:3.0.5` |
| 路径 | `org/mybatis/spring/annotation/MapperScannerRegistrar.java` |
| 行数 | 259 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis-spring/3.0.5/mybatis-spring-3.0.5-sources.jar |
| 抓取时间 | 2026-11-25T15:01:26.205Z |

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
   16 | package org.mybatis.spring.annotation;
   17 | 
   18 | import java.lang.annotation.Annotation;
   19 | import java.util.ArrayList;
   20 | import java.util.Arrays;
   21 | import java.util.HashMap;
   22 | import java.util.List;
   23 | import java.util.Map;
   24 | import java.util.stream.Collectors;
   25 | 
   26 | import org.mybatis.spring.mapper.ClassPathMapperScanner;
   27 | import org.mybatis.spring.mapper.MapperFactoryBean;
   28 | import org.mybatis.spring.mapper.MapperScannerConfigurer;
   29 | import org.springframework.beans.BeanUtils;
   30 | import org.springframework.beans.factory.config.BeanDefinition;
   31 | import org.springframework.beans.factory.support.AbstractBeanDefinition;
   32 | import org.springframework.beans.factory.support.BeanDefinitionBuilder;
   33 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   34 | import org.springframework.beans.factory.support.BeanNameGenerator;
   35 | import org.springframework.context.ResourceLoaderAware;
   36 | import org.springframework.context.annotation.FilterType;
   37 | import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
   38 | import org.springframework.core.annotation.AnnotationAttributes;
   39 | import org.springframework.core.io.ResourceLoader;
   40 | import org.springframework.core.type.AnnotationMetadata;
   41 | import org.springframework.core.type.filter.AnnotationTypeFilter;
   42 | import org.springframework.core.type.filter.AssignableTypeFilter;
   43 | import org.springframework.core.type.filter.TypeFilter;
   44 | import org.springframework.util.Assert;
   45 | import org.springframework.util.ClassUtils;
   46 | import org.springframework.util.StringUtils;
   47 | 
   48 | /**
   49 |  * A {@link ImportBeanDefinitionRegistrar} to allow annotation configuration of MyBatis mapper scanning. Using
   50 |  * an @Enable annotation allows beans to be registered via @Component configuration, whereas implementing
   51 |  * {@code BeanDefinitionRegistryPostProcessor} will work for XML configuration.
   52 |  *
   53 |  * @author Michael Lanyon
   54 |  * @author Eduardo Macarron
   55 |  * @author Putthiphong Boonphong
   56 |  *
   57 |  * @see MapperFactoryBean
   58 |  * @see ClassPathMapperScanner
   59 |  *
   60 |  * @since 1.2.0
   61 |  */
   62 | public class MapperScannerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {
   63 | 
   64 |   // Note: Do not move resourceLoader via cleanup
   65 |   private ResourceLoader resourceLoader;
   66 | 
   67 |   @Override
   68 |   public void setResourceLoader(ResourceLoader resourceLoader) {
   69 |     this.resourceLoader = resourceLoader;
   70 |   }
   71 | 
   72 |   @Override
   73 |   public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
   74 |     var mapperScanAttrs = AnnotationAttributes
   75 |         .fromMap(importingClassMetadata.getAnnotationAttributes(MapperScan.class.getName()));
   76 |     if (mapperScanAttrs != null) {
   77 |       registerBeanDefinitions(importingClassMetadata, mapperScanAttrs, registry,
   78 |           generateBaseBeanName(importingClassMetadata, 0));
   79 |     }
   80 |   }
   81 | 
   82 |   void registerBeanDefinitions(AnnotationMetadata annoMeta, AnnotationAttributes annoAttrs,
   83 |       BeanDefinitionRegistry registry, String beanName) {
   84 | 
   85 |     var builder = BeanDefinitionBuilder.genericBeanDefinition(MapperScannerConfigurer.class);
   86 |     builder.addPropertyValue("processPropertyPlaceHolders", annoAttrs.getBoolean("processPropertyPlaceHolders"));
   87 | 
   88 |     Class<? extends Annotation> annotationClass = annoAttrs.getClass("annotationClass");
   89 |     if (!Annotation.class.equals(annotationClass)) {
   90 |       builder.addPropertyValue("annotationClass", annotationClass);
   91 |     }
   92 | 
   93 |     Class<?> markerInterface = annoAttrs.getClass("markerInterface");
   94 |     if (!Class.class.equals(markerInterface)) {
   95 |       builder.addPropertyValue("markerInterface", markerInterface);
   96 |     }
   97 | 
   98 |     Class<? extends BeanNameGenerator> generatorClass = annoAttrs.getClass("nameGenerator");
   99 |     if (!BeanNameGenerator.class.equals(generatorClass)) {
  100 |       builder.addPropertyValue("nameGenerator", BeanUtils.instantiateClass(generatorClass));
  101 |     }
  102 | 
  103 |     Class<? extends MapperFactoryBean> mapperFactoryBeanClass = annoAttrs.getClass("factoryBean");
  104 |     if (!MapperFactoryBean.class.equals(mapperFactoryBeanClass)) {
  105 |       builder.addPropertyValue("mapperFactoryBeanClass", mapperFactoryBeanClass);
  106 |     }
  107 | 
  108 |     var sqlSessionTemplateRef = annoAttrs.getString("sqlSessionTemplateRef");
  109 |     if (StringUtils.hasText(sqlSessionTemplateRef)) {
  110 |       builder.addPropertyValue("sqlSessionTemplateBeanName", annoAttrs.getString("sqlSessionTemplateRef"));
  111 |     }
  112 | 
  113 |     var sqlSessionFactoryRef = annoAttrs.getString("sqlSessionFactoryRef");
  114 |     if (StringUtils.hasText(sqlSessionFactoryRef)) {
  115 |       builder.addPropertyValue("sqlSessionFactoryBeanName", annoAttrs.getString("sqlSessionFactoryRef"));
  116 |     }
  117 | 
  118 |     List<String> basePackages = new ArrayList<>(Arrays.stream(annoAttrs.getStringArray("basePackages"))
  119 |         .filter(StringUtils::hasText).collect(Collectors.toList()));
  120 | 
  121 |     basePackages.addAll(Arrays.stream(annoAttrs.getClassArray("basePackageClasses")).map(ClassUtils::getPackageName)
  122 |         .collect(Collectors.toList()));
  123 | 
  124 |     if (basePackages.isEmpty()) {
  125 |       basePackages.add(getDefaultBasePackage(annoMeta));
  126 |     }
  127 | 
  128 |     var excludeFilterArray = annoAttrs.getAnnotationArray("excludeFilters");
  129 |     if (excludeFilterArray.length > 0) {
  130 |       List<TypeFilter> typeFilters = new ArrayList<>();
  131 |       List<Map<String, String>> rawTypeFilters = new ArrayList<>();
  132 |       for (AnnotationAttributes excludeFilters : excludeFilterArray) {
  133 |         if (excludeFilters.getStringArray("pattern").length > 0) {
  134 |           // in oder to apply placeholder resolver
  135 |           rawTypeFilters.addAll(parseFiltersHasPatterns(excludeFilters));
  136 |         } else {
  137 |           typeFilters.addAll(typeFiltersFor(excludeFilters));
  138 |         }
  139 |       }
  140 |       builder.addPropertyValue("excludeFilters", typeFilters);
  141 |       builder.addPropertyValue("rawExcludeFilters", rawTypeFilters);
  142 |     }
  143 | 
  144 |     var lazyInitialization = annoAttrs.getString("lazyInitialization");
  145 |     if (StringUtils.hasText(lazyInitialization)) {
  146 |       builder.addPropertyValue("lazyInitialization", lazyInitialization);
  147 |     }
  148 | 
  149 |     var defaultScope = annoAttrs.getString("defaultScope");
  150 |     if (!AbstractBeanDefinition.SCOPE_DEFAULT.equals(defaultScope)) {
  151 |       builder.addPropertyValue("defaultScope", defaultScope);
  152 |     }
  153 | 
  154 |     builder.addPropertyValue("basePackage", StringUtils.collectionToCommaDelimitedString(basePackages));
  155 | 
  156 |     // for spring-native
  157 |     builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
  158 | 
  159 |     registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
  160 | 
  161 |   }
  162 | 
  163 |   /**
  164 |    * Parse excludeFilters which FilterType is REGEX or ASPECTJ
  165 |    *
  166 |    * @param filterAttributes
  167 |    *          AnnotationAttributes of excludeFilters
  168 |    *
  169 |    * @since 3.0.4
  170 |    */
  171 |   private List<Map<String, String>> parseFiltersHasPatterns(AnnotationAttributes filterAttributes) {
  172 | 
  173 |     List<Map<String, String>> rawTypeFilters = new ArrayList<>();
  174 |     FilterType filterType = filterAttributes.getEnum("type");
  175 |     var expressionArray = filterAttributes.getStringArray("pattern");
  176 |     for (String expression : expressionArray) {
  177 |       switch (filterType) {
  178 |         case REGEX:
  179 |         case ASPECTJ:
  180 |           Map<String, String> typeFilter = new HashMap<>(16);
  181 |           typeFilter.put("type", filterType.name().toLowerCase());
  182 |           typeFilter.put("expression", expression);
  183 |           rawTypeFilters.add(typeFilter);
  184 |           break;
  185 |         default:
  186 |           throw new IllegalArgumentException("Cannot specify the 'pattern' attribute if use the " + filterType
  187 |               + " FilterType in exclude filter of @MapperScan");
  188 |       }
  189 |     }
  190 |     return rawTypeFilters;
  191 |   }
  192 | 
  193 |   /**
  194 |    * Parse excludeFilters which FilterType is ANNOTATION ASSIGNABLE or CUSTOM
  195 |    *
  196 |    * @param filterAttributes
  197 |    *          AnnotationAttributes of excludeFilters
  198 |    *
  199 |    * @since 3.0.4
  200 |    */
  201 |   private List<TypeFilter> typeFiltersFor(AnnotationAttributes filterAttributes) {
  202 | 
  203 |     List<TypeFilter> typeFilters = new ArrayList<>();
  204 |     FilterType filterType = filterAttributes.getEnum("type");
  205 | 
  206 |     for (Class<?> filterClass : filterAttributes.getClassArray("value")) {
  207 |       switch (filterType) {
  208 |         case ANNOTATION:
  209 |           Assert.isAssignable(Annotation.class, filterClass,
  210 |               "Specified an unsupported type in 'ANNOTATION' exclude filter of @MapperScan");
  211 |           @SuppressWarnings("unchecked")
  212 |           var annoClass = (Class<Annotation>) filterClass;
  213 |           typeFilters.add(new AnnotationTypeFilter(annoClass));
  214 |           break;
  215 |         case ASSIGNABLE_TYPE:
  216 |           typeFilters.add(new AssignableTypeFilter(filterClass));
  217 |           break;
  218 |         case CUSTOM:
  219 |           Assert.isAssignable(TypeFilter.class, filterClass,
  220 |               "An error occured when processing a @ComponentScan " + "CUSTOM type filter: ");
  221 |           typeFilters.add(BeanUtils.instantiateClass(filterClass, TypeFilter.class));
  222 |           break;
  223 |         default:
  224 |           throw new IllegalArgumentException("Cannot specify the 'value' or 'classes' attribute if use the "
  225 |               + filterType + " FilterType in exclude filter of @MapperScan");
  226 |       }
  227 |     }
  228 |     return typeFilters;
  229 |   }
  230 | 
  231 |   private static String generateBaseBeanName(AnnotationMetadata importingClassMetadata, int index) {
  232 |     return importingClassMetadata.getClassName() + "#" + MapperScannerRegistrar.class.getSimpleName() + "#" + index;
  233 |   }
  234 | 
  235 |   private static String getDefaultBasePackage(AnnotationMetadata importingClassMetadata) {
  236 |     return ClassUtils.getPackageName(importingClassMetadata.getClassName());
  237 |   }
  238 | 
  239 |   /**
  240 |    * A {@link MapperScannerRegistrar} for {@link MapperScans}.
  241 |    *
  242 |    * @since 2.0.0
  243 |    */
  244 |   static class RepeatingRegistrar extends MapperScannerRegistrar {
  245 |     @Override
  246 |     public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
  247 |       var mapperScansAttrs = AnnotationAttributes
  248 |           .fromMap(importingClassMetadata.getAnnotationAttributes(MapperScans.class.getName()));
  249 |       if (mapperScansAttrs != null) {
  250 |         var annotations = mapperScansAttrs.getAnnotationArray("value");
  251 |         for (var i = 0; i < annotations.length; i++) {
  252 |           registerBeanDefinitions(importingClassMetadata, annotations[i], registry,
  253 |               generateBaseBeanName(importingClassMetadata, i));
  254 |         }
  255 |       }
  256 |     }
  257 |   }
  258 | 
  259 | }
```
