<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.nacos:nacos-client:3.3.0 | path: com/alibaba/nacos/client/naming/cache/ServiceInfoHolder.java -->
<!-- 正文引用：[[源码:ServiceInfoHolder.java#L起-L止]] ；代码块头部：(src: ServiceInfoHolder.java#L起-L止) -->

# ServiceInfoHolder.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.nacos:nacos-client:3.3.0` |
| 路径 | `com/alibaba/nacos/client/naming/cache/ServiceInfoHolder.java` |
| 行数 | 215 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/nacos/nacos-client/3.3.0/nacos-client-3.3.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:10.346Z |

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
   17 | package com.alibaba.nacos.client.naming.cache;
   18 | 
   19 | import com.alibaba.nacos.api.PropertyKeyConst;
   20 | import com.alibaba.nacos.api.exception.NacosException;
   21 | import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
   22 | import com.alibaba.nacos.api.naming.utils.NamingUtils;
   23 | import com.alibaba.nacos.client.env.NacosClientProperties;
   24 | import com.alibaba.nacos.client.monitor.MetricsMonitor;
   25 | import com.alibaba.nacos.client.naming.backups.FailoverReactor;
   26 | import com.alibaba.nacos.client.naming.event.InstancesChangeEvent;
   27 | import com.alibaba.nacos.client.naming.event.InstancesDiff;
   28 | import com.alibaba.nacos.client.naming.utils.CacheDirUtil;
   29 | import com.alibaba.nacos.common.lifecycle.Closeable;
   30 | import com.alibaba.nacos.common.notify.NotifyCenter;
   31 | import com.alibaba.nacos.common.utils.ConvertUtils;
   32 | import com.alibaba.nacos.api.utils.json.JsonUtils;
   33 | import com.alibaba.nacos.common.utils.StringUtils;
   34 | 
   35 | import java.util.Map;
   36 | import java.util.concurrent.ConcurrentHashMap;
   37 | import java.util.concurrent.ConcurrentMap;
   38 | 
   39 | import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;
   40 | 
   41 | /**
   42 |  * Naming client service information holder.
   43 |  *
   44 |  * @author xiweng.yy
   45 |  */
   46 | public class ServiceInfoHolder implements Closeable {
   47 |     
   48 |     private final ConcurrentMap<String, ServiceInfo> serviceInfoMap;
   49 |     
   50 |     private final FailoverReactor failoverReactor;
   51 |     
   52 |     private final boolean pushEmptyProtection;
   53 |     
   54 |     private final InstancesDiffer instancesDiffer;
   55 |     
   56 |     private final ServiceInfoDiskCacheRefresher serviceInfoDiskCacheRefresher;
   57 |     
   58 |     private String cacheDir;
   59 |     
   60 |     private String notifierEventScope;
   61 |     
   62 |     private boolean enableClientMetrics = true;
   63 |     
   64 |     public ServiceInfoHolder(String namespace, String notifierEventScope,
   65 |         NacosClientProperties properties) {
   66 |         cacheDir = CacheDirUtil.initCacheDir(namespace, properties);
   67 |         instancesDiffer = new InstancesDiffer();
   68 |         if (isLoadCacheAtStart(properties)) {
   69 |             this.serviceInfoMap = new ConcurrentHashMap<>(DiskCache.read(this.cacheDir));
   70 |         } else {
   71 |             this.serviceInfoMap = new ConcurrentHashMap<>(16);
   72 |         }
   73 |         this.failoverReactor = new FailoverReactor(this, notifierEventScope);
   74 |         this.serviceInfoDiskCacheRefresher = new ServiceInfoDiskCacheRefresher();
   75 |         this.pushEmptyProtection = isPushEmptyProtect(properties);
   76 |         this.notifierEventScope = notifierEventScope;
   77 |         this.enableClientMetrics = Boolean.parseBoolean(
   78 |             properties.getProperty(PropertyKeyConst.ENABLE_CLIENT_METRICS, "true"));
   79 |     }
   80 |     
   81 |     private boolean isLoadCacheAtStart(NacosClientProperties properties) {
   82 |         boolean loadCacheAtStart = false;
   83 |         if (properties != null && StringUtils.isNotEmpty(
   84 |             properties.getProperty(PropertyKeyConst.NAMING_LOAD_CACHE_AT_START))) {
   85 |             loadCacheAtStart = ConvertUtils.toBoolean(
   86 |                 properties.getProperty(PropertyKeyConst.NAMING_LOAD_CACHE_AT_START));
   87 |         }
   88 |         return loadCacheAtStart;
   89 |     }
   90 |     
   91 |     private boolean isPushEmptyProtect(NacosClientProperties properties) {
   92 |         boolean pushEmptyProtection = false;
   93 |         if (properties != null && StringUtils.isNotEmpty(
   94 |             properties.getProperty(PropertyKeyConst.NAMING_PUSH_EMPTY_PROTECTION))) {
   95 |             pushEmptyProtection = ConvertUtils.toBoolean(
   96 |                 properties.getProperty(PropertyKeyConst.NAMING_PUSH_EMPTY_PROTECTION));
   97 |         }
   98 |         return pushEmptyProtection;
   99 |     }
  100 |     
  101 |     public Map<String, ServiceInfo> getServiceInfoMap() {
  102 |         return serviceInfoMap;
  103 |     }
  104 |     
  105 |     public ServiceInfo getServiceInfo(final String serviceName, final String groupName) {
  106 |         String key = NamingUtils.getGroupedName(serviceName, groupName);
  107 |         ServiceInfo serviceInfo = serviceInfoMap.get(key);
  108 |         return serviceInfo == null ? null : serviceInfo.clone();
  109 |     }
  110 |     
  111 |     /**
  112 |      * Process service json.
  113 |      *
  114 |      * @param json service json
  115 |      * @return service info
  116 |      */
  117 |     public ServiceInfo processServiceInfo(String json) {
  118 |         ServiceInfo serviceInfo = JsonUtils.toObj(json, ServiceInfo.class);
  119 |         serviceInfo.setJsonFromServer(json);
  120 |         return processServiceInfo(serviceInfo);
  121 |     }
  122 |     
  123 |     /**
  124 |      * Process service info.
  125 |      *
  126 |      * @param serviceInfo new service info
  127 |      * @return service info
  128 |      */
  129 |     public ServiceInfo processServiceInfo(ServiceInfo serviceInfo) {
  130 |         String serviceKey = serviceInfo.getKeyWithoutClusters();
  131 |         if (serviceKey == null) {
  132 |             NAMING_LOGGER.warn("process service info but serviceKey is null, service host: {}",
  133 |                 JsonUtils.toJson(serviceInfo.getHosts()));
  134 |             return null;
  135 |         }
  136 |         ServiceInfo oldService = serviceInfoMap.get(serviceKey);
  137 |         if (isEmptyOrErrorPush(serviceInfo)) {
  138 |             //empty or error push, just ignore
  139 |             NAMING_LOGGER.warn(
  140 |                 "process service info but found empty or error push, serviceKey: {}, "
  141 |                     + "pushEmptyProtection: {}, hosts: {}",
  142 |                 serviceKey, pushEmptyProtection, serviceInfo.getHosts());
  143 |             return oldService;
  144 |         }
  145 |         serviceInfoMap.put(serviceKey, serviceInfo);
  146 |         InstancesDiff diff = getServiceInfoDiff(oldService, serviceInfo);
  147 |         if (StringUtils.isBlank(serviceInfo.getJsonFromServer())) {
  148 |             serviceInfo.setJsonFromServer(JsonUtils.toJson(serviceInfo));
  149 |         }
  150 |         
  151 |         if (enableClientMetrics) {
  152 |             try {
  153 |                 MetricsMonitor.recordServiceInfoMapSize(serviceInfoMap.size());
  154 |             } catch (Throwable t) {
  155 |                 NAMING_LOGGER.error("Failed to update metrics for service info map size", t);
  156 |             }
  157 |         }
  158 |         
  159 |         if (diff.hasDifferent()) {
  160 |             NAMING_LOGGER.info("current ips:({}) service: {} -> {}", serviceInfo.ipCount(),
  161 |                 serviceKey,
  162 |                 JsonUtils.toJson(serviceInfo.getHosts()));
  163 |             
  164 |             if (!failoverReactor.isFailoverSwitch(serviceKey)) {
  165 |                 NotifyCenter.publishEvent(
  166 |                     new InstancesChangeEvent(notifierEventScope, serviceInfo.getName(),
  167 |                         serviceInfo.getGroupName(),
  168 |                         serviceInfo.getClusters(), serviceInfo.getHosts(), diff));
  169 |             }
  170 |             publishDiskCacheRefreshEvent(serviceKey, serviceInfo);
  171 |         }
  172 |         return serviceInfo;
  173 |     }
  174 |     
  175 |     /**
  176 |      * Publish a disk cache refresh event for async persistence.
  177 |      *
  178 |      * @param serviceKey service key without clusters
  179 |      * @param serviceInfo latest service info snapshot
  180 |      */
  181 |     private void publishDiskCacheRefreshEvent(String serviceKey, ServiceInfo serviceInfo) {
  182 |         serviceInfoDiskCacheRefresher.publishEvent(
  183 |             new ServiceInfoDiskCacheRefreshEvent(serviceKey, serviceInfo, cacheDir));
  184 |     }
  185 |     
  186 |     private boolean isEmptyOrErrorPush(ServiceInfo serviceInfo) {
  187 |         return null == serviceInfo.getHosts() || (pushEmptyProtection && !serviceInfo.validate());
  188 |     }
  189 |     
  190 |     private InstancesDiff getServiceInfoDiff(ServiceInfo oldService, ServiceInfo newService) {
  191 |         return instancesDiffer.doDiff(oldService, newService);
  192 |     }
  193 |     
  194 |     public String getCacheDir() {
  195 |         return cacheDir;
  196 |     }
  197 |     
  198 |     public boolean isFailoverSwitch() {
  199 |         return failoverReactor.isFailoverSwitch();
  200 |     }
  201 |     
  202 |     public ServiceInfo getFailoverServiceInfo(final String serviceName, final String groupName) {
  203 |         String key = NamingUtils.getGroupedName(serviceName, groupName);
  204 |         return failoverReactor.getService(key);
  205 |     }
  206 |     
  207 |     @Override
  208 |     public void shutdown() throws NacosException {
  209 |         String className = this.getClass().getName();
  210 |         NAMING_LOGGER.info("{} do shutdown begin", className);
  211 |         failoverReactor.shutdown();
  212 |         serviceInfoDiskCacheRefresher.shutdown();
  213 |         NAMING_LOGGER.info("{} do shutdown stop", className);
  214 |     }
  215 | }
```
