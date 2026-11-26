<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/annotation/ImportCandidates.java -->
<!-- 正文引用：[[源码:ImportCandidates.java#L起-L止]] ；代码块头部：(src: ImportCandidates.java#L起-L止) -->

# ImportCandidates.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/annotation/ImportCandidates.java` |
| 行数 | 126 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:00:48.208Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.annotation;
    6 | 
    7 | import java.io.BufferedReader;
    8 | import java.io.IOException;
    9 | import java.io.InputStreamReader;
   10 | import java.net.URL;
   11 | import java.nio.charset.StandardCharsets;
   12 | import java.util.ArrayList;
   13 | import java.util.Collections;
   14 | import java.util.Enumeration;
   15 | import java.util.Iterator;
   16 | import java.util.List;
   17 | 
   18 | import org.jspecify.annotations.Nullable;
   19 | 
   20 | import org.springframework.core.io.UrlResource;
   21 | import org.springframework.util.Assert;
   22 | 
   23 | /**
   24 |  * Contains {@code @Configuration} import candidates, usually auto-configurations.
   25 |  *
   26 |  * The {@link #load(Class, ClassLoader)} method can be used to discover the import
   27 |  * candidates.
   28 |  *
   29 |  * @author Moritz Halbritter
   30 |  * @author Scott Frederick
   31 |  * @since 2.7.0
   32 |  */
   33 | public final class ImportCandidates implements Iterable<String> {
   34 | 
   35 | 	private static final String LOCATION = "META-INF/spring/%s.imports";
   36 | 
   37 | 	private static final String COMMENT_START = "#";
   38 | 
   39 | 	private final List<String> candidates;
   40 | 
   41 | 	private ImportCandidates(List<String> candidates) {
   42 | 		Assert.notNull(candidates, "'candidates' must not be null");
   43 | 		this.candidates = Collections.unmodifiableList(candidates);
   44 | 	}
   45 | 
   46 | 	@Override
   47 | 	public Iterator<String> iterator() {
   48 | 		return this.candidates.iterator();
   49 | 	}
   50 | 
   51 | 	/**
   52 | 	 * Returns the list of loaded import candidates.
   53 | 	 * @return the list of import candidates
   54 | 	 */
   55 | 	public List<String> getCandidates() {
   56 | 		return this.candidates;
   57 | 	}
   58 | 
   59 | 	/**
   60 | 	 * Loads the names of import candidates from the classpath. The names of the import
   61 | 	 * candidates are stored in files named
   62 | 	 * {@code META-INF/spring/full-qualified-annotation-name.imports} on the classpath.
   63 | 	 * Every line contains the full qualified name of the candidate class. Comments are
   64 | 	 * supported using the # character.
   65 | 	 * @param annotation annotation to load
   66 | 	 * @param classLoader class loader to use for loading
   67 | 	 * @return list of names of annotated classes
   68 | 	 */
   69 | 	public static ImportCandidates load(Class<?> annotation, @Nullable ClassLoader classLoader) {
   70 | 		Assert.notNull(annotation, "'annotation' must not be null");
   71 | 		ClassLoader classLoaderToUse = decideClassloader(classLoader);
   72 | 		String location = String.format(LOCATION, annotation.getName());
   73 | 		Enumeration<URL> urls = findUrlsInClasspath(classLoaderToUse, location);
   74 | 		List<String> importCandidates = new ArrayList<>();
   75 | 		while (urls.hasMoreElements()) {
   76 | 			URL url = urls.nextElement();
   77 | 			importCandidates.addAll(readCandidateConfigurations(url));
   78 | 		}
   79 | 		return new ImportCandidates(importCandidates);
   80 | 	}
   81 | 
   82 | 	private static ClassLoader decideClassloader(@Nullable ClassLoader classLoader) {
   83 | 		if (classLoader == null) {
   84 | 			return ImportCandidates.class.getClassLoader();
   85 | 		}
   86 | 		return classLoader;
   87 | 	}
   88 | 
   89 | 	private static Enumeration<URL> findUrlsInClasspath(ClassLoader classLoader, String location) {
   90 | 		try {
   91 | 			return classLoader.getResources(location);
   92 | 		}
   93 | 		catch (IOException ex) {
   94 | 			throw new IllegalArgumentException("Failed to load configurations from location [" + location + "]", ex);
   95 | 		}
   96 | 	}
   97 | 
   98 | 	private static List<String> readCandidateConfigurations(URL url) {
   99 | 		try (BufferedReader reader = new BufferedReader(
  100 | 				new InputStreamReader(new UrlResource(url).getInputStream(), StandardCharsets.UTF_8))) {
  101 | 			List<String> candidates = new ArrayList<>();
  102 | 			String line;
  103 | 			while ((line = reader.readLine()) != null) {
  104 | 				line = stripComment(line);
  105 | 				line = line.trim();
  106 | 				if (line.isEmpty()) {
  107 | 					continue;
  108 | 				}
  109 | 				candidates.add(line);
  110 | 			}
  111 | 			return candidates;
  112 | 		}
  113 | 		catch (IOException ex) {
  114 | 			throw new IllegalArgumentException("Unable to load configurations from location [" + url + "]", ex);
  115 | 		}
  116 | 	}
  117 | 
  118 | 	private static String stripComment(String line) {
  119 | 		int commentStart = line.indexOf(COMMENT_START);
  120 | 		if (commentStart == -1) {
  121 | 			return line;
  122 | 		}
  123 | 		return line.substring(0, commentStart);
  124 | 	}
  125 | 
  126 | }
```
