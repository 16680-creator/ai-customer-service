<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-jdbc:4.2.0 | path: org/springframework/boot/jdbc/autoconfigure/DataSourceAutoConfiguration.java -->
<!-- 正文引用：[[源码:DataSourceAutoConfiguration.java#L起-L止]] ；代码块头部：(src: DataSourceAutoConfiguration.java#L起-L止) -->

# DataSourceAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-jdbc:4.2.0` |
| 路径 | `org/springframework/boot/jdbc/autoconfigure/DataSourceAutoConfiguration.java` |
| 行数 | 164 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-jdbc/4.2.0/spring-boot-jdbc-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:52.277Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.jdbc.autoconfigure;
    6 | 
    7 | import javax.sql.DataSource;
    8 | import javax.sql.XADataSource;
    9 | 
   10 | import org.springframework.boot.autoconfigure.AutoConfiguration;
   11 | import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
   12 | import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
   13 | import org.springframework.boot.autoconfigure.condition.ConditionMessage;
   14 | import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
   15 | import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
   16 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   17 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   18 | import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
   19 | import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
   20 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   21 | import org.springframework.boot.jdbc.DataSourceBuilder;
   22 | import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
   23 | import org.springframework.context.annotation.Bean;
   24 | import org.springframework.context.annotation.Condition;
   25 | import org.springframework.context.annotation.ConditionContext;
   26 | import org.springframework.context.annotation.Conditional;
   27 | import org.springframework.context.annotation.Configuration;
   28 | import org.springframework.context.annotation.Import;
   29 | import org.springframework.core.env.Environment;
   30 | import org.springframework.core.type.AnnotatedTypeMetadata;
   31 | import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
   32 | import org.springframework.util.ClassUtils;
   33 | import org.springframework.util.StringUtils;
   34 | 
   35 | /**
   36 |  * {@link EnableAutoConfiguration Auto-configuration} for {@link DataSource}.
   37 |  *
   38 |  * @author Dave Syer
   39 |  * @author Phillip Webb
   40 |  * @author Stephane Nicoll
   41 |  * @author Kazuki Shimizu
   42 |  * @author Olga Maciaszek-Sharma
   43 |  * @since 4.0.0
   44 |  */
   45 | @AutoConfiguration(before = DataSourceInitializationAutoConfiguration.class)
   46 | @ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
   47 | @ConditionalOnMissingBean(type = "io.r2dbc.spi.ConnectionFactory")
   48 | @EnableConfigurationProperties(DataSourceProperties.class)
   49 | @Import({ DataSourcePoolMetadataProvidersConfiguration.class, DataSourceCheckpointRestoreConfiguration.class })
   50 | public final class DataSourceAutoConfiguration {
   51 | 
   52 | 	@Configuration(proxyBeanMethods = false)
   53 | 	@Conditional(EmbeddedDatabaseCondition.class)
   54 | 	@ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
   55 | 	@Import(EmbeddedDataSourceConfiguration.class)
   56 | 	protected static class EmbeddedDatabaseConfiguration {
   57 | 
   58 | 	}
   59 | 
   60 | 	@Configuration(proxyBeanMethods = false)
   61 | 	@Conditional(PooledDataSourceCondition.class)
   62 | 	@ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
   63 | 	@Import({ DataSourceConfiguration.Hikari.class, DataSourceConfiguration.Tomcat.class,
   64 | 			DataSourceConfiguration.Dbcp2.class, DataSourceConfiguration.OracleUcp.class,
   65 | 			DataSourceConfiguration.Generic.class, DataSourceJmxConfiguration.class,
   66 | 			LazyConnectionDataSourceConfiguration.class })
   67 | 	protected static class PooledDataSourceConfiguration {
   68 | 
   69 | 		@Bean
   70 | 		@ConditionalOnMissingBean(JdbcConnectionDetails.class)
   71 | 		PropertiesJdbcConnectionDetails jdbcConnectionDetails(DataSourceProperties properties) {
   72 | 			return new PropertiesJdbcConnectionDetails(properties);
   73 | 		}
   74 | 
   75 | 	}
   76 | 
   77 | 	/**
   78 | 	 * {@link AnyNestedCondition} that checks that either {@code spring.datasource.type}
   79 | 	 * is set or {@link PooledDataSourceAvailableCondition} applies.
   80 | 	 */
   81 | 	static class PooledDataSourceCondition extends AnyNestedCondition {
   82 | 
   83 | 		PooledDataSourceCondition() {
   84 | 			super(ConfigurationPhase.PARSE_CONFIGURATION);
   85 | 		}
   86 | 
   87 | 		@ConditionalOnProperty("spring.datasource.type")
   88 | 		static class ExplicitType {
   89 | 
   90 | 		}
   91 | 
   92 | 		@Conditional(PooledDataSourceAvailableCondition.class)
   93 | 		static class PooledDataSourceAvailable {
   94 | 
   95 | 		}
   96 | 
   97 | 	}
   98 | 
   99 | 	/**
  100 | 	 * {@link Condition} to test if a supported connection pool is available.
  101 | 	 */
  102 | 	static class PooledDataSourceAvailableCondition extends SpringBootCondition {
  103 | 
  104 | 		@Override
  105 | 		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
  106 | 			ConditionMessage.Builder message = ConditionMessage.forCondition("PooledDataSource");
  107 | 			if (DataSourceBuilder.findType(context.getClassLoader()) != null) {
  108 | 				return ConditionOutcome.match(message.foundExactly("supported DataSource"));
  109 | 			}
  110 | 			return ConditionOutcome.noMatch(message.didNotFind("supported DataSource").atAll());
  111 | 		}
  112 | 
  113 | 	}
  114 | 
  115 | 	/**
  116 | 	 * {@link Condition} to detect when an embedded {@link DataSource} type can be used.
  117 | 	 * If a pooled {@link DataSource} is available, it will always be preferred to an
  118 | 	 * {@code EmbeddedDatabase}.
  119 | 	 */
  120 | 	static class EmbeddedDatabaseCondition extends SpringBootCondition {
  121 | 
  122 | 		private static final String DATASOURCE_URL_PROPERTY = "spring.datasource.url";
  123 | 
  124 | 		private static final String EMBEDDED_DATABASE_TYPE = "org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType";
  125 | 
  126 | 		private final SpringBootCondition pooledCondition = new PooledDataSourceCondition();
  127 | 
  128 | 		@Override
  129 | 		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
  130 | 			ConditionMessage.Builder message = ConditionMessage.forCondition("EmbeddedDataSource");
  131 | 			if (hasDataSourceUrlProperty(context)) {
  132 | 				return ConditionOutcome.noMatch(message.because(DATASOURCE_URL_PROPERTY + " is set"));
  133 | 			}
  134 | 			if (anyMatches(context, metadata, this.pooledCondition)) {
  135 | 				return ConditionOutcome.noMatch(message.foundExactly("supported pooled data source"));
  136 | 			}
  137 | 			if (!ClassUtils.isPresent(EMBEDDED_DATABASE_TYPE, context.getClassLoader())) {
  138 | 				return ConditionOutcome
  139 | 					.noMatch(message.didNotFind("required class").items(Style.QUOTE, EMBEDDED_DATABASE_TYPE));
  140 | 			}
  141 | 			EmbeddedDatabaseType type = EmbeddedDatabaseConnection.get(context.getClassLoader()).getType();
  142 | 			if (type == null) {
  143 | 				return ConditionOutcome.noMatch(message.didNotFind("embedded database").atAll());
  144 | 			}
  145 | 			return ConditionOutcome.match(message.found("embedded database").items(type));
  146 | 		}
  147 | 
  148 | 		private boolean hasDataSourceUrlProperty(ConditionContext context) {
  149 | 			Environment environment = context.getEnvironment();
  150 | 			if (environment.containsProperty(DATASOURCE_URL_PROPERTY)) {
  151 | 				try {
  152 | 					return StringUtils.hasText(environment.getProperty(DATASOURCE_URL_PROPERTY));
  153 | 				}
  154 | 				catch (IllegalArgumentException ex) {
  155 | 					// NOTE: This should be PlaceholderResolutionException
  156 | 					// Ignore unresolvable placeholder errors
  157 | 				}
  158 | 			}
  159 | 			return false;
  160 | 		}
  161 | 
  162 | 	}
  163 | 
  164 | }
```
