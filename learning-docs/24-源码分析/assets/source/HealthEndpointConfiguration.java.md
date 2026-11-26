<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-health:4.2.0 | path: org/springframework/boot/health/autoconfigure/actuate/endpoint/HealthEndpointConfiguration.java -->
<!-- 正文引用：[[源码:HealthEndpointConfiguration.java#L起-L止]] ；代码块头部：(src: HealthEndpointConfiguration.java#L起-L止) -->

# HealthEndpointConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-health:4.2.0` |
| 路径 | `org/springframework/boot/health/autoconfigure/actuate/endpoint/HealthEndpointConfiguration.java` |
| 行数 | 119 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-health/4.2.0/spring-boot-health-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:03.014Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.health.autoconfigure.actuate.endpoint;
    6 | 
    7 | import org.springframework.beans.BeansException;
    8 | import org.springframework.beans.factory.ObjectProvider;
    9 | import org.springframework.beans.factory.config.BeanPostProcessor;
   10 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
   11 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   12 | import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
   13 | import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroups;
   14 | import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroupsPostProcessor;
   15 | import org.springframework.boot.health.actuate.endpoint.HttpCodeStatusMapper;
   16 | import org.springframework.boot.health.actuate.endpoint.StatusAggregator;
   17 | import org.springframework.boot.health.autoconfigure.contributor.HealthContributorMembershipValidator;
   18 | import org.springframework.boot.health.registry.HealthContributorRegistry;
   19 | import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;
   20 | import org.springframework.context.ApplicationContext;
   21 | import org.springframework.context.annotation.Bean;
   22 | import org.springframework.context.annotation.Configuration;
   23 | 
   24 | /**
   25 |  * Configuration for {@link HealthEndpoint} infrastructure beans.
   26 |  *
   27 |  * @author Phillip Webb
   28 |  * @see HealthEndpointAutoConfiguration
   29 |  */
   30 | @Configuration(proxyBeanMethods = false)
   31 | class HealthEndpointConfiguration {
   32 | 
   33 | 	private static final String VALIDATE_MEMBERSHIP_PROPERTY = "management.endpoint.health.validate-group-membership";
   34 | 
   35 | 	@Bean
   36 | 	@ConditionalOnMissingBean
   37 | 	StatusAggregator healthStatusAggregator(HealthEndpointProperties properties) {
   38 | 		return StatusAggregator.of(properties.getStatus().getOrder());
   39 | 	}
   40 | 
   41 | 	@Bean
   42 | 	@ConditionalOnMissingBean
   43 | 	HttpCodeStatusMapper healthHttpCodeStatusMapper(HealthEndpointProperties properties) {
   44 | 		return HttpCodeStatusMapper.of(properties.getStatus().getHttpMapping());
   45 | 	}
   46 | 
   47 | 	@Bean
   48 | 	@ConditionalOnMissingBean(HealthEndpointGroups.class)
   49 | 	AutoConfiguredHealthEndpointGroups healthEndpointGroups(ApplicationContext applicationContext,
   50 | 			HealthEndpointProperties properties) {
   51 | 		return new AutoConfiguredHealthEndpointGroups(applicationContext, properties);
   52 | 	}
   53 | 
   54 | 	@Bean
   55 | 	GroupsHealthContributorNameValidator groupsHealthContributorNameValidator(
   56 | 			ObjectProvider<HealthEndpointGroups> healthEndpointGroups) {
   57 | 		return new GroupsHealthContributorNameValidator(healthEndpointGroups.getIfAvailable());
   58 | 	}
   59 | 
   60 | 	@Bean
   61 | 	@ConditionalOnBooleanProperty(name = VALIDATE_MEMBERSHIP_PROPERTY, matchIfMissing = true)
   62 | 	HealthContributorMembershipValidator healthEndpointGroupMembershipValidator(HealthEndpointProperties properties,
   63 | 			HealthContributorRegistry healthContributorRegistry,
   64 | 			ObjectProvider<ReactiveHealthContributorRegistry> reactiveHealthContributorRegistry) {
   65 | 		return new HealthContributorMembershipValidator(healthContributorRegistry,
   66 | 				reactiveHealthContributorRegistry.getIfAvailable(), VALIDATE_MEMBERSHIP_PROPERTY,
   67 | 				(members) -> properties.getGroup().forEach((groupName, group) -> {
   68 | 					String property = "management.endpoint.health.group." + groupName;
   69 | 					members.member(property + ".include".formatted(groupName), group.getInclude());
   70 | 					members.member(property + ".exclude".formatted(groupName), group.getExclude());
   71 | 				}));
   72 | 	}
   73 | 
   74 | 	@Bean
   75 | 	@ConditionalOnMissingBean
   76 | 	HealthEndpoint healthEndpoint(HealthContributorRegistry healthContributorRegistry,
   77 | 			ObjectProvider<ReactiveHealthContributorRegistry> reactiveHealthContributorRegistry,
   78 | 			HealthEndpointGroups groups, HealthEndpointProperties properties) {
   79 | 		return new HealthEndpoint(healthContributorRegistry, reactiveHealthContributorRegistry.getIfAvailable(), groups,
   80 | 				properties.getLogging().getSlowIndicatorThreshold());
   81 | 	}
   82 | 
   83 | 	@Bean
   84 | 	static HealthEndpointGroupsBeanPostProcessor healthEndpointGroupsBeanPostProcessor(
   85 | 			ObjectProvider<HealthEndpointGroupsPostProcessor> healthEndpointGroupsPostProcessors) {
   86 | 		return new HealthEndpointGroupsBeanPostProcessor(healthEndpointGroupsPostProcessors);
   87 | 	}
   88 | 
   89 | 	/**
   90 | 	 * {@link BeanPostProcessor} to invoke {@link HealthEndpointGroupsPostProcessor}
   91 | 	 * beans.
   92 | 	 */
   93 | 	static class HealthEndpointGroupsBeanPostProcessor implements BeanPostProcessor {
   94 | 
   95 | 		private final ObjectProvider<HealthEndpointGroupsPostProcessor> postProcessors;
   96 | 
   97 | 		HealthEndpointGroupsBeanPostProcessor(ObjectProvider<HealthEndpointGroupsPostProcessor> postProcessors) {
   98 | 			this.postProcessors = postProcessors;
   99 | 		}
  100 | 
  101 | 		@Override
  102 | 		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
  103 | 			if (bean instanceof HealthEndpointGroups groups) {
  104 | 				return applyPostProcessors(groups);
  105 | 			}
  106 | 			return bean;
  107 | 		}
  108 | 
  109 | 		private Object applyPostProcessors(HealthEndpointGroups bean) {
  110 | 			for (HealthEndpointGroupsPostProcessor postProcessor : this.postProcessors.orderedStream()
  111 | 				.toArray(HealthEndpointGroupsPostProcessor[]::new)) {
  112 | 				bean = postProcessor.postProcessHealthEndpointGroups(bean);
  113 | 			}
  114 | 			return bean;
  115 | 		}
  116 | 
  117 | 	}
  118 | 
  119 | }
```
