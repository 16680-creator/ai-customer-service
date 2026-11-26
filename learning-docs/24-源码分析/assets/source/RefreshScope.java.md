<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.cloud:spring-cloud-context:5.1.0 | path: org/springframework/cloud/context/scope/refresh/RefreshScope.java -->
<!-- 正文引用：[[源码:RefreshScope.java#L起-L止]] ；代码块头部：(src: RefreshScope.java#L起-L止) -->

# RefreshScope.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.cloud:spring-cloud-context:5.1.0` |
| 路径 | `org/springframework/cloud/context/scope/refresh/RefreshScope.java` |
| 行数 | 177 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-context/5.1.0/spring-cloud-context-5.1.0-sources.jar |
| 抓取时间 | 2026-11-25T15:31:36.711Z |

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
   17 | package org.springframework.cloud.context.scope.refresh;
   18 | 
   19 | import java.io.Serializable;
   20 | 
   21 | import org.springframework.aop.scope.ScopedProxyUtils;
   22 | import org.springframework.beans.BeansException;
   23 | import org.springframework.beans.factory.BeanFactoryUtils;
   24 | import org.springframework.beans.factory.config.BeanDefinition;
   25 | import org.springframework.beans.factory.support.BeanDefinitionRegistry;
   26 | import org.springframework.cloud.context.scope.GenericScope;
   27 | import org.springframework.context.ApplicationContext;
   28 | import org.springframework.context.ApplicationContextAware;
   29 | import org.springframework.context.ApplicationListener;
   30 | import org.springframework.context.event.ContextRefreshedEvent;
   31 | import org.springframework.core.Ordered;
   32 | import org.springframework.jmx.export.annotation.ManagedOperation;
   33 | import org.springframework.jmx.export.annotation.ManagedResource;
   34 | 
   35 | /**
   36 |  * <p>
   37 |  * A Scope implementation that allows for beans to be refreshed dynamically at runtime
   38 |  * (see {@link #refresh(String)} and {@link #refreshAll()}). If a bean is refreshed then
   39 |  * the next time the bean is accessed (i.e. a method is executed) a new instance is
   40 |  * created. All lifecycle methods are applied to the bean instances, so any destruction
   41 |  * callbacks that were registered in the bean factory are called when it is refreshed, and
   42 |  * then the initialization callbacks are invoked as normal when the new instance is
   43 |  * created. A new bean instance is created from the original bean definition, so any
   44 |  * externalized content (property placeholders or expressions in string literals) is
   45 |  * re-evaluated when it is created.
   46 |  * </p>
   47 |  *
   48 |  * <p>
   49 |  * Note that all beans in this scope are <em>only</em> initialized when first accessed, so
   50 |  * the scope forces lazy initialization semantics.
   51 |  * </p>
   52 |  *
   53 |  * <p>
   54 |  * The scoped proxy approach adopted here has a side benefit that bean instances are
   55 |  * automatically {@link Serializable}, and can be sent across the wire as long as the
   56 |  * receiver has an identical application context on the other side. To ensure that the two
   57 |  * contexts agree that they are identical, they have to have the same serialization ID.
   58 |  * One will be generated automatically by default from the bean names, so two contexts
   59 |  * with the same bean names are by default able to exchange beans by name. If you need to
   60 |  * override the default ID, then provide an explicit {@link #setId(String) id} when the
   61 |  * Scope is declared.
   62 |  * </p>
   63 |  *
   64 |  * @author Dave Syer
   65 |  * @since 3.1
   66 |  *
   67 |  */
   68 | @ManagedResource
   69 | public class RefreshScope extends GenericScope
   70 | 		implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent>, Ordered {
   71 | 
   72 | 	private ApplicationContext context;
   73 | 
   74 | 	private BeanDefinitionRegistry registry;
   75 | 
   76 | 	private boolean eager = true;
   77 | 
   78 | 	private int order = Ordered.LOWEST_PRECEDENCE - 100;
   79 | 
   80 | 	/**
   81 | 	 * Creates a scope instance and gives it the default name: "refresh".
   82 | 	 */
   83 | 	public RefreshScope() {
   84 | 		super.setName("refresh");
   85 | 	}
   86 | 
   87 | 	@Override
   88 | 	public int getOrder() {
   89 | 		return this.order;
   90 | 	}
   91 | 
   92 | 	public void setOrder(int order) {
   93 | 		this.order = order;
   94 | 	}
   95 | 
   96 | 	/**
   97 | 	 * Flag to determine whether all beans in refresh scope should be instantiated eagerly
   98 | 	 * on startup. Default true.
   99 | 	 * @param eager The flag to set.
  100 | 	 */
  101 | 	public void setEager(boolean eager) {
  102 | 		this.eager = eager;
  103 | 	}
  104 | 
  105 | 	@Override
  106 | 	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
  107 | 		this.registry = registry;
  108 | 		super.postProcessBeanDefinitionRegistry(registry);
  109 | 	}
  110 | 
  111 | 	@Override
  112 | 	public void onApplicationEvent(ContextRefreshedEvent event) {
  113 | 		start(event);
  114 | 	}
  115 | 
  116 | 	public void start(ContextRefreshedEvent event) {
  117 | 		if (event.getApplicationContext() == this.context && this.eager && this.registry != null) {
  118 | 			eagerlyInitialize();
  119 | 		}
  120 | 	}
  121 | 
  122 | 	@SuppressWarnings("ReturnValueIgnored")
  123 | 	private void eagerlyInitialize() {
  124 | 		for (String name : this.context.getBeanDefinitionNames()) {
  125 | 			BeanDefinition definition = this.registry.getBeanDefinition(name);
  126 | 			if (this.getName().equals(definition.getScope()) && !definition.isLazyInit()) {
  127 | 				Object bean = this.context.getBean(name);
  128 | 				if (bean != null) {
  129 | 					bean.getClass();
  130 | 				}
  131 | 			}
  132 | 		}
  133 | 	}
  134 | 
  135 | 	/**
  136 | 	 * WARNING: This method refreshes beans from any context in the hierarchy using the
  137 | 	 * main application context.
  138 | 	 * @param type bean type to rebind.
  139 | 	 * @return true, if successful.
  140 | 	 */
  141 | 	public boolean refresh(Class type) {
  142 | 		String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.context, type);
  143 | 		if (beanNamesForType.length > 0) {
  144 | 			return refresh(beanNamesForType[0]);
  145 | 		}
  146 | 		return false;
  147 | 	}
  148 | 
  149 | 	@ManagedOperation(description = "Dispose of the current instance of bean name "
  150 | 			+ "provided and force a refresh on next method execution.")
  151 | 	public boolean refresh(String name) {
  152 | 		if (!ScopedProxyUtils.isScopedTarget(name)) {
  153 | 			// User wants to refresh the bean with this name but that isn't the one in the
  154 | 			// cache...
  155 | 			name = ScopedProxyUtils.getTargetBeanName(name);
  156 | 		}
  157 | 		// Ensure lifecycle is finished if bean was disposable
  158 | 		if (super.destroy(name)) {
  159 | 			this.context.publishEvent(new RefreshScopeRefreshedEvent(name));
  160 | 			return true;
  161 | 		}
  162 | 		return false;
  163 | 	}
  164 | 
  165 | 	@ManagedOperation(description = "Dispose of the current instance of all beans "
  166 | 			+ "in this scope and force a refresh on next method execution.")
  167 | 	public void refreshAll() {
  168 | 		super.destroy();
  169 | 		this.context.publishEvent(new RefreshScopeRefreshedEvent());
  170 | 	}
  171 | 
  172 | 	@Override
  173 | 	public void setApplicationContext(ApplicationContext context) throws BeansException {
  174 | 		this.context = context;
  175 | 	}
  176 | 
  177 | }
```
