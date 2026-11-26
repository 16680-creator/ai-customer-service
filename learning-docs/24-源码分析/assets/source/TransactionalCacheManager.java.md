<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/cache/TransactionalCacheManager.java -->
<!-- 正文引用：[[源码:TransactionalCacheManager.java#L起-L止]] ；代码块头部：(src: TransactionalCacheManager.java#L起-L止) -->

# TransactionalCacheManager.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/cache/TransactionalCacheManager.java` |
| 行数 | 58 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:00:16.156Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2025 the original author or authors.
    3 |  *
    4 |  *    Licensed under the Apache License, Version 2.0 (the "License");
    5 |  *    you may not use this file except in compliance with the License.
    6 |  *    You may obtain a copy of the License at
    7 |  *
    8 |  *       https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  *    Unless required by applicable law or agreed to in writing, software
   11 |  *    distributed under the License is distributed on an "AS IS" BASIS,
   12 |  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  *    See the License for the specific language governing permissions and
   14 |  *    limitations under the License.
   15 |  */
   16 | package org.apache.ibatis.cache;
   17 | 
   18 | import java.util.HashMap;
   19 | import java.util.Map;
   20 | 
   21 | import org.apache.ibatis.cache.decorators.TransactionalCache;
   22 | 
   23 | /**
   24 |  * @author Clinton Begin
   25 |  */
   26 | public class TransactionalCacheManager {
   27 | 
   28 |   private final Map<Cache, TransactionalCache> transactionalCaches = new HashMap<>();
   29 | 
   30 |   public void clear(Cache cache) {
   31 |     getTransactionalCache(cache).clear();
   32 |   }
   33 | 
   34 |   public Object getObject(Cache cache, CacheKey key) {
   35 |     return getTransactionalCache(cache).getObject(key);
   36 |   }
   37 | 
   38 |   public void putObject(Cache cache, CacheKey key, Object value) {
   39 |     getTransactionalCache(cache).putObject(key, value);
   40 |   }
   41 | 
   42 |   public void commit() {
   43 |     for (TransactionalCache txCache : transactionalCaches.values()) {
   44 |       txCache.commit();
   45 |     }
   46 |   }
   47 | 
   48 |   public void rollback() {
   49 |     for (TransactionalCache txCache : transactionalCaches.values()) {
   50 |       txCache.rollback();
   51 |     }
   52 |   }
   53 | 
   54 |   private TransactionalCache getTransactionalCache(Cache cache) {
   55 |     return transactionalCaches.computeIfAbsent(cache, TransactionalCache::new);
   56 |   }
   57 | 
   58 | }
```
