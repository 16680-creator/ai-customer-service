<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/ConfigurationPropertiesBinder.java -->
<!-- 正文引用：[[源码:ConfigurationPropertiesBinder.java#L起-L止]] ；代码块头部：(src: ConfigurationPropertiesBinder.java#L起-L止) -->

# ConfigurationPropertiesBinder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/ConfigurationPropertiesBinder.java` |
| 行数 | 288 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:08:19.781Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.List;
    9 | import java.util.function.Consumer;
   10 | 
   11 | import org.jspecify.annotations.Nullable;
   12 | 
   13 | import org.springframework.beans.BeansException;
   14 | import org.springframework.beans.PropertyEditorRegistry;
   15 | import org.springframework.beans.factory.BeanFactory;
   16 | import org.springframework.beans.factory.FactoryBean;
   17 | import org.springframework.beans.factory.config.BeanDefinition;
   18 | import org.springframework.beans.factory.support.BeanDefinitionBuilder;
   19 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   20 | import org.springframework.boot.context.properties.bind.AbstractBindHandler;
   21 | import org.springframework.boot.context.properties.bind.BindContext;
   22 | import org.springframework.boot.context.properties.bind.BindHandler;
   23 | import org.springframework.boot.context.properties.bind.BindResult;
   24 | import org.springframework.boot.context.properties.bind.Bindable;
   25 | import org.springframework.boot.context.properties.bind.Bindable.BindRestriction;
   26 | import org.springframework.boot.context.properties.bind.Binder;
   27 | import org.springframework.boot.context.properties.bind.BoundPropertiesTrackingBindHandler;
   28 | import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
   29 | import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
   30 | import org.springframework.boot.context.properties.bind.handler.IgnoreTopLevelConverterNotFoundBindHandler;
   31 | import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
   32 | import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler;
   33 | import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
   34 | import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
   35 | import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
   36 | import org.springframework.boot.context.properties.source.UnboundElementsSourceFilter;
   37 | import org.springframework.context.ApplicationContext;
   38 | import org.springframework.context.ApplicationContextAware;
   39 | import org.springframework.context.ConfigurableApplicationContext;
   40 | import org.springframework.core.annotation.MergedAnnotations;
   41 | import org.springframework.core.convert.ConversionService;
   42 | import org.springframework.core.env.PropertySources;
   43 | import org.springframework.util.Assert;
   44 | import org.springframework.validation.Errors;
   45 | import org.springframework.validation.Validator;
   46 | import org.springframework.validation.annotation.Validated;
   47 | 
   48 | /**
   49 |  * Internal class used by the {@link ConfigurationPropertiesBindingPostProcessor} to
   50 |  * handle the actual {@link ConfigurationProperties @ConfigurationProperties} binding.
   51 |  *
   52 |  * @author Stephane Nicoll
   53 |  * @author Phillip Webb
   54 |  */
   55 | class ConfigurationPropertiesBinder {
   56 | 
   57 | 	private static final String BEAN_NAME = "org.springframework.boot.context.internalConfigurationPropertiesBinder";
   58 | 
   59 | 	private static final String VALIDATOR_BEAN_NAME = EnableConfigurationProperties.VALIDATOR_BEAN_NAME;
   60 | 
   61 | 	private final ApplicationContext applicationContext;
   62 | 
   63 | 	private final PropertySources propertySources;
   64 | 
   65 | 	private final @Nullable Validator configurationPropertiesValidator;
   66 | 
   67 | 	private final boolean jsr303Present;
   68 | 
   69 | 	private volatile @Nullable List<ConfigurationPropertiesBindHandlerAdvisor> bindHandlerAdvisors;
   70 | 
   71 | 	private volatile @Nullable Binder binder;
   72 | 
   73 | 	ConfigurationPropertiesBinder(ApplicationContext applicationContext) {
   74 | 		this.applicationContext = applicationContext;
   75 | 		this.propertySources = new PropertySourcesDeducer(applicationContext).getPropertySources();
   76 | 		this.configurationPropertiesValidator = getConfigurationPropertiesValidator(applicationContext);
   77 | 		this.jsr303Present = ConfigurationPropertiesJsr303Validator.isJsr303Present(applicationContext);
   78 | 	}
   79 | 
   80 | 	BindResult<?> bind(ConfigurationPropertiesBean propertiesBean) {
   81 | 		Bindable<?> target = propertiesBean.asBindTarget();
   82 | 		ConfigurationProperties annotation = propertiesBean.getAnnotation();
   83 | 		BindHandler bindHandler = getBindHandler(target, annotation);
   84 | 		return getBinder().bind(annotation.prefix(), target, bindHandler);
   85 | 	}
   86 | 
   87 | 	Object bindOrCreate(ConfigurationPropertiesBean propertiesBean) {
   88 | 		Bindable<?> target = propertiesBean.asBindTarget();
   89 | 		ConfigurationProperties annotation = propertiesBean.getAnnotation();
   90 | 		BindHandler bindHandler = getBindHandler(target, annotation);
   91 | 		return getBinder().bindOrCreate(annotation.prefix(), target, bindHandler);
   92 | 	}
   93 | 
   94 | 	private @Nullable Validator getConfigurationPropertiesValidator(ApplicationContext applicationContext) {
   95 | 		if (applicationContext.containsBean(VALIDATOR_BEAN_NAME)) {
   96 | 			return applicationContext.getBean(VALIDATOR_BEAN_NAME, Validator.class);
   97 | 		}
   98 | 		return null;
   99 | 	}
  100 | 
  101 | 	private <T> BindHandler getBindHandler(Bindable<T> target, ConfigurationProperties annotation) {
  102 | 		List<Validator> validators = getValidators(target);
  103 | 		BindHandler handler = getHandler();
  104 | 		handler = new ConfigurationPropertiesBindHandler(handler);
  105 | 		if (annotation.ignoreInvalidFields()) {
  106 | 			handler = new IgnoreErrorsBindHandler(handler);
  107 | 		}
  108 | 		if (!annotation.ignoreUnknownFields()) {
  109 | 			UnboundElementsSourceFilter filter = new UnboundElementsSourceFilter();
  110 | 			handler = new NoUnboundElementsBindHandler(handler, filter);
  111 | 		}
  112 | 		if (!validators.isEmpty()) {
  113 | 			handler = new ValidationBindHandler(handler, validators.toArray(new Validator[0]));
  114 | 		}
  115 | 		for (ConfigurationPropertiesBindHandlerAdvisor advisor : getBindHandlerAdvisors()) {
  116 | 			handler = advisor.apply(handler);
  117 | 		}
  118 | 		return handler;
  119 | 	}
  120 | 
  121 | 	private List<ConfigurationPropertiesBindHandlerAdvisor> getBindHandlerAdvisors() {
  122 | 		List<ConfigurationPropertiesBindHandlerAdvisor> bindHandlerAdvisors = this.bindHandlerAdvisors;
  123 | 		if (bindHandlerAdvisors == null) {
  124 | 			bindHandlerAdvisors = this.applicationContext
  125 | 				.getBeanProvider(ConfigurationPropertiesBindHandlerAdvisor.class)
  126 | 				.orderedStream()
  127 | 				.toList();
  128 | 			this.bindHandlerAdvisors = bindHandlerAdvisors;
  129 | 		}
  130 | 		return bindHandlerAdvisors;
  131 | 	}
  132 | 
  133 | 	private IgnoreTopLevelConverterNotFoundBindHandler getHandler() {
  134 | 		BoundConfigurationProperties bound = BoundConfigurationProperties.get(this.applicationContext);
  135 | 		return (bound != null)
  136 | 				? new IgnoreTopLevelConverterNotFoundBindHandler(new BoundPropertiesTrackingBindHandler(bound::add))
  137 | 				: new IgnoreTopLevelConverterNotFoundBindHandler();
  138 | 	}
  139 | 
  140 | 	private List<Validator> getValidators(Bindable<?> target) {
  141 | 		List<Validator> validators = new ArrayList<>(3);
  142 | 		if (this.configurationPropertiesValidator != null) {
  143 | 			validators.add(this.configurationPropertiesValidator);
  144 | 		}
  145 | 		if (this.jsr303Present && target.getAnnotation(Validated.class) != null) {
  146 | 			Class<?> resolved = target.getType().resolve();
  147 | 			Assert.state(resolved != null, "'resolved' must not be null");
  148 | 			validators.add(getJsr303Validator(resolved));
  149 | 		}
  150 | 		Validator selfValidator = getSelfValidator(target);
  151 | 		if (selfValidator != null) {
  152 | 			validators.add(selfValidator);
  153 | 		}
  154 | 		return validators;
  155 | 	}
  156 | 
  157 | 	private @Nullable Validator getSelfValidator(Bindable<?> target) {
  158 | 		if (target.getValue() != null) {
  159 | 			Object value = target.getValue().get();
  160 | 			return (value instanceof Validator validator) ? validator : null;
  161 | 		}
  162 | 		Class<?> type = target.getType().resolve();
  163 | 		if (type != null && Validator.class.isAssignableFrom(type)) {
  164 | 			return new SelfValidatingConstructorBoundBindableValidator(type);
  165 | 		}
  166 | 		return null;
  167 | 	}
  168 | 
  169 | 	private Validator getJsr303Validator(Class<?> type) {
  170 | 		return new ConfigurationPropertiesJsr303Validator(this.applicationContext, type);
  171 | 	}
  172 | 
  173 | 	private Binder getBinder() {
  174 | 		Binder binder = this.binder;
  175 | 		if (binder == null) {
  176 | 			binder = new Binder(getConfigurationPropertySources(), getPropertySourcesPlaceholdersResolver(),
  177 | 					getConversionServices(), getPropertyEditorInitializer(), null, null);
  178 | 			this.binder = binder;
  179 | 		}
  180 | 		return binder;
  181 | 	}
  182 | 
  183 | 	private Iterable<ConfigurationPropertySource> getConfigurationPropertySources() {
  184 | 		return ConfigurationPropertySources.from(this.propertySources);
  185 | 	}
  186 | 
  187 | 	private PropertySourcesPlaceholdersResolver getPropertySourcesPlaceholdersResolver() {
  188 | 		return new PropertySourcesPlaceholdersResolver(this.propertySources);
  189 | 	}
  190 | 
  191 | 	private @Nullable List<ConversionService> getConversionServices() {
  192 | 		return new ConversionServiceDeducer(this.applicationContext).getConversionServices();
  193 | 	}
  194 | 
  195 | 	private @Nullable Consumer<PropertyEditorRegistry> getPropertyEditorInitializer() {
  196 | 		if (this.applicationContext instanceof ConfigurableApplicationContext configurableContext) {
  197 | 			return configurableContext.getBeanFactory()::copyRegisteredEditorsTo;
  198 | 		}
  199 | 		return null;
  200 | 	}
  201 | 
  202 | 	static void register(BeanDefinitionRegistry registry) {
  203 | 		if (!registry.containsBeanDefinition(BEAN_NAME)) {
  204 | 			BeanDefinition definition = BeanDefinitionBuilder
  205 | 				.rootBeanDefinition(ConfigurationPropertiesBinderFactory.class)
  206 | 				.getBeanDefinition();
  207 | 			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
  208 | 			registry.registerBeanDefinition(BEAN_NAME, definition);
  209 | 		}
  210 | 	}
  211 | 
  212 | 	static ConfigurationPropertiesBinder get(BeanFactory beanFactory) {
  213 | 		return beanFactory.getBean(BEAN_NAME, ConfigurationPropertiesBinder.class);
  214 | 	}
  215 | 
  216 | 	/**
  217 | 	 * {@link BindHandler} to deal with
  218 | 	 * {@link ConfigurationProperties @ConfigurationProperties} concerns.
  219 | 	 */
  220 | 	private static class ConfigurationPropertiesBindHandler extends AbstractBindHandler {
  221 | 
  222 | 		ConfigurationPropertiesBindHandler(BindHandler handler) {
  223 | 			super(handler);
  224 | 		}
  225 | 
  226 | 		@Override
  227 | 		public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
  228 | 			return isConfigurationProperties(target.getType().resolve())
  229 | 					? target.withBindRestrictions(BindRestriction.NO_DIRECT_PROPERTY) : target;
  230 | 		}
  231 | 
  232 | 		private boolean isConfigurationProperties(@Nullable Class<?> target) {
  233 | 			return target != null && MergedAnnotations.from(target).isPresent(ConfigurationProperties.class);
  234 | 		}
  235 | 
  236 | 	}
  237 | 
  238 | 	/**
  239 | 	 * {@link FactoryBean} to create the {@link ConfigurationPropertiesBinder}.
  240 | 	 */
  241 | 	static class ConfigurationPropertiesBinderFactory
  242 | 			implements FactoryBean<ConfigurationPropertiesBinder>, ApplicationContextAware {
  243 | 
  244 | 		private @Nullable ConfigurationPropertiesBinder binder;
  245 | 
  246 | 		@Override
  247 | 		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
  248 | 			this.binder = (this.binder != null) ? this.binder : new ConfigurationPropertiesBinder(applicationContext);
  249 | 		}
  250 | 
  251 | 		@Override
  252 | 		public Class<?> getObjectType() {
  253 | 			return ConfigurationPropertiesBinder.class;
  254 | 		}
  255 | 
  256 | 		@Override
  257 | 		public ConfigurationPropertiesBinder getObject() throws Exception {
  258 | 			Assert.state(this.binder != null, "Binder was not created due to missing setApplicationContext call");
  259 | 			return this.binder;
  260 | 		}
  261 | 
  262 | 	}
  263 | 
  264 | 	/**
  265 | 	 * A {@code Validator} for a constructor-bound {@code Bindable} where the type being
  266 | 	 * bound is itself a {@code Validator} implementation.
  267 | 	 */
  268 | 	static class SelfValidatingConstructorBoundBindableValidator implements Validator {
  269 | 
  270 | 		private final Class<?> type;
  271 | 
  272 | 		SelfValidatingConstructorBoundBindableValidator(Class<?> type) {
  273 | 			this.type = type;
  274 | 		}
  275 | 
  276 | 		@Override
  277 | 		public boolean supports(Class<?> candidate) {
  278 | 			return candidate.isAssignableFrom(this.type);
  279 | 		}
  280 | 
  281 | 		@Override
  282 | 		public void validate(Object target, Errors errors) {
  283 | 			((Validator) target).validate(target, errors);
  284 | 		}
  285 | 
  286 | 	}
  287 | 
  288 | }
```
