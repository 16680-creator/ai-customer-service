<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-hibernate:4.2.0 | path: org/springframework/boot/hibernate/autoconfigure/HibernateJpaAutoConfiguration.java -->
<!-- 正文引用：[[源码:HibernateJpaAutoConfiguration.java#L起-L止]] ；代码块头部：(src: HibernateJpaAutoConfiguration.java#L起-L止) -->

# HibernateJpaAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-hibernate:4.2.0` |
| 路径 | `org/springframework/boot/hibernate/autoconfigure/HibernateJpaAutoConfiguration.java` |
| 行数 | 41 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-hibernate/4.2.0/spring-boot-hibernate-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:28:01.922Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.hibernate.autoconfigure;
    6 | 
    7 | import jakarta.persistence.EntityManager;
    8 | import org.hibernate.engine.spi.SessionImplementor;
    9 | 
   10 | import org.springframework.boot.autoconfigure.AutoConfiguration;
   11 | import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
   12 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   13 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   14 | import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
   15 | import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
   16 | import org.springframework.boot.jpa.autoconfigure.JpaProperties;
   17 | import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
   18 | import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizationAutoConfiguration;
   19 | import org.springframework.boot.transaction.jta.autoconfigure.JtaAutoConfiguration;
   20 | import org.springframework.context.annotation.Import;
   21 | import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
   22 | 
   23 | /**
   24 |  * {@link EnableAutoConfiguration Auto-configuration} for Hibernate JPA.
   25 |  *
   26 |  * @author Phillip Webb
   27 |  * @author Josh Long
   28 |  * @author Manuel Doninger
   29 |  * @author Andy Wilkinson
   30 |  * @since 4.0.0
   31 |  */
   32 | @AutoConfiguration(
   33 | 		after = { DataSourceAutoConfiguration.class, JtaAutoConfiguration.class,
   34 | 				TransactionManagerCustomizationAutoConfiguration.class },
   35 | 		before = { TransactionAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class })
   36 | @ConditionalOnClass({ LocalContainerEntityManagerFactoryBean.class, EntityManager.class, SessionImplementor.class })
   37 | @EnableConfigurationProperties(JpaProperties.class)
   38 | @Import(HibernateJpaConfiguration.class)
   39 | public final class HibernateJpaAutoConfiguration {
   40 | 
   41 | }
```
