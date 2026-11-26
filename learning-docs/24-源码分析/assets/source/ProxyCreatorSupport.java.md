<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/ProxyCreatorSupport.java -->
<!-- 正文引用：[[源码:ProxyCreatorSupport#L起-L止]] ；代码块头部：(src: ProxyCreatorSupport#L起-L止) -->

# ProxyCreatorSupport

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/ProxyCreatorSupport.java` |
| 行数 | 142 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T17:39:25.217Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
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
   17 | package org.springframework.aop.framework;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | 
   22 | import org.springframework.util.Assert;
   23 | 
   24 | /**
   25 |  * Base class for proxy factories.
   26 |  * Provides convenient access to a configurable AopProxyFactory.
   27 |  *
   28 |  * @author Juergen Hoeller
   29 |  * @since 2.0.3
   30 |  * @see #createAopProxy()
   31 |  */
   32 | @SuppressWarnings("serial")
   33 | public class ProxyCreatorSupport extends AdvisedSupport {
   34 | 
   35 | 	private AopProxyFactory aopProxyFactory;
   36 | 
   37 | 	private final List<AdvisedSupportListener> listeners = new ArrayList<>();
   38 | 
   39 | 	/** Set to true when the first AOP proxy has been created. */
   40 | 	private boolean active = false;
   41 | 
   42 | 
   43 | 	/**
   44 | 	 * Create a new ProxyCreatorSupport instance.
   45 | 	 */
   46 | 	public ProxyCreatorSupport() {
   47 | 		this.aopProxyFactory = DefaultAopProxyFactory.INSTANCE;
   48 | 	}
   49 | 
   50 | 	/**
   51 | 	 * Create a new ProxyCreatorSupport instance.
   52 | 	 * @param aopProxyFactory the AopProxyFactory to use
   53 | 	 */
   54 | 	public ProxyCreatorSupport(AopProxyFactory aopProxyFactory) {
   55 | 		Assert.notNull(aopProxyFactory, "AopProxyFactory must not be null");
   56 | 		this.aopProxyFactory = aopProxyFactory;
   57 | 	}
   58 | 
   59 | 
   60 | 	/**
   61 | 	 * Customize the AopProxyFactory, allowing different strategies
   62 | 	 * to be dropped in without changing the core framework.
   63 | 	 * <p>Default is {@link DefaultAopProxyFactory}, using dynamic JDK
   64 | 	 * proxies or CGLIB proxies based on the requirements.
   65 | 	 */
   66 | 	public void setAopProxyFactory(AopProxyFactory aopProxyFactory) {
   67 | 		Assert.notNull(aopProxyFactory, "AopProxyFactory must not be null");
   68 | 		this.aopProxyFactory = aopProxyFactory;
   69 | 	}
   70 | 
   71 | 	/**
   72 | 	 * Return the AopProxyFactory that this ProxyConfig uses.
   73 | 	 */
   74 | 	public AopProxyFactory getAopProxyFactory() {
   75 | 		return this.aopProxyFactory;
   76 | 	}
   77 | 
   78 | 	/**
   79 | 	 * Add the given AdvisedSupportListener to this proxy configuration.
   80 | 	 * @param listener the listener to register
   81 | 	 */
   82 | 	public void addListener(AdvisedSupportListener listener) {
   83 | 		Assert.notNull(listener, "AdvisedSupportListener must not be null");
   84 | 		this.listeners.add(listener);
   85 | 	}
   86 | 
   87 | 	/**
   88 | 	 * Remove the given AdvisedSupportListener from this proxy configuration.
   89 | 	 * @param listener the listener to remove
   90 | 	 */
   91 | 	public void removeListener(AdvisedSupportListener listener) {
   92 | 		Assert.notNull(listener, "AdvisedSupportListener must not be null");
   93 | 		this.listeners.remove(listener);
   94 | 	}
   95 | 
   96 | 
   97 | 	/**
   98 | 	 * Subclasses should call this to get a new AOP proxy. They should <b>not</b>
   99 | 	 * create an AOP proxy with {@code this} as an argument.
  100 | 	 */
  101 | 	protected final synchronized AopProxy createAopProxy() {
  102 | 		if (!this.active) {
  103 | 			activate();
  104 | 		}
  105 | 		return getAopProxyFactory().createAopProxy(this);
  106 | 	}
  107 | 
  108 | 	/**
  109 | 	 * Activate this proxy configuration.
  110 | 	 * @see AdvisedSupportListener#activated
  111 | 	 */
  112 | 	private void activate() {
  113 | 		this.active = true;
  114 | 		for (AdvisedSupportListener listener : this.listeners) {
  115 | 			listener.activated(this);
  116 | 		}
  117 | 	}
  118 | 
  119 | 	/**
  120 | 	 * Propagate advice change event to all AdvisedSupportListeners.
  121 | 	 * @see AdvisedSupportListener#adviceChanged
  122 | 	 */
  123 | 	@Override
  124 | 	protected void adviceChanged() {
  125 | 		super.adviceChanged();
  126 | 		synchronized (this) {
  127 | 			if (this.active) {
  128 | 				for (AdvisedSupportListener listener : this.listeners) {
  129 | 					listener.adviceChanged(this);
  130 | 				}
  131 | 			}
  132 | 		}
  133 | 	}
  134 | 
  135 | 	/**
  136 | 	 * Subclasses can call this to check whether any AOP proxies have been created yet.
  137 | 	 */
  138 | 	protected final synchronized boolean isActive() {
  139 | 		return this.active;
  140 | 	}
  141 | 
  142 | }
```
