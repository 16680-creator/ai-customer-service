<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-autoconfigure:4.2.0 | path: org/springframework/boot/autoconfigure/condition/SpringBootCondition.java -->
<!-- 正文引用：[[源码:SpringBootCondition.java#L起-L止]] ；代码块头部：(src: SpringBootCondition.java#L起-L止) -->

# SpringBootCondition.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-autoconfigure:4.2.0` |
| 路径 | `org/springframework/boot/autoconfigure/condition/SpringBootCondition.java` |
| 行数 | 136 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-autoconfigure/4.2.0/spring-boot-autoconfigure-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:48.034Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.autoconfigure.condition;
    6 | 
    7 | import org.apache.commons.logging.Log;
    8 | import org.apache.commons.logging.LogFactory;
    9 | 
   10 | import org.springframework.context.annotation.Condition;
   11 | import org.springframework.context.annotation.ConditionContext;
   12 | import org.springframework.core.type.AnnotatedTypeMetadata;
   13 | import org.springframework.core.type.AnnotationMetadata;
   14 | import org.springframework.core.type.ClassMetadata;
   15 | import org.springframework.core.type.MethodMetadata;
   16 | import org.springframework.util.ClassUtils;
   17 | import org.springframework.util.StringUtils;
   18 | 
   19 | /**
   20 |  * Base of all {@link Condition} implementations used with Spring Boot. Provides sensible
   21 |  * logging to help the user diagnose what classes are loaded.
   22 |  *
   23 |  * @author Phillip Webb
   24 |  * @author Greg Turnquist
   25 |  * @since 1.0.0
   26 |  */
   27 | public abstract class SpringBootCondition implements Condition {
   28 | 
   29 | 	private final Log logger = LogFactory.getLog(getClass());
   30 | 
   31 | 	@Override
   32 | 	public final boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
   33 | 		String classOrMethodName = getClassOrMethodName(metadata);
   34 | 		try {
   35 | 			ConditionOutcome outcome = getMatchOutcome(context, metadata);
   36 | 			logOutcome(classOrMethodName, outcome);
   37 | 			recordEvaluation(context, classOrMethodName, outcome);
   38 | 			return outcome.isMatch();
   39 | 		}
   40 | 		catch (NoClassDefFoundError ex) {
   41 | 			throw new IllegalStateException("Could not evaluate condition on " + classOrMethodName + " due to "
   42 | 					+ ex.getMessage() + " not found. Make sure your own configuration does not rely on "
   43 | 					+ "that class. This can also happen if you are "
   44 | 					+ "@ComponentScanning a springframework package (e.g. if you "
   45 | 					+ "put a @ComponentScan in the default package by mistake)", ex);
   46 | 		}
   47 | 		catch (RuntimeException ex) {
   48 | 			throw new IllegalStateException("Error processing condition on " + getName(metadata), ex);
   49 | 		}
   50 | 	}
   51 | 
   52 | 	private String getName(AnnotatedTypeMetadata metadata) {
   53 | 		if (metadata instanceof AnnotationMetadata annotationMetadata) {
   54 | 			return annotationMetadata.getClassName();
   55 | 		}
   56 | 		if (metadata instanceof MethodMetadata methodMetadata) {
   57 | 			return methodMetadata.getDeclaringClassName() + "." + methodMetadata.getMethodName();
   58 | 		}
   59 | 		return metadata.toString();
   60 | 	}
   61 | 
   62 | 	private static String getClassOrMethodName(AnnotatedTypeMetadata metadata) {
   63 | 		if (metadata instanceof ClassMetadata classMetadata) {
   64 | 			return classMetadata.getClassName();
   65 | 		}
   66 | 		MethodMetadata methodMetadata = (MethodMetadata) metadata;
   67 | 		return methodMetadata.getDeclaringClassName() + "#" + methodMetadata.getMethodName();
   68 | 	}
   69 | 
   70 | 	protected final void logOutcome(String classOrMethodName, ConditionOutcome outcome) {
   71 | 		if (this.logger.isTraceEnabled()) {
   72 | 			this.logger.trace(getLogMessage(classOrMethodName, outcome));
   73 | 		}
   74 | 	}
   75 | 
   76 | 	private StringBuilder getLogMessage(String classOrMethodName, ConditionOutcome outcome) {
   77 | 		StringBuilder message = new StringBuilder();
   78 | 		message.append("Condition ");
   79 | 		message.append(ClassUtils.getShortName(getClass()));
   80 | 		message.append(" on ");
   81 | 		message.append(classOrMethodName);
   82 | 		message.append(outcome.isMatch() ? " matched" : " did not match");
   83 | 		if (StringUtils.hasLength(outcome.getMessage())) {
   84 | 			message.append(" due to ");
   85 | 			message.append(outcome.getMessage());
   86 | 		}
   87 | 		return message;
   88 | 	}
   89 | 
   90 | 	private void recordEvaluation(ConditionContext context, String classOrMethodName, ConditionOutcome outcome) {
   91 | 		if (context.getBeanFactory() != null) {
   92 | 			ConditionEvaluationReport.get(context.getBeanFactory())
   93 | 				.recordConditionEvaluation(classOrMethodName, this, outcome);
   94 | 		}
   95 | 	}
   96 | 
   97 | 	/**
   98 | 	 * Determine the outcome of the match along with suitable log output.
   99 | 	 * @param context the condition context
  100 | 	 * @param metadata the annotation metadata
  101 | 	 * @return the condition outcome
  102 | 	 */
  103 | 	public abstract ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata);
  104 | 
  105 | 	/**
  106 | 	 * Return true if any of the specified conditions match.
  107 | 	 * @param context the context
  108 | 	 * @param metadata the annotation meta-data
  109 | 	 * @param conditions conditions to test
  110 | 	 * @return {@code true} if any condition matches.
  111 | 	 */
  112 | 	protected final boolean anyMatches(ConditionContext context, AnnotatedTypeMetadata metadata,
  113 | 			Condition... conditions) {
  114 | 		for (Condition condition : conditions) {
  115 | 			if (matches(context, metadata, condition)) {
  116 | 				return true;
  117 | 			}
  118 | 		}
  119 | 		return false;
  120 | 	}
  121 | 
  122 | 	/**
  123 | 	 * Return true if any of the specified condition matches.
  124 | 	 * @param context the context
  125 | 	 * @param metadata the annotation meta-data
  126 | 	 * @param condition condition to test
  127 | 	 * @return {@code true} if the condition matches.
  128 | 	 */
  129 | 	protected final boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata, Condition condition) {
  130 | 		if (condition instanceof SpringBootCondition springBootCondition) {
  131 | 			return springBootCondition.getMatchOutcome(context, metadata).isMatch();
  132 | 		}
  133 | 		return condition.matches(context, metadata);
  134 | 	}
  135 | 
  136 | }
```
