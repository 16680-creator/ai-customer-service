<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-health:4.2.0 | path: org/springframework/boot/health/actuate/endpoint/HealthEndpoint.java -->
<!-- 正文引用：[[源码:HealthEndpoint.java#L起-L止]] ；代码块头部：(src: HealthEndpoint.java#L起-L止) -->

# HealthEndpoint.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-health:4.2.0` |
| 路径 | `org/springframework/boot/health/actuate/endpoint/HealthEndpoint.java` |
| 行数 | 78 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-health/4.2.0/spring-boot-health-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:02.115Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.health.actuate.endpoint;
    6 | 
    7 | import java.time.Duration;
    8 | import java.util.Map;
    9 | import java.util.Set;
   10 | 
   11 | import org.jspecify.annotations.Nullable;
   12 | 
   13 | import org.springframework.boot.actuate.endpoint.ApiVersion;
   14 | import org.springframework.boot.actuate.endpoint.EndpointId;
   15 | import org.springframework.boot.actuate.endpoint.SecurityContext;
   16 | import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
   17 | import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
   18 | import org.springframework.boot.actuate.endpoint.annotation.Selector;
   19 | import org.springframework.boot.actuate.endpoint.annotation.Selector.Match;
   20 | import org.springframework.boot.health.contributor.Health;
   21 | import org.springframework.boot.health.registry.HealthContributorRegistry;
   22 | import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;
   23 | 
   24 | /**
   25 |  * {@link Endpoint @Endpoint} to expose application health information.
   26 |  *
   27 |  * @author Dave Syer
   28 |  * @author Christian Dupuis
   29 |  * @author Andy Wilkinson
   30 |  * @author Stephane Nicoll
   31 |  * @author Scott Frederick
   32 |  * @since 4.0.0
   33 |  */
   34 | @Endpoint(id = "health")
   35 | public class HealthEndpoint extends HealthEndpointSupport<Health, HealthDescriptor> {
   36 | 
   37 | 	/**
   38 | 	 * Health endpoint id.
   39 | 	 */
   40 | 	public static final EndpointId ID = EndpointId.of("health");
   41 | 
   42 | 	/**
   43 | 	 * Create a new {@link HealthEndpoint} instance.
   44 | 	 * @param registry the health contributor registry
   45 | 	 * @param fallbackRegistry the fallback registry or {@code null}
   46 | 	 * @param groups the health endpoint groups
   47 | 	 * @param slowContributorLoggingThreshold duration after which slow health indicator
   48 | 	 * logging should occur
   49 | 	 */
   50 | 	public HealthEndpoint(HealthContributorRegistry registry,
   51 | 			@Nullable ReactiveHealthContributorRegistry fallbackRegistry, HealthEndpointGroups groups,
   52 | 			@Nullable Duration slowContributorLoggingThreshold) {
   53 | 		super(Contributor.blocking(registry, fallbackRegistry), groups, slowContributorLoggingThreshold);
   54 | 	}
   55 | 
   56 | 	@ReadOperation
   57 | 	public HealthDescriptor health() {
   58 | 		HealthDescriptor health = health(ApiVersion.V3, EMPTY_PATH);
   59 | 		return (health != null) ? health : IndicatedHealthDescriptor.UP;
   60 | 	}
   61 | 
   62 | 	@ReadOperation
   63 | 	public @Nullable HealthDescriptor healthForPath(@Selector(match = Match.ALL_REMAINING) String... path) {
   64 | 		return health(ApiVersion.V3, path);
   65 | 	}
   66 | 
   67 | 	private @Nullable HealthDescriptor health(ApiVersion apiVersion, String... path) {
   68 | 		Result<HealthDescriptor> result = getResult(apiVersion, null, SecurityContext.NONE, true, path);
   69 | 		return (result != null) ? result.descriptor() : null;
   70 | 	}
   71 | 
   72 | 	@Override
   73 | 	protected HealthDescriptor aggregateDescriptors(ApiVersion apiVersion, Map<String, HealthDescriptor> contributions,
   74 | 			StatusAggregator statusAggregator, boolean showComponents, @Nullable Set<String> groupNames) {
   75 | 		return getCompositeDescriptor(apiVersion, contributions, statusAggregator, showComponents, groupNames);
   76 | 	}
   77 | 
   78 | }
```
