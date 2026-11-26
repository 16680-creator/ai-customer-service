<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-health:4.2.0 | path: org/springframework/boot/health/actuate/endpoint/HealthEndpointWebExtension.java -->
<!-- 正文引用：[[源码:HealthEndpointWebExtension.java#L起-L止]] ；代码块头部：(src: HealthEndpointWebExtension.java#L起-L止) -->

# HealthEndpointWebExtension.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-health:4.2.0` |
| 路径 | `org/springframework/boot/health/actuate/endpoint/HealthEndpointWebExtension.java` |
| 行数 | 91 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-health/4.2.0/spring-boot-health-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:02.392Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.health.actuate.endpoint;
    6 | 
    7 | import java.time.Duration;
    8 | import java.util.Arrays;
    9 | import java.util.Map;
   10 | import java.util.Set;
   11 | 
   12 | import org.jspecify.annotations.Nullable;
   13 | 
   14 | import org.springframework.boot.actuate.endpoint.ApiVersion;
   15 | import org.springframework.boot.actuate.endpoint.SecurityContext;
   16 | import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
   17 | import org.springframework.boot.actuate.endpoint.annotation.Selector;
   18 | import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
   19 | import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
   20 | import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
   21 | import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
   22 | import org.springframework.boot.health.contributor.Health;
   23 | import org.springframework.boot.health.registry.HealthContributorRegistry;
   24 | import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;
   25 | import org.springframework.context.annotation.ImportRuntimeHints;
   26 | 
   27 | /**
   28 |  * {@link EndpointWebExtension @EndpointWebExtension} for the {@link HealthEndpoint}.
   29 |  *
   30 |  * @author Christian Dupuis
   31 |  * @author Dave Syer
   32 |  * @author Andy Wilkinson
   33 |  * @author Phillip Webb
   34 |  * @author Eddú Meléndez
   35 |  * @author Madhura Bhave
   36 |  * @author Stephane Nicoll
   37 |  * @author Scott Frederick
   38 |  * @since 4.0.0
   39 |  */
   40 | @EndpointWebExtension(endpoint = HealthEndpoint.class)
   41 | @ImportRuntimeHints(HealthEndpointWebExtensionRuntimeHints.class)
   42 | public class HealthEndpointWebExtension extends HealthEndpointSupport<Health, HealthDescriptor> {
   43 | 
   44 | 	/**
   45 | 	 * Create a new {@link HealthEndpointWebExtension} instance.
   46 | 	 * @param registry the health contributor registry
   47 | 	 * @param fallbackRegistry the fallback registry or {@code null}
   48 | 	 * @param groups the health endpoint groups
   49 | 	 * @param slowContributorLoggingThreshold duration after which slow health indicator
   50 | 	 * logging should occur
   51 | 	 */
   52 | 	public HealthEndpointWebExtension(HealthContributorRegistry registry,
   53 | 			@Nullable ReactiveHealthContributorRegistry fallbackRegistry, HealthEndpointGroups groups,
   54 | 			@Nullable Duration slowContributorLoggingThreshold) {
   55 | 		super(Contributor.blocking(registry, fallbackRegistry), groups, slowContributorLoggingThreshold);
   56 | 	}
   57 | 
   58 | 	@ReadOperation
   59 | 	public WebEndpointResponse<HealthDescriptor> health(ApiVersion apiVersion, WebServerNamespace serverNamespace,
   60 | 			SecurityContext securityContext) {
   61 | 		return health(apiVersion, serverNamespace, securityContext, false, EMPTY_PATH);
   62 | 	}
   63 | 
   64 | 	@ReadOperation
   65 | 	public WebEndpointResponse<HealthDescriptor> health(ApiVersion apiVersion, WebServerNamespace serverNamespace,
   66 | 			SecurityContext securityContext, @Selector(match = Match.ALL_REMAINING) String... path) {
   67 | 		return health(apiVersion, serverNamespace, securityContext, false, path);
   68 | 	}
   69 | 
   70 | 	public WebEndpointResponse<HealthDescriptor> health(ApiVersion apiVersion,
   71 | 			@Nullable WebServerNamespace serverNamespace, SecurityContext securityContext, boolean showAll,
   72 | 			String... path) {
   73 | 		Result<HealthDescriptor> result = getResult(apiVersion, serverNamespace, securityContext, showAll, path);
   74 | 		if (result == null) {
   75 | 			return (Arrays.equals(path, EMPTY_PATH))
   76 | 					? new WebEndpointResponse<>(IndicatedHealthDescriptor.UP, WebEndpointResponse.STATUS_OK)
   77 | 					: new WebEndpointResponse<>(WebEndpointResponse.STATUS_NOT_FOUND);
   78 | 		}
   79 | 		HealthDescriptor descriptor = result.descriptor();
   80 | 		HealthEndpointGroup group = result.group();
   81 | 		int statusCode = group.getHttpCodeStatusMapper().getStatusCode(descriptor.getStatus());
   82 | 		return new WebEndpointResponse<>(descriptor, statusCode);
   83 | 	}
   84 | 
   85 | 	@Override
   86 | 	protected HealthDescriptor aggregateDescriptors(ApiVersion apiVersion, Map<String, HealthDescriptor> contributions,
   87 | 			StatusAggregator statusAggregator, boolean showComponents, @Nullable Set<String> groupNames) {
   88 | 		return getCompositeDescriptor(apiVersion, contributions, statusAggregator, showComponents, groupNames);
   89 | 	}
   90 | 
   91 | }
```
