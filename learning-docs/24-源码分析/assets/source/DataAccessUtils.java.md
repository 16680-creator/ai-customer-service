<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-tx:7.1.0 | path: org/springframework/dao/support/DataAccessUtils.java -->
<!-- 正文引用：[[源码:DataAccessUtils.java#L起-L止]] ；代码块头部：(src: DataAccessUtils.java#L起-L止) -->

# DataAccessUtils.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-tx:7.1.0` |
| 路径 | `org/springframework/dao/support/DataAccessUtils.java` |
| 行数 | 357 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-tx/7.1.0/spring-tx-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:08:12.061Z |

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
   17 | package org.springframework.dao.support;
   18 | 
   19 | import java.util.Collection;
   20 | import java.util.Iterator;
   21 | import java.util.List;
   22 | import java.util.Optional;
   23 | import java.util.stream.Stream;
   24 | 
   25 | import org.jspecify.annotations.NonNull;
   26 | import org.jspecify.annotations.Nullable;
   27 | 
   28 | import org.springframework.dao.DataAccessException;
   29 | import org.springframework.dao.EmptyResultDataAccessException;
   30 | import org.springframework.dao.IncorrectResultSizeDataAccessException;
   31 | import org.springframework.dao.TypeMismatchDataAccessException;
   32 | import org.springframework.util.Assert;
   33 | import org.springframework.util.CollectionUtils;
   34 | import org.springframework.util.NumberUtils;
   35 | 
   36 | /**
   37 |  * Miscellaneous utility methods for DAO implementations.
   38 |  *
   39 |  * <p>Useful with any data access technology.
   40 |  *
   41 |  * @author Juergen Hoeller
   42 |  * @since 1.0.2
   43 |  */
   44 | public abstract class DataAccessUtils {
   45 | 
   46 | 	/**
   47 | 	 * Return a single result object from the given Collection.
   48 | 	 * <p>Returns {@code null} if 0 result objects found;
   49 | 	 * throws an exception if more than 1 element found.
   50 | 	 * @param results the result Collection (can be {@code null})
   51 | 	 * @return the single result object, or {@code null} if none
   52 | 	 * @throws IncorrectResultSizeDataAccessException if more than one
   53 | 	 * element has been found in the given Collection
   54 | 	 */
   55 | 	public static <T extends @Nullable Object> @Nullable T singleResult(@Nullable Collection<T> results)
   56 | 			throws IncorrectResultSizeDataAccessException {
   57 | 		if (CollectionUtils.isEmpty(results)) {
   58 | 			return null;
   59 | 		}
   60 | 		if (results.size() > 1) {
   61 | 			throw new IncorrectResultSizeDataAccessException(1, results.size());
   62 | 		}
   63 | 		return results.iterator().next();
   64 | 	}
   65 | 
   66 | 	/**
   67 | 	 * Return a single result object from the given Stream.
   68 | 	 * <p>Returns {@code null} if 0 result objects found;
   69 | 	 * throws an exception if more than 1 element found.
   70 | 	 * @param results the result Stream (can be {@code null})
   71 | 	 * @return the single result object, or {@code null} if none
   72 | 	 * @throws IncorrectResultSizeDataAccessException if more than one
   73 | 	 * element has been found in the given Stream
   74 | 	 * @since 6.1
   75 | 	 */
   76 | 	public static <T extends @Nullable Object> @Nullable T singleResult(@Nullable Stream<T> results)
   77 | 			throws IncorrectResultSizeDataAccessException {
   78 | 		if (results == null) {
   79 | 			return null;
   80 | 		}
   81 | 		try (results) {
   82 | 			List<T> resultList = results.limit(2).toList();
   83 | 			if (resultList.size() > 1) {
   84 | 				throw new IncorrectResultSizeDataAccessException(1);
   85 | 			}
   86 | 			return (!resultList.isEmpty() ? resultList.get(0) : null);
   87 | 		}
   88 | 	}
   89 | 
   90 | 	/**
   91 | 	 * Return a single result object from the given Iterator.
   92 | 	 * <p>Returns {@code null} if 0 result objects found;
   93 | 	 * throws an exception if more than 1 element found.
   94 | 	 * @param results the result Iterator (can be {@code null})
   95 | 	 * @return the single result object, or {@code null} if none
   96 | 	 * @throws IncorrectResultSizeDataAccessException if more than one
   97 | 	 * element has been found in the given Iterator
   98 | 	 * @since 6.1
   99 | 	 */
  100 | 	public static <T extends @Nullable Object> @Nullable T singleResult(@Nullable Iterator<T> results)
  101 | 			throws IncorrectResultSizeDataAccessException {
  102 | 		if (results == null) {
  103 | 			return null;
  104 | 		}
  105 | 		T result = (results.hasNext() ? results.next() : null);
  106 | 		if (results.hasNext()) {
  107 | 			throw new IncorrectResultSizeDataAccessException(1);
  108 | 		}
  109 | 		return result;
  110 | 	}
  111 | 
  112 | 	/**
  113 | 	 * Return a single result object from the given Collection.
  114 | 	 * <p>Returns {@code Optional.empty()} if 0 result objects found;
  115 | 	 * throws an exception if more than 1 element found.
  116 | 	 * @param results the result Collection (can be {@code null})
  117 | 	 * @return the single optional result object, or {@code Optional.empty()} if none
  118 | 	 * @throws IncorrectResultSizeDataAccessException if more than one
  119 | 	 * element has been found in the given Collection
  120 | 	 * @since 6.1
  121 | 	 */
  122 | 	public static <T> Optional<T> optionalResult(@Nullable Collection<? extends @Nullable T> results)
  123 | 			throws IncorrectResultSizeDataAccessException {
  124 | 
  125 | 		return Optional.ofNullable(singleResult(results));
  126 | 	}
  127 | 
  128 | 	/**
  129 | 	 * Return a single result object from the given Stream.
  130 | 	 * <p>Returns {@code Optional.empty()} if 0 result objects found;
  131 | 	 * throws an exception if more than 1 element found.
  132 | 	 * @param results the result Stream (can be {@code null})
  133 | 	 * @return the single optional result object, or {@code Optional.empty()} if none
  134 | 	 * @throws IncorrectResultSizeDataAccessException if more than one
  135 | 	 * element has been found in the given Stream
  136 | 	 * @since 6.1
  137 | 	 */
  138 | 	public static <T> Optional<T> optionalResult(@Nullable Stream<? extends @Nullable T> results)
  139 | 			throws IncorrectResultSizeDataAccessException {
  140 | 		return Optional.ofNullable(singleResult(results));
  141 | 	}
  142 | 
  143 | 	/**
  144 | 	 * Return a single result object from the given Iterator.
  145 | 	 * <p>Returns {@code Optional.empty()} if 0 result objects found;
  146 | 	 * throws an exception if more than 1 element found.
  147 | 	 * @param results the result Iterator (can be {@code null})
  148 | 	 * @return the single optional result object, or {@code Optional.empty()} if none
  149 | 	 * @throws IncorrectResultSizeDataAccessException if more than one
  150 | 	 * element has been found in the given Iterator
  151 | 	 * @since 6.1
  152 | 	 */
  153 | 	public static <T> Optional<T> optionalResult(@Nullable Iterator<? extends @Nullable T> results)
  154 | 			throws IncorrectResultSizeDataAccessException {
  155 | 		return Optional.ofNullable(singleResult(results));
  156 | 	}
  157 | 
  158 | 	/**
  159 | 	 * Return a single result object from the given Collection.
  160 | 	 * <p>Throws an exception if 0 or more than 1 element found.
  161 | 	 * @param results the result Collection (can be {@code null}
  162 | 	 * but is not expected to contain {@code null} elements)
  163 | 	 * @return the single result object
  164 | 	 * @throws IncorrectResultSizeDataAccessException if more than one
  165 | 	 * element has been found in the given Collection
  166 | 	 * @throws EmptyResultDataAccessException if no element at all
  167 | 	 * has been found in the given Collection
  168 | 	 */
  169 | 	public static <T extends @Nullable Object> @NonNull T requiredSingleResult(@Nullable Collection<T> results)
  170 | 			throws IncorrectResultSizeDataAccessException {
  171 | 
  172 | 		if (CollectionUtils.isEmpty(results)) {
  173 | 			throw new EmptyResultDataAccessException(1);
  174 | 		}
  175 | 		if (results.size() > 1) {
  176 | 			throw new IncorrectResultSizeDataAccessException(1, results.size());
  177 | 		}
  178 | 		T result = results.iterator().next();
  179 | 		if (result == null) {
  180 | 			throw new TypeMismatchDataAccessException("Result value is null but no null value expected");
  181 | 		}
  182 | 		return result;
  183 | 	}
  184 | 
  185 | 	/**
  186 | 	 * Return a single result object from the given Collection.
  187 | 	 * <p>Throws an exception if 0 or more than 1 element found.
  188 | 	 * @param results the result Collection (can be {@code null}
  189 | 	 * and is also expected to contain {@code null} elements)
  190 | 	 * @return the single result object
  191 | 	 * @throws IncorrectResultSizeDataAccessException if more than one
  192 | 	 * element has been found in the given Collection
  193 | 	 * @throws EmptyResultDataAccessException if no element at all
  194 | 	 * has been found in the given Collection
  195 | 	 * @since 5.0.2
  196 | 	 */
  197 | 	public static <T extends @Nullable Object> T nullableSingleResult(@Nullable Collection<T> results)
  198 | 			throws IncorrectResultSizeDataAccessException {
  199 | 
  200 | 		// This is identical to the requiredSingleResult implementation but differs in the
  201 | 		// semantics of the incoming Collection (which we currently can't formally express)
  202 | 		if (CollectionUtils.isEmpty(results)) {
  203 | 			throw new EmptyResultDataAccessException(1);
  204 | 		}
  205 | 		if (results.size() > 1) {
  206 | 			throw new IncorrectResultSizeDataAccessException(1, results.size());
  207 | 		}
  208 | 		return results.iterator().next();
  209 | 	}
  210 | 
  211 | 	/**
  212 | 	 * Return a unique result object from the given Collection.
  213 | 	 * <p>Returns {@code null} if 0 result objects found;
  214 | 	 * throws an exception if more than 1 instance found.
  215 | 	 * @param results the result Collection (can be {@code null})
  216 | 	 * @return the unique result object, or {@code null} if none
  217 | 	 * @throws IncorrectResultSizeDataAccessException if more than one
  218 | 	 * result object has been found in the given Collection
  219 | 	 * @see org.springframework.util.CollectionUtils#hasUniqueObject
  220 | 	 */
  221 | 	public static <T extends @Nullable Object> @Nullable T uniqueResult(@Nullable Collection<T> results)
  222 | 			throws IncorrectResultSizeDataAccessException {
  223 | 		if (CollectionUtils.isEmpty(results)) {
  224 | 			return null;
  225 | 		}
  226 | 		if (!CollectionUtils.hasUniqueObject(results)) {
  227 | 			throw new IncorrectResultSizeDataAccessException(1, results.size());
  228 | 		}
  229 | 		return results.iterator().next();
  230 | 	}
  231 | 
  232 | 	/**
  233 | 	 * Return a unique result object from the given Collection.
  234 | 	 * <p>Throws an exception if 0 or more than 1 instance found.
  235 | 	 * @param results the result Collection (can be {@code null}
  236 | 	 * but is not expected to contain {@code null} elements)
  237 | 	 * @return the unique result object
  238 | 	 * @throws IncorrectResultSizeDataAccessException if more than one
  239 | 	 * result object has been found in the given Collection
  240 | 	 * @throws EmptyResultDataAccessException if no result object at all
  241 | 	 * has been found in the given Collection
  242 | 	 * @see org.springframework.util.CollectionUtils#hasUniqueObject
  243 | 	 */
  244 | 	public static <T extends @Nullable Object> @NonNull T requiredUniqueResult(@Nullable Collection<T> results)
  245 | 			throws IncorrectResultSizeDataAccessException {
  246 | 		if (CollectionUtils.isEmpty(results)) {
  247 | 			throw new EmptyResultDataAccessException(1);
  248 | 		}
  249 | 		if (!CollectionUtils.hasUniqueObject(results)) {
  250 | 			throw new IncorrectResultSizeDataAccessException(1, results.size());
  251 | 		}
  252 | 		T result = results.iterator().next();
  253 | 		if (result == null) {
  254 | 			throw new TypeMismatchDataAccessException("Result value is null but no null value expected");
  255 | 		}
  256 | 		return result;
  257 | 	}
  258 | 
  259 | 	/**
  260 | 	 * Return a unique result object from the given Collection.
  261 | 	 * Throws an exception if 0 or more than 1 result objects found,
  262 | 	 * of if the unique result object is not convertible to the
  263 | 	 * specified required type.
  264 | 	 * @param results the result Collection (can be {@code null}
  265 | 	 * but is not expected to contain {@code null} elements)
  266 | 	 * @return the unique result object
  267 | 	 * @throws IncorrectResultSizeDataAccessException if more than one
  268 | 	 * result object has been found in the given Collection
  269 | 	 * @throws EmptyResultDataAccessException if no result object
  270 | 	 * at all has been found in the given Collection
  271 | 	 * @throws TypeMismatchDataAccessException if the unique object does
  272 | 	 * not match the specified required type
  273 | 	 */
  274 | 	@SuppressWarnings("unchecked")
  275 | 	public static <T> T objectResult(@Nullable Collection<?> results, @Nullable Class<T> requiredType)
  276 | 			throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {
  277 | 
  278 | 		Object result = requiredUniqueResult(results);
  279 | 		if (requiredType != null && !requiredType.isInstance(result)) {
  280 | 			if (String.class == requiredType) {
  281 | 				result = result.toString();
  282 | 			}
  283 | 			else if (Number.class.isAssignableFrom(requiredType) && result instanceof Number number) {
  284 | 				try {
  285 | 					result = NumberUtils.convertNumberToTargetClass(number, (Class<? extends Number>) requiredType);
  286 | 				}
  287 | 				catch (IllegalArgumentException ex) {
  288 | 					throw new TypeMismatchDataAccessException(ex.getMessage());
  289 | 				}
  290 | 			}
  291 | 			else {
  292 | 				throw new TypeMismatchDataAccessException(
  293 | 						"Result object is of type [" + result.getClass().getName() +
  294 | 						"] and could not be converted to required type [" + requiredType.getName() + "]");
  295 | 			}
  296 | 		}
  297 | 		return (T) result;
  298 | 	}
  299 | 
  300 | 	/**
  301 | 	 * Return a unique int result from the given Collection.
  302 | 	 * Throws an exception if 0 or more than 1 result objects found,
  303 | 	 * of if the unique result object is not convertible to an int.
  304 | 	 * @param results the result Collection (can be {@code null}
  305 | 	 * but is not expected to contain {@code null} elements)
  306 | 	 * @return the unique int result
  307 | 	 * @throws IncorrectResultSizeDataAccessException if more than one
  308 | 	 * result object has been found in the given Collection
  309 | 	 * @throws EmptyResultDataAccessException if no result object
  310 | 	 * at all has been found in the given Collection
  311 | 	 * @throws TypeMismatchDataAccessException if the unique object
  312 | 	 * in the collection is not convertible to an int
  313 | 	 */
  314 | 	public static int intResult(@Nullable Collection<?> results)
  315 | 			throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {
  316 | 
  317 | 		return objectResult(results, Number.class).intValue();
  318 | 	}
  319 | 
  320 | 	/**
  321 | 	 * Return a unique long result from the given Collection.
  322 | 	 * Throws an exception if 0 or more than 1 result objects found,
  323 | 	 * of if the unique result object is not convertible to a long.
  324 | 	 * @param results the result Collection (can be {@code null}
  325 | 	 * but is not expected to contain {@code null} elements)
  326 | 	 * @return the unique long result
  327 | 	 * @throws IncorrectResultSizeDataAccessException if more than one
  328 | 	 * result object has been found in the given Collection
  329 | 	 * @throws EmptyResultDataAccessException if no result object
  330 | 	 * at all has been found in the given Collection
  331 | 	 * @throws TypeMismatchDataAccessException if the unique object
  332 | 	 * in the collection is not convertible to a long
  333 | 	 */
  334 | 	public static long longResult(@Nullable Collection<?> results)
  335 | 			throws IncorrectResultSizeDataAccessException, TypeMismatchDataAccessException {
  336 | 
  337 | 		return objectResult(results, Number.class).longValue();
  338 | 	}
  339 | 
  340 | 
  341 | 	/**
  342 | 	 * Return a translated exception if this is appropriate,
  343 | 	 * otherwise return the given exception as-is.
  344 | 	 * @param rawException an exception that we may wish to translate
  345 | 	 * @param pet the PersistenceExceptionTranslator to use to perform the translation
  346 | 	 * @return a translated persistence exception if translation is possible,
  347 | 	 * or the raw exception if it is not
  348 | 	 */
  349 | 	public static RuntimeException translateIfNecessary(
  350 | 			RuntimeException rawException, PersistenceExceptionTranslator pet) {
  351 | 
  352 | 		Assert.notNull(pet, "PersistenceExceptionTranslator must not be null");
  353 | 		DataAccessException dae = pet.translateExceptionIfPossible(rawException);
  354 | 		return (dae != null ? dae : rawException);
  355 | 	}
  356 | 
  357 | }
```
