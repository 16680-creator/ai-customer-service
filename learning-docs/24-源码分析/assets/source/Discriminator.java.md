<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/mapping/Discriminator.java -->
<!-- 正文引用：[[源码:Discriminator.java#L起-L止]] ；代码块头部：(src: Discriminator.java#L起-L止) -->

# Discriminator.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/mapping/Discriminator.java` |
| 行数 | 63 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:00:25.703Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2026 the original author or authors.
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
   16 | package org.apache.ibatis.mapping;
   17 | 
   18 | import java.util.Map;
   19 | 
   20 | import org.apache.ibatis.session.Configuration;
   21 | 
   22 | /**
   23 |  * @author Clinton Begin
   24 |  */
   25 | public class Discriminator {
   26 | 
   27 |   private ResultMapping resultMapping;
   28 |   private Map<String, String> discriminatorMap;
   29 | 
   30 |   Discriminator() {
   31 |   }
   32 | 
   33 |   public static class Builder {
   34 |     private final Discriminator discriminator = new Discriminator();
   35 | 
   36 |     public Builder(Configuration configuration, ResultMapping resultMapping, Map<String, String> discriminatorMap) {
   37 |       discriminator.resultMapping = resultMapping;
   38 |       discriminator.discriminatorMap = discriminatorMap;
   39 |     }
   40 | 
   41 |     public Discriminator build() {
   42 |       assert discriminator.resultMapping != null;
   43 |       assert discriminator.discriminatorMap != null;
   44 |       assert !discriminator.discriminatorMap.isEmpty();
   45 |       // lock down map
   46 |       discriminator.discriminatorMap = Map.copyOf(discriminator.discriminatorMap);
   47 |       return discriminator;
   48 |     }
   49 |   }
   50 | 
   51 |   public ResultMapping getResultMapping() {
   52 |     return resultMapping;
   53 |   }
   54 | 
   55 |   public Map<String, String> getDiscriminatorMap() {
   56 |     return discriminatorMap;
   57 |   }
   58 | 
   59 |   public String getMapIdFor(String s) {
   60 |     return discriminatorMap.get(s);
   61 |   }
   62 | 
   63 | }
```
