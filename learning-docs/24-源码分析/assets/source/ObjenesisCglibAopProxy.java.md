<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-aop:7.1.0 | path: org/springframework/aop/framework/ObjenesisCglibAopProxy.java -->
<!-- 正文引用：[[源码:ObjenesisCglibAopProxy.java#L起-L止]] ；代码块头部：(src: ObjenesisCglibAopProxy.java#L起-L止) -->

# ObjenesisCglibAopProxy.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-aop:7.1.0` |
| 路径 | `org/springframework/aop/framework/ObjenesisCglibAopProxy.java` |
| 行数 | 95 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-aop/7.1.0/spring-aop-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:59:57.137Z |

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
   19 | import java.lang.reflect.Constructor;
   20 | 
   21 | import org.apache.commons.logging.Log;
   22 | import org.apache.commons.logging.LogFactory;
   23 | 
   24 | import org.springframework.cglib.proxy.Callback;
   25 | import org.springframework.cglib.proxy.Enhancer;
   26 | import org.springframework.cglib.proxy.Factory;
   27 | import org.springframework.objenesis.SpringObjenesis;
   28 | import org.springframework.util.ReflectionUtils;
   29 | 
   30 | /**
   31 |  * Objenesis-based extension of {@link CglibAopProxy} to create proxy instances
   32 |  * without invoking the constructor of the class. Used by default.
   33 |  *
   34 |  * @author Oliver Gierke
   35 |  * @author Juergen Hoeller
   36 |  * @since 4.0
   37 |  */
   38 | @SuppressWarnings("serial")
   39 | class ObjenesisCglibAopProxy extends CglibAopProxy {
   40 | 
   41 | 	private static final Log logger = LogFactory.getLog(ObjenesisCglibAopProxy.class);
   42 | 
   43 | 	private static final SpringObjenesis objenesis = new SpringObjenesis();
   44 | 
   45 | 
   46 | 	/**
   47 | 	 * Create a new ObjenesisCglibAopProxy for the given AOP configuration.
   48 | 	 * @param config the AOP configuration as AdvisedSupport object
   49 | 	 */
   50 | 	public ObjenesisCglibAopProxy(AdvisedSupport config) {
   51 | 		super(config);
   52 | 	}
   53 | 
   54 | 
   55 | 	@Override
   56 | 	protected Class<?> createProxyClass(Enhancer enhancer) {
   57 | 		return enhancer.createClass();
   58 | 	}
   59 | 
   60 | 	@Override
   61 | 	protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) {
   62 | 		Class<?> proxyClass = enhancer.createClass();
   63 | 		Object proxyInstance = null;
   64 | 
   65 | 		if (objenesis.isWorthTrying()) {
   66 | 			try {
   67 | 				proxyInstance = objenesis.newInstance(proxyClass, enhancer.getUseCache());
   68 | 			}
   69 | 			catch (Throwable ex) {
   70 | 				logger.debug("Unable to instantiate proxy using Objenesis, " +
   71 | 						"falling back to regular proxy construction", ex);
   72 | 			}
   73 | 		}
   74 | 
   75 | 		if (proxyInstance == null) {
   76 | 			// Regular instantiation via default constructor...
   77 | 			try {
   78 | 				Constructor<?> ctor = (this.constructorArgs != null ?
   79 | 						proxyClass.getDeclaredConstructor(this.constructorArgTypes) :
   80 | 						proxyClass.getDeclaredConstructor());
   81 | 				ReflectionUtils.makeAccessible(ctor);
   82 | 				proxyInstance = (this.constructorArgs != null ?
   83 | 						ctor.newInstance(this.constructorArgs) : ctor.newInstance());
   84 | 			}
   85 | 			catch (Throwable ex) {
   86 | 				throw new AopConfigException("Unable to instantiate proxy using Objenesis, " +
   87 | 						"and regular proxy instantiation via default constructor fails as well", ex);
   88 | 			}
   89 | 		}
   90 | 
   91 | 		((Factory) proxyInstance).setCallbacks(callbacks);
   92 | 		return proxyInstance;
   93 | 	}
   94 | 
   95 | }
```
