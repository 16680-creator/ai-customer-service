<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-health:4.2.0 | path: org/springframework/boot/health/actuate/endpoint/HealthEndpointSupport.java -->
<!-- 正文引用：[[源码:HealthEndpointSupport.java#L起-L止]] ；代码块头部：(src: HealthEndpointSupport.java#L起-L止) -->

# HealthEndpointSupport.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-health:4.2.0` |
| 路径 | `org/springframework/boot/health/actuate/endpoint/HealthEndpointSupport.java` |
| 行数 | 219 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-health/4.2.0/spring-boot-health-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:02.235Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.health.actuate.endpoint;
    6 | 
    7 | import java.time.Duration;
    8 | import java.time.Instant;
    9 | import java.util.LinkedHashMap;
   10 | import java.util.Map;
   11 | import java.util.Set;
   12 | import java.util.TreeSet;
   13 | import java.util.stream.Collectors;
   14 | 
   15 | import org.apache.commons.logging.Log;
   16 | import org.apache.commons.logging.LogFactory;
   17 | import org.jspecify.annotations.Nullable;
   18 | 
   19 | import org.springframework.boot.actuate.endpoint.ApiVersion;
   20 | import org.springframework.boot.actuate.endpoint.SecurityContext;
   21 | import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
   22 | import org.springframework.boot.convert.DurationStyle;
   23 | import org.springframework.boot.health.contributor.Status;
   24 | import org.springframework.core.log.LogMessage;
   25 | import org.springframework.util.Assert;
   26 | import org.springframework.util.StringUtils;
   27 | 
   28 | /**
   29 |  * Base class for health endpoints and health endpoint extensions.
   30 |  *
   31 |  * @param <H> the health type
   32 |  * @param <D> the descriptor type
   33 |  * @author Phillip Webb
   34 |  * @author Scott Frederick
   35 |  * @author Wan bin yu
   36 |  */
   37 | abstract class HealthEndpointSupport<H, D> {
   38 | 
   39 | 	static final String[] EMPTY_PATH = {};
   40 | 
   41 | 	private static final Log logger = LogFactory.getLog(HealthEndpointSupport.class);
   42 | 
   43 | 	private final Contributor<H, D> rootContributor;
   44 | 
   45 | 	private final HealthEndpointGroups groups;
   46 | 
   47 | 	private final @Nullable Duration slowContributorLoggingThreshold;
   48 | 
   49 | 	/**
   50 | 	 * Create a new {@link HealthEndpointSupport} instance.
   51 | 	 * @param rootContributor the health contributor registry
   52 | 	 * @param groups the health endpoint groups
   53 | 	 * @param slowContributorLoggingThreshold duration after which slow health contributor
   54 | 	 * logging should occur
   55 | 	 */
   56 | 	HealthEndpointSupport(Contributor<H, D> rootContributor, HealthEndpointGroups groups,
   57 | 			@Nullable Duration slowContributorLoggingThreshold) {
   58 | 		Assert.notNull(rootContributor, "'rootContributor' must not be null");
   59 | 		Assert.notNull(groups, "'groups' must not be null");
   60 | 		this.rootContributor = rootContributor;
   61 | 		this.groups = groups;
   62 | 		this.slowContributorLoggingThreshold = slowContributorLoggingThreshold;
   63 | 	}
   64 | 
   65 | 	@Nullable Result<D> getResult(ApiVersion apiVersion, @Nullable WebServerNamespace serverNamespace,
   66 | 			SecurityContext securityContext, boolean showAll, String... path) {
   67 | 		GroupMatch groupMatch = (path.length > 0) ? getGroup(serverNamespace, path) : null;
   68 | 		if (groupMatch != null) {
   69 | 			return getResult(apiVersion, groupMatch.group(), securityContext, showAll, path, groupMatch.pathOffset());
   70 | 		}
   71 | 		return getResult(apiVersion, this.groups.getPrimary(), securityContext, showAll, path, 0);
   72 | 	}
   73 | 
   74 | 	private @Nullable GroupMatch getGroup(@Nullable WebServerNamespace serverNamespace, String... path) {
   75 | 		HealthEndpointGroup group = this.groups.get(path[0]);
   76 | 		if (group != null) {
   77 | 			return new GroupMatch(group, 1);
   78 | 		}
   79 | 		if (serverNamespace != null) {
   80 | 			StringBuilder additionalPath = new StringBuilder();
   81 | 			GroupMatch groupMatch = null;
   82 | 			for (int i = 0; i < path.length; i++) {
   83 | 				additionalPath.append((i != 0) ? "/" : "").append(path[i]);
   84 | 				group = this.groups.get(AdditionalHealthEndpointPath.of(serverNamespace, additionalPath.toString()));
   85 | 				if (group != null) {
   86 | 					groupMatch = new GroupMatch(group, i + 1);
   87 | 				}
   88 | 			}
   89 | 			return groupMatch;
   90 | 		}
   91 | 		return null;
   92 | 	}
   93 | 
   94 | 	private @Nullable Result<D> getResult(ApiVersion apiVersion, HealthEndpointGroup group,
   95 | 			SecurityContext securityContext, boolean showAll, String[] path, int pathOffset) {
   96 | 		boolean showComponents = showAll || group.showComponents(securityContext);
   97 | 		boolean showDetails = showAll || group.showDetails(securityContext);
   98 | 		boolean isSystemHealth = group == this.groups.getPrimary() && pathOffset == 0;
   99 | 		boolean isRoot = path.length - pathOffset == 0;
  100 | 		if (!showComponents && !isRoot) {
  101 | 			return null;
  102 | 		}
  103 | 		Contributor<H, D> contributor = getContributor(path, pathOffset);
  104 | 		if (contributor == null) {
  105 | 			return null;
  106 | 		}
  107 | 		String name = getName(path, pathOffset);
  108 | 		Set<String> groupNames = (!isSystemHealth) ? null : new TreeSet<>(this.groups.getNames());
  109 | 		D descriptor = getDescriptor(apiVersion, group, name, contributor, showComponents, showDetails, groupNames);
  110 | 		return (descriptor != null) ? new Result<>(descriptor, group) : null;
  111 | 	}
  112 | 
  113 | 	private @Nullable Contributor<H, D> getContributor(String[] path, int pathOffset) {
  114 | 		Contributor<H, D> contributor = this.rootContributor;
  115 | 		while (pathOffset < path.length) {
  116 | 			if (!contributor.isComposite()) {
  117 | 				return null;
  118 | 			}
  119 | 			Contributor<H, D> child = contributor.getChild(path[pathOffset]);
  120 | 			if (child == null) {
  121 | 				return null;
  122 | 			}
  123 | 			contributor = child;
  124 | 			pathOffset++;
  125 | 		}
  126 | 		return contributor;
  127 | 	}
  128 | 
  129 | 	private String getName(String[] path, int pathOffset) {
  130 | 		StringBuilder name = new StringBuilder();
  131 | 		while (pathOffset < path.length) {
  132 | 			name.append((!name.isEmpty()) ? "/" : "");
  133 | 			name.append(path[pathOffset]);
  134 | 			pathOffset++;
  135 | 		}
  136 | 		return name.toString();
  137 | 	}
  138 | 
  139 | 	private @Nullable D getDescriptor(ApiVersion apiVersion, HealthEndpointGroup group, String name,
  140 | 			Contributor<H, D> contributor, boolean showComponents, boolean showDetails,
  141 | 			@Nullable Set<String> groupNames) {
  142 | 		if (contributor.isComposite()) {
  143 | 			return getAggregateDescriptor(apiVersion, group, name, contributor, showComponents, showDetails,
  144 | 					groupNames);
  145 | 		}
  146 | 		if (name.isEmpty() || group.isMember(name)) {
  147 | 			return getDescriptorAndLogIfSlow(contributor, name, showDetails);
  148 | 		}
  149 | 		return null;
  150 | 	}
  151 | 
  152 | 	private @Nullable D getAggregateDescriptor(ApiVersion apiVersion, HealthEndpointGroup group, String name,
  153 | 			Contributor<H, D> contributor, boolean showComponents, boolean showDetails,
  154 | 			@Nullable Set<String> groupNames) {
  155 | 		String prefix = (StringUtils.hasText(name)) ? name + "/" : "";
  156 | 		Map<String, D> descriptors = new LinkedHashMap<>();
  157 | 		for (Contributor.Child<H, D> child : contributor) {
  158 | 			String childName = child.name();
  159 | 			D descriptor = getDescriptor(apiVersion, group, prefix + childName, child.contributor(), showComponents,
  160 | 					showDetails, null);
  161 | 			if (descriptor != null) {
  162 | 				descriptors.put(childName, descriptor);
  163 | 			}
  164 | 		}
  165 | 		if (descriptors.isEmpty()) {
  166 | 			return null;
  167 | 		}
  168 | 		return aggregateDescriptors(apiVersion, descriptors, group.getStatusAggregator(), showComponents, groupNames);
  169 | 	}
  170 | 
  171 | 	private @Nullable D getDescriptorAndLogIfSlow(Contributor<H, D> contributor, String name, boolean showDetails) {
  172 | 		Instant start = Instant.now();
  173 | 		try {
  174 | 			return contributor.getDescriptor(showDetails);
  175 | 		}
  176 | 		finally {
  177 | 			if (logger.isWarnEnabled() && this.slowContributorLoggingThreshold != null) {
  178 | 				Duration duration = Duration.between(start, Instant.now());
  179 | 				if (duration.compareTo(this.slowContributorLoggingThreshold) > 0) {
  180 | 					logger.warn(LogMessage.format("Health contributor %s took %s to respond",
  181 | 							contributor.getIdentifier(name), DurationStyle.SIMPLE.print(duration)));
  182 | 				}
  183 | 			}
  184 | 		}
  185 | 	}
  186 | 
  187 | 	abstract D aggregateDescriptors(ApiVersion apiVersion, Map<String, D> descriptors,
  188 | 			StatusAggregator statusAggregator, boolean showComponents, @Nullable Set<String> groupNames);
  189 | 
  190 | 	final CompositeHealthDescriptor getCompositeDescriptor(ApiVersion apiVersion,
  191 | 			Map<String, HealthDescriptor> descriptors, StatusAggregator statusAggregator, boolean showComponents,
  192 | 			@Nullable Set<String> groupNames) {
  193 | 		Status status = statusAggregator
  194 | 			.getAggregateStatus(descriptors.values().stream().map(this::getStatus).collect(Collectors.toSet()));
  195 | 		descriptors = (!showComponents) ? null : descriptors;
  196 | 		return (groupNames != null) ? new SystemHealthDescriptor(apiVersion, status, descriptors, groupNames)
  197 | 				: new CompositeHealthDescriptor(apiVersion, status, descriptors);
  198 | 	}
  199 | 
  200 | 	private Status getStatus(@Nullable HealthDescriptor component) {
  201 | 		return (component != null) ? component.getStatus() : Status.UNKNOWN;
  202 | 	}
  203 | 
  204 | 	/**
  205 | 	 * A health result containing descriptor and the group that created it.
  206 | 	 *
  207 | 	 * @param descriptor the health descriptor
  208 | 	 * @param group the group used to create the health
  209 | 	 * @param <D> the details type
  210 | 	 */
  211 | 	record Result<D>(D descriptor, HealthEndpointGroup group) {
  212 | 
  213 | 	}
  214 | 
  215 | 	private record GroupMatch(HealthEndpointGroup group, int pathOffset) {
  216 | 
  217 | 	}
  218 | 
  219 | }
```
