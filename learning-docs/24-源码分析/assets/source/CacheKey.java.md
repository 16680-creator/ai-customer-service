<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/cache/CacheKey.java -->
<!-- 正文引用：[[源码:CacheKey.java#L起-L止]] ；代码块头部：(src: CacheKey.java#L起-L止) -->

# CacheKey.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/cache/CacheKey.java` |
| 行数 | 138 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:22.123Z |

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
   18 | import java.io.Serializable;
   19 | import java.util.ArrayList;
   20 | import java.util.List;
   21 | import java.util.StringJoiner;
   22 | 
   23 | import org.apache.ibatis.reflection.ArrayUtil;
   24 | 
   25 | /**
   26 |  * @author Clinton Begin
   27 |  */
   28 | public class CacheKey implements Cloneable, Serializable {
   29 | 
   30 |   private static final long serialVersionUID = 1146682552656046210L;
   31 | 
   32 |   public static final CacheKey NULL_CACHE_KEY = new CacheKey() {
   33 | 
   34 |     private static final long serialVersionUID = 1L;
   35 | 
   36 |     @Override
   37 |     public void update(Object object) {
   38 |       throw new CacheException("Not allowed to update a null cache key instance.");
   39 |     }
   40 | 
   41 |     @Override
   42 |     public void updateAll(Object[] objects) {
   43 |       throw new CacheException("Not allowed to update a null cache key instance.");
   44 |     }
   45 |   };
   46 | 
   47 |   private static final int DEFAULT_MULTIPLIER = 37;
   48 |   private static final int DEFAULT_HASHCODE = 17;
   49 | 
   50 |   private final int multiplier;
   51 |   private int hashcode;
   52 |   private long checksum;
   53 |   private int count;
   54 |   // 8/21/2017 - Sonarlint flags this as needing to be marked transient. While true if content is not serializable, this
   55 |   // is not always true and thus should not be marked transient.
   56 |   private List<Object> updateList;
   57 | 
   58 |   public CacheKey() {
   59 |     this.hashcode = DEFAULT_HASHCODE;
   60 |     this.multiplier = DEFAULT_MULTIPLIER;
   61 |     this.count = 0;
   62 |     this.updateList = new ArrayList<>();
   63 |   }
   64 | 
   65 |   public CacheKey(Object[] objects) {
   66 |     this();
   67 |     updateAll(objects);
   68 |   }
   69 | 
   70 |   public int getUpdateCount() {
   71 |     return updateList.size();
   72 |   }
   73 | 
   74 |   public void update(Object object) {
   75 |     int baseHashCode = object == null ? 1 : ArrayUtil.hashCode(object);
   76 | 
   77 |     count++;
   78 |     checksum += baseHashCode;
   79 |     baseHashCode *= count;
   80 | 
   81 |     hashcode = multiplier * hashcode + baseHashCode;
   82 | 
   83 |     updateList.add(object);
   84 |   }
   85 | 
   86 |   public void updateAll(Object[] objects) {
   87 |     for (Object o : objects) {
   88 |       update(o);
   89 |     }
   90 |   }
   91 | 
   92 |   @Override
   93 |   public boolean equals(Object object) {
   94 |     if (this == object) {
   95 |       return true;
   96 |     }
   97 |     if (!(object instanceof CacheKey)) {
   98 |       return false;
   99 |     }
  100 | 
  101 |     final CacheKey cacheKey = (CacheKey) object;
  102 | 
  103 |     if ((hashcode != cacheKey.hashcode) || (checksum != cacheKey.checksum) || (count != cacheKey.count)) {
  104 |       return false;
  105 |     }
  106 | 
  107 |     for (int i = 0; i < updateList.size(); i++) {
  108 |       Object thisObject = updateList.get(i);
  109 |       Object thatObject = cacheKey.updateList.get(i);
  110 |       if (!ArrayUtil.equals(thisObject, thatObject)) {
  111 |         return false;
  112 |       }
  113 |     }
  114 |     return true;
  115 |   }
  116 | 
  117 |   @Override
  118 |   public int hashCode() {
  119 |     return hashcode;
  120 |   }
  121 | 
  122 |   @Override
  123 |   public String toString() {
  124 |     StringJoiner returnValue = new StringJoiner(":");
  125 |     returnValue.add(String.valueOf(hashcode));
  126 |     returnValue.add(String.valueOf(checksum));
  127 |     updateList.stream().map(ArrayUtil::toString).forEach(returnValue::add);
  128 |     return returnValue.toString();
  129 |   }
  130 | 
  131 |   @Override
  132 |   public CacheKey clone() throws CloneNotSupportedException {
  133 |     CacheKey clonedCacheKey = (CacheKey) super.clone();
  134 |     clonedCacheKey.updateList = new ArrayList<>(updateList);
  135 |     return clonedCacheKey;
  136 |   }
  137 | 
  138 | }
```
