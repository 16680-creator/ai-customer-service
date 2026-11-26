<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/cache/Cache.java -->
<!-- 正文引用：[[源码:Cache.java#L起-L止]] ；代码块头部：(src: Cache.java#L起-L止) -->

# Cache.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/cache/Cache.java` |
| 行数 | 101 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:09:18.244Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2023 the original author or authors.
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
   18 | import java.util.concurrent.locks.ReadWriteLock;
   19 | 
   20 | /**
   21 |  * SPI for cache providers.
   22 |  * <p>
   23 |  * One instance of cache will be created for each namespace.
   24 |  * <p>
   25 |  * The cache implementation must have a constructor that receives the cache id as an String parameter.
   26 |  * <p>
   27 |  * MyBatis will pass the namespace as id to the constructor.
   28 |  *
   29 |  * <pre>
   30 |  * public MyCache(final String id) {
   31 |  *   if (id == null) {
   32 |  *     throw new IllegalArgumentException("Cache instances require an ID");
   33 |  *   }
   34 |  *   this.id = id;
   35 |  *   initialize();
   36 |  * }
   37 |  * </pre>
   38 |  *
   39 |  * @author Clinton Begin
   40 |  */
   41 | 
   42 | public interface Cache {
   43 | 
   44 |   /**
   45 |    * @return The identifier of this cache
   46 |    */
   47 |   String getId();
   48 | 
   49 |   /**
   50 |    * @param key
   51 |    *          Can be any object but usually it is a {@link CacheKey}
   52 |    * @param value
   53 |    *          The result of a select.
   54 |    */
   55 |   void putObject(Object key, Object value);
   56 | 
   57 |   /**
   58 |    * @param key
   59 |    *          The key
   60 |    *
   61 |    * @return The object stored in the cache.
   62 |    */
   63 |   Object getObject(Object key);
   64 | 
   65 |   /**
   66 |    * As of 3.3.0 this method is only called during a rollback for any previous value that was missing in the cache. This
   67 |    * lets any blocking cache to release the lock that may have previously put on the key. A blocking cache puts a lock
   68 |    * when a value is null and releases it when the value is back again. This way other threads will wait for the value
   69 |    * to be available instead of hitting the database.
   70 |    *
   71 |    * @param key
   72 |    *          The key
   73 |    *
   74 |    * @return Not used
   75 |    */
   76 |   Object removeObject(Object key);
   77 | 
   78 |   /**
   79 |    * Clears this cache instance.
   80 |    */
   81 |   void clear();
   82 | 
   83 |   /**
   84 |    * Optional. This method is not called by the core.
   85 |    *
   86 |    * @return The number of elements stored in the cache (not its capacity).
   87 |    */
   88 |   int getSize();
   89 | 
   90 |   /**
   91 |    * Optional. As of 3.2.6 this method is no longer called by the core.
   92 |    * <p>
   93 |    * Any locking needed by the cache must be provided internally by the cache provider.
   94 |    *
   95 |    * @return A ReadWriteLock
   96 |    */
   97 |   default ReadWriteLock getReadWriteLock() {
   98 |     return null;
   99 |   }
  100 | 
  101 | }
```
