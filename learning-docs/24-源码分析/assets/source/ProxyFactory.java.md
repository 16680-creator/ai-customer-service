<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/ProxyFactory.java -->
<!-- 正文引用：[[源码:ProxyFactory.java#L起-L止]] ；代码块头部：(src: ProxyFactory.java#L起-L止) -->

# ProxyFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/ProxyFactory.java` |
| 行数 | 169 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T16:34:21.832Z |

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
   19 | import org.aopalliance.intercept.Interceptor;
   20 | import org.jspecify.annotations.Nullable;
   21 | 
   22 | import org.springframework.aop.TargetSource;
   23 | import org.springframework.util.ClassUtils;
   24 | 
   25 | /**
   26 |  * Factory for AOP proxies for programmatic use, rather than via declarative
   27 |  * setup in a bean factory. This class provides a simple way of obtaining
   28 |  * and configuring AOP proxy instances in custom user code.
   29 |  *
   30 |  * @author Rod Johnson
   31 |  * @author Juergen Hoeller
   32 |  * @author Rob Harrop
   33 |  * @since 14.03.2003
   34 |  */
   35 | @SuppressWarnings("serial")
   36 | public class ProxyFactory extends ProxyCreatorSupport {
   37 | 
   38 | 	/**
   39 | 	 * Create a new ProxyFactory.
   40 | 	 */
   41 | 	public ProxyFactory() {
   42 | 	}
   43 | 
   44 | 	/**
   45 | 	 * Create a new ProxyFactory.
   46 | 	 * <p>Will proxy all interfaces that the given target implements.
   47 | 	 * @param target the target object to be proxied
   48 | 	 */
   49 | 	public ProxyFactory(Object target) {
   50 | 		setTarget(target);
   51 | 		setInterfaces(ClassUtils.getAllInterfaces(target));
   52 | 	}
   53 | 
   54 | 	/**
   55 | 	 * Create a new ProxyFactory.
   56 | 	 * <p>No target, only interfaces. Must add interceptors.
   57 | 	 * @param proxyInterfaces the interfaces that the proxy should implement
   58 | 	 */
   59 | 	public ProxyFactory(Class<?>... proxyInterfaces) {
   60 | 		setInterfaces(proxyInterfaces);
   61 | 	}
   62 | 
   63 | 	/**
   64 | 	 * Create a new ProxyFactory for the given interface and interceptor.
   65 | 	 * <p>Convenience method for creating a proxy for a single interceptor,
   66 | 	 * assuming that the interceptor handles all calls itself rather than
   67 | 	 * delegating to a target, like in the case of remoting proxies.
   68 | 	 * @param proxyInterface the interface that the proxy should implement
   69 | 	 * @param interceptor the interceptor that the proxy should invoke
   70 | 	 */
   71 | 	public ProxyFactory(Class<?> proxyInterface, Interceptor interceptor) {
   72 | 		addInterface(proxyInterface);
   73 | 		addAdvice(interceptor);
   74 | 	}
   75 | 
   76 | 	/**
   77 | 	 * Create a ProxyFactory for the specified {@code TargetSource},
   78 | 	 * making the proxy implement the specified interface.
   79 | 	 * @param proxyInterface the interface that the proxy should implement
   80 | 	 * @param targetSource the TargetSource that the proxy should invoke
   81 | 	 */
   82 | 	public ProxyFactory(Class<?> proxyInterface, TargetSource targetSource) {
   83 | 		addInterface(proxyInterface);
   84 | 		setTargetSource(targetSource);
   85 | 	}
   86 | 
   87 | 
   88 | 	/**
   89 | 	 * Create a new proxy according to the settings in this factory.
   90 | 	 * <p>Can be called repeatedly. Effect will vary if we've added
   91 | 	 * or removed interfaces. Can add and remove interceptors.
   92 | 	 * <p>Uses a default class loader: Usually, the thread context class loader
   93 | 	 * (if necessary for proxy creation).
   94 | 	 * @return the proxy object
   95 | 	 */
   96 | 	public Object getProxy() {
   97 | 		return createAopProxy().getProxy();
   98 | 	}
   99 | 
  100 | 	/**
  101 | 	 * Create a new proxy according to the settings in this factory.
  102 | 	 * <p>Can be called repeatedly. Effect will vary if we've added
  103 | 	 * or removed interfaces. Can add and remove interceptors.
  104 | 	 * <p>Uses the given class loader (if necessary for proxy creation).
  105 | 	 * @param classLoader the class loader to create the proxy with
  106 | 	 * (or {@code null} for the low-level proxy facility's default)
  107 | 	 * @return the proxy object
  108 | 	 */
  109 | 	public Object getProxy(@Nullable ClassLoader classLoader) {
  110 | 		return createAopProxy().getProxy(classLoader);
  111 | 	}
  112 | 
  113 | 	/**
  114 | 	 * Determine the proxy class according to the settings in this factory.
  115 | 	 * @param classLoader the class loader to create the proxy class with
  116 | 	 * (or {@code null} for the low-level proxy facility's default)
  117 | 	 * @return the proxy class
  118 | 	 * @since 6.0
  119 | 	 */
  120 | 	public Class<?> getProxyClass(@Nullable ClassLoader classLoader) {
  121 | 		return createAopProxy().getProxyClass(classLoader);
  122 | 	}
  123 | 
  124 | 
  125 | 	/**
  126 | 	 * Create a new proxy for the given interface and interceptor.
  127 | 	 * <p>Convenience method for creating a proxy for a single interceptor,
  128 | 	 * assuming that the interceptor handles all calls itself rather than
  129 | 	 * delegating to a target, like in the case of remoting proxies.
  130 | 	 * @param proxyInterface the interface that the proxy should implement
  131 | 	 * @param interceptor the interceptor that the proxy should invoke
  132 | 	 * @return the proxy object
  133 | 	 * @see #ProxyFactory(Class, org.aopalliance.intercept.Interceptor)
  134 | 	 */
  135 | 	@SuppressWarnings("unchecked")
  136 | 	public static <T> T getProxy(Class<T> proxyInterface, Interceptor interceptor) {
  137 | 		return (T) new ProxyFactory(proxyInterface, interceptor).getProxy();
  138 | 	}
  139 | 
  140 | 	/**
  141 | 	 * Create a proxy for the specified {@code TargetSource},
  142 | 	 * implementing the specified interface.
  143 | 	 * @param proxyInterface the interface that the proxy should implement
  144 | 	 * @param targetSource the TargetSource that the proxy should invoke
  145 | 	 * @return the proxy object
  146 | 	 * @see #ProxyFactory(Class, org.springframework.aop.TargetSource)
  147 | 	 */
  148 | 	@SuppressWarnings("unchecked")
  149 | 	public static <T> T getProxy(Class<T> proxyInterface, TargetSource targetSource) {
  150 | 		return (T) new ProxyFactory(proxyInterface, targetSource).getProxy();
  151 | 	}
  152 | 
  153 | 	/**
  154 | 	 * Create a proxy for the specified {@code TargetSource} that extends
  155 | 	 * the target class of the {@code TargetSource}.
  156 | 	 * @param targetSource the TargetSource that the proxy should invoke
  157 | 	 * @return the proxy object
  158 | 	 */
  159 | 	public static Object getProxy(TargetSource targetSource) {
  160 | 		if (targetSource.getTargetClass() == null) {
  161 | 			throw new IllegalArgumentException("Cannot create class proxy for TargetSource with null target class");
  162 | 		}
  163 | 		ProxyFactory proxyFactory = new ProxyFactory();
  164 | 		proxyFactory.setTargetSource(targetSource);
  165 | 		proxyFactory.setProxyTargetClass(true);
  166 | 		return proxyFactory.getProxy();
  167 | 	}
  168 | 
  169 | }
```
