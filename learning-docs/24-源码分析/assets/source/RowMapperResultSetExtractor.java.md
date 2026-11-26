<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/core/RowMapperResultSetExtractor.java -->
<!-- 正文引用：[[源码:RowMapperResultSetExtractor.java#L起-L止]] ；代码块头部：(src: RowMapperResultSetExtractor.java#L起-L止) -->

# RowMapperResultSetExtractor.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/core/RowMapperResultSetExtractor.java` |
| 行数 | 115 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:10.062Z |

```java
    1 | /*
    2 |  * Copyright 2002-present the original author or authors.
    3 |  *
    4 |  * Licensed under the Apache License, Version 2.0 (the "License");
    5 |  * you may not use this file except in compliance with the License.
    6 |  * You may obtain a copy of the License at
    7 |  *
    8 |  *      https://www.apache.org/licenses/LICENSE-2.0
    9 |  *
   10 |  * Unless required by applicable law or agreed to in writing, software
   11 |  * distributed under the License is distributed on an "AS IS" BASIS,
   12 |  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   13 |  * See the License for the specific language governing permissions and
   14 |  * limitations under the License.
   15 |  */
   16 | 
   17 | package org.springframework.jdbc.core;
   18 | 
   19 | import java.sql.ResultSet;
   20 | import java.sql.SQLException;
   21 | import java.util.ArrayList;
   22 | import java.util.List;
   23 | 
   24 | import org.springframework.util.Assert;
   25 | 
   26 | /**
   27 |  * Adapter implementation of the ResultSetExtractor interface that delegates
   28 |  * to a RowMapper which is supposed to create an object for each row.
   29 |  * Each object is added to the results List of this ResultSetExtractor.
   30 |  *
   31 |  * <p>Useful for the typical case of one object per row in the database table.
   32 |  * The number of entries in the results list will match the number of rows.
   33 |  *
   34 |  * <p>Note that a RowMapper object is typically stateless and thus reusable;
   35 |  * just the RowMapperResultSetExtractor adapter is stateful.
   36 |  *
   37 |  * <p>A usage example with JdbcTemplate:
   38 |  *
   39 |  * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
   40 |  * RowMapper rowMapper = new UserRowMapper();  // reusable object
   41 |  *
   42 |  * List allUsers = (List) jdbcTemplate.query(
   43 |  *     "select * from user",
   44 |  *     new RowMapperResultSetExtractor(rowMapper, 10));
   45 |  *
   46 |  * User user = (User) jdbcTemplate.queryForObject(
   47 |  *     "select * from user where id=?", new Object[] {id},
   48 |  *     new RowMapperResultSetExtractor(rowMapper, 1));</pre>
   49 |  *
   50 |  * <p>Alternatively, consider subclassing MappingSqlQuery from the {@code jdbc.object}
   51 |  * package: Instead of working with separate JdbcTemplate and RowMapper objects,
   52 |  * you can have executable query objects (containing row-mapping logic) there.
   53 |  *
   54 |  * @author Juergen Hoeller
   55 |  * @author Yanming Zhou
   56 |  * @since 1.0.2
   57 |  * @param <T> the result element type
   58 |  * @see RowMapper
   59 |  * @see JdbcTemplate
   60 |  * @see org.springframework.jdbc.object.MappingSqlQuery
   61 |  */
   62 | public class RowMapperResultSetExtractor<T> implements ResultSetExtractor<List<T>> {
   63 | 
   64 | 	private final RowMapper<T> rowMapper;
   65 | 
   66 | 	private final int rowsExpected;
   67 | 
   68 | 	private final int maxRows;
   69 | 
   70 | 
   71 | 	/**
   72 | 	 * Create a new RowMapperResultSetExtractor.
   73 | 	 * @param rowMapper the RowMapper which creates an object for each row
   74 | 	 */
   75 | 	public RowMapperResultSetExtractor(RowMapper<T> rowMapper) {
   76 | 		this(rowMapper, 0);
   77 | 	}
   78 | 
   79 | 	/**
   80 | 	 * Create a new RowMapperResultSetExtractor.
   81 | 	 * @param rowMapper the RowMapper which creates an object for each row
   82 | 	 * @param rowsExpected the number of expected rows
   83 | 	 * (just used for optimized collection handling)
   84 | 	 */
   85 | 	public RowMapperResultSetExtractor(RowMapper<T> rowMapper, int rowsExpected) {
   86 | 		this(rowMapper, rowsExpected, -1);
   87 | 	}
   88 | 
   89 | 	/**
   90 | 	 * Create a new RowMapperResultSetExtractor.
   91 | 	 * @param rowMapper the RowMapper which creates an object for each row
   92 | 	 * @param rowsExpected the number of expected rows
   93 | 	 * (just used for optimized collection handling)
   94 | 	 * @param maxRows the number of max rows (or -1 for the driver's default)
   95 | 	 * @since 7.0
   96 | 	 */
   97 | 	public RowMapperResultSetExtractor(RowMapper<T> rowMapper, int rowsExpected, int maxRows) {
   98 | 		Assert.notNull(rowMapper, "RowMapper must not be null");
   99 | 		this.rowMapper = rowMapper;
  100 | 		this.rowsExpected = rowsExpected;
  101 | 		this.maxRows = maxRows;
  102 | 	}
  103 | 
  104 | 
  105 | 	@Override
  106 | 	public List<T> extractData(ResultSet rs) throws SQLException {
  107 | 		List<T> results = (this.rowsExpected > 0 ? new ArrayList<>(this.rowsExpected) : new ArrayList<>());
  108 | 		int rowNum = 0;
  109 | 		while (rs.next() && (this.maxRows == -1 || rowNum < this.maxRows)) {
  110 | 			results.add(this.rowMapper.mapRow(rs, rowNum++));
  111 | 		}
  112 | 		return results;
  113 | 	}
  114 | 
  115 | }
```
