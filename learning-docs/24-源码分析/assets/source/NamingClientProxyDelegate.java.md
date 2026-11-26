<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.nacos:nacos-client:3.3.0 | path: com/alibaba/nacos/client/naming/remote/NamingClientProxyDelegate.java -->
<!-- 正文引用：[[源码:NamingClientProxyDelegate.java#L起-L止]] ；代码块头部：(src: NamingClientProxyDelegate.java#L起-L止) -->

# NamingClientProxyDelegate.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.nacos:nacos-client:3.3.0` |
| 路径 | `com/alibaba/nacos/client/naming/remote/NamingClientProxyDelegate.java` |
| 行数 | 238 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/nacos/nacos-client/3.3.0/nacos-client-3.3.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:10.462Z |

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
   17 | package com.alibaba.nacos.client.naming.remote;
   18 | 
   19 | import com.alibaba.nacos.api.ability.constant.AbilityKey;
   20 | import com.alibaba.nacos.api.exception.NacosException;
   21 | import com.alibaba.nacos.api.naming.pojo.Instance;
   22 | import com.alibaba.nacos.api.naming.pojo.ListView;
   23 | import com.alibaba.nacos.api.naming.pojo.Service;
   24 | import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
   25 | import com.alibaba.nacos.api.naming.utils.NamingUtils;
   26 | import com.alibaba.nacos.api.selector.AbstractSelector;
   27 | import com.alibaba.nacos.client.env.NacosClientProperties;
   28 | import com.alibaba.nacos.client.naming.cache.NamingFuzzyWatchServiceListHolder;
   29 | import com.alibaba.nacos.client.naming.cache.ServiceInfoHolder;
   30 | import com.alibaba.nacos.client.naming.core.NamingServerListManager;
   31 | import com.alibaba.nacos.client.naming.core.ServiceInfoUpdateService;
   32 | import com.alibaba.nacos.client.naming.event.InstancesChangeNotifier;
   33 | import com.alibaba.nacos.client.naming.remote.gprc.NamingGrpcClientProxy;
   34 | import com.alibaba.nacos.client.naming.remote.http.NamingHttpClientManager;
   35 | import com.alibaba.nacos.client.naming.remote.http.NamingHttpClientProxy;
   36 | import com.alibaba.nacos.client.security.SecurityProxy;
   37 | import com.alibaba.nacos.common.executor.NameThreadFactory;
   38 | import com.alibaba.nacos.common.utils.CollectionUtils;
   39 | import com.alibaba.nacos.common.utils.ThreadUtils;
   40 | 
   41 | import java.util.List;
   42 | import java.util.Properties;
   43 | import java.util.concurrent.ScheduledExecutorService;
   44 | import java.util.concurrent.ScheduledThreadPoolExecutor;
   45 | import java.util.concurrent.TimeUnit;
   46 | 
   47 | import static com.alibaba.nacos.client.constant.Constants.Security.SECURITY_INFO_REFRESH_INTERVAL_MILLS;
   48 | import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;
   49 | 
   50 | /**
   51 |  * Delegate of naming client proxy.
   52 |  *
   53 |  * @author xiweng.yy
   54 |  */
   55 | public class NamingClientProxyDelegate implements NamingClientProxy {
   56 |     
   57 |     private final NamingServerListManager serverListManager;
   58 |     
   59 |     private final ServiceInfoUpdateService serviceInfoUpdateService;
   60 |     
   61 |     private final ServiceInfoHolder serviceInfoHolder;
   62 |     
   63 |     private final NamingHttpClientProxy httpClientProxy;
   64 |     
   65 |     private final NamingGrpcClientProxy grpcClientProxy;
   66 |     
   67 |     private final SecurityProxy securityProxy;
   68 |     
   69 |     private ScheduledExecutorService executorService;
   70 |     
   71 |     public NamingClientProxyDelegate(String namespace, ServiceInfoHolder serviceInfoHolder,
   72 |         NacosClientProperties properties, InstancesChangeNotifier changeNotifier,
   73 |         NamingFuzzyWatchServiceListHolder namingFuzzyWatchServiceListHolder)
   74 |         throws NacosException {
   75 |         this.serviceInfoUpdateService =
   76 |             new ServiceInfoUpdateService(properties, serviceInfoHolder, this,
   77 |                 changeNotifier);
   78 |         this.serverListManager = new NamingServerListManager(properties, namespace);
   79 |         this.serverListManager.start();
   80 |         this.serviceInfoHolder = serviceInfoHolder;
   81 |         this.securityProxy = new SecurityProxy(this.serverListManager,
   82 |             NamingHttpClientManager.getInstance().getNacosRestTemplate());
   83 |         initSecurityProxy(properties);
   84 |         this.httpClientProxy =
   85 |             new NamingHttpClientProxy(namespace, securityProxy, serverListManager, properties);
   86 |         this.grpcClientProxy =
   87 |             new NamingGrpcClientProxy(namespace, securityProxy, serverListManager, properties,
   88 |                 serviceInfoHolder, namingFuzzyWatchServiceListHolder);
   89 |     }
   90 |     
   91 |     private void initSecurityProxy(NacosClientProperties properties) {
   92 |         this.executorService = new ScheduledThreadPoolExecutor(1,
   93 |             new NameThreadFactory("com.alibaba.nacos.client.naming.security"));
   94 |         final Properties nacosClientPropertiesView = properties.asProperties();
   95 |         this.securityProxy.login(nacosClientPropertiesView);
   96 |         this.executorService.scheduleWithFixedDelay(
   97 |             () -> securityProxy.login(nacosClientPropertiesView), 0,
   98 |             SECURITY_INFO_REFRESH_INTERVAL_MILLS, TimeUnit.MILLISECONDS);
   99 |     }
  100 |     
  101 |     @Override
  102 |     public void registerService(String serviceName, String groupName, Instance instance)
  103 |         throws NacosException {
  104 |         getExecuteClientProxy(instance).registerService(serviceName, groupName, instance);
  105 |     }
  106 |     
  107 |     @Override
  108 |     public void batchRegisterService(String serviceName, String groupName, List<Instance> instances)
  109 |         throws NacosException {
  110 |         NAMING_LOGGER.info("batchRegisterInstance instances: {} ,serviceName: {} begin.", instances,
  111 |             serviceName);
  112 |         if (CollectionUtils.isEmpty(instances)) {
  113 |             NAMING_LOGGER.warn("batchRegisterInstance instances is Empty:{}", instances);
  114 |         }
  115 |         grpcClientProxy.batchRegisterService(serviceName, groupName, instances);
  116 |         NAMING_LOGGER.info("batchRegisterInstance instances: {} ,serviceName: {} finish.",
  117 |             instances, serviceName);
  118 |     }
  119 |     
  120 |     @Override
  121 |     public void batchDeregisterService(String serviceName, String groupName,
  122 |         List<Instance> instances)
  123 |         throws NacosException {
  124 |         NAMING_LOGGER.info("batch DeregisterInstance instances: {} ,serviceName: {} begin.",
  125 |             instances, serviceName);
  126 |         if (CollectionUtils.isEmpty(instances)) {
  127 |             NAMING_LOGGER.warn("batch DeregisterInstance instances is Empty:{}", instances);
  128 |         }
  129 |         grpcClientProxy.batchDeregisterService(serviceName, groupName, instances);
  130 |         NAMING_LOGGER.info("batch DeregisterInstance instances: {} ,serviceName: {} finish.",
  131 |             instances, serviceName);
  132 |     }
  133 |     
  134 |     @Override
  135 |     public void deregisterService(String serviceName, String groupName, Instance instance)
  136 |         throws NacosException {
  137 |         getExecuteClientProxy(instance).deregisterService(serviceName, groupName, instance);
  138 |     }
  139 |     
  140 |     @Override
  141 |     public void updateInstance(String serviceName, String groupName, Instance instance)
  142 |         throws NacosException {
  143 |         
  144 |     }
  145 |     
  146 |     @Override
  147 |     public ServiceInfo queryInstancesOfService(String serviceName, String groupName,
  148 |         String clusters,
  149 |         boolean healthyOnly) throws NacosException {
  150 |         return grpcClientProxy.queryInstancesOfService(serviceName, groupName, clusters,
  151 |             healthyOnly);
  152 |     }
  153 |     
  154 |     @Override
  155 |     public Service queryService(String serviceName, String groupName) throws NacosException {
  156 |         return null;
  157 |     }
  158 |     
  159 |     @Override
  160 |     public void createService(Service service, AbstractSelector selector) throws NacosException {
  161 |         
  162 |     }
  163 |     
  164 |     @Override
  165 |     public boolean deleteService(String serviceName, String groupName) throws NacosException {
  166 |         return false;
  167 |     }
  168 |     
  169 |     @Override
  170 |     public void updateService(Service service, AbstractSelector selector) throws NacosException {
  171 |         
  172 |     }
  173 |     
  174 |     @Override
  175 |     public ListView<String> getServiceList(int pageNo, int pageSize, String groupName,
  176 |         AbstractSelector selector)
  177 |         throws NacosException {
  178 |         return grpcClientProxy.getServiceList(pageNo, pageSize, groupName, selector);
  179 |     }
  180 |     
  181 |     @Override
  182 |     public ServiceInfo subscribe(String serviceName, String groupName, String clusters)
  183 |         throws NacosException {
  184 |         NAMING_LOGGER.info("[SUBSCRIBE-SERVICE] service:{}, group:{}, clusters:{} ", serviceName,
  185 |             groupName, clusters);
  186 |         String serviceNameWithGroup = NamingUtils.getGroupedName(serviceName, groupName);
  187 |         String serviceKey = ServiceInfo.getKey(serviceNameWithGroup, clusters);
  188 |         serviceInfoUpdateService.scheduleUpdateIfAbsent(serviceName, groupName, clusters);
  189 |         ServiceInfo result = serviceInfoHolder.getServiceInfoMap().get(serviceKey);
  190 |         if (null == result || !isSubscribed(serviceName, groupName, clusters)) {
  191 |             result = grpcClientProxy.subscribe(serviceName, groupName, clusters);
  192 |         }
  193 |         serviceInfoHolder.processServiceInfo(result);
  194 |         return result;
  195 |     }
  196 |     
  197 |     @Override
  198 |     public void unsubscribe(String serviceName, String groupName, String clusters)
  199 |         throws NacosException {
  200 |         NAMING_LOGGER.debug("[UNSUBSCRIBE-SERVICE] service:{}, group:{}, cluster:{} ", serviceName,
  201 |             groupName,
  202 |             clusters);
  203 |         serviceInfoUpdateService.stopUpdateIfContain(serviceName, groupName, clusters);
  204 |         grpcClientProxy.unsubscribe(serviceName, groupName, clusters);
  205 |     }
  206 |     
  207 |     @Override
  208 |     public boolean isSubscribed(String serviceName, String groupName, String clusters)
  209 |         throws NacosException {
  210 |         return grpcClientProxy.isSubscribed(serviceName, groupName, clusters);
  211 |     }
  212 |     
  213 |     @Override
  214 |     public boolean serverHealthy() {
  215 |         return grpcClientProxy.serverHealthy() || httpClientProxy.serverHealthy();
  216 |     }
  217 |     
  218 |     private NamingClientProxy getExecuteClientProxy(Instance instance) {
  219 |         if (instance.isEphemeral() || grpcClientProxy.isAbilitySupportedByServer(
  220 |             AbilityKey.SERVER_PERSISTENT_INSTANCE_BY_GRPC)) {
  221 |             return grpcClientProxy;
  222 |         }
  223 |         return httpClientProxy;
  224 |     }
  225 |     
  226 |     @Override
  227 |     public void shutdown() throws NacosException {
  228 |         String className = this.getClass().getName();
  229 |         NAMING_LOGGER.info("{} do shutdown begin", className);
  230 |         serviceInfoUpdateService.shutdown();
  231 |         serverListManager.shutdown();
  232 |         httpClientProxy.shutdown();
  233 |         grpcClientProxy.shutdown();
  234 |         securityProxy.shutdown();
  235 |         ThreadUtils.shutdownThreadPool(executorService, NAMING_LOGGER);
  236 |         NAMING_LOGGER.info("{} do shutdown stop", className);
  237 |     }
  238 | }
```
