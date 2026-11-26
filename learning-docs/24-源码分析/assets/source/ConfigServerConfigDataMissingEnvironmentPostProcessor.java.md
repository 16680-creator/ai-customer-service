<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-config-client:5.1.0 | path: org/springframework/cloud/config/client/ConfigServerConfigDataMissingEnvironmentPostProcessor.java -->
<!-- 正文引用：[[源码:ConfigServerConfigDataMissingEnvironmentPostProcessor.java#L起-L止]] ；代码块头部：(src: ConfigServerConfigDataMissingEnvironmentPostProcessor.java#L起-L止) -->

# ConfigServerConfigDataMissingEnvironmentPostProcessor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-config-client:5.1.0` |
| 路径 | `org/springframework/cloud/config/client/ConfigServerConfigDataMissingEnvironmentPostProcessor.java` |
| 行数 | 83 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-config-client/5.1.0/spring-cloud-config-client-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:31:50.024Z |

```java
    1 | /*
    2 |  * Copyright 2015-present the original author or authors.
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
   17 | package org.springframework.cloud.config.client;
   18 | 
   19 | import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
   20 | import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
   21 | import org.springframework.boot.diagnostics.FailureAnalysis;
   22 | import org.springframework.cloud.commons.ConfigDataMissingEnvironmentPostProcessor;
   23 | import org.springframework.core.env.Environment;
   24 | 
   25 | import static org.springframework.cloud.config.client.ConfigServerConfigDataLocationResolver.PREFIX;
   26 | import static org.springframework.cloud.util.PropertyUtils.bootstrapEnabled;
   27 | import static org.springframework.cloud.util.PropertyUtils.useLegacyProcessing;
   28 | 
   29 | public class ConfigServerConfigDataMissingEnvironmentPostProcessor extends ConfigDataMissingEnvironmentPostProcessor {
   30 | 
   31 | 	/**
   32 | 	 * Order of post processor, set to run after
   33 | 	 * {@link ConfigDataEnvironmentPostProcessor}.
   34 | 	 */
   35 | 	public static final int ORDER = ConfigDataEnvironmentPostProcessor.ORDER + 1000;
   36 | 
   37 | 	@Override
   38 | 	public int getOrder() {
   39 | 		return ORDER;
   40 | 	}
   41 | 
   42 | 	@Override
   43 | 	protected String getPrefix() {
   44 | 		return PREFIX;
   45 | 	}
   46 | 
   47 | 	@Override
   48 | 	protected boolean shouldProcessEnvironment(Environment environment) {
   49 | 		// don't run if using bootstrap or legacy processing
   50 | 		if (bootstrapEnabled(environment) || useLegacyProcessing(environment)) {
   51 | 			return false;
   52 | 		}
   53 | 		boolean configEnabled = environment.getProperty(ConfigClientProperties.PREFIX + ".enabled", Boolean.class,
   54 | 				true);
   55 | 		boolean importCheckEnabled = environment.getProperty(ConfigClientProperties.PREFIX + ".import-check.enabled",
   56 | 				Boolean.class, true);
   57 | 		if (!configEnabled || !importCheckEnabled) {
   58 | 			return false;
   59 | 		}
   60 | 		return true;
   61 | 	}
   62 | 
   63 | 	static class ImportExceptionFailureAnalyzer extends AbstractFailureAnalyzer<ImportException> {
   64 | 
   65 | 		@Override
   66 | 		protected FailureAnalysis analyze(Throwable rootFailure, ImportException cause) {
   67 | 			String description;
   68 | 			if (cause.missingPrefix) {
   69 | 				description = "The spring.config.import property is missing a " + PREFIX + " entry";
   70 | 			}
   71 | 			else {
   72 | 				description = "No spring.config.import property has been defined";
   73 | 			}
   74 | 			String action = "Add a spring.config.import=configserver: property to your configuration.\n"
   75 | 					+ "\tIf configuration is not required add spring.config.import=optional:configserver: instead.\n"
   76 | 					+ "\tTo disable this check, set spring.cloud.config.enabled=false or \n"
   77 | 					+ "\tspring.cloud.config.import-check.enabled=false.";
   78 | 			return new FailureAnalysis(description, action, cause);
   79 | 		}
   80 | 
   81 | 	}
   82 | 
   83 | }
```
