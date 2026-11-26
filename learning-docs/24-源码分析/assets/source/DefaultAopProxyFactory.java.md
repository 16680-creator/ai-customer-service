<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/DefaultAopProxyFactory.java -->
<!-- 正文引用：[[源码:DefaultAopProxyFactory.java#L起-L止]] ；代码块头部：(src: DefaultAopProxyFactory.java#L起-L止) -->

# DefaultAopProxyFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/DefaultAopProxyFactory.java` |
| 行数 | 78 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:56.989Z |

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
   19 | import java.io.Serializable;
   20 | import java.lang.reflect.Proxy;
   21 | 
   22 | import org.springframework.util.ClassUtils;
   23 | 
   24 | /**
   25 |  * Default {@link AopProxyFactory} implementation, creating either a CGLIB proxy
   26 |  * or a JDK dynamic proxy.
   27 |  *
   28 |  * <p>Creates a CGLIB proxy if one the following is true for a given
   29 |  * {@link AdvisedSupport} instance:
   30 |  * <ul>
   31 |  * <li>the {@code optimize} flag is set
   32 |  * <li>the {@code proxyTargetClass} flag is set
   33 |  * <li>no proxy interfaces have been specified
   34 |  * </ul>
   35 |  *
   36 |  * <p>In general, specify {@code proxyTargetClass} to enforce a CGLIB proxy,
   37 |  * or specify one or more interfaces to use a JDK dynamic proxy.
   38 |  *
   39 |  * @author Rod Johnson
   40 |  * @author Juergen Hoeller
   41 |  * @author Sebastien Deleuze
   42 |  * @author Sam Brannen
   43 |  * @since 12.03.2004
   44 |  * @see AdvisedSupport#setOptimize
   45 |  * @see AdvisedSupport#setProxyTargetClass
   46 |  * @see AdvisedSupport#setInterfaces
   47 |  */
   48 | public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {
   49 | 
   50 | 	/**
   51 | 	 * Singleton instance of this class.
   52 | 	 * @since 6.0.10
   53 | 	 */
   54 | 	public static final DefaultAopProxyFactory INSTANCE = new DefaultAopProxyFactory();
   55 | 
   56 | 	private static final long serialVersionUID = 7930414337282325166L;
   57 | 
   58 | 
   59 | 	@Override
   60 | 	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
   61 | 		if (config.isOptimize() || config.isProxyTargetClass() || !config.hasUserSuppliedInterfaces()) {
   62 | 			Class<?> targetClass = config.getTargetClass();
   63 | 			if (targetClass == null && config.getProxiedInterfaces().length == 0) {
   64 | 				throw new AopConfigException("TargetSource cannot determine target class: " +
   65 | 						"Either an interface or a target is required for proxy creation.");
   66 | 			}
   67 | 			if (targetClass == null || targetClass.isInterface() ||
   68 | 					Proxy.isProxyClass(targetClass) || ClassUtils.isLambdaClass(targetClass)) {
   69 | 				return new JdkDynamicAopProxy(config);
   70 | 			}
   71 | 			return new ObjenesisCglibAopProxy(config);
   72 | 		}
   73 | 		else {
   74 | 			return new JdkDynamicAopProxy(config);
   75 | 		}
   76 | 	}
   77 | 
   78 | }
```
