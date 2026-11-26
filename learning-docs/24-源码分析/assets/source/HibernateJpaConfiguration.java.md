<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-hibernate:4.2.0 | path: org/springframework/boot/hibernate/autoconfigure/HibernateJpaConfiguration.java -->
<!-- 正文引用：[[源码:HibernateJpaConfiguration.java#L起-L止]] ；代码块头部：(src: HibernateJpaConfiguration.java#L起-L止) -->

# HibernateJpaConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-hibernate:4.2.0` |
| 路径 | `org/springframework/boot/hibernate/autoconfigure/HibernateJpaConfiguration.java` |
| 行数 | 189 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-hibernate/4.2.0/spring-boot-hibernate-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:28:42.241Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.hibernate.autoconfigure;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.Collection;
    9 | import java.util.LinkedHashMap;
   10 | import java.util.List;
   11 | import java.util.Map;
   12 | import java.util.function.Consumer;
   13 | import java.util.function.Supplier;
   14 | 
   15 | import javax.sql.DataSource;
   16 | 
   17 | import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
   18 | import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
   19 | import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
   20 | import org.jspecify.annotations.Nullable;
   21 | 
   22 | import org.springframework.aot.hint.MemberCategory;
   23 | import org.springframework.aot.hint.RuntimeHints;
   24 | import org.springframework.aot.hint.RuntimeHintsRegistrar;
   25 | import org.springframework.aot.hint.TypeHint;
   26 | import org.springframework.aot.hint.TypeHint.Builder;
   27 | import org.springframework.aot.hint.TypeReference;
   28 | import org.springframework.beans.factory.ObjectProvider;
   29 | import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
   30 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   31 | import org.springframework.boot.hibernate.SpringImplicitNamingStrategy;
   32 | import org.springframework.boot.hibernate.autoconfigure.HibernateJpaConfiguration.HibernateRuntimeHints;
   33 | import org.springframework.boot.jdbc.SchemaManagementProvider;
   34 | import org.springframework.boot.jdbc.metadata.CompositeDataSourcePoolMetadataProvider;
   35 | import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadata;
   36 | import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
   37 | import org.springframework.boot.jpa.autoconfigure.JpaBaseConfiguration;
   38 | import org.springframework.boot.jpa.autoconfigure.JpaProperties;
   39 | import org.springframework.context.annotation.Configuration;
   40 | import org.springframework.context.annotation.ImportRuntimeHints;
   41 | import org.springframework.jdbc.support.SQLExceptionTranslator;
   42 | import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
   43 | import org.springframework.transaction.jta.JtaTransactionManager;
   44 | 
   45 | /**
   46 |  * {@link JpaBaseConfiguration} implementation for Hibernate.
   47 |  *
   48 |  * @author Phillip Webb
   49 |  * @author Josh Long
   50 |  * @author Manuel Doninger
   51 |  * @author Andy Wilkinson
   52 |  * @author Stephane Nicoll
   53 |  * @author Moritz Halbritter
   54 |  */
   55 | @Configuration(proxyBeanMethods = false)
   56 | @EnableConfigurationProperties(HibernateProperties.class)
   57 | @ConditionalOnSingleCandidate(DataSource.class)
   58 | @ImportRuntimeHints(HibernateRuntimeHints.class)
   59 | class HibernateJpaConfiguration extends JpaBaseConfiguration {
   60 | 
   61 | 	private static final String PROVIDER_DISABLES_AUTOCOMMIT = "hibernate.connection.provider_disables_autocommit";
   62 | 
   63 | 	/**
   64 | 	 * {@code NoJtaPlatform} implementations for various Hibernate versions.
   65 | 	 */
   66 | 	private static final String[] NO_JTA_PLATFORM_CLASSES = {
   67 | 			"org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform",
   68 | 			"org.hibernate.service.jta.platform.internal.NoJtaPlatform" };
   69 | 
   70 | 	private final HibernateProperties hibernateProperties;
   71 | 
   72 | 	private final HibernateDefaultDdlAutoProvider defaultDdlAutoProvider;
   73 | 
   74 | 	private final DataSourcePoolMetadataProvider poolMetadataProvider;
   75 | 
   76 | 	private final ObjectProvider<SQLExceptionTranslator> sqlExceptionTranslator;
   77 | 
   78 | 	private final List<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers;
   79 | 
   80 | 	HibernateJpaConfiguration(DataSource dataSource, JpaProperties jpaProperties,
   81 | 			ObjectProvider<JtaTransactionManager> jtaTransactionManager, HibernateProperties hibernateProperties,
   82 | 			ObjectProvider<Collection<DataSourcePoolMetadataProvider>> metadataProviders,
   83 | 			ObjectProvider<SchemaManagementProvider> providers,
   84 | 			ObjectProvider<PhysicalNamingStrategy> physicalNamingStrategy,
   85 | 			ObjectProvider<ImplicitNamingStrategy> implicitNamingStrategy,
   86 | 			ObjectProvider<SQLExceptionTranslator> sqlExceptionTranslator,
   87 | 			ObjectProvider<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers) {
   88 | 		super(dataSource, jpaProperties, jtaTransactionManager);
   89 | 		this.hibernateProperties = hibernateProperties;
   90 | 		this.defaultDdlAutoProvider = new HibernateDefaultDdlAutoProvider(providers);
   91 | 		this.poolMetadataProvider = new CompositeDataSourcePoolMetadataProvider(metadataProviders.getIfAvailable());
   92 | 		this.sqlExceptionTranslator = sqlExceptionTranslator;
   93 | 		this.hibernatePropertiesCustomizers = determineHibernatePropertiesCustomizers(
   94 | 				physicalNamingStrategy.getIfAvailable(), implicitNamingStrategy.getIfAvailable(),
   95 | 				hibernatePropertiesCustomizers.orderedStream().toList());
   96 | 	}
   97 | 
   98 | 	private List<HibernatePropertiesCustomizer> determineHibernatePropertiesCustomizers(
   99 | 			@Nullable PhysicalNamingStrategy physicalNamingStrategy,
  100 | 			@Nullable ImplicitNamingStrategy implicitNamingStrategy,
  101 | 			List<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers) {
  102 | 		List<HibernatePropertiesCustomizer> customizers = new ArrayList<>();
  103 | 		if (physicalNamingStrategy != null || implicitNamingStrategy != null) {
  104 | 			customizers
  105 | 				.add(new NamingStrategiesHibernatePropertiesCustomizer(physicalNamingStrategy, implicitNamingStrategy));
  106 | 		}
  107 | 		customizers.addAll(hibernatePropertiesCustomizers);
  108 | 		return customizers;
  109 | 	}
  110 | 
  111 | 	@Override
  112 | 	protected HibernateJpaVendorAdapter createJpaVendorAdapter() {
  113 | 		HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
  114 | 		this.sqlExceptionTranslator.ifUnique(adapter.getJpaDialect()::setJdbcExceptionTranslator);
  115 | 		JtaTransactionManager jtaTransactionManager = getJtaTransactionManager();
  116 | 		if (jtaTransactionManager != null) {
  117 | 			adapter.setJtaTransactionManager(jtaTransactionManager);
  118 | 		}
  119 | 		return adapter;
  120 | 	}
  121 | 
  122 | 	@Override
  123 | 	protected Map<String, Object> getVendorProperties(DataSource dataSource) {
  124 | 		Supplier<String> defaultDdlMode = () -> this.defaultDdlAutoProvider.getDefaultDdlAuto(dataSource);
  125 | 		return new LinkedHashMap<>(this.hibernateProperties.determineHibernateProperties(
  126 | 				getProperties().getProperties(), new HibernateSettings().ddlAuto(defaultDdlMode)
  127 | 					.hibernatePropertiesCustomizers(this.hibernatePropertiesCustomizers)));
  128 | 	}
  129 | 
  130 | 	@Override
  131 | 	protected void customizeVendorProperties(Map<String, Object> vendorProperties) {
  132 | 		super.customizeVendorProperties(vendorProperties);
  133 | 		if (!vendorProperties.containsKey(PROVIDER_DISABLES_AUTOCOMMIT)) {
  134 | 			configureProviderDisablesAutocommit(vendorProperties);
  135 | 		}
  136 | 	}
  137 | 
  138 | 	private void configureProviderDisablesAutocommit(Map<String, Object> vendorProperties) {
  139 | 		if (isDataSourceAutoCommitDisabled() && !isJta()) {
  140 | 			vendorProperties.put(PROVIDER_DISABLES_AUTOCOMMIT, "true");
  141 | 		}
  142 | 	}
  143 | 
  144 | 	private boolean isDataSourceAutoCommitDisabled() {
  145 | 		DataSourcePoolMetadata poolMetadata = this.poolMetadataProvider.getDataSourcePoolMetadata(getDataSource());
  146 | 		return poolMetadata != null && Boolean.FALSE.equals(poolMetadata.getDefaultAutoCommit());
  147 | 	}
  148 | 
  149 | 	private static class NamingStrategiesHibernatePropertiesCustomizer implements HibernatePropertiesCustomizer {
  150 | 
  151 | 		private final @Nullable PhysicalNamingStrategy physicalNamingStrategy;
  152 | 
  153 | 		private final @Nullable ImplicitNamingStrategy implicitNamingStrategy;
  154 | 
  155 | 		NamingStrategiesHibernatePropertiesCustomizer(@Nullable PhysicalNamingStrategy physicalNamingStrategy,
  156 | 				@Nullable ImplicitNamingStrategy implicitNamingStrategy) {
  157 | 			this.physicalNamingStrategy = physicalNamingStrategy;
  158 | 			this.implicitNamingStrategy = implicitNamingStrategy;
  159 | 		}
  160 | 
  161 | 		@Override
  162 | 		public void customize(Map<String, Object> hibernateProperties) {
  163 | 			if (this.physicalNamingStrategy != null) {
  164 | 				hibernateProperties.put("hibernate.physical_naming_strategy", this.physicalNamingStrategy);
  165 | 			}
  166 | 			if (this.implicitNamingStrategy != null) {
  167 | 				hibernateProperties.put("hibernate.implicit_naming_strategy", this.implicitNamingStrategy);
  168 | 			}
  169 | 		}
  170 | 
  171 | 	}
  172 | 
  173 | 	static class HibernateRuntimeHints implements RuntimeHintsRegistrar {
  174 | 
  175 | 		private static final Consumer<Builder> INVOKE_DECLARED_CONSTRUCTORS = TypeHint
  176 | 			.builtWith(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
  177 | 
  178 | 		@Override
  179 | 		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
  180 | 			for (String noJtaPlatformClass : NO_JTA_PLATFORM_CLASSES) {
  181 | 				hints.reflection().registerType(TypeReference.of(noJtaPlatformClass), INVOKE_DECLARED_CONSTRUCTORS);
  182 | 			}
  183 | 			hints.reflection().registerType(SpringImplicitNamingStrategy.class, INVOKE_DECLARED_CONSTRUCTORS);
  184 | 			hints.reflection().registerType(PhysicalNamingStrategySnakeCaseImpl.class, INVOKE_DECLARED_CONSTRUCTORS);
  185 | 		}
  186 | 
  187 | 	}
  188 | 
  189 | }
```
