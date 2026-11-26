<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.apache.dubbo:dubbo:3.3.6 | path: org/apache/dubbo/rpc/proxy/javassist/JavassistProxyFactory.java -->
<!-- 正文引用：[[源码:JavassistProxyFactory.java#L起-L止]] ；代码块头部：(src: JavassistProxyFactory.java#L起-L止) -->

# JavassistProxyFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.apache.dubbo:dubbo:3.3.6` |
| 路径 | `org/apache/dubbo/rpc/proxy/javassist/JavassistProxyFactory.java` |
| 行数 | 124 |
| 在线地址 | https://repo1.maven.org/maven2/org/apache/dubbo/dubbo/3.3.6/dubbo-3.3.6-sources.jar |
| 抓取时间 | 2026-11-25T15:33:31.158Z |

```java
    1 | /*
    2 |  * Licensed to the Apache Software Foundation (ASF) under one or more
    3 |  * contributor license agreements.  See the NOTICE file distributed with
    4 |  * this work for additional information regarding copyright ownership.
    5 |  * The ASF licenses this file to You under the Apache License, Version 2.0
    6 |  * (the "License"); you may not use this file except in compliance with
    7 |  * the License.  You may obtain a copy of the License at
    8 |  *
    9 |  *     http://www.apache.org/licenses/LICENSE-2.0
   10 |  *
   11 |  * Unless required by applicable law or agreed to in writing, software
   12 |  * distributed under the License is distributed on an "AS IS" BASIS,
   13 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   14 |  * See the License for the specific language governing permissions and
   15 |  * limitations under the License.
   16 |  */
   17 | package org.apache.dubbo.rpc.proxy.javassist;
   18 | 
   19 | import org.apache.dubbo.common.URL;
   20 | import org.apache.dubbo.common.bytecode.Proxy;
   21 | import org.apache.dubbo.common.bytecode.Wrapper;
   22 | import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
   23 | import org.apache.dubbo.common.logger.LoggerFactory;
   24 | import org.apache.dubbo.rpc.Invoker;
   25 | import org.apache.dubbo.rpc.proxy.AbstractProxyFactory;
   26 | import org.apache.dubbo.rpc.proxy.AbstractProxyInvoker;
   27 | import org.apache.dubbo.rpc.proxy.InvokerInvocationHandler;
   28 | import org.apache.dubbo.rpc.proxy.jdk.JdkProxyFactory;
   29 | 
   30 | import java.util.Arrays;
   31 | 
   32 | import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROXY_FAILED;
   33 | 
   34 | /**
   35 |  * JavassistRpcProxyFactory
   36 |  */
   37 | public class JavassistProxyFactory extends AbstractProxyFactory {
   38 |     private static final ErrorTypeAwareLogger logger =
   39 |             LoggerFactory.getErrorTypeAwareLogger(JavassistProxyFactory.class);
   40 |     private final JdkProxyFactory jdkProxyFactory = new JdkProxyFactory();
   41 | 
   42 |     @Override
   43 |     @SuppressWarnings("unchecked")
   44 |     public <T> T getProxy(Invoker<T> invoker, Class<?>[] interfaces) {
   45 |         try {
   46 |             return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
   47 |         } catch (Throwable fromJavassist) {
   48 |             // try fall back to JDK proxy factory
   49 |             try {
   50 |                 T proxy = jdkProxyFactory.getProxy(invoker, interfaces);
   51 |                 logger.error(
   52 |                         PROXY_FAILED,
   53 |                         "",
   54 |                         "",
   55 |                         "Failed to generate proxy by Javassist failed. Fallback to use JDK proxy success. "
   56 |                                 + "Interfaces: " + Arrays.toString(interfaces),
   57 |                         fromJavassist);
   58 |                 return proxy;
   59 |             } catch (Throwable fromJdk) {
   60 |                 logger.error(
   61 |                         PROXY_FAILED,
   62 |                         "",
   63 |                         "",
   64 |                         "Failed to generate proxy by Javassist failed. Fallback to use JDK proxy is also failed. "
   65 |                                 + "Interfaces: " + Arrays.toString(interfaces) + " Javassist Error.",
   66 |                         fromJavassist);
   67 |                 logger.error(
   68 |                         PROXY_FAILED,
   69 |                         "",
   70 |                         "",
   71 |                         "Failed to generate proxy by Javassist failed. Fallback to use JDK proxy is also failed. "
   72 |                                 + "Interfaces: " + Arrays.toString(interfaces) + " JDK Error.",
   73 |                         fromJdk);
   74 |                 throw fromJavassist;
   75 |             }
   76 |         }
   77 |     }
   78 | 
   79 |     @Override
   80 |     public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) {
   81 |         try {
   82 |             // TODO Wrapper cannot handle this scenario correctly: the classname contains '$'
   83 |             final Wrapper wrapper =
   84 |                     Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);
   85 |             return new AbstractProxyInvoker<T>(proxy, type, url) {
   86 |                 @Override
   87 |                 protected Object doInvoke(T proxy, String methodName, Class<?>[] parameterTypes, Object[] arguments)
   88 |                         throws Throwable {
   89 |                     return wrapper.invokeMethod(proxy, methodName, parameterTypes, arguments);
   90 |                 }
   91 |             };
   92 |         } catch (Throwable fromJavassist) {
   93 |             // try fall back to JDK proxy factory
   94 |             try {
   95 |                 Invoker<T> invoker = jdkProxyFactory.getInvoker(proxy, type, url);
   96 |                 logger.error(
   97 |                         PROXY_FAILED,
   98 |                         "",
   99 |                         "",
  100 |                         "Failed to generate invoker by Javassist failed. Fallback to use JDK proxy success. "
  101 |                                 + "Interfaces: " + type,
  102 |                         fromJavassist);
  103 |                 // log out error
  104 |                 return invoker;
  105 |             } catch (Throwable fromJdk) {
  106 |                 logger.error(
  107 |                         PROXY_FAILED,
  108 |                         "",
  109 |                         "",
  110 |                         "Failed to generate invoker by Javassist failed. Fallback to use JDK proxy is also failed. "
  111 |                                 + "Interfaces: " + type + " Javassist Error.",
  112 |                         fromJavassist);
  113 |                 logger.error(
  114 |                         PROXY_FAILED,
  115 |                         "",
  116 |                         "",
  117 |                         "Failed to generate invoker by Javassist failed. Fallback to use JDK proxy is also failed. "
  118 |                                 + "Interfaces: " + type + " JDK Error.",
  119 |                         fromJdk);
  120 |                 throw fromJavassist;
  121 |             }
  122 |         }
  123 |     }
  124 | }
```
