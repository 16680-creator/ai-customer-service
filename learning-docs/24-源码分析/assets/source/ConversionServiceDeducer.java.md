<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/ConversionServiceDeducer.java -->
<!-- 正文引用：[[源码:ConversionServiceDeducer.java#L起-L止]] ；代码块头部：(src: ConversionServiceDeducer.java#L起-L止) -->

# ConversionServiceDeducer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/ConversionServiceDeducer.java` |
| 行数 | 80 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:52.433Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.Collections;
    9 | import java.util.List;
   10 | import java.util.Map;
   11 | 
   12 | import org.jspecify.annotations.Nullable;
   13 | 
   14 | import org.springframework.boot.convert.ApplicationConversionService;
   15 | import org.springframework.context.ApplicationContext;
   16 | import org.springframework.context.ConfigurableApplicationContext;
   17 | import org.springframework.core.convert.ConversionService;
   18 | import org.springframework.core.convert.support.DefaultConversionService;
   19 | import org.springframework.format.support.FormattingConversionService;
   20 | 
   21 | /**
   22 |  * Utility to deduce the {@link ConversionService} to use for configuration properties
   23 |  * binding.
   24 |  *
   25 |  * @author Phillip Webb
   26 |  */
   27 | class ConversionServiceDeducer {
   28 | 
   29 | 	private final ApplicationContext applicationContext;
   30 | 
   31 | 	ConversionServiceDeducer(ApplicationContext applicationContext) {
   32 | 		this.applicationContext = applicationContext;
   33 | 	}
   34 | 
   35 | 	@Nullable List<ConversionService> getConversionServices() {
   36 | 		if (hasUserDefinedConfigurationServiceBean()) {
   37 | 			return Collections.singletonList(this.applicationContext
   38 | 				.getBean(ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
   39 | 		}
   40 | 		if (this.applicationContext instanceof ConfigurableApplicationContext configurableContext) {
   41 | 			return getConversionServices(configurableContext);
   42 | 		}
   43 | 		return null;
   44 | 	}
   45 | 
   46 | 	private List<ConversionService> getConversionServices(ConfigurableApplicationContext applicationContext) {
   47 | 		List<ConversionService> conversionServices = new ArrayList<>();
   48 | 		FormattingConversionService beansConverterService = new FormattingConversionService();
   49 | 		Map<String, Object> converterBeans = addBeans(applicationContext, beansConverterService);
   50 | 		if (!converterBeans.isEmpty()) {
   51 | 			conversionServices.add(beansConverterService);
   52 | 		}
   53 | 		if (applicationContext.getBeanFactory().getConversionService() != null) {
   54 | 			conversionServices.add(applicationContext.getBeanFactory().getConversionService());
   55 | 		}
   56 | 		if (!converterBeans.isEmpty()) {
   57 | 			// Converters beans used to be added to a custom ApplicationConversionService
   58 | 			// after the BeanFactory's ConversionService. For backwards compatibility, we
   59 | 			// add an ApplicationConversationService as a fallback in the same place in
   60 | 			// the list.
   61 | 			conversionServices.add(ApplicationConversionService.getSharedInstance());
   62 | 		}
   63 | 		return conversionServices;
   64 | 	}
   65 | 
   66 | 	private Map<String, Object> addBeans(ConfigurableApplicationContext applicationContext,
   67 | 			FormattingConversionService converterService) {
   68 | 		DefaultConversionService.addCollectionConverters(converterService);
   69 | 		converterService.addConverter(new ConfigurationPropertiesCharSequenceToObjectConverter(converterService));
   70 | 		return ApplicationConversionService.addBeans(converterService, applicationContext.getBeanFactory(),
   71 | 				ConfigurationPropertiesBinding.VALUE);
   72 | 	}
   73 | 
   74 | 	private boolean hasUserDefinedConfigurationServiceBean() {
   75 | 		String beanName = ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME;
   76 | 		return this.applicationContext.containsBean(beanName) && this.applicationContext.getAutowireCapableBeanFactory()
   77 | 			.isTypeMatch(beanName, ConversionService.class);
   78 | 	}
   79 | 
   80 | }
```
