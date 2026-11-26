<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.apache.dubbo:dubbo:3.3.6 | path: org/apache/dubbo/config/annotation/DubboReference.java -->
<!-- 正文引用：[[源码:DubboReference.java#L起-L止]] ；代码块头部：(src: DubboReference.java#L起-L止) -->

# DubboReference.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.apache.dubbo:dubbo:3.3.6` |
| 路径 | `org/apache/dubbo/config/annotation/DubboReference.java` |
| 行数 | 386 |
| 在线地址 | https://repo1.maven.org/maven2/org/apache/dubbo/dubbo/3.3.6/dubbo-3.3.6-sources.jar |
| 抓取时间 | 2026-11-25T15:33:30.788Z |

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
   17 | package org.apache.dubbo.config.annotation;
   18 | 
   19 | import org.apache.dubbo.common.constants.ClusterRules;
   20 | import org.apache.dubbo.common.constants.LoadbalanceRules;
   21 | import org.apache.dubbo.common.constants.RegistryConstants;
   22 | import org.apache.dubbo.config.AbstractReferenceConfig;
   23 | import org.apache.dubbo.config.ReferenceConfigBase;
   24 | 
   25 | import java.lang.annotation.Documented;
   26 | import java.lang.annotation.ElementType;
   27 | import java.lang.annotation.Retention;
   28 | import java.lang.annotation.RetentionPolicy;
   29 | import java.lang.annotation.Target;
   30 | 
   31 | /**
   32 |  * An annotation used for referencing a Dubbo service.
   33 |  * <p>
   34 |  * <b>It is recommended to use @DubboReference on the @Bean method in the Java-config class, but not on the fields or setter methods to be injected.</b>
   35 |  * </p>
   36 |  * <p>
   37 |  * Step 1: Register ReferenceBean in Java-config class:
   38 |  * <pre class="code">
   39 |  * &#64;Configuration
   40 |  * public class ReferenceConfiguration {
   41 |  *     &#64;Bean
   42 |  *     &#64;DubboReference(group = "demo")
   43 |  *     public ReferenceBean&lt;HelloService&gt; helloService() {
   44 |  *         return new ReferenceBean();
   45 |  *     }
   46 |  *
   47 |  *     &#64;Bean
   48 |  *     &#64;DubboReference(group = "demo", interfaceClass = HelloService.class)
   49 |  *     public ReferenceBean&lt;GenericService&gt; genericHelloService() {
   50 |  *         return new ReferenceBean();
   51 |  *     }
   52 |  * }
   53 |  * </pre>
   54 |  * <p>
   55 |  * Step 2: Inject ReferenceBean by @Autowired
   56 |  * <pre class="code">
   57 |  * public class FooController {
   58 |  *     &#64;Autowired
   59 |  *     private HelloService helloService;
   60 |  *
   61 |  *     &#64;Autowired
   62 |  *     private GenericService genericHelloService;
   63 |  * }
   64 |  * </pre>
   65 |  *
   66 |  * @see org.apache.dubbo.config.spring.reference.ReferenceBeanBuilder
   67 |  * @see org.apache.dubbo.config.spring.ReferenceBean
   68 |  * @since 2.7.7
   69 |  */
   70 | @Documented
   71 | @Retention(RetentionPolicy.RUNTIME)
   72 | @Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
   73 | public @interface DubboReference {
   74 | 
   75 |     /**
   76 |      * Interface class, default value is void.class
   77 |      */
   78 |     Class<?> interfaceClass() default void.class;
   79 | 
   80 |     /**
   81 |      * Interface class name, default value is empty string
   82 |      */
   83 |     String interfaceName() default "";
   84 | 
   85 |     /**
   86 |      * Service version, default value is empty string
   87 |      */
   88 |     String version() default "";
   89 | 
   90 |     /**
   91 |      * Service group, default value is empty string
   92 |      */
   93 |     String group() default "";
   94 | 
   95 |     /**
   96 |      * Service target URL for direct invocation, if this is specified, then registry center takes no effect.
   97 |      */
   98 |     String url() default "";
   99 | 
  100 |     /**
  101 |      * Client transport type, default value is "netty"
  102 |      */
  103 |     String client() default "";
  104 | 
  105 |     /**
  106 |      * Whether to enable generic invocation, default value is false
  107 |      *
  108 |      * @deprecated Do not need specify generic value, judge by injection type and interface class
  109 |      */
  110 |     @Deprecated
  111 |     boolean generic() default false;
  112 | 
  113 |     /**
  114 |      * When enable, prefer to call local service in the same JVM if it's present, default value is true
  115 |      *
  116 |      * @deprecated using scope="local" or scope="remote" instead
  117 |      */
  118 |     @Deprecated
  119 |     boolean injvm() default true;
  120 | 
  121 |     /**
  122 |      * Check if service provider is available during boot up, default value is true
  123 |      */
  124 |     boolean check() default true;
  125 | 
  126 |     /**
  127 |      * Whether eager initialize the reference bean when all properties are set, default value is true ( null as true)
  128 |      *
  129 |      * @see ReferenceConfigBase#shouldInit()
  130 |      */
  131 |     boolean init() default true;
  132 | 
  133 |     /**
  134 |      * Whether to make connection when the client is created, the default value is false
  135 |      */
  136 |     boolean lazy() default false;
  137 | 
  138 |     /**
  139 |      * Export an stub service for event dispatch, default value is false.
  140 |      * <p>
  141 |      * see org.apache.dubbo.rpc.Constants#STUB_EVENT_METHODS_KEY
  142 |      */
  143 |     boolean stubevent() default false;
  144 | 
  145 |     /**
  146 |      * Whether to reconnect if connection is lost, if not specify, reconnect is enabled by default, and the interval
  147 |      * for retry connecting is 2000 ms
  148 |      * <p>
  149 |      * see org.apache.dubbo.remoting.Constants#DEFAULT_RECONNECT_PERIOD
  150 |      */
  151 |     String reconnect() default "";
  152 | 
  153 |     /**
  154 |      * Whether to stick to the same node in the cluster, the default value is false
  155 |      * <p>
  156 |      * see Constants#DEFAULT_CLUSTER_STICKY
  157 |      */
  158 |     boolean sticky() default false;
  159 | 
  160 |     /**
  161 |      * How the proxy is generated, legal values include: jdk, javassist
  162 |      */
  163 |     String proxy() default "";
  164 | 
  165 |     /**
  166 |      * Service stub name, use interface name + Stub if not set
  167 |      */
  168 |     String stub() default "";
  169 | 
  170 |     /**
  171 |      * Cluster strategy, legal values include: failover, failfast, failsafe, failback, forking
  172 |      * you can use {@link org.apache.dubbo.common.constants.ClusterRules#FAIL_FAST} ……
  173 |      */
  174 |     String cluster() default ClusterRules.EMPTY;
  175 | 
  176 |     /**
  177 |      * Maximum connections service provider can accept, default value is 0 - connection is shared
  178 |      */
  179 |     int connections() default -1;
  180 | 
  181 |     /**
  182 |      * The callback instance limit peer connection
  183 |      * <p>
  184 |      * see org.apache.dubbo.rpc.Constants#DEFAULT_CALLBACK_INSTANCES
  185 |      */
  186 |     int callbacks() default -1;
  187 | 
  188 |     /**
  189 |      * Callback method name when connected, default value is empty string
  190 |      */
  191 |     String onconnect() default "";
  192 | 
  193 |     /**
  194 |      * Callback method name when disconnected, default value is empty string
  195 |      */
  196 |     String ondisconnect() default "";
  197 | 
  198 |     /**
  199 |      * Service owner, default value is empty string
  200 |      */
  201 |     String owner() default "";
  202 | 
  203 |     /**
  204 |      * Service layer, default value is empty string
  205 |      */
  206 |     String layer() default "";
  207 | 
  208 |     /**
  209 |      * Service invocation retry times
  210 |      * <p>
  211 |      * see Constants#DEFAULT_RETRIES
  212 |      */
  213 |     int retries() default -1;
  214 | 
  215 |     /**
  216 |      * Load balance strategy, legal values include: random, roundrobin, leastactive
  217 |      * you can use {@link org.apache.dubbo.common.constants.LoadbalanceRules#RANDOM} ……
  218 |      */
  219 |     String loadbalance() default LoadbalanceRules.EMPTY;
  220 | 
  221 |     /**
  222 |      * Whether to enable async invocation, default value is false
  223 |      */
  224 |     boolean async() default false;
  225 | 
  226 |     /**
  227 |      * Maximum active requests allowed, default value is 0
  228 |      */
  229 |     int actives() default -1;
  230 | 
  231 |     /**
  232 |      * Whether the async request has already been sent, the default value is false
  233 |      */
  234 |     boolean sent() default false;
  235 | 
  236 |     /**
  237 |      * Service mock name, use interface name + Mock if not set
  238 |      */
  239 |     String mock() default "";
  240 | 
  241 |     /**
  242 |      * Whether to use JSR303 validation, legal values are: true, false
  243 |      */
  244 |     String validation() default "";
  245 | 
  246 |     /**
  247 |      * Timeout value for service invocation, default value is 0
  248 |      */
  249 |     int timeout() default -1;
  250 | 
  251 |     /**
  252 |      * Specify cache implementation for service invocation, legal values include: lru, threadlocal, jcache
  253 |      */
  254 |     String cache() default "";
  255 | 
  256 |     /**
  257 |      * Filters for service invocation
  258 |      * <p>
  259 |      * see Filter
  260 |      */
  261 |     String[] filter() default {};
  262 | 
  263 |     /**
  264 |      * Listeners for service exporting and unexporting
  265 |      * <p>
  266 |      * see ExporterListener
  267 |      */
  268 |     String[] listener() default {};
  269 | 
  270 |     /**
  271 |      * Customized parameter key-value pair, for example: {key1, value1, key2, value2} or {"key1=value1", "key2=value2"}
  272 |      */
  273 |     String[] parameters() default {};
  274 | 
  275 |     /**
  276 |      * Application name
  277 |      *
  278 |      * @deprecated This attribute was deprecated, use bind application/module of spring ApplicationContext
  279 |      */
  280 |     @Deprecated
  281 |     String application() default "";
  282 | 
  283 |     /**
  284 |      * Module associated name
  285 |      */
  286 |     String module() default "";
  287 | 
  288 |     /**
  289 |      * Consumer associated name
  290 |      */
  291 |     String consumer() default "";
  292 | 
  293 |     /**
  294 |      * Monitor associated name
  295 |      */
  296 |     String monitor() default "";
  297 | 
  298 |     /**
  299 |      * Registry associated name
  300 |      */
  301 |     String[] registry() default {};
  302 | 
  303 |     /**
  304 |      * The communication protocol of Dubbo Service
  305 |      *
  306 |      * @return the default value is ""
  307 |      * @since 2.6.6
  308 |      */
  309 |     String protocol() default "";
  310 | 
  311 |     /**
  312 |      * Service tag name
  313 |      */
  314 |     String tag() default "";
  315 | 
  316 |     /**
  317 |      * Service merger
  318 |      */
  319 |     String merger() default "";
  320 | 
  321 |     /**
  322 |      * methods support
  323 |      */
  324 |     Method[] methods() default {};
  325 | 
  326 |     /**
  327 |      * The id
  328 |      * NOTE: The id attribute is ignored when using @DubboReference on @Bean method
  329 |      *
  330 |      * @return default value is empty
  331 |      * @since 2.7.3
  332 |      */
  333 |     String id() default "";
  334 | 
  335 |     /**
  336 |      * @return The service names that the Dubbo interface subscribed
  337 |      * @see RegistryConstants#SUBSCRIBED_SERVICE_NAMES_KEY
  338 |      * @since 2.7.8
  339 |      * @deprecated using {@link DubboReference#providedBy()}
  340 |      */
  341 |     @Deprecated
  342 |     String[] services() default {};
  343 | 
  344 |     /**
  345 |      * declares which app or service this interface belongs to
  346 |      *
  347 |      * @see RegistryConstants#PROVIDED_BY
  348 |      */
  349 |     String[] providedBy() default {};
  350 | 
  351 |     /**
  352 |      * The service port of the provider
  353 |      *
  354 |      * @see AbstractReferenceConfig#providerPort
  355 |      * @since 3.1.0
  356 |      */
  357 |     int providerPort() default -1;
  358 | 
  359 |     /**
  360 |      * assign the namespace that provider belong to
  361 |      * @see AbstractReferenceConfig#providerNamespace
  362 |      * @since 3.1.1
  363 |      */
  364 |     String providerNamespace() default "";
  365 | 
  366 |     /**
  367 |      * the scope for referring/exporting a service, if it's local, it means searching in current JVM only.
  368 |      *
  369 |      * @see org.apache.dubbo.rpc.Constants#SCOPE_LOCAL
  370 |      * @see org.apache.dubbo.rpc.Constants#SCOPE_REMOTE
  371 |      */
  372 |     String scope() default "";
  373 | 
  374 |     /**
  375 |      * Weather the reference is refer asynchronously
  376 |      */
  377 |     boolean referAsync() default false;
  378 | 
  379 |     /**
  380 |      * unload Cluster related in mesh mode
  381 |      *
  382 |      * @see ReferenceConfigBase#unloadClusterRelated
  383 |      * @since 3.1.0
  384 |      */
  385 |     boolean unloadClusterRelated() default false;
  386 | }
```
