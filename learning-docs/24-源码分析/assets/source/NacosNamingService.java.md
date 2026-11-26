<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.nacos:nacos-client:3.3.0 | path: com/alibaba/nacos/client/naming/NacosNamingService.java -->
<!-- 正文引用：[[源码:NacosNamingService.java#L起-L止]] ；代码块头部：(src: NacosNamingService.java#L起-L止) -->

# NacosNamingService.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.nacos:nacos-client:3.3.0` |
| 路径 | `com/alibaba/nacos/client/naming/NacosNamingService.java` |
| 行数 | 759 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/nacos/nacos-client/3.3.0/nacos-client-3.3.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:10.102Z |

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
   17 | package com.alibaba.nacos.client.naming;
   18 | 
   19 | import com.alibaba.nacos.api.PropertyKeyConst;
   20 | import com.alibaba.nacos.api.common.Constants;
   21 | import com.alibaba.nacos.api.exception.NacosException;
   22 | import com.alibaba.nacos.api.naming.NamingService;
   23 | import com.alibaba.nacos.api.naming.listener.EventListener;
   24 | import com.alibaba.nacos.api.naming.listener.FuzzyWatchEventWatcher;
   25 | import com.alibaba.nacos.api.naming.pojo.Instance;
   26 | import com.alibaba.nacos.api.naming.pojo.ListView;
   27 | import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
   28 | import com.alibaba.nacos.api.naming.selector.NamingContext;
   29 | import com.alibaba.nacos.api.naming.selector.NamingResult;
   30 | import com.alibaba.nacos.api.naming.selector.NamingSelector;
   31 | import com.alibaba.nacos.api.naming.utils.NamingUtils;
   32 | import com.alibaba.nacos.api.selector.AbstractSelector;
   33 | import com.alibaba.nacos.client.env.NacosClientProperties;
   34 | import com.alibaba.nacos.client.naming.cache.NamingFuzzyWatchContext;
   35 | import com.alibaba.nacos.client.naming.cache.NamingFuzzyWatchServiceListHolder;
   36 | import com.alibaba.nacos.client.naming.cache.ServiceInfoHolder;
   37 | import com.alibaba.nacos.client.naming.core.Balancer;
   38 | import com.alibaba.nacos.client.naming.event.InstancesChangeEvent;
   39 | import com.alibaba.nacos.client.naming.event.InstancesChangeNotifier;
   40 | import com.alibaba.nacos.client.naming.event.InstancesDiff;
   41 | import com.alibaba.nacos.client.naming.event.NamingFuzzyWatchNotifyEvent;
   42 | import com.alibaba.nacos.client.naming.remote.NamingClientProxy;
   43 | import com.alibaba.nacos.client.naming.remote.NamingClientProxyDelegate;
   44 | import com.alibaba.nacos.client.naming.selector.NamingSelectorFactory;
   45 | import com.alibaba.nacos.client.naming.selector.NamingSelectorWrapper;
   46 | import com.alibaba.nacos.client.naming.selector.ServiceInfoContext;
   47 | import com.alibaba.nacos.client.naming.utils.InitUtils;
   48 | import com.alibaba.nacos.client.naming.utils.UtilAndComs;
   49 | import com.alibaba.nacos.client.utils.ClientBasicParamUtil;
   50 | import com.alibaba.nacos.client.utils.PreInitUtils;
   51 | import com.alibaba.nacos.client.utils.ValidatorUtils;
   52 | import com.alibaba.nacos.common.notify.NotifyCenter;
   53 | import com.alibaba.nacos.common.utils.CollectionUtils;
   54 | import com.alibaba.nacos.common.utils.FuzzyGroupKeyPattern;
   55 | import com.alibaba.nacos.api.utils.json.JsonUtils;
   56 | import com.alibaba.nacos.common.utils.StringUtils;
   57 | 
   58 | import java.util.ArrayList;
   59 | import java.util.Iterator;
   60 | import java.util.List;
   61 | import java.util.Properties;
   62 | import java.util.UUID;
   63 | import java.util.concurrent.Future;
   64 | 
   65 | import static com.alibaba.nacos.api.common.Constants.ANY_PATTERN;
   66 | import static com.alibaba.nacos.client.naming.selector.NamingSelectorFactory.getUniqueClusterString;
   67 | import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;
   68 | 
   69 | /**
   70 |  * Nacos Naming Service.
   71 |  *
   72 |  * @author nkorange
   73 |  */
   74 | public class NacosNamingService implements NamingService {
   75 |     
   76 |     private static final String DEFAULT_NAMING_LOG_FILE_PATH = "naming.log";
   77 |     
   78 |     private static final String UP = "UP";
   79 |     
   80 |     private static final String DOWN = "DOWN";
   81 |     
   82 |     /**
   83 |      * Each Naming service should have different namespace.
   84 |      */
   85 |     private String namespace;
   86 |     
   87 |     @Deprecated
   88 |     private String logName;
   89 |     
   90 |     private ServiceInfoHolder serviceInfoHolder;
   91 |     
   92 |     private NamingFuzzyWatchServiceListHolder namingFuzzyWatchServiceListHolder;
   93 |     
   94 |     private InstancesChangeNotifier changeNotifier;
   95 |     
   96 |     private NamingClientProxy clientProxy;
   97 |     
   98 |     private String notifierEventScope;
   99 |     
  100 |     public NacosNamingService(String serverList) throws NacosException {
  101 |         Properties properties = new Properties();
  102 |         properties.setProperty(PropertyKeyConst.SERVER_ADDR, serverList);
  103 |         init(properties);
  104 |     }
  105 |     
  106 |     public NacosNamingService(Properties properties) throws NacosException {
  107 |         init(properties);
  108 |     }
  109 |     
  110 |     private void init(Properties properties) throws NacosException {
  111 |         PreInitUtils.asyncPreLoadCostComponent();
  112 |         final NacosClientProperties nacosClientProperties =
  113 |             NacosClientProperties.PROTOTYPE.derive(properties);
  114 |         NAMING_LOGGER.info(
  115 |             ClientBasicParamUtil.getInputParameters(nacosClientProperties.asProperties()));
  116 |         ValidatorUtils.checkInitParam(nacosClientProperties);
  117 |         this.namespace = InitUtils.initNamespaceForNaming(nacosClientProperties);
  118 |         InitUtils.initSerialization();
  119 |         InitUtils.initWebRootContext(nacosClientProperties);
  120 |         initLogName(nacosClientProperties);
  121 |         
  122 |         this.notifierEventScope = UUID.randomUUID().toString();
  123 |         this.changeNotifier = new InstancesChangeNotifier(this.notifierEventScope);
  124 |         NotifyCenter.registerToPublisher(InstancesChangeEvent.class, 16384);
  125 |         NotifyCenter.registerSubscriber(changeNotifier);
  126 |         this.serviceInfoHolder =
  127 |             new ServiceInfoHolder(namespace, this.notifierEventScope, nacosClientProperties);
  128 |         
  129 |         NotifyCenter.registerToPublisher(NamingFuzzyWatchNotifyEvent.class, 16384);
  130 |         this.namingFuzzyWatchServiceListHolder =
  131 |             new NamingFuzzyWatchServiceListHolder(this.notifierEventScope);
  132 |         
  133 |         this.clientProxy = new NamingClientProxyDelegate(this.namespace, serviceInfoHolder,
  134 |             nacosClientProperties,
  135 |             changeNotifier, namingFuzzyWatchServiceListHolder);
  136 |     }
  137 |     
  138 |     @Deprecated
  139 |     private void initLogName(NacosClientProperties properties) {
  140 |         logName = properties.getProperty(UtilAndComs.NACOS_NAMING_LOG_NAME,
  141 |             DEFAULT_NAMING_LOG_FILE_PATH);
  142 |     }
  143 |     
  144 |     @Override
  145 |     public void registerInstance(String serviceName, String ip, int port) throws NacosException {
  146 |         registerInstance(serviceName, ip, port, Constants.DEFAULT_CLUSTER_NAME);
  147 |     }
  148 |     
  149 |     @Override
  150 |     public void registerInstance(String serviceName, String groupName, String ip, int port)
  151 |         throws NacosException {
  152 |         registerInstance(serviceName, groupName, ip, port, Constants.DEFAULT_CLUSTER_NAME);
  153 |     }
  154 |     
  155 |     @Override
  156 |     public void registerInstance(String serviceName, String ip, int port, String clusterName)
  157 |         throws NacosException {
  158 |         registerInstance(serviceName, Constants.DEFAULT_GROUP, ip, port, clusterName);
  159 |     }
  160 |     
  161 |     @Override
  162 |     public void registerInstance(String serviceName, String groupName, String ip, int port,
  163 |         String clusterName)
  164 |         throws NacosException {
  165 |         Instance instance = new Instance();
  166 |         instance.setIp(ip);
  167 |         instance.setPort(port);
  168 |         instance.setWeight(1.0);
  169 |         instance.setClusterName(clusterName);
  170 |         registerInstance(serviceName, groupName, instance);
  171 |     }
  172 |     
  173 |     @Override
  174 |     public void registerInstance(String serviceName, Instance instance) throws NacosException {
  175 |         registerInstance(serviceName, Constants.DEFAULT_GROUP, instance);
  176 |     }
  177 |     
  178 |     @Override
  179 |     public void registerInstance(String serviceName, String groupName, Instance instance)
  180 |         throws NacosException {
  181 |         NamingUtils.checkInstanceIsLegal(instance);
  182 |         checkAndStripGroupNamePrefix(instance, groupName);
  183 |         clientProxy.registerService(serviceName, groupName, instance);
  184 |     }
  185 |     
  186 |     @Override
  187 |     public void batchRegisterInstance(String serviceName, String groupName,
  188 |         List<Instance> instances)
  189 |         throws NacosException {
  190 |         NamingUtils.batchCheckInstanceIsLegal(instances);
  191 |         batchCheckAndStripGroupNamePrefix(instances, groupName);
  192 |         clientProxy.batchRegisterService(serviceName, groupName, instances);
  193 |     }
  194 |     
  195 |     @Override
  196 |     public void batchDeregisterInstance(String serviceName, String groupName,
  197 |         List<Instance> instances)
  198 |         throws NacosException {
  199 |         NamingUtils.batchCheckInstanceIsLegal(instances);
  200 |         batchCheckAndStripGroupNamePrefix(instances, groupName);
  201 |         clientProxy.batchDeregisterService(serviceName, groupName, instances);
  202 |     }
  203 |     
  204 |     @Override
  205 |     public void deregisterInstance(String serviceName, String ip, int port) throws NacosException {
  206 |         deregisterInstance(serviceName, ip, port, Constants.DEFAULT_CLUSTER_NAME);
  207 |     }
  208 |     
  209 |     @Override
  210 |     public void deregisterInstance(String serviceName, String groupName, String ip, int port)
  211 |         throws NacosException {
  212 |         deregisterInstance(serviceName, groupName, ip, port, Constants.DEFAULT_CLUSTER_NAME);
  213 |     }
  214 |     
  215 |     @Override
  216 |     public void deregisterInstance(String serviceName, String ip, int port, String clusterName)
  217 |         throws NacosException {
  218 |         deregisterInstance(serviceName, Constants.DEFAULT_GROUP, ip, port, clusterName);
  219 |     }
  220 |     
  221 |     @Override
  222 |     public void deregisterInstance(String serviceName, String groupName, String ip, int port,
  223 |         String clusterName)
  224 |         throws NacosException {
  225 |         Instance instance = new Instance();
  226 |         instance.setIp(ip);
  227 |         instance.setPort(port);
  228 |         instance.setClusterName(clusterName);
  229 |         deregisterInstance(serviceName, groupName, instance);
  230 |     }
  231 |     
  232 |     @Override
  233 |     public void deregisterInstance(String serviceName, Instance instance) throws NacosException {
  234 |         deregisterInstance(serviceName, Constants.DEFAULT_GROUP, instance);
  235 |     }
  236 |     
  237 |     @Override
  238 |     public void deregisterInstance(String serviceName, String groupName, Instance instance)
  239 |         throws NacosException {
  240 |         NamingUtils.checkInstanceIsLegal(instance);
  241 |         checkAndStripGroupNamePrefix(instance, groupName);
  242 |         clientProxy.deregisterService(serviceName, groupName, instance);
  243 |     }
  244 |     
  245 |     @Override
  246 |     public List<Instance> getAllInstances(String serviceName) throws NacosException {
  247 |         return getAllInstances(serviceName, new ArrayList<>());
  248 |     }
  249 |     
  250 |     @Override
  251 |     public List<Instance> getAllInstances(String serviceName, String groupName)
  252 |         throws NacosException {
  253 |         return getAllInstances(serviceName, groupName, new ArrayList<>());
  254 |     }
  255 |     
  256 |     @Override
  257 |     public List<Instance> getAllInstances(String serviceName, boolean subscribe)
  258 |         throws NacosException {
  259 |         return getAllInstances(serviceName, new ArrayList<>(), subscribe);
  260 |     }
  261 |     
  262 |     @Override
  263 |     public List<Instance> getAllInstances(String serviceName, String groupName, boolean subscribe)
  264 |         throws NacosException {
  265 |         return getAllInstances(serviceName, groupName, new ArrayList<>(), subscribe);
  266 |     }
  267 |     
  268 |     @Override
  269 |     public List<Instance> getAllInstances(String serviceName, List<String> clusters)
  270 |         throws NacosException {
  271 |         return getAllInstances(serviceName, clusters, true);
  272 |     }
  273 |     
  274 |     @Override
  275 |     public List<Instance> getAllInstances(String serviceName, String groupName,
  276 |         List<String> clusters)
  277 |         throws NacosException {
  278 |         return getAllInstances(serviceName, groupName, clusters, true);
  279 |     }
  280 |     
  281 |     @Override
  282 |     public List<Instance> getAllInstances(String serviceName, List<String> clusters,
  283 |         boolean subscribe)
  284 |         throws NacosException {
  285 |         return getAllInstances(serviceName, Constants.DEFAULT_GROUP, clusters, subscribe);
  286 |     }
  287 |     
  288 |     @Override
  289 |     public List<Instance> getAllInstances(String serviceName, String groupName,
  290 |         List<String> clusters,
  291 |         boolean subscribe) throws NacosException {
  292 |         List<Instance> list;
  293 |         ServiceInfo serviceInfo = getServiceInfo(serviceName, groupName, clusters, subscribe);
  294 |         if (serviceInfo == null || CollectionUtils.isEmpty(list = serviceInfo.getHosts())) {
  295 |             return new ArrayList<>();
  296 |         }
  297 |         return list;
  298 |     }
  299 |     
  300 |     @Override
  301 |     public List<Instance> selectInstances(String serviceName, boolean healthy)
  302 |         throws NacosException {
  303 |         return selectInstances(serviceName, new ArrayList<>(), healthy);
  304 |     }
  305 |     
  306 |     @Override
  307 |     public List<Instance> selectInstances(String serviceName, String groupName, boolean healthy)
  308 |         throws NacosException {
  309 |         return selectInstances(serviceName, groupName, healthy, true);
  310 |     }
  311 |     
  312 |     @Override
  313 |     public List<Instance> selectInstances(String serviceName, boolean healthy, boolean subscribe)
  314 |         throws NacosException {
  315 |         return selectInstances(serviceName, new ArrayList<>(), healthy, subscribe);
  316 |     }
  317 |     
  318 |     @Override
  319 |     public List<Instance> selectInstances(String serviceName, String groupName, boolean healthy,
  320 |         boolean subscribe)
  321 |         throws NacosException {
  322 |         return selectInstances(serviceName, groupName, new ArrayList<>(), healthy, subscribe);
  323 |     }
  324 |     
  325 |     @Override
  326 |     public List<Instance> selectInstances(String serviceName, List<String> clusters,
  327 |         boolean healthy)
  328 |         throws NacosException {
  329 |         return selectInstances(serviceName, clusters, healthy, true);
  330 |     }
  331 |     
  332 |     @Override
  333 |     public List<Instance> selectInstances(String serviceName, String groupName,
  334 |         List<String> clusters, boolean healthy)
  335 |         throws NacosException {
  336 |         return selectInstances(serviceName, groupName, clusters, healthy, true);
  337 |     }
  338 |     
  339 |     @Override
  340 |     public List<Instance> selectInstances(String serviceName, List<String> clusters,
  341 |         boolean healthy, boolean subscribe)
  342 |         throws NacosException {
  343 |         return selectInstances(serviceName, Constants.DEFAULT_GROUP, clusters, healthy, subscribe);
  344 |     }
  345 |     
  346 |     @Override
  347 |     public List<Instance> selectInstances(String serviceName, String groupName,
  348 |         List<String> clusters, boolean healthy,
  349 |         boolean subscribe) throws NacosException {
  350 |         ServiceInfo serviceInfo = getServiceInfo(serviceName, groupName, clusters, subscribe);
  351 |         return selectInstances(serviceInfo, healthy);
  352 |     }
  353 |     
  354 |     private List<Instance> selectInstances(ServiceInfo serviceInfo, boolean healthy) {
  355 |         List<Instance> list;
  356 |         if (serviceInfo == null || CollectionUtils.isEmpty(list = serviceInfo.getHosts())) {
  357 |             return new ArrayList<>();
  358 |         }
  359 |         
  360 |         Iterator<Instance> iterator = list.iterator();
  361 |         while (iterator.hasNext()) {
  362 |             Instance instance = iterator.next();
  363 |             if (healthy != instance.isHealthy() || !instance.isEnabled()
  364 |                 || instance.getWeight() <= 0) {
  365 |                 iterator.remove();
  366 |             }
  367 |         }
  368 |         
  369 |         return list;
  370 |     }
  371 |     
  372 |     private ServiceInfo getServiceInfo(String serviceName, String groupName, List<String> clusters,
  373 |         boolean subscribe)
  374 |         throws NacosException {
  375 |         ServiceInfo serviceInfo;
  376 |         NamingSelector clusterSelector = NamingSelectorFactory.newClusterSelector(clusters);
  377 |         if (serviceInfoHolder.isFailoverSwitch()) {
  378 |             serviceInfo = getServiceInfoByFailover(serviceName, groupName, clusterSelector);
  379 |             if (serviceInfo != null && !serviceInfo.getHosts().isEmpty()) {
  380 |                 NAMING_LOGGER.debug("getServiceInfo from failover,serviceName: {}  data:{}",
  381 |                     serviceName,
  382 |                     JsonUtils.toJson(serviceInfo.getHosts()));
  383 |                 return serviceInfo;
  384 |             }
  385 |         }
  386 |         serviceInfo = getServiceInfoBySubscribe(serviceName, groupName, clusters, clusterSelector,
  387 |             subscribe);
  388 |         return serviceInfo;
  389 |     }
  390 |     
  391 |     private ServiceInfo getServiceInfoByFailover(String serviceName, String groupName,
  392 |         NamingSelector clusterSelector) {
  393 |         ServiceInfo result = serviceInfoHolder.getFailoverServiceInfo(serviceName, groupName);
  394 |         return doSelectInstance(result, clusterSelector);
  395 |     }
  396 |     
  397 |     private ServiceInfo getServiceInfoBySubscribe(String serviceName, String groupName,
  398 |         List<String> clusters,
  399 |         NamingSelector selector, boolean subscribe) throws NacosException {
  400 |         ServiceInfo serviceInfo;
  401 |         if (subscribe) {
  402 |             serviceInfo = serviceInfoHolder.getServiceInfo(serviceName, groupName);
  403 |             serviceInfo = tryToSubscribe(serviceName, groupName, serviceInfo);
  404 |             serviceInfo = doSelectInstance(serviceInfo, selector);
  405 |         } else {
  406 |             String clusterString = NamingSelectorFactory.getUniqueClusterString(clusters);
  407 |             serviceInfo = clientProxy.queryInstancesOfService(serviceName, groupName, clusterString,
  408 |                 false);
  409 |         }
  410 |         return serviceInfo;
  411 |     }
  412 |     
  413 |     private ServiceInfo tryToSubscribe(String serviceName, String groupName,
  414 |         ServiceInfo cachedServiceInfo)
  415 |         throws NacosException {
  416 |         // not found in cache, service never subscribed.
  417 |         if (null == cachedServiceInfo) {
  418 |             return clientProxy.subscribe(serviceName, groupName, StringUtils.EMPTY);
  419 |         }
  420 |         // found in cache, and subscribed.
  421 |         if (clientProxy.isSubscribed(serviceName, groupName, StringUtils.EMPTY)) {
  422 |             return cachedServiceInfo;
  423 |         }
  424 |         // found in cached, but not subscribed, such as cached from local file when starting.
  425 |         ServiceInfo result = cachedServiceInfo;
  426 |         try {
  427 |             result = clientProxy.subscribe(serviceName, groupName, StringUtils.EMPTY);
  428 |         } catch (NacosException e) {
  429 |             NAMING_LOGGER.warn("Subscribe from Server failed, will use local cache. fail message: ",
  430 |                 e);
  431 |         }
  432 |         return result;
  433 |     }
  434 |     
  435 |     private ServiceInfo doSelectInstance(ServiceInfo serviceInfo, NamingSelector clusterSelector) {
  436 |         if (null == serviceInfo) {
  437 |             return null;
  438 |         }
  439 |         NamingContext context = new ServiceInfoContext(serviceInfo);
  440 |         NamingResult result = clusterSelector.select(context);
  441 |         serviceInfo.setHosts(result.getResult());
  442 |         return serviceInfo;
  443 |     }
  444 |     
  445 |     @Override
  446 |     public Instance selectOneHealthyInstance(String serviceName) throws NacosException {
  447 |         return selectOneHealthyInstance(serviceName, new ArrayList<>());
  448 |     }
  449 |     
  450 |     @Override
  451 |     public Instance selectOneHealthyInstance(String serviceName, String groupName)
  452 |         throws NacosException {
  453 |         return selectOneHealthyInstance(serviceName, groupName, true);
  454 |     }
  455 |     
  456 |     @Override
  457 |     public Instance selectOneHealthyInstance(String serviceName, boolean subscribe)
  458 |         throws NacosException {
  459 |         return selectOneHealthyInstance(serviceName, new ArrayList<>(), subscribe);
  460 |     }
  461 |     
  462 |     @Override
  463 |     public Instance selectOneHealthyInstance(String serviceName, String groupName,
  464 |         boolean subscribe)
  465 |         throws NacosException {
  466 |         return selectOneHealthyInstance(serviceName, groupName, new ArrayList<>(), subscribe);
  467 |     }
  468 |     
  469 |     @Override
  470 |     public Instance selectOneHealthyInstance(String serviceName, List<String> clusters)
  471 |         throws NacosException {
  472 |         return selectOneHealthyInstance(serviceName, clusters, true);
  473 |     }
  474 |     
  475 |     @Override
  476 |     public Instance selectOneHealthyInstance(String serviceName, String groupName,
  477 |         List<String> clusters)
  478 |         throws NacosException {
  479 |         return selectOneHealthyInstance(serviceName, groupName, clusters, true);
  480 |     }
  481 |     
  482 |     @Override
  483 |     public Instance selectOneHealthyInstance(String serviceName, List<String> clusters,
  484 |         boolean subscribe)
  485 |         throws NacosException {
  486 |         return selectOneHealthyInstance(serviceName, Constants.DEFAULT_GROUP, clusters, subscribe);
  487 |     }
  488 |     
  489 |     @Override
  490 |     public Instance selectOneHealthyInstance(String serviceName, String groupName,
  491 |         List<String> clusters,
  492 |         boolean subscribe) throws NacosException {
  493 |         ServiceInfo serviceInfo = getServiceInfo(serviceName, groupName, clusters, subscribe);
  494 |         return Balancer.RandomByWeight.selectHost(serviceInfo);
  495 |     }
  496 |     
  497 |     @Override
  498 |     public void subscribe(String serviceName, EventListener listener) throws NacosException {
  499 |         subscribe(serviceName, new ArrayList<>(), listener);
  500 |     }
  501 |     
  502 |     @Override
  503 |     public void subscribe(String serviceName, String groupName, EventListener listener)
  504 |         throws NacosException {
  505 |         subscribe(serviceName, groupName, new ArrayList<>(), listener);
  506 |     }
  507 |     
  508 |     @Override
  509 |     public void subscribe(String serviceName, List<String> clusters, EventListener listener)
  510 |         throws NacosException {
  511 |         subscribe(serviceName, Constants.DEFAULT_GROUP, clusters, listener);
  512 |     }
  513 |     
  514 |     @Override
  515 |     public void subscribe(String serviceName, String groupName, List<String> clusters,
  516 |         EventListener listener)
  517 |         throws NacosException {
  518 |         NamingSelector clusterSelector = NamingSelectorFactory.newClusterSelector(clusters);
  519 |         doSubscribe(serviceName, groupName, getUniqueClusterString(clusters), clusterSelector,
  520 |             listener);
  521 |     }
  522 |     
  523 |     @Override
  524 |     public void subscribe(String serviceName, NamingSelector selector, EventListener listener)
  525 |         throws NacosException {
  526 |         subscribe(serviceName, Constants.DEFAULT_GROUP, selector, listener);
  527 |     }
  528 |     
  529 |     @Override
  530 |     public void subscribe(String serviceName, String groupName, NamingSelector selector,
  531 |         EventListener listener)
  532 |         throws NacosException {
  533 |         doSubscribe(serviceName, groupName, Constants.NULL, selector, listener);
  534 |     }
  535 |     
  536 |     private void doSubscribe(String serviceName, String groupName, String clusters,
  537 |         NamingSelector selector,
  538 |         EventListener listener) throws NacosException {
  539 |         if (selector == null || listener == null) {
  540 |             return;
  541 |         }
  542 |         NamingSelectorWrapper wrapper =
  543 |             new NamingSelectorWrapper(serviceName, groupName, clusters, selector, listener);
  544 |         changeNotifier.registerListener(groupName, serviceName, wrapper);
  545 |         notifyIfSubscribed(serviceName, groupName, wrapper);
  546 |         clientProxy.subscribe(serviceName, groupName, Constants.NULL);
  547 |     }
  548 |     
  549 |     @Override
  550 |     public void unsubscribe(String serviceName, EventListener listener) throws NacosException {
  551 |         unsubscribe(serviceName, new ArrayList<>(), listener);
  552 |     }
  553 |     
  554 |     @Override
  555 |     public void unsubscribe(String serviceName, String groupName, EventListener listener)
  556 |         throws NacosException {
  557 |         unsubscribe(serviceName, groupName, new ArrayList<>(), listener);
  558 |     }
  559 |     
  560 |     @Override
  561 |     public void unsubscribe(String serviceName, List<String> clusters, EventListener listener)
  562 |         throws NacosException {
  563 |         unsubscribe(serviceName, Constants.DEFAULT_GROUP, clusters, listener);
  564 |     }
  565 |     
  566 |     @Override
  567 |     public void unsubscribe(String serviceName, String groupName, List<String> clusters,
  568 |         EventListener listener)
  569 |         throws NacosException {
  570 |         NamingSelector clusterSelector = NamingSelectorFactory.newClusterSelector(clusters);
  571 |         unsubscribe(serviceName, groupName, clusterSelector, listener);
  572 |     }
  573 |     
  574 |     @Override
  575 |     public void unsubscribe(String serviceName, NamingSelector selector, EventListener listener)
  576 |         throws NacosException {
  577 |         unsubscribe(serviceName, Constants.DEFAULT_GROUP, selector, listener);
  578 |     }
  579 |     
  580 |     @Override
  581 |     public void unsubscribe(String serviceName, String groupName, NamingSelector selector,
  582 |         EventListener listener)
  583 |         throws NacosException {
  584 |         doUnsubscribe(serviceName, groupName, selector, listener);
  585 |     }
  586 |     
  587 |     private void doUnsubscribe(String serviceName, String groupName, NamingSelector selector,
  588 |         EventListener listener)
  589 |         throws NacosException {
  590 |         if (selector == null || listener == null) {
  591 |             return;
  592 |         }
  593 |         NamingSelectorWrapper wrapper = new NamingSelectorWrapper(selector, listener);
  594 |         changeNotifier.deregisterListener(groupName, serviceName, wrapper);
  595 |         if (!changeNotifier.isSubscribed(groupName, serviceName)) {
  596 |             clientProxy.unsubscribe(serviceName, groupName, Constants.NULL);
  597 |         }
  598 |     }
  599 |     
  600 |     @Override
  601 |     public void fuzzyWatch(String fixedGroupName, FuzzyWatchEventWatcher listener)
  602 |         throws NacosException {
  603 |         doFuzzyWatch(ANY_PATTERN, fixedGroupName, listener);
  604 |     }
  605 |     
  606 |     @Override
  607 |     public void fuzzyWatch(String serviceNamePattern, String groupNamePattern,
  608 |         FuzzyWatchEventWatcher listener)
  609 |         throws NacosException {
  610 |         doFuzzyWatch(serviceNamePattern, groupNamePattern, listener);
  611 |     }
  612 |     
  613 |     @Override
  614 |     public Future<ListView<String>> fuzzyWatchWithServiceKeys(String fixedGroupName,
  615 |         FuzzyWatchEventWatcher listener)
  616 |         throws NacosException {
  617 |         return doFuzzyWatch(ANY_PATTERN, fixedGroupName, listener);
  618 |     }
  619 |     
  620 |     @Override
  621 |     public Future<ListView<String>> fuzzyWatchWithServiceKeys(String serviceNamePattern,
  622 |         String groupNamePattern,
  623 |         FuzzyWatchEventWatcher listener) throws NacosException {
  624 |         return doFuzzyWatch(serviceNamePattern, groupNamePattern, listener);
  625 |     }
  626 |     
  627 |     private Future<ListView<String>> doFuzzyWatch(String serviceNamePattern,
  628 |         String groupNamePattern,
  629 |         FuzzyWatchEventWatcher watcher) {
  630 |         if (null == watcher) {
  631 |             return null;
  632 |         }
  633 |         
  634 |         String groupKeyPattern = FuzzyGroupKeyPattern.generatePattern(serviceNamePattern,
  635 |             groupNamePattern, namespace);
  636 |         NamingFuzzyWatchContext namingFuzzyWatchContext =
  637 |             namingFuzzyWatchServiceListHolder.registerFuzzyWatcher(
  638 |                 groupKeyPattern, watcher);
  639 |         return namingFuzzyWatchContext.createNewFuture();
  640 |     }
  641 |     
  642 |     @Override
  643 |     public void cancelFuzzyWatch(String fixedGroupName, FuzzyWatchEventWatcher listener)
  644 |         throws NacosException {
  645 |         doCancelFuzzyWatch(ANY_PATTERN, fixedGroupName, listener);
  646 |     }
  647 |     
  648 |     @Override
  649 |     public void cancelFuzzyWatch(String serviceNamePattern, String fixedGroupName,
  650 |         FuzzyWatchEventWatcher listener)
  651 |         throws NacosException {
  652 |         doCancelFuzzyWatch(serviceNamePattern, fixedGroupName, listener);
  653 |     }
  654 |     
  655 |     private void doCancelFuzzyWatch(String serviceNamePattern, String groupNamePattern,
  656 |         FuzzyWatchEventWatcher watcher)
  657 |         throws NacosException {
  658 |         if (null == watcher) {
  659 |             return;
  660 |         }
  661 |         String groupKeyPattern = FuzzyGroupKeyPattern.generatePattern(serviceNamePattern,
  662 |             groupNamePattern, namespace);
  663 |         
  664 |         NamingFuzzyWatchContext namingFuzzyWatchContext =
  665 |             namingFuzzyWatchServiceListHolder.getFuzzyWatchContext(
  666 |                 groupKeyPattern);
  667 |         if (namingFuzzyWatchContext != null) {
  668 |             namingFuzzyWatchContext.removeWatcher(watcher);
  669 |         }
  670 |     }
  671 |     
  672 |     @Override
  673 |     public ListView<String> getServicesOfServer(int pageNo, int pageSize) throws NacosException {
  674 |         return getServicesOfServer(pageNo, pageSize, Constants.DEFAULT_GROUP);
  675 |     }
  676 |     
  677 |     @Override
  678 |     public ListView<String> getServicesOfServer(int pageNo, int pageSize, String groupName)
  679 |         throws NacosException {
  680 |         return getServicesOfServer(pageNo, pageSize, groupName, null);
  681 |     }
  682 |     
  683 |     @Override
  684 |     public ListView<String> getServicesOfServer(int pageNo, int pageSize, AbstractSelector selector)
  685 |         throws NacosException {
  686 |         return getServicesOfServer(pageNo, pageSize, Constants.DEFAULT_GROUP, selector);
  687 |     }
  688 |     
  689 |     @Override
  690 |     public ListView<String> getServicesOfServer(int pageNo, int pageSize, String groupName,
  691 |         AbstractSelector selector)
  692 |         throws NacosException {
  693 |         return clientProxy.getServiceList(pageNo, pageSize, groupName, selector);
  694 |     }
  695 |     
  696 |     @Override
  697 |     public List<ServiceInfo> getSubscribeServices() {
  698 |         return changeNotifier.getSubscribeServices();
  699 |     }
  700 |     
  701 |     @Override
  702 |     public String getServerStatus() {
  703 |         return clientProxy.serverHealthy() ? UP : DOWN;
  704 |     }
  705 |     
  706 |     @Override
  707 |     public void shutDown() throws NacosException {
  708 |         serviceInfoHolder.shutdown();
  709 |         clientProxy.shutdown();
  710 |         namingFuzzyWatchServiceListHolder.shutdown();
  711 |         NotifyCenter.deregisterSubscriber(changeNotifier);
  712 |     }
  713 |     
  714 |     private void batchCheckAndStripGroupNamePrefix(List<Instance> instances, String groupName)
  715 |         throws NacosException {
  716 |         for (Instance instance : instances) {
  717 |             checkAndStripGroupNamePrefix(instance, groupName);
  718 |         }
  719 |     }
  720 |     
  721 |     private void checkAndStripGroupNamePrefix(Instance instance, String groupName)
  722 |         throws NacosException {
  723 |         String serviceName = instance.getServiceName();
  724 |         if (NamingUtils.isServiceNameCompatibilityMode(serviceName)) {
  725 |             String groupNameOfInstance = NamingUtils.getGroupName(serviceName);
  726 |             if (!groupName.equals(groupNameOfInstance)) {
  727 |                 throw new NacosException(NacosException.CLIENT_INVALID_PARAM, String.format(
  728 |                     "wrong group name prefix of instance service name! it should be: %s, Instance: %s",
  729 |                     groupName,
  730 |                     instance));
  731 |             }
  732 |             instance.setServiceName(NamingUtils.getServiceName(serviceName));
  733 |         }
  734 |     }
  735 |     
  736 |     private void notifyIfSubscribed(String serviceName, String groupName,
  737 |         NamingSelectorWrapper wrapper)
  738 |         throws NacosException {
  739 |         if (clientProxy.isSubscribed(serviceName, groupName, StringUtils.EMPTY)) {
  740 |             NAMING_LOGGER.warn(
  741 |                 "Duplicate subscribe for groupName: {}, serviceName: {}; directly use current cached to notify.",
  742 |                 groupName, serviceName);
  743 |             ServiceInfo serviceInfo = serviceInfoHolder.getServiceInfo(serviceName, groupName);
  744 |             InstancesChangeEvent event = transferToEvent(serviceInfo);
  745 |             wrapper.notifyListener(event);
  746 |         }
  747 |     }
  748 |     
  749 |     private InstancesChangeEvent transferToEvent(ServiceInfo serviceInfo) {
  750 |         if (serviceInfo == null) {
  751 |             return null;
  752 |         }
  753 |         InstancesDiff diff = new InstancesDiff();
  754 |         diff.setAddedInstances(serviceInfo.getHosts());
  755 |         return new InstancesChangeEvent(notifierEventScope, serviceInfo.getName(),
  756 |             serviceInfo.getGroupName(),
  757 |             serviceInfo.getClusters(), serviceInfo.getHosts(), diff);
  758 |     }
  759 | }
```
