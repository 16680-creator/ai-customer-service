<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis-spring:3.0.5 | path: org/mybatis/spring/mapper/MapperFactoryBean.java -->
<!-- 正文引用：[[源码:MapperFactoryBean.java#L起-L止]] ；代码块头部：(src: MapperFactoryBean.java#L起-L止) -->

# MapperFactoryBean.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis-spring:3.0.5` |
| 路径 | `org/mybatis/spring/mapper/MapperFactoryBean.java` |
| 行数 | 145 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis-spring/3.0.5/mybatis-spring-3.0.5-sources.jar |
| 抓取时间 | 2026-11-25T15:01:25.197Z |

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
   20 | import org.apache.ibatis.executor.ErrorContext;
   21 | import org.mybatis.spring.SqlSessionTemplate;
   22 | import org.mybatis.spring.support.SqlSessionDaoSupport;
   23 | import org.springframework.beans.factory.FactoryBean;
   24 | 
   25 | /**
   26 |  * BeanFactory that enables injection of MyBatis mapper interfaces. It can be set up with a SqlSessionFactory or a
   27 |  * pre-configured SqlSessionTemplate.
   28 |  * <p>
   29 |  * Sample configuration:
   30 |  *
   31 |  * <pre class="code">
   32 |  * {@code
   33 |  *   <bean id="baseMapper" class="org.mybatis.spring.mapper.MapperFactoryBean" abstract="true" lazy-init="true">
   34 |  *     <property name="sqlSessionFactory" ref="sqlSessionFactory" />
   35 |  *   </bean>
   36 |  *
   37 |  *   <bean id="oneMapper" parent="baseMapper">
   38 |  *     <property name="mapperInterface" value="my.package.MyMapperInterface" />
   39 |  *   </bean>
   40 |  *
   41 |  *   <bean id="anotherMapper" parent="baseMapper">
   42 |  *     <property name="mapperInterface" value="my.package.MyAnotherMapperInterface" />
   43 |  *   </bean>
   44 |  * }
   45 |  * </pre>
   46 |  * <p>
   47 |  * Note that this factory can only inject <em>interfaces</em>, not concrete classes.
   48 |  *
   49 |  * @author Eduardo Macarron
   50 |  *
   51 |  * @see SqlSessionTemplate
   52 |  */
   53 | public class MapperFactoryBean<T> extends SqlSessionDaoSupport implements FactoryBean<T> {
   54 | 
   55 |   private Class<T> mapperInterface;
   56 | 
   57 |   private boolean addToConfig = true;
   58 | 
   59 |   public MapperFactoryBean() {
   60 |     // intentionally empty
   61 |   }
   62 | 
   63 |   public MapperFactoryBean(Class<T> mapperInterface) {
   64 |     this.mapperInterface = mapperInterface;
   65 |   }
   66 | 
   67 |   @Override
   68 |   protected void checkDaoConfig() {
   69 |     super.checkDaoConfig();
   70 | 
   71 |     notNull(this.mapperInterface, "Property 'mapperInterface' is required");
   72 | 
   73 |     var configuration = getSqlSession().getConfiguration();
   74 |     if (this.addToConfig && !configuration.hasMapper(this.mapperInterface)) {
   75 |       try {
   76 |         configuration.addMapper(this.mapperInterface);
   77 |       } catch (Exception e) {
   78 |         logger.error("Error while adding the mapper '" + this.mapperInterface + "' to configuration.", e);
   79 |         throw new IllegalArgumentException(e);
   80 |       } finally {
   81 |         ErrorContext.instance().reset();
   82 |       }
   83 |     }
   84 |   }
   85 | 
   86 |   @Override
   87 |   public T getObject() throws Exception {
   88 |     return getSqlSession().getMapper(this.mapperInterface);
   89 |   }
   90 | 
   91 |   @Override
   92 |   public Class<T> getObjectType() {
   93 |     return this.mapperInterface;
   94 |   }
   95 | 
   96 |   @Override
   97 |   public boolean isSingleton() {
   98 |     return true;
   99 |   }
  100 | 
  101 |   // ------------- mutators --------------
  102 | 
  103 |   /**
  104 |    * Sets the mapper interface of the MyBatis mapper
  105 |    *
  106 |    * @param mapperInterface
  107 |    *          class of the interface
  108 |    */
  109 |   public void setMapperInterface(Class<T> mapperInterface) {
  110 |     this.mapperInterface = mapperInterface;
  111 |   }
  112 | 
  113 |   /**
  114 |    * Return the mapper interface of the MyBatis mapper
  115 |    *
  116 |    * @return class of the interface
  117 |    */
  118 |   public Class<T> getMapperInterface() {
  119 |     return mapperInterface;
  120 |   }
  121 | 
  122 |   /**
  123 |    * If addToConfig is false the mapper will not be added to MyBatis. This means it must have been included in
  124 |    * mybatis-config.xml.
  125 |    * <p>
  126 |    * If it is true, the mapper will be added to MyBatis in the case it is not already registered.
  127 |    * <p>
  128 |    * By default addToConfig is true.
  129 |    *
  130 |    * @param addToConfig
  131 |    *          a flag that whether add mapper to MyBatis or not
  132 |    */
  133 |   public void setAddToConfig(boolean addToConfig) {
  134 |     this.addToConfig = addToConfig;
  135 |   }
  136 | 
  137 |   /**
  138 |    * Return the flag for addition into MyBatis config.
  139 |    *
  140 |    * @return true if the mapper will be added to MyBatis in the case it is not already registered.
  141 |    */
  142 |   public boolean isAddToConfig() {
  143 |     return addToConfig;
  144 |   }
  145 | }
```
