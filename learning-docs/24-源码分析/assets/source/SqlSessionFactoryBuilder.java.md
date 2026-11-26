<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis:3.6.0 | path: org/apache/ibatis/session/SqlSessionFactoryBuilder.java -->
<!-- 正文引用：[[源码:SqlSessionFactoryBuilder.java#L起-L止]] ；代码块头部：(src: SqlSessionFactoryBuilder.java#L起-L止) -->

# SqlSessionFactoryBuilder.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis:3.6.0` |
| 路径 | `org/apache/ibatis/session/SqlSessionFactoryBuilder.java` |
| 行数 | 99 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis/3.6.0/mybatis-3.6.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:18.066Z |

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
   16 | package org.apache.ibatis.session;
   17 | 
   18 | import java.io.IOException;
   19 | import java.io.InputStream;
   20 | import java.io.Reader;
   21 | import java.util.Properties;
   22 | 
   23 | import org.apache.ibatis.builder.xml.XMLConfigBuilder;
   24 | import org.apache.ibatis.exceptions.ExceptionFactory;
   25 | import org.apache.ibatis.executor.ErrorContext;
   26 | import org.apache.ibatis.session.defaults.DefaultSqlSessionFactory;
   27 | 
   28 | /**
   29 |  * Builds {@link SqlSession} instances.
   30 |  *
   31 |  * @author Clinton Begin
   32 |  */
   33 | public class SqlSessionFactoryBuilder {
   34 | 
   35 |   public SqlSessionFactory build(Reader reader) {
   36 |     return build(reader, null, null);
   37 |   }
   38 | 
   39 |   public SqlSessionFactory build(Reader reader, String environment) {
   40 |     return build(reader, environment, null);
   41 |   }
   42 | 
   43 |   public SqlSessionFactory build(Reader reader, Properties properties) {
   44 |     return build(reader, null, properties);
   45 |   }
   46 | 
   47 |   public SqlSessionFactory build(Reader reader, String environment, Properties properties) {
   48 |     try {
   49 |       XMLConfigBuilder parser = new XMLConfigBuilder(reader, environment, properties);
   50 |       return build(parser.parse());
   51 |     } catch (Exception e) {
   52 |       throw ExceptionFactory.wrapException("Error building SqlSession.", e);
   53 |     } finally {
   54 |       ErrorContext.instance().reset();
   55 |       try {
   56 |         if (reader != null) {
   57 |           reader.close();
   58 |         }
   59 |       } catch (IOException e) {
   60 |         // Intentionally ignore. Prefer previous error.
   61 |       }
   62 |     }
   63 |   }
   64 | 
   65 |   public SqlSessionFactory build(InputStream inputStream) {
   66 |     return build(inputStream, null, null);
   67 |   }
   68 | 
   69 |   public SqlSessionFactory build(InputStream inputStream, String environment) {
   70 |     return build(inputStream, environment, null);
   71 |   }
   72 | 
   73 |   public SqlSessionFactory build(InputStream inputStream, Properties properties) {
   74 |     return build(inputStream, null, properties);
   75 |   }
   76 | 
   77 |   public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
   78 |     try {
   79 |       XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
   80 |       return build(parser.parse());
   81 |     } catch (Exception e) {
   82 |       throw ExceptionFactory.wrapException("Error building SqlSession.", e);
   83 |     } finally {
   84 |       ErrorContext.instance().reset();
   85 |       try {
   86 |         if (inputStream != null) {
   87 |           inputStream.close();
   88 |         }
   89 |       } catch (IOException e) {
   90 |         // Intentionally ignore. Prefer previous error.
   91 |       }
   92 |     }
   93 |   }
   94 | 
   95 |   public SqlSessionFactory build(Configuration config) {
   96 |     return new DefaultSqlSessionFactory(config);
   97 |   }
   98 | 
   99 | }
```
