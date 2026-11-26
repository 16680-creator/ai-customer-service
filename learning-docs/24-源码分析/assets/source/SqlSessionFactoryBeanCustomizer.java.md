<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis.spring.boot:mybatis-spring-boot-autoconfigure:4.1.0 | path: org/mybatis/spring/boot/autoconfigure/SqlSessionFactoryBeanCustomizer.java -->
<!-- 正文引用：[[源码:SqlSessionFactoryBeanCustomizer.java#L起-L止]] ；代码块头部：(src: SqlSessionFactoryBeanCustomizer.java#L起-L止) -->

# SqlSessionFactoryBeanCustomizer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis.spring.boot:mybatis-spring-boot-autoconfigure:4.1.0` |
| 路径 | `org/mybatis/spring/boot/autoconfigure/SqlSessionFactoryBeanCustomizer.java` |
| 行数 | 38 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/spring/boot/mybatis-spring-boot-autoconfigure/4.1.0/mybatis-spring-boot-autoconfigure-4.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:01:44.908Z |

```java
    1 | /*
    2 |  *    Copyright 2015-2022 the original author or authors.
    3 |  *
    4 |  *    Licensed under the Apache License, Version 2.0 (the "License");
    5 |  *    you may not use this file except in compliance with the License.
    6 |  *    You may obtain a copy of the License at
    7 |  *
    8 |  *       https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  *    Unless required by applicable law or agreed to in writing, software
   11 |  *    distributed under the License is distributed on an "AS IS" BASIS,
   12 |  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  *    See the License for the specific language governing permissions and
   14 |  *    limitations under the License.
   15 |  */
   16 | package org.mybatis.spring.boot.autoconfigure;
   17 | 
   18 | import org.mybatis.spring.SqlSessionFactoryBean;
   19 | 
   20 | /**
   21 |  * Callback interface that can be customized a {@link SqlSessionFactoryBean} object generated on auto-configuration.
   22 |  *
   23 |  * @author Kazuki Shimizu
   24 |  *
   25 |  * @since 2.2.2
   26 |  */
   27 | @FunctionalInterface
   28 | public interface SqlSessionFactoryBeanCustomizer {
   29 | 
   30 |   /**
   31 |    * Customize the given a {@link SqlSessionFactoryBean} object.
   32 |    *
   33 |    * @param factoryBean
   34 |    *          the factory bean object to customize
   35 |    */
   36 |   void customize(SqlSessionFactoryBean factoryBean);
   37 | 
   38 | }
```
