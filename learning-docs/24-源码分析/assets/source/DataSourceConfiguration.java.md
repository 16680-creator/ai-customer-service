<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-jdbc:4.2.0 | path: org/springframework/boot/jdbc/autoconfigure/DataSourceConfiguration.java -->
<!-- 正文引用：[[源码:DataSourceConfiguration.java#L起-L止]] ；代码块头部：(src: DataSourceConfiguration.java#L起-L止) -->

# DataSourceConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-jdbc:4.2.0` |
| 路径 | `org/springframework/boot/jdbc/autoconfigure/DataSourceConfiguration.java` |
| 行数 | 197 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-jdbc/4.2.0/spring-boot-jdbc-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:09:22.198Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.jdbc.autoconfigure;
    6 | 
    7 | import java.sql.SQLException;
    8 | 
    9 | import javax.sql.DataSource;
   10 | 
   11 | import com.zaxxer.hikari.HikariDataSource;
   12 | import oracle.jdbc.OracleConnection;
   13 | import oracle.ucp.jdbc.PoolDataSourceImpl;
   14 | import org.jspecify.annotations.Nullable;
   15 | 
   16 | import org.springframework.beans.factory.ObjectProvider;
   17 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   18 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   19 | import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
   20 | import org.springframework.boot.context.properties.ConfigurationProperties;
   21 | import org.springframework.boot.jdbc.DataSourceBuilder;
   22 | import org.springframework.boot.jdbc.DatabaseDriver;
   23 | import org.springframework.context.annotation.Bean;
   24 | import org.springframework.context.annotation.Configuration;
   25 | import org.springframework.core.env.Environment;
   26 | import org.springframework.util.StringUtils;
   27 | 
   28 | /**
   29 |  * Actual DataSource configurations imported by {@link DataSourceAutoConfiguration}.
   30 |  *
   31 |  * @author Dave Syer
   32 |  * @author Phillip Webb
   33 |  * @author Stephane Nicoll
   34 |  * @author Fabio Grassi
   35 |  * @author Moritz Halbritter
   36 |  * @author Andy Wilkinson
   37 |  */
   38 | abstract class DataSourceConfiguration {
   39 | 
   40 | 	@SuppressWarnings("unchecked")
   41 | 	private static <T> T createDataSource(JdbcConnectionDetails connectionDetails,
   42 | 			@Nullable Class<? extends DataSource> type, ClassLoader classLoader) {
   43 | 		return createDataSource(connectionDetails, type, classLoader, true);
   44 | 	}
   45 | 
   46 | 	@SuppressWarnings("unchecked")
   47 | 	private static <T> T createDataSource(JdbcConnectionDetails connectionDetails,
   48 | 			@Nullable Class<? extends DataSource> type, ClassLoader classLoader, boolean applyDriverClassName) {
   49 | 		DataSourceBuilder<? extends DataSource> builder = DataSourceBuilder.create(classLoader).type(type);
   50 | 		if (applyDriverClassName) {
   51 | 			builder.driverClassName(connectionDetails.getDriverClassName());
   52 | 		}
   53 | 		return (T) builder.url(connectionDetails.getJdbcUrl())
   54 | 			.username(connectionDetails.getUsername())
   55 | 			.password(connectionDetails.getPassword())
   56 | 			.build();
   57 | 	}
   58 | 
   59 | 	/**
   60 | 	 * Tomcat Pool DataSource configuration.
   61 | 	 */
   62 | 	@Configuration(proxyBeanMethods = false)
   63 | 	@ConditionalOnClass(org.apache.tomcat.jdbc.pool.DataSource.class)
   64 | 	@ConditionalOnMissingBean(DataSource.class)
   65 | 	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.tomcat.jdbc.pool.DataSource",
   66 | 			matchIfMissing = true)
   67 | 	static class Tomcat {
   68 | 
   69 | 		@Bean
   70 | 		@ConditionalOnMissingBean(PropertiesJdbcConnectionDetails.class)
   71 | 		static TomcatJdbcConnectionDetailsBeanPostProcessor tomcatJdbcConnectionDetailsBeanPostProcessor(
   72 | 				ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
   73 | 			return new TomcatJdbcConnectionDetailsBeanPostProcessor(connectionDetailsProvider);
   74 | 		}
   75 | 
   76 | 		@Bean
   77 | 		@ConfigurationProperties("spring.datasource.tomcat")
   78 | 		org.apache.tomcat.jdbc.pool.DataSource dataSource(DataSourceProperties properties,
   79 | 				JdbcConnectionDetails connectionDetails) {
   80 | 			Class<? extends DataSource> dataSourceType = org.apache.tomcat.jdbc.pool.DataSource.class;
   81 | 			org.apache.tomcat.jdbc.pool.DataSource dataSource = createDataSource(connectionDetails, dataSourceType,
   82 | 					properties.getClassLoader());
   83 | 			String validationQuery;
   84 | 			DatabaseDriver databaseDriver = DatabaseDriver.fromJdbcUrl(connectionDetails.getJdbcUrl());
   85 | 			validationQuery = databaseDriver.getValidationQuery();
   86 | 			if (validationQuery != null) {
   87 | 				dataSource.setTestOnBorrow(true);
   88 | 				dataSource.setValidationQuery(validationQuery);
   89 | 			}
   90 | 			return dataSource;
   91 | 		}
   92 | 
   93 | 	}
   94 | 
   95 | 	/**
   96 | 	 * Hikari DataSource configuration.
   97 | 	 */
   98 | 	@Configuration(proxyBeanMethods = false)
   99 | 	@ConditionalOnClass(HikariDataSource.class)
  100 | 	@ConditionalOnMissingBean(DataSource.class)
  101 | 	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "com.zaxxer.hikari.HikariDataSource",
  102 | 			matchIfMissing = true)
  103 | 	static class Hikari {
  104 | 
  105 | 		@Bean
  106 | 		static HikariJdbcConnectionDetailsBeanPostProcessor jdbcConnectionDetailsHikariBeanPostProcessor(
  107 | 				ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
  108 | 			return new HikariJdbcConnectionDetailsBeanPostProcessor(connectionDetailsProvider);
  109 | 		}
  110 | 
  111 | 		@Bean
  112 | 		@ConfigurationProperties("spring.datasource.hikari")
  113 | 		HikariDataSource dataSource(DataSourceProperties properties, JdbcConnectionDetails connectionDetails,
  114 | 				Environment environment) {
  115 | 			String dataSourceClassName = environment.getProperty("spring.datasource.hikari.data-source-class-name");
  116 | 			HikariDataSource dataSource = createDataSource(connectionDetails, HikariDataSource.class,
  117 | 					properties.getClassLoader(), dataSourceClassName == null);
  118 | 			if (StringUtils.hasText(properties.getName())) {
  119 | 				dataSource.setPoolName(properties.getName());
  120 | 			}
  121 | 			return dataSource;
  122 | 		}
  123 | 
  124 | 	}
  125 | 
  126 | 	/**
  127 | 	 * DBCP DataSource configuration.
  128 | 	 */
  129 | 	@Configuration(proxyBeanMethods = false)
  130 | 	@ConditionalOnClass(org.apache.commons.dbcp2.BasicDataSource.class)
  131 | 	@ConditionalOnMissingBean(DataSource.class)
  132 | 	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "org.apache.commons.dbcp2.BasicDataSource",
  133 | 			matchIfMissing = true)
  134 | 	static class Dbcp2 {
  135 | 
  136 | 		@Bean
  137 | 		static Dbcp2JdbcConnectionDetailsBeanPostProcessor dbcp2JdbcConnectionDetailsBeanPostProcessor(
  138 | 				ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
  139 | 			return new Dbcp2JdbcConnectionDetailsBeanPostProcessor(connectionDetailsProvider);
  140 | 		}
  141 | 
  142 | 		@Bean
  143 | 		@ConfigurationProperties("spring.datasource.dbcp2")
  144 | 		org.apache.commons.dbcp2.BasicDataSource dataSource(DataSourceProperties properties,
  145 | 				JdbcConnectionDetails connectionDetails) {
  146 | 			Class<? extends DataSource> dataSourceType = org.apache.commons.dbcp2.BasicDataSource.class;
  147 | 			return createDataSource(connectionDetails, dataSourceType, properties.getClassLoader());
  148 | 		}
  149 | 
  150 | 	}
  151 | 
  152 | 	/**
  153 | 	 * Oracle UCP DataSource configuration.
  154 | 	 */
  155 | 	@Configuration(proxyBeanMethods = false)
  156 | 	@ConditionalOnClass({ PoolDataSourceImpl.class, OracleConnection.class })
  157 | 	@ConditionalOnMissingBean(DataSource.class)
  158 | 	@ConditionalOnProperty(name = "spring.datasource.type", havingValue = "oracle.ucp.jdbc.PoolDataSource",
  159 | 			matchIfMissing = true)
  160 | 	static class OracleUcp {
  161 | 
  162 | 		@Bean
  163 | 		static OracleUcpJdbcConnectionDetailsBeanPostProcessor oracleUcpJdbcConnectionDetailsBeanPostProcessor(
  164 | 				ObjectProvider<JdbcConnectionDetails> connectionDetailsProvider) {
  165 | 			return new OracleUcpJdbcConnectionDetailsBeanPostProcessor(connectionDetailsProvider);
  166 | 		}
  167 | 
  168 | 		@Bean
  169 | 		@ConfigurationProperties("spring.datasource.oracleucp")
  170 | 		PoolDataSourceImpl dataSource(DataSourceProperties properties, JdbcConnectionDetails connectionDetails)
  171 | 				throws SQLException {
  172 | 			PoolDataSourceImpl dataSource = createDataSource(connectionDetails, PoolDataSourceImpl.class,
  173 | 					properties.getClassLoader());
  174 | 			if (StringUtils.hasText(properties.getName())) {
  175 | 				dataSource.setConnectionPoolName(properties.getName());
  176 | 			}
  177 | 			return dataSource;
  178 | 		}
  179 | 
  180 | 	}
  181 | 
  182 | 	/**
  183 | 	 * Generic DataSource configuration.
  184 | 	 */
  185 | 	@Configuration(proxyBeanMethods = false)
  186 | 	@ConditionalOnMissingBean(DataSource.class)
  187 | 	@ConditionalOnProperty(name = "spring.datasource.type")
  188 | 	static class Generic {
  189 | 
  190 | 		@Bean
  191 | 		DataSource dataSource(DataSourceProperties properties, JdbcConnectionDetails connectionDetails) {
  192 | 			return createDataSource(connectionDetails, properties.getType(), properties.getClassLoader());
  193 | 		}
  194 | 
  195 | 	}
  196 | 
  197 | }
```
