<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-health:4.2.0 | path: org/springframework/boot/health/autoconfigure/actuate/endpoint/AutoConfiguredHealthEndpointGroups.java -->
<!-- 正文引用：[[源码:AutoConfiguredHealthEndpointGroups.java#L起-L止]] ；代码块头部：(src: AutoConfiguredHealthEndpointGroups.java#L起-L止) -->

# AutoConfiguredHealthEndpointGroups.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-health:4.2.0` |
| 路径 | `org/springframework/boot/health/autoconfigure/actuate/endpoint/AutoConfiguredHealthEndpointGroups.java` |
| 行数 | 167 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-health/4.2.0/spring-boot-health-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:03.137Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.health.autoconfigure.actuate.endpoint;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.Collections;
    9 | import java.util.List;
   10 | import java.util.Map;
   11 | import java.util.Objects;
   12 | import java.util.Set;
   13 | import java.util.TreeMap;
   14 | import java.util.function.Supplier;
   15 | import java.util.stream.Stream;
   16 | 
   17 | import org.jspecify.annotations.Nullable;
   18 | 
   19 | import org.springframework.beans.factory.BeanFactory;
   20 | import org.springframework.beans.factory.BeanFactoryUtils;
   21 | import org.springframework.beans.factory.ListableBeanFactory;
   22 | import org.springframework.beans.factory.NoSuchBeanDefinitionException;
   23 | import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
   24 | import org.springframework.boot.actuate.endpoint.EndpointId;
   25 | import org.springframework.boot.actuate.endpoint.Show;
   26 | import org.springframework.boot.actuate.endpoint.web.AdditionalPathsMapper;
   27 | import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
   28 | import org.springframework.boot.health.actuate.endpoint.AdditionalHealthEndpointPath;
   29 | import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
   30 | import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroup;
   31 | import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroups;
   32 | import org.springframework.boot.health.actuate.endpoint.HttpCodeStatusMapper;
   33 | import org.springframework.boot.health.actuate.endpoint.StatusAggregator;
   34 | import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointProperties.Group;
   35 | import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthProperties.Status;
   36 | import org.springframework.boot.health.autoconfigure.contributor.HealthContributorMembership;
   37 | import org.springframework.context.ApplicationContext;
   38 | import org.springframework.context.ConfigurableApplicationContext;
   39 | import org.springframework.util.CollectionUtils;
   40 | import org.springframework.util.ObjectUtils;
   41 | 
   42 | /**
   43 |  * Auto-configured {@link HealthEndpointGroups}.
   44 |  *
   45 |  * @author Phillip Webb
   46 |  * @author Madhura Bhave
   47 |  */
   48 | class AutoConfiguredHealthEndpointGroups implements HealthEndpointGroups, AdditionalPathsMapper {
   49 | 
   50 | 	private final HealthEndpointGroup primaryGroup;
   51 | 
   52 | 	private final Map<String, HealthEndpointGroup> groups;
   53 | 
   54 | 	/**
   55 | 	 * Create a new {@link AutoConfiguredHealthEndpointGroups} instance.
   56 | 	 * @param applicationContext the application context used to check for override beans
   57 | 	 * @param properties the health endpoint properties
   58 | 	 */
   59 | 	AutoConfiguredHealthEndpointGroups(ApplicationContext applicationContext, HealthEndpointProperties properties) {
   60 | 		ListableBeanFactory beanFactory = (applicationContext instanceof ConfigurableApplicationContext configurableContext)
   61 | 				? configurableContext.getBeanFactory() : applicationContext;
   62 | 		Show showComponents = properties.getShowComponents();
   63 | 		Show showDetails = properties.getShowDetails();
   64 | 		Set<String> roles = properties.getRoles();
   65 | 		StatusAggregator statusAggregator = getNonQualifiedBean(beanFactory, StatusAggregator.class,
   66 | 				() -> StatusAggregator.of(properties.getStatus().getOrder()));
   67 | 		HttpCodeStatusMapper statusMapper = getNonQualifiedBean(beanFactory, HttpCodeStatusMapper.class,
   68 | 				() -> HttpCodeStatusMapper.of(properties.getStatus().getHttpMapping()));
   69 | 		this.primaryGroup = new AutoConfiguredHealthEndpointGroup(HealthContributorMembership.always(),
   70 | 				statusAggregator, statusMapper, showComponents, showDetails, roles, null);
   71 | 		this.groups = createGroups(properties.getGroup(), beanFactory, statusAggregator, statusMapper, showComponents,
   72 | 				showDetails, roles);
   73 | 	}
   74 | 
   75 | 	private Map<String, HealthEndpointGroup> createGroups(Map<String, Group> groupProperties, BeanFactory beanFactory,
   76 | 			StatusAggregator defaultStatusAggregator, HttpCodeStatusMapper defaultStatusMapper,
   77 | 			@Nullable Show defaultShowComponents, Show defaultShowDetails, Set<String> defaultRoles) {
   78 | 		Map<String, HealthEndpointGroup> groups = new TreeMap<>();
   79 | 		groupProperties.forEach((groupName, group) -> {
   80 | 			Status status = group.getStatus();
   81 | 			Show showComponents = (group.getShowComponents() != null) ? group.getShowComponents()
   82 | 					: defaultShowComponents;
   83 | 			Show showDetails = (group.getShowDetails() != null) ? group.getShowDetails() : defaultShowDetails;
   84 | 			Set<String> roles = !CollectionUtils.isEmpty(group.getRoles()) ? group.getRoles() : defaultRoles;
   85 | 			StatusAggregator statusAggregator = getQualifiedBean(beanFactory, StatusAggregator.class, groupName,
   86 | 					() -> createStatusAggregator(status.getOrder(), defaultStatusAggregator));
   87 | 			HttpCodeStatusMapper statusMapper = getQualifiedBean(beanFactory, HttpCodeStatusMapper.class, groupName,
   88 | 					() -> createStatusMapper(status.getHttpMapping(), defaultStatusMapper));
   89 | 			HealthContributorMembership membership = HealthContributorMembership.byIncludeExclude(group.getInclude(),
   90 | 					group.getExclude());
   91 | 			AdditionalHealthEndpointPath additionalPath = (group.getAdditionalPath() != null)
   92 | 					? AdditionalHealthEndpointPath.from(group.getAdditionalPath()) : null;
   93 | 			groups.put(groupName, new AutoConfiguredHealthEndpointGroup(membership, statusAggregator, statusMapper,
   94 | 					showComponents, showDetails, roles, additionalPath));
   95 | 		});
   96 | 		return Collections.unmodifiableMap(groups);
   97 | 	}
   98 | 
   99 | 	private StatusAggregator createStatusAggregator(List<String> order, StatusAggregator defaultStatusAggregator) {
  100 | 		return (!CollectionUtils.isEmpty(order)) ? StatusAggregator.of(order) : defaultStatusAggregator;
  101 | 	}
  102 | 
  103 | 	private HttpCodeStatusMapper createStatusMapper(Map<String, Integer> mapping,
  104 | 			HttpCodeStatusMapper defaultStatusMapper) {
  105 | 		return (!CollectionUtils.isEmpty(mapping)) ? HttpCodeStatusMapper.of(mapping) : defaultStatusMapper;
  106 | 	}
  107 | 
  108 | 	private <T> T getNonQualifiedBean(ListableBeanFactory beanFactory, Class<T> type, Supplier<T> fallback) {
  109 | 		List<String> candidates = new ArrayList<>();
  110 | 		for (String beanName : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, type)) {
  111 | 			String[] aliases = beanFactory.getAliases(beanName);
  112 | 			if (!BeanFactoryAnnotationUtils.isQualifierMatch(
  113 | 					(qualifier) -> !qualifier.equals(beanName) && !ObjectUtils.containsElement(aliases, qualifier),
  114 | 					beanName, beanFactory)) {
  115 | 				candidates.add(beanName);
  116 | 			}
  117 | 		}
  118 | 		if (candidates.isEmpty()) {
  119 | 			return fallback.get();
  120 | 		}
  121 | 		if (candidates.size() == 1) {
  122 | 			return beanFactory.getBean(candidates.get(0), type);
  123 | 		}
  124 | 		return beanFactory.getBean(type);
  125 | 	}
  126 | 
  127 | 	private <T> T getQualifiedBean(BeanFactory beanFactory, Class<T> type, String qualifier, Supplier<T> fallback) {
  128 | 		try {
  129 | 			return BeanFactoryAnnotationUtils.qualifiedBeanOfType(beanFactory, type, qualifier);
  130 | 		}
  131 | 		catch (NoSuchBeanDefinitionException ex) {
  132 | 			return fallback.get();
  133 | 		}
  134 | 	}
  135 | 
  136 | 	@Override
  137 | 	public HealthEndpointGroup getPrimary() {
  138 | 		return this.primaryGroup;
  139 | 	}
  140 | 
  141 | 	@Override
  142 | 	public Set<String> getNames() {
  143 | 		return this.groups.keySet();
  144 | 	}
  145 | 
  146 | 	@Override
  147 | 	public @Nullable HealthEndpointGroup get(String name) {
  148 | 		return this.groups.get(name);
  149 | 	}
  150 | 
  151 | 	@Override
  152 | 	public @Nullable List<String> getAdditionalPaths(EndpointId endpointId, WebServerNamespace webServerNamespace) {
  153 | 		if (!HealthEndpoint.ID.equals(endpointId)) {
  154 | 			return null;
  155 | 		}
  156 | 		return streamAllGroups().map(HealthEndpointGroup::getAdditionalPath)
  157 | 			.filter(Objects::nonNull)
  158 | 			.filter((additionalPath) -> additionalPath.hasNamespace(webServerNamespace))
  159 | 			.map(AdditionalHealthEndpointPath::getValue)
  160 | 			.toList();
  161 | 	}
  162 | 
  163 | 	private Stream<HealthEndpointGroup> streamAllGroups() {
  164 | 		return Stream.concat(Stream.of(this.primaryGroup), this.groups.values().stream());
  165 | 	}
  166 | 
  167 | }
```
