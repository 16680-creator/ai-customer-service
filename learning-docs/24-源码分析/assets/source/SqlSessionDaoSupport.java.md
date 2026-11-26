<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.mybatis:mybatis-spring:3.0.5 | path: org/mybatis/spring/support/SqlSessionDaoSupport.java -->
<!-- 正文引用：[[源码:SqlSessionDaoSupport.java#L起-L止]] ；代码块头部：(src: SqlSessionDaoSupport.java#L起-L止) -->

# SqlSessionDaoSupport.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.mybatis:mybatis-spring:3.0.5` |
| 路径 | `org/mybatis/spring/support/SqlSessionDaoSupport.java` |
| 行数 | 125 |
| 在线地址 | https://repo1.maven.org/maven2/org/mybatis/mybatis-spring/3.0.5/mybatis-spring-3.0.5-sources.jar |
| 抓取时间 | 2026-11-25T15:20:11.389Z |

```java
    1 | /*
    2 |  * Copyright 2010-2024 the original author or authors.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *    https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | package org.mybatis.spring.support;
   17 | 
   18 | import static org.springframework.util.Assert.notNull;
   19 | 
   20 | import org.apache.ibatis.session.SqlSession;
   21 | import org.apache.ibatis.session.SqlSessionFactory;
   22 | import org.mybatis.spring.SqlSessionTemplate;
   23 | import org.springframework.dao.support.DaoSupport;
   24 | 
   25 | /**
   26 |  * Convenient super class for MyBatis SqlSession data access objects. It gives you access to the template which can then
   27 |  * be used to execute SQL methods.
   28 |  * <p>
   29 |  * This class needs a SqlSessionTemplate or a SqlSessionFactory. If both are set the SqlSessionFactory will be ignored.
   30 |  * <p>
   31 |  *
   32 |  * @author Putthiphong Boonphong
   33 |  * @author Eduardo Macarron
   34 |  *
   35 |  * @see #setSqlSessionFactory
   36 |  * @see #setSqlSessionTemplate
   37 |  * @see SqlSessionTemplate
   38 |  */
   39 | public abstract class SqlSessionDaoSupport extends DaoSupport {
   40 | 
   41 |   private SqlSessionTemplate sqlSessionTemplate;
   42 | 
   43 |   /**
   44 |    * Set MyBatis SqlSessionFactory to be used by this DAO. Will automatically create SqlSessionTemplate for the given
   45 |    * SqlSessionFactory.
   46 |    *
   47 |    * @param sqlSessionFactory
   48 |    *          a factory of SqlSession
   49 |    */
   50 |   public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
   51 |     if (this.sqlSessionTemplate == null || sqlSessionFactory != this.sqlSessionTemplate.getSqlSessionFactory()) {
   52 |       this.sqlSessionTemplate = createSqlSessionTemplate(sqlSessionFactory);
   53 |     }
   54 |   }
   55 | 
   56 |   /**
   57 |    * Create a SqlSessionTemplate for the given SqlSessionFactory. Only invoked if populating the DAO with a
   58 |    * SqlSessionFactory reference!
   59 |    * <p>
   60 |    * Can be overridden in subclasses to provide a SqlSessionTemplate instance with different configuration, or a custom
   61 |    * SqlSessionTemplate subclass.
   62 |    *
   63 |    * @param sqlSessionFactory
   64 |    *          the MyBatis SqlSessionFactory to create a SqlSessionTemplate for
   65 |    *
   66 |    * @return the new SqlSessionTemplate instance
   67 |    *
   68 |    * @see #setSqlSessionFactory
   69 |    */
   70 |   @SuppressWarnings("WeakerAccess")
   71 |   protected SqlSessionTemplate createSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
   72 |     return new SqlSessionTemplate(sqlSessionFactory);
   73 |   }
   74 | 
   75 |   /**
   76 |    * Return the MyBatis SqlSessionFactory used by this DAO.
   77 |    *
   78 |    * @return a factory of SqlSession
   79 |    */
   80 |   public final SqlSessionFactory getSqlSessionFactory() {
   81 |     return this.sqlSessionTemplate != null ? this.sqlSessionTemplate.getSqlSessionFactory() : null;
   82 |   }
   83 | 
   84 |   /**
   85 |    * Set the SqlSessionTemplate for this DAO explicitly, as an alternative to specifying a SqlSessionFactory.
   86 |    *
   87 |    * @param sqlSessionTemplate
   88 |    *          a template of SqlSession
   89 |    *
   90 |    * @see #setSqlSessionFactory
   91 |    */
   92 |   public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
   93 |     this.sqlSessionTemplate = sqlSessionTemplate;
   94 |   }
   95 | 
   96 |   /**
   97 |    * Users should use this method to get a SqlSession to call its statement methods This is SqlSession is managed by
   98 |    * spring. Users should not commit/rollback/close it because it will be automatically done.
   99 |    *
  100 |    * @return Spring managed thread safe SqlSession
  101 |    */
  102 |   public SqlSession getSqlSession() {
  103 |     return this.sqlSessionTemplate;
  104 |   }
  105 | 
  106 |   /**
  107 |    * Return the SqlSessionTemplate for this DAO, pre-initialized with the SessionFactory or set explicitly.
  108 |    * <p>
  109 |    * <b>Note: The returned SqlSessionTemplate is a shared instance.</b> You may introspect its configuration, but not
  110 |    * modify the configuration (other than from within an {@link #initDao} implementation). Consider creating a custom
  111 |    * SqlSessionTemplate instance via {@code new SqlSessionTemplate(getSqlSessionFactory())}, in which case you're
  112 |    * allowed to customize the settings on the resulting instance.
  113 |    *
  114 |    * @return a template of SqlSession
  115 |    */
  116 |   public SqlSessionTemplate getSqlSessionTemplate() {
  117 |     return this.sqlSessionTemplate;
  118 |   }
  119 | 
  120 |   @Override
  121 |   protected void checkDaoConfig() {
  122 |     notNull(this.sqlSessionTemplate, "Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required");
  123 |   }
  124 | 
  125 | }
```
