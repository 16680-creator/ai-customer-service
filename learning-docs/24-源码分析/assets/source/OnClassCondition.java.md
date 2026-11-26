<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-autoconfigure:4.2.0 | path: org/springframework/boot/autoconfigure/condition/OnClassCondition.java -->
<!-- 正文引用：[[源码:OnClassCondition.java#L起-L止]] ；代码块头部：(src: OnClassCondition.java#L起-L止) -->

# OnClassCondition.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-autoconfigure:4.2.0` |
| 路径 | `org/springframework/boot/autoconfigure/condition/OnClassCondition.java` |
| 行数 | 241 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-autoconfigure/4.2.0/spring-boot-autoconfigure-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:47.757Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.autoconfigure.condition;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.Collections;
    9 | import java.util.List;
   10 | 
   11 | import org.jspecify.annotations.Nullable;
   12 | 
   13 | import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
   14 | import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
   15 | import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
   16 | import org.springframework.context.annotation.Condition;
   17 | import org.springframework.context.annotation.ConditionContext;
   18 | import org.springframework.core.Ordered;
   19 | import org.springframework.core.annotation.Order;
   20 | import org.springframework.core.type.AnnotatedTypeMetadata;
   21 | import org.springframework.util.MultiValueMap;
   22 | import org.springframework.util.ReflectionUtils;
   23 | import org.springframework.util.StringUtils;
   24 | 
   25 | /**
   26 |  * {@link Condition} and {@link AutoConfigurationImportFilter} that checks for the
   27 |  * presence or absence of specific classes.
   28 |  *
   29 |  * @author Phillip Webb
   30 |  * @see ConditionalOnClass
   31 |  * @see ConditionalOnMissingClass
   32 |  */
   33 | @Order(Ordered.HIGHEST_PRECEDENCE)
   34 | class OnClassCondition extends FilteringSpringBootCondition {
   35 | 
   36 | 	@Override
   37 | 	protected final @Nullable ConditionOutcome[] getOutcomes(@Nullable String[] autoConfigurationClasses,
   38 | 			AutoConfigurationMetadata autoConfigurationMetadata) {
   39 | 		// Split the work and perform half in a background thread if more than one
   40 | 		// processor is available. Using a single additional thread seems to offer the
   41 | 		// best performance. More threads make things worse.
   42 | 		if (autoConfigurationClasses.length > 1 && Runtime.getRuntime().availableProcessors() > 1) {
   43 | 			return resolveOutcomesThreaded(autoConfigurationClasses, autoConfigurationMetadata);
   44 | 		}
   45 | 		else {
   46 | 			OutcomesResolver outcomesResolver = new StandardOutcomesResolver(autoConfigurationClasses, 0,
   47 | 					autoConfigurationClasses.length, autoConfigurationMetadata, getBeanClassLoader());
   48 | 			return outcomesResolver.resolveOutcomes();
   49 | 		}
   50 | 	}
   51 | 
   52 | 	private @Nullable ConditionOutcome[] resolveOutcomesThreaded(@Nullable String[] autoConfigurationClasses,
   53 | 			AutoConfigurationMetadata autoConfigurationMetadata) {
   54 | 		int split = autoConfigurationClasses.length / 2;
   55 | 		OutcomesResolver firstHalfResolver = createOutcomesResolver(autoConfigurationClasses, 0, split,
   56 | 				autoConfigurationMetadata);
   57 | 		OutcomesResolver secondHalfResolver = new StandardOutcomesResolver(autoConfigurationClasses, split,
   58 | 				autoConfigurationClasses.length, autoConfigurationMetadata, getBeanClassLoader());
   59 | 		@Nullable ConditionOutcome[] secondHalf = secondHalfResolver.resolveOutcomes();
   60 | 		@Nullable ConditionOutcome[] firstHalf = firstHalfResolver.resolveOutcomes();
   61 | 		@Nullable ConditionOutcome[] outcomes = new ConditionOutcome[autoConfigurationClasses.length];
   62 | 		System.arraycopy(firstHalf, 0, outcomes, 0, firstHalf.length);
   63 | 		System.arraycopy(secondHalf, 0, outcomes, split, secondHalf.length);
   64 | 		return outcomes;
   65 | 	}
   66 | 
   67 | 	private OutcomesResolver createOutcomesResolver(@Nullable String[] autoConfigurationClasses, int start, int end,
   68 | 			AutoConfigurationMetadata autoConfigurationMetadata) {
   69 | 		OutcomesResolver outcomesResolver = new StandardOutcomesResolver(autoConfigurationClasses, start, end,
   70 | 				autoConfigurationMetadata, getBeanClassLoader());
   71 | 		return new ThreadedOutcomesResolver(outcomesResolver);
   72 | 	}
   73 | 
   74 | 	@Override
   75 | 	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
   76 | 		ClassLoader classLoader = context.getClassLoader();
   77 | 		ConditionMessage matchMessage = ConditionMessage.empty();
   78 | 		List<String> onClasses = getCandidates(metadata, ConditionalOnClass.class);
   79 | 		if (onClasses != null) {
   80 | 			List<String> missing = filter(onClasses, ClassNameFilter.MISSING, classLoader);
   81 | 			if (!missing.isEmpty()) {
   82 | 				return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
   83 | 					.didNotFind("required class", "required classes")
   84 | 					.items(Style.QUOTE, missing));
   85 | 			}
   86 | 			matchMessage = matchMessage.andCondition(ConditionalOnClass.class)
   87 | 				.found("required class", "required classes")
   88 | 				.items(Style.QUOTE, filter(onClasses, ClassNameFilter.PRESENT, classLoader));
   89 | 		}
   90 | 		List<String> onMissingClasses = getCandidates(metadata, ConditionalOnMissingClass.class);
   91 | 		if (onMissingClasses != null) {
   92 | 			List<String> present = filter(onMissingClasses, ClassNameFilter.PRESENT, classLoader);
   93 | 			if (!present.isEmpty()) {
   94 | 				return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnMissingClass.class)
   95 | 					.found("unwanted class", "unwanted classes")
   96 | 					.items(Style.QUOTE, present));
   97 | 			}
   98 | 			matchMessage = matchMessage.andCondition(ConditionalOnMissingClass.class)
   99 | 				.didNotFind("unwanted class", "unwanted classes")
  100 | 				.items(Style.QUOTE, filter(onMissingClasses, ClassNameFilter.MISSING, classLoader));
  101 | 		}
  102 | 		return ConditionOutcome.match(matchMessage);
  103 | 	}
  104 | 
  105 | 	private @Nullable List<String> getCandidates(AnnotatedTypeMetadata metadata, Class<?> annotationType) {
  106 | 		MultiValueMap<String, @Nullable Object> attributes = metadata
  107 | 			.getAllAnnotationAttributes(annotationType.getName(), true);
  108 | 		if (attributes == null) {
  109 | 			return null;
  110 | 		}
  111 | 		List<String> candidates = new ArrayList<>();
  112 | 		addAll(candidates, attributes.get("value"));
  113 | 		addAll(candidates, attributes.get("name"));
  114 | 		return candidates;
  115 | 	}
  116 | 
  117 | 	private void addAll(List<String> list, @Nullable List<@Nullable Object> itemsToAdd) {
  118 | 		if (itemsToAdd != null) {
  119 | 			for (Object item : itemsToAdd) {
  120 | 				if (item != null) {
  121 | 					Collections.addAll(list, (String[]) item);
  122 | 				}
  123 | 			}
  124 | 		}
  125 | 	}
  126 | 
  127 | 	private interface OutcomesResolver {
  128 | 
  129 | 		@Nullable ConditionOutcome[] resolveOutcomes();
  130 | 
  131 | 	}
  132 | 
  133 | 	private static final class ThreadedOutcomesResolver implements OutcomesResolver {
  134 | 
  135 | 		private final Thread thread;
  136 | 
  137 | 		private volatile @Nullable ConditionOutcome @Nullable [] outcomes;
  138 | 
  139 | 		private volatile @Nullable Throwable failure;
  140 | 
  141 | 		private ThreadedOutcomesResolver(OutcomesResolver outcomesResolver) {
  142 | 			this.thread = new Thread(() -> {
  143 | 				try {
  144 | 					this.outcomes = outcomesResolver.resolveOutcomes();
  145 | 				}
  146 | 				catch (Throwable ex) {
  147 | 					this.failure = ex;
  148 | 				}
  149 | 			});
  150 | 			this.thread.start();
  151 | 		}
  152 | 
  153 | 		@Override
  154 | 		public @Nullable ConditionOutcome[] resolveOutcomes() {
  155 | 			try {
  156 | 				this.thread.join();
  157 | 			}
  158 | 			catch (InterruptedException ex) {
  159 | 				Thread.currentThread().interrupt();
  160 | 			}
  161 | 			Throwable failure = this.failure;
  162 | 			if (failure != null) {
  163 | 				ReflectionUtils.rethrowRuntimeException(failure);
  164 | 			}
  165 | 			@Nullable ConditionOutcome[] outcomes = this.outcomes;
  166 | 			return (outcomes != null) ? outcomes : new ConditionOutcome[0];
  167 | 		}
  168 | 
  169 | 	}
  170 | 
  171 | 	private static final class StandardOutcomesResolver implements OutcomesResolver {
  172 | 
  173 | 		private final @Nullable String[] autoConfigurationClasses;
  174 | 
  175 | 		private final int start;
  176 | 
  177 | 		private final int end;
  178 | 
  179 | 		private final AutoConfigurationMetadata autoConfigurationMetadata;
  180 | 
  181 | 		private final ClassLoader beanClassLoader;
  182 | 
  183 | 		private StandardOutcomesResolver(@Nullable String[] autoConfigurationClasses, int start, int end,
  184 | 				AutoConfigurationMetadata autoConfigurationMetadata, ClassLoader beanClassLoader) {
  185 | 			this.autoConfigurationClasses = autoConfigurationClasses;
  186 | 			this.start = start;
  187 | 			this.end = end;
  188 | 			this.autoConfigurationMetadata = autoConfigurationMetadata;
  189 | 			this.beanClassLoader = beanClassLoader;
  190 | 		}
  191 | 
  192 | 		@Override
  193 | 		public @Nullable ConditionOutcome[] resolveOutcomes() {
  194 | 			return getOutcomes(this.autoConfigurationClasses, this.start, this.end, this.autoConfigurationMetadata);
  195 | 		}
  196 | 
  197 | 		private @Nullable ConditionOutcome[] getOutcomes(@Nullable String[] autoConfigurationClasses, int start,
  198 | 				int end, AutoConfigurationMetadata autoConfigurationMetadata) {
  199 | 			@Nullable ConditionOutcome[] outcomes = new ConditionOutcome[end - start];
  200 | 			for (int i = start; i < end; i++) {
  201 | 				String autoConfigurationClass = autoConfigurationClasses[i];
  202 | 				if (autoConfigurationClass != null) {
  203 | 					String candidates = autoConfigurationMetadata.get(autoConfigurationClass, "ConditionalOnClass");
  204 | 					if (candidates != null) {
  205 | 						outcomes[i - start] = getOutcome(candidates);
  206 | 					}
  207 | 				}
  208 | 			}
  209 | 			return outcomes;
  210 | 		}
  211 | 
  212 | 		private @Nullable ConditionOutcome getOutcome(String candidates) {
  213 | 			try {
  214 | 				if (!candidates.contains(",")) {
  215 | 					return getOutcome(candidates, this.beanClassLoader);
  216 | 				}
  217 | 				for (String candidate : StringUtils.commaDelimitedListToStringArray(candidates)) {
  218 | 					ConditionOutcome outcome = getOutcome(candidate, this.beanClassLoader);
  219 | 					if (outcome != null) {
  220 | 						return outcome;
  221 | 					}
  222 | 				}
  223 | 			}
  224 | 			catch (Exception ex) {
  225 | 				// We'll get another chance later
  226 | 			}
  227 | 			return null;
  228 | 		}
  229 | 
  230 | 		private @Nullable ConditionOutcome getOutcome(String className, ClassLoader classLoader) {
  231 | 			if (ClassNameFilter.MISSING.matches(className, classLoader)) {
  232 | 				return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
  233 | 					.didNotFind("required class")
  234 | 					.items(Style.QUOTE, className));
  235 | 			}
  236 | 			return null;
  237 | 		}
  238 | 
  239 | 	}
  240 | 
  241 | }
```
