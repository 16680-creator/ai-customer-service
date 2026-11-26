<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-context:5.1.0 | path: org/springframework/cloud/context/refresh/ConfigDataContextRefresher.java -->
<!-- 正文引用：[[源码:ConfigDataContextRefresher.java#L起-L止]] ；代码块头部：(src: ConfigDataContextRefresher.java#L起-L止) -->

# ConfigDataContextRefresher.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-context:5.1.0` |
| 路径 | `org/springframework/cloud/context/refresh/ConfigDataContextRefresher.java` |
| 行数 | 140 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-context/5.1.0/spring-cloud-context-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:31:38.037Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *      https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | 
   17 | package org.springframework.cloud.context.refresh;
   18 | 
   19 | import java.util.List;
   20 | import java.util.function.Supplier;
   21 | 
   22 | import org.apache.commons.logging.Log;
   23 | import org.apache.commons.logging.LogFactory;
   24 | 
   25 | import org.springframework.boot.EnvironmentPostProcessor;
   26 | import org.springframework.boot.SpringApplication;
   27 | import org.springframework.boot.bootstrap.BootstrapContext;
   28 | import org.springframework.boot.bootstrap.BootstrapRegistry;
   29 | import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
   30 | import org.springframework.boot.bootstrap.DefaultBootstrapContext;
   31 | import org.springframework.boot.logging.DeferredLogFactory;
   32 | import org.springframework.boot.util.Instantiator;
   33 | import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
   34 | import org.springframework.cloud.context.config.ContextRefreshedWithApplicationEvent;
   35 | import org.springframework.cloud.context.scope.refresh.RefreshScope;
   36 | import org.springframework.context.ApplicationListener;
   37 | import org.springframework.context.ConfigurableApplicationContext;
   38 | import org.springframework.core.env.MutablePropertySources;
   39 | import org.springframework.core.env.PropertySource;
   40 | import org.springframework.core.env.StandardEnvironment;
   41 | import org.springframework.core.io.support.SpringFactoriesLoader;
   42 | 
   43 | /**
   44 |  * @author Dave Syer
   45 |  * @author Venil Noronha
   46 |  */
   47 | public class ConfigDataContextRefresher extends ContextRefresher
   48 | 		implements ApplicationListener<ContextRefreshedWithApplicationEvent> {
   49 | 
   50 | 	private SpringApplication application;
   51 | 
   52 | 	@Deprecated
   53 | 	public ConfigDataContextRefresher(ConfigurableApplicationContext context, RefreshScope scope) {
   54 | 		super(context, scope);
   55 | 	}
   56 | 
   57 | 	public ConfigDataContextRefresher(ConfigurableApplicationContext context, RefreshScope scope,
   58 | 			RefreshAutoConfiguration.RefreshProperties properties) {
   59 | 		super(context, scope, properties);
   60 | 	}
   61 | 
   62 | 	@Override
   63 | 	public void onApplicationEvent(ContextRefreshedWithApplicationEvent event) {
   64 | 		application = event.getSpringApplication();
   65 | 	}
   66 | 
   67 | 	@Override
   68 | 	protected void updateEnvironment() {
   69 | 		if (logger.isTraceEnabled()) {
   70 | 			logger.trace("Re-processing environment to add config data");
   71 | 		}
   72 | 		StandardEnvironment environment = copyEnvironment(getContext().getEnvironment());
   73 | 		ConfigurableBootstrapContext bootstrapContext = getContext().getBeanProvider(ConfigurableBootstrapContext.class)
   74 | 			.getIfAvailable(DefaultBootstrapContext::new);
   75 | 
   76 | 		// run thru all EnvironmentPostProcessor instances. This lets things like vcap and
   77 | 		// decrypt happen after refresh. The hard coded call to
   78 | 		// ConfigDataEnvironmentPostProcessor.applyTo() is now automated as well.
   79 | 		DeferredLogFactory logFactory = new PassthruDeferredLogFactory();
   80 | 		List<String> classNames = SpringFactoriesLoader.loadFactoryNames(EnvironmentPostProcessor.class,
   81 | 				getClass().getClassLoader());
   82 | 		Instantiator<EnvironmentPostProcessor> instantiator = new Instantiator<>(EnvironmentPostProcessor.class,
   83 | 				(parameters) -> {
   84 | 					parameters.add(DeferredLogFactory.class, logFactory);
   85 | 					parameters.add(Log.class, logFactory::getLog);
   86 | 					parameters.add(ConfigurableBootstrapContext.class, bootstrapContext);
   87 | 					parameters.add(BootstrapContext.class, bootstrapContext);
   88 | 					parameters.add(BootstrapRegistry.class, bootstrapContext);
   89 | 				});
   90 | 		List<EnvironmentPostProcessor> postProcessors = instantiator.instantiate(classNames);
   91 | 		for (EnvironmentPostProcessor postProcessor : postProcessors) {
   92 | 			postProcessor.postProcessEnvironment(environment, application);
   93 | 		}
   94 | 
   95 | 		MutablePropertySources target = getContext().getEnvironment().getPropertySources();
   96 | 		String targetName = null;
   97 | 		for (PropertySource<?> source : environment.getPropertySources()) {
   98 | 			String name = source.getName();
   99 | 			if (target.contains(name)) {
  100 | 				targetName = name;
  101 | 			}
  102 | 			if (!this.standardSources.contains(name)) {
  103 | 				if (target.contains(name)) {
  104 | 					target.replace(name, source);
  105 | 				}
  106 | 				else {
  107 | 					if (targetName != null) {
  108 | 						target.addAfter(targetName, source);
  109 | 					}
  110 | 					else {
  111 | 						// targetName was null so we are at the start of the list
  112 | 						target.addFirst(source);
  113 | 					}
  114 | 					// update targetName to preserve ordering
  115 | 					targetName = name;
  116 | 				}
  117 | 			}
  118 | 		}
  119 | 	}
  120 | 
  121 | 	static class PassthruDeferredLogFactory implements DeferredLogFactory {
  122 | 
  123 | 		@Override
  124 | 		public Log getLog(Supplier<Log> destination) {
  125 | 			return destination.get();
  126 | 		}
  127 | 
  128 | 		@Override
  129 | 		public Log getLog(Class<?> destination) {
  130 | 			return getLog(() -> LogFactory.getLog(destination));
  131 | 		}
  132 | 
  133 | 		@Override
  134 | 		public Log getLog(Log destination) {
  135 | 			return getLog(() -> destination);
  136 | 		}
  137 | 
  138 | 	}
  139 | 
  140 | }
```
