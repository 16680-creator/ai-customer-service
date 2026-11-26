<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/ConfigurationPropertiesBindingPostProcessor.java -->
<!-- 正文引用：[[源码:ConfigurationPropertiesBindingPostProcessor.java#L起-L止]] ；代码块头部：(src: ConfigurationPropertiesBindingPostProcessor.java#L起-L止) -->

# ConfigurationPropertiesBindingPostProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/ConfigurationPropertiesBindingPostProcessor.java` |
| 行数 | 114 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:08:19.973Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties;
    6 | 
    7 | import org.jspecify.annotations.Nullable;
    8 | 
    9 | import org.springframework.beans.BeansException;
   10 | import org.springframework.beans.factory.InitializingBean;
   11 | import org.springframework.beans.factory.config.BeanDefinition;
   12 | import org.springframework.beans.factory.config.BeanPostProcessor;
   13 | import org.springframework.beans.factory.support.BeanDefinitionBuilder;
   14 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   15 | import org.springframework.boot.context.properties.bind.BindMethod;
   16 | import org.springframework.context.ApplicationContext;
   17 | import org.springframework.context.ApplicationContextAware;
   18 | import org.springframework.core.Ordered;
   19 | import org.springframework.core.PriorityOrdered;
   20 | import org.springframework.core.env.PropertySources;
   21 | import org.springframework.util.Assert;
   22 | 
   23 | /**
   24 |  * {@link BeanPostProcessor} to bind {@link PropertySources} to beans annotated with
   25 |  * {@link ConfigurationProperties @ConfigurationProperties}.
   26 |  *
   27 |  * @author Dave Syer
   28 |  * @author Phillip Webb
   29 |  * @author Christian Dupuis
   30 |  * @author Stephane Nicoll
   31 |  * @author Madhura Bhave
   32 |  * @since 1.0.0
   33 |  */
   34 | public class ConfigurationPropertiesBindingPostProcessor
   35 | 		implements BeanPostProcessor, PriorityOrdered, ApplicationContextAware, InitializingBean {
   36 | 
   37 | 	/**
   38 | 	 * The bean name that this post-processor is registered with.
   39 | 	 */
   40 | 	public static final String BEAN_NAME = ConfigurationPropertiesBindingPostProcessor.class.getName();
   41 | 
   42 | 	@SuppressWarnings("NullAway.Init")
   43 | 	private ApplicationContext applicationContext;
   44 | 
   45 | 	@SuppressWarnings("NullAway.Init")
   46 | 	private BeanDefinitionRegistry registry;
   47 | 
   48 | 	@SuppressWarnings("NullAway.Init")
   49 | 	private ConfigurationPropertiesBinder binder;
   50 | 
   51 | 	@Override
   52 | 	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
   53 | 		this.applicationContext = applicationContext;
   54 | 	}
   55 | 
   56 | 	@Override
   57 | 	public void afterPropertiesSet() throws Exception {
   58 | 		// We can't use constructor injection of the application context because
   59 | 		// it causes eager factory bean initialization
   60 | 		this.registry = (BeanDefinitionRegistry) this.applicationContext.getAutowireCapableBeanFactory();
   61 | 		this.binder = ConfigurationPropertiesBinder.get(this.applicationContext);
   62 | 	}
   63 | 
   64 | 	@Override
   65 | 	public int getOrder() {
   66 | 		return Ordered.HIGHEST_PRECEDENCE + 1;
   67 | 	}
   68 | 
   69 | 	@Override
   70 | 	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
   71 | 		if (!hasBoundValueObject(beanName)) {
   72 | 			bind(ConfigurationPropertiesBean.get(this.applicationContext, bean, beanName));
   73 | 		}
   74 | 		return bean;
   75 | 	}
   76 | 
   77 | 	private boolean hasBoundValueObject(String beanName) {
   78 | 		return BindMethod.VALUE_OBJECT.equals(BindMethodAttribute.get(this.registry, beanName));
   79 | 	}
   80 | 
   81 | 	private void bind(@Nullable ConfigurationPropertiesBean bean) {
   82 | 		if (bean == null) {
   83 | 			return;
   84 | 		}
   85 | 		Assert.state(bean.asBindTarget().getBindMethod() != BindMethod.VALUE_OBJECT,
   86 | 				"Cannot bind @ConfigurationProperties for bean '" + bean.getName()
   87 | 						+ "'. Ensure that @ConstructorBinding has not been applied to regular bean");
   88 | 		try {
   89 | 			this.binder.bind(bean);
   90 | 		}
   91 | 		catch (Exception ex) {
   92 | 			throw new ConfigurationPropertiesBindException(bean, ex);
   93 | 		}
   94 | 	}
   95 | 
   96 | 	/**
   97 | 	 * Register a {@link ConfigurationPropertiesBindingPostProcessor} bean if one is not
   98 | 	 * already registered.
   99 | 	 * @param registry the bean definition registry
  100 | 	 * @since 2.2.0
  101 | 	 */
  102 | 	public static void register(BeanDefinitionRegistry registry) {
  103 | 		Assert.notNull(registry, "'registry' must not be null");
  104 | 		if (!registry.containsBeanDefinition(BEAN_NAME)) {
  105 | 			BeanDefinition definition = BeanDefinitionBuilder
  106 | 				.rootBeanDefinition(ConfigurationPropertiesBindingPostProcessor.class)
  107 | 				.getBeanDefinition();
  108 | 			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
  109 | 			registry.registerBeanDefinition(BEAN_NAME, definition);
  110 | 		}
  111 | 		ConfigurationPropertiesBinder.register(registry);
  112 | 	}
  113 | 
  114 | }
```
