<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/mapping/Environment.java -->
<!-- 正文引用：[[源码:Environment.java#L起-L止]] ；代码块头部：(src: Environment.java#L起-L止) -->

# Environment.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/mapping/Environment.java` |
| 行数 | 86 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:19.649Z |

```java
    1 | /*
    2 |  *    Copyright 2009-2022 the original author or authors.
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
   18 | import javax.sql.DataSource;
   19 | 
   20 | import org.apache.ibatis.transaction.TransactionFactory;
   21 | 
   22 | /**
   23 |  * @author Clinton Begin
   24 |  */
   25 | public final class Environment {
   26 |   private final String id;
   27 |   private final TransactionFactory transactionFactory;
   28 |   private final DataSource dataSource;
   29 | 
   30 |   public Environment(String id, TransactionFactory transactionFactory, DataSource dataSource) {
   31 |     if (id == null) {
   32 |       throw new IllegalArgumentException("Parameter 'id' must not be null");
   33 |     }
   34 |     if (transactionFactory == null) {
   35 |       throw new IllegalArgumentException("Parameter 'transactionFactory' must not be null");
   36 |     }
   37 |     this.id = id;
   38 |     if (dataSource == null) {
   39 |       throw new IllegalArgumentException("Parameter 'dataSource' must not be null");
   40 |     }
   41 |     this.transactionFactory = transactionFactory;
   42 |     this.dataSource = dataSource;
   43 |   }
   44 | 
   45 |   public static class Builder {
   46 |     private final String id;
   47 |     private TransactionFactory transactionFactory;
   48 |     private DataSource dataSource;
   49 | 
   50 |     public Builder(String id) {
   51 |       this.id = id;
   52 |     }
   53 | 
   54 |     public Builder transactionFactory(TransactionFactory transactionFactory) {
   55 |       this.transactionFactory = transactionFactory;
   56 |       return this;
   57 |     }
   58 | 
   59 |     public Builder dataSource(DataSource dataSource) {
   60 |       this.dataSource = dataSource;
   61 |       return this;
   62 |     }
   63 | 
   64 |     public String id() {
   65 |       return this.id;
   66 |     }
   67 | 
   68 |     public Environment build() {
   69 |       return new Environment(this.id, this.transactionFactory, this.dataSource);
   70 |     }
   71 | 
   72 |   }
   73 | 
   74 |   public String getId() {
   75 |     return this.id;
   76 |   }
   77 | 
   78 |   public TransactionFactory getTransactionFactory() {
   79 |     return this.transactionFactory;
   80 |   }
   81 | 
   82 |   public DataSource getDataSource() {
   83 |     return this.dataSource;
   84 |   }
   85 | 
   86 | }
```
