<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-autoconfigure:4.2.0 | path: org/springframework/boot/autoconfigure/condition/FilteringSpringBootCondition.java -->
<!-- 正文引用：[[源码:FilteringSpringBootCondition.java#L起-L止]] ；代码块头部：(src: FilteringSpringBootCondition.java#L起-L止) -->

# FilteringSpringBootCondition.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-autoconfigure:4.2.0` |
| 路径 | `org/springframework/boot/autoconfigure/condition/FilteringSpringBootCondition.java` |
| 行数 | 148 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-autoconfigure/4.2.0/spring-boot-autoconfigure-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:48.118Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.autoconfigure.condition;
    6 | 
    7 | import java.util.ArrayList;
    8 | import java.util.Collection;
    9 | import java.util.Collections;
   10 | import java.util.List;
   11 | 
   12 | import org.jspecify.annotations.Nullable;
   13 | 
   14 | import org.springframework.beans.BeansException;
   15 | import org.springframework.beans.factory.BeanClassLoaderAware;
   16 | import org.springframework.beans.factory.BeanFactory;
   17 | import org.springframework.beans.factory.BeanFactoryAware;
   18 | import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
   19 | import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
   20 | import org.springframework.util.Assert;
   21 | import org.springframework.util.ClassUtils;
   22 | import org.springframework.util.CollectionUtils;
   23 | 
   24 | /**
   25 |  * Abstract base class for a {@link SpringBootCondition} that also implements
   26 |  * {@link AutoConfigurationImportFilter}.
   27 |  *
   28 |  * @author Phillip Webb
   29 |  */
   30 | abstract class FilteringSpringBootCondition extends SpringBootCondition
   31 | 		implements AutoConfigurationImportFilter, BeanFactoryAware, BeanClassLoaderAware {
   32 | 
   33 | 	@SuppressWarnings("NullAway.Init")
   34 | 	private BeanFactory beanFactory;
   35 | 
   36 | 	@SuppressWarnings("NullAway.Init")
   37 | 	private ClassLoader beanClassLoader;
   38 | 
   39 | 	@Override
   40 | 	public boolean[] match(@Nullable String[] autoConfigurationClasses,
   41 | 			AutoConfigurationMetadata autoConfigurationMetadata) {
   42 | 		ConditionEvaluationReport report = ConditionEvaluationReport.find(this.beanFactory);
   43 | 		@Nullable ConditionOutcome[] outcomes = getOutcomes(autoConfigurationClasses, autoConfigurationMetadata);
   44 | 		boolean[] match = new boolean[outcomes.length];
   45 | 		for (int i = 0; i < outcomes.length; i++) {
   46 | 			ConditionOutcome outcome = outcomes[i];
   47 | 			match[i] = (outcome == null || outcome.isMatch());
   48 | 			if (!match[i] && outcome != null) {
   49 | 				String autoConfigurationClass = autoConfigurationClasses[i];
   50 | 				Assert.state(autoConfigurationClass != null, "'autoConfigurationClass' must not be null");
   51 | 				logOutcome(autoConfigurationClass, outcome);
   52 | 				if (report != null) {
   53 | 					report.recordConditionEvaluation(autoConfigurationClass, this, outcome);
   54 | 				}
   55 | 			}
   56 | 		}
   57 | 		return match;
   58 | 	}
   59 | 
   60 | 	protected abstract @Nullable ConditionOutcome[] getOutcomes(@Nullable String[] autoConfigurationClasses,
   61 | 			AutoConfigurationMetadata autoConfigurationMetadata);
   62 | 
   63 | 	@Override
   64 | 	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
   65 | 		this.beanFactory = beanFactory;
   66 | 	}
   67 | 
   68 | 	protected final BeanFactory getBeanFactory() {
   69 | 		return this.beanFactory;
   70 | 	}
   71 | 
   72 | 	protected final ClassLoader getBeanClassLoader() {
   73 | 		return this.beanClassLoader;
   74 | 	}
   75 | 
   76 | 	@Override
   77 | 	public void setBeanClassLoader(ClassLoader classLoader) {
   78 | 		this.beanClassLoader = classLoader;
   79 | 	}
   80 | 
   81 | 	protected final List<String> filter(@Nullable Collection<String> classNames, ClassNameFilter classNameFilter,
   82 | 			@Nullable ClassLoader classLoader) {
   83 | 		if (CollectionUtils.isEmpty(classNames)) {
   84 | 			return Collections.emptyList();
   85 | 		}
   86 | 		List<String> matches = new ArrayList<>(classNames.size());
   87 | 		for (String candidate : classNames) {
   88 | 			if (classNameFilter.matches(candidate, classLoader)) {
   89 | 				matches.add(candidate);
   90 | 			}
   91 | 		}
   92 | 		return matches;
   93 | 	}
   94 | 
   95 | 	/**
   96 | 	 * Slightly faster variant of {@link ClassUtils#forName(String, ClassLoader)} that
   97 | 	 * doesn't deal with primitives, arrays or inner types.
   98 | 	 * @param className the class name to resolve
   99 | 	 * @param classLoader the class loader to use
  100 | 	 * @return a resolved class
  101 | 	 * @throws ClassNotFoundException if the class cannot be found
  102 | 	 */
  103 | 	protected static Class<?> resolve(String className, @Nullable ClassLoader classLoader)
  104 | 			throws ClassNotFoundException {
  105 | 		if (classLoader != null) {
  106 | 			return Class.forName(className, false, classLoader);
  107 | 		}
  108 | 		return Class.forName(className);
  109 | 	}
  110 | 
  111 | 	protected enum ClassNameFilter {
  112 | 
  113 | 		PRESENT {
  114 | 
  115 | 			@Override
  116 | 			public boolean matches(String className, @Nullable ClassLoader classLoader) {
  117 | 				return isPresent(className, classLoader);
  118 | 			}
  119 | 
  120 | 		},
  121 | 
  122 | 		MISSING {
  123 | 
  124 | 			@Override
  125 | 			public boolean matches(String className, @Nullable ClassLoader classLoader) {
  126 | 				return !isPresent(className, classLoader);
  127 | 			}
  128 | 
  129 | 		};
  130 | 
  131 | 		abstract boolean matches(String className, @Nullable ClassLoader classLoader);
  132 | 
  133 | 		private static boolean isPresent(String className, @Nullable ClassLoader classLoader) {
  134 | 			if (classLoader == null) {
  135 | 				classLoader = ClassUtils.getDefaultClassLoader();
  136 | 			}
  137 | 			try {
  138 | 				resolve(className, classLoader);
  139 | 				return true;
  140 | 			}
  141 | 			catch (Throwable ex) {
  142 | 				return false;
  143 | 			}
  144 | 		}
  145 | 
  146 | 	}
  147 | 
  148 | }
```
