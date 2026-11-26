<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-web:7.1.0 | path: org/springframework/web/method/annotation/ExceptionHandlerMethodResolver.java -->
<!-- 正文引用：[[源码:ExceptionHandlerMethodResolver.java#L起-L止]] ；代码块头部：(src: ExceptionHandlerMethodResolver.java#L起-L止) -->

# ExceptionHandlerMethodResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-web:7.1.0` |
| 路径 | `org/springframework/web/method/annotation/ExceptionHandlerMethodResolver.java` |
| 行数 | 296 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-web/7.1.0/spring-web-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:28.314Z |

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
   17 | package org.springframework.web.method.annotation;
   18 | 
   19 | import java.lang.reflect.Method;
   20 | import java.util.ArrayList;
   21 | import java.util.Arrays;
   22 | import java.util.Comparator;
   23 | import java.util.HashMap;
   24 | import java.util.LinkedHashSet;
   25 | import java.util.List;
   26 | import java.util.Map;
   27 | import java.util.Objects;
   28 | import java.util.Set;
   29 | 
   30 | import org.jspecify.annotations.Nullable;
   31 | 
   32 | import org.springframework.core.ExceptionDepthComparator;
   33 | import org.springframework.core.MethodIntrospector;
   34 | import org.springframework.core.annotation.AnnotatedElementUtils;
   35 | import org.springframework.http.InvalidMediaTypeException;
   36 | import org.springframework.http.MediaType;
   37 | import org.springframework.util.Assert;
   38 | import org.springframework.util.ConcurrentLruCache;
   39 | import org.springframework.util.MimeType;
   40 | import org.springframework.util.ReflectionUtils.MethodFilter;
   41 | import org.springframework.web.bind.annotation.ExceptionHandler;
   42 | 
   43 | /**
   44 |  * Discovers {@linkplain ExceptionHandler @ExceptionHandler} methods in a given class,
   45 |  * including all of its superclasses, and helps to resolve a given {@link Exception}
   46 |  * and {@link MediaType} requested by clients to combinations supported by a given {@link Method}.
   47 |  * <p>This resolver will use the exception information declared as {@code @ExceptionHandler}
   48 |  * annotation attributes, or as a method argument as a fallback. This will throw
   49 |  * {@code IllegalStateException} instances if:
   50 |  * <ul>
   51 |  *     <li>No Exception information could be found for a method
   52 |  *     <li>An invalid {@link MediaType} has been declared as {@code @ExceptionHandler} attribute
   53 |  *     <li>Multiple handlers declare the same exception + media type mapping
   54 |  * </ul>
   55 |  *
   56 |  * @author Rossen Stoyanchev
   57 |  * @author Juergen Hoeller
   58 |  * @author Sam Brannen
   59 |  * @author Brian Clozel
   60 |  * @since 3.1
   61 |  */
   62 | public class ExceptionHandlerMethodResolver {
   63 | 
   64 | 	/**
   65 | 	 * A filter for selecting {@code @ExceptionHandler} methods.
   66 | 	 */
   67 | 	private static final MethodFilter EXCEPTION_HANDLER_METHODS = method ->
   68 | 			AnnotatedElementUtils.hasAnnotation(method, ExceptionHandler.class);
   69 | 
   70 | 	private static final ExceptionHandlerMappingInfo NO_MATCHING_EXCEPTION_HANDLER;
   71 | 
   72 | 	static {
   73 | 		try {
   74 | 			NO_MATCHING_EXCEPTION_HANDLER = new ExceptionHandlerMappingInfo(Set.of(), Set.of(),
   75 | 					ExceptionHandlerMethodResolver.class.getDeclaredMethod("noMatchingExceptionHandler"));
   76 | 		}
   77 | 		catch (NoSuchMethodException ex) {
   78 | 			throw new IllegalStateException("Expected method not found: " + ex);
   79 | 		}
   80 | 	}
   81 | 
   82 | 	private final Map<ExceptionMapping, ExceptionHandlerMappingInfo> mappedMethods = new HashMap<>(16);
   83 | 
   84 | 	private final ConcurrentLruCache<ExceptionMapping, ExceptionHandlerMappingInfo> lookupCache = new ConcurrentLruCache<>(24,
   85 | 			cacheKey -> getMappedMethod(cacheKey.exceptionType(), cacheKey.mediaType()));
   86 | 
   87 | 
   88 | 	/**
   89 | 	 * A constructor that finds {@link ExceptionHandler} methods in the given type.
   90 | 	 * @param handlerType the type to introspect
   91 | 	 * @throws IllegalStateException in case of invalid or ambiguous exception mapping declarations
   92 | 	 */
   93 | 	public ExceptionHandlerMethodResolver(Class<?> handlerType) {
   94 | 		for (Method method : MethodIntrospector.selectMethods(handlerType, EXCEPTION_HANDLER_METHODS)) {
   95 | 			ExceptionHandlerMappingInfo mappingInfo = detectExceptionMappings(method);
   96 | 			for (Class<? extends Throwable> exceptionType : mappingInfo.getExceptionTypes()) {
   97 | 				for (MediaType producibleType : mappingInfo.getProducibleTypes()) {
   98 | 					addExceptionMapping(new ExceptionMapping(exceptionType, producibleType), mappingInfo);
   99 | 				}
  100 | 				if (mappingInfo.getProducibleTypes().isEmpty()) {
  101 | 					addExceptionMapping(new ExceptionMapping(exceptionType, MediaType.ALL), mappingInfo);
  102 | 				}
  103 | 			}
  104 | 		}
  105 | 	}
  106 | 
  107 | 
  108 | 	/**
  109 | 	 * Extract exception mappings from the {@code @ExceptionHandler} annotation first,
  110 | 	 * and then as a fallback from the method signature itself.
  111 | 	 */
  112 | 	@SuppressWarnings("unchecked")
  113 | 	private ExceptionHandlerMappingInfo detectExceptionMappings(Method method) {
  114 | 		ExceptionHandler exceptionHandler = readExceptionHandlerAnnotation(method);
  115 | 		List<Class<? extends Throwable>> exceptions = new ArrayList<>(Arrays.asList(exceptionHandler.exception()));
  116 | 		if (exceptions.isEmpty()) {
  117 | 			for (Class<?> paramType : method.getParameterTypes()) {
  118 | 				if (Throwable.class.isAssignableFrom(paramType)) {
  119 | 					exceptions.add((Class<? extends Throwable>) paramType);
  120 | 				}
  121 | 			}
  122 | 		}
  123 | 		if (exceptions.isEmpty()) {
  124 | 			throw new IllegalStateException("No exception types mapped to " + method);
  125 | 		}
  126 | 		Set<MediaType> mediaTypes = new LinkedHashSet<>();
  127 | 		for (String mediaType : exceptionHandler.produces()) {
  128 | 			try {
  129 | 				mediaTypes.add(MediaType.parseMediaType(mediaType));
  130 | 			}
  131 | 			catch (InvalidMediaTypeException exc) {
  132 | 				throw new IllegalStateException("Invalid media type [" + mediaType + "] declared on @ExceptionHandler for " + method, exc);
  133 | 			}
  134 | 		}
  135 | 		return new ExceptionHandlerMappingInfo(Set.copyOf(exceptions), mediaTypes, method);
  136 | 	}
  137 | 
  138 | 	private ExceptionHandler readExceptionHandlerAnnotation(Method method) {
  139 | 		ExceptionHandler ann = AnnotatedElementUtils.findMergedAnnotation(method, ExceptionHandler.class);
  140 | 		Assert.state(ann != null, "No ExceptionHandler annotation");
  141 | 		return ann;
  142 | 	}
  143 | 
  144 | 	private void addExceptionMapping(ExceptionMapping mapping, ExceptionHandlerMappingInfo mappingInfo) {
  145 | 		ExceptionHandlerMappingInfo oldMapping = this.mappedMethods.put(mapping, mappingInfo);
  146 | 		if (oldMapping != null && !oldMapping.getHandlerMethod().equals(mappingInfo.getHandlerMethod())) {
  147 | 			throw new IllegalStateException("Ambiguous @ExceptionHandler method mapped for [" +
  148 | 					mapping + "]: {" + oldMapping.getHandlerMethod() + ", " + mappingInfo.getHandlerMethod() + "}");
  149 | 		}
  150 | 	}
  151 | 
  152 | 	/**
  153 | 	 * Whether the contained type has any exception mappings.
  154 | 	 */
  155 | 	public boolean hasExceptionMappings() {
  156 | 		return !this.mappedMethods.isEmpty();
  157 | 	}
  158 | 
  159 | 	/**
  160 | 	 * Find a {@link Method} to handle the given exception.
  161 | 	 * <p>Uses {@link ExceptionDepthComparator} if more than one match is found.
  162 | 	 * @param exception the exception
  163 | 	 * @return a Method to handle the exception, or {@code null} if none found
  164 | 	 */
  165 | 	public @Nullable Method resolveMethod(Exception exception) {
  166 | 		ExceptionHandlerMappingInfo mappingInfo = resolveExceptionMapping(exception, MediaType.ALL);
  167 | 		return (mappingInfo != null) ? mappingInfo.getHandlerMethod() : null;
  168 | 	}
  169 | 
  170 | 	/**
  171 | 	 * Find a {@link Method} to handle the given Throwable.
  172 | 	 * <p>Uses {@link ExceptionDepthComparator} if more than one match is found.
  173 | 	 * @param exception the exception
  174 | 	 * @return a Method to handle the exception, or {@code null} if none found
  175 | 	 * @since 5.0
  176 | 	 */
  177 | 	public @Nullable Method resolveMethodByThrowable(Throwable exception) {
  178 | 		ExceptionHandlerMappingInfo mappingInfo = resolveExceptionMapping(exception, MediaType.ALL);
  179 | 		return (mappingInfo != null) ? mappingInfo.getHandlerMethod() : null;
  180 | 	}
  181 | 
  182 | 	/**
  183 | 	 * Find a {@link Method} to handle the given Throwable for the requested {@link MediaType}.
  184 | 	 * <p>Uses {@link ExceptionDepthComparator} and {@link MediaType#isMoreSpecific(MimeType)}
  185 | 	 * if more than one match is found.
  186 | 	 * @param exception the exception
  187 | 	 * @param mediaType the media type requested by the HTTP client
  188 | 	 * @return a Method to handle the exception, or {@code null} if none found
  189 | 	 * @since 6.2
  190 | 	 */
  191 | 	public @Nullable ExceptionHandlerMappingInfo resolveExceptionMapping(Throwable exception, MediaType mediaType) {
  192 | 		ExceptionHandlerMappingInfo mappingInfo = resolveExceptionMappingByExceptionType(exception.getClass(), mediaType);
  193 | 		if (mappingInfo == null) {
  194 | 			Throwable cause = exception.getCause();
  195 | 			if (cause != null) {
  196 | 				mappingInfo = resolveExceptionMapping(cause, mediaType);
  197 | 			}
  198 | 		}
  199 | 		return mappingInfo;
  200 | 	}
  201 | 
  202 | 	/**
  203 | 	 * Find a {@link Method} to handle the given exception type. This can be
  204 | 	 * useful if an {@link Exception} instance is not available (for example, for tools).
  205 | 	 * <p>Uses {@link ExceptionDepthComparator} if more than one match is found.
  206 | 	 * @param exceptionType the exception type
  207 | 	 * @return a Method to handle the exception, or {@code null} if none found
  208 | 	 */
  209 | 	public @Nullable Method resolveMethodByExceptionType(Class<? extends Throwable> exceptionType) {
  210 | 		ExceptionHandlerMappingInfo mappingInfo = resolveExceptionMappingByExceptionType(exceptionType, MediaType.ALL);
  211 | 		return (mappingInfo != null) ? mappingInfo.getHandlerMethod() : null;
  212 | 	}
  213 | 
  214 | 	/**
  215 | 	 * Find a {@link Method} to handle the given exception type and media type.
  216 | 	 * This can be useful if an {@link Exception} instance is not available (for example, for tools).
  217 | 	 * @param exceptionType the exception type
  218 | 	 * @param mediaType the media type requested by the HTTP client
  219 | 	 * @return a Method to handle the exception, or {@code null} if none found
  220 | 	 */
  221 | 	public @Nullable ExceptionHandlerMappingInfo resolveExceptionMappingByExceptionType(Class<? extends Throwable> exceptionType, MediaType mediaType) {
  222 | 		ExceptionHandlerMappingInfo mappingInfo = this.lookupCache.get(new ExceptionMapping(exceptionType, mediaType));
  223 | 		return (mappingInfo != NO_MATCHING_EXCEPTION_HANDLER ? mappingInfo : null);
  224 | 	}
  225 | 
  226 | 	/**
  227 | 	 * Return the {@link Method} mapped to the given exception type, or
  228 | 	 * {@link #NO_MATCHING_EXCEPTION_HANDLER} if none.
  229 | 	 */
  230 | 	private ExceptionHandlerMappingInfo getMappedMethod(Class<? extends Throwable> exceptionType, MediaType mediaType) {
  231 | 		List<ExceptionMapping> matches = new ArrayList<>();
  232 | 		for (ExceptionMapping mappingInfo : this.mappedMethods.keySet()) {
  233 | 			if (mappingInfo.exceptionType().isAssignableFrom(exceptionType) && mappingInfo.mediaType().isCompatibleWith(mediaType)) {
  234 | 				matches.add(mappingInfo);
  235 | 			}
  236 | 		}
  237 | 		if (!matches.isEmpty()) {
  238 | 			if (matches.size() > 1) {
  239 | 				matches.sort(new ExceptionMappingComparator(exceptionType, mediaType));
  240 | 			}
  241 | 			return Objects.requireNonNull(this.mappedMethods.get(matches.get(0)));
  242 | 		}
  243 | 		else {
  244 | 			return NO_MATCHING_EXCEPTION_HANDLER;
  245 | 		}
  246 | 	}
  247 | 
  248 | 	/**
  249 | 	 * For the {@link #NO_MATCHING_EXCEPTION_HANDLER} constant.
  250 |  	 */
  251 | 	@SuppressWarnings("unused")
  252 | 	private void noMatchingExceptionHandler() {
  253 | 	}
  254 | 
  255 | 	private record ExceptionMapping(Class<? extends Throwable> exceptionType, MediaType mediaType) {
  256 | 
  257 | 		@Override
  258 | 		public String toString() {
  259 | 			return "ExceptionHandler{" +
  260 | 					"exceptionType=" + this.exceptionType.getCanonicalName() +
  261 | 					", mediaType=" + this.mediaType +
  262 | 					'}';
  263 | 		}
  264 | 	}
  265 | 
  266 | 	private static class ExceptionMappingComparator implements Comparator<ExceptionMapping> {
  267 | 
  268 | 		private final ExceptionDepthComparator exceptionDepthComparator;
  269 | 
  270 | 		private final MediaType mediaType;
  271 | 
  272 | 		public ExceptionMappingComparator(Class<? extends Throwable> exceptionType, MediaType mediaType) {
  273 | 			this.exceptionDepthComparator = new ExceptionDepthComparator(exceptionType);
  274 | 			this.mediaType = mediaType;
  275 | 		}
  276 | 
  277 | 		@Override
  278 | 		public int compare(ExceptionMapping o1, ExceptionMapping o2) {
  279 | 			int result = this.exceptionDepthComparator.compare(o1.exceptionType(), o2.exceptionType());
  280 | 			if (result != 0) {
  281 | 				return result;
  282 | 			}
  283 | 			if(o1.mediaType.equals(this.mediaType)) {
  284 | 				return -1;
  285 | 			}
  286 | 			if(o2.mediaType.equals(this.mediaType)) {
  287 | 				return 1;
  288 | 			}
  289 | 			if (o1.mediaType.equals(o2.mediaType)) {
  290 | 				return 0;
  291 | 			}
  292 | 			return (o1.mediaType.isMoreSpecific(o2.mediaType)) ? -1 : 1;
  293 | 		}
  294 | 	}
  295 | 
  296 | }
```
