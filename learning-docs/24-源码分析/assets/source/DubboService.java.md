<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.apache.dubbo:dubbo:3.3.6 | path: org/apache/dubbo/config/annotation/DubboService.java -->
<!-- 正文引用：[[源码:DubboService.java#L起-L止]] ；代码块头部：(src: DubboService.java#L起-L止) -->

# DubboService.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.apache.dubbo:dubbo:3.3.6` |
| 路径 | `org/apache/dubbo/config/annotation/DubboService.java` |
| 行数 | 357 |
| 在线地址 | https://repo1.maven.org/maven2/org/apache/dubbo/dubbo/3.3.6/dubbo-3.3.6-sources.jar |
| 抓取时间 | 2026-11-25T15:33:30.964Z |

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
   21 | 
   22 | import java.lang.annotation.Documented;
   23 | import java.lang.annotation.ElementType;
   24 | import java.lang.annotation.Inherited;
   25 | import java.lang.annotation.Retention;
   26 | import java.lang.annotation.RetentionPolicy;
   27 | import java.lang.annotation.Target;
   28 | 
   29 | /**
   30 |  * Class-level annotation used for declaring Dubbo service.
   31 |  * <p/>
   32 |  * <b>1. Using with java config bean:</b>
   33 |  * <p/>
   34 |  * <b>This usage is recommended</b>.<br/>
   35 |  * It is more flexible on bean methods than on implementation classes, and is more compatible with Spring.
   36 |  * <pre>
   37 |  * &#64;Configuration
   38 |  * class ProviderConfiguration {
   39 |  *
   40 |  *     &#64;Bean
   41 |  *     &#64;DubboService(group="demo")
   42 |  *     public DemoService demoServiceImpl() {
   43 |  *         return new DemoServiceImpl();
   44 |  *     }
   45 |  * }
   46 |  * </pre>
   47 |  *
   48 |  * <b>2. Using on implementation class of service:  </b>
   49 |  * <pre>
   50 |  * &#64;DubboService(group="demo")
   51 |  * public class DemoServiceImpl implements DemoService {
   52 |  *     ...
   53 |  * }
   54 |  * </pre>
   55 |  *
   56 |  * This usage causes the implementation class to rely on the Dubbo module.
   57 |  *
   58 |  *
   59 |  * @since 2.7.7
   60 |  */
   61 | @Documented
   62 | @Retention(RetentionPolicy.RUNTIME)
   63 | @Target({ElementType.TYPE, ElementType.METHOD})
   64 | @Inherited
   65 | public @interface DubboService {
   66 | 
   67 |     /**
   68 |      * Interface class, default value is void.class
   69 |      */
   70 |     Class<?> interfaceClass() default void.class;
   71 | 
   72 |     /**
   73 |      * Interface class name, default value is empty string
   74 |      */
   75 |     String interfaceName() default "";
   76 | 
   77 |     /**
   78 |      * Service version, default value is empty string
   79 |      */
   80 |     String version() default "";
   81 | 
   82 |     /**
   83 |      * Service group, default value is empty string
   84 |      */
   85 |     String group() default "";
   86 | 
   87 |     /**
   88 |      * Service path, default value is empty string
   89 |      */
   90 |     String path() default "";
   91 | 
   92 |     /**
   93 |      * Whether to export service, default value is true
   94 |      */
   95 |     boolean export() default true;
   96 | 
   97 |     /**
   98 |      * Service token, default value is empty string
   99 |      */
  100 |     String token() default "";
  101 | 
  102 |     /**
  103 |      * Whether the service is deprecated, default value is false
  104 |      */
  105 |     boolean deprecated() default false;
  106 | 
  107 |     /**
  108 |      * Whether the service is dynamic, default value is true
  109 |      */
  110 |     boolean dynamic() default true;
  111 | 
  112 |     /**
  113 |      * Access log for the service, default value is empty string
  114 |      */
  115 |     String accesslog() default "";
  116 | 
  117 |     /**
  118 |      * Maximum concurrent executes for the service, default value is -1 - no limits
  119 |      */
  120 |     int executes() default -1;
  121 | 
  122 |     /**
  123 |      * Whether to register the service to register center, default value is true
  124 |      */
  125 |     boolean register() default true;
  126 | 
  127 |     /**
  128 |      * Service weight value, default value is -1
  129 |      */
  130 |     int weight() default -1;
  131 | 
  132 |     /**
  133 |      * Service doc, default value is empty string
  134 |      */
  135 |     String document() default "";
  136 | 
  137 |     /**
  138 |      * Delay time for service registration, default value is -1
  139 |      */
  140 |     int delay() default -1;
  141 | 
  142 |     /**
  143 |      * @see DubboService#stub()
  144 |      * @deprecated
  145 |      */
  146 |     String local() default "";
  147 | 
  148 |     /**
  149 |      * Service stub name, use interface name + Local if not set
  150 |      */
  151 |     String stub() default "";
  152 | 
  153 |     /**
  154 |      * Cluster strategy, legal values include: failover, failfast, failsafe, failback, forking
  155 |      * you can use {@link org.apache.dubbo.common.constants.ClusterRules#FAIL_FAST} ……
  156 |      */
  157 |     String cluster() default ClusterRules.EMPTY;
  158 | 
  159 |     /**
  160 |      * How the proxy is generated, legal values include: jdk, javassist
  161 |      */
  162 |     String proxy() default "";
  163 | 
  164 |     /**
  165 |      * Maximum connections service provider can accept, default value is -1 - connection is shared
  166 |      */
  167 |     int connections() default -1;
  168 | 
  169 |     /**
  170 |      * The callback instance limit peer connection
  171 |      * <p>
  172 |      * see org.apache.dubbo.common.constants.CommonConstants.DEFAULT_CALLBACK_INSTANCES
  173 |      */
  174 |     int callbacks() default -1;
  175 | 
  176 |     /**
  177 |      * Callback method name when connected, default value is empty string
  178 |      */
  179 |     String onconnect() default "";
  180 | 
  181 |     /**
  182 |      * Callback method name when disconnected, default value is empty string
  183 |      */
  184 |     String ondisconnect() default "";
  185 | 
  186 |     /**
  187 |      * Service owner, default value is empty string
  188 |      */
  189 |     String owner() default "";
  190 | 
  191 |     /**
  192 |      * Service layer, default value is empty string
  193 |      */
  194 |     String layer() default "";
  195 | 
  196 |     /**
  197 |      * Service invocation retry times
  198 |      *
  199 |      * @see org.apache.dubbo.common.constants.CommonConstants#DEFAULT_RETRIES
  200 |      */
  201 |     int retries() default -1;
  202 | 
  203 |     /**
  204 |      * Load balance strategy, legal values include: random, roundrobin, leastactive
  205 |      *
  206 |      * you can use {@link org.apache.dubbo.common.constants.LoadbalanceRules#RANDOM} ……
  207 |      */
  208 |     String loadbalance() default LoadbalanceRules.EMPTY;
  209 | 
  210 |     /**
  211 |      * Whether to enable async invocation, default value is false
  212 |      */
  213 |     boolean async() default false;
  214 | 
  215 |     /**
  216 |      * Maximum active requests allowed, default value is -1
  217 |      */
  218 |     int actives() default -1;
  219 | 
  220 |     /**
  221 |      * Whether the async request has already been sent, the default value is false
  222 |      */
  223 |     boolean sent() default false;
  224 | 
  225 |     /**
  226 |      * Service mock name, use interface name + Mock if not set
  227 |      */
  228 |     String mock() default "";
  229 | 
  230 |     /**
  231 |      * Whether to use JSR303 validation, legal values are: true, false
  232 |      */
  233 |     String validation() default "";
  234 | 
  235 |     /**
  236 |      * Timeout value for service invocation, default value is -1
  237 |      */
  238 |     int timeout() default -1;
  239 | 
  240 |     /**
  241 |      * Specify cache implementation for service invocation, legal values include: lru, threadlocal, jcache
  242 |      */
  243 |     String cache() default "";
  244 | 
  245 |     /**
  246 |      * Filters for service invocation
  247 |      *
  248 |      * @see Filter
  249 |      */
  250 |     String[] filter() default {};
  251 | 
  252 |     /**
  253 |      * Listeners for service exporting and unexporting
  254 |      *
  255 |      * @see ExporterListener
  256 |      */
  257 |     String[] listener() default {};
  258 | 
  259 |     /**
  260 |      * Customized parameter key-value pair, for example:
  261 |      * <pre>
  262 |      *  ["a","b"] ==> {a=b}
  263 |      *  [" a "," b "] ==> {a=b}
  264 |      *  ["a=b"] ==>{a=b}
  265 |      *  ["a:b"] ==>{a=b}
  266 |      *  ["a=b","c","d"] ==>{a=b,c=d}
  267 |      *  ["a","a:b"] ==>{a="a:b"}
  268 |      *  ["a","a,b"] ==>{a="a,b"}
  269 |      * </pre>
  270 |      * @see org.apache.dubbo.config.spring.util.DubboAnnotationUtils#convertParameters(java.lang.String[])
  271 |      */
  272 |     String[] parameters() default {};
  273 | 
  274 |     /**
  275 |      * Application spring bean name
  276 |      * @deprecated This attribute was deprecated, use bind application/module of spring ApplicationContext
  277 |      */
  278 |     @Deprecated
  279 |     String application() default "";
  280 | 
  281 |     /**
  282 |      * Module spring bean name
  283 |      */
  284 |     String module() default "";
  285 | 
  286 |     /**
  287 |      * Provider spring bean name
  288 |      */
  289 |     String provider() default "";
  290 | 
  291 |     /**
  292 |      * Protocol spring bean names
  293 |      */
  294 |     String[] protocol() default {};
  295 | 
  296 |     /**
  297 |      * Monitor spring bean name
  298 |      */
  299 |     String monitor() default "";
  300 | 
  301 |     /**
  302 |      * Registry spring bean name
  303 |      */
  304 |     String[] registry() default {};
  305 | 
  306 |     /**
  307 |      * Service tag name
  308 |      */
  309 |     String tag() default "";
  310 | 
  311 |     /**
  312 |      * methods support
  313 |      *
  314 |      * @return
  315 |      */
  316 |     Method[] methods() default {};
  317 | 
  318 |     /**
  319 |      * the scope for referring/exporting a service, if it's local, it means searching in current JVM only.
  320 |      * @see org.apache.dubbo.rpc.Constants#SCOPE_LOCAL
  321 |      * @see org.apache.dubbo.rpc.Constants#SCOPE_REMOTE
  322 |      */
  323 |     String scope() default "";
  324 | 
  325 |     /**
  326 |      * Weather the service is export asynchronously
  327 |      */
  328 |     boolean exportAsync() default false;
  329 | 
  330 |     /**
  331 |      * bean name of service executor(thread pool), used for thread pool isolation between services
  332 |      * @return
  333 |      */
  334 |     String executor() default "";
  335 | 
  336 |     /**
  337 |      * Payload max length.
  338 |      */
  339 |     String payload() default "";
  340 | 
  341 |     /**
  342 |      * The serialization type
  343 |      */
  344 |     String serialization() default "";
  345 | 
  346 |     /**
  347 |      * If the parameter has a value, the consumer will read the parameter first.
  348 |      * If the Dubbo Sdk you are using contains the serialization type, the serialization method specified by the argument is used.
  349 |      * <p>
  350 |      * When this parameter is null or the serialization type specified by this parameter does not exist in the Dubbo SDK, the serialization type specified by serialization is used.
  351 |      * If the Dubbo SDK if still does not exist, the default type of the Dubbo SDK is used.
  352 |      * For Dubbo SDK >= 3.2, <code>preferSerialization</code> takes precedence over <code>serialization</code>
  353 |      * <p>
  354 |      * The configuration supports multiple, which are separated by commas.Such as:<code>fastjson2,fastjson,hessian2</code>
  355 |      */
  356 |     String preferSerialization() default "";
  357 | }
```
