<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/type/JdbcType.java -->
<!-- 正文引用：[[源码:JdbcType.java#L起-L止]] ；代码块头部：(src: JdbcType.java#L起-L止) -->

# JdbcType.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/type/JdbcType.java` |
| 行数 | 128 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T15:09:17.734Z |

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
   16 | package org.apache.ibatis.type;
   17 | 
   18 | import java.sql.Types;
   19 | import java.util.HashMap;
   20 | import java.util.Map;
   21 | 
   22 | /**
   23 |  * @author Clinton Begin
   24 |  */
   25 | public enum JdbcType {
   26 |   /*
   27 |    * This is added to enable basic support for the ARRAY data type - but a custom type handler is still required
   28 |    */
   29 |   ARRAY(Types.ARRAY),
   30 | 
   31 |   BIT(Types.BIT),
   32 | 
   33 |   TINYINT(Types.TINYINT),
   34 | 
   35 |   SMALLINT(Types.SMALLINT),
   36 | 
   37 |   INTEGER(Types.INTEGER),
   38 | 
   39 |   BIGINT(Types.BIGINT),
   40 | 
   41 |   FLOAT(Types.FLOAT),
   42 | 
   43 |   REAL(Types.REAL),
   44 | 
   45 |   DOUBLE(Types.DOUBLE),
   46 | 
   47 |   NUMERIC(Types.NUMERIC),
   48 | 
   49 |   DECIMAL(Types.DECIMAL),
   50 | 
   51 |   CHAR(Types.CHAR),
   52 | 
   53 |   VARCHAR(Types.VARCHAR),
   54 | 
   55 |   LONGVARCHAR(Types.LONGVARCHAR),
   56 | 
   57 |   DATE(Types.DATE),
   58 | 
   59 |   TIME(Types.TIME),
   60 | 
   61 |   TIMESTAMP(Types.TIMESTAMP),
   62 | 
   63 |   BINARY(Types.BINARY),
   64 | 
   65 |   VARBINARY(Types.VARBINARY),
   66 | 
   67 |   LONGVARBINARY(Types.LONGVARBINARY),
   68 | 
   69 |   NULL(Types.NULL),
   70 | 
   71 |   OTHER(Types.OTHER),
   72 | 
   73 |   BLOB(Types.BLOB),
   74 | 
   75 |   CLOB(Types.CLOB),
   76 | 
   77 |   BOOLEAN(Types.BOOLEAN),
   78 | 
   79 |   CURSOR(-10), // Oracle
   80 | 
   81 |   UNDEFINED(Integer.MIN_VALUE + 1000),
   82 | 
   83 |   NVARCHAR(Types.NVARCHAR), // JDK6
   84 | 
   85 |   NCHAR(Types.NCHAR), // JDK6
   86 | 
   87 |   NCLOB(Types.NCLOB), // JDK6
   88 | 
   89 |   STRUCT(Types.STRUCT),
   90 | 
   91 |   JAVA_OBJECT(Types.JAVA_OBJECT),
   92 | 
   93 |   DISTINCT(Types.DISTINCT),
   94 | 
   95 |   REF(Types.REF),
   96 | 
   97 |   DATALINK(Types.DATALINK),
   98 | 
   99 |   ROWID(Types.ROWID), // JDK6
  100 | 
  101 |   LONGNVARCHAR(Types.LONGNVARCHAR), // JDK6
  102 | 
  103 |   SQLXML(Types.SQLXML), // JDK6
  104 | 
  105 |   DATETIMEOFFSET(-155), // SQL Server 2008
  106 | 
  107 |   TIME_WITH_TIMEZONE(Types.TIME_WITH_TIMEZONE), // JDBC 4.2 JDK8
  108 | 
  109 |   TIMESTAMP_WITH_TIMEZONE(Types.TIMESTAMP_WITH_TIMEZONE); // JDBC 4.2 JDK8
  110 | 
  111 |   public final int TYPE_CODE;
  112 |   private static final Map<Integer, JdbcType> codeLookup = new HashMap<>();
  113 | 
  114 |   static {
  115 |     for (JdbcType type : JdbcType.values()) {
  116 |       codeLookup.put(type.TYPE_CODE, type);
  117 |     }
  118 |   }
  119 | 
  120 |   JdbcType(int code) {
  121 |     this.TYPE_CODE = code;
  122 |   }
  123 | 
  124 |   public static JdbcType forCode(int code) {
  125 |     return codeLookup.get(code);
  126 |   }
  127 | 
  128 | }
```
