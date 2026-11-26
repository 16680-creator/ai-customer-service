<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis.spring.boot:mybatis-spring-boot-autoconfigure:4.1.0 | path: org/mybatis/spring/boot/autoconfigure/MybatisAutoConfiguration.java -->
<!-- 正文引用：[[源码:MybatisAutoConfiguration.java#L起-L止]] ；代码块头部：(src: MybatisAutoConfiguration.java#L起-L止) -->

# MybatisAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis.spring.boot:mybatis-spring-boot-autoconfigure:4.1.0` |
| 路径 | `org/mybatis/spring/boot/autoconfigure/MybatisAutoConfiguration.java` |
| 行数 | 325 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/spring/boot/mybatis-spring-boot-autoconfigure/4.1.0/mybatis-spring-boot-autoconfigure-4.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:01:44.152Z |

```java
    1 | /*
    2 |  *    Copyright 2015-2025 the original author or authors.
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
   18 | import java.beans.PropertyDescriptor;
   19 | import java.util.List;
   20 | import java.util.Optional;
   21 | import java.util.Set;
   22 | import java.util.stream.Collectors;
   23 | import java.util.stream.Stream;
   24 | 
   25 | import javax.sql.DataSource;
   26 | 
   27 | import org.apache.ibatis.annotations.Mapper;
   28 | import org.apache.ibatis.mapping.DatabaseIdProvider;
   29 | import org.apache.ibatis.plugin.Interceptor;
   30 | import org.apache.ibatis.scripting.LanguageDriver;
   31 | import org.apache.ibatis.session.Configuration;
   32 | import org.apache.ibatis.session.ExecutorType;
   33 | import org.apache.ibatis.session.SqlSessionFactory;
   34 | import org.apache.ibatis.type.TypeHandler;
   35 | import org.mybatis.spring.SqlSessionFactoryBean;
   36 | import org.mybatis.spring.SqlSessionTemplate;
   37 | import org.mybatis.spring.mapper.MapperFactoryBean;
   38 | import org.mybatis.spring.mapper.MapperScannerConfigurer;
   39 | import org.slf4j.Logger;
   40 | import org.slf4j.LoggerFactory;
   41 | import org.springframework.beans.BeanWrapper;
   42 | import org.springframework.beans.BeanWrapperImpl;
   43 | import org.springframework.beans.factory.BeanFactory;
   44 | import org.springframework.beans.factory.BeanFactoryAware;
   45 | import org.springframework.beans.factory.InitializingBean;
   46 | import org.springframework.beans.factory.ListableBeanFactory;
   47 | import org.springframework.beans.factory.ObjectProvider;
   48 | import org.springframework.beans.factory.config.BeanDefinition;
   49 | import org.springframework.beans.factory.support.BeanDefinitionBuilder;
   50 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   51 | import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
   52 | import org.springframework.boot.autoconfigure.AutoConfigureAfter;
   53 | import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
   54 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   55 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   56 | import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
   57 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   58 | import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
   59 | import org.springframework.context.EnvironmentAware;
   60 | import org.springframework.context.annotation.Bean;
   61 | import org.springframework.context.annotation.Import;
   62 | import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
   63 | import org.springframework.core.env.Environment;
   64 | import org.springframework.core.io.Resource;
   65 | import org.springframework.core.io.ResourceLoader;
   66 | import org.springframework.core.type.AnnotationMetadata;
   67 | import org.springframework.util.Assert;
   68 | import org.springframework.util.CollectionUtils;
   69 | import org.springframework.util.ObjectUtils;
   70 | import org.springframework.util.StringUtils;
   71 | 
   72 | /**
   73 |  * {@link EnableAutoConfiguration Auto-Configuration} for Mybatis. Contributes a {@link SqlSessionFactory} and a
   74 |  * {@link SqlSessionTemplate}. If {@link org.mybatis.spring.annotation.MapperScan} is used, or a configuration file is
   75 |  * specified as a property, those will be considered, otherwise this auto-configuration will attempt to register mappers
   76 |  * based on the interface definitions in or under the root auto-configuration package.
   77 |  *
   78 |  * @author Eddú Meléndez
   79 |  * @author Josh Long
   80 |  * @author Kazuki Shimizu
   81 |  * @author Eduardo Macarrón
   82 |  */
   83 | @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
   84 | @ConditionalOnClass({ SqlSessionFactory.class, SqlSessionFactoryBean.class })
   85 | @ConditionalOnSingleCandidate(DataSource.class)
   86 | @EnableConfigurationProperties(MybatisProperties.class)
   87 | @AutoConfigureAfter({ DataSourceAutoConfiguration.class, MybatisLanguageDriverAutoConfiguration.class })
   88 | public class MybatisAutoConfiguration implements InitializingBean {
   89 | 
   90 |   private static final Logger logger = LoggerFactory.getLogger(MybatisAutoConfiguration.class);
   91 | 
   92 |   private final MybatisProperties properties;
   93 | 
   94 |   private final Interceptor[] interceptors;
   95 | 
   96 |   private final TypeHandler[] typeHandlers;
   97 | 
   98 |   private final LanguageDriver[] languageDrivers;
   99 | 
  100 |   private final ResourceLoader resourceLoader;
  101 | 
  102 |   private final DatabaseIdProvider databaseIdProvider;
  103 | 
  104 |   private final List<ConfigurationCustomizer> configurationCustomizers;
  105 | 
  106 |   private final List<SqlSessionFactoryBeanCustomizer> sqlSessionFactoryBeanCustomizers;
  107 | 
  108 |   public MybatisAutoConfiguration(MybatisProperties properties, ObjectProvider<Interceptor[]> interceptorsProvider,
  109 |       ObjectProvider<TypeHandler[]> typeHandlersProvider, ObjectProvider<LanguageDriver[]> languageDriversProvider,
  110 |       ResourceLoader resourceLoader, ObjectProvider<DatabaseIdProvider> databaseIdProvider,
  111 |       ObjectProvider<List<ConfigurationCustomizer>> configurationCustomizersProvider,
  112 |       ObjectProvider<List<SqlSessionFactoryBeanCustomizer>> sqlSessionFactoryBeanCustomizers) {
  113 |     this.properties = properties;
  114 |     this.interceptors = interceptorsProvider.getIfAvailable();
  115 |     this.typeHandlers = typeHandlersProvider.getIfAvailable();
  116 |     this.languageDrivers = languageDriversProvider.getIfAvailable();
  117 |     this.resourceLoader = resourceLoader;
  118 |     this.databaseIdProvider = databaseIdProvider.getIfAvailable();
  119 |     this.configurationCustomizers = configurationCustomizersProvider.getIfAvailable();
  120 |     this.sqlSessionFactoryBeanCustomizers = sqlSessionFactoryBeanCustomizers.getIfAvailable();
  121 |   }
  122 | 
  123 |   @Override
  124 |   public void afterPropertiesSet() {
  125 |     checkConfigFileExists();
  126 |   }
  127 | 
  128 |   private void checkConfigFileExists() {
  129 |     if (this.properties.isCheckConfigLocation() && StringUtils.hasText(this.properties.getConfigLocation())) {
  130 |       Resource resource = this.resourceLoader.getResource(this.properties.getConfigLocation());
  131 |       Assert.state(resource.exists(),
  132 |           "Cannot find config location: " + resource + " (please add config file or check your Mybatis configuration)");
  133 |     }
  134 |   }
  135 | 
  136 |   @Bean
  137 |   @ConditionalOnMissingBean
  138 |   public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
  139 |     SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
  140 |     factory.setDataSource(dataSource);
  141 |     if (properties.getConfiguration() == null || properties.getConfiguration().getVfsImpl() == null) {
  142 |       factory.setVfs(SpringBootVFS.class);
  143 |     }
  144 |     if (StringUtils.hasText(this.properties.getConfigLocation())) {
  145 |       factory.setConfigLocation(this.resourceLoader.getResource(this.properties.getConfigLocation()));
  146 |     }
  147 |     applyConfiguration(factory);
  148 |     if (this.properties.getConfigurationProperties() != null) {
  149 |       factory.setConfigurationProperties(this.properties.getConfigurationProperties());
  150 |     }
  151 |     if (!ObjectUtils.isEmpty(this.interceptors)) {
  152 |       factory.setPlugins(this.interceptors);
  153 |     }
  154 |     if (this.databaseIdProvider != null) {
  155 |       factory.setDatabaseIdProvider(this.databaseIdProvider);
  156 |     }
  157 |     if (StringUtils.hasLength(this.properties.getTypeAliasesPackage())) {
  158 |       factory.setTypeAliasesPackage(this.properties.getTypeAliasesPackage());
  159 |     }
  160 |     if (this.properties.getTypeAliasesSuperType() != null) {
  161 |       factory.setTypeAliasesSuperType(this.properties.getTypeAliasesSuperType());
  162 |     }
  163 |     if (StringUtils.hasLength(this.properties.getTypeHandlersPackage())) {
  164 |       factory.setTypeHandlersPackage(this.properties.getTypeHandlersPackage());
  165 |     }
  166 |     if (!ObjectUtils.isEmpty(this.typeHandlers)) {
  167 |       factory.setTypeHandlers(this.typeHandlers);
  168 |     }
  169 |     Resource[] mapperLocations = this.properties.resolveMapperLocations();
  170 |     if (!ObjectUtils.isEmpty(mapperLocations)) {
  171 |       factory.setMapperLocations(mapperLocations);
  172 |     }
  173 |     Set<String> factoryPropertyNames = Stream
  174 |         .of(new BeanWrapperImpl(SqlSessionFactoryBean.class).getPropertyDescriptors()).map(PropertyDescriptor::getName)
  175 |         .collect(Collectors.toSet());
  176 |     Class<? extends LanguageDriver> defaultLanguageDriver = this.properties.getDefaultScriptingLanguageDriver();
  177 |     if (factoryPropertyNames.contains("scriptingLanguageDrivers") && !ObjectUtils.isEmpty(this.languageDrivers)) {
  178 |       // Need to mybatis-spring 2.0.2+
  179 |       factory.setScriptingLanguageDrivers(this.languageDrivers);
  180 |       if (defaultLanguageDriver == null && this.languageDrivers.length == 1) {
  181 |         defaultLanguageDriver = this.languageDrivers[0].getClass();
  182 |       }
  183 |     }
  184 |     if (factoryPropertyNames.contains("defaultScriptingLanguageDriver")) {
  185 |       // Need to mybatis-spring 2.0.2+
  186 |       factory.setDefaultScriptingLanguageDriver(defaultLanguageDriver);
  187 |     }
  188 |     applySqlSessionFactoryBeanCustomizers(factory);
  189 |     return factory.getObject();
  190 |   }
  191 | 
  192 |   private void applyConfiguration(SqlSessionFactoryBean factory) {
  193 |     MybatisProperties.CoreConfiguration coreConfiguration = this.properties.getConfiguration();
  194 |     Configuration configuration = null;
  195 |     if (coreConfiguration != null || !StringUtils.hasText(this.properties.getConfigLocation())) {
  196 |       configuration = new Configuration();
  197 |     }
  198 |     if (configuration != null && coreConfiguration != null) {
  199 |       coreConfiguration.applyTo(configuration);
  200 |     }
  201 |     if (configuration != null && !CollectionUtils.isEmpty(this.configurationCustomizers)) {
  202 |       for (ConfigurationCustomizer customizer : this.configurationCustomizers) {
  203 |         customizer.customize(configuration);
  204 |       }
  205 |     }
  206 |     factory.setConfiguration(configuration);
  207 |   }
  208 | 
  209 |   private void applySqlSessionFactoryBeanCustomizers(SqlSessionFactoryBean factory) {
  210 |     if (!CollectionUtils.isEmpty(this.sqlSessionFactoryBeanCustomizers)) {
  211 |       for (SqlSessionFactoryBeanCustomizer customizer : this.sqlSessionFactoryBeanCustomizers) {
  212 |         customizer.customize(factory);
  213 |       }
  214 |     }
  215 |   }
  216 | 
  217 |   @Bean
  218 |   @ConditionalOnMissingBean
  219 |   public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
  220 |     ExecutorType executorType = this.properties.getExecutorType();
  221 |     if (executorType != null) {
  222 |       return new SqlSessionTemplate(sqlSessionFactory, executorType);
  223 |     } else {
  224 |       return new SqlSessionTemplate(sqlSessionFactory);
  225 |     }
  226 |   }
  227 | 
  228 |   /**
  229 |    * This will just scan the same base package as Spring Boot does. If you want more power, you can explicitly use
  230 |    * {@link org.mybatis.spring.annotation.MapperScan} but this will get typed mappers working correctly, out-of-the-box,
  231 |    * similar to using Spring Data JPA repositories.
  232 |    */
  233 |   public static class AutoConfiguredMapperScannerRegistrar
  234 |       implements BeanFactoryAware, EnvironmentAware, ImportBeanDefinitionRegistrar {
  235 | 
  236 |     private BeanFactory beanFactory;
  237 |     private Environment environment;
  238 | 
  239 |     @Override
  240 |     public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
  241 | 
  242 |       if (!AutoConfigurationPackages.has(this.beanFactory)) {
  243 |         logger.debug("Could not determine auto-configuration package, automatic mapper scanning disabled.");
  244 |         return;
  245 |       }
  246 | 
  247 |       logger.debug("Searching for mappers annotated with @Mapper");
  248 | 
  249 |       List<String> packages = AutoConfigurationPackages.get(this.beanFactory);
  250 |       if (logger.isDebugEnabled()) {
  251 |         packages.forEach(pkg -> logger.debug("Using auto-configuration base package '{}'", pkg));
  252 |       }
  253 | 
  254 |       BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MapperScannerConfigurer.class);
  255 |       builder.addPropertyValue("processPropertyPlaceHolders", true);
  256 |       builder.addPropertyValue("annotationClass", Mapper.class);
  257 |       builder.addPropertyValue("basePackage", StringUtils.collectionToCommaDelimitedString(packages));
  258 |       BeanWrapper beanWrapper = new BeanWrapperImpl(MapperScannerConfigurer.class);
  259 |       Set<String> propertyNames = Stream.of(beanWrapper.getPropertyDescriptors()).map(PropertyDescriptor::getName)
  260 |           .collect(Collectors.toSet());
  261 |       if (propertyNames.contains("lazyInitialization")) {
  262 |         // Need to mybatis-spring 2.0.2+
  263 |         builder.addPropertyValue("lazyInitialization", "${mybatis.lazy-initialization:false}");
  264 |       }
  265 |       if (propertyNames.contains("defaultScope")) {
  266 |         // Need to mybatis-spring 2.0.6+
  267 |         builder.addPropertyValue("defaultScope", "${mybatis.mapper-default-scope:}");
  268 |       }
  269 | 
  270 |       // for spring-native
  271 |       boolean injectSqlSession = environment.getProperty("mybatis.inject-sql-session-on-mapper-scan", Boolean.class,
  272 |           Boolean.TRUE);
  273 |       if (injectSqlSession && this.beanFactory instanceof ListableBeanFactory) {
  274 |         ListableBeanFactory listableBeanFactory = (ListableBeanFactory) this.beanFactory;
  275 |         Optional<String> sqlSessionTemplateBeanName = Optional
  276 |             .ofNullable(getBeanNameForType(SqlSessionTemplate.class, listableBeanFactory));
  277 |         Optional<String> sqlSessionFactoryBeanName = Optional
  278 |             .ofNullable(getBeanNameForType(SqlSessionFactory.class, listableBeanFactory));
  279 |         if (sqlSessionTemplateBeanName.isPresent() || !sqlSessionFactoryBeanName.isPresent()) {
  280 |           builder.addPropertyValue("sqlSessionTemplateBeanName",
  281 |               sqlSessionTemplateBeanName.orElse("sqlSessionTemplate"));
  282 |         } else {
  283 |           builder.addPropertyValue("sqlSessionFactoryBeanName", sqlSessionFactoryBeanName.orElseThrow());
  284 |         }
  285 |       }
  286 |       builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
  287 | 
  288 |       registry.registerBeanDefinition(MapperScannerConfigurer.class.getName(), builder.getBeanDefinition());
  289 |     }
  290 | 
  291 |     @Override
  292 |     public void setBeanFactory(BeanFactory beanFactory) {
  293 |       this.beanFactory = beanFactory;
  294 |     }
  295 | 
  296 |     @Override
  297 |     public void setEnvironment(Environment environment) {
  298 |       this.environment = environment;
  299 |     }
  300 | 
  301 |     private String getBeanNameForType(Class<?> type, ListableBeanFactory factory) {
  302 |       String[] beanNames = factory.getBeanNamesForType(type);
  303 |       return beanNames.length > 0 ? beanNames[0] : null;
  304 |     }
  305 | 
  306 |   }
  307 | 
  308 |   /**
  309 |    * If mapper registering configuration or mapper scanning configuration not present, this configuration allow to scan
  310 |    * mappers based on the same component-scanning path as Spring Boot itself.
  311 |    */
  312 |   @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
  313 |   @Import(AutoConfiguredMapperScannerRegistrar.class)
  314 |   @ConditionalOnMissingBean({ MapperFactoryBean.class, MapperScannerConfigurer.class })
  315 |   public static class MapperScannerRegistrarNotFoundConfiguration implements InitializingBean {
  316 | 
  317 |     @Override
  318 |     public void afterPropertiesSet() {
  319 |       logger.debug(
  320 |           "Not found configuration for registering mapper bean using @MapperScan, MapperFactoryBean and MapperScannerConfigurer.");
  321 |     }
  322 | 
  323 |   }
  324 | 
  325 | }
```
