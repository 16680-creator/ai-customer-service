<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot-test:4.2.0 | path: org/springframework/boot/test/context/filter/annotation/AnnotationCustomizableTypeExcludeFilter.java -->
<!-- 正文引用：[[源码:AnnotationCustomizableTypeExcludeFilter.java#L起-L止]] ；代码块头部：(src: AnnotationCustomizableTypeExcludeFilter.java#L起-L止) -->

# AnnotationCustomizableTypeExcludeFilter.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot-test:4.2.0` |
| 路径 | `org/springframework/boot/test/context/filter/annotation/AnnotationCustomizableTypeExcludeFilter.java` |
| 行数 | 139 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-test/4.2.0/spring-boot-test-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:05:16.266Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.test.context.filter.annotation;
    6 | 
    7 | import java.io.IOException;
    8 | import java.lang.annotation.Annotation;
    9 | import java.util.Arrays;
   10 | import java.util.Objects;
   11 | import java.util.Set;
   12 | 
   13 | import org.jspecify.annotations.Nullable;
   14 | 
   15 | import org.springframework.beans.factory.BeanClassLoaderAware;
   16 | import org.springframework.boot.context.TypeExcludeFilter;
   17 | import org.springframework.context.annotation.ComponentScan.Filter;
   18 | import org.springframework.core.type.classreading.MetadataReader;
   19 | import org.springframework.core.type.classreading.MetadataReaderFactory;
   20 | import org.springframework.core.type.filter.AnnotationTypeFilter;
   21 | import org.springframework.core.type.filter.AssignableTypeFilter;
   22 | import org.springframework.util.ObjectUtils;
   23 | 
   24 | /**
   25 |  * Abstract base class for a {@link TypeExcludeFilter} that can be customized using an
   26 |  * annotation.
   27 |  *
   28 |  * @author Phillip Webb
   29 |  * @since 4.0.0
   30 |  */
   31 | public abstract class AnnotationCustomizableTypeExcludeFilter extends TypeExcludeFilter
   32 | 		implements BeanClassLoaderAware {
   33 | 
   34 | 	@SuppressWarnings("NullAway.Init")
   35 | 	private ClassLoader classLoader;
   36 | 
   37 | 	@Override
   38 | 	public void setBeanClassLoader(ClassLoader classLoader) {
   39 | 		this.classLoader = classLoader;
   40 | 	}
   41 | 
   42 | 	@Override
   43 | 	public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
   44 | 			throws IOException {
   45 | 		if (hasAnnotation()) {
   46 | 			return !(include(metadataReader, metadataReaderFactory) && !exclude(metadataReader, metadataReaderFactory));
   47 | 		}
   48 | 		return false;
   49 | 	}
   50 | 
   51 | 	protected boolean include(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
   52 | 			throws IOException {
   53 | 		if (new FilterAnnotations(this.classLoader, getFilters(FilterType.INCLUDE)).anyMatches(metadataReader,
   54 | 				metadataReaderFactory)) {
   55 | 			return true;
   56 | 		}
   57 | 		return isUseDefaultFilters() && defaultInclude(metadataReader, metadataReaderFactory);
   58 | 	}
   59 | 
   60 | 	protected boolean defaultInclude(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
   61 | 			throws IOException {
   62 | 		for (Class<?> include : getDefaultIncludes()) {
   63 | 			if (isTypeOrAnnotated(metadataReader, metadataReaderFactory, include)) {
   64 | 				return true;
   65 | 			}
   66 | 		}
   67 | 		for (Class<?> component : getComponentIncludes()) {
   68 | 			if (isTypeOrAnnotated(metadataReader, metadataReaderFactory, component)) {
   69 | 				return true;
   70 | 			}
   71 | 		}
   72 | 		return false;
   73 | 	}
   74 | 
   75 | 	protected boolean exclude(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory)
   76 | 			throws IOException {
   77 | 		return new FilterAnnotations(this.classLoader, getFilters(FilterType.EXCLUDE)).anyMatches(metadataReader,
   78 | 				metadataReaderFactory);
   79 | 	}
   80 | 
   81 | 	@SuppressWarnings("unchecked")
   82 | 	protected final boolean isTypeOrAnnotated(MetadataReader metadataReader,
   83 | 			MetadataReaderFactory metadataReaderFactory, Class<?> type) throws IOException {
   84 | 		AnnotationTypeFilter annotationFilter = new AnnotationTypeFilter((Class<? extends Annotation>) type);
   85 | 		AssignableTypeFilter typeFilter = new AssignableTypeFilter(type);
   86 | 		return annotationFilter.match(metadataReader, metadataReaderFactory)
   87 | 				|| typeFilter.match(metadataReader, metadataReaderFactory);
   88 | 	}
   89 | 
   90 | 	protected abstract boolean hasAnnotation();
   91 | 
   92 | 	protected abstract Filter[] getFilters(FilterType type);
   93 | 
   94 | 	protected abstract boolean isUseDefaultFilters();
   95 | 
   96 | 	protected abstract Set<Class<?>> getDefaultIncludes();
   97 | 
   98 | 	protected abstract Set<Class<?>> getComponentIncludes();
   99 | 
  100 | 	protected enum FilterType {
  101 | 
  102 | 		INCLUDE, EXCLUDE
  103 | 
  104 | 	}
  105 | 
  106 | 	@Override
  107 | 	public boolean equals(@Nullable Object obj) {
  108 | 		if (this == obj) {
  109 | 			return true;
  110 | 		}
  111 | 		if (obj == null || getClass() != obj.getClass()) {
  112 | 			return false;
  113 | 		}
  114 | 		AnnotationCustomizableTypeExcludeFilter other = (AnnotationCustomizableTypeExcludeFilter) obj;
  115 | 		boolean result = hasAnnotation() == other.hasAnnotation();
  116 | 		for (FilterType filterType : FilterType.values()) {
  117 | 			result &= ObjectUtils.nullSafeEquals(getFilters(filterType), other.getFilters(filterType));
  118 | 		}
  119 | 		result = result && isUseDefaultFilters() == other.isUseDefaultFilters();
  120 | 		result = result && ObjectUtils.nullSafeEquals(getDefaultIncludes(), other.getDefaultIncludes());
  121 | 		result = result && ObjectUtils.nullSafeEquals(getComponentIncludes(), other.getComponentIncludes());
  122 | 		return result;
  123 | 	}
  124 | 
  125 | 	@Override
  126 | 	public int hashCode() {
  127 | 		final int prime = 31;
  128 | 		int result = 0;
  129 | 		result = prime * result + Boolean.hashCode(hasAnnotation());
  130 | 		for (FilterType filterType : FilterType.values()) {
  131 | 			result = prime * result + Arrays.hashCode(getFilters(filterType));
  132 | 		}
  133 | 		result = prime * result + Boolean.hashCode(isUseDefaultFilters());
  134 | 		result = prime * result + Objects.hashCode(getDefaultIncludes());
  135 | 		result = prime * result + Objects.hashCode(getComponentIncludes());
  136 | 		return result;
  137 | 	}
  138 | 
  139 | }
```
