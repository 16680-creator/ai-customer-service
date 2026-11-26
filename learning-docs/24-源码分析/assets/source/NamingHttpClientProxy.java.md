<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.nacos:nacos-client:3.3.0 | path: com/alibaba/nacos/client/naming/remote/http/NamingHttpClientProxy.java -->
<!-- 正文引用：[[源码:NamingHttpClientProxy.java#L起-L止]] ；代码块头部：(src: NamingHttpClientProxy.java#L起-L止) -->

# NamingHttpClientProxy.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.nacos:nacos-client:3.3.0` |
| 路径 | `com/alibaba/nacos/client/naming/remote/http/NamingHttpClientProxy.java` |
| 行数 | 519 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/nacos/nacos-client/3.3.0/nacos-client-3.3.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:10.732Z |

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
   17 | package com.alibaba.nacos.client.naming.remote.http;
   18 | 
   19 | import com.alibaba.nacos.api.PropertyKeyConst;
   20 | import com.alibaba.nacos.api.SystemPropertyKeyConst;
   21 | import com.alibaba.nacos.api.exception.NacosException;
   22 | import com.alibaba.nacos.api.naming.CommonParams;
   23 | import com.alibaba.nacos.api.naming.pojo.Instance;
   24 | import com.alibaba.nacos.api.naming.pojo.ListView;
   25 | import com.alibaba.nacos.api.naming.pojo.Service;
   26 | import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
   27 | import com.alibaba.nacos.api.naming.utils.NamingUtils;
   28 | import com.alibaba.nacos.api.selector.AbstractSelector;
   29 | import com.alibaba.nacos.api.selector.ExpressionSelector;
   30 | import com.alibaba.nacos.api.selector.SelectorType;
   31 | import com.alibaba.nacos.api.utils.json.JsonUtils;
   32 | import com.alibaba.nacos.api.utils.json.NacosTypeReference;
   33 | import com.alibaba.nacos.client.address.ServerListChangeEvent;
   34 | import com.alibaba.nacos.client.env.NacosClientProperties;
   35 | import com.alibaba.nacos.client.monitor.MetricsMonitor;
   36 | import com.alibaba.nacos.client.naming.core.NamingServerListManager;
   37 | import com.alibaba.nacos.client.naming.remote.AbstractNamingClientProxy;
   38 | import com.alibaba.nacos.client.naming.utils.UtilAndComs;
   39 | import com.alibaba.nacos.client.security.SecurityProxy;
   40 | import com.alibaba.nacos.common.http.HttpRestResult;
   41 | import com.alibaba.nacos.common.http.HttpUtils;
   42 | import com.alibaba.nacos.common.http.client.NacosRestTemplate;
   43 | import com.alibaba.nacos.common.http.param.Header;
   44 | import com.alibaba.nacos.common.http.param.Query;
   45 | import com.alibaba.nacos.common.notify.Event;
   46 | import com.alibaba.nacos.common.utils.CollectionUtils;
   47 | import com.alibaba.nacos.common.utils.ConvertUtils;
   48 | import com.alibaba.nacos.common.utils.HttpMethod;
   49 | import com.alibaba.nacos.common.utils.InternetAddressUtil;
   50 | import com.alibaba.nacos.common.utils.StringUtils;
   51 | import org.apache.hc.core5.http.HttpStatus;
   52 | 
   53 | import java.util.Collections;
   54 | import java.util.HashMap;
   55 | import java.util.List;
   56 | import java.util.Map;
   57 | import java.util.concurrent.ThreadLocalRandom;
   58 | 
   59 | import static com.alibaba.nacos.client.utils.LogUtils.NAMING_LOGGER;
   60 | import static com.alibaba.nacos.common.constant.RequestUrlConstants.HTTPS_PREFIX;
   61 | import static com.alibaba.nacos.common.constant.RequestUrlConstants.HTTP_PREFIX;
   62 | 
   63 | /**
   64 |  * Naming proxy.
   65 |  *
   66 |  * @author nkorange
   67 |  */
   68 | @Deprecated
   69 | public class NamingHttpClientProxy extends AbstractNamingClientProxy {
   70 |     
   71 |     private final NacosRestTemplate nacosRestTemplate =
   72 |         NamingHttpClientManager.getInstance().getNacosRestTemplate();
   73 |     
   74 |     private static final int DEFAULT_SERVER_PORT = 8848;
   75 |     
   76 |     private static final String MODULE_NAME = "Naming";
   77 |     
   78 |     private static final String IP_PARAM = "ip";
   79 |     
   80 |     private static final String PORT_PARAM = "port";
   81 |     
   82 |     private static final String WEIGHT_PARAM = "weight";
   83 |     
   84 |     private static final String ENABLE_PARAM = "enabled";
   85 |     
   86 |     private static final String EPHEMERAL_PARAM = "ephemeral";
   87 |     
   88 |     private static final String META_PARAM = "metadata";
   89 |     
   90 |     private static final String SELECTOR_PARAM = "selector";
   91 |     
   92 |     private static final String HEALTHY_PARAM = "healthy";
   93 |     
   94 |     private static final String PROTECT_THRESHOLD_PARAM = "protectThreshold";
   95 |     
   96 |     private static final String REGISTER_ENABLE_PARAM = "enable";
   97 |     
   98 |     private final String namespaceId;
   99 |     
  100 |     private final NamingServerListManager serverListManager;
  101 |     
  102 |     private final int maxRetry;
  103 |     
  104 |     private int serverPort = DEFAULT_SERVER_PORT;
  105 |     
  106 |     private boolean enableClientMetrics = true;
  107 |     
  108 |     // TODO: Remove this deprecated HTTP naming proxy after naming client fully relies on gRPC.
  109 |     public NamingHttpClientProxy(String namespaceId, SecurityProxy securityProxy,
  110 |         NamingServerListManager serverListManager,
  111 |         NacosClientProperties properties) {
  112 |         super(securityProxy);
  113 |         this.serverListManager = serverListManager;
  114 |         this.setServerPort(DEFAULT_SERVER_PORT);
  115 |         this.namespaceId = namespaceId;
  116 |         this.maxRetry = ConvertUtils
  117 |             .toInt(properties.getProperty(PropertyKeyConst.NAMING_REQUEST_DOMAIN_RETRY_COUNT,
  118 |                 String.valueOf(UtilAndComs.REQUEST_DOMAIN_RETRY_COUNT)));
  119 |         this.enableClientMetrics = Boolean.parseBoolean(
  120 |             properties.getProperty(PropertyKeyConst.ENABLE_CLIENT_METRICS, "true"));
  121 |     }
  122 |     
  123 |     @Override
  124 |     public void onEvent(ServerListChangeEvent event) {
  125 |         // do nothing in http client
  126 |     }
  127 |     
  128 |     @Override
  129 |     public Class<? extends Event> subscribeType() {
  130 |         return ServerListChangeEvent.class;
  131 |     }
  132 |     
  133 |     @Override
  134 |     public void registerService(String serviceName, String groupName, Instance instance)
  135 |         throws NacosException {
  136 |         NAMING_LOGGER.info("[REGISTER-SERVICE] {} registering service {} with instance: {}",
  137 |             namespaceId, serviceName,
  138 |             instance);
  139 |         String groupedServiceName = NamingUtils.getGroupedName(serviceName, groupName);
  140 |         if (instance.isEphemeral()) {
  141 |             throw new UnsupportedOperationException(
  142 |                 "Do not support register ephemeral instances by HTTP, please use gRPC replaced.");
  143 |         }
  144 |         final Map<String, String> params = new HashMap<>(32);
  145 |         params.put(CommonParams.NAMESPACE_ID, namespaceId);
  146 |         params.put(CommonParams.SERVICE_NAME, groupedServiceName);
  147 |         params.put(CommonParams.GROUP_NAME, groupName);
  148 |         params.put(CommonParams.CLUSTER_NAME, instance.getClusterName());
  149 |         params.put(IP_PARAM, instance.getIp());
  150 |         params.put(PORT_PARAM, String.valueOf(instance.getPort()));
  151 |         params.put(WEIGHT_PARAM, String.valueOf(instance.getWeight()));
  152 |         params.put(REGISTER_ENABLE_PARAM, String.valueOf(instance.isEnabled()));
  153 |         params.put(HEALTHY_PARAM, String.valueOf(instance.isHealthy()));
  154 |         params.put(EPHEMERAL_PARAM, String.valueOf(instance.isEphemeral()));
  155 |         params.put(META_PARAM, JsonUtils.toJson(instance.getMetadata()));
  156 |         reqApi(UtilAndComs.nacosUrlInstance, params, HttpMethod.POST);
  157 |     }
  158 |     
  159 |     @Override
  160 |     public void batchRegisterService(String serviceName, String groupName,
  161 |         List<Instance> instances) {
  162 |         throw new UnsupportedOperationException(
  163 |             "Do not support persistent instances to perform batch registration methods.");
  164 |     }
  165 |     
  166 |     @Override
  167 |     public void batchDeregisterService(String serviceName, String groupName,
  168 |         List<Instance> instances) {
  169 |         throw new UnsupportedOperationException(
  170 |             "Do not support persistent instances to perform batch de registration methods.");
  171 |     }
  172 |     
  173 |     @Override
  174 |     public void deregisterService(String serviceName, String groupName, Instance instance)
  175 |         throws NacosException {
  176 |         NAMING_LOGGER.info("[DEREGISTER-SERVICE] {} deregistering service {} with instance: {}",
  177 |             namespaceId,
  178 |             serviceName, instance);
  179 |         if (instance.isEphemeral()) {
  180 |             return;
  181 |         }
  182 |         final Map<String, String> params = new HashMap<>(16);
  183 |         params.put(CommonParams.NAMESPACE_ID, namespaceId);
  184 |         params.put(CommonParams.SERVICE_NAME, NamingUtils.getGroupedName(serviceName, groupName));
  185 |         params.put(CommonParams.CLUSTER_NAME, instance.getClusterName());
  186 |         params.put(IP_PARAM, instance.getIp());
  187 |         params.put(PORT_PARAM, String.valueOf(instance.getPort()));
  188 |         params.put(EPHEMERAL_PARAM, String.valueOf(instance.isEphemeral()));
  189 |         
  190 |         reqApi(UtilAndComs.nacosUrlInstance, params, HttpMethod.DELETE);
  191 |     }
  192 |     
  193 |     @Override
  194 |     public void updateInstance(String serviceName, String groupName, Instance instance)
  195 |         throws NacosException {
  196 |         NAMING_LOGGER.info("[UPDATE-SERVICE] {} update service {} with instance: {}", namespaceId,
  197 |             serviceName,
  198 |             instance);
  199 |         
  200 |         final Map<String, String> params = new HashMap<>(32);
  201 |         params.put(CommonParams.NAMESPACE_ID, namespaceId);
  202 |         params.put(CommonParams.SERVICE_NAME, serviceName);
  203 |         params.put(CommonParams.GROUP_NAME, groupName);
  204 |         params.put(CommonParams.CLUSTER_NAME, instance.getClusterName());
  205 |         params.put(IP_PARAM, instance.getIp());
  206 |         params.put(PORT_PARAM, String.valueOf(instance.getPort()));
  207 |         params.put(WEIGHT_PARAM, String.valueOf(instance.getWeight()));
  208 |         params.put(ENABLE_PARAM, String.valueOf(instance.isEnabled()));
  209 |         params.put(EPHEMERAL_PARAM, String.valueOf(instance.isEphemeral()));
  210 |         params.put(META_PARAM, JsonUtils.toJson(instance.getMetadata()));
  211 |         
  212 |         reqApi(UtilAndComs.nacosUrlInstance, params, HttpMethod.PUT);
  213 |     }
  214 |     
  215 |     @Override
  216 |     public ServiceInfo queryInstancesOfService(String serviceName, String groupName,
  217 |         String clusters,
  218 |         boolean healthyOnly) {
  219 |         throw new UnsupportedOperationException(
  220 |             "Do not support query instance by http client,please use gRPC replaced.");
  221 |     }
  222 |     
  223 |     @Override
  224 |     public Service queryService(String serviceName, String groupName) throws NacosException {
  225 |         NAMING_LOGGER.info("[QUERY-SERVICE] {} query service : {}, {}", namespaceId, serviceName,
  226 |             groupName);
  227 |         
  228 |         final Map<String, String> params = new HashMap<>(16);
  229 |         params.put(CommonParams.NAMESPACE_ID, namespaceId);
  230 |         params.put(CommonParams.SERVICE_NAME, serviceName);
  231 |         params.put(CommonParams.GROUP_NAME, groupName);
  232 |         
  233 |         String result = reqApi(UtilAndComs.nacosUrlService, params, HttpMethod.GET);
  234 |         return JsonUtils.toObj(result, Service.class);
  235 |     }
  236 |     
  237 |     @Override
  238 |     public void createService(Service service, AbstractSelector selector) throws NacosException {
  239 |         
  240 |         NAMING_LOGGER.info("[CREATE-SERVICE] {} creating service : {}", namespaceId, service);
  241 |         
  242 |         final Map<String, String> params = new HashMap<>(16);
  243 |         params.put(CommonParams.NAMESPACE_ID, namespaceId);
  244 |         params.put(CommonParams.SERVICE_NAME, service.getName());
  245 |         params.put(CommonParams.GROUP_NAME, service.getGroupName());
  246 |         params.put(PROTECT_THRESHOLD_PARAM, String.valueOf(service.getProtectThreshold()));
  247 |         params.put(META_PARAM, JsonUtils.toJson(service.getMetadata()));
  248 |         params.put(SELECTOR_PARAM, JsonUtils.toJson(selector));
  249 |         
  250 |         reqApi(UtilAndComs.nacosUrlService, params, HttpMethod.POST);
  251 |         
  252 |     }
  253 |     
  254 |     @Override
  255 |     public boolean deleteService(String serviceName, String groupName) throws NacosException {
  256 |         NAMING_LOGGER.info("[DELETE-SERVICE] {} deleting service : {} with groupName : {}",
  257 |             namespaceId, serviceName,
  258 |             groupName);
  259 |         
  260 |         final Map<String, String> params = new HashMap<>(16);
  261 |         params.put(CommonParams.NAMESPACE_ID, namespaceId);
  262 |         params.put(CommonParams.SERVICE_NAME, serviceName);
  263 |         params.put(CommonParams.GROUP_NAME, groupName);
  264 |         
  265 |         String result = reqApi(UtilAndComs.nacosUrlService, params, HttpMethod.DELETE);
  266 |         return "ok".equals(result);
  267 |     }
  268 |     
  269 |     @Override
  270 |     public void updateService(Service service, AbstractSelector selector) throws NacosException {
  271 |         NAMING_LOGGER.info("[UPDATE-SERVICE] {} updating service : {}", namespaceId, service);
  272 |         
  273 |         final Map<String, String> params = new HashMap<>(16);
  274 |         params.put(CommonParams.NAMESPACE_ID, namespaceId);
  275 |         params.put(CommonParams.SERVICE_NAME, service.getName());
  276 |         params.put(CommonParams.GROUP_NAME, service.getGroupName());
  277 |         params.put(PROTECT_THRESHOLD_PARAM, String.valueOf(service.getProtectThreshold()));
  278 |         params.put(META_PARAM, JsonUtils.toJson(service.getMetadata()));
  279 |         params.put(SELECTOR_PARAM, JsonUtils.toJson(selector));
  280 |         
  281 |         reqApi(UtilAndComs.nacosUrlService, params, HttpMethod.PUT);
  282 |     }
  283 |     
  284 |     @Override
  285 |     public boolean serverHealthy() {
  286 |         try {
  287 |             String result = reqApi(UtilAndComs.webContext + "/v3/admin/core/state/liveness",
  288 |                 new HashMap<>(8), HttpMethod.GET);
  289 |             Map<String, Object> json =
  290 |                 JsonUtils.toObj(result, new NacosTypeReference<Map<String, Object>>() {
  291 |                 });
  292 |             Object statusCode = json.get("code");
  293 |             return statusCode instanceof Number && 0 == ((Number) statusCode).intValue();
  294 |         } catch (Exception e) {
  295 |             return false;
  296 |         }
  297 |     }
  298 |     
  299 |     @Override
  300 |     public ListView<String> getServiceList(int pageNo, int pageSize, String groupName,
  301 |         AbstractSelector selector)
  302 |         throws NacosException {
  303 |         
  304 |         Map<String, String> params = new HashMap<>(16);
  305 |         params.put("pageNo", String.valueOf(pageNo));
  306 |         params.put("pageSize", String.valueOf(pageSize));
  307 |         params.put(CommonParams.NAMESPACE_ID, namespaceId);
  308 |         params.put(CommonParams.GROUP_NAME, groupName);
  309 |         
  310 |         if (selector != null) {
  311 |             switch (SelectorType.valueOf(selector.getType())) {
  312 |                 case none:
  313 |                     break;
  314 |                 case label:
  315 |                     ExpressionSelector expressionSelector = (ExpressionSelector) selector;
  316 |                     params.put(SELECTOR_PARAM, JsonUtils.toJson(expressionSelector));
  317 |                     break;
  318 |                 default:
  319 |                     break;
  320 |             }
  321 |         }
  322 |         
  323 |         String result = reqApi(UtilAndComs.nacosUrlBase + "/service/list", params, HttpMethod.GET);
  324 |         
  325 |         Map<String, Object> json =
  326 |             JsonUtils.toObj(result, new NacosTypeReference<Map<String, Object>>() {
  327 |             });
  328 |         ListView<String> listView = new ListView<>();
  329 |         Object count = json.get("count");
  330 |         listView.setCount(count instanceof Number ? ((Number) count).intValue() : 0);
  331 |         listView.setData(JsonUtils.toObj(JsonUtils.toJson(json.get("doms")),
  332 |             new NacosTypeReference<List<String>>() {
  333 |             }));
  334 |         
  335 |         return listView;
  336 |     }
  337 |     
  338 |     @Override
  339 |     public ServiceInfo subscribe(String serviceName, String groupName, String clusters)
  340 |         throws NacosException {
  341 |         throw new UnsupportedOperationException(
  342 |             "Do not support subscribe service by UDP, please use gRPC replaced.");
  343 |     }
  344 |     
  345 |     @Override
  346 |     public void unsubscribe(String serviceName, String groupName, String clusters)
  347 |         throws NacosException {
  348 |     }
  349 |     
  350 |     @Override
  351 |     public boolean isSubscribed(String serviceName, String groupName, String clusters)
  352 |         throws NacosException {
  353 |         return true;
  354 |     }
  355 |     
  356 |     public String reqApi(String api, Map<String, String> params, String method)
  357 |         throws NacosException {
  358 |         return reqApi(api, params, Collections.EMPTY_MAP, method);
  359 |     }
  360 |     
  361 |     public String reqApi(String api, Map<String, String> params, Map<String, String> body,
  362 |         String method)
  363 |         throws NacosException {
  364 |         return reqApi(api, params, body, serverListManager.getServerList(), method);
  365 |     }
  366 |     
  367 |     /**
  368 |      * Request api.
  369 |      *
  370 |      * @param api     api
  371 |      * @param params  parameters
  372 |      * @param body    body
  373 |      * @param servers servers
  374 |      * @param method  http method
  375 |      * @return result
  376 |      * @throws NacosException nacos exception
  377 |      */
  378 |     public String reqApi(String api, Map<String, String> params, Map<String, String> body,
  379 |         List<String> servers,
  380 |         String method) throws NacosException {
  381 |         
  382 |         params.put(CommonParams.NAMESPACE_ID, getNamespaceId());
  383 |         
  384 |         if (CollectionUtils.isEmpty(servers) && !serverListManager.isDomain()) {
  385 |             throw new NacosException(NacosException.INVALID_PARAM, "no server available");
  386 |         }
  387 |         
  388 |         NacosException exception = new NacosException();
  389 |         if (serverListManager.isDomain()) {
  390 |             String nacosDomain = serverListManager.getNacosDomain();
  391 |             for (int i = 0; i < maxRetry; i++) {
  392 |                 try {
  393 |                     return callServer(api, params, body, nacosDomain, method);
  394 |                 } catch (NacosException e) {
  395 |                     exception = e;
  396 |                     if (NAMING_LOGGER.isDebugEnabled()) {
  397 |                         NAMING_LOGGER.debug("request {} failed.", nacosDomain, e);
  398 |                     }
  399 |                 }
  400 |             }
  401 |         } else {
  402 |             int index = ThreadLocalRandom.current().nextInt(servers.size());
  403 |             
  404 |             for (int i = 0; i < servers.size(); i++) {
  405 |                 String server = servers.get(index);
  406 |                 try {
  407 |                     return callServer(api, params, body, server, method);
  408 |                 } catch (NacosException e) {
  409 |                     exception = e;
  410 |                     if (NAMING_LOGGER.isDebugEnabled()) {
  411 |                         NAMING_LOGGER.debug("request {} failed.", server, e);
  412 |                     }
  413 |                 }
  414 |                 index = (index + 1) % servers.size();
  415 |             }
  416 |         }
  417 |         
  418 |         NAMING_LOGGER.error("request: {} failed, servers: {}, code: {}, msg: {}", api, servers,
  419 |             exception.getErrCode(),
  420 |             exception.getErrMsg());
  421 |         
  422 |         throw new NacosException(exception.getErrCode(),
  423 |             "failed to req API:" + api + " after all servers(" + servers + ") tried: "
  424 |                 + exception.getMessage());
  425 |         
  426 |     }
  427 |     
  428 |     /**
  429 |      * Call server.
  430 |      *
  431 |      * @param api       api
  432 |      * @param params    parameters
  433 |      * @param body      body
  434 |      * @param curServer ?
  435 |      * @param method    http method
  436 |      * @return result
  437 |      * @throws NacosException nacos exception
  438 |      */
  439 |     public String callServer(String api, Map<String, String> params, Map<String, String> body,
  440 |         String curServer,
  441 |         String method) throws NacosException {
  442 |         long start = System.currentTimeMillis();
  443 |         long end = 0;
  444 |         String namespace = params.get(CommonParams.NAMESPACE_ID);
  445 |         String group = params.get(CommonParams.GROUP_NAME);
  446 |         String serviceName = params.get(CommonParams.SERVICE_NAME);
  447 |         params.putAll(getSecurityHeaders(namespace, group, serviceName));
  448 |         Header header = HttpUtils.builderHeader(MODULE_NAME);
  449 |         
  450 |         String url;
  451 |         if (curServer.startsWith(HTTPS_PREFIX) || curServer.startsWith(HTTP_PREFIX)) {
  452 |             url = curServer + api;
  453 |         } else {
  454 |             if (!InternetAddressUtil.containsPort(curServer)) {
  455 |                 curServer = curServer + InternetAddressUtil.IP_PORT_SPLITER + serverPort;
  456 |             }
  457 |             url = NamingHttpClientManager.getInstance().getPrefix() + curServer + api;
  458 |         }
  459 |         try {
  460 |             HttpRestResult<String> restResult = nacosRestTemplate.exchangeForm(url, header,
  461 |                 Query.newInstance().initParams(params), body, method, String.class);
  462 |             end = System.currentTimeMillis();
  463 |             
  464 |             if (enableClientMetrics) {
  465 |                 try {
  466 |                     MetricsMonitor.observeNamingRequest(method, url,
  467 |                         String.valueOf(restResult.getCode()),
  468 |                         end - start);
  469 |                 } catch (Throwable t) {
  470 |                     NAMING_LOGGER.error(
  471 |                         "Failed to record metrics. Method: {}, URL: {}, HTTP Status Code: {}",
  472 |                         method, url, restResult.getCode(), t);
  473 |                 }
  474 |             }
  475 |             
  476 |             if (restResult.ok()) {
  477 |                 return restResult.getData();
  478 |             }
  479 |             if (HttpStatus.SC_NOT_MODIFIED == restResult.getCode()) {
  480 |                 return StringUtils.EMPTY;
  481 |             }
  482 |             
  483 |             // If the 403 login operation is triggered, refresh the accessToken of the client
  484 |             if (HttpStatus.SC_FORBIDDEN == restResult.getCode()) {
  485 |                 reLogin();
  486 |             }
  487 |             
  488 |             throw new NacosException(restResult.getCode(), restResult.getMessage());
  489 |         } catch (NacosException e) {
  490 |             NAMING_LOGGER.error("[NA] failed to request", e);
  491 |             throw e;
  492 |         } catch (Exception e) {
  493 |             NAMING_LOGGER.error("[NA] failed to request", e);
  494 |             throw new NacosException(NacosException.SERVER_ERROR, e);
  495 |         }
  496 |     }
  497 |     
  498 |     public String getNamespaceId() {
  499 |         return namespaceId;
  500 |     }
  501 |     
  502 |     public void setServerPort(int serverPort) {
  503 |         this.serverPort = serverPort;
  504 |         
  505 |         String sp = NacosClientProperties.PROTOTYPE
  506 |             .getProperty(SystemPropertyKeyConst.NAMING_SERVER_PORT);
  507 |         if (StringUtils.isNotBlank(sp)) {
  508 |             this.serverPort = Integer.parseInt(sp);
  509 |         }
  510 |     }
  511 |     
  512 |     @Override
  513 |     public void shutdown() throws NacosException {
  514 |         String className = this.getClass().getName();
  515 |         NAMING_LOGGER.info("{} do shutdown begin", className);
  516 |         NamingHttpClientManager.getInstance().shutdown();
  517 |         NAMING_LOGGER.info("{} do shutdown stop", className);
  518 |     }
  519 | }
```
