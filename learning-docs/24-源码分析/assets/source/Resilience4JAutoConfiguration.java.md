<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-circuitbreaker-resilience4j:5.1.0 | path: org/springframework/cloud/circuitbreaker/resilience4j/Resilience4JAutoConfiguration.java -->
<!-- 正文引用：[[源码:Resilience4JAutoConfiguration.java#L起-L止]] ；代码块头部：(src: Resilience4JAutoConfiguration.java#L起-L止) -->

# Resilience4JAutoConfiguration.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-circuitbreaker-resilience4j:5.1.0` |
| 路径 | `org/springframework/cloud/circuitbreaker/resilience4j/Resilience4JAutoConfiguration.java` |
| 行数 | 182 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-circuitbreaker-resilience4j/5.1.0/spring-cloud-circuitbreaker-resilience4j-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:33:29.281Z |

```java
    1 | /*
    2 |  * Copyright 2013-present the original author or authors.
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
   17 | package org.springframework.cloud.circuitbreaker.resilience4j;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | 
   22 | import io.github.resilience4j.bulkhead.Bulkhead;
   23 | import io.github.resilience4j.bulkhead.BulkheadRegistry;
   24 | import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
   25 | import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
   26 | import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
   27 | import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
   28 | import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetricsPublisher;
   29 | import io.github.resilience4j.micrometer.tagged.TaggedThreadPoolBulkheadMetrics;
   30 | import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerMetricsAutoConfiguration;
   31 | import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
   32 | import io.micrometer.core.instrument.Meter;
   33 | import io.micrometer.core.instrument.MeterRegistry;
   34 | import io.micrometer.core.instrument.Tag;
   35 | import io.micrometer.core.instrument.config.MeterFilter;
   36 | import io.micrometer.observation.ObservationRegistry;
   37 | import jakarta.annotation.PostConstruct;
   38 | import org.jspecify.annotations.Nullable;
   39 | 
   40 | import org.springframework.beans.factory.annotation.Autowired;
   41 | import org.springframework.boot.autoconfigure.AutoConfigureAfter;
   42 | import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
   43 | import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
   44 | import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
   45 | import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
   46 | import org.springframework.boot.context.properties.EnableConfigurationProperties;
   47 | import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
   48 | import org.springframework.cloud.client.circuitbreaker.Customizer;
   49 | import org.springframework.context.annotation.Bean;
   50 | import org.springframework.context.annotation.Configuration;
   51 | 
   52 | /**
   53 |  * @author Ryan Baxter
   54 |  * @author Eric Bussieres
   55 |  * @author Andrii Bohutskyi
   56 |  */
   57 | @Configuration(proxyBeanMethods = false)
   58 | @EnableConfigurationProperties(Resilience4JConfigurationProperties.class)
   59 | @ConditionalOnProperty(name = { "spring.cloud.circuitbreaker.resilience4j.enabled",
   60 | 		"spring.cloud.circuitbreaker.resilience4j.blocking.enabled" }, matchIfMissing = true)
   61 | @AutoConfigureAfter(CircuitBreakerMetricsAutoConfiguration.class)
   62 | public class Resilience4JAutoConfiguration {
   63 | 
   64 | 	@Autowired(required = false)
   65 | 	private List<Customizer<Resilience4JCircuitBreakerFactory>> customizers = new ArrayList<>();
   66 | 
   67 | 	@Bean
   68 | 	@ConditionalOnMissingBean(CircuitBreakerFactory.class)
   69 | 	public Resilience4JCircuitBreakerFactory resilience4jCircuitBreakerFactory(
   70 | 			CircuitBreakerRegistry circuitBreakerRegistry, TimeLimiterRegistry timeLimiterRegistry,
   71 | 			@Autowired(required = false) @Nullable Resilience4jBulkheadProvider bulkheadProvider,
   72 | 			Resilience4JConfigurationProperties resilience4JConfigurationProperties) {
   73 | 		Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory(circuitBreakerRegistry,
   74 | 				timeLimiterRegistry, bulkheadProvider, resilience4JConfigurationProperties);
   75 | 		customizers.forEach(customizer -> customizer.customize(factory));
   76 | 		return factory;
   77 | 	}
   78 | 
   79 | 	@Configuration(proxyBeanMethods = false)
   80 | 	@ConditionalOnClass(Bulkhead.class)
   81 | 	@ConditionalOnProperty(value = "spring.cloud.circuitbreaker.bulkhead.resilience4j.enabled", matchIfMissing = true)
   82 | 	public static class Resilience4jBulkheadConfiguration {
   83 | 
   84 | 		@Autowired(required = false)
   85 | 		private List<Customizer<Resilience4jBulkheadProvider>> bulkheadCustomizers = new ArrayList<>();
   86 | 
   87 | 		@Bean
   88 | 		public Resilience4jBulkheadProvider bulkheadProvider(ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
   89 | 				BulkheadRegistry bulkheadRegistry,
   90 | 				Resilience4JConfigurationProperties resilience4JConfigurationProperties) {
   91 | 			Resilience4jBulkheadProvider resilience4jBulkheadProvider = new Resilience4jBulkheadProvider(
   92 | 					threadPoolBulkheadRegistry, bulkheadRegistry, resilience4JConfigurationProperties);
   93 | 			bulkheadCustomizers.forEach(customizer -> customizer.customize(resilience4jBulkheadProvider));
   94 | 			return resilience4jBulkheadProvider;
   95 | 		}
   96 | 
   97 | 	}
   98 | 
   99 | 	@Configuration(proxyBeanMethods = false)
  100 | 	@ConditionalOnBean({ MeterRegistry.class })
  101 | 	public static class MicrometerResilience4JGroupCustomizerConfiguration {
  102 | 
  103 | 		private static final String RESILIENCE4J_METER_PREFIX = "resilience4j";
  104 | 
  105 | 		@Bean
  106 | 		@ConditionalOnProperty(value = "spring.cloud.circuitbreaker.resilience4j.enableGroupMeterFilter",
  107 | 				havingValue = "true", matchIfMissing = true)
  108 | 		MeterFilter resilience4JMeterFilter(Resilience4JConfigurationProperties properties) {
  109 | 			return new MeterFilter() {
  110 | 				@Override
  111 | 				public Meter.Id map(Meter.Id id) {
  112 | 					if (id.getName().startsWith(RESILIENCE4J_METER_PREFIX)
  113 | 							&& id.getTag(Resilience4JCircuitBreaker.CIRCUIT_BREAKER_GROUP_TAG) == null) {
  114 | 						return id.withTag(Tag.of(Resilience4JCircuitBreaker.CIRCUIT_BREAKER_GROUP_TAG,
  115 | 								properties.getDefaultGroupTag()));
  116 | 					}
  117 | 					return id;
  118 | 				}
  119 | 			};
  120 | 		}
  121 | 
  122 | 	}
  123 | 
  124 | 	@Configuration(proxyBeanMethods = false)
  125 | 	@ConditionalOnBean({ MeterRegistry.class })
  126 | 	@ConditionalOnClass(name = { "io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics",
  127 | 			"io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetricsPublisher" })
  128 | 	@ConditionalOnMissingBean({ TaggedCircuitBreakerMetricsPublisher.class })
  129 | 	public static class MicrometerResilience4JCustomizerConfiguration {
  130 | 
  131 | 		@Autowired(required = false)
  132 | 		private @Nullable Resilience4JCircuitBreakerFactory factory;
  133 | 
  134 | 		@Autowired(required = false)
  135 | 		private @Nullable Resilience4jBulkheadProvider bulkheadProvider;
  136 | 
  137 | 		@Autowired(required = false)
  138 | 		private @Nullable TaggedCircuitBreakerMetrics taggedCircuitBreakerMetrics;
  139 | 
  140 | 		@Autowired
  141 | 		private MeterRegistry meterRegistry;
  142 | 
  143 | 		@PostConstruct
  144 | 		public void init() {
  145 | 			if (factory != null) {
  146 | 				if (taggedCircuitBreakerMetrics == null) {
  147 | 					taggedCircuitBreakerMetrics = TaggedCircuitBreakerMetrics
  148 | 						.ofCircuitBreakerRegistry(factory.getCircuitBreakerRegistry());
  149 | 				}
  150 | 				taggedCircuitBreakerMetrics.bindTo(meterRegistry);
  151 | 			}
  152 | 			if (bulkheadProvider != null) {
  153 | 				TaggedBulkheadMetrics.ofBulkheadRegistry(bulkheadProvider.getBulkheadRegistry()).bindTo(meterRegistry);
  154 | 				TaggedThreadPoolBulkheadMetrics
  155 | 					.ofThreadPoolBulkheadRegistry(bulkheadProvider.getThreadPoolBulkheadRegistry())
  156 | 					.bindTo(meterRegistry);
  157 | 			}
  158 | 		}
  159 | 
  160 | 	}
  161 | 
  162 | 	@Configuration(proxyBeanMethods = false)
  163 | 	@ConditionalOnBean(ObservationRegistry.class)
  164 | 	@ConditionalOnProperty(value = "spring.cloud.circuitbreaker.resilience4j.micrometer.enabled", matchIfMissing = true)
  165 | 	public static class ObservationRegistryCustomizerResilience4jCustomizer {
  166 | 
  167 | 		@Autowired
  168 | 		ObservationRegistry observationRegistry;
  169 | 
  170 | 		@Autowired(required = false)
  171 | 		private @Nullable Resilience4JCircuitBreakerFactory factory;
  172 | 
  173 | 		@PostConstruct
  174 | 		public void init() {
  175 | 			if (this.factory != null) {
  176 | 				this.factory.setObservationRegistry(this.observationRegistry);
  177 | 			}
  178 | 		}
  179 | 
  180 | 	}
  181 | 
  182 | }
```
