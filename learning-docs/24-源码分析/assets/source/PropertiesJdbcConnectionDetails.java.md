<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-jdbc:4.2.0 | path: org/springframework/boot/jdbc/autoconfigure/PropertiesJdbcConnectionDetails.java -->
<!-- 正文引用：[[源码:PropertiesJdbcConnectionDetails.java#L起-L止]] ；代码块头部：(src: PropertiesJdbcConnectionDetails.java#L起-L止) -->

# PropertiesJdbcConnectionDetails.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-jdbc:4.2.0` |
| 路径 | `org/springframework/boot/jdbc/autoconfigure/PropertiesJdbcConnectionDetails.java` |
| 行数 | 49 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-jdbc/4.2.0/spring-boot-jdbc-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:09:22.464Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.jdbc.autoconfigure;
    6 | 
    7 | import org.jspecify.annotations.Nullable;
    8 | 
    9 | /**
   10 |  * Adapts {@link DataSourceProperties} to {@link JdbcConnectionDetails}.
   11 |  *
   12 |  * @author Andy Wilkinson
   13 |  */
   14 | final class PropertiesJdbcConnectionDetails implements JdbcConnectionDetails {
   15 | 
   16 | 	private final DataSourceProperties properties;
   17 | 
   18 | 	PropertiesJdbcConnectionDetails(DataSourceProperties properties) {
   19 | 		this.properties = properties;
   20 | 	}
   21 | 
   22 | 	@Override
   23 | 	public @Nullable String getUsername() {
   24 | 		return this.properties.determineUsername();
   25 | 	}
   26 | 
   27 | 	@Override
   28 | 	public @Nullable String getPassword() {
   29 | 		return this.properties.determinePassword();
   30 | 	}
   31 | 
   32 | 	@Override
   33 | 	public String getJdbcUrl() {
   34 | 		return this.properties.determineUrl();
   35 | 	}
   36 | 
   37 | 	@Override
   38 | 	public String getDriverClassName() {
   39 | 		return this.properties.determineDriverClassName();
   40 | 	}
   41 | 
   42 | 	@Override
   43 | 	public @Nullable String getXaDataSourceClassName() {
   44 | 		return (this.properties.getXa().getDataSourceClassName() != null)
   45 | 				? this.properties.getXa().getDataSourceClassName()
   46 | 				: JdbcConnectionDetails.super.getXaDataSourceClassName();
   47 | 	}
   48 | 
   49 | }
```
