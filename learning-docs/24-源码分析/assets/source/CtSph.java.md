<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: com.alibaba.csp:sentinel-core:1.8.10 | path: com/alibaba/csp/sentinel/CtSph.java -->
<!-- 正文引用：[[源码:CtSph.java#L起-L止]] ；代码块头部：(src: CtSph.java#L起-L止) -->

# CtSph.java

| 项 | 值 |
| --- | --- |
| 来源 | `com.alibaba.csp:sentinel-core:1.8.10` |
| 路径 | `com/alibaba/csp/sentinel/CtSph.java` |
| 行数 | 356 |
| 在线地址 | https://repo1.maven.org/maven2/com/alibaba/csp/sentinel-core/1.8.10/sentinel-core-1.8.10-sources.jar |
| 抓取时间 | 2026-11-25T15:33:29.497Z |

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
   19 | import java.util.HashMap;
   20 | import java.util.Map;
   21 | 
   22 | import com.alibaba.csp.sentinel.log.RecordLog;
   23 | import com.alibaba.csp.sentinel.context.Context;
   24 | import com.alibaba.csp.sentinel.context.ContextUtil;
   25 | import com.alibaba.csp.sentinel.context.NullContext;
   26 | import com.alibaba.csp.sentinel.slotchain.MethodResourceWrapper;
   27 | import com.alibaba.csp.sentinel.slotchain.ProcessorSlot;
   28 | import com.alibaba.csp.sentinel.slotchain.ProcessorSlotChain;
   29 | import com.alibaba.csp.sentinel.slotchain.ResourceWrapper;
   30 | import com.alibaba.csp.sentinel.slotchain.SlotChainProvider;
   31 | import com.alibaba.csp.sentinel.slotchain.StringResourceWrapper;
   32 | import com.alibaba.csp.sentinel.slots.block.BlockException;
   33 | import com.alibaba.csp.sentinel.slots.block.Rule;
   34 | 
   35 | /**
   36 |  * {@inheritDoc}
   37 |  *
   38 |  * @author jialiang.linjl
   39 |  * @author leyou(lihao)
   40 |  * @author Eric Zhao
   41 |  * @see Sph
   42 |  */
   43 | public class CtSph implements Sph {
   44 | 
   45 |     private static final Object[] OBJECTS0 = new Object[0];
   46 | 
   47 |     /**
   48 |      * Same resource({@link ResourceWrapper#equals(Object)}) will share the same
   49 |      * {@link ProcessorSlotChain}, no matter in which {@link Context}.
   50 |      */
   51 |     private static volatile Map<ResourceWrapper, ProcessorSlotChain> chainMap
   52 |         = new HashMap<ResourceWrapper, ProcessorSlotChain>();
   53 | 
   54 |     private static final Object LOCK = new Object();
   55 | 
   56 |     private AsyncEntry asyncEntryWithNoChain(ResourceWrapper resourceWrapper, Context context) {
   57 |         AsyncEntry entry = new AsyncEntry(resourceWrapper, null, context);
   58 |         entry.initAsyncContext();
   59 |         // The async entry will be removed from current context as soon as it has been created.
   60 |         entry.cleanCurrentEntryInLocal();
   61 |         return entry;
   62 |     }
   63 | 
   64 |     private AsyncEntry asyncEntryWithPriorityInternal(ResourceWrapper resourceWrapper, int count, boolean prioritized,
   65 |                                                       Object... args) throws BlockException {
   66 |         Context context = ContextUtil.getContext();
   67 |         if (context instanceof NullContext) {
   68 |             // The {@link NullContext} indicates that the amount of context has exceeded the threshold,
   69 |             // so here init the entry only. No rule checking will be done.
   70 |             return asyncEntryWithNoChain(resourceWrapper, context);
   71 |         }
   72 |         if (context == null) {
   73 |             // Using default context.
   74 |             context = InternalContextUtil.internalEnter(Constants.CONTEXT_DEFAULT_NAME);
   75 |         }
   76 | 
   77 |         // Global switch is turned off, so no rule checking will be done.
   78 |         if (!Constants.ON) {
   79 |             return asyncEntryWithNoChain(resourceWrapper, context);
   80 |         }
   81 | 
   82 |         ProcessorSlot<Object> chain = lookProcessChain(resourceWrapper);
   83 | 
   84 |         // Means processor cache size exceeds {@link Constants.MAX_SLOT_CHAIN_SIZE}, so no rule checking will be done.
   85 |         if (chain == null) {
   86 |             return asyncEntryWithNoChain(resourceWrapper, context);
   87 |         }
   88 | 
   89 |         AsyncEntry asyncEntry = new AsyncEntry(resourceWrapper, chain, context, count, args);
   90 |         try {
   91 |             chain.entry(context, resourceWrapper, null, count, prioritized, args);
   92 |             // Initiate the async context only when the entry successfully passed the slot chain.
   93 |             asyncEntry.initAsyncContext();
   94 |             // The asynchronous call may take time in background, and current context should not be hanged on it.
   95 |             // So we need to remove current async entry from current context.
   96 |             asyncEntry.cleanCurrentEntryInLocal();
   97 |         } catch (BlockException e1) {
   98 |             // When blocked, the async entry will be exited on current context.
   99 |             // The async context will not be initialized.
  100 |             asyncEntry.exitForContext(context, count, args);
  101 |             throw e1;
  102 |         } catch (Throwable e1) {
  103 |             // This should not happen, unless there are errors existing in Sentinel internal.
  104 |             // When this happens, async context is not initialized.
  105 |             RecordLog.warn("Sentinel unexpected exception in asyncEntryInternal", e1);
  106 | 
  107 |             asyncEntry.cleanCurrentEntryInLocal();
  108 |         }
  109 |         return asyncEntry;
  110 |     }
  111 | 
  112 |     private AsyncEntry asyncEntryInternal(ResourceWrapper resourceWrapper, int count, Object... args)
  113 |         throws BlockException {
  114 |         return asyncEntryWithPriorityInternal(resourceWrapper, count, false, args);
  115 |     }
  116 | 
  117 |     private Entry entryWithPriority(ResourceWrapper resourceWrapper, int count, boolean prioritized, Object... args)
  118 |         throws BlockException {
  119 |         Context context = ContextUtil.getContext();
  120 |         if (context instanceof NullContext) {
  121 |             // The {@link NullContext} indicates that the amount of context has exceeded the threshold,
  122 |             // so here init the entry only. No rule checking will be done.
  123 |             return new CtEntry(resourceWrapper, null, context);
  124 |         }
  125 | 
  126 |         if (context == null) {
  127 |             // Using default context.
  128 |             context = InternalContextUtil.internalEnter(Constants.CONTEXT_DEFAULT_NAME);
  129 |         }
  130 | 
  131 |         // Global switch is close, no rule checking will do.
  132 |         if (!Constants.ON) {
  133 |             return new CtEntry(resourceWrapper, null, context);
  134 |         }
  135 | 
  136 |         ProcessorSlot<Object> chain = lookProcessChain(resourceWrapper);
  137 | 
  138 |         /*
  139 |          * Means amount of resources (slot chain) exceeds {@link Constants.MAX_SLOT_CHAIN_SIZE},
  140 |          * so no rule checking will be done.
  141 |          */
  142 |         if (chain == null) {
  143 |             return new CtEntry(resourceWrapper, null, context);
  144 |         }
  145 | 
  146 |         Entry e = new CtEntry(resourceWrapper, chain, context, count, args);
  147 |         try {
  148 |             chain.entry(context, resourceWrapper, null, count, prioritized, args);
  149 |         } catch (BlockException e1) {
  150 |             e.exit(count, args);
  151 |             throw e1;
  152 |         } catch (Throwable e1) {
  153 |             // This should not happen, unless there are errors existing in Sentinel internal.
  154 |             RecordLog.info("Sentinel unexpected exception", e1);
  155 |         }
  156 |         return e;
  157 |     }
  158 | 
  159 |     /**
  160 |      * Do all {@link Rule}s checking about the resource.
  161 |      *
  162 |      * <p>Each distinct resource will use a {@link ProcessorSlot} to do rules checking. Same resource will use
  163 |      * same {@link ProcessorSlot} globally. </p>
  164 |      *
  165 |      * <p>Note that total {@link ProcessorSlot} count must not exceed {@link Constants#MAX_SLOT_CHAIN_SIZE},
  166 |      * otherwise no rules checking will do. In this condition, all requests will pass directly, with no checking
  167 |      * or exception.</p>
  168 |      *
  169 |      * @param resourceWrapper resource name
  170 |      * @param count           tokens needed
  171 |      * @param args            arguments of user method call
  172 |      * @return {@link Entry} represents this call
  173 |      * @throws BlockException if any rule's threshold is exceeded
  174 |      */
  175 |     public Entry entry(ResourceWrapper resourceWrapper, int count, Object... args) throws BlockException {
  176 |         return entryWithPriority(resourceWrapper, count, false, args);
  177 |     }
  178 | 
  179 |     /**
  180 |      * Get {@link ProcessorSlotChain} of the resource. new {@link ProcessorSlotChain} will
  181 |      * be created if the resource doesn't relate one.
  182 |      *
  183 |      * <p>Same resource({@link ResourceWrapper#equals(Object)}) will share the same
  184 |      * {@link ProcessorSlotChain} globally, no matter in which {@link Context}.<p/>
  185 |      *
  186 |      * <p>
  187 |      * Note that total {@link ProcessorSlot} count must not exceed {@link Constants#MAX_SLOT_CHAIN_SIZE},
  188 |      * otherwise null will return.
  189 |      * </p>
  190 |      *
  191 |      * @param resourceWrapper target resource
  192 |      * @return {@link ProcessorSlotChain} of the resource
  193 |      */
  194 |     ProcessorSlot<Object> lookProcessChain(ResourceWrapper resourceWrapper) {
  195 |         ProcessorSlotChain chain = chainMap.get(resourceWrapper);
  196 |         if (chain == null) {
  197 |             synchronized (LOCK) {
  198 |                 chain = chainMap.get(resourceWrapper);
  199 |                 if (chain == null) {
  200 |                     // Entry size limit.
  201 |                     if (chainMap.size() >= Constants.MAX_SLOT_CHAIN_SIZE) {
  202 |                         return null;
  203 |                     }
  204 | 
  205 |                     chain = SlotChainProvider.newSlotChain();
  206 |                     Map<ResourceWrapper, ProcessorSlotChain> newMap = new HashMap<ResourceWrapper, ProcessorSlotChain>(
  207 |                         chainMap.size() + 1);
  208 |                     newMap.putAll(chainMap);
  209 |                     newMap.put(resourceWrapper, chain);
  210 |                     chainMap = newMap;
  211 |                 }
  212 |             }
  213 |         }
  214 |         return chain;
  215 |     }
  216 | 
  217 |     /**
  218 |      * Get current size of created slot chains.
  219 |      *
  220 |      * @return size of created slot chains
  221 |      * @since 0.2.0
  222 |      */
  223 |     public static int entrySize() {
  224 |         return chainMap.size();
  225 |     }
  226 | 
  227 |     /**
  228 |      * Reset the slot chain map. Only for internal test.
  229 |      *
  230 |      * @since 0.2.0
  231 |      */
  232 |     static void resetChainMap() {
  233 |         chainMap.clear();
  234 |     }
  235 | 
  236 |     /**
  237 |      * Only for internal test.
  238 |      *
  239 |      * @since 0.2.0
  240 |      */
  241 |     static Map<ResourceWrapper, ProcessorSlotChain> getChainMap() {
  242 |         return chainMap;
  243 |     }
  244 | 
  245 |     /**
  246 |      * This class is used for skip context name checking.
  247 |      */
  248 |     private final static class InternalContextUtil extends ContextUtil {
  249 |         static Context internalEnter(String name) {
  250 |             return trueEnter(name, "");
  251 |         }
  252 | 
  253 |         static Context internalEnter(String name, String origin) {
  254 |             return trueEnter(name, origin);
  255 |         }
  256 |     }
  257 | 
  258 |     @Override
  259 |     public Entry entry(String name) throws BlockException {
  260 |         StringResourceWrapper resource = new StringResourceWrapper(name, EntryType.OUT);
  261 |         return entry(resource, 1, OBJECTS0);
  262 |     }
  263 | 
  264 |     @Override
  265 |     public Entry entry(Method method) throws BlockException {
  266 |         MethodResourceWrapper resource = new MethodResourceWrapper(method, EntryType.OUT);
  267 |         return entry(resource, 1, OBJECTS0);
  268 |     }
  269 | 
  270 |     @Override
  271 |     public Entry entry(Method method, EntryType type) throws BlockException {
  272 |         MethodResourceWrapper resource = new MethodResourceWrapper(method, type);
  273 |         return entry(resource, 1, OBJECTS0);
  274 |     }
  275 | 
  276 |     @Override
  277 |     public Entry entry(String name, EntryType type) throws BlockException {
  278 |         StringResourceWrapper resource = new StringResourceWrapper(name, type);
  279 |         return entry(resource, 1, OBJECTS0);
  280 |     }
  281 | 
  282 |     @Override
  283 |     public Entry entry(Method method, EntryType type, int count) throws BlockException {
  284 |         MethodResourceWrapper resource = new MethodResourceWrapper(method, type);
  285 |         return entry(resource, count, OBJECTS0);
  286 |     }
  287 | 
  288 |     @Override
  289 |     public Entry entry(String name, EntryType type, int count) throws BlockException {
  290 |         StringResourceWrapper resource = new StringResourceWrapper(name, type);
  291 |         return entry(resource, count, OBJECTS0);
  292 |     }
  293 | 
  294 |     @Override
  295 |     public Entry entry(Method method, int count) throws BlockException {
  296 |         MethodResourceWrapper resource = new MethodResourceWrapper(method, EntryType.OUT);
  297 |         return entry(resource, count, OBJECTS0);
  298 |     }
  299 | 
  300 |     @Override
  301 |     public Entry entry(String name, int count) throws BlockException {
  302 |         StringResourceWrapper resource = new StringResourceWrapper(name, EntryType.OUT);
  303 |         return entry(resource, count, OBJECTS0);
  304 |     }
  305 | 
  306 |     @Override
  307 |     public Entry entry(Method method, EntryType type, int count, Object... args) throws BlockException {
  308 |         MethodResourceWrapper resource = new MethodResourceWrapper(method, type);
  309 |         return entry(resource, count, args);
  310 |     }
  311 | 
  312 |     @Override
  313 |     public Entry entry(String name, EntryType type, int count, Object... args) throws BlockException {
  314 |         StringResourceWrapper resource = new StringResourceWrapper(name, type);
  315 |         return entry(resource, count, args);
  316 |     }
  317 | 
  318 |     @Override
  319 |     public AsyncEntry asyncEntry(String name, EntryType type, int count, Object... args) throws BlockException {
  320 |         StringResourceWrapper resource = new StringResourceWrapper(name, type);
  321 |         return asyncEntryInternal(resource, count, args);
  322 |     }
  323 | 
  324 |     @Override
  325 |     public Entry entryWithPriority(String name, EntryType type, int count, boolean prioritized) throws BlockException {
  326 |         StringResourceWrapper resource = new StringResourceWrapper(name, type);
  327 |         return entryWithPriority(resource, count, prioritized);
  328 |     }
  329 | 
  330 |     @Override
  331 |     public Entry entryWithPriority(String name, EntryType type, int count, boolean prioritized, Object... args)
  332 |         throws BlockException {
  333 |         StringResourceWrapper resource = new StringResourceWrapper(name, type);
  334 |         return entryWithPriority(resource, count, prioritized, args);
  335 |     }
  336 | 
  337 |     @Override
  338 |     public Entry entryWithType(String name, int resourceType, EntryType entryType, int count, Object[] args)
  339 |         throws BlockException {
  340 |         return entryWithType(name, resourceType, entryType, count, false, args);
  341 |     }
  342 | 
  343 |     @Override
  344 |     public Entry entryWithType(String name, int resourceType, EntryType entryType, int count, boolean prioritized,
  345 |                                Object[] args) throws BlockException {
  346 |         StringResourceWrapper resource = new StringResourceWrapper(name, entryType, resourceType);
  347 |         return entryWithPriority(resource, count, prioritized, args);
  348 |     }
  349 | 
  350 |     @Override
  351 |     public AsyncEntry asyncEntryWithType(String name, int resourceType, EntryType entryType, int count,
  352 |                                          boolean prioritized, Object[] args) throws BlockException {
  353 |         StringResourceWrapper resource = new StringResourceWrapper(name, entryType, resourceType);
  354 |         return asyncEntryWithPriorityInternal(resource, count, prioritized, args);
  355 |     }
  356 | }
```
