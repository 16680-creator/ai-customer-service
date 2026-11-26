<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.nacos:nacos-client:3.3.0 | path: com/alibaba/nacos/api/naming/NamingService.java -->
<!-- 正文引用：[[源码:NamingService.java#L起-L止]] ；代码块头部：(src: NamingService.java#L起-L止) -->

# NamingService.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.nacos:nacos-client:3.3.0` |
| 路径 | `com/alibaba/nacos/api/naming/NamingService.java` |
| 行数 | 829 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/nacos/nacos-client/3.3.0/nacos-client-3.3.0-sources.jar |
| 抓取时间 | 2026-11-25T15:34:09.835Z |

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
   17 | package com.alibaba.nacos.api.naming;
   18 | 
   19 | import com.alibaba.nacos.api.annotation.Since;
   20 | import com.alibaba.nacos.api.exception.NacosException;
   21 | import com.alibaba.nacos.api.naming.listener.FuzzyWatchEventWatcher;
   22 | import com.alibaba.nacos.api.naming.listener.EventListener;
   23 | import com.alibaba.nacos.api.naming.pojo.Instance;
   24 | import com.alibaba.nacos.api.naming.pojo.ListView;
   25 | import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
   26 | import com.alibaba.nacos.api.naming.selector.NamingSelector;
   27 | import com.alibaba.nacos.api.selector.AbstractSelector;
   28 | 
   29 | import java.util.List;
   30 | import java.util.concurrent.Future;
   31 | 
   32 | /**
   33 |  * Naming Service.
   34 |  *
   35 |  * @author nkorange
   36 |  */
   37 | public interface NamingService {
   38 |     
   39 |     /**
   40 |      * register an instance to service.
   41 |      *
   42 |      * @param serviceName name of service
   43 |      * @param ip          instance ip
   44 |      * @param port        instance port
   45 |      * @throws NacosException nacos exception
   46 |      */
   47 |     @Since("0.2.0")
   48 |     void registerInstance(String serviceName, String ip, int port) throws NacosException;
   49 |     
   50 |     /**
   51 |      * register an instance to service.
   52 |      *
   53 |      * @param serviceName name of service
   54 |      * @param groupName   group of service
   55 |      * @param ip          instance ip
   56 |      * @param port        instance port
   57 |      * @throws NacosException nacos exception
   58 |      */
   59 |     @Since("0.2.0")
   60 |     void registerInstance(String serviceName, String groupName, String ip, int port)
   61 |         throws NacosException;
   62 |     
   63 |     /**
   64 |      * register an instance to service with specified cluster name.
   65 |      *
   66 |      * @param serviceName name of service
   67 |      * @param ip          instance ip
   68 |      * @param port        instance port
   69 |      * @param clusterName instance cluster name
   70 |      * @throws NacosException nacos exception
   71 |      */
   72 |     @Since("0.2.0")
   73 |     void registerInstance(String serviceName, String ip, int port, String clusterName)
   74 |         throws NacosException;
   75 |     
   76 |     /**
   77 |      * register an instance to service with specified cluster name.
   78 |      *
   79 |      * @param serviceName name of service
   80 |      * @param groupName   group of service
   81 |      * @param ip          instance ip
   82 |      * @param port        instance port
   83 |      * @param clusterName instance cluster name
   84 |      * @throws NacosException nacos exception
   85 |      */
   86 |     @Since("1.0.0")
   87 |     void registerInstance(String serviceName, String groupName, String ip, int port,
   88 |         String clusterName)
   89 |         throws NacosException;
   90 |     
   91 |     /**
   92 |      * register an instance to service with specified instance properties.
   93 |      *
   94 |      * @param serviceName name of service
   95 |      * @param instance    instance to register
   96 |      * @throws NacosException nacos exception
   97 |      */
   98 |     @Since("0.2.0")
   99 |     void registerInstance(String serviceName, Instance instance) throws NacosException;
  100 |     
  101 |     /**
  102 |      * register an instance to service with specified instance properties.
  103 |      *
  104 |      * @param serviceName name of service
  105 |      * @param groupName   group of service
  106 |      * @param instance    instance to register
  107 |      * @throws NacosException nacos exception
  108 |      */
  109 |     @Since("0.2.0")
  110 |     void registerInstance(String serviceName, String groupName, Instance instance)
  111 |         throws NacosException;
  112 |     
  113 |     /**
  114 |      * batch register instance to service with specified instance properties.
  115 |      *
  116 |      * @param serviceName name of service
  117 |      * @param groupName   group of service
  118 |      * @param instances   instances to register
  119 |      * @throws NacosException nacos exception
  120 |      * @since 2.1.1
  121 |      */
  122 |     @Since("2.1.1")
  123 |     void batchRegisterInstance(String serviceName, String groupName, List<Instance> instances)
  124 |         throws NacosException;
  125 |     
  126 |     /**
  127 |      * batch deRegister instance to service with specified instance properties.
  128 |      *
  129 |      * @param serviceName name of service
  130 |      * @param groupName   group of service
  131 |      * @param instances   instances to deRegister
  132 |      * @throws NacosException nacos exception
  133 |      * @since 2.2.0
  134 |      */
  135 |     @Since("2.2.0")
  136 |     void batchDeregisterInstance(String serviceName, String groupName, List<Instance> instances)
  137 |         throws NacosException;
  138 |     
  139 |     /**
  140 |      * deregister instance from a service.
  141 |      *
  142 |      * @param serviceName name of service
  143 |      * @param ip          instance ip
  144 |      * @param port        instance port
  145 |      * @throws NacosException nacos exception
  146 |      */
  147 |     @Since("0.2.0")
  148 |     void deregisterInstance(String serviceName, String ip, int port) throws NacosException;
  149 |     
  150 |     /**
  151 |      * deregister instance from a service.
  152 |      *
  153 |      * @param serviceName name of service
  154 |      * @param groupName   group of service
  155 |      * @param ip          instance ip
  156 |      * @param port        instance port
  157 |      * @throws NacosException nacos exception
  158 |      */
  159 |     @Since("0.2.0")
  160 |     void deregisterInstance(String serviceName, String groupName, String ip, int port)
  161 |         throws NacosException;
  162 |     
  163 |     /**
  164 |      * deregister instance with specified cluster name from a service.
  165 |      *
  166 |      * @param serviceName name of service
  167 |      * @param ip          instance ip
  168 |      * @param port        instance port
  169 |      * @param clusterName instance cluster name
  170 |      * @throws NacosException nacos exception
  171 |      */
  172 |     @Since("0.2.0")
  173 |     void deregisterInstance(String serviceName, String ip, int port, String clusterName)
  174 |         throws NacosException;
  175 |     
  176 |     /**
  177 |      * deregister instance with specified cluster name from a service.
  178 |      *
  179 |      * @param serviceName name of service
  180 |      * @param groupName   group of service
  181 |      * @param ip          instance ip
  182 |      * @param port        instance port
  183 |      * @param clusterName instance cluster name
  184 |      * @throws NacosException nacos exception
  185 |      */
  186 |     @Since("1.0.0")
  187 |     void deregisterInstance(String serviceName, String groupName, String ip, int port,
  188 |         String clusterName)
  189 |         throws NacosException;
  190 |     
  191 |     /**
  192 |      * deregister instance with full instance information and default groupName.
  193 |      *
  194 |      * @param serviceName name of service
  195 |      * @param instance    instance
  196 |      * @throws NacosException nacos exception
  197 |      */
  198 |     @Since("1.1.0")
  199 |     void deregisterInstance(String serviceName, Instance instance) throws NacosException;
  200 |     
  201 |     /**
  202 |      * deregister instance with full instance information.
  203 |      *
  204 |      * @param serviceName name of service
  205 |      * @param groupName   group of service
  206 |      * @param instance    instance information
  207 |      * @throws NacosException nacos exception
  208 |      */
  209 |     @Since("0.2.0")
  210 |     void deregisterInstance(String serviceName, String groupName, Instance instance)
  211 |         throws NacosException;
  212 |     
  213 |     /**
  214 |      * get all instances of a service.
  215 |      *
  216 |      * @param serviceName name of service
  217 |      * @return A list of instance
  218 |      * @throws NacosException nacos exception
  219 |      */
  220 |     @Since("0.2.0")
  221 |     List<Instance> getAllInstances(String serviceName) throws NacosException;
  222 |     
  223 |     /**
  224 |      * get all instances of a service.
  225 |      *
  226 |      * @param serviceName name of service
  227 |      * @param groupName   group of service
  228 |      * @return A list of instance
  229 |      * @throws NacosException nacos exception
  230 |      */
  231 |     @Since("0.2.0")
  232 |     List<Instance> getAllInstances(String serviceName, String groupName) throws NacosException;
  233 |     
  234 |     /**
  235 |      * Get all instances of a service.
  236 |      *
  237 |      * @param serviceName name of service
  238 |      * @param subscribe   if subscribe the service
  239 |      * @return A list of instance
  240 |      * @throws NacosException nacos exception
  241 |      */
  242 |     @Since("0.2.0")
  243 |     List<Instance> getAllInstances(String serviceName, boolean subscribe) throws NacosException;
  244 |     
  245 |     /**
  246 |      * Get all instances of a service.
  247 |      *
  248 |      * @param serviceName name of service
  249 |      * @param groupName   group of service
  250 |      * @param subscribe   if subscribe the service
  251 |      * @return A list of instance
  252 |      * @throws NacosException nacos exception
  253 |      */
  254 |     @Since("0.8.0")
  255 |     List<Instance> getAllInstances(String serviceName, String groupName, boolean subscribe)
  256 |         throws NacosException;
  257 |     
  258 |     /**
  259 |      * Get all instances within specified clusters of a service.
  260 |      *
  261 |      * @param serviceName name of service
  262 |      * @param clusters    list of cluster
  263 |      * @return A list of qualified instance
  264 |      * @throws NacosException nacos exception
  265 |      */
  266 |     @Since("0.2.0")
  267 |     List<Instance> getAllInstances(String serviceName, List<String> clusters) throws NacosException;
  268 |     
  269 |     /**
  270 |      * Get all instances within specified clusters of a service.
  271 |      *
  272 |      * @param serviceName name of service
  273 |      * @param groupName   group of service
  274 |      * @param clusters    list of cluster
  275 |      * @return A list of qualified instance
  276 |      * @throws NacosException nacos exception
  277 |      */
  278 |     @Since("0.8.0")
  279 |     List<Instance> getAllInstances(String serviceName, String groupName, List<String> clusters)
  280 |         throws NacosException;
  281 |     
  282 |     /**
  283 |      * Get all instances within specified clusters of a service.
  284 |      *
  285 |      * @param serviceName name of service
  286 |      * @param clusters    list of cluster
  287 |      * @param subscribe   if subscribe the service
  288 |      * @return A list of qualified instance
  289 |      * @throws NacosException nacos exception
  290 |      */
  291 |     @Since("0.8.0")
  292 |     List<Instance> getAllInstances(String serviceName, List<String> clusters, boolean subscribe)
  293 |         throws NacosException;
  294 |     
  295 |     /**
  296 |      * Get all instances within specified clusters of a service.
  297 |      *
  298 |      * @param serviceName name of service
  299 |      * @param groupName   group of service
  300 |      * @param clusters    list of cluster
  301 |      * @param subscribe   if subscribe the service
  302 |      * @return A list of qualified instance
  303 |      * @throws NacosException nacos exception
  304 |      */
  305 |     @Since("1.0.0")
  306 |     List<Instance> getAllInstances(String serviceName, String groupName, List<String> clusters,
  307 |         boolean subscribe)
  308 |         throws NacosException;
  309 |     
  310 |     /**
  311 |      * Get qualified instances of service.
  312 |      *
  313 |      * @param serviceName name of service.
  314 |      * @param healthy     a flag to indicate returning healthy or unhealthy instances
  315 |      * @return A qualified list of instance
  316 |      * @throws NacosException nacos exception
  317 |      */
  318 |     @Since("0.2.0")
  319 |     List<Instance> selectInstances(String serviceName, boolean healthy) throws NacosException;
  320 |     
  321 |     /**
  322 |      * Get qualified instances of service.
  323 |      *
  324 |      * @param serviceName name of service
  325 |      * @param groupName   group of service
  326 |      * @param healthy     a flag to indicate returning healthy or unhealthy instances
  327 |      * @return A qualified list of instance
  328 |      * @throws NacosException nacos exception
  329 |      */
  330 |     @Since("0.2.0")
  331 |     List<Instance> selectInstances(String serviceName, String groupName, boolean healthy)
  332 |         throws NacosException;
  333 |     
  334 |     /**
  335 |      * Get qualified instances of service.
  336 |      *
  337 |      * @param serviceName name of service
  338 |      * @param healthy     a flag to indicate returning healthy or unhealthy instances
  339 |      * @param subscribe   if subscribe the service
  340 |      * @return A qualified list of instance
  341 |      * @throws NacosException nacos exception
  342 |      */
  343 |     @Since("0.2.0")
  344 |     List<Instance> selectInstances(String serviceName, boolean healthy, boolean subscribe)
  345 |         throws NacosException;
  346 |     
  347 |     /**
  348 |      * Get qualified instances of service.
  349 |      *
  350 |      * @param serviceName name of service
  351 |      * @param groupName   group of service
  352 |      * @param healthy     a flag to indicate returning healthy or unhealthy instances
  353 |      * @param subscribe   if subscribe the service
  354 |      * @return A qualified list of instance
  355 |      * @throws NacosException nacos exception
  356 |      */
  357 |     @Since("0.8.0")
  358 |     List<Instance> selectInstances(String serviceName, String groupName, boolean healthy,
  359 |         boolean subscribe)
  360 |         throws NacosException;
  361 |     
  362 |     /**
  363 |      * Get qualified instances within specified clusters of service.
  364 |      *
  365 |      * @param serviceName name of service
  366 |      * @param clusters    list of cluster
  367 |      * @param healthy     a flag to indicate returning healthy or unhealthy instances
  368 |      * @return A qualified list of instance
  369 |      * @throws NacosException nacos exception
  370 |      */
  371 |     @Since("0.2.0")
  372 |     List<Instance> selectInstances(String serviceName, List<String> clusters, boolean healthy)
  373 |         throws NacosException;
  374 |     
  375 |     /**
  376 |      * Get qualified instances within specified clusters of service.
  377 |      *
  378 |      * @param serviceName name of service
  379 |      * @param groupName   group of service
  380 |      * @param clusters    list of cluster
  381 |      * @param healthy     a flag to indicate returning healthy or unhealthy instances
  382 |      * @return A qualified list of instance
  383 |      * @throws NacosException nacos exception
  384 |      */
  385 |     @Since("0.8.0")
  386 |     List<Instance> selectInstances(String serviceName, String groupName, List<String> clusters,
  387 |         boolean healthy)
  388 |         throws NacosException;
  389 |     
  390 |     /**
  391 |      * Get qualified instances within specified clusters of service.
  392 |      *
  393 |      * @param serviceName name of service
  394 |      * @param clusters    list of cluster
  395 |      * @param healthy     a flag to indicate returning healthy or unhealthy instances
  396 |      * @param subscribe   if subscribe the service
  397 |      * @return A qualified list of instance
  398 |      * @throws NacosException nacos exception
  399 |      */
  400 |     @Since("0.8.0")
  401 |     List<Instance> selectInstances(String serviceName, List<String> clusters, boolean healthy,
  402 |         boolean subscribe)
  403 |         throws NacosException;
  404 |     
  405 |     /**
  406 |      * Get qualified instances within specified clusters of service.
  407 |      *
  408 |      * @param serviceName name of service
  409 |      * @param groupName   group of service
  410 |      * @param clusters    list of cluster
  411 |      * @param healthy     a flag to indicate returning healthy or unhealthy instances
  412 |      * @param subscribe   if subscribe the service
  413 |      * @return A qualified list of instance
  414 |      * @throws NacosException nacos exception
  415 |      */
  416 |     @Since("1.0.0")
  417 |     List<Instance> selectInstances(String serviceName, String groupName, List<String> clusters,
  418 |         boolean healthy,
  419 |         boolean subscribe) throws NacosException;
  420 |     
  421 |     /**
  422 |      * Select one healthy instance of service using predefined load balance strategy.
  423 |      *
  424 |      * @param serviceName name of service
  425 |      * @return qualified instance
  426 |      * @throws NacosException nacos exception
  427 |      */
  428 |     @Since("0.2.0")
  429 |     Instance selectOneHealthyInstance(String serviceName) throws NacosException;
  430 |     
  431 |     /**
  432 |      * Select one healthy instance of service using predefined load balance strategy.
  433 |      *
  434 |      * @param serviceName name of service
  435 |      * @param groupName   group of service
  436 |      * @return qualified instance
  437 |      * @throws NacosException nacos exception
  438 |      */
  439 |     @Since("0.2.0")
  440 |     Instance selectOneHealthyInstance(String serviceName, String groupName) throws NacosException;
  441 |     
  442 |     /**
  443 |      * select one healthy instance of service using predefined load balance strategy.
  444 |      *
  445 |      * @param serviceName name of service
  446 |      * @param subscribe   if subscribe the service
  447 |      * @return qualified instance
  448 |      * @throws NacosException nacos exception
  449 |      */
  450 |     @Since("0.2.0")
  451 |     Instance selectOneHealthyInstance(String serviceName, boolean subscribe) throws NacosException;
  452 |     
  453 |     /**
  454 |      * select one healthy instance of service using predefined load balance strategy.
  455 |      *
  456 |      * @param serviceName name of service
  457 |      * @param groupName   group of service
  458 |      * @param subscribe   if subscribe the service
  459 |      * @return qualified instance
  460 |      * @throws NacosException nacos exception
  461 |      */
  462 |     @Since("0.8.0")
  463 |     Instance selectOneHealthyInstance(String serviceName, String groupName, boolean subscribe)
  464 |         throws NacosException;
  465 |     
  466 |     /**
  467 |      * Select one healthy instance of service using predefined load balance strategy.
  468 |      *
  469 |      * @param serviceName name of service
  470 |      * @param clusters    a list of clusters should the instance belongs to
  471 |      * @return qualified instance
  472 |      * @throws NacosException nacos exception
  473 |      */
  474 |     @Since("0.2.0")
  475 |     Instance selectOneHealthyInstance(String serviceName, List<String> clusters)
  476 |         throws NacosException;
  477 |     
  478 |     /**
  479 |      * Select one healthy instance of service using predefined load balance strategy.
  480 |      *
  481 |      * @param serviceName name of service
  482 |      * @param groupName   group of service
  483 |      * @param clusters    a list of clusters should the instance belongs to
  484 |      * @return qualified instance
  485 |      * @throws NacosException nacos exception
  486 |      */
  487 |     @Since("0.8.0")
  488 |     Instance selectOneHealthyInstance(String serviceName, String groupName, List<String> clusters)
  489 |         throws NacosException;
  490 |     
  491 |     /**
  492 |      * Select one healthy instance of service using predefined load balance strategy.
  493 |      *
  494 |      * @param serviceName name of service
  495 |      * @param clusters    a list of clusters should the instance belongs to
  496 |      * @param subscribe   if subscribe the service
  497 |      * @return qualified instance
  498 |      * @throws NacosException nacos exception
  499 |      */
  500 |     @Since("0.8.0")
  501 |     Instance selectOneHealthyInstance(String serviceName, List<String> clusters, boolean subscribe)
  502 |         throws NacosException;
  503 |     
  504 |     /**
  505 |      * Select one healthy instance of service using predefined load balance strategy.
  506 |      *
  507 |      * @param serviceName name of service
  508 |      * @param groupName   group of service
  509 |      * @param clusters    a list of clusters should the instance belongs to
  510 |      * @param subscribe   if subscribe the service
  511 |      * @return qualified instance
  512 |      * @throws NacosException nacos exception
  513 |      */
  514 |     @Since("1.0.0")
  515 |     Instance selectOneHealthyInstance(String serviceName, String groupName, List<String> clusters,
  516 |         boolean subscribe)
  517 |         throws NacosException;
  518 |     
  519 |     /**
  520 |      * Subscribe service to receive events of instances alteration.
  521 |      *
  522 |      * @param serviceName name of service
  523 |      * @param listener    event listener
  524 |      * @throws NacosException nacos exception
  525 |      */
  526 |     @Since("0.2.0")
  527 |     void subscribe(String serviceName, EventListener listener) throws NacosException;
  528 |     
  529 |     /**
  530 |      * Subscribe service to receive events of instances alteration.
  531 |      *
  532 |      * @param serviceName name of service
  533 |      * @param groupName   group of service
  534 |      * @param listener    event listener
  535 |      * @throws NacosException nacos exception
  536 |      */
  537 |     @Since("0.2.0")
  538 |     void subscribe(String serviceName, String groupName, EventListener listener)
  539 |         throws NacosException;
  540 |     
  541 |     /**
  542 |      * Subscribe service to receive events of instances alteration.
  543 |      *
  544 |      * @param serviceName name of service
  545 |      * @param clusters    list of cluster
  546 |      * @param listener    event listener
  547 |      * @throws NacosException nacos exception
  548 |      */
  549 |     @Since("0.2.0")
  550 |     void subscribe(String serviceName, List<String> clusters, EventListener listener)
  551 |         throws NacosException;
  552 |     
  553 |     /**
  554 |      * Subscribe service to receive events of instances alteration.
  555 |      *
  556 |      * @param serviceName name of service
  557 |      * @param groupName   group of service
  558 |      * @param clusters    list of cluster
  559 |      * @param listener    event listener
  560 |      * @throws NacosException nacos exception
  561 |      */
  562 |     @Since("1.0.0")
  563 |     void subscribe(String serviceName, String groupName, List<String> clusters,
  564 |         EventListener listener)
  565 |         throws NacosException;
  566 |     
  567 |     /**
  568 |      * Subscribe service to receive events of instances alteration.
  569 |      *
  570 |      * @param serviceName name of service
  571 |      * @param selector    selector of instances
  572 |      * @param listener    event listener
  573 |      * @throws NacosException nacos exception
  574 |      */
  575 |     @Since("0.2.0")
  576 |     void subscribe(String serviceName, NamingSelector selector, EventListener listener)
  577 |         throws NacosException;
  578 |     
  579 |     /**
  580 |      * Subscribe service to receive events of instances alteration.
  581 |      *
  582 |      * @param serviceName name of service
  583 |      * @param groupName   group of service
  584 |      * @param selector    selector of instances
  585 |      * @param listener    event listener
  586 |      * @throws NacosException nacos exception
  587 |      */
  588 |     @Since("1.0.0")
  589 |     void subscribe(String serviceName, String groupName, NamingSelector selector,
  590 |         EventListener listener)
  591 |         throws NacosException;
  592 |     
  593 |     /**
  594 |      * Unsubscribe event listener of service.
  595 |      *
  596 |      * @param serviceName name of service
  597 |      * @param listener    event listener
  598 |      * @throws NacosException nacos exception
  599 |      */
  600 |     @Since("0.2.0")
  601 |     void unsubscribe(String serviceName, EventListener listener) throws NacosException;
  602 |     
  603 |     /**
  604 |      * unsubscribe event listener of service.
  605 |      *
  606 |      * @param serviceName name of service
  607 |      * @param groupName   group of service
  608 |      * @param listener    event listener
  609 |      * @throws NacosException nacos exception
  610 |      */
  611 |     @Since("0.2.0")
  612 |     void unsubscribe(String serviceName, String groupName, EventListener listener)
  613 |         throws NacosException;
  614 |     
  615 |     /**
  616 |      * Unsubscribe event listener of service.
  617 |      *
  618 |      * @param serviceName name of service
  619 |      * @param clusters    list of cluster
  620 |      * @param listener    event listener
  621 |      * @throws NacosException nacos exception
  622 |      */
  623 |     @Since("0.2.0")
  624 |     void unsubscribe(String serviceName, List<String> clusters, EventListener listener)
  625 |         throws NacosException;
  626 |     
  627 |     /**
  628 |      * Unsubscribe event listener of service.
  629 |      *
  630 |      * @param serviceName name of service
  631 |      * @param groupName   group of service
  632 |      * @param clusters    list of cluster
  633 |      * @param listener    event listener
  634 |      * @throws NacosException nacos exception
  635 |      */
  636 |     @Since("1.0.0")
  637 |     void unsubscribe(String serviceName, String groupName, List<String> clusters,
  638 |         EventListener listener)
  639 |         throws NacosException;
  640 |     
  641 |     /**
  642 |      * Unsubscribe event listener of service.
  643 |      *
  644 |      * @param serviceName name of service
  645 |      * @param selector    selector of instances
  646 |      * @param listener    event listener
  647 |      * @throws NacosException nacos exception
  648 |      */
  649 |     @Since("0.2.0")
  650 |     void unsubscribe(String serviceName, NamingSelector selector, EventListener listener)
  651 |         throws NacosException;
  652 |     
  653 |     /**
  654 |      * Unsubscribe event listener of service.
  655 |      *
  656 |      * @param serviceName name of service
  657 |      * @param groupName   group of service
  658 |      * @param selector    selector of instances
  659 |      * @param listener    event listener
  660 |      * @throws NacosException nacos exception
  661 |      */
  662 |     @Since("1.0.0")
  663 |     void unsubscribe(String serviceName, String groupName, NamingSelector selector,
  664 |         EventListener listener)
  665 |         throws NacosException;
  666 |     
  667 |     /**
  668 |      * According to matching rules, watch services within a specific scope, and receive notifications when
  669 |      * changes occur in the services within the scope.
  670 |      * When given a fixed group name, watch changes in all services under this group.
  671 |      *
  672 |      * @param groupNamePattern  group name pattern for fuzzy watch
  673 |      * @param listener event listener
  674 |      * @throws NacosException nacos exception
  675 |      */
  676 |     @Since("3.0.0")
  677 |     void fuzzyWatch(String groupNamePattern, FuzzyWatchEventWatcher listener) throws NacosException;
  678 |     
  679 |     /**
  680 |      * According to matching rules, watch services within a specific scope, and receive notifications when
  681 |      * changes occur in the services within the scope.
  682 |      * When provided with a fixed group name and pattern of service name, watch changes in services under
  683 |      * this group that match the specified pattern.
  684 |      *
  685 |      * @param serviceNamePattern service name pattern for fuzzy watch
  686 |      * @param groupNamePattern  group name pattern for fuzzy watch
  687 |      * @param listener event listener
  688 |      * @throws NacosException nacos exception
  689 |      */
  690 |     @Since("3.0.0")
  691 |     void fuzzyWatch(String serviceNamePattern, String groupNamePattern,
  692 |         FuzzyWatchEventWatcher listener) throws NacosException;
  693 |     
  694 |     /**
  695 |      * According to matching rules, watch services within a specific scope, and receive notifications when
  696 |      * changes occur in the services within the scope.
  697 |      * When given a fixed group name, watch changes in all services under this group.
  698 |      *
  699 |      * @param groupNamePattern  group name pattern for fuzzy watch
  700 |      * @param listener event listener
  701 |      * @return matched service keys.
  702 |      * @throws NacosException nacos exception
  703 |      */
  704 |     @Since("3.0.0")
  705 |     Future<ListView<String>> fuzzyWatchWithServiceKeys(String groupNamePattern,
  706 |         FuzzyWatchEventWatcher listener) throws NacosException;
  707 |     
  708 |     /**
  709 |      * According to matching rules, watch services within a specific scope, and receive notifications when
  710 |      * changes occur in the services within the scope.
  711 |      * When provided with a fixed group name and pattern of service name, watch changes in services under
  712 |      * this group that match the specified pattern.
  713 |      *
  714 |      * @param serviceNamePattern service name pattern for fuzzy watch
  715 |      * @param groupNamePattern  group name pattern for fuzzy watch
  716 |      * @param listener event listener
  717 |      * @return matched service keys.
  718 |      * @throws NacosException nacos exception
  719 |      */
  720 |     @Since("3.0.0")
  721 |     Future<ListView<String>> fuzzyWatchWithServiceKeys(String serviceNamePattern,
  722 |         String groupNamePattern,
  723 |         FuzzyWatchEventWatcher listener) throws NacosException;
  724 |     
  725 |     /**
  726 |      * Cancel fuzzy watch, and remove event listener of a pattern.
  727 |      *
  728 |      * @param groupNamePattern  group name for fuzzy watch
  729 |      * @param listener event listener
  730 |      * @throws NacosException nacos exception
  731 |      */
  732 |     @Since("3.0.0")
  733 |     void cancelFuzzyWatch(String groupNamePattern, FuzzyWatchEventWatcher listener)
  734 |         throws NacosException;
  735 |     
  736 |     /**
  737 |      * Cancel fuzzy watch, and remove event listener of a pattern.
  738 |      *
  739 |      * @param serviceNamePattern service name pattern for fuzzy watch
  740 |      * @param groupNamePattern fixed group name for fuzzy watch
  741 |      * @param listener event listener
  742 |      * @throws NacosException nacos exception
  743 |      */
  744 |     @Since("3.0.0")
  745 |     void cancelFuzzyWatch(String serviceNamePattern, String groupNamePattern,
  746 |         FuzzyWatchEventWatcher listener) throws NacosException;
  747 |     
  748 |     /**
  749 |      * Get all service names from server.
  750 |      *
  751 |      * @param pageNo   page index
  752 |      * @param pageSize page size
  753 |      * @return list of service names
  754 |      * @throws NacosException nacos exception
  755 |      */
  756 |     @Since("0.2.0")
  757 |     ListView<String> getServicesOfServer(int pageNo, int pageSize) throws NacosException;
  758 |     
  759 |     /**
  760 |      * Get all service names from server.
  761 |      *
  762 |      * @param pageNo    page index
  763 |      * @param pageSize  page size
  764 |      * @param groupName group name
  765 |      * @return list of service names
  766 |      * @throws NacosException nacos exception
  767 |      */
  768 |     @Since("0.7.0")
  769 |     ListView<String> getServicesOfServer(int pageNo, int pageSize, String groupName)
  770 |         throws NacosException;
  771 |     
  772 |     /**
  773 |      * Get all service names from server with selector.
  774 |      *
  775 |      * @param pageNo   page index
  776 |      * @param pageSize page size
  777 |      * @param selector selector to filter the resource
  778 |      * @return list of service names
  779 |      * @throws NacosException nacos exception
  780 |      * @since 0.7.0
  781 |      * @deprecated after 3.3.0.
  782 |      */
  783 |     @Since("0.7.0")
  784 |     @Deprecated
  785 |     ListView<String> getServicesOfServer(int pageNo, int pageSize, AbstractSelector selector)
  786 |         throws NacosException;
  787 |     
  788 |     /**
  789 |      * Get all service names from server with selector.
  790 |      *
  791 |      * @param pageNo    page index
  792 |      * @param pageSize  page size
  793 |      * @param groupName group name
  794 |      * @param selector  selector to filter the resource
  795 |      * @return list of service names
  796 |      * @throws NacosException nacos exception
  797 |      * @deprecated after 3.3.0.
  798 |      */
  799 |     @Since("1.0.0")
  800 |     @Deprecated
  801 |     ListView<String> getServicesOfServer(int pageNo, int pageSize, String groupName,
  802 |         AbstractSelector selector)
  803 |         throws NacosException;
  804 |     
  805 |     /**
  806 |      * Get all subscribed services of current client.
  807 |      *
  808 |      * @return subscribed services
  809 |      * @throws NacosException nacos exception
  810 |      */
  811 |     @Since("0.2.0")
  812 |     List<ServiceInfo> getSubscribeServices() throws NacosException;
  813 |     
  814 |     /**
  815 |      * get server health status.
  816 |      *
  817 |      * @return is server healthy
  818 |      */
  819 |     @Since("0.2.0")
  820 |     String getServerStatus();
  821 |     
  822 |     /**
  823 |      * Shutdown the resource service.
  824 |      *
  825 |      * @throws NacosException exception.
  826 |      */
  827 |     @Since("1.3.1")
  828 |     void shutDown() throws NacosException;
  829 | }
```
