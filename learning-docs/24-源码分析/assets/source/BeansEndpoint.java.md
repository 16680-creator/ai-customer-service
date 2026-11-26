<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-actuator:4.2.0 | path: org/springframework/boot/actuate/beans/BeansEndpoint.java -->
<!-- 正文引用：[[源码:BeansEndpoint.java#L起-L止]] ；代码块头部：(src: BeansEndpoint.java#L起-L止) -->

# BeansEndpoint.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-actuator:4.2.0` |
| 路径 | `org/springframework/boot/actuate/beans/BeansEndpoint.java` |
| 行数 | 185 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-actuator/4.2.0/spring-boot-actuator-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:01.566Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.actuate.beans;
    6 | 
    7 | import java.util.HashMap;
    8 | import java.util.Map;
    9 | 
   10 | import org.jspecify.annotations.Nullable;
   11 | 
   12 | import org.springframework.beans.factory.config.BeanDefinition;
   13 | import org.springframework.beans.factory.config.ConfigurableBeanFactory;
   14 | import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
   15 | import org.springframework.boot.actuate.endpoint.OperationResponseBody;
   16 | import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
   17 | import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
   18 | import org.springframework.context.ApplicationContext;
   19 | import org.springframework.context.ConfigurableApplicationContext;
   20 | import org.springframework.lang.Contract;
   21 | import org.springframework.util.StringUtils;
   22 | 
   23 | /**
   24 |  * {@link Endpoint @Endpoint} to expose details of an application's beans, grouped by
   25 |  * application context.
   26 |  *
   27 |  * @author Dave Syer
   28 |  * @author Andy Wilkinson
   29 |  * @since 2.0.0
   30 |  */
   31 | @Endpoint(id = "beans")
   32 | public class BeansEndpoint {
   33 | 
   34 | 	private final ConfigurableApplicationContext context;
   35 | 
   36 | 	/**
   37 | 	 * Creates a new {@code BeansEndpoint} that will describe the beans in the given
   38 | 	 * {@code context} and all of its ancestors.
   39 | 	 * @param context the application context
   40 | 	 * @see ConfigurableApplicationContext#getParent()
   41 | 	 */
   42 | 	public BeansEndpoint(ConfigurableApplicationContext context) {
   43 | 		this.context = context;
   44 | 	}
   45 | 
   46 | 	@ReadOperation
   47 | 	public BeansDescriptor beans() {
   48 | 		Map<@Nullable String, ContextBeansDescriptor> contexts = new HashMap<>();
   49 | 		ConfigurableApplicationContext context = this.context;
   50 | 		while (context != null) {
   51 | 			contexts.put(context.getId(), ContextBeansDescriptor.describing(context));
   52 | 			context = getConfigurableParent(context);
   53 | 		}
   54 | 		return new BeansDescriptor(contexts);
   55 | 	}
   56 | 
   57 | 	private static @Nullable ConfigurableApplicationContext getConfigurableParent(
   58 | 			ConfigurableApplicationContext context) {
   59 | 		ApplicationContext parent = context.getParent();
   60 | 		if (parent instanceof ConfigurableApplicationContext configurableParent) {
   61 | 			return configurableParent;
   62 | 		}
   63 | 		return null;
   64 | 	}
   65 | 
   66 | 	/**
   67 | 	 * Description of an application's beans.
   68 | 	 */
   69 | 	public static final class BeansDescriptor implements OperationResponseBody {
   70 | 
   71 | 		private final Map<@Nullable String, ContextBeansDescriptor> contexts;
   72 | 
   73 | 		private BeansDescriptor(Map<@Nullable String, ContextBeansDescriptor> contexts) {
   74 | 			this.contexts = contexts;
   75 | 		}
   76 | 
   77 | 		public Map<@Nullable String, ContextBeansDescriptor> getContexts() {
   78 | 			return this.contexts;
   79 | 		}
   80 | 
   81 | 	}
   82 | 
   83 | 	/**
   84 | 	 * Description of an application context beans.
   85 | 	 */
   86 | 	public static final class ContextBeansDescriptor {
   87 | 
   88 | 		private final Map<String, BeanDescriptor> beans;
   89 | 
   90 | 		private final @Nullable String parentId;
   91 | 
   92 | 		private ContextBeansDescriptor(Map<String, BeanDescriptor> beans, @Nullable String parentId) {
   93 | 			this.beans = beans;
   94 | 			this.parentId = parentId;
   95 | 		}
   96 | 
   97 | 		public @Nullable String getParentId() {
   98 | 			return this.parentId;
   99 | 		}
  100 | 
  101 | 		public Map<String, BeanDescriptor> getBeans() {
  102 | 			return this.beans;
  103 | 		}
  104 | 
  105 | 		@Contract("!null -> !null")
  106 | 		private static @Nullable ContextBeansDescriptor describing(@Nullable ConfigurableApplicationContext context) {
  107 | 			if (context == null) {
  108 | 				return null;
  109 | 			}
  110 | 			ConfigurableApplicationContext parent = getConfigurableParent(context);
  111 | 			return new ContextBeansDescriptor(describeBeans(context.getBeanFactory()),
  112 | 					(parent != null) ? parent.getId() : null);
  113 | 		}
  114 | 
  115 | 		private static Map<String, BeanDescriptor> describeBeans(ConfigurableListableBeanFactory beanFactory) {
  116 | 			Map<String, BeanDescriptor> beans = new HashMap<>();
  117 | 			for (String beanName : beanFactory.getBeanDefinitionNames()) {
  118 | 				BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
  119 | 				if (isBeanEligible(beanName, definition, beanFactory)) {
  120 | 					beans.put(beanName, describeBean(beanName, definition, beanFactory));
  121 | 				}
  122 | 			}
  123 | 			return beans;
  124 | 		}
  125 | 
  126 | 		private static BeanDescriptor describeBean(String name, BeanDefinition definition,
  127 | 				ConfigurableListableBeanFactory factory) {
  128 | 			return new BeanDescriptor(factory.getAliases(name), definition.getScope(), factory.getType(name),
  129 | 					definition.getResourceDescription(), factory.getDependenciesForBean(name));
  130 | 		}
  131 | 
  132 | 		private static boolean isBeanEligible(String beanName, BeanDefinition bd, ConfigurableBeanFactory bf) {
  133 | 			return (bd.getRole() != BeanDefinition.ROLE_INFRASTRUCTURE
  134 | 					&& (!bd.isLazyInit() || bf.containsSingleton(beanName)));
  135 | 		}
  136 | 
  137 | 	}
  138 | 
  139 | 	/**
  140 | 	 * Description of a bean.
  141 | 	 */
  142 | 	public static final class BeanDescriptor {
  143 | 
  144 | 		private final String[] aliases;
  145 | 
  146 | 		private final @Nullable String scope;
  147 | 
  148 | 		private final @Nullable Class<?> type;
  149 | 
  150 | 		private final @Nullable String resource;
  151 | 
  152 | 		private final String[] dependencies;
  153 | 
  154 | 		private BeanDescriptor(String[] aliases, @Nullable String scope, @Nullable Class<?> type,
  155 | 				@Nullable String resource, String[] dependencies) {
  156 | 			this.aliases = aliases;
  157 | 			this.scope = (StringUtils.hasText(scope) ? scope : ConfigurableBeanFactory.SCOPE_SINGLETON);
  158 | 			this.type = type;
  159 | 			this.resource = resource;
  160 | 			this.dependencies = dependencies;
  161 | 		}
  162 | 
  163 | 		public String[] getAliases() {
  164 | 			return this.aliases;
  165 | 		}
  166 | 
  167 | 		public @Nullable String getScope() {
  168 | 			return this.scope;
  169 | 		}
  170 | 
  171 | 		public @Nullable Class<?> getType() {
  172 | 			return this.type;
  173 | 		}
  174 | 
  175 | 		public @Nullable String getResource() {
  176 | 			return this.resource;
  177 | 		}
  178 | 
  179 | 		public String[] getDependencies() {
  180 | 			return this.dependencies;
  181 | 		}
  182 | 
  183 | 	}
  184 | 
  185 | }
```
