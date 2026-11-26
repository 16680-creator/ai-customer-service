<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-jpa:4.2.0 | path: org/springframework/boot/jpa/autoconfigure/JpaBaseConfiguration.java -->
<!-- 正文引用：[[源码:JpaBaseConfiguration.java#L起-L止]] ；代码块头部：(src: JpaBaseConfiguration.java#L起-L止) -->

# JpaBaseConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-jpa:4.2.0` |
| 路径 | `org/springframework/boot/jpa/autoconfigure/JpaBaseConfiguration.java` |
| 行数 | 272 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-jpa/4.2.0/spring-boot-jpa-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:28:01.816Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.jpa.autoconfigure;
    6 | 
    7 | import java.util.HashMap;
    8 | import java.util.List;
    9 | import java.util.Map;
   10 | 
   11 | import javax.sql.DataSource;
   12 | 
   13 | import jakarta.persistence.EntityManagerFactory;
   14 | import org.apache.commons.logging.Log;
   15 | import org.apache.commons.logging.LogFactory;
   16 | import org.jspecify.annotations.Nullable;
   17 | 
   18 | import org.springframework.beans.factory.BeanFactory;
   19 | import org.springframework.beans.factory.ListableBeanFactory;
   20 | import org.springframework.beans.factory.ObjectProvider;
   21 | import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
   22 | import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
   23 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
   24 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   25 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   26 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingFilterBean;
   27 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
   28 | import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
   29 | import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
   30 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   31 | import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
   32 | import org.springframework.boot.jpa.autoconfigure.JpaProperties.Bootstrap;
   33 | import org.springframework.boot.persistence.autoconfigure.EntityScanPackages;
   34 | import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizers;
   35 | import org.springframework.context.annotation.Bean;
   36 | import org.springframework.context.annotation.Configuration;
   37 | import org.springframework.context.annotation.Primary;
   38 | import org.springframework.core.io.ResourceLoader;
   39 | import org.springframework.core.task.AsyncTaskExecutor;
   40 | import org.springframework.orm.jpa.JpaTransactionManager;
   41 | import org.springframework.orm.jpa.JpaVendorAdapter;
   42 | import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
   43 | import org.springframework.orm.jpa.persistenceunit.ManagedClassNameFilter;
   44 | import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
   45 | import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypesScanner;
   46 | import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
   47 | import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
   48 | import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
   49 | import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
   50 | import org.springframework.transaction.PlatformTransactionManager;
   51 | import org.springframework.transaction.TransactionManager;
   52 | import org.springframework.transaction.jta.JtaTransactionManager;
   53 | import org.springframework.util.ObjectUtils;
   54 | import org.springframework.util.StringUtils;
   55 | import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
   56 | import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
   57 | 
   58 | /**
   59 |  * Base {@link EnableAutoConfiguration Auto-configuration} for JPA.
   60 |  *
   61 |  * @author Phillip Webb
   62 |  * @author Dave Syer
   63 |  * @author Oliver Gierke
   64 |  * @author Andy Wilkinson
   65 |  * @author Kazuki Shimizu
   66 |  * @author Eddú Meléndez
   67 |  * @author Yanming Zhou
   68 |  * @since 4.0.0
   69 |  */
   70 | @Configuration(proxyBeanMethods = false)
   71 | @EnableConfigurationProperties(JpaProperties.class)
   72 | public abstract class JpaBaseConfiguration {
   73 | 
   74 | 	private final DataSource dataSource;
   75 | 
   76 | 	private final JpaProperties properties;
   77 | 
   78 | 	private final @Nullable JtaTransactionManager jtaTransactionManager;
   79 | 
   80 | 	protected JpaBaseConfiguration(DataSource dataSource, JpaProperties properties,
   81 | 			ObjectProvider<JtaTransactionManager> jtaTransactionManager) {
   82 | 		this.dataSource = dataSource;
   83 | 		this.properties = properties;
   84 | 		this.jtaTransactionManager = jtaTransactionManager.getIfAvailable();
   85 | 	}
   86 | 
   87 | 	@Bean
   88 | 	@ConditionalOnMissingBean(TransactionManager.class)
   89 | 	public PlatformTransactionManager transactionManager(
   90 | 			ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
   91 | 		JpaTransactionManager transactionManager = new JpaTransactionManager();
   92 | 		transactionManagerCustomizers.ifAvailable((customizers) -> customizers.customize(transactionManager));
   93 | 		return transactionManager;
   94 | 	}
   95 | 
   96 | 	@Bean
   97 | 	@ConditionalOnMissingBean
   98 | 	public JpaVendorAdapter jpaVendorAdapter() {
   99 | 		AbstractJpaVendorAdapter adapter = createJpaVendorAdapter();
  100 | 		adapter.setShowSql(this.properties.isShowSql());
  101 | 		if (this.properties.getDatabase() != null) {
  102 | 			adapter.setDatabase(this.properties.getDatabase());
  103 | 		}
  104 | 		if (this.properties.getDatabasePlatform() != null) {
  105 | 			adapter.setDatabasePlatform(this.properties.getDatabasePlatform());
  106 | 		}
  107 | 		adapter.setGenerateDdl(this.properties.isGenerateDdl());
  108 | 		return adapter;
  109 | 	}
  110 | 
  111 | 	@Bean
  112 | 	@ConditionalOnMissingBean
  113 | 	public EntityManagerFactoryBuilder entityManagerFactoryBuilder(JpaVendorAdapter jpaVendorAdapter,
  114 | 			ObjectProvider<PersistenceUnitManager> persistenceUnitManager,
  115 | 			ObjectProvider<EntityManagerFactoryBuilderCustomizer> customizers, ListableBeanFactory beanFactory) {
  116 | 		EntityManagerFactoryBuilder builder = new EntityManagerFactoryBuilder(jpaVendorAdapter,
  117 | 				this::buildJpaProperties, persistenceUnitManager.getIfAvailable(), null,
  118 | 				() -> determineBootstrapExecutor(beanFactory));
  119 | 		if (this.properties.getBootstrap() == Bootstrap.ASYNC) {
  120 | 			builder.requireBootstrapExecutor(
  121 | 					() -> BootstrapExecutorRequiredException.ofProperty("spring.jpa.bootstrap", "async"));
  122 | 		}
  123 | 		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
  124 | 		return builder;
  125 | 	}
  126 | 
  127 | 	private @Nullable AsyncTaskExecutor determineBootstrapExecutor(ListableBeanFactory beanFactory) {
  128 | 		Map<String, AsyncTaskExecutor> taskExecutors = beanFactory.getBeansOfType(AsyncTaskExecutor.class);
  129 | 		return (taskExecutors.size() == 1) ? taskExecutors.values().iterator().next()
  130 | 				: taskExecutors.get(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME);
  131 | 	}
  132 | 
  133 | 	private Map<String, ?> buildJpaProperties(DataSource dataSource) {
  134 | 		Map<String, Object> properties = new HashMap<>(this.properties.getProperties());
  135 | 		Map<String, Object> vendorProperties = getVendorProperties(dataSource);
  136 | 		customizeVendorProperties(vendorProperties);
  137 | 		properties.putAll(vendorProperties);
  138 | 		return properties;
  139 | 	}
  140 | 
  141 | 	@Bean
  142 | 	@Primary
  143 | 	@ConditionalOnMissingBean({ LocalContainerEntityManagerFactoryBean.class, EntityManagerFactory.class })
  144 | 	public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder factoryBuilder,
  145 | 			PersistenceManagedTypes persistenceManagedTypes) {
  146 | 		return factoryBuilder.dataSource(this.dataSource)
  147 | 			.managedTypes(persistenceManagedTypes)
  148 | 			.mappingResources(getMappingResources())
  149 | 			.jta(isJta())
  150 | 			.build();
  151 | 	}
  152 | 
  153 | 	protected abstract AbstractJpaVendorAdapter createJpaVendorAdapter();
  154 | 
  155 | 	/**
  156 | 	 * Return the vendor-specific properties for the given {@link DataSource}.
  157 | 	 * @param dataSource the data source
  158 | 	 * @return the vendor properties
  159 | 	 */
  160 | 	protected abstract Map<String, Object> getVendorProperties(DataSource dataSource);
  161 | 
  162 | 	/**
  163 | 	 * Customize vendor properties before they are used. Allows for post-processing (for
  164 | 	 * example to configure JTA specific settings).
  165 | 	 * @param vendorProperties the vendor properties to customize
  166 | 	 */
  167 | 	protected void customizeVendorProperties(Map<String, Object> vendorProperties) {
  168 | 	}
  169 | 
  170 | 	private String @Nullable [] getMappingResources() {
  171 | 		List<String> mappingResources = this.properties.getMappingResources();
  172 | 		return (!ObjectUtils.isEmpty(mappingResources) ? StringUtils.toStringArray(mappingResources) : null);
  173 | 	}
  174 | 
  175 | 	/**
  176 | 	 * Return the JTA transaction manager.
  177 | 	 * @return the transaction manager or {@code null}
  178 | 	 */
  179 | 	protected @Nullable JtaTransactionManager getJtaTransactionManager() {
  180 | 		return this.jtaTransactionManager;
  181 | 	}
  182 | 
  183 | 	/**
  184 | 	 * Returns if a JTA {@link PlatformTransactionManager} is being used.
  185 | 	 * @return if a JTA transaction manager is being used
  186 | 	 */
  187 | 	protected final boolean isJta() {
  188 | 		return (this.jtaTransactionManager != null);
  189 | 	}
  190 | 
  191 | 	/**
  192 | 	 * Return the {@link JpaProperties}.
  193 | 	 * @return the properties
  194 | 	 */
  195 | 	protected final JpaProperties getProperties() {
  196 | 		return this.properties;
  197 | 	}
  198 | 
  199 | 	/**
  200 | 	 * Return the {@link DataSource}.
  201 | 	 * @return the data source
  202 | 	 */
  203 | 	protected final DataSource getDataSource() {
  204 | 		return this.dataSource;
  205 | 	}
  206 | 
  207 | 	@Configuration(proxyBeanMethods = false)
  208 | 	@ConditionalOnMissingBean({ LocalContainerEntityManagerFactoryBean.class, EntityManagerFactory.class })
  209 | 	static class PersistenceManagedTypesConfiguration {
  210 | 
  211 | 		@Bean
  212 | 		@Primary
  213 | 		@ConditionalOnMissingBean
  214 | 		static PersistenceManagedTypes persistenceManagedTypes(BeanFactory beanFactory, ResourceLoader resourceLoader,
  215 | 				ObjectProvider<ManagedClassNameFilter> managedClassNameFilter) {
  216 | 			String[] packagesToScan = getPackagesToScan(beanFactory);
  217 | 			return new PersistenceManagedTypesScanner(resourceLoader, managedClassNameFilter.getIfAvailable())
  218 | 				.scan(packagesToScan);
  219 | 		}
  220 | 
  221 | 		private static String[] getPackagesToScan(BeanFactory beanFactory) {
  222 | 			List<String> packages = EntityScanPackages.get(beanFactory).getPackageNames();
  223 | 			if (packages.isEmpty() && AutoConfigurationPackages.has(beanFactory)) {
  224 | 				packages = AutoConfigurationPackages.get(beanFactory);
  225 | 			}
  226 | 			return StringUtils.toStringArray(packages);
  227 | 		}
  228 | 
  229 | 	}
  230 | 
  231 | 	@Configuration(proxyBeanMethods = false)
  232 | 	@ConditionalOnWebApplication(type = Type.SERVLET)
  233 | 	@ConditionalOnClass(WebMvcConfigurer.class)
  234 | 	@ConditionalOnMissingBean({ OpenEntityManagerInViewInterceptor.class, OpenEntityManagerInViewFilter.class })
  235 | 	@ConditionalOnMissingFilterBean(OpenEntityManagerInViewFilter.class)
  236 | 	@ConditionalOnBooleanProperty(name = "spring.jpa.open-in-view", matchIfMissing = true)
  237 | 	protected static class JpaWebConfiguration {
  238 | 
  239 | 		private static final Log logger = LogFactory.getLog(JpaWebConfiguration.class);
  240 | 
  241 | 		private final JpaProperties jpaProperties;
  242 | 
  243 | 		protected JpaWebConfiguration(JpaProperties jpaProperties) {
  244 | 			this.jpaProperties = jpaProperties;
  245 | 		}
  246 | 
  247 | 		@Bean
  248 | 		public OpenEntityManagerInViewInterceptor openEntityManagerInViewInterceptor() {
  249 | 			if (this.jpaProperties.getOpenInView() == null) {
  250 | 				logger.warn("spring.jpa.open-in-view is enabled by default. "
  251 | 						+ "Therefore, database queries may be performed during view "
  252 | 						+ "rendering. Explicitly configure spring.jpa.open-in-view to disable this warning");
  253 | 			}
  254 | 			return new OpenEntityManagerInViewInterceptor();
  255 | 		}
  256 | 
  257 | 		@Bean
  258 | 		public WebMvcConfigurer openEntityManagerInViewInterceptorConfigurer(
  259 | 				OpenEntityManagerInViewInterceptor interceptor) {
  260 | 			return new WebMvcConfigurer() {
  261 | 
  262 | 				@Override
  263 | 				public void addInterceptors(InterceptorRegistry registry) {
  264 | 					registry.addWebRequestInterceptor(interceptor);
  265 | 				}
  266 | 
  267 | 			};
  268 | 		}
  269 | 
  270 | 	}
  271 | 
  272 | }
```
