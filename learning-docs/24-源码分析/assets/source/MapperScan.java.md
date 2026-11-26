<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis-spring:3.0.5 | path: org/mybatis/spring/annotation/MapperScan.java -->
<!-- 正文引用：[[源码:MapperScan.java#L起-L止]] ；代码块头部：(src: MapperScan.java#L起-L止) -->

# MapperScan.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis-spring:3.0.5` |
| 路径 | `org/mybatis/spring/annotation/MapperScan.java` |
| 行数 | 209 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis-spring/3.0.5/mybatis-spring-3.0.5-sources.jar |
| 抓取时间 | 2026-11-25T15:01:26.060Z |

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
   19 | import java.lang.annotation.Documented;
   20 | import java.lang.annotation.ElementType;
   21 | import java.lang.annotation.Repeatable;
   22 | import java.lang.annotation.Retention;
   23 | import java.lang.annotation.RetentionPolicy;
   24 | import java.lang.annotation.Target;
   25 | 
   26 | import org.mybatis.spring.mapper.MapperFactoryBean;
   27 | import org.mybatis.spring.mapper.MapperScannerConfigurer;
   28 | import org.springframework.beans.factory.support.AbstractBeanDefinition;
   29 | import org.springframework.beans.factory.support.BeanNameGenerator;
   30 | import org.springframework.context.annotation.ComponentScan;
   31 | import org.springframework.context.annotation.Import;
   32 | import org.springframework.core.annotation.AliasFor;
   33 | 
   34 | /**
   35 |  * Use this annotation to register MyBatis mapper interfaces when using Java Config. It performs when same work as
   36 |  * {@link MapperScannerConfigurer} via {@link MapperScannerRegistrar}.
   37 |  * <p>
   38 |  * Either {@link #basePackageClasses} or {@link #basePackages} (or its alias {@link #value}) may be specified to define
   39 |  * specific packages to scan. Since 2.0.4, If specific packages are not defined, scanning will occur from the package of
   40 |  * the class that declares this annotation.
   41 |  * <p>
   42 |  * Configuration example:
   43 |  * </p>
   44 |  *
   45 |  * <pre class="code">
   46 |  * &#064;Configuration
   47 |  * &#064;MapperScan("org.mybatis.spring.sample.mapper")
   48 |  * public class AppConfig {
   49 |  *
   50 |  *   &#064;Bean
   51 |  *   public DataSource dataSource() {
   52 |  *     return new EmbeddedDatabaseBuilder().addScript("schema.sql").build();
   53 |  *   }
   54 |  *
   55 |  *   &#064;Bean
   56 |  *   public DataSourceTransactionManager transactionManager() {
   57 |  *     return new DataSourceTransactionManager(dataSource());
   58 |  *   }
   59 |  *
   60 |  *   &#064;Bean
   61 |  *   public SqlSessionFactory sqlSessionFactory() throws Exception {
   62 |  *     SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
   63 |  *     sessionFactory.setDataSource(dataSource());
   64 |  *     return sessionFactory.getObject();
   65 |  *   }
   66 |  * }
   67 |  * </pre>
   68 |  *
   69 |  * @author Michael Lanyon
   70 |  * @author Eduardo Macarron
   71 |  * @author Qimiao Chen
   72 |  *
   73 |  * @since 1.2.0
   74 |  *
   75 |  * @see MapperScannerRegistrar
   76 |  * @see MapperFactoryBean
   77 |  */
   78 | @Retention(RetentionPolicy.RUNTIME)
   79 | @Target(ElementType.TYPE)
   80 | @Documented
   81 | @Import(MapperScannerRegistrar.class)
   82 | @Repeatable(MapperScans.class)
   83 | public @interface MapperScan {
   84 | 
   85 |   /**
   86 |    * Alias for the {@link #basePackages()} attribute. Allows for more concise annotation declarations e.g.:
   87 |    * {@code @MapperScan("org.my.pkg")} instead of {@code @MapperScan(basePackages = "org.my.pkg"})}.
   88 |    *
   89 |    * @return base package names
   90 |    */
   91 |   @AliasFor("basePackages")
   92 |   String[] value() default {};
   93 | 
   94 |   /**
   95 |    * Base packages to scan for MyBatis interfaces. Note that only interfaces with at least one method will be
   96 |    * registered; concrete classes will be ignored.
   97 |    *
   98 |    * @return base package names for scanning mapper interface
   99 |    */
  100 |   @AliasFor("value")
  101 |   String[] basePackages() default {};
  102 | 
  103 |   /**
  104 |    * Type-safe alternative to {@link #basePackages()} for specifying the packages to scan for annotated components. The
  105 |    * package of each class specified will be scanned.
  106 |    * <p>
  107 |    * Consider creating a special no-op marker class or interface in each package that serves no purpose other than being
  108 |    * referenced by this attribute.
  109 |    *
  110 |    * @return classes that indicate base package for scanning mapper interface
  111 |    */
  112 |   Class<?>[] basePackageClasses() default {};
  113 | 
  114 |   /**
  115 |    * The {@link BeanNameGenerator} class to be used for naming detected components within the Spring container.
  116 |    *
  117 |    * @return the class of {@link BeanNameGenerator}
  118 |    */
  119 |   Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;
  120 | 
  121 |   /**
  122 |    * This property specifies the annotation that the scanner will search for.
  123 |    * <p>
  124 |    * The scanner will register all interfaces in the base package that also have the specified annotation.
  125 |    * <p>
  126 |    * Note this can be combined with markerInterface.
  127 |    *
  128 |    * @return the annotation that the scanner will search for
  129 |    */
  130 |   Class<? extends Annotation> annotationClass() default Annotation.class;
  131 | 
  132 |   /**
  133 |    * This property specifies the parent that the scanner will search for.
  134 |    * <p>
  135 |    * The scanner will register all interfaces in the base package that also have the specified interface class as a
  136 |    * parent.
  137 |    * <p>
  138 |    * Note this can be combined with annotationClass.
  139 |    *
  140 |    * @return the parent that the scanner will search for
  141 |    */
  142 |   Class<?> markerInterface() default Class.class;
  143 | 
  144 |   /**
  145 |    * Specifies which {@code SqlSessionTemplate} to use in the case that there is more than one in the spring context.
  146 |    * Usually this is only needed when you have more than one datasource.
  147 |    *
  148 |    * @return the bean name of {@code SqlSessionTemplate}
  149 |    */
  150 |   String sqlSessionTemplateRef() default "";
  151 | 
  152 |   /**
  153 |    * Specifies which {@code SqlSessionFactory} to use in the case that there is more than one in the spring context.
  154 |    * Usually this is only needed when you have more than one datasource.
  155 |    *
  156 |    * @return the bean name of {@code SqlSessionFactory}
  157 |    */
  158 |   String sqlSessionFactoryRef() default "";
  159 | 
  160 |   /**
  161 |    * Specifies a custom MapperFactoryBean to return a mybatis proxy as spring bean.
  162 |    *
  163 |    * @return the class of {@code MapperFactoryBean}
  164 |    */
  165 |   Class<? extends MapperFactoryBean> factoryBean() default MapperFactoryBean.class;
  166 | 
  167 |   /**
  168 |    * Whether enable lazy initialization of mapper bean.
  169 |    * <p>
  170 |    * Default is {@code false}.
  171 |    * </p>
  172 |    *
  173 |    * @return set {@code true} to enable lazy initialization
  174 |    *
  175 |    * @since 2.0.2
  176 |    */
  177 |   String lazyInitialization() default "";
  178 | 
  179 |   /**
  180 |    * Specifies the default scope of scanned mappers.
  181 |    * <p>
  182 |    * Default is {@code ""} (equiv to singleton).
  183 |    * </p>
  184 |    *
  185 |    * @return the default scope
  186 |    */
  187 |   String defaultScope() default AbstractBeanDefinition.SCOPE_DEFAULT;
  188 | 
  189 |   /**
  190 |    * Specifies a flag that whether execute a property placeholder processing or not.
  191 |    * <p>
  192 |    * The default is {@literal true}. This means that a property placeholder processing execute.
  193 |    *
  194 |    * @since 3.0.3
  195 |    *
  196 |    * @return a flag that whether execute a property placeholder processing or not
  197 |    */
  198 |   boolean processPropertyPlaceHolders() default true;
  199 | 
  200 |   /**
  201 |    * Specifies which types are not eligible for mapper scanning.
  202 |    *
  203 |    * @since 3.0.4
  204 |    *
  205 |    * @return array of customized mapper excludeFilter
  206 |    */
  207 |   ComponentScan.Filter[] excludeFilters() default {};
  208 | 
  209 | }
```
