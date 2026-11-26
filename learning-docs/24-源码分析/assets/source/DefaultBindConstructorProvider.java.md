<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/properties/bind/DefaultBindConstructorProvider.java -->
<!-- 正文引用：[[源码:DefaultBindConstructorProvider.java#L起-L止]] ；代码块头部：(src: DefaultBindConstructorProvider.java#L起-L止) -->

# DefaultBindConstructorProvider.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/properties/bind/DefaultBindConstructorProvider.java` |
| 行数 | 202 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:53.522Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.properties.bind;
    6 | 
    7 | import java.lang.reflect.Constructor;
    8 | import java.lang.reflect.Modifier;
    9 | import java.util.Arrays;
   10 | import java.util.stream.Stream;
   11 | 
   12 | import org.jspecify.annotations.Nullable;
   13 | 
   14 | import org.springframework.beans.BeanUtils;
   15 | import org.springframework.beans.factory.annotation.Autowired;
   16 | import org.springframework.core.KotlinDetector;
   17 | import org.springframework.core.annotation.MergedAnnotations;
   18 | import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
   19 | import org.springframework.util.Assert;
   20 | import org.springframework.util.ClassUtils;
   21 | 
   22 | /**
   23 |  * Default {@link BindConstructorProvider} implementation.
   24 |  *
   25 |  * @author Madhura Bhave
   26 |  * @author Phillip Webb
   27 |  */
   28 | class DefaultBindConstructorProvider implements BindConstructorProvider {
   29 | 
   30 | 	@Override
   31 | 	public @Nullable Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding) {
   32 | 		Constructors constructors = Constructors.getConstructors(bindable.getType().resolve(),
   33 | 				isNestedConstructorBinding);
   34 | 		if (constructors.getBind() != null && constructors.isDeducedBindConstructor()
   35 | 				&& !constructors.isImmutableType()) {
   36 | 			if (bindable.getValue() != null && bindable.getValue().get() != null) {
   37 | 				return null;
   38 | 			}
   39 | 		}
   40 | 		return constructors.getBind();
   41 | 	}
   42 | 
   43 | 	@Override
   44 | 	public @Nullable Constructor<?> getBindConstructor(Class<?> type, boolean isNestedConstructorBinding) {
   45 | 		Constructors constructors = Constructors.getConstructors(type, isNestedConstructorBinding);
   46 | 		return constructors.getBind();
   47 | 	}
   48 | 
   49 | 	/**
   50 | 	 * Data holder for autowired and bind constructors.
   51 | 	 */
   52 | 	static final class Constructors {
   53 | 
   54 | 		private static final Constructors NONE = new Constructors(false, null, false, false);
   55 | 
   56 | 		private final boolean hasAutowired;
   57 | 
   58 | 		private final @Nullable Constructor<?> bind;
   59 | 
   60 | 		private final boolean deducedBindConstructor;
   61 | 
   62 | 		private final boolean immutableType;
   63 | 
   64 | 		private Constructors(boolean hasAutowired, @Nullable Constructor<?> bind, boolean deducedBindConstructor,
   65 | 				boolean immutableType) {
   66 | 			this.hasAutowired = hasAutowired;
   67 | 			this.bind = bind;
   68 | 			this.deducedBindConstructor = deducedBindConstructor;
   69 | 			this.immutableType = immutableType;
   70 | 		}
   71 | 
   72 | 		boolean hasAutowired() {
   73 | 			return this.hasAutowired;
   74 | 		}
   75 | 
   76 | 		@Nullable Constructor<?> getBind() {
   77 | 			return this.bind;
   78 | 		}
   79 | 
   80 | 		boolean isDeducedBindConstructor() {
   81 | 			return this.deducedBindConstructor;
   82 | 		}
   83 | 
   84 | 		boolean isImmutableType() {
   85 | 			return this.immutableType;
   86 | 		}
   87 | 
   88 | 		static Constructors getConstructors(@Nullable Class<?> type, boolean isNestedConstructorBinding) {
   89 | 			if (type == null) {
   90 | 				return NONE;
   91 | 			}
   92 | 			boolean hasAutowiredConstructor = isAutowiredPresent(type);
   93 | 			Constructor<?>[] candidates = getCandidateConstructors(type);
   94 | 			MergedAnnotations[] candidateAnnotations = getAnnotations(candidates);
   95 | 			boolean deducedBindConstructor = false;
   96 | 			boolean immutableType = type.isRecord();
   97 | 			Constructor<?> bind = getConstructorBindingAnnotated(type, candidates, candidateAnnotations);
   98 | 			if (bind == null && !hasAutowiredConstructor) {
   99 | 				bind = deduceBindConstructor(type, candidates);
  100 | 				deducedBindConstructor = bind != null;
  101 | 			}
  102 | 			if (bind == null && !hasAutowiredConstructor && isKotlinType(type)) {
  103 | 				bind = deduceKotlinBindConstructor(type);
  104 | 				deducedBindConstructor = bind != null;
  105 | 			}
  106 | 			if (bind != null || isNestedConstructorBinding) {
  107 | 				Assert.state(!hasAutowiredConstructor,
  108 | 						() -> type.getName() + " declares @ConstructorBinding and @Autowired constructor");
  109 | 			}
  110 | 			return new Constructors(hasAutowiredConstructor, bind, deducedBindConstructor, immutableType);
  111 | 		}
  112 | 
  113 | 		private static boolean isAutowiredPresent(Class<?> type) {
  114 | 			if (Stream.of(type.getDeclaredConstructors())
  115 | 				.map(MergedAnnotations::from)
  116 | 				.anyMatch((annotations) -> annotations.isPresent(Autowired.class))) {
  117 | 				return true;
  118 | 			}
  119 | 			Class<?> userClass = ClassUtils.getUserClass(type);
  120 | 			return (userClass != type) && isAutowiredPresent(userClass);
  121 | 		}
  122 | 
  123 | 		private static Constructor<?>[] getCandidateConstructors(Class<?> type) {
  124 | 			if (isInnerClass(type)) {
  125 | 				return new Constructor<?>[0];
  126 | 			}
  127 | 			return Arrays.stream(type.getDeclaredConstructors())
  128 | 				.filter(Constructors::isNonSynthetic)
  129 | 				.toArray(Constructor[]::new);
  130 | 		}
  131 | 
  132 | 		private static boolean isInnerClass(Class<?> type) {
  133 | 			try {
  134 | 				return type.getDeclaredField("this$0").isSynthetic();
  135 | 			}
  136 | 			catch (NoSuchFieldException ex) {
  137 | 				return false;
  138 | 			}
  139 | 		}
  140 | 
  141 | 		private static boolean isNonSynthetic(Constructor<?> constructor) {
  142 | 			return !constructor.isSynthetic();
  143 | 		}
  144 | 
  145 | 		private static MergedAnnotations[] getAnnotations(Constructor<?>[] candidates) {
  146 | 			MergedAnnotations[] candidateAnnotations = new MergedAnnotations[candidates.length];
  147 | 			for (int i = 0; i < candidates.length; i++) {
  148 | 				candidateAnnotations[i] = MergedAnnotations.from(candidates[i], SearchStrategy.SUPERCLASS);
  149 | 			}
  150 | 			return candidateAnnotations;
  151 | 		}
  152 | 
  153 | 		private static @Nullable Constructor<?> getConstructorBindingAnnotated(Class<?> type,
  154 | 				Constructor<?>[] candidates, MergedAnnotations[] mergedAnnotations) {
  155 | 			Constructor<?> result = null;
  156 | 			for (int i = 0; i < candidates.length; i++) {
  157 | 				if (mergedAnnotations[i].isPresent(ConstructorBinding.class)) {
  158 | 					Assert.state(candidates[i].getParameterCount() > 0,
  159 | 							() -> type.getName() + " declares @ConstructorBinding on a no-args constructor");
  160 | 					Assert.state(result == null,
  161 | 							() -> type.getName() + " has more than one @ConstructorBinding constructor");
  162 | 					result = candidates[i];
  163 | 				}
  164 | 			}
  165 | 			return result;
  166 | 
  167 | 		}
  168 | 
  169 | 		private static @Nullable Constructor<?> deduceBindConstructor(Class<?> type, Constructor<?>[] candidates) {
  170 | 			if (candidates.length == 1 && candidates[0].getParameterCount() > 0) {
  171 | 				if (type.isMemberClass() && Modifier.isPrivate(candidates[0].getModifiers())) {
  172 | 					return null;
  173 | 				}
  174 | 				return candidates[0];
  175 | 			}
  176 | 			Constructor<?> result = null;
  177 | 			for (Constructor<?> candidate : candidates) {
  178 | 				if (!Modifier.isPrivate(candidate.getModifiers())) {
  179 | 					if (result != null) {
  180 | 						return null;
  181 | 					}
  182 | 					result = candidate;
  183 | 				}
  184 | 			}
  185 | 			return (result != null && result.getParameterCount() > 0) ? result : null;
  186 | 		}
  187 | 
  188 | 		private static boolean isKotlinType(Class<?> type) {
  189 | 			return KotlinDetector.isKotlinPresent() && KotlinDetector.isKotlinType(type);
  190 | 		}
  191 | 
  192 | 		private static @Nullable Constructor<?> deduceKotlinBindConstructor(Class<?> type) {
  193 | 			Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(type);
  194 | 			if (primaryConstructor != null && primaryConstructor.getParameterCount() > 0) {
  195 | 				return primaryConstructor;
  196 | 			}
  197 | 			return null;
  198 | 		}
  199 | 
  200 | 	}
  201 | 
  202 | }
```
