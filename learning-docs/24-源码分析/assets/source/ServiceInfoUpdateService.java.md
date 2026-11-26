<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.nacos:nacos-client:3.3.0 | path: com/alibaba/nacos/client/naming/core/ServiceInfoUpdateService.java -->
<!-- 正文引用：[[源码:ServiceInfoUpdateService.java#L起-L止]] ；代码块头部：(src: ServiceInfoUpdateService.java#L起-L止) -->

# ServiceInfoUpdateService.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.nacos:nacos-client:3.3.0` |
| 路径 | `com/alibaba/nacos/client/naming/core/ServiceInfoUpdateService.java` |
| 行数 | 266 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/nacos/nacos-client/3.3.0/nacos-client-3.3.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:10.861Z |

```java
    1 | /*
    2 |  * Copyright 1999-2018 Alibaba Group Holding Ltd.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *      http://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | 
   17 | package com.alibaba.nacos.client.naming.core;
   18 | 
   19 | import com.alibaba.nacos.api.PropertyKeyConst;
   20 | import com.alibaba.nacos.api.exception.NacosException;
   21 | import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
   22 | import com.alibaba.nacos.api.naming.utils.NamingUtils;
   23 | import com.alibaba.nacos.client.env.NacosClientProperties;
   24 | import com.alibaba.nacos.client.naming.cache.ServiceInfoHolder;
   25 | import com.alibaba.nacos.client.naming.event.InstancesChangeNotifier;
   26 | import com.alibaba.nacos.client.naming.remote.NamingClientProxy;
   27 | import com.alibaba.nacos.common.executor.NameThreadFactory;
   28 | import com.alibaba.nacos.common.lifecycle.Closeable;
   29 | import com.alibaba.nacos.common.utils.CollectionUtils;
   30 | import com.alibaba.nacos.common.utils.ConvertUtils;
   31 | import com.alibaba.nacos.common.utils.ThreadUtils;
   32 | 
   33 | import java.util.HashMap;
   34 | import java.util.Map;
   35 | import java.util.concurrent.ScheduledExecutorService;
   36 | import java.util.concurrent.ScheduledFuture;
   37 | import java.util.concurrent.ScheduledThreadPoolExecutor;
   38 | import java.util.concurrent.TimeUnit;
   39 | 
   40 | import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;
   41 | 
   42 | /**
   43 |  * Service information update service.
   44 |  *
   45 |  * @author xiweng.yy
   46 |  */
   47 | public class ServiceInfoUpdateService implements Closeable {
   48 |     
   49 |     private static final long DEFAULT_DELAY = 1000L;
   50 |     
   51 |     private static final int DEFAULT_UPDATE_CACHE_TIME_MULTIPLE = 6;
   52 |     
   53 |     private static final int MIN_THREAD_NUM = 1;
   54 |     
   55 |     private final Map<String, ScheduledFuture<?>> futureMap = new HashMap<>();
   56 |     
   57 |     private final ServiceInfoHolder serviceInfoHolder;
   58 |     
   59 |     private final ScheduledExecutorService executor;
   60 |     
   61 |     private final NamingClientProxy namingClientProxy;
   62 |     
   63 |     private final InstancesChangeNotifier changeNotifier;
   64 |     
   65 |     private final boolean asyncQuerySubscribeService;
   66 |     
   67 |     public ServiceInfoUpdateService(NacosClientProperties properties,
   68 |         ServiceInfoHolder serviceInfoHolder,
   69 |         NamingClientProxy namingClientProxy, InstancesChangeNotifier changeNotifier) {
   70 |         this.asyncQuerySubscribeService = isAsyncQueryForSubscribeService(properties);
   71 |         this.executor = new ScheduledThreadPoolExecutor(initPollingThreadCount(properties),
   72 |             new NameThreadFactory("com.alibaba.nacos.client.naming.updater"));
   73 |         this.serviceInfoHolder = serviceInfoHolder;
   74 |         this.namingClientProxy = namingClientProxy;
   75 |         this.changeNotifier = changeNotifier;
   76 |     }
   77 |     
   78 |     private boolean isAsyncQueryForSubscribeService(NacosClientProperties properties) {
   79 |         if (properties == null
   80 |             || !properties.containsKey(PropertyKeyConst.NAMING_ASYNC_QUERY_SUBSCRIBE_SERVICE)) {
   81 |             return false;
   82 |         }
   83 |         return ConvertUtils.toBoolean(
   84 |             properties.getProperty(PropertyKeyConst.NAMING_ASYNC_QUERY_SUBSCRIBE_SERVICE),
   85 |             false);
   86 |     }
   87 |     
   88 |     private int initPollingThreadCount(NacosClientProperties properties) {
   89 |         int count = ThreadUtils.getSuitableThreadCount(1) > 1
   90 |             ? ThreadUtils.getSuitableThreadCount(1) / 2 : 1;
   91 |         if (properties == null) {
   92 |             return count;
   93 |         }
   94 |         count = Math.min(
   95 |             properties.getInteger(PropertyKeyConst.NAMING_POLLING_MAX_THREAD_COUNT, count),
   96 |             count);
   97 |         count = Math.max(count, MIN_THREAD_NUM);
   98 |         return properties.getInteger(PropertyKeyConst.NAMING_POLLING_THREAD_COUNT, count);
   99 |     }
  100 |     
  101 |     /**
  102 |      * Schedule update if absent.
  103 |      *
  104 |      * @param serviceName service name
  105 |      * @param groupName   group name
  106 |      * @param clusters    clusters
  107 |      */
  108 |     public void scheduleUpdateIfAbsent(String serviceName, String groupName, String clusters) {
  109 |         if (!asyncQuerySubscribeService) {
  110 |             return;
  111 |         }
  112 |         String serviceKey =
  113 |             ServiceInfo.getKey(NamingUtils.getGroupedName(serviceName, groupName), clusters);
  114 |         if (futureMap.get(serviceKey) != null) {
  115 |             return;
  116 |         }
  117 |         synchronized (futureMap) {
  118 |             if (futureMap.get(serviceKey) != null) {
  119 |                 return;
  120 |             }
  121 |             
  122 |             ScheduledFuture<?> future = addTask(new UpdateTask(serviceName, groupName, clusters));
  123 |             futureMap.put(serviceKey, future);
  124 |         }
  125 |     }
  126 |     
  127 |     private synchronized ScheduledFuture<?> addTask(UpdateTask task) {
  128 |         return executor.schedule(task, DEFAULT_DELAY, TimeUnit.MILLISECONDS);
  129 |     }
  130 |     
  131 |     /**
  132 |      * Stop to schedule update if contain task.
  133 |      *
  134 |      * @param serviceName service name
  135 |      * @param groupName   group name
  136 |      * @param clusters    clusters
  137 |      */
  138 |     public void stopUpdateIfContain(String serviceName, String groupName, String clusters) {
  139 |         String serviceKey =
  140 |             ServiceInfo.getKey(NamingUtils.getGroupedName(serviceName, groupName), clusters);
  141 |         if (!futureMap.containsKey(serviceKey)) {
  142 |             return;
  143 |         }
  144 |         synchronized (futureMap) {
  145 |             if (!futureMap.containsKey(serviceKey)) {
  146 |                 return;
  147 |             }
  148 |             futureMap.remove(serviceKey);
  149 |         }
  150 |     }
  151 |     
  152 |     @Override
  153 |     public void shutdown() throws NacosException {
  154 |         String className = this.getClass().getName();
  155 |         NAMING_LOGGER.info("{} do shutdown begin", className);
  156 |         ThreadUtils.shutdownThreadPool(executor, NAMING_LOGGER);
  157 |         NAMING_LOGGER.info("{} do shutdown stop", className);
  158 |     }
  159 |     
  160 |     public class UpdateTask implements Runnable {
  161 |         
  162 |         long lastRefTime = Long.MAX_VALUE;
  163 |         
  164 |         private boolean isCancel;
  165 |         
  166 |         private final String serviceName;
  167 |         
  168 |         private final String groupName;
  169 |         
  170 |         private final String clusters;
  171 |         
  172 |         private final String groupedServiceName;
  173 |         
  174 |         private final String serviceKey;
  175 |         
  176 |         /**
  177 |          * the fail situation. 1:can't connect to server 2:serviceInfo's hosts is empty
  178 |          */
  179 |         private int failCount = 0;
  180 |         
  181 |         public UpdateTask(String serviceName, String groupName, String clusters) {
  182 |             this.serviceName = serviceName;
  183 |             this.groupName = groupName;
  184 |             this.clusters = clusters;
  185 |             this.groupedServiceName = NamingUtils.getGroupedName(serviceName, groupName);
  186 |             this.serviceKey = ServiceInfo.getKey(groupedServiceName, clusters);
  187 |         }
  188 |         
  189 |         @Override
  190 |         public void run() {
  191 |             long delayTime = DEFAULT_DELAY;
  192 |             
  193 |             try {
  194 |                 if (!changeNotifier.isSubscribed(groupName, serviceName) && !futureMap.containsKey(
  195 |                     serviceKey)) {
  196 |                     NAMING_LOGGER.info("update task is stopped, service:{}, clusters:{}",
  197 |                         groupedServiceName, clusters);
  198 |                     isCancel = true;
  199 |                     return;
  200 |                 }
  201 |                 
  202 |                 ServiceInfo serviceObj = serviceInfoHolder.getServiceInfoMap().get(serviceKey);
  203 |                 if (serviceObj == null) {
  204 |                     serviceObj = namingClientProxy.queryInstancesOfService(serviceName, groupName,
  205 |                         clusters, false);
  206 |                     serviceInfoHolder.processServiceInfo(serviceObj);
  207 |                     // TODO multiple time can be configured.
  208 |                     delayTime = serviceObj.getCacheMillis() * DEFAULT_UPDATE_CACHE_TIME_MULTIPLE;
  209 |                     lastRefTime = serviceObj.getLastRefTime();
  210 |                     return;
  211 |                 }
  212 |                 
  213 |                 if (serviceObj.getLastRefTime() <= lastRefTime) {
  214 |                     serviceObj = namingClientProxy.queryInstancesOfService(serviceName, groupName,
  215 |                         clusters, false);
  216 |                     serviceInfoHolder.processServiceInfo(serviceObj);
  217 |                 }
  218 |                 lastRefTime = serviceObj.getLastRefTime();
  219 |                 if (CollectionUtils.isEmpty(serviceObj.getHosts())) {
  220 |                     incFailCount();
  221 |                     return;
  222 |                 }
  223 |                 // TODO multiple time can be configured.
  224 |                 delayTime = serviceObj.getCacheMillis() * DEFAULT_UPDATE_CACHE_TIME_MULTIPLE;
  225 |                 resetFailCount();
  226 |             } catch (NacosException e) {
  227 |                 handleNacosException(e);
  228 |             } catch (Throwable e) {
  229 |                 handleUnknownException(e);
  230 |             } finally {
  231 |                 if (!isCancel) {
  232 |                     executor.schedule(this, Math.min(delayTime << failCount, DEFAULT_DELAY * 60),
  233 |                         TimeUnit.MILLISECONDS);
  234 |                 }
  235 |             }
  236 |         }
  237 |         
  238 |         private void handleNacosException(NacosException e) {
  239 |             incFailCount();
  240 |             int errorCode = e.getErrCode();
  241 |             if (NacosException.SERVER_ERROR == errorCode) {
  242 |                 handleUnknownException(e);
  243 |             }
  244 |             NAMING_LOGGER.warn("Can't update serviceName: {}, reason: {}", groupedServiceName,
  245 |                 e.getErrMsg());
  246 |         }
  247 |         
  248 |         private void handleUnknownException(Throwable throwable) {
  249 |             incFailCount();
  250 |             NAMING_LOGGER.warn("[NA] failed to update serviceName: {}", groupedServiceName,
  251 |                 throwable);
  252 |         }
  253 |         
  254 |         private void incFailCount() {
  255 |             int limit = 6;
  256 |             if (failCount == limit) {
  257 |                 return;
  258 |             }
  259 |             failCount++;
  260 |         }
  261 |         
  262 |         private void resetFailCount() {
  263 |             failCount = 0;
  264 |         }
  265 |     }
  266 | }
```
