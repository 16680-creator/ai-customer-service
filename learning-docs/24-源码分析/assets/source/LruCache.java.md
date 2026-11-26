<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/cache/decorators/LruCache.java -->
<!-- 正文引用：[[源码:LruCache.java#L起-L止]] ；代码块头部：(src: LruCache.java#L起-L止) -->

# LruCache.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/cache/decorators/LruCache.java` |
| 行数 | 96 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:09:18.056Z |

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
   16 | package org.apache.ibatis.cache.decorators;
   17 | 
   18 | import java.util.LinkedHashMap;
   19 | import java.util.Map;
   20 | 
   21 | import org.apache.ibatis.cache.Cache;
   22 | 
   23 | /**
   24 |  * Lru (least recently used) cache decorator.
   25 |  *
   26 |  * @author Clinton Begin
   27 |  */
   28 | public class LruCache implements Cache {
   29 | 
   30 |   private final Cache delegate;
   31 |   private Map<Object, Object> keyMap;
   32 |   private Object eldestKey;
   33 | 
   34 |   public LruCache(Cache delegate) {
   35 |     this.delegate = delegate;
   36 |     setSize(1024);
   37 |   }
   38 | 
   39 |   @Override
   40 |   public String getId() {
   41 |     return delegate.getId();
   42 |   }
   43 | 
   44 |   @Override
   45 |   public int getSize() {
   46 |     return delegate.getSize();
   47 |   }
   48 | 
   49 |   public void setSize(final int size) {
   50 |     keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
   51 |       private static final long serialVersionUID = 4267176411845948333L;
   52 | 
   53 |       @Override
   54 |       protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
   55 |         boolean tooBig = size() > size;
   56 |         if (tooBig) {
   57 |           eldestKey = eldest.getKey();
   58 |         }
   59 |         return tooBig;
   60 |       }
   61 |     };
   62 |   }
   63 | 
   64 |   @Override
   65 |   public void putObject(Object key, Object value) {
   66 |     delegate.putObject(key, value);
   67 |     cycleKeyList(key);
   68 |   }
   69 | 
   70 |   @Override
   71 |   public Object getObject(Object key) {
   72 |     keyMap.get(key); // touch
   73 |     return delegate.getObject(key);
   74 |   }
   75 | 
   76 |   @Override
   77 |   public Object removeObject(Object key) {
   78 |     keyMap.remove(key);
   79 |     return delegate.removeObject(key);
   80 |   }
   81 | 
   82 |   @Override
   83 |   public void clear() {
   84 |     delegate.clear();
   85 |     keyMap.clear();
   86 |   }
   87 | 
   88 |   private void cycleKeyList(Object key) {
   89 |     keyMap.put(key, key);
   90 |     if (eldestKey != null) {
   91 |       delegate.removeObject(eldestKey);
   92 |       eldestKey = null;
   93 |     }
   94 |   }
   95 | 
   96 | }
```
