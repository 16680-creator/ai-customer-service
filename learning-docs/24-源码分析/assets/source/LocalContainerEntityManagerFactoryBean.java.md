<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-orm:7.1.0 | path: org/springframework/orm/jpa/LocalContainerEntityManagerFactoryBean.java -->
<!-- 正文引用：[[源码:LocalContainerEntityManagerFactoryBean.java#L起-L止]] ；代码块头部：(src: LocalContainerEntityManagerFactoryBean.java#L起-L止) -->

# LocalContainerEntityManagerFactoryBean.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-orm:7.1.0` |
| 路径 | `org/springframework/orm/jpa/LocalContainerEntityManagerFactoryBean.java` |
| 行数 | 507 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-orm/7.1.0/spring-orm-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:52.548Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *      https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | 
   17 | package org.springframework.orm.jpa;
   18 | 
   19 | import java.util.List;
   20 | 
   21 | import javax.sql.DataSource;
   22 | 
   23 | import jakarta.persistence.EntityManagerFactory;
   24 | import jakarta.persistence.FetchType;
   25 | import jakarta.persistence.PersistenceConfiguration;
   26 | import jakarta.persistence.PersistenceException;
   27 | import jakarta.persistence.SharedCacheMode;
   28 | import jakarta.persistence.ValidationMode;
   29 | import jakarta.persistence.spi.PersistenceProvider;
   30 | import jakarta.persistence.spi.PersistenceUnitInfo;
   31 | import org.jspecify.annotations.Nullable;
   32 | 
   33 | import org.springframework.beans.BeanUtils;
   34 | import org.springframework.beans.factory.BeanFactory;
   35 | import org.springframework.beans.factory.config.BeanDefinition;
   36 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   37 | import org.springframework.beans.factory.support.AbstractBeanDefinition;
   38 | import org.springframework.beans.factory.support.AutowireCandidateQualifier;
   39 | import org.springframework.context.ResourceLoaderAware;
   40 | import org.springframework.context.weaving.LoadTimeWeaverAware;
   41 | import org.springframework.core.io.ResourceLoader;
   42 | import org.springframework.instrument.classloading.LoadTimeWeaver;
   43 | import org.springframework.jdbc.datasource.lookup.SingleDataSourceLookup;
   44 | import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
   45 | import org.springframework.orm.jpa.persistenceunit.ManagedClassNameFilter;
   46 | import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
   47 | import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
   48 | import org.springframework.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
   49 | import org.springframework.orm.jpa.persistenceunit.SmartPersistenceUnitInfo;
   50 | import org.springframework.util.Assert;
   51 | import org.springframework.util.ClassUtils;
   52 | import org.springframework.util.CollectionUtils;
   53 | import org.springframework.util.StringUtils;
   54 | 
   55 | /**
   56 |  * {@link org.springframework.beans.factory.FactoryBean} that creates a JPA
   57 |  * {@link jakarta.persistence.EntityManagerFactory} according to JPA's standard
   58 |  * <i>container</i> bootstrap contract. This is the most powerful way to set
   59 |  * up a shared JPA EntityManagerFactory in a Spring application context;
   60 |  * the EntityManagerFactory can then be passed to JPA-based DAOs via
   61 |  * dependency injection. Note that switching to a JNDI lookup or to a
   62 |  * {@link LocalEntityManagerFactoryBean} definition is just a matter of
   63 |  * configuration!
   64 |  *
   65 |  * <p>As with {@link LocalEntityManagerFactoryBean}, configuration settings
   66 |  * are usually read in from a {@code META-INF/persistence.xml} config file,
   67 |  * residing in the class path, according to the general JPA configuration contract.
   68 |  * However, this FactoryBean is more flexible in that you can override the location
   69 |  * of the {@code persistence.xml} file, specify the JDBC DataSources to link to,
   70 |  * etc. Furthermore, it allows for pluggable class instrumentation through Spring's
   71 |  * {@link org.springframework.instrument.classloading.LoadTimeWeaver} abstraction,
   72 |  * instead of being tied to a special VM agent specified on JVM startup.
   73 |  *
   74 |  * <p>Internally, this FactoryBean parses the {@code persistence.xml} file
   75 |  * itself and creates a corresponding {@link jakarta.persistence.spi.PersistenceUnitInfo}
   76 |  * object (with further configuration merged in, such as JDBC DataSources and the
   77 |  * Spring LoadTimeWeaver), to be passed to the chosen JPA
   78 |  * {@link jakarta.persistence.spi.PersistenceProvider}. This corresponds to a
   79 |  * local JPA container with full support for the standard JPA container contract.
   80 |  *
   81 |  * <p>The exposed EntityManagerFactory object will implement all the interfaces of
   82 |  * the underlying native EntityManagerFactory returned by the PersistenceProvider,
   83 |  * plus the {@link EntityManagerFactoryInfo} interface which exposes additional
   84 |  * metadata as assembled by this FactoryBean.
   85 |  *
   86 |  * @author Juergen Hoeller
   87 |  * @author Rod Johnson
   88 |  * @since 2.0
   89 |  * @see #setJpaProperties
   90 |  * @see #setJpaVendorAdapter
   91 |  * @see #setLoadTimeWeaver
   92 |  * @see #setDataSource
   93 |  * @see LocalEntityManagerFactoryBean
   94 |  * @see jakarta.persistence.spi.PersistenceProvider#createContainerEntityManagerFactory
   95 |  */
   96 | @SuppressWarnings("serial")
   97 | public class LocalContainerEntityManagerFactoryBean extends AbstractEntityManagerFactoryBean
   98 | 		implements ResourceLoaderAware, LoadTimeWeaverAware {
   99 | 
  100 | 	private @Nullable PersistenceUnitManager persistenceUnitManager;
  101 | 
  102 | 	private final DefaultPersistenceUnitManager internalPersistenceUnitManager = new DefaultPersistenceUnitManager();
  103 | 
  104 | 	private @Nullable PersistenceUnitInfo persistenceUnitInfo;
  105 | 
  106 | 
  107 | 	/**
  108 | 	 * Set the PersistenceUnitManager to use for obtaining the JPA persistence unit
  109 | 	 * that this FactoryBean is supposed to build an EntityManagerFactory for.
  110 | 	 * <p>The default is to rely on the local settings specified on this FactoryBean,
  111 | 	 * such as "persistenceXmlLocation", "dataSource" and "loadTimeWeaver".
  112 | 	 * <p>For reuse of existing persistence unit configuration or more advanced forms
  113 | 	 * of custom persistence unit handling, consider defining a separate
  114 | 	 * PersistenceUnitManager bean (typically a DefaultPersistenceUnitManager instance)
  115 | 	 * and linking it in here. {@code persistence.xml} location, DataSource
  116 | 	 * configuration and LoadTimeWeaver will be defined on that separate
  117 | 	 * DefaultPersistenceUnitManager bean in such a scenario.
  118 | 	 * @see #setPersistenceXmlLocation
  119 | 	 * @see #setDataSource
  120 | 	 * @see #setLoadTimeWeaver
  121 | 	 * @see org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager
  122 | 	 */
  123 | 	public void setPersistenceUnitManager(PersistenceUnitManager persistenceUnitManager) {
  124 | 		this.persistenceUnitManager = persistenceUnitManager;
  125 | 	}
  126 | 
  127 | 	/**
  128 | 	 * Set the location of the {@code persistence.xml} file
  129 | 	 * we want to use. This is a Spring resource location.
  130 | 	 * <p>Default is "classpath:META-INF/persistence.xml".
  131 | 	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
  132 | 	 * @param persistenceXmlLocation a Spring resource String
  133 | 	 * identifying the location of the {@code persistence.xml} file
  134 | 	 * that this LocalContainerEntityManagerFactoryBean should parse
  135 | 	 * @see #setPersistenceUnitManager
  136 | 	 */
  137 | 	public void setPersistenceXmlLocation(String persistenceXmlLocation) {
  138 | 		this.internalPersistenceUnitManager.setPersistenceXmlLocation(persistenceXmlLocation);
  139 | 	}
  140 | 
  141 | 	/**
  142 | 	 * Specify the name of the persistence unit configuration to use.
  143 | 	 * <p>Uses the specified persistence unit name as the name of the
  144 | 	 * default persistence unit, if applicable. Otherwise, it selects
  145 | 	 * among the available persistence units.
  146 | 	 * @see DefaultPersistenceUnitManager#setDefaultPersistenceUnitName
  147 | 	 */
  148 | 	@Override
  149 | 	public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
  150 | 		super.setPersistenceUnitName(persistenceUnitName);
  151 | 		if (persistenceUnitName != null) {
  152 | 			this.internalPersistenceUnitManager.setDefaultPersistenceUnitName(persistenceUnitName);
  153 | 		}
  154 | 	}
  155 | 
  156 | 	/**
  157 | 	 * Set a persistence unit root location for the default persistence unit.
  158 | 	 * <p>Default is "classpath:", that is, the root of the current classpath
  159 | 	 * (nearest root directory). To be overridden if unit-specific resolution
  160 | 	 * does not work and the classpath root is not appropriate either.
  161 | 	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
  162 | 	 * @since 4.3.3
  163 | 	 * @see DefaultPersistenceUnitManager#setDefaultPersistenceUnitRootLocation
  164 | 	 */
  165 | 	public void setPersistenceUnitRootLocation(String defaultPersistenceUnitRootLocation) {
  166 | 		this.internalPersistenceUnitManager.setDefaultPersistenceUnitRootLocation(defaultPersistenceUnitRootLocation);
  167 | 	}
  168 | 
  169 | 	/**
  170 | 	 * Set a local JPA 3.2 {@link PersistenceConfiguration} to use for this
  171 | 	 * persistence unit.
  172 | 	 * <p>Note: {@link PersistenceConfiguration} includes a persistence unit name,
  173 | 	 * so this effectively overrides the {@link #setPersistenceUnitName} method.
  174 | 	 * In contrast, all other settings will be merged with the settings in the
  175 | 	 * {@code PersistenceConfiguration} instance.
  176 | 	 * @since 7.0
  177 | 	 * @see DefaultPersistenceUnitManager#setPersistenceConfiguration
  178 | 	 */
  179 | 	public void setPersistenceConfiguration(PersistenceConfiguration configuration) {
  180 | 		Assert.notNull(configuration, "PersistenceConfiguration must not be null");
  181 | 		super.setPersistenceUnitName(configuration.name());
  182 | 		this.internalPersistenceUnitManager.setPersistenceConfiguration(configuration);
  183 | 	}
  184 | 
  185 | 	/**
  186 | 	 * Set the {@link PersistenceManagedTypes} to use to build the list of managed types
  187 | 	 * as an alternative to entity scanning.
  188 | 	 * @param managedTypes the managed types
  189 | 	 * @since 6.0
  190 | 	 * @see DefaultPersistenceUnitManager#setManagedTypes(PersistenceManagedTypes)
  191 | 	 */
  192 | 	public void setManagedTypes(PersistenceManagedTypes managedTypes) {
  193 | 		this.internalPersistenceUnitManager.setManagedTypes(managedTypes);
  194 | 	}
  195 | 
  196 | 	/**
  197 | 	 * Set whether to use Spring-based scanning for entity classes in the classpath
  198 | 	 * instead of using JPA's standard scanning of jar files with {@code persistence.xml}
  199 | 	 * markers in them. In case of Spring-based scanning, no {@code persistence.xml}
  200 | 	 * is necessary; all you need to do is to specify base packages to search here.
  201 | 	 * <p>Default is none. Specify packages to search for autodetection of your entity
  202 | 	 * classes in the classpath. This is analogous to Spring's component-scan feature
  203 | 	 * ({@link org.springframework.context.annotation.ClassPathBeanDefinitionScanner}).
  204 | 	 * <p>Consider setting a {@link PersistenceManagedTypes} instead that allows the
  205 | 	 * scanning logic to be optimized by AOT processing.
  206 | 	 * <p><b>Note: There may be limitations in comparison to regular JPA scanning.</b>
  207 | 	 * In particular, JPA providers may pick up annotated packages for provider-specific
  208 | 	 * annotations only when driven by {@code persistence.xml}. As of 4.1, Spring's
  209 | 	 * scan can detect annotated packages as well if supported by the given
  210 | 	 * {@link JpaVendorAdapter} (for example, for Hibernate).
  211 | 	 * <p>If no explicit {@link #setMappingResources mapping resources} have been
  212 | 	 * specified in addition to these packages, Spring's setup looks for a default
  213 | 	 * {@code META-INF/orm.xml} file in the classpath, registering it as a mapping
  214 | 	 * resource for the default unit if the mapping file is not co-located with a
  215 | 	 * {@code persistence.xml} file (in which case we assume it is only meant to be
  216 | 	 * used with the persistence units defined there, like in standard JPA).
  217 | 	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
  218 | 	 * @param packagesToScan one or more base packages to search, analogous to
  219 | 	 * Spring's component-scan configuration for regular Spring components
  220 | 	 * @see #setPersistenceUnitManager
  221 | 	 * @see DefaultPersistenceUnitManager#setPackagesToScan
  222 | 	 */
  223 | 	public void setPackagesToScan(String... packagesToScan) {
  224 | 		this.internalPersistenceUnitManager.setPackagesToScan(packagesToScan);
  225 | 	}
  226 | 
  227 | 	/**
  228 | 	 * Set the {@link ManagedClassNameFilter} to apply on entity classes discovered
  229 | 	 * using {@linkplain #setPackagesToScan(String...) classpath scanning}.
  230 | 	 * @param managedClassNameFilter a predicate to filter entity classes
  231 | 	 * @since 6.1.4
  232 | 	 * @see DefaultPersistenceUnitManager#setManagedClassNameFilter
  233 | 	 */
  234 | 	public void setManagedClassNameFilter(ManagedClassNameFilter managedClassNameFilter) {
  235 | 		this.internalPersistenceUnitManager.setManagedClassNameFilter(managedClassNameFilter);
  236 | 	}
  237 | 
  238 | 	/**
  239 | 	 * Specify one or more mapping resources (equivalent to {@code <mapping-file>}
  240 | 	 * entries in {@code persistence.xml}) for the default persistence unit.
  241 | 	 * Can be used on its own or in combination with entity scanning in the classpath,
  242 | 	 * in both cases avoiding {@code persistence.xml}.
  243 | 	 * <p>Note that mapping resources must be relative to the classpath root,
  244 | 	 * for example, "META-INF/mappings.xml" or "com/mycompany/repository/mappings.xml",
  245 | 	 * so that they can be loaded through {@code ClassLoader.getResource}.
  246 | 	 * <p>If no explicit mapping resources have been specified next to
  247 | 	 * {@link #setPackagesToScan packages to scan}, Spring's setup looks for a default
  248 | 	 * {@code META-INF/orm.xml} file in the classpath, registering it as a mapping
  249 | 	 * resource for the default unit if the mapping file is not co-located with a
  250 | 	 * {@code persistence.xml} file (in which case we assume it is only meant to be
  251 | 	 * used with the persistence units defined there, like in standard JPA).
  252 | 	 * <p>Note that specifying an empty array/list here suppresses the default
  253 | 	 * {@code META-INF/orm.xml} check. On the other hand, explicitly specifying
  254 | 	 * {@code META-INF/orm.xml} here will register that file even if it happens
  255 | 	 * to be co-located with a {@code persistence.xml} file.
  256 | 	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
  257 | 	 * @see #setPersistenceUnitManager
  258 | 	 * @see DefaultPersistenceUnitManager#setMappingResources
  259 | 	 */
  260 | 	public void setMappingResources(String... mappingResources) {
  261 | 		this.internalPersistenceUnitManager.setMappingResources(mappingResources);
  262 | 	}
  263 | 
  264 | 	/**
  265 | 	 * Specify the JPA 2.0 shared cache mode for this persistence unit,
  266 | 	 * overriding a value in {@code persistence.xml} if set.
  267 | 	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
  268 | 	 * @since 4.0
  269 | 	 * @see jakarta.persistence.spi.PersistenceUnitInfo#getSharedCacheMode()
  270 | 	 * @see #setPersistenceUnitManager
  271 | 	 */
  272 | 	public void setSharedCacheMode(SharedCacheMode sharedCacheMode) {
  273 | 		this.internalPersistenceUnitManager.setSharedCacheMode(sharedCacheMode);
  274 | 	}
  275 | 
  276 | 	/**
  277 | 	 * Specify the JPA 2.0 validation mode for this persistence unit,
  278 | 	 * overriding a value in {@code persistence.xml} if set.
  279 | 	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
  280 | 	 * @since 4.0
  281 | 	 * @see jakarta.persistence.spi.PersistenceUnitInfo#getValidationMode()
  282 | 	 * @see #setPersistenceUnitManager
  283 | 	 */
  284 | 	public void setValidationMode(ValidationMode validationMode) {
  285 | 		this.internalPersistenceUnitManager.setValidationMode(validationMode);
  286 | 	}
  287 | 
  288 | 	/**
  289 | 	 * Specify the JPA 4.0 default fetch type for all of this manager's persistence
  290 | 	 * units, overriding any value in {@code persistence.xml} if set.
  291 | 	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified. Also,
  292 | 	 * this requires a JPA 4.0+ persistence provider and will be ignored otherwise.</b>
  293 | 	 * @since 7.0.4
  294 | 	 */
  295 | 	public void setDefaultToOneFetchType(FetchType defaultToOneFetchType) {
  296 | 		this.internalPersistenceUnitManager.setDefaultToOneFetchType(defaultToOneFetchType);
  297 | 	}
  298 | 
  299 | 	/**
  300 | 	 * Specify the JDBC DataSource that the JPA persistence provider is supposed
  301 | 	 * to use for accessing the database. This is an alternative to keeping the
  302 | 	 * JDBC configuration in {@code persistence.xml}, passing in a Spring-managed
  303 | 	 * DataSource instead.
  304 | 	 * <p>In JPA speak, a DataSource passed in here will be used as "nonJtaDataSource"
  305 | 	 * on the PersistenceUnitInfo passed to the PersistenceProvider, as well as
  306 | 	 * overriding data source configuration in {@code persistence.xml} (if any).
  307 | 	 * Note that this variant typically works for JTA transaction management as well;
  308 | 	 * if it does not, consider using the explicit {@link #setJtaDataSource} instead.
  309 | 	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
  310 | 	 * @see jakarta.persistence.spi.PersistenceUnitInfo#getNonJtaDataSource()
  311 | 	 * @see #setPersistenceUnitManager
  312 | 	 */
  313 | 	public void setDataSource(@Nullable DataSource dataSource) {
  314 | 		this.internalPersistenceUnitManager.setDataSourceLookup(
  315 | 				dataSource != null ? new SingleDataSourceLookup(dataSource) : null);
  316 | 		this.internalPersistenceUnitManager.setDefaultDataSource(dataSource);
  317 | 	}
  318 | 
  319 | 	/**
  320 | 	 * Specify the JDBC DataSource that the JPA persistence provider is supposed
  321 | 	 * to use for accessing the database. This is an alternative to keeping the
  322 | 	 * JDBC configuration in {@code persistence.xml}, passing in a Spring-managed
  323 | 	 * DataSource instead.
  324 | 	 * <p>In JPA speak, a DataSource passed in here will be used as "jtaDataSource"
  325 | 	 * on the PersistenceUnitInfo passed to the PersistenceProvider, as well as
  326 | 	 * overriding data source configuration in {@code persistence.xml} (if any).
  327 | 	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
  328 | 	 * @see jakarta.persistence.spi.PersistenceUnitInfo#getJtaDataSource()
  329 | 	 * @see #setPersistenceUnitManager
  330 | 	 */
  331 | 	public void setJtaDataSource(@Nullable DataSource jtaDataSource) {
  332 | 		this.internalPersistenceUnitManager.setDataSourceLookup(
  333 | 				jtaDataSource != null ? new SingleDataSourceLookup(jtaDataSource) : null);
  334 | 		this.internalPersistenceUnitManager.setDefaultJtaDataSource(jtaDataSource);
  335 | 	}
  336 | 
  337 | 	/**
  338 | 	 * Set the PersistenceUnitPostProcessors to be applied to the
  339 | 	 * PersistenceUnitInfo used for creating this EntityManagerFactory.
  340 | 	 * <p>Such post-processors can, for example, register further entity
  341 | 	 * classes and jar files, in addition to the metadata read from
  342 | 	 * {@code persistence.xml}.
  343 | 	 * <p><b>NOTE: Only applied if no external PersistenceUnitManager specified.</b>
  344 | 	 * @see #setPersistenceUnitManager
  345 | 	 */
  346 | 	public void setPersistenceUnitPostProcessors(PersistenceUnitPostProcessor... postProcessors) {
  347 | 		this.internalPersistenceUnitManager.setPersistenceUnitPostProcessors(postProcessors);
  348 | 	}
  349 | 
  350 | 	/**
  351 | 	 * Specify the Spring LoadTimeWeaver to use for class instrumentation according
  352 | 	 * to the JPA class transformer contract.
  353 | 	 * <p>It is a not required to specify a LoadTimeWeaver: Most providers will be
  354 | 	 * able to provide a subset of their functionality without class instrumentation
  355 | 	 * as well, or operate with their VM agent specified on JVM startup.
  356 | 	 * <p>In terms of Spring-provided weaving options, the most important ones are
  357 | 	 * InstrumentationLoadTimeWeaver, which requires a Spring-specific (but very general)
  358 | 	 * VM agent specified on JVM startup, and ReflectiveLoadTimeWeaver, which interacts
  359 | 	 * with an underlying ClassLoader based on specific extended methods being available
  360 | 	 * on it.
  361 | 	 * <p><b>NOTE:</b> As of Spring 2.5, the context's default LoadTimeWeaver (defined
  362 | 	 * as bean with name "loadTimeWeaver") will be picked up automatically, if available,
  363 | 	 * removing the need for LoadTimeWeaver configuration on each affected target bean.
  364 | 	 * Consider using the {@code context:load-time-weaver} XML tag for creating
  365 | 	 * such a shared LoadTimeWeaver (autodetecting the environment by default).
  366 | 	 * <p><b>NOTE:</b> Only applied if no external PersistenceUnitManager specified.
  367 | 	 * Otherwise, the external {@link #setPersistenceUnitManager PersistenceUnitManager}
  368 | 	 * is responsible for the weaving configuration.
  369 | 	 * @see org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver
  370 | 	 * @see org.springframework.instrument.classloading.ReflectiveLoadTimeWeaver
  371 | 	 */
  372 | 	@Override
  373 | 	public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
  374 | 		this.internalPersistenceUnitManager.setLoadTimeWeaver(loadTimeWeaver);
  375 | 	}
  376 | 
  377 | 	@Override
  378 | 	public void setResourceLoader(ResourceLoader resourceLoader) {
  379 | 		this.internalPersistenceUnitManager.setResourceLoader(resourceLoader);
  380 | 	}
  381 | 
  382 | 
  383 | 	@Override
  384 | 	public void afterPropertiesSet() throws PersistenceException {
  385 | 		PersistenceUnitManager managerToUse = this.persistenceUnitManager;
  386 | 		if (managerToUse == null) {
  387 | 			this.internalPersistenceUnitManager.afterPropertiesSet();
  388 | 			managerToUse = this.internalPersistenceUnitManager;
  389 | 		}
  390 | 
  391 | 		this.persistenceUnitInfo = determinePersistenceUnitInfo(managerToUse);
  392 | 		JpaVendorAdapter jpaVendorAdapter = getJpaVendorAdapter();
  393 | 		if (jpaVendorAdapter != null && this.persistenceUnitInfo instanceof SmartPersistenceUnitInfo smartInfo) {
  394 | 			String rootPackage = jpaVendorAdapter.getPersistenceProviderRootPackage();
  395 | 			if (rootPackage != null) {
  396 | 				smartInfo.setPersistenceProviderPackageName(rootPackage);
  397 | 			}
  398 | 		}
  399 | 
  400 | 		String scope = this.persistenceUnitInfo.getScopeAnnotationName();
  401 | 		if (StringUtils.hasText(scope)) {
  402 | 			logger.info("Scope annotation name for persistence unit ignored by Spring: " + scope);
  403 | 		}
  404 | 
  405 | 		List<String> qualifiers = this.persistenceUnitInfo.getQualifierAnnotationNames();
  406 | 		if (!CollectionUtils.isEmpty(qualifiers)) {
  407 | 			BeanFactory beanFactory = getBeanFactory();
  408 | 			String beanName = getBeanName();
  409 | 			if (beanFactory instanceof ConfigurableBeanFactory cbf && beanName != null) {
  410 | 				BeanDefinition bd = cbf.getMergedBeanDefinition(beanName);
  411 | 				if (bd instanceof AbstractBeanDefinition abd) {
  412 | 					for (String qualifier : qualifiers) {
  413 | 						abd.addQualifier(new AutowireCandidateQualifier(qualifier));
  414 | 					}
  415 | 				}
  416 | 			}
  417 | 		}
  418 | 
  419 | 		super.afterPropertiesSet();
  420 | 	}
  421 | 
  422 | 	@Override
  423 | 	protected EntityManagerFactory createNativeEntityManagerFactory() throws PersistenceException {
  424 | 		Assert.state(this.persistenceUnitInfo != null, "PersistenceUnitInfo not initialized");
  425 | 
  426 | 		PersistenceProvider provider = getPersistenceProvider();
  427 | 		if (provider == null) {
  428 | 			String providerClassName = this.persistenceUnitInfo.getPersistenceProviderClassName();
  429 | 			if (providerClassName == null) {
  430 | 				throw new IllegalArgumentException(
  431 | 						"No PersistenceProvider specified in EntityManagerFactory configuration, " +
  432 | 						"and chosen PersistenceUnitInfo does not specify a provider class name either");
  433 | 			}
  434 | 			Class<?> providerClass = ClassUtils.resolveClassName(providerClassName, getBeanClassLoader());
  435 | 			provider = (PersistenceProvider) BeanUtils.instantiateClass(providerClass);
  436 | 		}
  437 | 
  438 | 		if (logger.isDebugEnabled()) {
  439 | 			logger.debug("Building JPA container EntityManagerFactory for persistence unit '" +
  440 | 					this.persistenceUnitInfo.getPersistenceUnitName() + "'");
  441 | 		}
  442 | 		EntityManagerFactory emf =
  443 | 				provider.createContainerEntityManagerFactory(this.persistenceUnitInfo, getJpaPropertyMap());
  444 | 		postProcessEntityManagerFactory(emf, this.persistenceUnitInfo);
  445 | 
  446 | 		return emf;
  447 | 	}
  448 | 
  449 | 
  450 | 	/**
  451 | 	 * Determine the PersistenceUnitInfo to use for the EntityManagerFactory
  452 | 	 * created by this bean.
  453 | 	 * <p>The default implementation reads in all persistence unit infos from
  454 | 	 * {@code persistence.xml}, as defined in the JPA specification, selecting a unit
  455 | 	 * by name. If no persistence unit name was specified, it takes the default one
  456 | 	 * if configured, or otherwise the first persistence unit as found by the reader.
  457 | 	 * @param persistenceUnitManager the PersistenceUnitManager to obtain from
  458 | 	 * @return the chosen PersistenceUnitInfo
  459 | 	 */
  460 | 	protected PersistenceUnitInfo determinePersistenceUnitInfo(PersistenceUnitManager persistenceUnitManager) {
  461 | 		String persistenceUnitName = getPersistenceUnitName();
  462 | 		if (persistenceUnitName != null) {
  463 | 			return persistenceUnitManager.obtainPersistenceUnitInfo(persistenceUnitName);
  464 | 		}
  465 | 		else {
  466 | 			return persistenceUnitManager.obtainDefaultPersistenceUnitInfo();
  467 | 		}
  468 | 	}
  469 | 
  470 | 	/**
  471 | 	 * Hook method allowing subclasses to customize the EntityManagerFactory
  472 | 	 * after its creation via the PersistenceProvider.
  473 | 	 * <p>The default implementation is empty.
  474 | 	 * @param emf the newly created EntityManagerFactory we are working with
  475 | 	 * @param pui the PersistenceUnitInfo used to configure the EntityManagerFactory
  476 | 	 * @see jakarta.persistence.spi.PersistenceProvider#createContainerEntityManagerFactory
  477 | 	 */
  478 | 	protected void postProcessEntityManagerFactory(EntityManagerFactory emf, PersistenceUnitInfo pui) {
  479 | 	}
  480 | 
  481 | 
  482 | 	@Override
  483 | 	public @Nullable PersistenceUnitInfo getPersistenceUnitInfo() {
  484 | 		return this.persistenceUnitInfo;
  485 | 	}
  486 | 
  487 | 	@Override
  488 | 	public @Nullable String getPersistenceUnitName() {
  489 | 		if (this.persistenceUnitInfo != null) {
  490 | 			return this.persistenceUnitInfo.getPersistenceUnitName();
  491 | 		}
  492 | 		return super.getPersistenceUnitName();
  493 | 	}
  494 | 
  495 | 	@Override
  496 | 	public @Nullable DataSource getDataSource() {
  497 | 		if (this.persistenceUnitInfo != null) {
  498 | 			return (this.persistenceUnitInfo.getJtaDataSource() != null ?
  499 | 					this.persistenceUnitInfo.getJtaDataSource() :
  500 | 					this.persistenceUnitInfo.getNonJtaDataSource());
  501 | 		}
  502 | 		return (this.internalPersistenceUnitManager.getDefaultJtaDataSource() != null ?
  503 | 				this.internalPersistenceUnitManager.getDefaultJtaDataSource() :
  504 | 				this.internalPersistenceUnitManager.getDefaultDataSource());
  505 | 	}
  506 | 
  507 | }
```
