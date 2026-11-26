<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/executor/loader/cglib/CglibProxyFactory.java -->
<!-- 正文引用：[[源码:CglibProxyFactory.java#L起-L止]] ；代码块头部：(src: CglibProxyFactory.java#L起-L止) -->

# CglibProxyFactory.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/executor/loader/cglib/CglibProxyFactory.java` |
| 行数 | 217 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:00:26.270Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2026 the original author or authors.
    3 |  *
    4 |  *    Licensed under the Apache License, Version 2.0 (the "License");
    5 |  *    you may not use this file except in compliance with the License.
    6 |  *    You may obtain a copy of the License at
    7 |  *
    8 |  *       https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  *    Unless required by applicable law or agreed to in writing, software
   11 |  *    distributed under the License is distributed on an "AS IS" BASIS,
   12 |  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  *    See the License for the specific language governing permissions and
   14 |  *    limitations under the License.
   15 |  */
   16 | package org.apache.ibatis.executor.loader.cglib;
   17 | 
   18 | import java.lang.reflect.Method;
   19 | import java.util.List;
   20 | import java.util.Map;
   21 | import java.util.Set;
   22 | import java.util.concurrent.locks.ReentrantLock;
   23 | 
   24 | import net.sf.cglib.proxy.Callback;
   25 | import net.sf.cglib.proxy.Enhancer;
   26 | import net.sf.cglib.proxy.MethodInterceptor;
   27 | import net.sf.cglib.proxy.MethodProxy;
   28 | 
   29 | import org.apache.ibatis.executor.ErrorContext;
   30 | import org.apache.ibatis.executor.loader.AbstractEnhancedDeserializationProxy;
   31 | import org.apache.ibatis.executor.loader.AbstractSerialStateHolder;
   32 | import org.apache.ibatis.executor.loader.ProxyFactory;
   33 | import org.apache.ibatis.executor.loader.ResultLoaderMap;
   34 | import org.apache.ibatis.executor.loader.WriteReplaceInterface;
   35 | import org.apache.ibatis.io.Resources;
   36 | import org.apache.ibatis.logging.Log;
   37 | import org.apache.ibatis.logging.LogFactory;
   38 | import org.apache.ibatis.reflection.ExceptionUtil;
   39 | import org.apache.ibatis.reflection.factory.ObjectFactory;
   40 | import org.apache.ibatis.reflection.property.PropertyCopier;
   41 | import org.apache.ibatis.reflection.property.PropertyNamer;
   42 | import org.apache.ibatis.session.Configuration;
   43 | 
   44 | /**
   45 |  * @author Clinton Begin
   46 |  *
   47 |  * @deprecated Since 3.5.10, use Javassist instead.
   48 |  */
   49 | @Deprecated
   50 | public class CglibProxyFactory implements ProxyFactory {
   51 | 
   52 |   private static final String FINALIZE_METHOD = "finalize";
   53 |   private static final String WRITE_REPLACE_METHOD = "writeReplace";
   54 | 
   55 |   public CglibProxyFactory() {
   56 |     try {
   57 |       Resources.classForName("net.sf.cglib.proxy.Enhancer");
   58 |     } catch (Throwable e) {
   59 |       throw new IllegalStateException(
   60 |           "Cannot enable lazy loading because CGLIB is not available. Add CGLIB to your classpath.", e);
   61 |     }
   62 |   }
   63 | 
   64 |   @Override
   65 |   public Object createProxy(Object target, ResultLoaderMap lazyLoader, Configuration configuration,
   66 |       ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
   67 |     return EnhancedResultObjectProxyImpl.createProxy(target, lazyLoader, configuration, objectFactory,
   68 |         constructorArgTypes, constructorArgs);
   69 |   }
   70 | 
   71 |   public Object createDeserializationProxy(Object target, Map<String, ResultLoaderMap.LoadPair> unloadedProperties,
   72 |       ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
   73 |     return EnhancedDeserializationProxyImpl.createProxy(target, unloadedProperties, objectFactory, constructorArgTypes,
   74 |         constructorArgs);
   75 |   }
   76 | 
   77 |   static Object createStaticProxy(Class<?> type, Callback callback, List<Class<?>> constructorArgTypes,
   78 |       List<Object> constructorArgs) {
   79 |     LogHolder.log.warn("CglibProxyFactory is deprecated. Use another proxy factory implementation.");
   80 |     Enhancer enhancer = new Enhancer();
   81 |     enhancer.setCallback(callback);
   82 |     enhancer.setSuperclass(type);
   83 |     try {
   84 |       type.getDeclaredMethod(WRITE_REPLACE_METHOD);
   85 |       // ObjectOutputStream will call writeReplace of objects returned by writeReplace
   86 |       if (LogHolder.log.isDebugEnabled()) {
   87 |         LogHolder.log.debug(WRITE_REPLACE_METHOD + " method was found on bean " + type + ", make sure it returns this");
   88 |       }
   89 |     } catch (NoSuchMethodException e) {
   90 |       enhancer.setInterfaces(new Class[] { WriteReplaceInterface.class });
   91 |     } catch (SecurityException e) {
   92 |       // nothing to do here
   93 |     }
   94 |     Object enhanced;
   95 |     if (constructorArgTypes.isEmpty()) {
   96 |       enhanced = enhancer.create();
   97 |     } else {
   98 |       Class<?>[] typesArray = constructorArgTypes.toArray(new Class[constructorArgTypes.size()]);
   99 |       Object[] valuesArray = constructorArgs.toArray(new Object[constructorArgs.size()]);
  100 |       enhanced = enhancer.create(typesArray, valuesArray);
  101 |     }
  102 |     return enhanced;
  103 |   }
  104 | 
  105 |   private static class EnhancedResultObjectProxyImpl implements MethodInterceptor {
  106 | 
  107 |     private final Class<?> type;
  108 |     private final ResultLoaderMap lazyLoader;
  109 |     private final boolean aggressive;
  110 |     private final Set<String> lazyLoadTriggerMethods;
  111 |     private final ObjectFactory objectFactory;
  112 |     private final List<Class<?>> constructorArgTypes;
  113 |     private final List<Object> constructorArgs;
  114 |     private final ReentrantLock lock = new ReentrantLock();
  115 | 
  116 |     private EnhancedResultObjectProxyImpl(Class<?> type, ResultLoaderMap lazyLoader, Configuration configuration,
  117 |         ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
  118 |       this.type = type;
  119 |       this.lazyLoader = lazyLoader;
  120 |       this.aggressive = configuration.isAggressiveLazyLoading();
  121 |       this.lazyLoadTriggerMethods = configuration.getLazyLoadTriggerMethods();
  122 |       this.objectFactory = objectFactory;
  123 |       this.constructorArgTypes = constructorArgTypes;
  124 |       this.constructorArgs = constructorArgs;
  125 |     }
  126 | 
  127 |     public static Object createProxy(Object target, ResultLoaderMap lazyLoader, Configuration configuration,
  128 |         ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
  129 |       final Class<?> type = target.getClass();
  130 |       EnhancedResultObjectProxyImpl callback = new EnhancedResultObjectProxyImpl(type, lazyLoader, configuration,
  131 |           objectFactory, constructorArgTypes, constructorArgs);
  132 |       Object enhanced = createStaticProxy(type, callback, constructorArgTypes, constructorArgs);
  133 |       PropertyCopier.copyBeanProperties(type, target, enhanced);
  134 |       return enhanced;
  135 |     }
  136 | 
  137 |     @Override
  138 |     public Object intercept(Object enhanced, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
  139 |       final String methodName = method.getName();
  140 |       lock.lock();
  141 |       try {
  142 |         if (WRITE_REPLACE_METHOD.equals(methodName)) {
  143 |           Object original;
  144 |           if (constructorArgTypes.isEmpty()) {
  145 |             original = objectFactory.create(type);
  146 |           } else {
  147 |             original = objectFactory.create(type, constructorArgTypes, constructorArgs);
  148 |           }
  149 |           PropertyCopier.copyBeanProperties(type, enhanced, original);
  150 |           if (lazyLoader.size() > 0) {
  151 |             return new CglibSerialStateHolder(original, lazyLoader.getProperties(), objectFactory, constructorArgTypes,
  152 |                 constructorArgs);
  153 |           } else {
  154 |             return original;
  155 |           }
  156 |         }
  157 |         if (lazyLoader.size() > 0 && !FINALIZE_METHOD.equals(methodName)) {
  158 |           if (aggressive || lazyLoadTriggerMethods.contains(methodName)) {
  159 |             lazyLoader.loadAll();
  160 |           } else if (PropertyNamer.isSetter(methodName)) {
  161 |             final String property = PropertyNamer.methodToProperty(methodName);
  162 |             lazyLoader.remove(property);
  163 |           } else if (PropertyNamer.isGetter(methodName)) {
  164 |             final String property = PropertyNamer.methodToProperty(methodName);
  165 |             if (lazyLoader.hasLoader(property)) {
  166 |               lazyLoader.load(property);
  167 |             }
  168 |           }
  169 |         }
  170 |         return methodProxy.invokeSuper(enhanced, args);
  171 |       } catch (Throwable t) {
  172 |         throw ExceptionUtil.unwrapThrowable(t);
  173 |       } finally {
  174 |         lock.unlock();
  175 |         ErrorContext.instance().reset();
  176 |       }
  177 |     }
  178 |   }
  179 | 
  180 |   private static class EnhancedDeserializationProxyImpl extends AbstractEnhancedDeserializationProxy
  181 |       implements MethodInterceptor {
  182 | 
  183 |     private EnhancedDeserializationProxyImpl(Class<?> type, Map<String, ResultLoaderMap.LoadPair> unloadedProperties,
  184 |         ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
  185 |       super(type, unloadedProperties, objectFactory, constructorArgTypes, constructorArgs);
  186 |     }
  187 | 
  188 |     public static Object createProxy(Object target, Map<String, ResultLoaderMap.LoadPair> unloadedProperties,
  189 |         ObjectFactory objectFactory, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
  190 |       final Class<?> type = target.getClass();
  191 |       EnhancedDeserializationProxyImpl callback = new EnhancedDeserializationProxyImpl(type, unloadedProperties,
  192 |           objectFactory, constructorArgTypes, constructorArgs);
  193 |       Object enhanced = createStaticProxy(type, callback, constructorArgTypes, constructorArgs);
  194 |       PropertyCopier.copyBeanProperties(type, target, enhanced);
  195 |       return enhanced;
  196 |     }
  197 | 
  198 |     @Override
  199 |     public Object intercept(Object enhanced, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
  200 |       final Object o = super.invoke(enhanced, method, args);
  201 |       return o instanceof AbstractSerialStateHolder ? o : methodProxy.invokeSuper(o, args);
  202 |     }
  203 | 
  204 |     @Override
  205 |     protected AbstractSerialStateHolder newSerialStateHolder(Object userBean,
  206 |         Map<String, ResultLoaderMap.LoadPair> unloadedProperties, ObjectFactory objectFactory,
  207 |         List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
  208 |       return new CglibSerialStateHolder(userBean, unloadedProperties, objectFactory, constructorArgTypes,
  209 |           constructorArgs);
  210 |     }
  211 |   }
  212 | 
  213 |   private static class LogHolder {
  214 |     private static final Log log = LogFactory.getLog(CglibProxyFactory.class);
  215 |   }
  216 | 
  217 | }
```
