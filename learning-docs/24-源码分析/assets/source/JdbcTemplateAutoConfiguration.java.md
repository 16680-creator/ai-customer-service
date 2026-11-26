<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-jdbc:4.2.0 | path: org/springframework/boot/jdbc/autoconfigure/JdbcTemplateAutoConfiguration.java -->
<!-- 正文引用：[[源码:JdbcTemplateAutoConfiguration.java#L起-L止]] ；代码块头部：(src: JdbcTemplateAutoConfiguration.java#L起-L止) -->

# JdbcTemplateAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-jdbc:4.2.0` |
| 路径 | `org/springframework/boot/jdbc/autoconfigure/JdbcTemplateAutoConfiguration.java` |
| 行数 | 37 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-jdbc/4.2.0/spring-boot-jdbc-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:09:22.728Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.jdbc.autoconfigure;
    6 | 
    7 | import javax.sql.DataSource;
    8 | 
    9 | import org.springframework.boot.autoconfigure.AutoConfiguration;
   10 | import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
   11 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   12 | import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
   13 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   14 | import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
   15 | import org.springframework.context.annotation.Import;
   16 | import org.springframework.jdbc.core.JdbcTemplate;
   17 | import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
   18 | 
   19 | /**
   20 |  * {@link EnableAutoConfiguration Auto-configuration} for {@link JdbcTemplate} and
   21 |  * {@link NamedParameterJdbcTemplate}.
   22 |  *
   23 |  * @author Dave Syer
   24 |  * @author Phillip Webb
   25 |  * @author Stephane Nicoll
   26 |  * @author Kazuki Shimizu
   27 |  * @since 4.0.0
   28 |  */
   29 | @AutoConfiguration(after = DataSourceAutoConfiguration.class)
   30 | @ConditionalOnClass({ DataSource.class, JdbcTemplate.class })
   31 | @ConditionalOnSingleCandidate(DataSource.class)
   32 | @EnableConfigurationProperties(JdbcProperties.class)
   33 | @Import({ DatabaseInitializationDependencyConfigurer.class, JdbcTemplateConfiguration.class,
   34 | 		NamedParameterJdbcTemplateConfiguration.class })
   35 | public final class JdbcTemplateAutoConfiguration {
   36 | 
   37 | }
```
