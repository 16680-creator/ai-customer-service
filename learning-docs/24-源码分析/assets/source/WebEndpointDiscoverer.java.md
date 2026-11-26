<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-actuator:4.2.0 | path: org/springframework/boot/actuate/endpoint/web/annotation/WebEndpointDiscoverer.java -->
<!-- 正文引用：[[源码:WebEndpointDiscoverer.java#L起-L止]] ；代码块头部：(src: WebEndpointDiscoverer.java#L起-L止) -->

# WebEndpointDiscoverer.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-actuator:4.2.0` |
| 路径 | `org/springframework/boot/actuate/endpoint/web/annotation/WebEndpointDiscoverer.java` |
| 行数 | 109 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-actuator/4.2.0/spring-boot-actuator-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:01.679Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.actuate.endpoint.web.annotation;
    6 | 
    7 | import java.util.Collection;
    8 | import java.util.Collections;
    9 | import java.util.List;
   10 | 
   11 | import org.jspecify.annotations.Nullable;
   12 | 
   13 | import org.springframework.aot.hint.MemberCategory;
   14 | import org.springframework.aot.hint.RuntimeHints;
   15 | import org.springframework.aot.hint.RuntimeHintsRegistrar;
   16 | import org.springframework.boot.actuate.endpoint.Access;
   17 | import org.springframework.boot.actuate.endpoint.EndpointFilter;
   18 | import org.springframework.boot.actuate.endpoint.EndpointId;
   19 | import org.springframework.boot.actuate.endpoint.OperationFilter;
   20 | import org.springframework.boot.actuate.endpoint.annotation.DiscoveredOperationMethod;
   21 | import org.springframework.boot.actuate.endpoint.annotation.EndpointDiscoverer;
   22 | import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
   23 | import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
   24 | import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
   25 | import org.springframework.boot.actuate.endpoint.web.AdditionalPathsMapper;
   26 | import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
   27 | import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
   28 | import org.springframework.boot.actuate.endpoint.web.PathMapper;
   29 | import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
   30 | import org.springframework.boot.actuate.endpoint.web.WebOperation;
   31 | import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
   32 | import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpointDiscoverer.WebEndpointDiscovererRuntimeHints;
   33 | import org.springframework.context.ApplicationContext;
   34 | import org.springframework.context.annotation.ImportRuntimeHints;
   35 | 
   36 | /**
   37 |  * {@link EndpointDiscoverer} for {@link ExposableWebEndpoint web endpoints}.
   38 |  *
   39 |  * @author Phillip Webb
   40 |  * @since 2.0.0
   41 |  */
   42 | @ImportRuntimeHints(WebEndpointDiscovererRuntimeHints.class)
   43 | public class WebEndpointDiscoverer extends EndpointDiscoverer<ExposableWebEndpoint, WebOperation>
   44 | 		implements WebEndpointsSupplier {
   45 | 
   46 | 	private final List<PathMapper> endpointPathMappers;
   47 | 
   48 | 	private final List<AdditionalPathsMapper> additionalPathsMappers;
   49 | 
   50 | 	private final RequestPredicateFactory requestPredicateFactory;
   51 | 
   52 | 	/**
   53 | 	 * Create a new {@link WebEndpointDiscoverer} instance.
   54 | 	 * @param applicationContext the source application context
   55 | 	 * @param parameterValueMapper the parameter value mapper
   56 | 	 * @param endpointMediaTypes the endpoint media types
   57 | 	 * @param endpointPathMappers the endpoint path mappers
   58 | 	 * @param additionalPathsMappers the
   59 | 	 * @param invokerAdvisors invoker advisors to apply
   60 | 	 * @param endpointFilters endpoint filters to apply
   61 | 	 * @param operationFilters operation filters to apply
   62 | 	 * @since 3.4.0
   63 | 	 */
   64 | 	public WebEndpointDiscoverer(ApplicationContext applicationContext, ParameterValueMapper parameterValueMapper,
   65 | 			EndpointMediaTypes endpointMediaTypes, @Nullable List<PathMapper> endpointPathMappers,
   66 | 			@Nullable List<AdditionalPathsMapper> additionalPathsMappers,
   67 | 			Collection<OperationInvokerAdvisor> invokerAdvisors,
   68 | 			Collection<EndpointFilter<ExposableWebEndpoint>> endpointFilters,
   69 | 			Collection<OperationFilter<WebOperation>> operationFilters) {
   70 | 		super(applicationContext, parameterValueMapper, invokerAdvisors, endpointFilters, operationFilters);
   71 | 		this.endpointPathMappers = (endpointPathMappers != null) ? endpointPathMappers : Collections.emptyList();
   72 | 		this.additionalPathsMappers = (additionalPathsMappers != null) ? additionalPathsMappers
   73 | 				: Collections.emptyList();
   74 | 		this.requestPredicateFactory = new RequestPredicateFactory(endpointMediaTypes);
   75 | 	}
   76 | 
   77 | 	@Override
   78 | 	protected ExposableWebEndpoint createEndpoint(Object endpointBean, EndpointId id, Access defaultAccess,
   79 | 			Collection<WebOperation> operations) {
   80 | 		String rootPath = PathMapper.getRootPath(this.endpointPathMappers, id);
   81 | 		return new DiscoveredWebEndpoint(this, endpointBean, id, rootPath, defaultAccess, operations,
   82 | 				this.additionalPathsMappers);
   83 | 	}
   84 | 
   85 | 	@Override
   86 | 	protected WebOperation createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
   87 | 			OperationInvoker invoker) {
   88 | 		String rootPath = PathMapper.getRootPath(this.endpointPathMappers, endpointId);
   89 | 		WebOperationRequestPredicate requestPredicate = this.requestPredicateFactory.getRequestPredicate(rootPath,
   90 | 				operationMethod);
   91 | 		return new DiscoveredWebOperation(endpointId, operationMethod, invoker, requestPredicate);
   92 | 	}
   93 | 
   94 | 	@Override
   95 | 	protected OperationKey createOperationKey(WebOperation operation) {
   96 | 		return new OperationKey(operation.getRequestPredicate(),
   97 | 				() -> "web request predicate " + operation.getRequestPredicate());
   98 | 	}
   99 | 
  100 | 	static class WebEndpointDiscovererRuntimeHints implements RuntimeHintsRegistrar {
  101 | 
  102 | 		@Override
  103 | 		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
  104 | 			hints.reflection().registerType(WebEndpointFilter.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
  105 | 		}
  106 | 
  107 | 	}
  108 | 
  109 | }
```
