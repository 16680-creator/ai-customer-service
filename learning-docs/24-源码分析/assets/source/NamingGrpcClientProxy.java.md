<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.nacos:nacos-client:3.3.0 | path: com/alibaba/nacos/client/naming/remote/gprc/NamingGrpcClientProxy.java -->
<!-- 正文引用：[[源码:NamingGrpcClientProxy.java#L起-L止]] ；代码块头部：(src: NamingGrpcClientProxy.java#L起-L止) -->

# NamingGrpcClientProxy.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.nacos:nacos-client:3.3.0` |
| 路径 | `com/alibaba/nacos/client/naming/remote/gprc/NamingGrpcClientProxy.java` |
| 行数 | 610 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/nacos/nacos-client/3.3.0/nacos-client-3.3.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:10.594Z |

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
   17 | package com.alibaba.nacos.client.naming.remote.gprc;
   18 | 
   19 | import com.alibaba.nacos.api.PropertyKeyConst;
   20 | import com.alibaba.nacos.api.ability.constant.AbilityKey;
   21 | import com.alibaba.nacos.api.ability.constant.AbilityStatus;
   22 | import com.alibaba.nacos.api.common.Constants;
   23 | import com.alibaba.nacos.api.exception.NacosException;
   24 | import com.alibaba.nacos.api.naming.CommonParams;
   25 | import com.alibaba.nacos.api.naming.pojo.Instance;
   26 | import com.alibaba.nacos.api.naming.pojo.ListView;
   27 | import com.alibaba.nacos.api.naming.pojo.Service;
   28 | import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
   29 | import com.alibaba.nacos.api.naming.remote.NamingRemoteConstants;
   30 | import com.alibaba.nacos.api.naming.remote.request.AbstractNamingRequest;
   31 | import com.alibaba.nacos.api.naming.remote.request.BatchInstanceRequest;
   32 | import com.alibaba.nacos.api.naming.remote.request.InstanceRequest;
   33 | import com.alibaba.nacos.api.naming.remote.request.NamingFuzzyWatchRequest;
   34 | import com.alibaba.nacos.api.naming.remote.request.PersistentInstanceRequest;
   35 | import com.alibaba.nacos.api.naming.remote.request.ServiceListRequest;
   36 | import com.alibaba.nacos.api.naming.remote.request.ServiceQueryRequest;
   37 | import com.alibaba.nacos.api.naming.remote.request.SubscribeServiceRequest;
   38 | import com.alibaba.nacos.api.naming.remote.response.BatchInstanceResponse;
   39 | import com.alibaba.nacos.api.naming.remote.response.NamingFuzzyWatchResponse;
   40 | import com.alibaba.nacos.api.naming.remote.response.QueryServiceResponse;
   41 | import com.alibaba.nacos.api.naming.remote.response.ServiceListResponse;
   42 | import com.alibaba.nacos.api.naming.remote.response.SubscribeServiceResponse;
   43 | import com.alibaba.nacos.api.naming.utils.NamingUtils;
   44 | import com.alibaba.nacos.api.remote.RemoteConstants;
   45 | import com.alibaba.nacos.api.remote.request.Request;
   46 | import com.alibaba.nacos.api.remote.response.Response;
   47 | import com.alibaba.nacos.api.remote.response.ResponseCode;
   48 | import com.alibaba.nacos.api.selector.AbstractSelector;
   49 | import com.alibaba.nacos.api.selector.SelectorType;
   50 | import com.alibaba.nacos.client.address.ServerListChangeEvent;
   51 | import com.alibaba.nacos.client.env.NacosClientProperties;
   52 | import com.alibaba.nacos.client.monitor.MetricsMonitor;
   53 | import com.alibaba.nacos.client.naming.cache.NamingFuzzyWatchServiceListHolder;
   54 | import com.alibaba.nacos.client.naming.cache.ServiceInfoHolder;
   55 | import com.alibaba.nacos.client.naming.remote.AbstractNamingClientProxy;
   56 | import com.alibaba.nacos.client.naming.remote.gprc.redo.NamingGrpcRedoService;
   57 | import com.alibaba.nacos.client.naming.remote.gprc.redo.data.BatchInstanceRedoData;
   58 | import com.alibaba.nacos.client.naming.remote.gprc.redo.data.InstanceRedoData;
   59 | import com.alibaba.nacos.client.security.SecurityProxy;
   60 | import com.alibaba.nacos.client.utils.AppNameUtils;
   61 | import com.alibaba.nacos.common.notify.Event;
   62 | import com.alibaba.nacos.common.notify.NotifyCenter;
   63 | import com.alibaba.nacos.common.remote.ConnectionType;
   64 | import com.alibaba.nacos.common.remote.client.RpcClient;
   65 | import com.alibaba.nacos.common.remote.client.RpcClientConfigFactory;
   66 | import com.alibaba.nacos.common.remote.client.RpcClientFactory;
   67 | import com.alibaba.nacos.common.remote.client.ServerListFactory;
   68 | import com.alibaba.nacos.common.remote.client.grpc.GrpcClientConfig;
   69 | import com.alibaba.nacos.common.utils.CollectionUtils;
   70 | import com.alibaba.nacos.api.utils.json.JsonUtils;
   71 | 
   72 | import java.util.ArrayList;
   73 | import java.util.HashMap;
   74 | import java.util.Iterator;
   75 | import java.util.List;
   76 | import java.util.Map;
   77 | import java.util.Objects;
   78 | import java.util.UUID;
   79 | import java.util.function.Function;
   80 | import java.util.stream.Collectors;
   81 | 
   82 | import static com.alibaba.nacos.api.remote.RemoteConstants.MONITOR_LABEL_NONE;
   83 | import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;
   84 | 
   85 | /**
   86 |  * Naming grpc client proxy.
   87 |  *
   88 |  * @author xiweng.yy
   89 |  */
   90 | public class NamingGrpcClientProxy extends AbstractNamingClientProxy {
   91 |     
   92 |     private final String namespaceId;
   93 |     
   94 |     private final String uuid;
   95 |     
   96 |     private final Long requestTimeout;
   97 |     
   98 |     private final RpcClient rpcClient;
   99 |     
  100 |     private final NamingGrpcRedoService redoService;
  101 |     
  102 |     private boolean enableClientMetrics = true;
  103 |     
  104 |     public NamingGrpcClientProxy(String namespaceId, SecurityProxy securityProxy,
  105 |         ServerListFactory serverListFactory,
  106 |         NacosClientProperties properties, ServiceInfoHolder serviceInfoHolder,
  107 |         NamingFuzzyWatchServiceListHolder namingFuzzyWatchServiceListHolder)
  108 |         throws NacosException {
  109 |         super(securityProxy);
  110 |         this.namespaceId = namespaceId;
  111 |         this.uuid = UUID.randomUUID().toString();
  112 |         this.requestTimeout =
  113 |             Long.parseLong(properties.getProperty(CommonParams.NAMING_REQUEST_TIMEOUT, "-1"));
  114 |         Map<String, String> labels = new HashMap<>();
  115 |         labels.put(RemoteConstants.LABEL_SOURCE, RemoteConstants.LABEL_SOURCE_SDK);
  116 |         labels.put(RemoteConstants.LABEL_MODULE, RemoteConstants.LABEL_MODULE_NAMING);
  117 |         labels.put(Constants.APPNAME, AppNameUtils.getAppName());
  118 |         namingFuzzyWatchServiceListHolder.registerNamingGrpcClientProxy(this);
  119 |         GrpcClientConfig grpcClientConfig = RpcClientConfigFactory.getInstance()
  120 |             .createGrpcClientConfig(properties.asProperties(), labels);
  121 |         this.rpcClient = RpcClientFactory.createClient(uuid, ConnectionType.GRPC, grpcClientConfig);
  122 |         this.redoService =
  123 |             new NamingGrpcRedoService(this, namingFuzzyWatchServiceListHolder, properties);
  124 |         this.enableClientMetrics = Boolean.parseBoolean(
  125 |             properties.getProperty(PropertyKeyConst.ENABLE_CLIENT_METRICS, "true"));
  126 |         NAMING_LOGGER.info("Create naming rpc client for uuid->{}", uuid);
  127 |         start(serverListFactory, serviceInfoHolder, namingFuzzyWatchServiceListHolder);
  128 |     }
  129 |     
  130 |     private void start(ServerListFactory serverListFactory, ServiceInfoHolder serviceInfoHolder,
  131 |         NamingFuzzyWatchServiceListHolder namingFuzzyWatchServiceListHolder)
  132 |         throws NacosException {
  133 |         rpcClient.serverListFactory(serverListFactory);
  134 |         rpcClient.registerConnectionListener(redoService);
  135 |         rpcClient.registerServerRequestHandler(new NamingPushRequestHandler(serviceInfoHolder));
  136 |         rpcClient.registerServerRequestHandler(
  137 |             new NamingFuzzyWatchNotifyRequestHandler(namingFuzzyWatchServiceListHolder));
  138 |         rpcClient.start();
  139 |         namingFuzzyWatchServiceListHolder.start();
  140 |         NotifyCenter.registerSubscriber(this);
  141 |     }
  142 |     
  143 |     @Override
  144 |     public void onEvent(ServerListChangeEvent event) {
  145 |         rpcClient.onServerListChange();
  146 |     }
  147 |     
  148 |     @Override
  149 |     public Class<? extends Event> subscribeType() {
  150 |         return ServerListChangeEvent.class;
  151 |     }
  152 |     
  153 |     @Override
  154 |     public void registerService(String serviceName, String groupName, Instance instance)
  155 |         throws NacosException {
  156 |         NAMING_LOGGER.info("[REGISTER-SERVICE] {} registering service {} with instance {}",
  157 |             namespaceId, serviceName,
  158 |             instance);
  159 |         if (instance.isEphemeral()) {
  160 |             registerServiceForEphemeral(serviceName, groupName, instance);
  161 |         } else {
  162 |             doRegisterServiceForPersistent(serviceName, groupName, instance);
  163 |         }
  164 |     }
  165 |     
  166 |     private void registerServiceForEphemeral(String serviceName, String groupName,
  167 |         Instance instance)
  168 |         throws NacosException {
  169 |         redoService.cacheInstanceForRedo(serviceName, groupName, instance);
  170 |         doRegisterService(serviceName, groupName, instance);
  171 |     }
  172 |     
  173 |     @Override
  174 |     public void batchRegisterService(String serviceName, String groupName, List<Instance> instances)
  175 |         throws NacosException {
  176 |         redoService.cacheInstanceForRedo(serviceName, groupName, instances);
  177 |         doBatchRegisterService(serviceName, groupName, instances);
  178 |     }
  179 |     
  180 |     @Override
  181 |     public void batchDeregisterService(String serviceName, String groupName,
  182 |         List<Instance> instances)
  183 |         throws NacosException {
  184 |         synchronized (redoService.getRegisteredInstances()) {
  185 |             List<Instance> retainInstance = getRetainInstance(serviceName, groupName, instances);
  186 |             batchRegisterService(serviceName, groupName, retainInstance);
  187 |         }
  188 |     }
  189 |     
  190 |     /**
  191 |      * Get instance list that need to be Retained.
  192 |      *
  193 |      * @param serviceName         service name
  194 |      * @param groupName           group name
  195 |      * @param deRegisterInstances deregister instance list
  196 |      * @return instance list that need to be retained.
  197 |      */
  198 |     private List<Instance> getRetainInstance(String serviceName, String groupName,
  199 |         List<Instance> deRegisterInstances)
  200 |         throws NacosException {
  201 |         if (CollectionUtils.isEmpty(deRegisterInstances)) {
  202 |             throw new NacosException(NacosException.INVALID_PARAM,
  203 |                 String.format(
  204 |                     "[Batch deRegistration] need deRegister instance is empty, instances: %s,",
  205 |                     deRegisterInstances));
  206 |         }
  207 |         String combinedServiceName = NamingUtils.getGroupedName(serviceName, groupName);
  208 |         InstanceRedoData instanceRedoData =
  209 |             redoService.getRegisteredInstancesByKey(combinedServiceName);
  210 |         if (!(instanceRedoData instanceof BatchInstanceRedoData)) {
  211 |             throw new NacosException(NacosException.INVALID_PARAM, String.format(
  212 |                 "[Batch deRegistration] batch deRegister is not BatchInstanceRedoData type , instances: %s,",
  213 |                 deRegisterInstances));
  214 |         }
  215 |         
  216 |         BatchInstanceRedoData batchInstanceRedoData = (BatchInstanceRedoData) instanceRedoData;
  217 |         List<Instance> allRedoInstances = batchInstanceRedoData.getInstances();
  218 |         if (CollectionUtils.isEmpty(allRedoInstances)) {
  219 |             throw new NacosException(NacosException.INVALID_PARAM, String.format(
  220 |                 "[Batch deRegistration] not found all registerInstance , serviceName：%s , groupName: %s",
  221 |                 serviceName, groupName));
  222 |         }
  223 |         
  224 |         Map<Instance, Instance> deRegisterInstanceMap = deRegisterInstances.stream()
  225 |             .collect(Collectors.toMap(Function.identity(), Function.identity()));
  226 |         List<Instance> retainInstances = new ArrayList<>();
  227 |         for (Instance redoInstance : allRedoInstances) {
  228 |             boolean needRetained = true;
  229 |             Iterator<Map.Entry<Instance, Instance>> it =
  230 |                 deRegisterInstanceMap.entrySet().iterator();
  231 |             while (it.hasNext()) {
  232 |                 Instance deRegisterInstance = it.next().getKey();
  233 |                 // only compare Ip & Port because redoInstance's instanceId or serviceName might be null but deRegisterInstance's might not be null.
  234 |                 if (compareIpAndPort(deRegisterInstance, redoInstance)) {
  235 |                     needRetained = false;
  236 |                     // clear current entry to speed up next redoInstance comparing.
  237 |                     it.remove();
  238 |                     break;
  239 |                 }
  240 |             }
  241 |             if (needRetained) {
  242 |                 retainInstances.add(redoInstance);
  243 |             }
  244 |         }
  245 |         return retainInstances;
  246 |     }
  247 |     
  248 |     private boolean compareIpAndPort(Instance deRegisterInstance, Instance redoInstance) {
  249 |         return ((deRegisterInstance.getIp().equals(redoInstance.getIp()))
  250 |             && (deRegisterInstance.getPort() == redoInstance.getPort()));
  251 |     }
  252 |     
  253 |     /**
  254 |      * Execute batch register operation.
  255 |      *
  256 |      * @param serviceName service name
  257 |      * @param groupName   group name
  258 |      * @param instances   instances
  259 |      * @throws NacosException NacosException
  260 |      */
  261 |     public void doBatchRegisterService(String serviceName, String groupName,
  262 |         List<Instance> instances)
  263 |         throws NacosException {
  264 |         BatchInstanceRequest request = new BatchInstanceRequest(namespaceId, serviceName, groupName,
  265 |             NamingRemoteConstants.BATCH_REGISTER_INSTANCE, instances);
  266 |         long connectionEpoch = redoService.getCurrentConnectionEpoch();
  267 |         requestToServer(request, BatchInstanceResponse.class);
  268 |         redoService.instanceRegistered(serviceName, groupName, connectionEpoch);
  269 |     }
  270 |     
  271 |     /**
  272 |      * Execute register operation.
  273 |      *
  274 |      * @param serviceName name of service
  275 |      * @param groupName   group of service
  276 |      * @param instance    instance to register
  277 |      * @throws NacosException nacos exception
  278 |      */
  279 |     public void doRegisterService(String serviceName, String groupName, Instance instance)
  280 |         throws NacosException {
  281 |         InstanceRequest request = new InstanceRequest(namespaceId, serviceName, groupName,
  282 |             NamingRemoteConstants.REGISTER_INSTANCE, instance);
  283 |         long connectionEpoch = redoService.getCurrentConnectionEpoch();
  284 |         requestToServer(request, Response.class);
  285 |         redoService.instanceRegistered(serviceName, groupName, connectionEpoch);
  286 |     }
  287 |     
  288 |     /**
  289 |      * Execute register operation for persistent instance.
  290 |      *
  291 |      * @param serviceName name of service
  292 |      * @param groupName   group of service
  293 |      * @param instance    instance to register
  294 |      * @throws NacosException nacos exception
  295 |      */
  296 |     public void doRegisterServiceForPersistent(String serviceName, String groupName,
  297 |         Instance instance)
  298 |         throws NacosException {
  299 |         PersistentInstanceRequest request =
  300 |             new PersistentInstanceRequest(namespaceId, serviceName, groupName,
  301 |                 NamingRemoteConstants.REGISTER_INSTANCE, instance);
  302 |         requestToServer(request, Response.class);
  303 |     }
  304 |     
  305 |     @Override
  306 |     public void deregisterService(String serviceName, String groupName, Instance instance)
  307 |         throws NacosException {
  308 |         NAMING_LOGGER.info("[DEREGISTER-SERVICE] {} deregistering service {} with instance: {}",
  309 |             namespaceId,
  310 |             serviceName, instance);
  311 |         if (instance.isEphemeral()) {
  312 |             deregisterServiceForEphemeral(serviceName, groupName, instance);
  313 |         } else {
  314 |             doDeregisterServiceForPersistent(serviceName, groupName, instance);
  315 |         }
  316 |     }
  317 |     
  318 |     private void deregisterServiceForEphemeral(String serviceName, String groupName,
  319 |         Instance instance)
  320 |         throws NacosException {
  321 |         String key = NamingUtils.getGroupedName(serviceName, groupName);
  322 |         InstanceRedoData instanceRedoData = redoService.getRegisteredInstancesByKey(key);
  323 |         if (instanceRedoData instanceof BatchInstanceRedoData) {
  324 |             List<Instance> instances = new ArrayList<>();
  325 |             if (null != instance) {
  326 |                 instances.add(instance);
  327 |             }
  328 |             batchDeregisterService(serviceName, groupName, instances);
  329 |         } else {
  330 |             redoService.instanceDeregister(serviceName, groupName);
  331 |             doDeregisterService(serviceName, groupName, instance);
  332 |         }
  333 |     }
  334 |     
  335 |     /**
  336 |      * Execute deregister operation.
  337 |      *
  338 |      * @param serviceName service name
  339 |      * @param groupName   group name
  340 |      * @param instance    instance
  341 |      * @throws NacosException nacos exception
  342 |      */
  343 |     public void doDeregisterService(String serviceName, String groupName, Instance instance)
  344 |         throws NacosException {
  345 |         InstanceRequest request = new InstanceRequest(namespaceId, serviceName, groupName,
  346 |             NamingRemoteConstants.DE_REGISTER_INSTANCE, instance);
  347 |         requestToServer(request, Response.class);
  348 |         redoService.instanceDeregistered(serviceName, groupName);
  349 |     }
  350 |     
  351 |     /**
  352 |      * Execute deregister operation for persistent instance.
  353 |      *
  354 |      * @param serviceName service name
  355 |      * @param groupName   group name
  356 |      * @param instance    instance
  357 |      * @throws NacosException nacos exception
  358 |      */
  359 |     public void doDeregisterServiceForPersistent(String serviceName, String groupName,
  360 |         Instance instance)
  361 |         throws NacosException {
  362 |         PersistentInstanceRequest request =
  363 |             new PersistentInstanceRequest(namespaceId, serviceName, groupName,
  364 |                 NamingRemoteConstants.DE_REGISTER_INSTANCE, instance);
  365 |         requestToServer(request, Response.class);
  366 |     }
  367 |     
  368 |     @Override
  369 |     public void updateInstance(String serviceName, String groupName, Instance instance)
  370 |         throws NacosException {
  371 |     }
  372 |     
  373 |     @Override
  374 |     public ServiceInfo queryInstancesOfService(String serviceName, String groupName,
  375 |         String clusters,
  376 |         boolean healthyOnly) throws NacosException {
  377 |         ServiceQueryRequest request = new ServiceQueryRequest(namespaceId, serviceName, groupName);
  378 |         request.setCluster(clusters);
  379 |         request.setHealthyOnly(healthyOnly);
  380 |         QueryServiceResponse response = requestToServer(request, QueryServiceResponse.class);
  381 |         return response.getServiceInfo();
  382 |     }
  383 |     
  384 |     @Override
  385 |     public Service queryService(String serviceName, String groupName) throws NacosException {
  386 |         return null;
  387 |     }
  388 |     
  389 |     @Override
  390 |     public void createService(Service service, AbstractSelector selector) throws NacosException {
  391 |     }
  392 |     
  393 |     @Override
  394 |     public boolean deleteService(String serviceName, String groupName) throws NacosException {
  395 |         return false;
  396 |     }
  397 |     
  398 |     @Override
  399 |     public void updateService(Service service, AbstractSelector selector) throws NacosException {
  400 |     }
  401 |     
  402 |     @Override
  403 |     public ListView<String> getServiceList(int pageNo, int pageSize, String groupName,
  404 |         AbstractSelector selector)
  405 |         throws NacosException {
  406 |         ServiceListRequest request =
  407 |             new ServiceListRequest(namespaceId, groupName, pageNo, pageSize);
  408 |         if (selector != null) {
  409 |             if (SelectorType.valueOf(selector.getType()) == SelectorType.label) {
  410 |                 request.setSelector(JsonUtils.toJson(selector));
  411 |             }
  412 |         }
  413 |         ServiceListResponse response = requestToServer(request, ServiceListResponse.class);
  414 |         ListView<String> result = new ListView<>();
  415 |         result.setCount(response.getCount());
  416 |         result.setData(response.getServiceNames());
  417 |         return result;
  418 |     }
  419 |     
  420 |     @Override
  421 |     public ServiceInfo subscribe(String serviceName, String groupName, String clusters)
  422 |         throws NacosException {
  423 |         NAMING_LOGGER.info("[GRPC-SUBSCRIBE] {} service:{}, group:{}, cluster:{} ", namespaceId,
  424 |             serviceName,
  425 |             groupName, clusters);
  426 |         redoService.cacheSubscriberForRedo(serviceName, groupName, clusters);
  427 |         return doSubscribe(serviceName, groupName, clusters);
  428 |     }
  429 |     
  430 |     /**
  431 |      * Execute subscribe operation.
  432 |      *
  433 |      * @param serviceName service name
  434 |      * @param groupName   group name
  435 |      * @param clusters    clusters, current only support subscribe all clusters, maybe deprecated
  436 |      * @return current service info of subscribe service
  437 |      * @throws NacosException nacos exception
  438 |      */
  439 |     public ServiceInfo doSubscribe(String serviceName, String groupName, String clusters)
  440 |         throws NacosException {
  441 |         SubscribeServiceRequest request =
  442 |             new SubscribeServiceRequest(namespaceId, groupName, serviceName, clusters,
  443 |                 true);
  444 |         SubscribeServiceResponse response =
  445 |             requestToServer(request, SubscribeServiceResponse.class);
  446 |         redoService.subscriberRegistered(serviceName, groupName, clusters);
  447 |         return response.getServiceInfo();
  448 |     }
  449 |     
  450 |     @Override
  451 |     public void unsubscribe(String serviceName, String groupName, String clusters)
  452 |         throws NacosException {
  453 |         NAMING_LOGGER.info("[GRPC-UNSUBSCRIBE] {} service:{}, group:{}, cluster:{} ", namespaceId,
  454 |             serviceName,
  455 |             groupName, clusters);
  456 |         redoService.subscriberDeregister(serviceName, groupName, clusters);
  457 |         doUnsubscribe(serviceName, groupName, clusters);
  458 |     }
  459 |     
  460 |     @Override
  461 |     public boolean isSubscribed(String serviceName, String groupName, String clusters)
  462 |         throws NacosException {
  463 |         return redoService.isSubscriberRegistered(serviceName, groupName, clusters);
  464 |     }
  465 |     
  466 |     /**
  467 |      * Execute unsubscribe operation.
  468 |      *
  469 |      * @param serviceName service name
  470 |      * @param groupName   group name
  471 |      * @param clusters    clusters, current only support subscribe all clusters, maybe deprecated
  472 |      * @throws NacosException nacos exception
  473 |      */
  474 |     public void doUnsubscribe(String serviceName, String groupName, String clusters)
  475 |         throws NacosException {
  476 |         SubscribeServiceRequest request =
  477 |             new SubscribeServiceRequest(namespaceId, groupName, serviceName, clusters,
  478 |                 false);
  479 |         requestToServer(request, SubscribeServiceResponse.class);
  480 |         redoService.removeSubscriberForRedo(serviceName, groupName, clusters);
  481 |     }
  482 |     
  483 |     @Override
  484 |     public boolean serverHealthy() {
  485 |         return rpcClient.isRunning();
  486 |     }
  487 |     
  488 |     /**
  489 |      * Determine whether nacos-server supports the capability.
  490 |      *
  491 |      * @param abilityKey ability key
  492 |      * @return true if supported, otherwise false
  493 |      */
  494 |     public boolean isAbilitySupportedByServer(AbilityKey abilityKey) {
  495 |         return rpcClient.getConnectionAbility(abilityKey) == AbilityStatus.SUPPORTED;
  496 |     }
  497 |     
  498 |     /**
  499 |      * Execute unsubscribe operation.
  500 |      *
  501 |      * @param namingFuzzyWatchRequest namingFuzzyWatchRequest
  502 |      * @throws NacosException nacos exception
  503 |      */
  504 |     public NamingFuzzyWatchResponse fuzzyWatchRequest(
  505 |         NamingFuzzyWatchRequest namingFuzzyWatchRequest)
  506 |         throws NacosException {
  507 |         return requestToServer(namingFuzzyWatchRequest, NamingFuzzyWatchResponse.class);
  508 |     }
  509 |     
  510 |     private <T extends Response> T requestToServer(Request request, Class<T> responseClass)
  511 |         throws NacosException {
  512 |         Response response = null;
  513 |         try {
  514 |             if (request instanceof AbstractNamingRequest) {
  515 |                 request.putAllHeader(
  516 |                     getSecurityHeaders(((AbstractNamingRequest) request).getNamespace(),
  517 |                         ((AbstractNamingRequest) request).getGroupName(),
  518 |                         ((AbstractNamingRequest) request).getServiceName()));
  519 |             } else if (request instanceof NamingFuzzyWatchRequest) {
  520 |                 request.putAllHeader(
  521 |                     getSecurityHeaders(((NamingFuzzyWatchRequest) request).getNamespace(), null,
  522 |                         null));
  523 |             } else {
  524 |                 throw new NacosException(400, "unknown naming request type");
  525 |             }
  526 |             
  527 |             response = requestTimeout < 0 ? rpcClient.request(request)
  528 |                 : rpcClient.request(request, requestTimeout);
  529 |             if (ResponseCode.SUCCESS.getCode() != response.getResultCode()) {
  530 |                 // If the 403 login operation is triggered, refresh the accessToken of the client
  531 |                 if (NacosException.NO_RIGHT == response.getErrorCode()) {
  532 |                     reLogin();
  533 |                 }
  534 |                 throw new NacosException(response.getErrorCode(), response.getMessage());
  535 |             }
  536 |             if (responseClass.isAssignableFrom(response.getClass())) {
  537 |                 return (T) response;
  538 |             }
  539 |             NAMING_LOGGER.error(
  540 |                 "Server return unexpected response '{}', expected response should be '{}'",
  541 |                 response.getClass().getName(), responseClass.getName());
  542 |             throw new NacosException(NacosException.SERVER_ERROR, "Server return invalid response");
  543 |         } catch (NacosException e) {
  544 |             recordRequestFailedMetrics(request, e, response);
  545 |             throw e;
  546 |         } catch (Exception e) {
  547 |             recordRequestFailedMetrics(request, e, response);
  548 |             throw new NacosException(NacosException.SERVER_ERROR, "Request nacos server failed: ",
  549 |                 e);
  550 |         }
  551 |     }
  552 |     
  553 |     /**
  554 |      * Records registration metrics for a service instance.
  555 |      *
  556 |      * @param request   The registration request object.
  557 |      * @param exception The Exception encountered during the registration process, or null if registration was
  558 |      *                  successful.
  559 |      * @param response  The response object containing registration result information, or null if registration failed.
  560 |      */
  561 |     private void recordRequestFailedMetrics(Request request, Exception exception,
  562 |         Response response) {
  563 |         if (!enableClientMetrics) {
  564 |             return;
  565 |         }
  566 |         
  567 |         try {
  568 |             if (Objects.isNull(response)) {
  569 |                 MetricsMonitor.recordNamingRequestFailed(request.getClass().getSimpleName(),
  570 |                     MONITOR_LABEL_NONE,
  571 |                     MONITOR_LABEL_NONE, exception.getClass().getSimpleName());
  572 |             } else {
  573 |                 MetricsMonitor.recordNamingRequestFailed(request.getClass().getSimpleName(),
  574 |                     String.valueOf(response.getResultCode()),
  575 |                     String.valueOf(response.getErrorCode()),
  576 |                     MONITOR_LABEL_NONE);
  577 |             }
  578 |         } catch (Throwable t) {
  579 |             NAMING_LOGGER.warn("Fail to record metrics for request {}",
  580 |                 request.getClass().getSimpleName(), t);
  581 |         }
  582 |     }
  583 |     
  584 |     @Override
  585 |     public void shutdown() throws NacosException {
  586 |         NAMING_LOGGER.info("Shutdown naming grpc client proxy for  uuid->{}", uuid);
  587 |         redoService.shutdown();
  588 |         shutDownAndRemove(uuid);
  589 |         NotifyCenter.deregisterSubscriber(this);
  590 |     }
  591 |     
  592 |     private void shutDownAndRemove(String uuid) {
  593 |         synchronized (RpcClientFactory.getAllClientEntries()) {
  594 |             try {
  595 |                 RpcClientFactory.destroyClient(uuid);
  596 |                 NAMING_LOGGER.info("shutdown and remove naming rpc client  for uuid ->{}", uuid);
  597 |             } catch (NacosException e) {
  598 |                 NAMING_LOGGER.warn("Fail to shutdown naming rpc client  for uuid ->{}", uuid);
  599 |             }
  600 |         }
  601 |     }
  602 |     
  603 |     public boolean isEnable() {
  604 |         return rpcClient.isRunning();
  605 |     }
  606 |     
  607 |     public String getNamespaceId() {
  608 |         return namespaceId;
  609 |     }
  610 | }
```
