<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-jdbc:7.1.0 | path: org/springframework/jdbc/core/namedparam/NamedParameterUtils.java -->
<!-- 正文引用：[[源码:NamedParameterUtils.java#L起-L止]] ；代码块头部：(src: NamedParameterUtils.java#L起-L止) -->

# NamedParameterUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-jdbc:7.1.0` |
| 路径 | `org/springframework/jdbc/core/namedparam/NamedParameterUtils.java` |
| 行数 | 533 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-jdbc/7.1.0/spring-jdbc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:02:23.480Z |

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
   17 | package org.springframework.jdbc.core.namedparam;
   18 | 
   19 | import java.util.ArrayList;
   20 | import java.util.HashSet;
   21 | import java.util.List;
   22 | import java.util.Map;
   23 | import java.util.Set;
   24 | 
   25 | import org.jspecify.annotations.Nullable;
   26 | 
   27 | import org.springframework.dao.InvalidDataAccessApiUsageException;
   28 | import org.springframework.jdbc.core.SqlParameter;
   29 | import org.springframework.jdbc.core.SqlParameterValue;
   30 | import org.springframework.util.Assert;
   31 | 
   32 | /**
   33 |  * Helper methods for named parameter parsing.
   34 |  *
   35 |  * <p>Only intended for internal use within Spring's JDBC framework.
   36 |  *
   37 |  * @author Thomas Risberg
   38 |  * @author Juergen Hoeller
   39 |  * @author Yanming Zhou
   40 |  * @since 2.0
   41 |  */
   42 | public abstract class NamedParameterUtils {
   43 | 
   44 | 	/**
   45 | 	 * Set of characters that qualify as comment or quote starting characters.
   46 | 	 */
   47 | 	private static final String[] START_SKIP = {"'", "\"", "--", "/*", "`"};
   48 | 
   49 | 	/**
   50 | 	 * Set of characters that are the corresponding comment or quote ending characters.
   51 | 	 */
   52 | 	private static final String[] STOP_SKIP = {"'", "\"", "\n", "*/", "`"};
   53 | 
   54 | 	/**
   55 | 	 * Set of characters that qualify as parameter separators,
   56 | 	 * indicating that a parameter name in an SQL String has ended.
   57 | 	 */
   58 | 	private static final String PARAMETER_SEPARATORS = "\"':&,;()|=+-*%/\\<>^";
   59 | 
   60 | 	/**
   61 | 	 * An index with separator flags per character code.
   62 | 	 * Technically only needed between 34 and 124 at this point.
   63 | 	 */
   64 | 	private static final boolean[] separatorIndex = new boolean[128];
   65 | 
   66 | 	static {
   67 | 		for (char c : PARAMETER_SEPARATORS.toCharArray()) {
   68 | 			separatorIndex[c] = true;
   69 | 		}
   70 | 	}
   71 | 
   72 | 
   73 | 	//-------------------------------------------------------------------------
   74 | 	// Core methods used by NamedParameterJdbcTemplate and SqlQuery/SqlUpdate
   75 | 	//-------------------------------------------------------------------------
   76 | 
   77 | 	/**
   78 | 	 * Parse the SQL statement and locate any placeholders or named parameters.
   79 | 	 * Named parameters are substituted for a JDBC placeholder.
   80 | 	 * @param sql the SQL statement
   81 | 	 * @return the parsed statement, represented as {@link ParsedSql} instance
   82 | 	 */
   83 | 	public static ParsedSql parseSqlStatement(String sql) {
   84 | 		Assert.notNull(sql, "SQL must not be null");
   85 | 
   86 | 		Set<String> namedParameters = new HashSet<>();
   87 | 		StringBuilder sqlToUse = new StringBuilder(sql);
   88 | 		List<ParameterHolder> parameterList = new ArrayList<>();
   89 | 
   90 | 		char[] statement = sql.toCharArray();
   91 | 		int namedParameterCount = 0;
   92 | 		int unnamedParameterCount = 0;
   93 | 		int totalParameterCount = 0;
   94 | 
   95 | 		int escapes = 0;
   96 | 		int i = 0;
   97 | 		while (i < statement.length) {
   98 | 			int skipToPosition = i;
   99 | 			while (i < statement.length) {
  100 | 				skipToPosition = skipCommentsAndQuotes(statement, i);
  101 | 				if (i == skipToPosition) {
  102 | 					break;
  103 | 				}
  104 | 				else {
  105 | 					i = skipToPosition;
  106 | 				}
  107 | 			}
  108 | 			if (i >= statement.length) {
  109 | 				break;
  110 | 			}
  111 | 			char c = statement[i];
  112 | 			if (c == ':' || c == '&') {
  113 | 				int j = i + 1;
  114 | 				if (c == ':' && j < statement.length && statement[j] == ':') {
  115 | 					// Postgres-style "::" casting operator should be skipped
  116 | 					i = i + 2;
  117 | 					continue;
  118 | 				}
  119 | 				String parameter = null;
  120 | 				if (c == ':' && j < statement.length && statement[j] == '{') {
  121 | 					// :{x} style parameter
  122 | 					while (statement[j] != '}') {
  123 | 						j++;
  124 | 						if (j >= statement.length) {
  125 | 							throw new InvalidDataAccessApiUsageException(
  126 | 									"Non-terminated named parameter declaration at position " + i +
  127 | 									" in statement: " + sql);
  128 | 						}
  129 | 						if (statement[j] == ':' || statement[j] == '{') {
  130 | 							throw new InvalidDataAccessApiUsageException(
  131 | 									"Parameter name contains invalid character '" + statement[j] +
  132 | 									"' at position " + i + " in statement: " + sql);
  133 | 						}
  134 | 					}
  135 | 					if (j - i > 2) {
  136 | 						parameter = sql.substring(i + 2, j);
  137 | 						namedParameterCount = addNewNamedParameter(
  138 | 								namedParameters, namedParameterCount, parameter);
  139 | 						totalParameterCount = addNamedParameter(
  140 | 								parameterList, totalParameterCount, escapes, i, j + 1, parameter);
  141 | 					}
  142 | 					j++;
  143 | 				}
  144 | 				else {
  145 | 					boolean paramWithSquareBrackets = false;
  146 | 					while (j < statement.length) {
  147 | 						c = statement[j];
  148 | 						if (isParameterSeparator(c)) {
  149 | 							break;
  150 | 						}
  151 | 						if (c == '[') {
  152 | 							paramWithSquareBrackets = true;
  153 | 						}
  154 | 						else if (c == ']') {
  155 | 							if (!paramWithSquareBrackets) {
  156 | 								break;
  157 | 							}
  158 | 							paramWithSquareBrackets = false;
  159 | 						}
  160 | 						j++;
  161 | 					}
  162 | 					if (j - i > 1) {
  163 | 						parameter = sql.substring(i + 1, j);
  164 | 						namedParameterCount = addNewNamedParameter(
  165 | 								namedParameters, namedParameterCount, parameter);
  166 | 						totalParameterCount = addNamedParameter(
  167 | 								parameterList, totalParameterCount, escapes, i, j, parameter);
  168 | 					}
  169 | 				}
  170 | 				i = j - 1;
  171 | 			}
  172 | 			else {
  173 | 				if (c == '\\') {
  174 | 					int j = i + 1;
  175 | 					if (j < statement.length && statement[j] == ':') {
  176 | 						// escaped ":" should be skipped
  177 | 						sqlToUse.deleteCharAt(i - escapes);
  178 | 						escapes++;
  179 | 						i = i + 2;
  180 | 						continue;
  181 | 					}
  182 | 				}
  183 | 				if (c == '?') {
  184 | 					int j = i + 1;
  185 | 					if (j < statement.length && (statement[j] == '?' || statement[j] == '|' || statement[j] == '&')) {
  186 | 						// Postgres-style "??", "?|", "?&" operator should be skipped
  187 | 						i = i + 2;
  188 | 						continue;
  189 | 					}
  190 | 					unnamedParameterCount++;
  191 | 					totalParameterCount++;
  192 | 				}
  193 | 			}
  194 | 			i++;
  195 | 		}
  196 | 		ParsedSql parsedSql = new ParsedSql(sqlToUse.toString());
  197 | 		for (ParameterHolder ph : parameterList) {
  198 | 			parsedSql.addNamedParameter(ph.getParameterName(), ph.getStartIndex(), ph.getEndIndex());
  199 | 		}
  200 | 		parsedSql.setNamedParameterCount(namedParameterCount);
  201 | 		parsedSql.setUnnamedParameterCount(unnamedParameterCount);
  202 | 		parsedSql.setTotalParameterCount(totalParameterCount);
  203 | 		return parsedSql;
  204 | 	}
  205 | 
  206 | 	private static int addNamedParameter(List<ParameterHolder> parameterList,
  207 | 			int totalParameterCount, int escapes, int i, int j, String parameter) {
  208 | 
  209 | 		parameterList.add(new ParameterHolder(parameter, i - escapes, j - escapes));
  210 | 		totalParameterCount++;
  211 | 		return totalParameterCount;
  212 | 	}
  213 | 
  214 | 	private static int addNewNamedParameter(Set<String> namedParameters, int namedParameterCount, String parameter) {
  215 | 		if (!namedParameters.contains(parameter)) {
  216 | 			namedParameters.add(parameter);
  217 | 			namedParameterCount++;
  218 | 		}
  219 | 		return namedParameterCount;
  220 | 	}
  221 | 
  222 | 	/**
  223 | 	 * Skip over comments and quoted names present in an SQL statement.
  224 | 	 * @param statement character array containing SQL statement
  225 | 	 * @param position current position of statement
  226 | 	 * @return next position to process after any comments or quotes are skipped
  227 | 	 */
  228 | 	private static int skipCommentsAndQuotes(char[] statement, int position) {
  229 | 		for (int i = 0; i < START_SKIP.length; i++) {
  230 | 			if (statement[position] == START_SKIP[i].charAt(0)) {
  231 | 				boolean match = true;
  232 | 				for (int j = 1; j < START_SKIP[i].length(); j++) {
  233 | 					if (statement[position + j] != START_SKIP[i].charAt(j)) {
  234 | 						match = false;
  235 | 						break;
  236 | 					}
  237 | 				}
  238 | 				if (match) {
  239 | 					int offset = START_SKIP[i].length();
  240 | 					for (int m = position + offset; m < statement.length; m++) {
  241 | 						if (statement[m] == STOP_SKIP[i].charAt(0)) {
  242 | 							boolean endMatch = true;
  243 | 							int endPos = m;
  244 | 							for (int n = 1; n < STOP_SKIP[i].length(); n++) {
  245 | 								if (m + n >= statement.length) {
  246 | 									// last comment not closed properly
  247 | 									return statement.length;
  248 | 								}
  249 | 								if (statement[m + n] != STOP_SKIP[i].charAt(n)) {
  250 | 									endMatch = false;
  251 | 									break;
  252 | 								}
  253 | 								endPos = m + n;
  254 | 							}
  255 | 							if (endMatch) {
  256 | 								// found character sequence ending comment or quote
  257 | 								return endPos + 1;
  258 | 							}
  259 | 						}
  260 | 					}
  261 | 					// character sequence ending comment or quote not found
  262 | 					return statement.length;
  263 | 				}
  264 | 			}
  265 | 		}
  266 | 		return position;
  267 | 	}
  268 | 
  269 | 	/**
  270 | 	 * Parse the SQL statement and locate any placeholders or named parameters. Named
  271 | 	 * parameters are substituted for a JDBC placeholder, and any select list is expanded
  272 | 	 * to the required number of placeholders. Select lists may contain an array of
  273 | 	 * objects, and in that case the placeholders will be grouped and enclosed with
  274 | 	 * parentheses. This allows for the use of "expression lists" in the SQL statement
  275 | 	 * like: <br /><br />
  276 | 	 * {@code select id, name, state from table where (name, age) in (('John', 35), ('Ann', 50))}
  277 | 	 * <p>The parameter values passed in are used to determine the number of
  278 | 	 * placeholders to be used for a select list. Select lists should not be empty
  279 | 	 * and should be limited to 100 or fewer elements. An empty list or a larger
  280 | 	 * number of elements is not guaranteed to be supported by the database and
  281 | 	 * is strictly vendor-dependent.
  282 | 	 * @param parsedSql the parsed representation of the SQL statement
  283 | 	 * @param paramSource the source for named parameters
  284 | 	 * @return the SQL statement with substituted parameters
  285 | 	 * @see #parseSqlStatement
  286 | 	 */
  287 | 	public static String substituteNamedParameters(ParsedSql parsedSql, @Nullable SqlParameterSource paramSource) {
  288 | 		String originalSql = parsedSql.getOriginalSql();
  289 | 		List<String> paramNames = parsedSql.getParameterNames();
  290 | 		if (paramNames.isEmpty()) {
  291 | 			return originalSql;
  292 | 		}
  293 | 
  294 | 		StringBuilder actualSql = new StringBuilder(originalSql.length());
  295 | 		int lastIndex = 0;
  296 | 		for (int i = 0; i < paramNames.size(); i++) {
  297 | 			String paramName = paramNames.get(i);
  298 | 			int[] indexes = parsedSql.getParameterIndexes(i);
  299 | 			int startIndex = indexes[0];
  300 | 			int endIndex = indexes[1];
  301 | 			actualSql.append(originalSql, lastIndex, startIndex);
  302 | 			if (paramSource != null && paramSource.hasValue(paramName)) {
  303 | 				Object value = paramSource.getValue(paramName);
  304 | 				if (value instanceof SqlParameterValue sqlParameterValue) {
  305 | 					value = sqlParameterValue.getValue();
  306 | 				}
  307 | 				if (value instanceof Iterable<?> iterable) {
  308 | 					int k = 0;
  309 | 					for (Object entryItem : iterable) {
  310 | 						if (k > 0) {
  311 | 							actualSql.append(", ");
  312 | 						}
  313 | 						k++;
  314 | 						if (entryItem instanceof Object[] expressionList) {
  315 | 							actualSql.append('(');
  316 | 							for (int m = 0; m < expressionList.length; m++) {
  317 | 								if (m > 0) {
  318 | 									actualSql.append(", ");
  319 | 								}
  320 | 								actualSql.append('?');
  321 | 							}
  322 | 							actualSql.append(')');
  323 | 						}
  324 | 						else {
  325 | 							actualSql.append('?');
  326 | 						}
  327 | 					}
  328 | 				}
  329 | 				else {
  330 | 					actualSql.append('?');
  331 | 				}
  332 | 			}
  333 | 			else {
  334 | 				actualSql.append('?');
  335 | 			}
  336 | 			lastIndex = endIndex;
  337 | 		}
  338 | 		actualSql.append(originalSql, lastIndex, originalSql.length());
  339 | 		return actualSql.toString();
  340 | 	}
  341 | 
  342 | 	/**
  343 | 	 * Convert a Map of named parameter values to a corresponding array.
  344 | 	 * @param parsedSql the parsed SQL statement
  345 | 	 * @param paramSource the source for named parameters
  346 | 	 * @param declaredParams the List of declared SqlParameter objects
  347 | 	 * (may be {@code null}). If specified, the parameter metadata will
  348 | 	 * be built into the value array in the form of SqlParameterValue objects.
  349 | 	 * @return the array of values
  350 | 	 */
  351 | 	public static @Nullable Object[] buildValueArray(
  352 | 			ParsedSql parsedSql, SqlParameterSource paramSource, @Nullable List<SqlParameter> declaredParams) {
  353 | 
  354 | 		@Nullable Object[] paramArray = new Object[parsedSql.getTotalParameterCount()];
  355 | 		if (parsedSql.getNamedParameterCount() > 0 && parsedSql.getUnnamedParameterCount() > 0) {
  356 | 			throw new InvalidDataAccessApiUsageException(
  357 | 					"Not allowed to mix named and traditional ? placeholders. You have " +
  358 | 					parsedSql.getNamedParameterCount() + " named parameter(s) and " +
  359 | 					parsedSql.getUnnamedParameterCount() + " traditional placeholder(s) in statement: " +
  360 | 					parsedSql.getOriginalSql());
  361 | 		}
  362 | 		List<String> paramNames = parsedSql.getParameterNames();
  363 | 		for (int i = 0; i < paramNames.size(); i++) {
  364 | 			String paramName = paramNames.get(i);
  365 | 			try {
  366 | 				Object paramValue = paramSource.getValue(paramName);
  367 | 				if (paramValue instanceof SqlParameterValue) {
  368 | 					paramArray[i] = paramValue;
  369 | 				}
  370 | 				else {
  371 | 					SqlParameter param = findParameter(declaredParams, paramName, i);
  372 | 					paramArray[i] = (param != null ? new SqlParameterValue(param, paramValue) :
  373 | 							SqlParameterSourceUtils.getTypedValue(paramSource, paramName));
  374 | 				}
  375 | 			}
  376 | 			catch (IllegalArgumentException ex) {
  377 | 				throw new InvalidDataAccessApiUsageException(
  378 | 						"No value supplied for the SQL parameter '" + paramName + "': " + ex.getMessage());
  379 | 			}
  380 | 		}
  381 | 		return paramArray;
  382 | 	}
  383 | 
  384 | 	/**
  385 | 	 * Find a matching parameter in the given list of declared parameters.
  386 | 	 * @param declaredParams the declared SqlParameter objects
  387 | 	 * @param paramName the name of the desired parameter
  388 | 	 * @param paramIndex the index of the desired parameter
  389 | 	 * @return the declared SqlParameter, or {@code null} if none found
  390 | 	 */
  391 | 	private static @Nullable SqlParameter findParameter(
  392 | 			@Nullable List<SqlParameter> declaredParams, String paramName, int paramIndex) {
  393 | 
  394 | 		if (declaredParams != null) {
  395 | 			// First pass: Look for named parameter match.
  396 | 			for (SqlParameter declaredParam : declaredParams) {
  397 | 				if (paramName.equals(declaredParam.getName())) {
  398 | 					return declaredParam;
  399 | 				}
  400 | 			}
  401 | 			// Second pass: Look for parameter index match.
  402 | 			if (paramIndex < declaredParams.size()) {
  403 | 				SqlParameter declaredParam = declaredParams.get(paramIndex);
  404 | 				// Only accept unnamed parameters for index matches.
  405 | 				if (declaredParam.getName() == null) {
  406 | 					return declaredParam;
  407 | 				}
  408 | 			}
  409 | 		}
  410 | 		return null;
  411 | 	}
  412 | 
  413 | 	/**
  414 | 	 * Determine whether a parameter name ends at the current position,
  415 | 	 * that is, whether the given character qualifies as a separator.
  416 | 	 */
  417 | 	private static boolean isParameterSeparator(char c) {
  418 | 		return (c < 128 && separatorIndex[c]) || Character.isWhitespace(c);
  419 | 	}
  420 | 
  421 | 	/**
  422 | 	 * Convert parameter types from an SqlParameterSource into a corresponding int array.
  423 | 	 * This is necessary in order to reuse existing methods on JdbcTemplate.
  424 | 	 * Any named parameter types are placed in the correct position in the
  425 | 	 * Object array based on the parsed SQL statement info.
  426 | 	 * @param parsedSql the parsed SQL statement
  427 | 	 * @param paramSource the source for named parameters
  428 | 	 */
  429 | 	public static int[] buildSqlTypeArray(ParsedSql parsedSql, SqlParameterSource paramSource) {
  430 | 		int[] sqlTypes = new int[parsedSql.getTotalParameterCount()];
  431 | 		List<String> paramNames = parsedSql.getParameterNames();
  432 | 		for (int i = 0; i < paramNames.size(); i++) {
  433 | 			String paramName = paramNames.get(i);
  434 | 			sqlTypes[i] = paramSource.getSqlType(paramName);
  435 | 		}
  436 | 		return sqlTypes;
  437 | 	}
  438 | 
  439 | 	/**
  440 | 	 * Convert parameter declarations from an SqlParameterSource to a corresponding List of SqlParameters.
  441 | 	 * This is necessary in order to reuse existing methods on JdbcTemplate.
  442 | 	 * The SqlParameter for a named parameter is placed in the correct position in the
  443 | 	 * resulting list based on the parsed SQL statement info.
  444 | 	 * @param parsedSql the parsed SQL statement
  445 | 	 * @param paramSource the source for named parameters
  446 | 	 */
  447 | 	public static List<SqlParameter> buildSqlParameterList(ParsedSql parsedSql, SqlParameterSource paramSource) {
  448 | 		List<String> paramNames = parsedSql.getParameterNames();
  449 | 		List<SqlParameter> params = new ArrayList<>(paramNames.size());
  450 | 		for (String paramName : paramNames) {
  451 | 			params.add(new SqlParameter(
  452 | 					paramName, paramSource.getSqlType(paramName), paramSource.getTypeName(paramName)));
  453 | 		}
  454 | 		return params;
  455 | 	}
  456 | 
  457 | 
  458 | 	//-------------------------------------------------------------------------
  459 | 	// Convenience methods operating on a plain SQL String
  460 | 	//-------------------------------------------------------------------------
  461 | 
  462 | 	/**
  463 | 	 * Parse the SQL statement and locate any placeholders or named parameters.
  464 | 	 * <p>Named parameters are substituted for a JDBC placeholder.
  465 | 	 * <p>This is a shortcut version of
  466 | 	 * {@link #parseSqlStatement(String)} in combination with
  467 | 	 * {@link #substituteNamedParameters(ParsedSql, SqlParameterSource)}.
  468 | 	 * @param sql the SQL statement
  469 | 	 * @return the actual (parsed) SQL statement
  470 | 	 */
  471 | 	public static String parseSqlStatementIntoString(String sql) {
  472 | 		ParsedSql parsedSql = parseSqlStatement(sql);
  473 | 		return substituteNamedParameters(parsedSql, null);
  474 | 	}
  475 | 
  476 | 	/**
  477 | 	 * Parse the SQL statement and locate any placeholders or named parameters.
  478 | 	 * <p>Named parameters are substituted for a JDBC placeholder, and any select
  479 | 	 * list is expanded to the required number of placeholders.
  480 | 	 * <p>This is a shortcut version of
  481 | 	 * {@link #parseSqlStatement(String)} in combination with
  482 | 	 * {@link #substituteNamedParameters(ParsedSql, SqlParameterSource)}.
  483 | 	 * @param sql the SQL statement
  484 | 	 * @param paramSource the source for named parameters
  485 | 	 * @return the SQL statement with substituted parameters
  486 | 	 */
  487 | 	public static String substituteNamedParameters(String sql, SqlParameterSource paramSource) {
  488 | 		ParsedSql parsedSql = parseSqlStatement(sql);
  489 | 		return substituteNamedParameters(parsedSql, paramSource);
  490 | 	}
  491 | 
  492 | 	/**
  493 | 	 * Convert a Map of named parameter values to a corresponding array.
  494 | 	 * <p>This is a shortcut version of
  495 | 	 * {@link #buildValueArray(ParsedSql, SqlParameterSource, java.util.List)}.
  496 | 	 * @param sql the SQL statement
  497 | 	 * @param paramMap the Map of parameters
  498 | 	 * @return the array of values
  499 | 	 */
  500 | 	public static @Nullable Object[] buildValueArray(String sql, Map<String, ?> paramMap) {
  501 | 		ParsedSql parsedSql = parseSqlStatement(sql);
  502 | 		return buildValueArray(parsedSql, new MapSqlParameterSource(paramMap), null);
  503 | 	}
  504 | 
  505 | 
  506 | 	private static class ParameterHolder {
  507 | 
  508 | 		private final String parameterName;
  509 | 
  510 | 		private final int startIndex;
  511 | 
  512 | 		private final int endIndex;
  513 | 
  514 | 		public ParameterHolder(String parameterName, int startIndex, int endIndex) {
  515 | 			this.parameterName = parameterName;
  516 | 			this.startIndex = startIndex;
  517 | 			this.endIndex = endIndex;
  518 | 		}
  519 | 
  520 | 		public String getParameterName() {
  521 | 			return this.parameterName;
  522 | 		}
  523 | 
  524 | 		public int getStartIndex() {
  525 | 			return this.startIndex;
  526 | 		}
  527 | 
  528 | 		public int getEndIndex() {
  529 | 			return this.endIndex;
  530 | 		}
  531 | 	}
  532 | 
  533 | }
```
