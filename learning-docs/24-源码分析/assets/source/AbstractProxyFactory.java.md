<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.apache.dubbo:dubbo:3.3.6 | path: org/apache/dubbo/rpc/proxy/AbstractProxyFactory.java -->
<!-- 正文引用：[[源码:AbstractProxyFactory.java#L起-L止]] ；代码块头部：(src: AbstractProxyFactory.java#L起-L止) -->

# AbstractProxyFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.apache.dubbo:dubbo:3.3.6` |
| 路径 | `org/apache/dubbo/rpc/proxy/AbstractProxyFactory.java` |
| 行数 | 137 |
| 在线地址 | https://repo1.maven.org/maven2/org/apache/dubbo/dubbo/3.3.6/dubbo-3.3.6-sources.jar |
| 抓取时间 | 2026-11-25T15:33:31.363Z |

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
   17 | package org.apache.dubbo.rpc.proxy;
   18 | 
   19 | import org.apache.dubbo.common.compact.Dubbo2CompactUtils;
   20 | import org.apache.dubbo.common.logger.ErrorTypeAwareLogger;
   21 | import org.apache.dubbo.common.logger.LoggerFactory;
   22 | import org.apache.dubbo.common.utils.ClassUtils;
   23 | import org.apache.dubbo.common.utils.ReflectUtils;
   24 | import org.apache.dubbo.common.utils.StringUtils;
   25 | import org.apache.dubbo.rpc.Constants;
   26 | import org.apache.dubbo.rpc.Invoker;
   27 | import org.apache.dubbo.rpc.ProxyFactory;
   28 | import org.apache.dubbo.rpc.RpcException;
   29 | import org.apache.dubbo.rpc.model.ServiceModel;
   30 | import org.apache.dubbo.rpc.service.Destroyable;
   31 | import org.apache.dubbo.rpc.service.EchoService;
   32 | import org.apache.dubbo.rpc.service.GenericService;
   33 | 
   34 | import java.util.Arrays;
   35 | import java.util.LinkedHashSet;
   36 | 
   37 | import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
   38 | import static org.apache.dubbo.common.constants.LoggerCodeConstants.PROXY_UNSUPPORTED_INVOKER;
   39 | import static org.apache.dubbo.rpc.Constants.INTERFACES;
   40 | 
   41 | public abstract class AbstractProxyFactory implements ProxyFactory {
   42 |     private static final Class<?>[] INTERNAL_INTERFACES = new Class<?>[] {EchoService.class, Destroyable.class};
   43 | 
   44 |     private static final ErrorTypeAwareLogger logger =
   45 |             LoggerFactory.getErrorTypeAwareLogger(AbstractProxyFactory.class);
   46 | 
   47 |     @Override
   48 |     public <T> T getProxy(Invoker<T> invoker) throws RpcException {
   49 |         return getProxy(invoker, false);
   50 |     }
   51 | 
   52 |     @Override
   53 |     public <T> T getProxy(Invoker<T> invoker, boolean generic) throws RpcException {
   54 |         // when compiling with native image, ensure that the order of the interfaces remains unchanged
   55 |         LinkedHashSet<Class<?>> interfaces = new LinkedHashSet<>();
   56 |         ClassLoader classLoader = getClassLoader(invoker);
   57 | 
   58 |         String config = invoker.getUrl().getParameter(INTERFACES);
   59 |         if (StringUtils.isNotEmpty(config)) {
   60 |             String[] types = COMMA_SPLIT_PATTERN.split(config);
   61 |             for (String type : types) {
   62 |                 try {
   63 |                     interfaces.add(ReflectUtils.forName(classLoader, type));
   64 |                 } catch (Throwable e) {
   65 |                     // ignore
   66 |                 }
   67 |             }
   68 |         }
   69 | 
   70 |         Class<?> realInterfaceClass = null;
   71 |         if (generic) {
   72 |             try {
   73 |                 // find the real interface from url
   74 |                 String realInterface = invoker.getUrl().getParameter(Constants.INTERFACE);
   75 |                 realInterfaceClass = ReflectUtils.forName(classLoader, realInterface);
   76 |                 interfaces.add(realInterfaceClass);
   77 |             } catch (Throwable e) {
   78 |                 // ignore
   79 |             }
   80 | 
   81 |             if (GenericService.class.isAssignableFrom(invoker.getInterface())
   82 |                     && Dubbo2CompactUtils.isEnabled()
   83 |                     && Dubbo2CompactUtils.isGenericServiceClassLoaded()) {
   84 |                 interfaces.add(Dubbo2CompactUtils.getGenericServiceClass());
   85 |             }
   86 |             if (!GenericService.class.isAssignableFrom(invoker.getInterface())) {
   87 |                 if (Dubbo2CompactUtils.isEnabled() && Dubbo2CompactUtils.isGenericServiceClassLoaded()) {
   88 |                     interfaces.add(Dubbo2CompactUtils.getGenericServiceClass());
   89 |                 } else {
   90 |                     interfaces.add(org.apache.dubbo.rpc.service.GenericService.class);
   91 |                 }
   92 |             }
   93 |         }
   94 | 
   95 |         interfaces.add(invoker.getInterface());
   96 |         interfaces.addAll(Arrays.asList(INTERNAL_INTERFACES));
   97 | 
   98 |         try {
   99 |             return getProxy(invoker, interfaces.toArray(new Class<?>[0]));
  100 |         } catch (Throwable t) {
  101 |             if (generic) {
  102 |                 if (realInterfaceClass != null) {
  103 |                     interfaces.remove(realInterfaceClass);
  104 |                 }
  105 |                 interfaces.remove(invoker.getInterface());
  106 | 
  107 |                 logger.error(
  108 |                         PROXY_UNSUPPORTED_INVOKER,
  109 |                         "",
  110 |                         "",
  111 |                         "Error occur when creating proxy. Invoker is in generic mode. Trying to create proxy without real interface class.",
  112 |                         t);
  113 |                 return getProxy(invoker, interfaces.toArray(new Class<?>[0]));
  114 |             } else {
  115 |                 throw t;
  116 |             }
  117 |         }
  118 |     }
  119 | 
  120 |     private <T> ClassLoader getClassLoader(Invoker<T> invoker) {
  121 |         ServiceModel serviceModel = invoker.getUrl().getServiceModel();
  122 |         ClassLoader classLoader = null;
  123 |         if (serviceModel != null && serviceModel.getInterfaceClassLoader() != null) {
  124 |             classLoader = serviceModel.getInterfaceClassLoader();
  125 |         }
  126 |         if (classLoader == null) {
  127 |             classLoader = ClassUtils.getClassLoader();
  128 |         }
  129 |         return classLoader;
  130 |     }
  131 | 
  132 |     public static Class<?>[] getInternalInterfaces() {
  133 |         return INTERNAL_INTERFACES.clone();
  134 |     }
  135 | 
  136 |     public abstract <T> T getProxy(Invoker<T> invoker, Class<?>[] types);
  137 | }
```
