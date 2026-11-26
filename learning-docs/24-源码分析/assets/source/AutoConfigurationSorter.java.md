<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-autoconfigure:4.2.0 | path: org/springframework/boot/autoconfigure/AutoConfigurationSorter.java -->
<!-- 正文引用：[[源码:AutoConfigurationSorter.java#L起-L止]] ；代码块头部：(src: AutoConfigurationSorter.java#L起-L止) -->

# AutoConfigurationSorter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-autoconfigure:4.2.0` |
| 路径 | `org/springframework/boot/autoconfigure/AutoConfigurationSorter.java` |
| 行数 | 288 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-autoconfigure/4.2.0/spring-boot-autoconfigure-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:06.718Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.autoconfigure;
    6 | 
    7 | import java.io.IOException;
    8 | import java.lang.annotation.Annotation;
    9 | import java.util.ArrayList;
   10 | import java.util.Collection;
   11 | import java.util.Collections;
   12 | import java.util.Comparator;
   13 | import java.util.LinkedHashMap;
   14 | import java.util.LinkedHashSet;
   15 | import java.util.List;
   16 | import java.util.Map;
   17 | import java.util.Set;
   18 | import java.util.TreeSet;
   19 | import java.util.function.UnaryOperator;
   20 | 
   21 | import org.jspecify.annotations.Nullable;
   22 | 
   23 | import org.springframework.core.type.AnnotationMetadata;
   24 | import org.springframework.core.type.classreading.MetadataReader;
   25 | import org.springframework.core.type.classreading.MetadataReaderFactory;
   26 | import org.springframework.util.Assert;
   27 | 
   28 | /**
   29 |  * Sort {@link EnableAutoConfiguration auto-configuration} classes into priority order by
   30 |  * reading {@link AutoConfigureOrder @AutoConfigureOrder},
   31 |  * {@link AutoConfigureBefore @AutoConfigureBefore} and
   32 |  * {@link AutoConfigureAfter @AutoConfigureAfter} annotations (without loading classes).
   33 |  *
   34 |  * @author Phillip Webb
   35 |  */
   36 | class AutoConfigurationSorter {
   37 | 
   38 | 	private final MetadataReaderFactory metadataReaderFactory;
   39 | 
   40 | 	private final @Nullable AutoConfigurationMetadata autoConfigurationMetadata;
   41 | 
   42 | 	private final @Nullable UnaryOperator<String> replacementMapper;
   43 | 
   44 | 	AutoConfigurationSorter(MetadataReaderFactory metadataReaderFactory,
   45 | 			@Nullable AutoConfigurationMetadata autoConfigurationMetadata,
   46 | 			@Nullable UnaryOperator<String> replacementMapper) {
   47 | 		Assert.notNull(metadataReaderFactory, "'metadataReaderFactory' must not be null");
   48 | 		this.metadataReaderFactory = metadataReaderFactory;
   49 | 		this.autoConfigurationMetadata = autoConfigurationMetadata;
   50 | 		this.replacementMapper = replacementMapper;
   51 | 	}
   52 | 
   53 | 	List<String> getInPriorityOrder(Collection<String> classNames) {
   54 | 		// Initially sort alphabetically
   55 | 		List<String> alphabeticallyOrderedClassNames = new ArrayList<>(classNames);
   56 | 		Collections.sort(alphabeticallyOrderedClassNames);
   57 | 		// Then sort by order
   58 | 		AutoConfigurationClasses classes = new AutoConfigurationClasses(this.metadataReaderFactory,
   59 | 				this.autoConfigurationMetadata, alphabeticallyOrderedClassNames);
   60 | 		List<String> orderedClassNames = new ArrayList<>(classNames);
   61 | 		Collections.sort(orderedClassNames);
   62 | 		orderedClassNames.sort((o1, o2) -> {
   63 | 			int i1 = classes.get(o1).getOrder();
   64 | 			int i2 = classes.get(o2).getOrder();
   65 | 			return Integer.compare(i1, i2);
   66 | 		});
   67 | 		// Then respect @AutoConfigureBefore @AutoConfigureAfter
   68 | 		orderedClassNames = sortByAnnotation(new LinkedHashSet<>(orderedClassNames), classes);
   69 | 		return orderedClassNames;
   70 | 	}
   71 | 
   72 | 	private List<String> sortByAnnotation(Set<String> classNames, AutoConfigurationClasses classes) {
   73 | 		Map<String, Integer> sortOrder = new LinkedHashMap<>();
   74 | 		classNames.forEach((className) -> sortOrder.putIfAbsent(className, sortOrder.size()));
   75 | 		classes.getAllNames().forEach((className) -> sortOrder.putIfAbsent(className, sortOrder.size()));
   76 | 		Set<String> sorted = new LinkedHashSet<>();
   77 | 		Set<String> processing = new LinkedHashSet<>();
   78 | 		for (String className : sortOrder.keySet()) {
   79 | 			doSortByAfterAnnotation(classes, sortOrder, sorted, processing, className);
   80 | 		}
   81 | 		sorted.retainAll(classNames);
   82 | 		return new ArrayList<>(sorted);
   83 | 	}
   84 | 
   85 | 	private void doSortByAfterAnnotation(AutoConfigurationClasses classes, Map<String, Integer> sortOrder,
   86 | 			Set<String> sorted, Set<String> processing, String current) {
   87 | 		if (sorted.contains(current)) {
   88 | 			return;
   89 | 		}
   90 | 		processing.add(current);
   91 | 		Set<String> afters = new TreeSet<>(Comparator.comparingInt((String name) -> sortOrder.getOrDefault(name, -1)));
   92 | 		afters.addAll(classes.getClassesRequestedAfter(current));
   93 | 		for (String after : afters) {
   94 | 			checkForCycles(processing, current, after);
   95 | 			if (sortOrder.containsKey(after)) {
   96 | 				doSortByAfterAnnotation(classes, sortOrder, sorted, processing, after);
   97 | 			}
   98 | 		}
   99 | 		processing.remove(current);
  100 | 		sorted.add(current);
  101 | 	}
  102 | 
  103 | 	private void checkForCycles(Set<String> processing, String current, String after) {
  104 | 		Assert.state(!processing.contains(after),
  105 | 				() -> "AutoConfigure cycle detected between " + current + " and " + after);
  106 | 	}
  107 | 
  108 | 	private class AutoConfigurationClasses {
  109 | 
  110 | 		private final Map<String, AutoConfigurationClass> classes = new LinkedHashMap<>();
  111 | 
  112 | 		private final Map<String, Set<String>> classesRequestedAfter = new LinkedHashMap<>();
  113 | 
  114 | 		AutoConfigurationClasses(MetadataReaderFactory metadataReaderFactory,
  115 | 				@Nullable AutoConfigurationMetadata autoConfigurationMetadata, Collection<String> classNames) {
  116 | 			addToClasses(metadataReaderFactory, autoConfigurationMetadata, classNames, true);
  117 | 		}
  118 | 
  119 | 		Set<String> getAllNames() {
  120 | 			return this.classes.keySet();
  121 | 		}
  122 | 
  123 | 		private void addToClasses(MetadataReaderFactory metadataReaderFactory,
  124 | 				@Nullable AutoConfigurationMetadata autoConfigurationMetadata, Collection<String> classNames,
  125 | 				boolean required) {
  126 | 			for (String className : classNames) {
  127 | 				if (!this.classes.containsKey(className)) {
  128 | 					AutoConfigurationClass autoConfigurationClass = new AutoConfigurationClass(className,
  129 | 							metadataReaderFactory, autoConfigurationMetadata);
  130 | 					boolean available = autoConfigurationClass.isAvailable();
  131 | 					if (required || available) {
  132 | 						this.classes.put(className, autoConfigurationClass);
  133 | 					}
  134 | 					if (available) {
  135 | 						Set<String> before = autoConfigurationClass.getBefore();
  136 | 						for (String name : before) {
  137 | 							this.classesRequestedAfter.computeIfAbsent(name, (key) -> new LinkedHashSet<>())
  138 | 								.add(className);
  139 | 						}
  140 | 						addToClasses(metadataReaderFactory, autoConfigurationMetadata, before, false);
  141 | 						addToClasses(metadataReaderFactory, autoConfigurationMetadata,
  142 | 								autoConfigurationClass.getAfter(), false);
  143 | 					}
  144 | 				}
  145 | 			}
  146 | 		}
  147 | 
  148 | 		AutoConfigurationClass get(String className) {
  149 | 			AutoConfigurationClass autoConfigurationClass = this.classes.get(className);
  150 | 			Assert.state(autoConfigurationClass != null, "'autoConfigurationClass' must not be null");
  151 | 			return autoConfigurationClass;
  152 | 		}
  153 | 
  154 | 		Set<String> getClassesRequestedAfter(String className) {
  155 | 			Set<String> classesRequestedAfter = new LinkedHashSet<>(get(className).getAfter());
  156 | 			classesRequestedAfter.addAll(this.classesRequestedAfter.getOrDefault(className, Collections.emptySet()));
  157 | 			return classesRequestedAfter;
  158 | 		}
  159 | 
  160 | 	}
  161 | 
  162 | 	private class AutoConfigurationClass {
  163 | 
  164 | 		private final String className;
  165 | 
  166 | 		private final MetadataReaderFactory metadataReaderFactory;
  167 | 
  168 | 		private final @Nullable AutoConfigurationMetadata autoConfigurationMetadata;
  169 | 
  170 | 		private volatile @Nullable AnnotationMetadata annotationMetadata;
  171 | 
  172 | 		private volatile @Nullable Set<String> before;
  173 | 
  174 | 		private volatile @Nullable Set<String> after;
  175 | 
  176 | 		AutoConfigurationClass(String className, MetadataReaderFactory metadataReaderFactory,
  177 | 				@Nullable AutoConfigurationMetadata autoConfigurationMetadata) {
  178 | 			this.className = className;
  179 | 			this.metadataReaderFactory = metadataReaderFactory;
  180 | 			this.autoConfigurationMetadata = autoConfigurationMetadata;
  181 | 		}
  182 | 
  183 | 		boolean isAvailable() {
  184 | 			try {
  185 | 				if (!wasProcessed()) {
  186 | 					getAnnotationMetadata();
  187 | 				}
  188 | 				return true;
  189 | 			}
  190 | 			catch (Exception ex) {
  191 | 				return false;
  192 | 			}
  193 | 		}
  194 | 
  195 | 		Set<String> getBefore() {
  196 | 			Set<String> before = this.before;
  197 | 			if (before == null) {
  198 | 				before = getClassNames("AutoConfigureBefore", AutoConfigureBefore.class);
  199 | 				this.before = before;
  200 | 			}
  201 | 			return before;
  202 | 		}
  203 | 
  204 | 		Set<String> getAfter() {
  205 | 			Set<String> after = this.after;
  206 | 			if (after == null) {
  207 | 				after = getClassNames("AutoConfigureAfter", AutoConfigureAfter.class);
  208 | 				this.after = after;
  209 | 			}
  210 | 			return after;
  211 | 		}
  212 | 
  213 | 		private Set<String> getClassNames(String metadataKey, Class<? extends Annotation> annotation) {
  214 | 			Set<String> annotationValue = wasProcessed() ? getSet(metadataKey) : getAnnotationValue(annotation);
  215 | 			return applyReplacements(annotationValue);
  216 | 		}
  217 | 
  218 | 		private Set<String> getSet(String metadataKey) {
  219 | 			Assert.state(this.autoConfigurationMetadata != null, "'autoConfigurationMetadata' must not be null");
  220 | 			return this.autoConfigurationMetadata.getSet(this.className, metadataKey, Collections.emptySet());
  221 | 		}
  222 | 
  223 | 		private Set<String> applyReplacements(Set<String> values) {
  224 | 			if (AutoConfigurationSorter.this.replacementMapper == null) {
  225 | 				return values;
  226 | 			}
  227 | 			Set<String> replaced = new LinkedHashSet<>(values);
  228 | 			for (String value : values) {
  229 | 				replaced.add(AutoConfigurationSorter.this.replacementMapper.apply(value));
  230 | 			}
  231 | 			return replaced;
  232 | 		}
  233 | 
  234 | 		private int getOrder() {
  235 | 			if (wasProcessed()) {
  236 | 				Assert.state(this.autoConfigurationMetadata != null, "'autoConfigurationMetadata' must not be null");
  237 | 				return this.autoConfigurationMetadata.getInteger(this.className, "AutoConfigureOrder",
  238 | 						AutoConfigureOrder.DEFAULT_ORDER);
  239 | 			}
  240 | 			Map<String, @Nullable Object> attributes = getAnnotationMetadata()
  241 | 				.getAnnotationAttributes(AutoConfigureOrder.class.getName());
  242 | 			if (attributes != null) {
  243 | 				Integer value = (Integer) attributes.get("value");
  244 | 				Assert.state(value != null, "'value' must not be null");
  245 | 				return value;
  246 | 			}
  247 | 			return AutoConfigureOrder.DEFAULT_ORDER;
  248 | 		}
  249 | 
  250 | 		private boolean wasProcessed() {
  251 | 			return (this.autoConfigurationMetadata != null
  252 | 					&& this.autoConfigurationMetadata.wasProcessed(this.className));
  253 | 		}
  254 | 
  255 | 		private Set<String> getAnnotationValue(Class<?> annotation) {
  256 | 			Map<String, @Nullable Object> attributes = getAnnotationMetadata()
  257 | 				.getAnnotationAttributes(annotation.getName(), true);
  258 | 			if (attributes == null) {
  259 | 				return Collections.emptySet();
  260 | 			}
  261 | 			Set<String> result = new LinkedHashSet<>();
  262 | 			String[] value = (String[]) attributes.get("value");
  263 | 			String[] name = (String[]) attributes.get("name");
  264 | 			Assert.state(value != null, "'value' must not be null");
  265 | 			Assert.state(name != null, "'name' must not be null");
  266 | 			Collections.addAll(result, value);
  267 | 			Collections.addAll(result, name);
  268 | 			return result;
  269 | 		}
  270 | 
  271 | 		private AnnotationMetadata getAnnotationMetadata() {
  272 | 			AnnotationMetadata annotationMetadata = this.annotationMetadata;
  273 | 			if (annotationMetadata == null) {
  274 | 				try {
  275 | 					MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(this.className);
  276 | 					annotationMetadata = metadataReader.getAnnotationMetadata();
  277 | 					this.annotationMetadata = annotationMetadata;
  278 | 				}
  279 | 				catch (IOException ex) {
  280 | 					throw new IllegalStateException("Unable to read meta-data for class " + this.className, ex);
  281 | 				}
  282 | 			}
  283 | 			return annotationMetadata;
  284 | 		}
  285 | 
  286 | 	}
  287 | 
  288 | }
```
