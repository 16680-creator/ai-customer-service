<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-actuator:4.2.0 | path: org/springframework/boot/actuate/endpoint/web/PathMappedEndpoints.java -->
<!-- 正文引用：[[源码:PathMappedEndpoints.java#L起-L止]] ；代码块头部：(src: PathMappedEndpoints.java#L起-L止) -->

# PathMappedEndpoints.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-actuator:4.2.0` |
| 路径 | `org/springframework/boot/actuate/endpoint/web/PathMappedEndpoints.java` |
| 行数 | 174 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-actuator/4.2.0/spring-boot-actuator-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:01.788Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.actuate.endpoint.web;
    6 | 
    7 | import java.util.Collection;
    8 | import java.util.Collections;
    9 | import java.util.Iterator;
   10 | import java.util.LinkedHashMap;
   11 | import java.util.List;
   12 | import java.util.Map;
   13 | import java.util.stream.Stream;
   14 | 
   15 | import org.jspecify.annotations.Nullable;
   16 | 
   17 | import org.springframework.boot.actuate.endpoint.EndpointId;
   18 | import org.springframework.boot.actuate.endpoint.EndpointsSupplier;
   19 | import org.springframework.lang.Contract;
   20 | import org.springframework.util.Assert;
   21 | import org.springframework.util.CollectionUtils;
   22 | 
   23 | /**
   24 |  * A collection of {@link PathMappedEndpoint path mapped endpoints}.
   25 |  *
   26 |  * @author Phillip Webb
   27 |  * @since 2.0.0
   28 |  */
   29 | public class PathMappedEndpoints implements Iterable<PathMappedEndpoint> {
   30 | 
   31 | 	private final String basePath;
   32 | 
   33 | 	private final Map<EndpointId, PathMappedEndpoint> endpoints;
   34 | 
   35 | 	/**
   36 | 	 * Create a new {@link PathMappedEndpoints} instance for the given supplier.
   37 | 	 * @param basePath the base path of the endpoints
   38 | 	 * @param supplier the endpoint supplier
   39 | 	 */
   40 | 	public PathMappedEndpoints(@Nullable String basePath, EndpointsSupplier<?> supplier) {
   41 | 		Assert.notNull(supplier, "'supplier' must not be null");
   42 | 		this.basePath = (basePath != null) ? basePath : "";
   43 | 		this.endpoints = getEndpoints(Collections.singleton(supplier));
   44 | 	}
   45 | 
   46 | 	/**
   47 | 	 * Create a new {@link PathMappedEndpoints} instance for the given suppliers.
   48 | 	 * @param basePath the base path of the endpoints
   49 | 	 * @param suppliers the endpoint suppliers
   50 | 	 */
   51 | 	public PathMappedEndpoints(@Nullable String basePath, Collection<EndpointsSupplier<?>> suppliers) {
   52 | 		Assert.notNull(suppliers, "'suppliers' must not be null");
   53 | 		this.basePath = (basePath != null) ? basePath : "";
   54 | 		this.endpoints = getEndpoints(suppliers);
   55 | 	}
   56 | 
   57 | 	private Map<EndpointId, PathMappedEndpoint> getEndpoints(Collection<EndpointsSupplier<?>> suppliers) {
   58 | 		Map<EndpointId, PathMappedEndpoint> endpoints = new LinkedHashMap<>();
   59 | 		suppliers.forEach((supplier) -> supplier.getEndpoints().forEach((endpoint) -> {
   60 | 			if (endpoint instanceof PathMappedEndpoint pathMappedEndpoint) {
   61 | 				endpoints.put(endpoint.getEndpointId(), pathMappedEndpoint);
   62 | 			}
   63 | 		}));
   64 | 		return Collections.unmodifiableMap(endpoints);
   65 | 	}
   66 | 
   67 | 	/**
   68 | 	 * Return the base path for the endpoints.
   69 | 	 * @return the base path
   70 | 	 */
   71 | 	public String getBasePath() {
   72 | 		return this.basePath;
   73 | 	}
   74 | 
   75 | 	/**
   76 | 	 * Return the root path for the endpoint with the given ID or {@code null} if the
   77 | 	 * endpoint cannot be found.
   78 | 	 * @param endpointId the endpoint ID
   79 | 	 * @return the root path or {@code null}
   80 | 	 */
   81 | 	public @Nullable String getRootPath(EndpointId endpointId) {
   82 | 		PathMappedEndpoint endpoint = getEndpoint(endpointId);
   83 | 		return (endpoint != null) ? endpoint.getRootPath() : null;
   84 | 	}
   85 | 
   86 | 	/**
   87 | 	 * Return the full path for the endpoint with the given ID or {@code null} if the
   88 | 	 * endpoint cannot be found.
   89 | 	 * @param endpointId the endpoint ID
   90 | 	 * @return the full path or {@code null}
   91 | 	 */
   92 | 	public @Nullable String getPath(EndpointId endpointId) {
   93 | 		return getPath(getEndpoint(endpointId));
   94 | 	}
   95 | 
   96 | 	/**
   97 | 	 * Return the root paths for each mapped endpoint (excluding additional paths).
   98 | 	 * @return all root paths
   99 | 	 */
  100 | 	public Collection<String> getAllRootPaths() {
  101 | 		return stream().map(PathMappedEndpoint::getRootPath).toList();
  102 | 	}
  103 | 
  104 | 	/**
  105 | 	 * Return the full paths for each mapped endpoint (excluding additional paths).
  106 | 	 * @return all root paths
  107 | 	 */
  108 | 	public Collection<String> getAllPaths() {
  109 | 		return stream().map(this::getPath).toList();
  110 | 	}
  111 | 
  112 | 	/**
  113 | 	 * Return the additional paths for each mapped endpoint.
  114 | 	 * @param webServerNamespace the web server namespace
  115 | 	 * @param endpointId the endpoint ID
  116 | 	 * @return all additional paths
  117 | 	 * @since 3.4.0
  118 | 	 */
  119 | 	public Collection<String> getAdditionalPaths(WebServerNamespace webServerNamespace, EndpointId endpointId) {
  120 | 		return getAdditionalPaths(webServerNamespace, getEndpoint(endpointId)).toList();
  121 | 	}
  122 | 
  123 | 	private Stream<String> getAdditionalPaths(WebServerNamespace webServerNamespace,
  124 | 			@Nullable PathMappedEndpoint endpoint) {
  125 | 		List<String> additionalPaths = (endpoint != null) ? endpoint.getAdditionalPaths(webServerNamespace) : null;
  126 | 		if (CollectionUtils.isEmpty(additionalPaths)) {
  127 | 			return Stream.empty();
  128 | 		}
  129 | 		return additionalPaths.stream().map(this::getAdditionalPath);
  130 | 	}
  131 | 
  132 | 	private String getAdditionalPath(String path) {
  133 | 		return path.startsWith("/") ? path : "/" + path;
  134 | 	}
  135 | 
  136 | 	/**
  137 | 	 * Return the {@link PathMappedEndpoint} with the given ID or {@code null} if the
  138 | 	 * endpoint cannot be found.
  139 | 	 * @param endpointId the endpoint ID
  140 | 	 * @return the path mapped endpoint or {@code null}
  141 | 	 */
  142 | 	public @Nullable PathMappedEndpoint getEndpoint(EndpointId endpointId) {
  143 | 		return this.endpoints.get(endpointId);
  144 | 	}
  145 | 
  146 | 	/**
  147 | 	 * Stream all {@link PathMappedEndpoint path mapped endpoints}.
  148 | 	 * @return a stream of endpoints
  149 | 	 */
  150 | 	public Stream<PathMappedEndpoint> stream() {
  151 | 		return this.endpoints.values().stream();
  152 | 	}
  153 | 
  154 | 	@Override
  155 | 	public Iterator<PathMappedEndpoint> iterator() {
  156 | 		return this.endpoints.values().iterator();
  157 | 	}
  158 | 
  159 | 	@Contract("!null -> !null")
  160 | 	private @Nullable String getPath(@Nullable PathMappedEndpoint endpoint) {
  161 | 		if (endpoint == null) {
  162 | 			return null;
  163 | 		}
  164 | 		StringBuilder path = new StringBuilder(this.basePath);
  165 | 		if (!this.basePath.equals("/")) {
  166 | 			path.append("/");
  167 | 		}
  168 | 		if (!endpoint.getRootPath().equals("/")) {
  169 | 			path.append(endpoint.getRootPath());
  170 | 		}
  171 | 		return path.toString();
  172 | 	}
  173 | 
  174 | }
```
