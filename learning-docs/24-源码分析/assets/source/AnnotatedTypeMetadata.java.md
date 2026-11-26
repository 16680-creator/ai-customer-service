<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-core:7.1.0 | path: org/springframework/core/type/AnnotatedTypeMetadata.java -->
<!-- 正文引用：[[源码:AnnotatedTypeMetadata.java#L起-L止]] ；代码块头部：(src: AnnotatedTypeMetadata.java#L起-L止) -->

# AnnotatedTypeMetadata.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-core:7.1.0` |
| 路径 | `org/springframework/core/type/AnnotatedTypeMetadata.java` |
| 行数 | 301 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-core/7.1.0/spring-core-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:12.947Z |

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
   17 | package org.springframework.core.type;
   18 | 
   19 | import java.lang.annotation.Annotation;
   20 | import java.util.Comparator;
   21 | import java.util.LinkedHashSet;
   22 | import java.util.Map;
   23 | import java.util.Set;
   24 | import java.util.function.Predicate;
   25 | import java.util.stream.Collectors;
   26 | import java.util.stream.Stream;
   27 | 
   28 | import org.jspecify.annotations.Nullable;
   29 | 
   30 | import org.springframework.core.annotation.AnnotationAttributes;
   31 | import org.springframework.core.annotation.MergedAnnotation;
   32 | import org.springframework.core.annotation.MergedAnnotation.Adapt;
   33 | import org.springframework.core.annotation.MergedAnnotationCollectors;
   34 | import org.springframework.core.annotation.MergedAnnotationPredicates;
   35 | import org.springframework.core.annotation.MergedAnnotationSelectors;
   36 | import org.springframework.core.annotation.MergedAnnotations;
   37 | import org.springframework.util.MultiValueMap;
   38 | 
   39 | /**
   40 |  * Defines access to the annotations on a specific {@link AnnotationMetadata class}
   41 |  * or {@link MethodMetadata method}, in a form that does not necessarily require
   42 |  * class loading of the types being inspected. Note, however, that classes for
   43 |  * encountered annotations will be loaded.
   44 |  *
   45 |  * <p><strong>WARNING</strong>: If an annotation cannot be loaded because one of
   46 |  * its attributes references a {@link Class} or {@link Enum}
   47 |  * {@linkplain TypeNotPresentException that is not present in the classpath}, that
   48 |  * annotation will not be accessible via the {@code AnnotatedTypeMetadata} API.
   49 |  * To assist with diagnosing such scenarios, you can set the log level for
   50 |  * {@code "org.springframework.core.annotation.MergedAnnotation"} to {@code DEBUG},
   51 |  * {@code INFO}, or {@code WARN}.
   52 |  *
   53 |  * @author Juergen Hoeller
   54 |  * @author Mark Fisher
   55 |  * @author Mark Pollack
   56 |  * @author Chris Beams
   57 |  * @author Phillip Webb
   58 |  * @author Sam Brannen
   59 |  * @since 4.0
   60 |  * @see AnnotationMetadata
   61 |  * @see MethodMetadata
   62 |  */
   63 | public interface AnnotatedTypeMetadata {
   64 | 
   65 | 	/**
   66 | 	 * Get annotation details based on the direct annotations and meta-annotations
   67 | 	 * of the underlying element.
   68 | 	 * @return merged annotations based on the direct annotations and meta-annotations
   69 | 	 * @since 5.2
   70 | 	 */
   71 | 	MergedAnnotations getAnnotations();
   72 | 
   73 | 	/**
   74 | 	 * Determine whether the underlying element has an annotation or meta-annotation
   75 | 	 * of the given type defined.
   76 | 	 * <p>If this method returns {@code true}, then
   77 | 	 * {@link #getAnnotationAttributes} will return a non-null Map.
   78 | 	 * @param annotationName the fully-qualified class name of the annotation
   79 | 	 * type to look for
   80 | 	 * @return whether a matching annotation is defined
   81 | 	 */
   82 | 	default boolean isAnnotated(String annotationName) {
   83 | 		return getAnnotations().isPresent(annotationName);
   84 | 	}
   85 | 
   86 | 	/**
   87 | 	 * Retrieve the attributes of the annotation of the given type, if any (i.e. if
   88 | 	 * defined on the underlying element, as direct annotation or meta-annotation).
   89 | 	 * <p>{@link org.springframework.core.annotation.AliasFor @AliasFor} semantics
   90 | 	 * are fully supported, both within a single annotation and within annotation
   91 | 	 * hierarchies.
   92 | 	 * @param annotationName the fully-qualified class name of the annotation
   93 | 	 * type to look for
   94 | 	 * @return a {@link Map} of attributes, with each annotation attribute name
   95 | 	 * as map key (for example, "location") and the attribute's value as map value; or
   96 | 	 * {@code null} if no matching annotation is found
   97 | 	 */
   98 | 	default @Nullable Map<String, @Nullable Object> getAnnotationAttributes(String annotationName) {
   99 | 		return getAnnotationAttributes(annotationName, false);
  100 | 	}
  101 | 
  102 | 	/**
  103 | 	 * Retrieve the attributes of the annotation of the given type, if any (i.e. if
  104 | 	 * defined on the underlying element, as direct annotation or meta-annotation).
  105 | 	 * <p>{@link org.springframework.core.annotation.AliasFor @AliasFor} semantics
  106 | 	 * are fully supported, both within a single annotation and within annotation
  107 | 	 * hierarchies.
  108 | 	 * @param annotationName the fully-qualified class name of the annotation
  109 | 	 * type to look for
  110 | 	 * @param classValuesAsString whether to convert class references to String
  111 | 	 * class names for exposure as values in the returned Map, instead of Class
  112 | 	 * references which might potentially have to be loaded first
  113 | 	 * @return a {@link Map} of attributes, with each annotation attribute name
  114 | 	 * as map key (for example, "location") and the attribute's value as map value; or
  115 | 	 * {@code null} if no matching annotation is found
  116 | 	 */
  117 | 	default @Nullable Map<String, @Nullable Object> getAnnotationAttributes(String annotationName,
  118 | 			boolean classValuesAsString) {
  119 | 
  120 | 		MergedAnnotation<Annotation> annotation = getAnnotations().get(annotationName,
  121 | 				null, MergedAnnotationSelectors.firstDirectlyDeclared());
  122 | 		if (!annotation.isPresent()) {
  123 | 			return null;
  124 | 		}
  125 | 		return annotation.asAnnotationAttributes(Adapt.values(classValuesAsString, true));
  126 | 	}
  127 | 
  128 | 	/**
  129 | 	 * Retrieve all attributes of all annotations of the given type, if any (i.e. if
  130 | 	 * defined on the underlying element, as direct annotation or meta-annotation).
  131 | 	 * <p>Note: this method does <i>not</i> take attribute overrides on composed
  132 | 	 * annotations into account.
  133 | 	 * @param annotationName the fully-qualified class name of the annotation
  134 | 	 * type to look for
  135 | 	 * @return a {@link MultiValueMap} of attributes, with each annotation attribute
  136 | 	 * name as map key (for example, "location") and a list of the attribute's values as
  137 | 	 * map value; or {@code null} if no matching annotation is found
  138 | 	 * @see #getAllAnnotationAttributes(String, boolean)
  139 | 	 */
  140 | 	default @Nullable MultiValueMap<String, @Nullable Object> getAllAnnotationAttributes(String annotationName) {
  141 | 		return getAllAnnotationAttributes(annotationName, false);
  142 | 	}
  143 | 
  144 | 	/**
  145 | 	 * Retrieve all attributes of all annotations of the given type, if any (i.e. if
  146 | 	 * defined on the underlying element, as direct annotation or meta-annotation).
  147 | 	 * <p>Note: this method does <i>not</i> take attribute overrides on composed
  148 | 	 * annotations into account.
  149 | 	 * @param annotationName the fully-qualified class name of the annotation
  150 | 	 * type to look for
  151 | 	 * @param classValuesAsString whether to convert class references to String
  152 | 	 * class names for exposure as values in the returned Map, instead of Class
  153 | 	 * references which might potentially have to be loaded first
  154 | 	 * @return a {@link MultiValueMap} of attributes, with each annotation attribute
  155 | 	 * name as map key (for example, "location") and a list of the attribute's values as
  156 | 	 * map value; or {@code null} if no matching annotation is found
  157 | 	 * @see #getAllAnnotationAttributes(String)
  158 | 	 */
  159 | 	default @Nullable MultiValueMap<String, @Nullable Object> getAllAnnotationAttributes(
  160 | 			String annotationName, boolean classValuesAsString) {
  161 | 
  162 | 		Adapt[] adaptations = Adapt.values(classValuesAsString, true);
  163 | 		return getAnnotations().stream(annotationName)
  164 | 				.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes))
  165 | 				.map(MergedAnnotation::withNonMergedAttributes)
  166 | 				.collect(MergedAnnotationCollectors.toMultiValueMap(
  167 | 						(MultiValueMap<String, @Nullable Object> map) -> (map.isEmpty() ? null : map), adaptations));
  168 | 	}
  169 | 
  170 | 	/**
  171 | 	 * Retrieve all <em>repeatable annotations</em> of the given type within the
  172 | 	 * annotation hierarchy <em>above</em> the underlying element (as direct
  173 | 	 * annotation or meta-annotation); and for each annotation found, merge that
  174 | 	 * annotation's attributes with <em>matching</em> attributes from annotations
  175 | 	 * in lower levels of the annotation hierarchy and store the results in an
  176 | 	 * instance of {@link AnnotationAttributes}.
  177 | 	 * <p>{@link org.springframework.core.annotation.AliasFor @AliasFor} semantics
  178 | 	 * are fully supported, both within a single annotation and within annotation
  179 | 	 * hierarchies.
  180 | 	 * @param annotationType the annotation type to find
  181 | 	 * @param containerType the type of the container that holds the annotations
  182 | 	 * @param classValuesAsString whether to convert class references to {@code String}
  183 | 	 * class names for exposure as values in the returned {@code AnnotationAttributes},
  184 | 	 * instead of {@code Class} references which might potentially have to be loaded
  185 | 	 * first
  186 | 	 * @return the set of all merged repeatable {@code AnnotationAttributes} found,
  187 | 	 * or an empty set if none were found
  188 | 	 * @since 6.1
  189 | 	 * @see #getMergedRepeatableAnnotationAttributes(Class, Class, boolean, boolean)
  190 | 	 * @see #getMergedRepeatableAnnotationAttributes(Class, Class, Predicate, boolean, boolean)
  191 | 	 */
  192 | 	default Set<AnnotationAttributes> getMergedRepeatableAnnotationAttributes(
  193 | 			Class<? extends Annotation> annotationType, Class<? extends Annotation> containerType,
  194 | 			boolean classValuesAsString) {
  195 | 
  196 | 		return getMergedRepeatableAnnotationAttributes(annotationType, containerType, classValuesAsString, false);
  197 | 	}
  198 | 
  199 | 	/**
  200 | 	 * Retrieve all <em>repeatable annotations</em> of the given type within the
  201 | 	 * annotation hierarchy <em>above</em> the underlying element (as direct
  202 | 	 * annotation or meta-annotation); and for each annotation found, merge that
  203 | 	 * annotation's attributes with <em>matching</em> attributes from annotations
  204 | 	 * in lower levels of the annotation hierarchy and store the results in an
  205 | 	 * instance of {@link AnnotationAttributes}.
  206 | 	 * <p>{@link org.springframework.core.annotation.AliasFor @AliasFor} semantics
  207 | 	 * are fully supported, both within a single annotation and within annotation
  208 | 	 * hierarchies.
  209 | 	 * <p>If the {@code sortByReversedMetaDistance} flag is set to {@code true},
  210 | 	 * the results will be sorted in {@link Comparator#reversed() reversed} order
  211 | 	 * based on each annotation's {@linkplain MergedAnnotation#getDistance()
  212 | 	 * meta distance}, which effectively orders meta-annotations before annotations
  213 | 	 * that are declared directly on the underlying element.
  214 | 	 * @param annotationType the annotation type to find
  215 | 	 * @param containerType the type of the container that holds the annotations
  216 | 	 * @param classValuesAsString whether to convert class references to {@code String}
  217 | 	 * class names for exposure as values in the returned {@code AnnotationAttributes},
  218 | 	 * instead of {@code Class} references which might potentially have to be loaded
  219 | 	 * first
  220 | 	 * @param sortByReversedMetaDistance {@code true} if the results should be
  221 | 	 * sorted in reversed order based on each annotation's meta distance
  222 | 	 * @return the set of all merged repeatable {@code AnnotationAttributes} found,
  223 | 	 * or an empty set if none were found
  224 | 	 * @since 6.1
  225 | 	 * @see #getMergedRepeatableAnnotationAttributes(Class, Class, boolean)
  226 | 	 * @see #getMergedRepeatableAnnotationAttributes(Class, Class, Predicate, boolean, boolean)
  227 | 	 */
  228 | 	default Set<AnnotationAttributes> getMergedRepeatableAnnotationAttributes(
  229 | 			Class<? extends Annotation> annotationType, Class<? extends Annotation> containerType,
  230 | 			boolean classValuesAsString, boolean sortByReversedMetaDistance) {
  231 | 
  232 | 		return getMergedRepeatableAnnotationAttributes(annotationType, containerType,
  233 | 				mergedAnnotation -> true, classValuesAsString, sortByReversedMetaDistance);
  234 | 	}
  235 | 
  236 | 	/**
  237 | 	 * Retrieve all <em>repeatable annotations</em> of the given type within the
  238 | 	 * annotation hierarchy <em>above</em> the underlying element (as direct
  239 | 	 * annotation or meta-annotation); and for each annotation found, merge that
  240 | 	 * annotation's attributes with <em>matching</em> attributes from annotations
  241 | 	 * in lower levels of the annotation hierarchy and store the results in an
  242 | 	 * instance of {@link AnnotationAttributes}.
  243 | 	 * <p>{@link org.springframework.core.annotation.AliasFor @AliasFor} semantics
  244 | 	 * are fully supported, both within a single annotation and within annotation
  245 | 	 * hierarchies.
  246 | 	 * <p>The supplied {@link Predicate} will be used to filter the results. For
  247 | 	 * example, supply {@code mergedAnnotation -> true} to include all annotations
  248 | 	 * in the results; supply {@code MergedAnnotation::isDirectlyPresent} to limit
  249 | 	 * the results to directly declared annotations, etc.
  250 | 	 * <p>If the {@code sortByReversedMetaDistance} flag is set to {@code true},
  251 | 	 * the results will be sorted in {@link Comparator#reversed() reversed} order
  252 | 	 * based on each annotation's {@linkplain MergedAnnotation#getDistance()
  253 | 	 * meta distance}, which effectively orders meta-annotations before annotations
  254 | 	 * that are declared directly on the underlying element.
  255 | 	 * @param annotationType the annotation type to find
  256 | 	 * @param containerType the type of the container that holds the annotations
  257 | 	 * @param predicate a {@code Predicate} to apply to each {@code MergedAnnotation}
  258 | 	 * to determine if it should be included in the results
  259 | 	 * @param classValuesAsString whether to convert class references to {@code String}
  260 | 	 * class names for exposure as values in the returned {@code AnnotationAttributes},
  261 | 	 * instead of {@code Class} references which might potentially have to be loaded
  262 | 	 * first
  263 | 	 * @param sortByReversedMetaDistance {@code true} if the results should be
  264 | 	 * sorted in reversed order based on each annotation's meta distance
  265 | 	 * @return the set of all merged repeatable {@code AnnotationAttributes} found,
  266 | 	 * or an empty set if none were found
  267 | 	 * @since 6.1.2
  268 | 	 * @see #getMergedRepeatableAnnotationAttributes(Class, Class, boolean)
  269 | 	 * @see #getMergedRepeatableAnnotationAttributes(Class, Class, boolean, boolean)
  270 | 	 */
  271 | 	default Set<AnnotationAttributes> getMergedRepeatableAnnotationAttributes(
  272 | 			Class<? extends Annotation> annotationType, Class<? extends Annotation> containerType,
  273 | 			Predicate<MergedAnnotation<? extends Annotation>> predicate, boolean classValuesAsString,
  274 | 			boolean sortByReversedMetaDistance) {
  275 | 
  276 | 		Stream<MergedAnnotation<Annotation>> stream = getAnnotations().stream()
  277 | 				.filter(predicate)
  278 | 				.filter(MergedAnnotationPredicates.typeIn(containerType, annotationType));
  279 | 
  280 | 		if (sortByReversedMetaDistance) {
  281 | 			stream = stream.sorted(reversedMetaDistance());
  282 | 		}
  283 | 
  284 | 		Adapt[] adaptations = Adapt.values(classValuesAsString, true);
  285 | 		return stream
  286 | 				.map(annotation -> annotation.asAnnotationAttributes(adaptations))
  287 | 				.flatMap(attributes -> {
  288 | 					if (containerType.equals(attributes.annotationType())) {
  289 | 						return Stream.of(attributes.getAnnotationArray(MergedAnnotation.VALUE));
  290 | 					}
  291 | 					return Stream.of(attributes);
  292 | 				})
  293 | 				.collect(Collectors.toCollection(LinkedHashSet::new));
  294 | 	}
  295 | 
  296 | 
  297 | 	private static Comparator<MergedAnnotation<Annotation>> reversedMetaDistance() {
  298 | 		return Comparator.<MergedAnnotation<Annotation>> comparingInt(MergedAnnotation::getDistance).reversed();
  299 | 	}
  300 | 
  301 | }
```
