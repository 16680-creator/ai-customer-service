<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.csp:sentinel-core:1.8.10 | path: com/alibaba/csp/sentinel/SphU.java -->
<!-- 正文引用：[[源码:SphU.java#L起-L止]] ；代码块头部：(src: SphU.java#L起-L止) -->

# SphU.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.csp:sentinel-core:1.8.10` |
| 路径 | `com/alibaba/csp/sentinel/SphU.java` |
| 行数 | 368 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/csp/sentinel-core/1.8.10/sentinel-core-1.8.10-sources.jar |
| 抓取时间 | 2026-11-25T15:33:29.397Z |

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
   16 | package com.alibaba.csp.sentinel;
   17 | 
   18 | import java.lang.reflect.Method;
   19 | 
   20 | import com.alibaba.csp.sentinel.slots.block.BlockException;
   21 | import com.alibaba.csp.sentinel.slots.block.Rule;
   22 | import com.alibaba.csp.sentinel.slots.system.SystemRule;
   23 | 
   24 | /**
   25 |  * <p>The fundamental Sentinel API for recording statistics and performing rule checking for resources.</p>
   26 |  * <p>
   27 |  * Conceptually, physical or logical resource that need protection should be
   28 |  * surrounded by an entry. The requests to this resource will be blocked if any
   29 |  * criteria is met, eg. when any {@link Rule}'s threshold is exceeded. Once blocked,
   30 |  * a {@link BlockException} will be thrown.
   31 |  * </p>
   32 |  * <p>
   33 |  * To configure the criteria, we can use <code>XxxRuleManager.loadRules()</code> to load rules.
   34 |  * </p>
   35 |  *
   36 |  * <p>
   37 |  * Following code is an example, {@code "abc"} represent a unique name for the
   38 |  * protected resource:
   39 |  * </p>
   40 |  *
   41 |  * <pre>
   42 |  *  public void foo() {
   43 |  *     Entry entry = null;
   44 |  *     try {
   45 |  *        entry = SphU.entry("abc");
   46 |  *        // resource that need protection
   47 |  *     } catch (BlockException blockException) {
   48 |  *         // when goes there, it is blocked
   49 |  *         // add blocked handle logic here
   50 |  *     } catch (Throwable bizException) {
   51 |  *         // business exception
   52 |  *         Tracer.trace(bizException);
   53 |  *     } finally {
   54 |  *         // ensure finally be executed
   55 |  *         if (entry != null){
   56 |  *             entry.exit();
   57 |  *         }
   58 |  *     }
   59 |  *  }
   60 |  * </pre>
   61 |  *
   62 |  * <p>
   63 |  * Make sure {@code SphU.entry()} and {@link Entry#exit()} be paired in the same thread,
   64 |  * otherwise {@link ErrorEntryFreeException} will be thrown.
   65 |  * </p>
   66 |  *
   67 |  * @author jialiang.linjl
   68 |  * @author Eric Zhao
   69 |  * @see SphO
   70 |  */
   71 | public class SphU {
   72 | 
   73 |     private static final Object[] OBJECTS0 = new Object[0];
   74 | 
   75 |     private SphU() {}
   76 | 
   77 |     /**
   78 |      * Record statistics and perform rule checking for the given resource.
   79 |      *
   80 |      * @param name the unique name of the protected resource
   81 |      * @return the {@link Entry} of this invocation (used for mark the invocation complete and get context data)
   82 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
   83 |      */
   84 |     public static Entry entry(String name) throws BlockException {
   85 |         return Env.sph.entry(name, EntryType.OUT, 1, OBJECTS0);
   86 |     }
   87 | 
   88 |     /**
   89 |      * Checking all {@link Rule}s about the protected method.
   90 |      *
   91 |      * @param method the protected method
   92 |      * @return the {@link Entry} of this invocation (used for mark the invocation complete and get context data)
   93 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
   94 |      */
   95 |     public static Entry entry(Method method) throws BlockException {
   96 |         return Env.sph.entry(method, EntryType.OUT, 1, OBJECTS0);
   97 |     }
   98 | 
   99 |     /**
  100 |      * Checking all {@link Rule}s about the protected method.
  101 |      *
  102 |      * @param method     the protected method
  103 |      * @param batchCount the amount of calls within the invocation (e.g. batchCount=2 means request for 2 tokens)
  104 |      * @return the {@link Entry} of this invocation (used for mark the invocation complete and get context data)
  105 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  106 |      */
  107 |     public static Entry entry(Method method, int batchCount) throws BlockException {
  108 |         return Env.sph.entry(method, EntryType.OUT, batchCount, OBJECTS0);
  109 |     }
  110 | 
  111 |     /**
  112 |      * Record statistics and perform rule checking for the given resource.
  113 |      *
  114 |      * @param name       the unique string for the resource
  115 |      * @param batchCount the amount of calls within the invocation (e.g. batchCount=2 means request for 2 tokens)
  116 |      * @return the {@link Entry} of this invocation (used for mark the invocation complete and get context data)
  117 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  118 |      */
  119 |     public static Entry entry(String name, int batchCount) throws BlockException {
  120 |         return Env.sph.entry(name, EntryType.OUT, batchCount, OBJECTS0);
  121 |     }
  122 | 
  123 |     /**
  124 |      * Checking all {@link Rule}s about the protected method.
  125 |      *
  126 |      * @param method      the protected method
  127 |      * @param trafficType the traffic type (inbound, outbound or internal). This is used
  128 |      *                    to mark whether it can be blocked when the system is unstable,
  129 |      *                    only inbound traffic could be blocked by {@link SystemRule}
  130 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  131 |      */
  132 |     public static Entry entry(Method method, EntryType trafficType) throws BlockException {
  133 |         return Env.sph.entry(method, trafficType, 1, OBJECTS0);
  134 |     }
  135 | 
  136 |     /**
  137 |      * Record statistics and perform rule checking for the given resource.
  138 |      *
  139 |      * @param name        the unique name for the protected resource
  140 |      * @param trafficType the traffic type (inbound, outbound or internal). This is used
  141 |      *                    to mark whether it can be blocked when the system is unstable,
  142 |      *                    only inbound traffic could be blocked by {@link SystemRule}
  143 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  144 |      */
  145 |     public static Entry entry(String name, EntryType trafficType) throws BlockException {
  146 |         return Env.sph.entry(name, trafficType, 1, OBJECTS0);
  147 |     }
  148 | 
  149 |     /**
  150 |      * Checking all {@link Rule}s about the protected method.
  151 |      *
  152 |      * @param method      the protected method
  153 |      * @param trafficType the traffic type (inbound, outbound or internal). This is used
  154 |      *                    to mark whether it can be blocked when the system is unstable,
  155 |      *                    only inbound traffic could be blocked by {@link SystemRule}
  156 |      * @param batchCount  the amount of calls within the invocation (e.g. batchCount=2 means request for 2 tokens)
  157 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  158 |      */
  159 |     public static Entry entry(Method method, EntryType trafficType, int batchCount) throws BlockException {
  160 |         return Env.sph.entry(method, trafficType, batchCount, OBJECTS0);
  161 |     }
  162 | 
  163 |     /**
  164 |      * Record statistics and perform rule checking for the given resource.
  165 |      *
  166 |      * @param name        the unique name for the protected resource
  167 |      * @param trafficType the traffic type (inbound, outbound or internal). This is used
  168 |      *                    to mark whether it can be blocked when the system is unstable,
  169 |      *                    only inbound traffic could be blocked by {@link SystemRule}
  170 |      * @param batchCount  the amount of calls within the invocation (e.g. batchCount=2 means request for 2 tokens)
  171 |      * @return the {@link Entry} of this invocation (used for mark the invocation complete and get context data)
  172 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  173 |      */
  174 |     public static Entry entry(String name, EntryType trafficType, int batchCount) throws BlockException {
  175 |         return Env.sph.entry(name, trafficType, batchCount, OBJECTS0);
  176 |     }
  177 | 
  178 |     /**
  179 |      * Checking all {@link Rule}s about the protected method.
  180 |      *
  181 |      * @param method      the protected method
  182 |      * @param trafficType the traffic type (inbound, outbound or internal). This is used
  183 |      *                    to mark whether it can be blocked when the system is unstable,
  184 |      *                    only inbound traffic could be blocked by {@link SystemRule}
  185 |      * @param batchCount  the amount of calls within the invocation (e.g. batchCount=2 means request for 2 tokens)
  186 |      * @param args        args for parameter flow control or customized slots
  187 |      * @return the {@link Entry} of this invocation (used for mark the invocation complete and get context data)
  188 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  189 |      */
  190 |     public static Entry entry(Method method, EntryType trafficType, int batchCount, Object... args)
  191 |         throws BlockException {
  192 |         return Env.sph.entry(method, trafficType, batchCount, args);
  193 |     }
  194 | 
  195 |     /**
  196 |      * Record statistics and perform rule checking for the given resource.
  197 |      *
  198 |      * @param name        the unique name for the protected resource
  199 |      * @param trafficType the traffic type (inbound, outbound or internal). This is used
  200 |      *                    to mark whether it can be blocked when the system is unstable,
  201 |      *                    only inbound traffic could be blocked by {@link SystemRule}
  202 |      * @param batchCount  the amount of calls within the invocation (e.g. batchCount=2 means request for 2 tokens)
  203 |      * @param args        args for parameter flow control
  204 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  205 |      */
  206 |     public static Entry entry(String name, EntryType trafficType, int batchCount, Object... args)
  207 |         throws BlockException {
  208 |         return Env.sph.entry(name, trafficType, batchCount, args);
  209 |     }
  210 | 
  211 |     /**
  212 |      * Record statistics and check all rules of the resource that indicates an async invocation.
  213 |      *
  214 |      * @param name the unique name of the protected resource
  215 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  216 |      * @since 0.2.0
  217 |      */
  218 |     public static AsyncEntry asyncEntry(String name) throws BlockException {
  219 |         return Env.sph.asyncEntry(name, EntryType.OUT, 1, OBJECTS0);
  220 |     }
  221 | 
  222 |     /**
  223 |      * Record statistics and check all rules of the resource that indicates an async invocation.
  224 |      *
  225 |      * @param name        the unique name for the protected resource
  226 |      * @param trafficType the traffic type (inbound, outbound or internal). This is used
  227 |      *                    to mark whether it can be blocked when the system is unstable,
  228 |      *                    only inbound traffic could be blocked by {@link SystemRule}
  229 |      * @return the {@link Entry} of this invocation (used for mark the invocation complete and get context data)
  230 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  231 |      * @since 0.2.0
  232 |      */
  233 |     public static AsyncEntry asyncEntry(String name, EntryType trafficType) throws BlockException {
  234 |         return Env.sph.asyncEntry(name, trafficType, 1, OBJECTS0);
  235 |     }
  236 | 
  237 |     /**
  238 |      * Record statistics and check all rules of the resource that indicates an async invocation.
  239 |      *
  240 |      * @param name        the unique name for the protected resource
  241 |      * @param trafficType the traffic type (inbound, outbound or internal). This is used
  242 |      *                    to mark whether it can be blocked when the system is unstable,
  243 |      *                    only inbound traffic could be blocked by {@link SystemRule}
  244 |      * @param batchCount  the amount of calls within the invocation (e.g. batchCount=2 means request for 2 tokens)
  245 |      * @param args        args for parameter flow control
  246 |      * @return the {@link Entry} of this invocation (used for mark the invocation complete and get context data)
  247 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  248 |      * @since 0.2.0
  249 |      */
  250 |     public static AsyncEntry asyncEntry(String name, EntryType trafficType, int batchCount, Object... args)
  251 |         throws BlockException {
  252 |         return Env.sph.asyncEntry(name, trafficType, batchCount, args);
  253 |     }
  254 | 
  255 |     /**
  256 |      * Record statistics and perform rule checking for the given resource. The entry is prioritized.
  257 |      *
  258 |      * @param name the unique name for the protected resource
  259 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  260 |      * @since 1.4.0
  261 |      */
  262 |     public static Entry entryWithPriority(String name) throws BlockException {
  263 |         return Env.sph.entryWithPriority(name, EntryType.OUT, 1, true);
  264 |     }
  265 | 
  266 |     /**
  267 |      * Record statistics and perform rule checking for the given resource. The entry is prioritized.
  268 |      *
  269 |      * @param name        the unique name for the protected resource
  270 |      * @param trafficType the traffic type (inbound, outbound or internal). This is used
  271 |      *                    to mark whether it can be blocked when the system is unstable,
  272 |      *                    only inbound traffic could be blocked by {@link SystemRule}
  273 |      * @return the {@link Entry} of this invocation (used for mark the invocation complete and get context data)
  274 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  275 |      * @since 1.4.0
  276 |      */
  277 |     public static Entry entryWithPriority(String name, EntryType trafficType) throws BlockException {
  278 |         return Env.sph.entryWithPriority(name, trafficType, 1, true);
  279 |     }
  280 | 
  281 |     /**
  282 |      * Record statistics and perform rule checking for the given resource.
  283 |      *
  284 |      * @param name         the unique name for the protected resource
  285 |      * @param resourceType classification of the resource (e.g. Web or RPC)
  286 |      * @param trafficType  the traffic type (inbound, outbound or internal). This is used
  287 |      *                     to mark whether it can be blocked when the system is unstable,
  288 |      *                     only inbound traffic could be blocked by {@link SystemRule}
  289 |      * @return the {@link Entry} of this invocation (used for mark the invocation complete and get context data)
  290 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  291 |      * @since 1.7.0
  292 |      */
  293 |     public static Entry entry(String name, int resourceType, EntryType trafficType) throws BlockException {
  294 |         return Env.sph.entryWithType(name, resourceType, trafficType, 1, OBJECTS0);
  295 |     }
  296 | 
  297 |     /**
  298 |      * Record statistics and perform rule checking for the given resource.
  299 |      *
  300 |      * @param name         the unique name for the protected resource
  301 |      * @param trafficType  the traffic type (inbound, outbound or internal). This is used
  302 |      *                     to mark whether it can be blocked when the system is unstable,
  303 |      *                     only inbound traffic could be blocked by {@link SystemRule}
  304 |      * @param resourceType classification of the resource (e.g. Web or RPC)
  305 |      * @param args         args for parameter flow control or customized slots
  306 |      * @return the {@link Entry} of this invocation (used for mark the invocation complete and get context data)
  307 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  308 |      * @since 1.7.0
  309 |      */
  310 |     public static Entry entry(String name, int resourceType, EntryType trafficType, Object[] args)
  311 |         throws BlockException {
  312 |         return Env.sph.entryWithType(name, resourceType, trafficType, 1, args);
  313 |     }
  314 | 
  315 |     /**
  316 |      * Record statistics and perform rule checking for the given resource that indicates an async invocation.
  317 |      *
  318 |      * @param name         the unique name for the protected resource
  319 |      * @param trafficType  the traffic type (inbound, outbound or internal). This is used
  320 |      *                     to mark whether it can be blocked when the system is unstable,
  321 |      *                     only inbound traffic could be blocked by {@link SystemRule}
  322 |      * @param resourceType classification of the resource (e.g. Web or RPC)
  323 |      * @return the {@link Entry} of this invocation (used for mark the invocation complete and get context data)
  324 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  325 |      * @since 1.7.0
  326 |      */
  327 |     public static AsyncEntry asyncEntry(String name, int resourceType, EntryType trafficType)
  328 |         throws BlockException {
  329 |         return Env.sph.asyncEntryWithType(name, resourceType, trafficType, 1, false, OBJECTS0);
  330 |     }
  331 | 
  332 |     /**
  333 |      * Record statistics and perform rule checking for the given resource that indicates an async invocation.
  334 |      *
  335 |      * @param name         the unique name for the protected resource
  336 |      * @param trafficType  the traffic type (inbound, outbound or internal). This is used
  337 |      *                     to mark whether it can be blocked when the system is unstable,
  338 |      *                     only inbound traffic could be blocked by {@link SystemRule}
  339 |      * @param resourceType classification of the resource (e.g. Web or RPC)
  340 |      * @param args         args for parameter flow control or customized slots
  341 |      * @return the {@link Entry} of this invocation (used for mark the invocation complete and get context data)
  342 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  343 |      * @since 1.7.0
  344 |      */
  345 |     public static AsyncEntry asyncEntry(String name, int resourceType, EntryType trafficType, Object[] args)
  346 |         throws BlockException {
  347 |         return Env.sph.asyncEntryWithType(name, resourceType, trafficType, 1, false, args);
  348 |     }
  349 | 
  350 |     /**
  351 |      * Record statistics and perform rule checking for the given resource that indicates an async invocation.
  352 |      *
  353 |      * @param name         the unique name for the protected resource
  354 |      * @param trafficType  the traffic type (inbound, outbound or internal). This is used
  355 |      *                     to mark whether it can be blocked when the system is unstable,
  356 |      *                     only inbound traffic could be blocked by {@link SystemRule}
  357 |      * @param resourceType classification of the resource (e.g. Web or RPC)
  358 |      * @param batchCount   the amount of calls within the invocation (e.g. batchCount=2 means request for 2 tokens)
  359 |      * @param args         args for parameter flow control or customized slots
  360 |      * @return the {@link Entry} of this invocation (used for mark the invocation complete and get context data)
  361 |      * @throws BlockException if the block criteria is met (e.g. metric exceeded the threshold of any rules)
  362 |      * @since 1.7.0
  363 |      */
  364 |     public static AsyncEntry asyncEntry(String name, int resourceType, EntryType trafficType, int batchCount,
  365 |                                         Object[] args) throws BlockException {
  366 |         return Env.sph.asyncEntryWithType(name, resourceType, trafficType, batchCount, false, args);
  367 |     }
  368 | }
```
