<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework.boot:spring-boot:4.2.0 | path: org/springframework/boot/context/config/LocationResourceLoader.java -->
<!-- 正文引用：[[源码:LocationResourceLoader.java#L起-L止]] ；代码块头部：(src: LocationResourceLoader.java#L起-L止) -->

# LocationResourceLoader.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework.boot:spring-boot:4.2.0` |
| 路径 | `org/springframework/boot/context/config/LocationResourceLoader.java` |
| 行数 | 156 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/4.2.0/spring-boot-4.2.0-sources.jar |
| 抓取时间 | 2026-11-25T14:04:55.320Z |

```java
    1 | /*
    2 |  * Copyright 2012-present the original author or authors.
    3 |  */
    4 | 
    5 | package org.springframework.boot.context.config;
    6 | 
    7 | import java.io.File;
    8 | import java.io.FilenameFilter;
    9 | import java.util.ArrayList;
   10 | import java.util.Arrays;
   11 | import java.util.Comparator;
   12 | import java.util.List;
   13 | 
   14 | import org.springframework.core.io.FileSystemResource;
   15 | import org.springframework.core.io.Resource;
   16 | import org.springframework.core.io.ResourceLoader;
   17 | import org.springframework.core.io.support.ResourcePatternResolver;
   18 | import org.springframework.util.Assert;
   19 | import org.springframework.util.ResourceUtils;
   20 | import org.springframework.util.StringUtils;
   21 | 
   22 | /**
   23 |  * Strategy interface for loading resources from a location. Supports single resource and
   24 |  * simple wildcard directory patterns.
   25 |  *
   26 |  * @author Phillip Webb
   27 |  * @author Madhura Bhave
   28 |  */
   29 | class LocationResourceLoader {
   30 | 
   31 | 	private static final Resource[] EMPTY_RESOURCES = {};
   32 | 
   33 | 	private static final Comparator<File> FILE_PATH_COMPARATOR = Comparator.comparing(File::getAbsolutePath);
   34 | 
   35 | 	private static final Comparator<File> FILE_NAME_COMPARATOR = Comparator.comparing(File::getName);
   36 | 
   37 | 	private final ResourceLoader resourceLoader;
   38 | 
   39 | 	/**
   40 | 	 * Create a new {@link LocationResourceLoader} instance.
   41 | 	 * @param resourceLoader the underlying resource loader
   42 | 	 */
   43 | 	LocationResourceLoader(ResourceLoader resourceLoader) {
   44 | 		this.resourceLoader = resourceLoader;
   45 | 	}
   46 | 
   47 | 	/**
   48 | 	 * Returns if the location contains a pattern.
   49 | 	 * @param location the location to check
   50 | 	 * @return if the location is a pattern
   51 | 	 */
   52 | 	boolean isPattern(String location) {
   53 | 		return StringUtils.hasLength(location) && location.contains("*");
   54 | 	}
   55 | 
   56 | 	/**
   57 | 	 * Get a single resource from a non-pattern location.
   58 | 	 * @param location the location
   59 | 	 * @return the resource
   60 | 	 * @see #isPattern(String)
   61 | 	 */
   62 | 	Resource getResource(String location) {
   63 | 		validateNonPattern(location);
   64 | 		location = StringUtils.cleanPath(location);
   65 | 		if (!ResourceUtils.isUrl(location)) {
   66 | 			location = ResourceUtils.FILE_URL_PREFIX + location;
   67 | 		}
   68 | 		return this.resourceLoader.getResource(location);
   69 | 	}
   70 | 
   71 | 	private void validateNonPattern(String location) {
   72 | 		Assert.state(!isPattern(location), () -> String.format("Location '%s' must not be a pattern", location));
   73 | 	}
   74 | 
   75 | 	/**
   76 | 	 * Get a multiple resources from a location pattern.
   77 | 	 * @param location the location pattern
   78 | 	 * @param type the type of resource to return
   79 | 	 * @return the resources
   80 | 	 * @see #isPattern(String)
   81 | 	 */
   82 | 	Resource[] getResources(String location, ResourceType type) {
   83 | 		validatePattern(location, type);
   84 | 		String directoryPath = location.substring(0, location.indexOf("*/"));
   85 | 		String fileName = location.substring(location.lastIndexOf("/") + 1);
   86 | 		Resource resource = getResource(directoryPath);
   87 | 		if (!resource.exists()) {
   88 | 			return EMPTY_RESOURCES;
   89 | 		}
   90 | 		File file = getFile(location, resource);
   91 | 		if (!file.isDirectory()) {
   92 | 			return EMPTY_RESOURCES;
   93 | 		}
   94 | 		File[] subDirectories = file.listFiles(this::isVisibleDirectory);
   95 | 		if (subDirectories == null) {
   96 | 			return EMPTY_RESOURCES;
   97 | 		}
   98 | 		Arrays.sort(subDirectories, FILE_PATH_COMPARATOR);
   99 | 		if (type == ResourceType.DIRECTORY) {
  100 | 			return Arrays.stream(subDirectories).map(FileSystemResource::new).toArray(Resource[]::new);
  101 | 		}
  102 | 		List<Resource> resources = new ArrayList<>();
  103 | 		FilenameFilter filter = (dir, name) -> name.equals(fileName);
  104 | 		for (File subDirectory : subDirectories) {
  105 | 			File[] files = subDirectory.listFiles(filter);
  106 | 			if (files != null) {
  107 | 				Arrays.sort(files, FILE_NAME_COMPARATOR);
  108 | 				Arrays.stream(files).map(FileSystemResource::new).forEach(resources::add);
  109 | 			}
  110 | 		}
  111 | 		return resources.toArray(EMPTY_RESOURCES);
  112 | 	}
  113 | 
  114 | 	private void validatePattern(String location, ResourceType type) {
  115 | 		Assert.state(isPattern(location), () -> String.format("Location '%s' must be a pattern", location));
  116 | 		Assert.state(!location.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX),
  117 | 				() -> String.format("Location '%s' cannot use classpath wildcards", location));
  118 | 		Assert.state(StringUtils.countOccurrencesOf(location, "*") == 1,
  119 | 				() -> String.format("Location '%s' cannot contain multiple wildcards", location));
  120 | 		String directoryPath = (type != ResourceType.DIRECTORY) ? location.substring(0, location.lastIndexOf("/") + 1)
  121 | 				: location;
  122 | 		Assert.state(directoryPath.endsWith("*/"), () -> String.format("Location '%s' must end with '*/'", location));
  123 | 	}
  124 | 
  125 | 	private File getFile(String patternLocation, Resource resource) {
  126 | 		try {
  127 | 			return resource.getFile();
  128 | 		}
  129 | 		catch (Exception ex) {
  130 | 			throw new IllegalStateException(
  131 | 					"Unable to load config data resource from pattern '" + patternLocation + "'", ex);
  132 | 		}
  133 | 	}
  134 | 
  135 | 	private boolean isVisibleDirectory(File file) {
  136 | 		return file.isDirectory() && !file.getName().startsWith("..");
  137 | 	}
  138 | 
  139 | 	/**
  140 | 	 * Resource types that can be returned.
  141 | 	 */
  142 | 	enum ResourceType {
  143 | 
  144 | 		/**
  145 | 		 * Return file resources.
  146 | 		 */
  147 | 		FILE,
  148 | 
  149 | 		/**
  150 | 		 * Return directory resources.
  151 | 		 */
  152 | 		DIRECTORY
  153 | 
  154 | 	}
  155 | 
  156 | }
```
