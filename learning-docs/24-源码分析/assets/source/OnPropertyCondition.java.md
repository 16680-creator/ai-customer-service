<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-autoconfigure:4.2.0 | path: org/springframework/boot/autoconfigure/condition/OnPropertyCondition.java -->
<!-- 正文引用：[[源码:OnPropertyCondition.java#L起-L止]] ；代码块头部：(src: OnPropertyCondition.java#L起-L止) -->

# OnPropertyCondition.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-autoconfigure:4.2.0` |
| 路径 | `org/springframework/boot/autoconfigure/condition/OnPropertyCondition.java` |
| 行数 | 199 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-autoconfigure/4.2.0/spring-boot-autoconfigure-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:47.941Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.autoconfigure.condition;
    6 | 
    7 | import java.lang.annotation.Annotation;
    8 | import java.util.ArrayList;
    9 | import java.util.Arrays;
   10 | import java.util.List;
   11 | import java.util.stream.Stream;
   12 | 
   13 | import org.jspecify.annotations.Nullable;
   14 | 
   15 | import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
   16 | import org.springframework.context.annotation.Condition;
   17 | import org.springframework.context.annotation.ConditionContext;
   18 | import org.springframework.core.Ordered;
   19 | import org.springframework.core.annotation.AnnotationAttributes;
   20 | import org.springframework.core.annotation.MergedAnnotation;
   21 | import org.springframework.core.annotation.MergedAnnotationPredicates;
   22 | import org.springframework.core.annotation.MergedAnnotations;
   23 | import org.springframework.core.annotation.Order;
   24 | import org.springframework.core.env.PropertyResolver;
   25 | import org.springframework.core.type.AnnotatedTypeMetadata;
   26 | import org.springframework.util.Assert;
   27 | import org.springframework.util.ClassUtils;
   28 | import org.springframework.util.StringUtils;
   29 | 
   30 | /**
   31 |  * {@link Condition} that checks if properties are defined in the environment.
   32 |  *
   33 |  * @author Maciej Walkowiak
   34 |  * @author Phillip Webb
   35 |  * @author Stephane Nicoll
   36 |  * @author Andy Wilkinson
   37 |  * @see ConditionalOnProperty
   38 |  * @see ConditionalOnBooleanProperty
   39 |  */
   40 | @Order(Ordered.HIGHEST_PRECEDENCE + 40)
   41 | class OnPropertyCondition extends SpringBootCondition {
   42 | 
   43 | 	@Override
   44 | 	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
   45 | 		MergedAnnotations mergedAnnotations = metadata.getAnnotations();
   46 | 		List<MergedAnnotation<Annotation>> annotations = stream(mergedAnnotations).toList();
   47 | 		List<ConditionMessage> noMatch = new ArrayList<>();
   48 | 		List<ConditionMessage> match = new ArrayList<>();
   49 | 		for (MergedAnnotation<Annotation> annotation : annotations) {
   50 | 			ConditionOutcome outcome = determineOutcome(annotation, context.getEnvironment());
   51 | 			(outcome.isMatch() ? match : noMatch).add(outcome.getConditionMessage());
   52 | 		}
   53 | 		if (!noMatch.isEmpty()) {
   54 | 			return ConditionOutcome.noMatch(ConditionMessage.of(noMatch));
   55 | 		}
   56 | 		return ConditionOutcome.match(ConditionMessage.of(match));
   57 | 	}
   58 | 
   59 | 	private Stream<MergedAnnotation<Annotation>> stream(MergedAnnotations mergedAnnotations) {
   60 | 		return Stream.concat(stream(mergedAnnotations, ConditionalOnProperty.class, ConditionalOnProperties.class),
   61 | 				stream(mergedAnnotations, ConditionalOnBooleanProperty.class, ConditionalOnBooleanProperties.class));
   62 | 	}
   63 | 
   64 | 	private Stream<MergedAnnotation<Annotation>> stream(MergedAnnotations mergedAnnotations,
   65 | 			Class<? extends Annotation> type, Class<? extends Annotation> containerType) {
   66 | 		return Stream.concat(stream(mergedAnnotations, type), streamRepeated(mergedAnnotations, type, containerType));
   67 | 	}
   68 | 
   69 | 	private Stream<MergedAnnotation<Annotation>> streamRepeated(MergedAnnotations mergedAnnotations,
   70 | 			Class<? extends Annotation> type, Class<? extends Annotation> containerType) {
   71 | 		return stream(mergedAnnotations, containerType).flatMap((container) -> streamRepeated(container, type));
   72 | 	}
   73 | 
   74 | 	@SuppressWarnings("unchecked")
   75 | 	private Stream<MergedAnnotation<Annotation>> streamRepeated(MergedAnnotation<Annotation> container,
   76 | 			Class<? extends Annotation> type) {
   77 | 		MergedAnnotation<? extends Annotation>[] repeated = container.getAnnotationArray(MergedAnnotation.VALUE, type);
   78 | 		return Arrays.stream((MergedAnnotation<Annotation>[]) repeated);
   79 | 	}
   80 | 
   81 | 	private Stream<MergedAnnotation<Annotation>> stream(MergedAnnotations annotations,
   82 | 			Class<? extends Annotation> type) {
   83 | 		return annotations.stream(type.getName())
   84 | 			.filter(MergedAnnotationPredicates.unique(MergedAnnotation::getMetaTypes));
   85 | 	}
   86 | 
   87 | 	private ConditionOutcome determineOutcome(MergedAnnotation<Annotation> annotation, PropertyResolver resolver) {
   88 | 		Class<Annotation> annotationType = annotation.getType();
   89 | 		Spec spec = new Spec(annotationType, annotation.asAnnotationAttributes());
   90 | 		List<String> missingProperties = new ArrayList<>();
   91 | 		List<String> nonMatchingProperties = new ArrayList<>();
   92 | 		spec.collectProperties(resolver, missingProperties, nonMatchingProperties);
   93 | 		if (!missingProperties.isEmpty()) {
   94 | 			return ConditionOutcome.noMatch(ConditionMessage.forCondition(annotationType, spec)
   95 | 				.didNotFind("property", "properties")
   96 | 				.items(Style.QUOTE, missingProperties));
   97 | 		}
   98 | 		if (!nonMatchingProperties.isEmpty()) {
   99 | 			return ConditionOutcome.noMatch(ConditionMessage.forCondition(annotationType, spec)
  100 | 				.found("different value in property", "different value in properties")
  101 | 				.items(Style.QUOTE, nonMatchingProperties));
  102 | 		}
  103 | 		return ConditionOutcome.match(ConditionMessage.forCondition(annotationType, spec).because("matched"));
  104 | 	}
  105 | 
  106 | 	private static class Spec {
  107 | 
  108 | 		private final Class<? extends Annotation> annotationType;
  109 | 
  110 | 		private final String prefix;
  111 | 
  112 | 		private final String[] names;
  113 | 
  114 | 		private final String havingValue;
  115 | 
  116 | 		private final boolean matchIfMissing;
  117 | 
  118 | 		Spec(Class<? extends Annotation> annotationType, AnnotationAttributes annotationAttributes) {
  119 | 			this.annotationType = annotationType;
  120 | 			this.prefix = (!annotationAttributes.containsKey("prefix")) ? "" : getPrefix(annotationAttributes);
  121 | 			this.names = getNames(annotationAttributes);
  122 | 			this.havingValue = getHavingValue(annotationAttributes);
  123 | 			this.matchIfMissing = annotationAttributes.getBoolean("matchIfMissing");
  124 | 		}
  125 | 
  126 | 		private static String getHavingValue(AnnotationAttributes annotationAttributes) {
  127 | 			Object havingValue = annotationAttributes.get("havingValue");
  128 | 			Assert.state(havingValue != null, "'havingValue' must not be null");
  129 | 			return havingValue.toString();
  130 | 		}
  131 | 
  132 | 		private String getPrefix(AnnotationAttributes annotationAttributes) {
  133 | 			String prefix = annotationAttributes.getString("prefix").trim();
  134 | 			if (StringUtils.hasText(prefix) && !prefix.endsWith(".")) {
  135 | 				prefix = prefix + ".";
  136 | 			}
  137 | 			return prefix;
  138 | 		}
  139 | 
  140 | 		private String[] getNames(AnnotationAttributes annotationAttributes) {
  141 | 			String[] value = (String[]) annotationAttributes.get("value");
  142 | 			String[] name = (String[]) annotationAttributes.get("name");
  143 | 			Assert.state(value != null, "'value' must not be null");
  144 | 			Assert.state(name != null, "'name' must not be null");
  145 | 			Assert.state(value.length > 0 || name.length > 0,
  146 | 					() -> "The name or value attribute of @%s must be specified"
  147 | 						.formatted(ClassUtils.getShortName(this.annotationType)));
  148 | 			Assert.state(value.length == 0 || name.length == 0,
  149 | 					() -> "The name and value attributes of @%s are exclusive"
  150 | 						.formatted(ClassUtils.getShortName(this.annotationType)));
  151 | 			return (value.length > 0) ? value : name;
  152 | 		}
  153 | 
  154 | 		private void collectProperties(PropertyResolver resolver, List<String> missing, List<String> nonMatching) {
  155 | 			for (String name : this.names) {
  156 | 				String key = this.prefix + name;
  157 | 				if (resolver.containsProperty(key)) {
  158 | 					if (!isMatch(resolver.getProperty(key), this.havingValue)) {
  159 | 						nonMatching.add(name);
  160 | 					}
  161 | 				}
  162 | 				else {
  163 | 					if (!this.matchIfMissing) {
  164 | 						missing.add(name);
  165 | 					}
  166 | 				}
  167 | 			}
  168 | 		}
  169 | 
  170 | 		private boolean isMatch(@Nullable String value, String requiredValue) {
  171 | 			if (StringUtils.hasLength(requiredValue)) {
  172 | 				return requiredValue.equalsIgnoreCase(value);
  173 | 			}
  174 | 			return !"false".equalsIgnoreCase(value);
  175 | 		}
  176 | 
  177 | 		@Override
  178 | 		public String toString() {
  179 | 			StringBuilder result = new StringBuilder();
  180 | 			result.append("(");
  181 | 			result.append(this.prefix);
  182 | 			if (this.names.length == 1) {
  183 | 				result.append(this.names[0]);
  184 | 			}
  185 | 			else {
  186 | 				result.append("[");
  187 | 				result.append(StringUtils.arrayToCommaDelimitedString(this.names));
  188 | 				result.append("]");
  189 | 			}
  190 | 			if (StringUtils.hasLength(this.havingValue)) {
  191 | 				result.append("=").append(this.havingValue);
  192 | 			}
  193 | 			result.append(")");
  194 | 			return result.toString();
  195 | 		}
  196 | 
  197 | 	}
  198 | 
  199 | }
```
