<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/cache/decorators/TransactionalCache.java -->
<!-- 正文引用：[[源码:TransactionalCache.java#L起-L止]] ；代码块头部：(src: TransactionalCache.java#L起-L止) -->

# TransactionalCache.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/cache/decorators/TransactionalCache.java` |
| 行数 | 135 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:24.230Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2024 the original author or authors.
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
   16 | package org.apache.ibatis.cache.decorators;
   17 | 
   18 | import java.util.HashMap;
   19 | import java.util.HashSet;
   20 | import java.util.Map;
   21 | import java.util.Set;
   22 | 
   23 | import org.apache.ibatis.cache.Cache;
   24 | import org.apache.ibatis.logging.Log;
   25 | import org.apache.ibatis.logging.LogFactory;
   26 | 
   27 | /**
   28 |  * The 2nd level cache transactional buffer.
   29 |  * <p>
   30 |  * This class holds all cache entries that are to be added to the 2nd level cache during a Session. Entries are sent to
   31 |  * the cache when commit is called or discarded if the Session is rolled back. Blocking cache support has been added.
   32 |  * Therefore any get() that returns a cache miss will be followed by a put() so any lock associated with the key can be
   33 |  * released.
   34 |  *
   35 |  * @author Clinton Begin
   36 |  * @author Eduardo Macarron
   37 |  */
   38 | public class TransactionalCache implements Cache {
   39 | 
   40 |   private static final Log log = LogFactory.getLog(TransactionalCache.class);
   41 | 
   42 |   private final Cache delegate;
   43 |   private boolean clearOnCommit;
   44 |   private final Map<Object, Object> entriesToAddOnCommit;
   45 |   private final Set<Object> entriesMissedInCache;
   46 | 
   47 |   public TransactionalCache(Cache delegate) {
   48 |     this.delegate = delegate;
   49 |     this.clearOnCommit = false;
   50 |     this.entriesToAddOnCommit = new HashMap<>();
   51 |     this.entriesMissedInCache = new HashSet<>();
   52 |   }
   53 | 
   54 |   @Override
   55 |   public String getId() {
   56 |     return delegate.getId();
   57 |   }
   58 | 
   59 |   @Override
   60 |   public int getSize() {
   61 |     return delegate.getSize();
   62 |   }
   63 | 
   64 |   @Override
   65 |   public Object getObject(Object key) {
   66 |     // issue #116
   67 |     Object object = delegate.getObject(key);
   68 |     if (object == null) {
   69 |       entriesMissedInCache.add(key);
   70 |     }
   71 |     // issue #146
   72 |     if (clearOnCommit) {
   73 |       return null;
   74 |     }
   75 |     return object;
   76 |   }
   77 | 
   78 |   @Override
   79 |   public void putObject(Object key, Object object) {
   80 |     entriesToAddOnCommit.put(key, object);
   81 |   }
   82 | 
   83 |   @Override
   84 |   public Object removeObject(Object key) {
   85 |     return null;
   86 |   }
   87 | 
   88 |   @Override
   89 |   public void clear() {
   90 |     clearOnCommit = true;
   91 |     entriesToAddOnCommit.clear();
   92 |   }
   93 | 
   94 |   public void commit() {
   95 |     if (clearOnCommit) {
   96 |       delegate.clear();
   97 |     }
   98 |     flushPendingEntries();
   99 |     reset();
  100 |   }
  101 | 
  102 |   public void rollback() {
  103 |     unlockMissedEntries();
  104 |     reset();
  105 |   }
  106 | 
  107 |   private void reset() {
  108 |     clearOnCommit = false;
  109 |     entriesToAddOnCommit.clear();
  110 |     entriesMissedInCache.clear();
  111 |   }
  112 | 
  113 |   private void flushPendingEntries() {
  114 |     for (Map.Entry<Object, Object> entry : entriesToAddOnCommit.entrySet()) {
  115 |       delegate.putObject(entry.getKey(), entry.getValue());
  116 |     }
  117 |     for (Object entry : entriesMissedInCache) {
  118 |       if (!entriesToAddOnCommit.containsKey(entry)) {
  119 |         delegate.putObject(entry, null);
  120 |       }
  121 |     }
  122 |   }
  123 | 
  124 |   private void unlockMissedEntries() {
  125 |     for (Object entry : entriesMissedInCache) {
  126 |       try {
  127 |         delegate.removeObject(entry);
  128 |       } catch (Exception e) {
  129 |         log.warn("Unexpected exception while notifying a rollback to the cache adapter. "
  130 |             + "Consider upgrading your cache adapter to the latest version. Cause: " + e);
  131 |       }
  132 |     }
  133 |   }
  134 | 
  135 | }
```
