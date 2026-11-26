<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-core:7.1.0 | path: org/springframework/core/io/support/PathMatchingResourcePatternResolver.java -->
<!-- 正文引用：[[源码:PathMatchingResourcePatternResolver.java#L起-L止]] ；代码块头部：(src: PathMatchingResourcePatternResolver.java#L起-L止) -->

# PathMatchingResourcePatternResolver.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-core:7.1.0` |
| 路径 | `org/springframework/core/io/support/PathMatchingResourcePatternResolver.java` |
| 行数 | 1346 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-core/7.1.0/spring-core-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T13:52:12.893Z |

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
   17 | package org.springframework.core.io.support;
   18 | 
   19 | import java.io.File;
   20 | import java.io.FileNotFoundException;
   21 | import java.io.IOException;
   22 | import java.io.UncheckedIOException;
   23 | import java.lang.module.ModuleFinder;
   24 | import java.lang.module.ModuleReader;
   25 | import java.lang.module.ResolvedModule;
   26 | import java.lang.reflect.InvocationHandler;
   27 | import java.lang.reflect.Method;
   28 | import java.net.JarURLConnection;
   29 | import java.net.MalformedURLException;
   30 | import java.net.URI;
   31 | import java.net.URISyntaxException;
   32 | import java.net.URL;
   33 | import java.net.URLClassLoader;
   34 | import java.net.URLConnection;
   35 | import java.nio.file.FileSystemNotFoundException;
   36 | import java.nio.file.FileSystems;
   37 | import java.nio.file.FileVisitOption;
   38 | import java.nio.file.Files;
   39 | import java.nio.file.NoSuchFileException;
   40 | import java.nio.file.Path;
   41 | import java.util.Collections;
   42 | import java.util.Enumeration;
   43 | import java.util.HashSet;
   44 | import java.util.LinkedHashSet;
   45 | import java.util.Map;
   46 | import java.util.NavigableSet;
   47 | import java.util.Objects;
   48 | import java.util.Set;
   49 | import java.util.StringTokenizer;
   50 | import java.util.TreeSet;
   51 | import java.util.concurrent.ConcurrentHashMap;
   52 | import java.util.function.Predicate;
   53 | import java.util.jar.Attributes;
   54 | import java.util.jar.Attributes.Name;
   55 | import java.util.jar.JarEntry;
   56 | import java.util.jar.JarFile;
   57 | import java.util.jar.Manifest;
   58 | import java.util.stream.Collectors;
   59 | import java.util.stream.Stream;
   60 | import java.util.zip.ZipException;
   61 | 
   62 | import org.apache.commons.logging.Log;
   63 | import org.apache.commons.logging.LogFactory;
   64 | import org.jspecify.annotations.Nullable;
   65 | 
   66 | import org.springframework.core.NativeDetector;
   67 | import org.springframework.core.io.DefaultResourceLoader;
   68 | import org.springframework.core.io.FileSystemResource;
   69 | import org.springframework.core.io.Resource;
   70 | import org.springframework.core.io.ResourceLoader;
   71 | import org.springframework.core.io.UrlResource;
   72 | import org.springframework.core.io.VfsResource;
   73 | import org.springframework.util.AntPathMatcher;
   74 | import org.springframework.util.Assert;
   75 | import org.springframework.util.ClassUtils;
   76 | import org.springframework.util.PathMatcher;
   77 | import org.springframework.util.ReflectionUtils;
   78 | import org.springframework.util.ResourceUtils;
   79 | import org.springframework.util.StringUtils;
   80 | 
   81 | /**
   82 |  * A {@link ResourcePatternResolver} implementation that is able to resolve a
   83 |  * specified resource location path into one or more matching Resources.
   84 |  *
   85 |  * <p>The source path may be a simple path which has a one-to-one mapping to a
   86 |  * target {@link org.springframework.core.io.Resource}, or alternatively may
   87 |  * contain the special "{@code classpath*:}" prefix and/or internal Ant-style
   88 |  * path patterns (matched using Spring's {@link AntPathMatcher} utility). Both
   89 |  * of the latter are effectively wildcards.
   90 |  *
   91 |  * <h3>No Wildcards</h3>
   92 |  *
   93 |  * <p>In the simple case, if the specified location path does not start with the
   94 |  * {@code "classpath*:}" prefix and does not contain a {@link PathMatcher}
   95 |  * pattern, this resolver will simply return a single resource via a
   96 |  * {@code getResource()} call on the underlying {@code ResourceLoader}.
   97 |  * Examples are real URLs such as "{@code file:C:/context.xml}", pseudo-URLs
   98 |  * such as "{@code classpath:/context.xml}", and simple unprefixed paths
   99 |  * such as "{@code /WEB-INF/context.xml}". The latter will resolve in a
  100 |  * fashion specific to the underlying {@code ResourceLoader} (for example,
  101 |  * {@code ServletContextResource} for a {@code WebApplicationContext}).
  102 |  *
  103 |  * <h3>Ant-style Patterns</h3>
  104 |  *
  105 |  * <p>When the path location contains an Ant-style pattern, for example:
  106 |  * <pre class="code">
  107 |  * /WEB-INF/*-context.xml
  108 |  * com/example/**&#47;applicationContext.xml
  109 |  * file:C:/some/path/*-context.xml
  110 |  * classpath:com/example/**&#47;applicationContext.xml</pre>
  111 |  * the resolver follows a more complex but defined procedure to try to resolve
  112 |  * the wildcard. It produces a {@code Resource} for the path up to the last
  113 |  * non-wildcard segment and obtains a {@code URL} from it. If this URL is not a
  114 |  * "{@code jar:}" URL or container-specific variant (for example, "{@code zip:}" in WebLogic,
  115 |  * "{@code wsjar}" in WebSphere", etc.), then the root directory of the filesystem
  116 |  * associated with the URL is obtained and used to resolve the wildcards by walking
  117 |  * the filesystem. In the case of a jar URL, the resolver either gets a
  118 |  * {@code java.net.JarURLConnection} from it, or manually parses the jar URL, and
  119 |  * then traverses the contents of the jar file, to resolve the wildcards.
  120 |  *
  121 |  * <h3>Implications on Portability</h3>
  122 |  *
  123 |  * <p>If the specified path is already a file URL (either explicitly, or
  124 |  * implicitly because the base {@code ResourceLoader} is a filesystem one),
  125 |  * then wildcarding is guaranteed to work in a completely portable fashion.
  126 |  *
  127 |  * <p>If the specified path is a class path location, then the resolver must
  128 |  * obtain the last non-wildcard path segment URL via a
  129 |  * {@code Classloader.getResource()} call. Since this is just a
  130 |  * node of the path (not the file at the end) it is actually undefined
  131 |  * (in the ClassLoader Javadocs) exactly what sort of URL is returned in
  132 |  * this case. In practice, it is usually a {@code java.io.File} representing
  133 |  * the directory, where the class path resource resolves to a filesystem
  134 |  * location, or a jar URL of some sort, where the class path resource resolves
  135 |  * to a jar location. Still, there is a portability concern on this operation.
  136 |  *
  137 |  * <p>If a jar URL is obtained for the last non-wildcard segment, the resolver
  138 |  * must be able to get a {@code java.net.JarURLConnection} from it, or
  139 |  * manually parse the jar URL, to be able to walk the contents of the jar
  140 |  * and resolve the wildcard. This will work in most environments but will
  141 |  * fail in others, and it is strongly recommended that the wildcard
  142 |  * resolution of resources coming from jars be thoroughly tested in your
  143 |  * specific environment before you rely on it.
  144 |  *
  145 |  * <h3>{@code classpath*:} Prefix</h3>
  146 |  *
  147 |  * <p>There is special support for retrieving multiple class path resources with
  148 |  * the same name, via the "{@code classpath*:}" prefix. For example,
  149 |  * "{@code classpath*:META-INF/beans.xml}" will find all "META-INF/beans.xml"
  150 |  * files in the class path, be it in "classes" directories or in JAR files.
  151 |  * This is particularly useful for autodetecting config files of the same name
  152 |  * at the same location within each jar file. Internally, this happens via a
  153 |  * {@code ClassLoader.getResources()} call, and is completely portable.
  154 |  *
  155 |  * <p>The "{@code classpath*:}" prefix can also be combined with a {@code PathMatcher}
  156 |  * pattern in the rest of the location path &mdash; for example,
  157 |  * "{@code classpath*:META-INF/*-beans.xml"}. In this case, the resolution strategy
  158 |  * is fairly simple: a {@code ClassLoader.getResources()} call is used on the last
  159 |  * non-wildcard path segment to get all the matching resources in the class loader
  160 |  * hierarchy, and then off each resource the same {@code PathMatcher} resolution
  161 |  * strategy described above is used for the wildcard sub pattern.
  162 |  *
  163 |  * <h3>Other Notes</h3>
  164 |  *
  165 |  * <p>As of Spring Framework 6.0, if {@link #getResources(String)} is invoked with
  166 |  * a location pattern using the "{@code classpath*:}" prefix it will first search
  167 |  * all modules in the {@linkplain ModuleLayer#boot() boot layer}, excluding
  168 |  * {@linkplain ModuleFinder#ofSystem() system modules}. It will then search the
  169 |  * class path using {@link ClassLoader} APIs as described previously and return the
  170 |  * combined results. Consequently, some of the limitations of class path searches
  171 |  * may not apply when applications are deployed as modules.
  172 |  *
  173 |  * <p><b>WARNING:</b> Note that "{@code classpath*:}" when combined with
  174 |  * Ant-style patterns will only work reliably with at least one root directory
  175 |  * before the pattern starts, unless the actual target files reside in the file
  176 |  * system. This means that a pattern like "{@code classpath*:*.xml}" will
  177 |  * <i>not</i> retrieve files from the root of jar files but rather only from the
  178 |  * root of expanded directories. This originates from a limitation in the JDK's
  179 |  * {@code ClassLoader.getResources()} method which only returns file system
  180 |  * locations for a passed-in empty String (indicating potential roots to search).
  181 |  * This {@code ResourcePatternResolver} implementation tries to mitigate the
  182 |  * jar root lookup limitation through {@link URLClassLoader} introspection and
  183 |  * "{@code java.class.path}" manifest evaluation; however, without portability
  184 |  * guarantees.
  185 |  *
  186 |  * <p><b>WARNING:</b> Ant-style patterns with "{@code classpath:}" resources are not
  187 |  * guaranteed to find matching resources if the base package to search is available
  188 |  * in multiple class path locations. This is because a resource such as
  189 |  * <pre class="code">
  190 |  *   com/example/package1/service-context.xml</pre>
  191 |  * may exist in only one class path location, but when a location pattern such as
  192 |  * <pre class="code">
  193 |  *   classpath:com/example/**&#47;service-context.xml</pre>
  194 |  * is used to try to resolve it, the resolver will work off the (first) URL
  195 |  * returned by {@code getResource("com/example")}. If the {@code com/example} base
  196 |  * package node exists in multiple class path locations, the actual desired resource
  197 |  * may not be present under the {@code com/example} base package in the first URL.
  198 |  * Therefore, preferably, use "{@code classpath*:}" with the same Ant-style pattern
  199 |  * in such a case, which will search <i>all</i> class path locations that contain
  200 |  * the base package.
  201 |  *
  202 |  * @author Juergen Hoeller
  203 |  * @author Colin Sampaleanu
  204 |  * @author Marius Bogoevici
  205 |  * @author Costin Leau
  206 |  * @author Phillip Webb
  207 |  * @author Sam Brannen
  208 |  * @author Sebastien Deleuze
  209 |  * @author Dave Syer
  210 |  * @since 1.0.2
  211 |  * @see #CLASSPATH_ALL_URL_PREFIX
  212 |  * @see org.springframework.util.AntPathMatcher
  213 |  * @see org.springframework.core.io.ResourceLoader#getResource(String)
  214 |  * @see ClassLoader#getResources(String)
  215 |  */
  216 | public class PathMatchingResourcePatternResolver implements ResourcePatternResolver {
  217 | 
  218 | 	private static final Resource[] EMPTY_RESOURCE_ARRAY = {};
  219 | 
  220 | 	private static final Log logger = LogFactory.getLog(PathMatchingResourcePatternResolver.class);
  221 | 
  222 | 	/**
  223 | 	 * {@link Set} of {@linkplain ModuleFinder#ofSystem() system module} names.
  224 | 	 * @since 6.0
  225 | 	 * @see #isNotSystemModule
  226 | 	 */
  227 | 	private static final Set<String> systemModuleNames = NativeDetector.inNativeImage() ? Collections.emptySet() :
  228 | 			ModuleFinder.ofSystem().findAll().stream()
  229 | 					.map(moduleReference -> moduleReference.descriptor().name())
  230 | 					.collect(Collectors.toSet());
  231 | 
  232 | 	/**
  233 | 	 * {@link Predicate} that tests whether the supplied {@link ResolvedModule}
  234 | 	 * is not a {@linkplain ModuleFinder#ofSystem() system module}.
  235 | 	 * @since 6.0
  236 | 	 * @see #systemModuleNames
  237 | 	 */
  238 | 	private static final Predicate<ResolvedModule> isNotSystemModule =
  239 | 			resolvedModule -> !systemModuleNames.contains(resolvedModule.name());
  240 | 
  241 | 	private static @Nullable Method equinoxResolveMethod;
  242 | 
  243 | 	static {
  244 | 		try {
  245 | 			// Detect Equinox OSGi (for example, on WebSphere 6.1)
  246 | 			Class<?> fileLocatorClass = ClassUtils.forName("org.eclipse.core.runtime.FileLocator",
  247 | 					PathMatchingResourcePatternResolver.class.getClassLoader());
  248 | 			equinoxResolveMethod = fileLocatorClass.getMethod("resolve", URL.class);
  249 | 			logger.trace("Found Equinox FileLocator for OSGi bundle URL resolution");
  250 | 		}
  251 | 		catch (Throwable ex) {
  252 | 			equinoxResolveMethod = null;
  253 | 		}
  254 | 	}
  255 | 
  256 | 
  257 | 	private final ResourceLoader resourceLoader;
  258 | 
  259 | 	private PathMatcher pathMatcher = new AntPathMatcher();
  260 | 
  261 | 	@Nullable
  262 | 	private Boolean useCaches;
  263 | 
  264 | 	private final Map<String, Resource[]> rootDirCache = new ConcurrentHashMap<>();
  265 | 
  266 | 	private final Map<String, NavigableSet<String>> jarEntriesCache = new ConcurrentHashMap<>();
  267 | 
  268 | 	private volatile @Nullable Set<ClassPathManifestEntry> manifestEntriesCache;
  269 | 
  270 | 
  271 | 	/**
  272 | 	 * Create a {@code PathMatchingResourcePatternResolver} with a
  273 | 	 * {@link DefaultResourceLoader}.
  274 | 	 * <p>ClassLoader access will happen via the thread context class loader.
  275 | 	 * @see DefaultResourceLoader
  276 | 	 */
  277 | 	public PathMatchingResourcePatternResolver() {
  278 | 		this.resourceLoader = new DefaultResourceLoader();
  279 | 	}
  280 | 
  281 | 	/**
  282 | 	 * Create a {@code PathMatchingResourcePatternResolver} with the supplied
  283 | 	 * {@link ResourceLoader}.
  284 | 	 * <p>ClassLoader access will happen via the thread context class loader.
  285 | 	 * @param resourceLoader the {@code ResourceLoader} to load root directories
  286 | 	 * and actual resources with
  287 | 	 */
  288 | 	public PathMatchingResourcePatternResolver(ResourceLoader resourceLoader) {
  289 | 		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
  290 | 		this.resourceLoader = resourceLoader;
  291 | 	}
  292 | 
  293 | 	/**
  294 | 	 * Create a {@code PathMatchingResourcePatternResolver} with a
  295 | 	 * {@link DefaultResourceLoader} and the supplied {@link ClassLoader}.
  296 | 	 * @param classLoader the ClassLoader to load class path resources with,
  297 | 	 * or {@code null} for using the thread context class loader
  298 | 	 * at the time of actual resource access
  299 | 	 * @see org.springframework.core.io.DefaultResourceLoader
  300 | 	 */
  301 | 	public PathMatchingResourcePatternResolver(@Nullable ClassLoader classLoader) {
  302 | 		this.resourceLoader = new DefaultResourceLoader(classLoader);
  303 | 	}
  304 | 
  305 | 
  306 | 	/**
  307 | 	 * Return the {@link ResourceLoader} that this pattern resolver works with.
  308 | 	 */
  309 | 	public ResourceLoader getResourceLoader() {
  310 | 		return this.resourceLoader;
  311 | 	}
  312 | 
  313 | 	@Override
  314 | 	public @Nullable ClassLoader getClassLoader() {
  315 | 		return getResourceLoader().getClassLoader();
  316 | 	}
  317 | 
  318 | 	/**
  319 | 	 * Set the {@link PathMatcher} implementation to use for this
  320 | 	 * resource pattern resolver.
  321 | 	 * <p>Default is {@link AntPathMatcher}.
  322 | 	 * @see AntPathMatcher
  323 | 	 */
  324 | 	public void setPathMatcher(PathMatcher pathMatcher) {
  325 | 		Assert.notNull(pathMatcher, "PathMatcher must not be null");
  326 | 		this.pathMatcher = pathMatcher;
  327 | 	}
  328 | 
  329 | 	/**
  330 | 	 * Return the {@link PathMatcher} that this resource pattern resolver uses.
  331 | 	 */
  332 | 	public PathMatcher getPathMatcher() {
  333 | 		return this.pathMatcher;
  334 | 	}
  335 | 
  336 | 	/**
  337 | 	 * Specify whether this resolver should use jar caches. Default is {@code true}.
  338 | 	 * <p>Switch this flag to {@code false} in order to avoid any jar caching, at
  339 | 	 * the {@link JarURLConnection} level as well as within this resolver instance.
  340 | 	 * <p>Note that {@link JarURLConnection#setDefaultUseCaches} can be turned off
  341 | 	 * independently. This resolver-level setting is designed to only enforce
  342 | 	 * {@code JarURLConnection#setUseCaches(true/false)} if necessary but otherwise
  343 | 	 * leaves the JVM-level default in place (if this setter has not been called).
  344 | 	 * <p>As of 6.2.10, this setting propagates to {@link UrlResource#setUseCaches}.
  345 | 	 * @since 6.1.19
  346 | 	 * @see JarURLConnection#setUseCaches
  347 | 	 * @see UrlResource#setUseCaches
  348 | 	 * @see #clearCache()
  349 | 	 */
  350 | 	public void setUseCaches(boolean useCaches) {
  351 | 		this.useCaches = useCaches;
  352 | 	}
  353 | 
  354 | 
  355 | 	@Override
  356 | 	public Resource getResource(String location) {
  357 | 		Resource resource = getResourceLoader().getResource(location);
  358 | 		if (this.useCaches != null && resource instanceof UrlResource urlResource) {
  359 | 			urlResource.setUseCaches(this.useCaches);
  360 | 		}
  361 | 		return resource;
  362 | 	}
  363 | 
  364 | 	@Override
  365 | 	public Resource[] getResources(String locationPattern) throws IOException {
  366 | 		Assert.notNull(locationPattern, "Location pattern must not be null");
  367 | 		if (locationPattern.startsWith(CLASSPATH_ALL_URL_PREFIX)) {
  368 | 			// a class path resource (multiple resources for same name possible)
  369 | 			String locationPatternWithoutPrefix = locationPattern.substring(CLASSPATH_ALL_URL_PREFIX.length());
  370 | 			// Search the module path first.
  371 | 			Set<Resource> resources = findAllModulePathResources(locationPatternWithoutPrefix);
  372 | 			// Search the class path next.
  373 | 			if (getPathMatcher().isPattern(locationPatternWithoutPrefix)) {
  374 | 				// a class path resource pattern
  375 | 				Collections.addAll(resources, findPathMatchingResources(locationPattern));
  376 | 			}
  377 | 			else {
  378 | 				// all class path resources with the given name
  379 | 				Collections.addAll(resources, findAllClassPathResources(locationPatternWithoutPrefix));
  380 | 			}
  381 | 			return resources.toArray(EMPTY_RESOURCE_ARRAY);
  382 | 		}
  383 | 		else {
  384 | 			// Generally only look for a pattern after a prefix here,
  385 | 			// and on Tomcat only after the "*/" separator for its "war:" protocol.
  386 | 			int prefixEnd = (locationPattern.startsWith("war:") ? locationPattern.indexOf("*/") + 1 :
  387 | 					locationPattern.indexOf(':') + 1);
  388 | 			if (getPathMatcher().isPattern(locationPattern.substring(prefixEnd))) {
  389 | 				// a file pattern
  390 | 				return findPathMatchingResources(locationPattern);
  391 | 			}
  392 | 			else {
  393 | 				// a single resource with the given name
  394 | 				return new Resource[] {getResource(locationPattern)};
  395 | 			}
  396 | 		}
  397 | 	}
  398 | 
  399 | 	/**
  400 | 	 * Clear the local resource cache, removing all cached classpath/jar structures.
  401 | 	 * @since 6.2
  402 | 	 */
  403 | 	public void clearCache() {
  404 | 		this.rootDirCache.clear();
  405 | 		this.jarEntriesCache.clear();
  406 | 		this.manifestEntriesCache = null;
  407 | 	}
  408 | 
  409 | 
  410 | 	/**
  411 | 	 * Find all class location resources with the given location via the ClassLoader.
  412 | 	 * <p>Delegates to {@link #doFindAllClassPathResources(String)}.
  413 | 	 * @param location the absolute path within the class path
  414 | 	 * @return the result as Resource array
  415 | 	 * @throws IOException in case of I/O errors
  416 | 	 * @see java.lang.ClassLoader#getResources
  417 | 	 * @see #convertClassLoaderURL
  418 | 	 */
  419 | 	protected Resource[] findAllClassPathResources(String location) throws IOException {
  420 | 		String path = stripLeadingSlash(location);
  421 | 		Set<Resource> result = doFindAllClassPathResources(path);
  422 | 		if (logger.isTraceEnabled()) {
  423 | 			logger.trace("Resolved class path location [" + path + "] to resources " + result);
  424 | 		}
  425 | 		return result.toArray(EMPTY_RESOURCE_ARRAY);
  426 | 	}
  427 | 
  428 | 	/**
  429 | 	 * Find all class path resources with the given path via the configured
  430 | 	 * {@link #getClassLoader() ClassLoader}.
  431 | 	 * <p>Called by {@link #findAllClassPathResources(String)}.
  432 | 	 * @param path the absolute path within the class path (never a leading slash)
  433 | 	 * @return a mutable Set of matching Resource instances
  434 | 	 * @since 4.1.1
  435 | 	 */
  436 | 	protected Set<Resource> doFindAllClassPathResources(String path) throws IOException {
  437 | 		Set<Resource> result = new LinkedHashSet<>(16);
  438 | 		ClassLoader cl = getClassLoader();
  439 | 		Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
  440 | 		while (resourceUrls.hasMoreElements()) {
  441 | 			URL url = resourceUrls.nextElement();
  442 | 			result.add(convertClassLoaderURL(url));
  443 | 		}
  444 | 		if (!StringUtils.hasLength(path)) {
  445 | 			// The above result is likely to be incomplete, i.e. only containing file system references.
  446 | 			// We need to have pointers to each of the jar files on the class path as well...
  447 | 			addAllClassLoaderJarRoots(cl, result);
  448 | 		}
  449 | 		return result;
  450 | 	}
  451 | 
  452 | 	/**
  453 | 	 * Convert the given URL as returned from the configured
  454 | 	 * {@link #getClassLoader() ClassLoader} into a {@link Resource}, applying
  455 | 	 * to path lookups without a pattern (see {@link #findAllClassPathResources}).
  456 | 	 * <p>As of 6.0.5, the default implementation creates a {@link FileSystemResource}
  457 | 	 * in case of the "file" protocol or a {@link UrlResource} otherwise, matching
  458 | 	 * the outcome of pattern-based class path traversal in the same resource layout,
  459 | 	 * as well as matching the outcome of module path searches.
  460 | 	 * @param url a URL as returned from the configured ClassLoader
  461 | 	 * @return the corresponding Resource object
  462 | 	 * @see java.lang.ClassLoader#getResources
  463 | 	 * @see #doFindAllClassPathResources
  464 | 	 * @see #doFindPathMatchingFileResources
  465 | 	 */
  466 | 	@SuppressWarnings("deprecation")  // on JDK 20 (deprecated URL constructor)
  467 | 	protected Resource convertClassLoaderURL(URL url) {
  468 | 		if (ResourceUtils.URL_PROTOCOL_FILE.equals(url.getProtocol())) {
  469 | 			try {
  470 | 				// URI decoding for special characters such as spaces.
  471 | 				return new FileSystemResource(ResourceUtils.toURI(url).getSchemeSpecificPart());
  472 | 			}
  473 | 			catch (URISyntaxException ex) {
  474 | 				// Fallback for URLs that are not valid URIs (should hardly ever happen).
  475 | 				return new FileSystemResource(url.getFile());
  476 | 			}
  477 | 		}
  478 | 		else {
  479 | 			UrlResource resource = null;
  480 | 			String urlString = url.toString();
  481 | 			String cleanedPath = StringUtils.cleanPath(urlString);
  482 | 			if (!cleanedPath.equals(urlString)) {
  483 | 				// Prefer cleaned URL, aligned with UrlResource#createRelative(String)
  484 | 				try {
  485 | 					// Retain original URL instance, potentially including custom URLStreamHandler.
  486 | 					resource = new UrlResource(new URL(url, cleanedPath));
  487 | 				}
  488 | 				catch (MalformedURLException ex) {
  489 | 					// Fallback to regular URL construction below...
  490 | 				}
  491 | 			}
  492 | 			// Retain original URL instance, potentially including custom URLStreamHandler.
  493 | 			if (resource == null) {
  494 | 				resource = new UrlResource(url);
  495 | 			}
  496 | 			if (this.useCaches != null) {
  497 | 				resource.setUseCaches(this.useCaches);
  498 | 			}
  499 | 			return resource;
  500 | 		}
  501 | 	}
  502 | 
  503 | 	/**
  504 | 	 * Search all {@link URLClassLoader} URLs for jar file references and add each to the
  505 | 	 * given set of resources in the form of a pointer to the root of the jar file content.
  506 | 	 * @param classLoader the ClassLoader to search (including its ancestors)
  507 | 	 * @param result the set of resources to add jar roots to
  508 | 	 * @since 4.1.1
  509 | 	 */
  510 | 	protected void addAllClassLoaderJarRoots(@Nullable ClassLoader classLoader, Set<Resource> result) {
  511 | 		if (classLoader instanceof URLClassLoader urlClassLoader) {
  512 | 			try {
  513 | 				for (URL url : urlClassLoader.getURLs()) {
  514 | 					try {
  515 | 						UrlResource jarResource = (ResourceUtils.URL_PROTOCOL_JAR.equals(url.getProtocol()) ?
  516 | 								new UrlResource(url) :
  517 | 								new UrlResource(ResourceUtils.JAR_URL_PREFIX + url + ResourceUtils.JAR_URL_SEPARATOR));
  518 | 						if (this.useCaches != null) {
  519 | 							jarResource.setUseCaches(this.useCaches);
  520 | 						}
  521 | 						if (jarResource.exists()) {
  522 | 							result.add(jarResource);
  523 | 						}
  524 | 					}
  525 | 					catch (MalformedURLException ex) {
  526 | 						if (logger.isDebugEnabled()) {
  527 | 							logger.debug("Cannot search for matching files underneath [" + url +
  528 | 									"] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
  529 | 						}
  530 | 					}
  531 | 				}
  532 | 			}
  533 | 			catch (Exception ex) {
  534 | 				if (logger.isDebugEnabled()) {
  535 | 					logger.debug("Cannot introspect jar files since ClassLoader [" + classLoader +
  536 | 							"] does not support 'getURLs()': " + ex);
  537 | 				}
  538 | 			}
  539 | 		}
  540 | 
  541 | 		if (classLoader == ClassLoader.getSystemClassLoader()) {
  542 | 			// JAR "Class-Path" manifest header evaluation...
  543 | 			addClassPathManifestEntries(result);
  544 | 		}
  545 | 
  546 | 		if (classLoader != null) {
  547 | 			try {
  548 | 				// Hierarchy traversal...
  549 | 				addAllClassLoaderJarRoots(classLoader.getParent(), result);
  550 | 			}
  551 | 			catch (Exception ex) {
  552 | 				if (logger.isDebugEnabled()) {
  553 | 					logger.debug("Cannot introspect jar files in parent ClassLoader since [" + classLoader +
  554 | 							"] does not support 'getParent()': " + ex);
  555 | 				}
  556 | 			}
  557 | 		}
  558 | 	}
  559 | 
  560 | 	/**
  561 | 	 * Determine jar file references from {@code Class-Path} manifest entries (which
  562 | 	 * are added to the {@code java.class.path} JVM system property by the system
  563 | 	 * class loader) and add each to the given set of resources in the form of
  564 | 	 * a pointer to the root of the jar file content.
  565 | 	 * @param result the set of resources to add jar roots to
  566 | 	 * @since 4.3
  567 | 	 */
  568 | 	protected void addClassPathManifestEntries(Set<Resource> result) {
  569 | 		Set<ClassPathManifestEntry> entries = this.manifestEntriesCache;
  570 | 		if (entries == null) {
  571 | 			entries = getClassPathManifestEntries();
  572 | 			if (this.useCaches == null || this.useCaches) {
  573 | 				this.manifestEntriesCache = entries;
  574 | 			}
  575 | 		}
  576 | 		for (ClassPathManifestEntry entry : entries) {
  577 | 			if (!result.contains(entry.resource()) &&
  578 | 					(entry.alternative() != null && !result.contains(entry.alternative()))) {
  579 | 				result.add(entry.resource());
  580 | 			}
  581 | 		}
  582 | 	}
  583 | 
  584 | 	private Set<ClassPathManifestEntry> getClassPathManifestEntries() {
  585 | 		Set<ClassPathManifestEntry> manifestEntries = new LinkedHashSet<>();
  586 | 		Set<File> seen = new HashSet<>();
  587 | 		try {
  588 | 			String paths = System.getProperty("java.class.path");
  589 | 			for (String path : StringUtils.delimitedListToStringArray(paths, File.pathSeparator)) {
  590 | 				try {
  591 | 					File jar = new File(path).getAbsoluteFile();
  592 | 					if (jar.isFile() && seen.add(jar)) {
  593 | 						manifestEntries.add(ClassPathManifestEntry.of(jar, this.useCaches));
  594 | 						manifestEntries.addAll(getClassPathManifestEntriesFromJar(jar));
  595 | 					}
  596 | 				}
  597 | 				catch (MalformedURLException ex) {
  598 | 					if (logger.isDebugEnabled()) {
  599 | 						logger.debug("Cannot search for matching files underneath [" + path +
  600 | 								"] because it cannot be converted to a valid 'jar:' URL: " + ex.getMessage());
  601 | 					}
  602 | 				}
  603 | 			}
  604 | 			return Collections.unmodifiableSet(manifestEntries);
  605 | 		}
  606 | 		catch (Exception ex) {
  607 | 			if (logger.isDebugEnabled()) {
  608 | 				logger.debug("Failed to evaluate 'java.class.path' manifest entries: " + ex);
  609 | 			}
  610 | 			return Collections.emptySet();
  611 | 		}
  612 | 	}
  613 | 
  614 | 	@SuppressWarnings("deprecation")  // for deprecated URL constructor on JDK 20
  615 | 	private Set<ClassPathManifestEntry> getClassPathManifestEntriesFromJar(File jar) throws IOException {
  616 | 		URL base = jar.toURI().toURL();
  617 | 		File parent = jar.getAbsoluteFile().getParentFile();
  618 | 
  619 | 		try (JarFile jarFile = new JarFile(jar)) {
  620 | 			Manifest manifest = jarFile.getManifest();
  621 | 			Attributes attributes = (manifest != null ? manifest.getMainAttributes() : null);
  622 | 			String classPath = (attributes != null ? attributes.getValue(Name.CLASS_PATH) : null);
  623 | 
  624 | 			Set<ClassPathManifestEntry> manifestEntries = new LinkedHashSet<>();
  625 | 			if (StringUtils.hasLength(classPath)) {
  626 | 				StringTokenizer tokenizer = new StringTokenizer(classPath);
  627 | 				while (tokenizer.hasMoreTokens()) {
  628 | 					String path = tokenizer.nextToken();
  629 | 					if (path.indexOf(':') >= 0 && !"file".equalsIgnoreCase(new URL(base, path).getProtocol())) {
  630 | 						// See jdk.internal.loader.URLClassPath.JarLoader.tryResolveFile(URL, String)
  631 | 						continue;
  632 | 					}
  633 | 
  634 | 					// Handle absolute paths correctly: do not apply parent to absolute paths.
  635 | 					File pathFile = null;
  636 | 					boolean absolute = false;
  637 | 					if (path.startsWith(ResourceUtils.FILE_URL_PREFIX)) {
  638 | 						try {
  639 | 							pathFile = new File(ResourceUtils.toURI(path));
  640 | 							absolute = true;
  641 | 						}
  642 | 						catch (URISyntaxException | IllegalArgumentException ex) {
  643 | 							// Fall back to plain String constructor below.
  644 | 						}
  645 | 					}
  646 | 					if (pathFile == null) {
  647 | 						pathFile = new File(path);
  648 | 						absolute = pathFile.isAbsolute();
  649 | 					}
  650 | 					File candidate = (absolute ? pathFile : new File(parent, path));
  651 | 
  652 | 					// For relative paths, enforce security check: must be under parent.
  653 | 					// For absolute paths, just verify file exists (matching JVM behavior).
  654 | 					if (candidate.isFile() && (absolute ||
  655 | 							candidate.getCanonicalPath().contains(parent.getCanonicalPath()))) {
  656 | 						manifestEntries.add(ClassPathManifestEntry.of(candidate, this.useCaches));
  657 | 					}
  658 | 				}
  659 | 			}
  660 | 			return Collections.unmodifiableSet(manifestEntries);
  661 | 		}
  662 | 		catch (Exception ex) {
  663 | 			if (logger.isDebugEnabled()) {
  664 | 				logger.debug("Failed to load manifest entries from jar file '" + jar + "': " + ex);
  665 | 			}
  666 | 			return Collections.emptySet();
  667 | 		}
  668 | 	}
  669 | 
  670 | 	/**
  671 | 	 * Find all resources that match the given location pattern via the Ant-style
  672 | 	 * {@link #getPathMatcher() PathMatcher}.
  673 | 	 * <p>Supports resources in OSGi bundles, JBoss VFS, jar files, zip files,
  674 | 	 * and file systems.
  675 | 	 * @param locationPattern the location pattern to match
  676 | 	 * @return the result as Resource array
  677 | 	 * @throws IOException in case of I/O errors
  678 | 	 * @see #determineRootDir(String)
  679 | 	 * @see #resolveRootDirResource(Resource)
  680 | 	 * @see #isJarResource(Resource)
  681 | 	 * @see #doFindPathMatchingJarResources(Resource, URL, String)
  682 | 	 * @see #doFindPathMatchingFileResources(Resource, String)
  683 | 	 * @see org.springframework.util.PathMatcher
  684 | 	 */
  685 | 	protected Resource[] findPathMatchingResources(String locationPattern) throws IOException {
  686 | 		String rootDirPath = determineRootDir(locationPattern);
  687 | 		String subPattern = locationPattern.substring(rootDirPath.length());
  688 | 
  689 | 		// Look for pre-cached root dir resources, either a direct match or
  690 | 		// a match for a parent directory in the same classpath locations.
  691 | 		Resource[] rootDirResources = this.rootDirCache.get(rootDirPath);
  692 | 		String actualRootPath = null;
  693 | 		if (rootDirResources == null) {
  694 | 			// No direct match -> search for a common parent directory match
  695 | 			// (cached based on repeated searches in the same base location,
  696 | 			// in particular for different root directories in the same jar).
  697 | 			String commonPrefix = null;
  698 | 			String existingPath = null;
  699 | 			boolean commonUnique = true;
  700 | 			for (String path : this.rootDirCache.keySet()) {
  701 | 				String currentPrefix = null;
  702 | 				for (int i = 0; i < path.length(); i++) {
  703 | 					if (i == rootDirPath.length() || path.charAt(i) != rootDirPath.charAt(i)) {
  704 | 						currentPrefix = path.substring(0, path.lastIndexOf('/', i - 1) + 1);
  705 | 						break;
  706 | 					}
  707 | 				}
  708 | 				if (currentPrefix != null) {
  709 | 					if (checkPathWithinPackage(path.substring(currentPrefix.length()))) {
  710 | 						// A prefix match found, potentially to be turned into a common parent cache entry.
  711 | 						if (commonPrefix == null || !commonUnique || currentPrefix.length() > commonPrefix.length()) {
  712 | 							commonPrefix = currentPrefix;
  713 | 							existingPath = path;
  714 | 						}
  715 | 						else if (currentPrefix.equals(commonPrefix)) {
  716 | 							commonUnique = false;
  717 | 						}
  718 | 					}
  719 | 				}
  720 | 				else if (actualRootPath == null || path.length() > actualRootPath.length()) {
  721 | 					// A direct match found for a parent directory -> use it.
  722 | 					rootDirResources = this.rootDirCache.get(path);
  723 | 					actualRootPath = path;
  724 | 				}
  725 | 			}
  726 | 			if (rootDirResources == null && StringUtils.hasLength(commonPrefix)) {
  727 | 				// Try common parent directory as long as it points to the same classpath locations.
  728 | 				rootDirResources = getResources(commonPrefix);
  729 | 				Resource[] existingResources = this.rootDirCache.get(existingPath);
  730 | 				if (existingResources != null && rootDirResources.length == existingResources.length) {
  731 | 					// Replace existing subdirectory cache entry with common parent directory,
  732 | 					// avoiding repeated determination of root directories in the same jar.
  733 | 					this.rootDirCache.remove(existingPath);
  734 | 					this.rootDirCache.put(commonPrefix, rootDirResources);
  735 | 					actualRootPath = commonPrefix;
  736 | 				}
  737 | 				else if (commonPrefix.equals(rootDirPath)) {
  738 | 					// The identified common directory is equal to the currently requested path ->
  739 | 					// worth caching specifically, even if it cannot replace the existing sub-entry.
  740 | 					this.rootDirCache.put(rootDirPath, rootDirResources);
  741 | 				}
  742 | 				else {
  743 | 					// Mismatch: parent directory points to more classpath locations.
  744 | 					rootDirResources = null;
  745 | 				}
  746 | 			}
  747 | 			if (rootDirResources == null) {
  748 | 				// Lookup for specific directory, creating a cache entry for it.
  749 | 				rootDirResources = getResources(rootDirPath);
  750 | 				if (this.useCaches == null || this.useCaches) {
  751 | 					this.rootDirCache.put(rootDirPath, rootDirResources);
  752 | 				}
  753 | 			}
  754 | 		}
  755 | 
  756 | 		Set<Resource> result = new LinkedHashSet<>(64);
  757 | 		for (Resource rootDirResource : rootDirResources) {
  758 | 			if (actualRootPath != null && actualRootPath.length() < rootDirPath.length()) {
  759 | 				// Create sub-resource for requested sub-location from cached common root directory.
  760 | 				rootDirResource = rootDirResource.createRelative(rootDirPath.substring(actualRootPath.length()));
  761 | 			}
  762 | 			rootDirResource = resolveRootDirResource(rootDirResource);
  763 | 			URL rootDirUrl = rootDirResource.getURL();
  764 | 			if (equinoxResolveMethod != null && rootDirUrl.getProtocol().startsWith("bundle")) {
  765 | 				URL resolvedUrl = (URL) ReflectionUtils.invokeMethod(equinoxResolveMethod, null, rootDirUrl);
  766 | 				if (resolvedUrl != null) {
  767 | 					rootDirUrl = resolvedUrl;
  768 | 				}
  769 | 				UrlResource urlResource = new UrlResource(rootDirUrl);
  770 | 				if (this.useCaches != null) {
  771 | 					urlResource.setUseCaches(this.useCaches);
  772 | 				}
  773 | 				rootDirResource = urlResource;
  774 | 			}
  775 | 			if (rootDirUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
  776 | 				result.addAll(VfsResourceMatchingDelegate.findMatchingResources(rootDirUrl, subPattern, getPathMatcher()));
  777 | 			}
  778 | 			else if (ResourceUtils.isJarURL(rootDirUrl) || isJarResource(rootDirResource)) {
  779 | 				result.addAll(doFindPathMatchingJarResources(rootDirResource, rootDirUrl, subPattern));
  780 | 			}
  781 | 			else {
  782 | 				result.addAll(doFindPathMatchingFileResources(rootDirResource, subPattern));
  783 | 			}
  784 | 		}
  785 | 		if (logger.isTraceEnabled()) {
  786 | 			logger.trace("Resolved location pattern [" + locationPattern + "] to resources " + result);
  787 | 		}
  788 | 		return result.toArray(EMPTY_RESOURCE_ARRAY);
  789 | 	}
  790 | 
  791 | 	/**
  792 | 	 * Determine the root directory for the given location.
  793 | 	 * <p>Used for determining the starting point for file matching, resolving the
  794 | 	 * root directory location to be passed into {@link #getResources(String)},
  795 | 	 * with the remainder of the location to be used as the sub pattern.
  796 | 	 * <p>Will return "/WEB-INF/" for the location "/WEB-INF/*.xml", for example.
  797 | 	 * @param location the location to check
  798 | 	 * @return the part of the location that denotes the root directory
  799 | 	 * @see #findPathMatchingResources(String)
  800 | 	 */
  801 | 	protected String determineRootDir(String location) {
  802 | 		int prefixEnd = location.indexOf(':') + 1;
  803 | 		int rootDirEnd = location.length();
  804 | 		while (rootDirEnd > prefixEnd && getPathMatcher().isPattern(location.substring(prefixEnd, rootDirEnd))) {
  805 | 			rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
  806 | 		}
  807 | 		if (rootDirEnd == 0) {
  808 | 			rootDirEnd = prefixEnd;
  809 | 		}
  810 | 		return location.substring(0, rootDirEnd);
  811 | 	}
  812 | 
  813 | 	/**
  814 | 	 * Resolve the supplied root directory resource for path matching.
  815 | 	 * <p>By default, {@link #findPathMatchingResources(String)} resolves Equinox
  816 | 	 * OSGi "bundleresource:" and "bundleentry:" URLs into standard jar file URLs
  817 | 	 * that will be traversed using Spring's standard jar file traversal algorithm.
  818 | 	 * <p>For any custom resolution, override this template method and replace the
  819 | 	 * supplied resource handle accordingly.
  820 | 	 * <p>The default implementation of this method returns the supplied resource
  821 | 	 * unmodified.
  822 | 	 * @param original the resource to resolve
  823 | 	 * @return the resolved resource (may be identical to the supplied resource)
  824 | 	 * @throws IOException in case of resolution failure
  825 | 	 * @see #findPathMatchingResources(String)
  826 | 	 */
  827 | 	protected Resource resolveRootDirResource(Resource original) throws IOException {
  828 | 		return original;
  829 | 	}
  830 | 
  831 | 	/**
  832 | 	 * Determine if the given resource handle indicates a jar resource that the
  833 | 	 * {@link #doFindPathMatchingJarResources} method can handle.
  834 | 	 * <p>{@link #findPathMatchingResources(String)} delegates to
  835 | 	 * {@link ResourceUtils#isJarURL(URL)} to determine whether the given URL
  836 | 	 * points to a resource in a jar file, and only invokes this method as a fallback.
  837 | 	 * <p>This template method therefore allows for detecting further kinds of
  838 | 	 * jar-like resources &mdash; for example, via {@code instanceof} checks on
  839 | 	 * the resource handle type.
  840 | 	 * <p>The default implementation of this method returns {@code false}.
  841 | 	 * @param resource the resource handle to check (usually the root directory
  842 | 	 * to start path matching from)
  843 | 	 * @return {@code true} if the given resource handle indicates a jar resource
  844 | 	 * @throws IOException in case of I/O errors
  845 | 	 * @see #findPathMatchingResources(String)
  846 | 	 * @see #doFindPathMatchingJarResources(Resource, URL, String)
  847 | 	 * @see org.springframework.util.ResourceUtils#isJarURL
  848 | 	 */
  849 | 	protected boolean isJarResource(Resource resource) throws IOException {
  850 | 		return false;
  851 | 	}
  852 | 
  853 | 	/**
  854 | 	 * Find all resources in jar files that match the given location pattern
  855 | 	 * via the Ant-style {@link #getPathMatcher() PathMatcher}.
  856 | 	 * @param rootDirResource the root directory as Resource
  857 | 	 * @param rootDirUrl the pre-resolved root directory URL
  858 | 	 * @param subPattern the sub pattern to match (below the root directory)
  859 | 	 * @return a mutable Set of matching Resource instances
  860 | 	 * @throws IOException in case of I/O errors
  861 | 	 * @since 4.3
  862 | 	 * @see java.net.JarURLConnection
  863 | 	 * @see org.springframework.util.PathMatcher
  864 | 	 */
  865 | 	protected Set<Resource> doFindPathMatchingJarResources(Resource rootDirResource, URL rootDirUrl, String subPattern)
  866 | 			throws IOException {
  867 | 
  868 | 		String jarFileUrl = null;
  869 | 		String rootEntryPath = "";
  870 | 
  871 | 		String urlFile = rootDirUrl.getFile();
  872 | 		int separatorIndex = urlFile.indexOf(ResourceUtils.WAR_URL_SEPARATOR);
  873 | 		if (separatorIndex == -1) {
  874 | 			separatorIndex = urlFile.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
  875 | 		}
  876 | 		if (separatorIndex >= 0) {
  877 | 			jarFileUrl = urlFile.substring(0, separatorIndex);
  878 | 			rootEntryPath = urlFile.substring(separatorIndex + 2);  // both separators are 2 chars
  879 | 			NavigableSet<String> entriesCache = this.jarEntriesCache.get(jarFileUrl);
  880 | 			if (entriesCache != null) {
  881 | 				Set<Resource> result = new LinkedHashSet<>(64);
  882 | 				// Clean root entry path to match jar entries format without "!" separators
  883 | 				rootEntryPath = rootEntryPath.replace(ResourceUtils.JAR_URL_SEPARATOR, "/");
  884 | 				// Search sorted entries from first entry with rootEntryPath prefix
  885 | 				boolean rootEntryPathFound = false;
  886 | 				for (String entryPath : entriesCache.tailSet(rootEntryPath, false)) {
  887 | 					if (!entryPath.startsWith(rootEntryPath)) {
  888 | 						// We are beyond the potential matches in the current TreeSet.
  889 | 						break;
  890 | 					}
  891 | 					rootEntryPathFound = true;
  892 | 					String relativePath = entryPath.substring(rootEntryPath.length());
  893 | 					if (getPathMatcher().match(subPattern, relativePath)) {
  894 | 						result.add(rootDirResource.createRelative(relativePath));
  895 | 					}
  896 | 				}
  897 | 				if (rootEntryPathFound) {
  898 | 					return result;
  899 | 				}
  900 | 			}
  901 | 		}
  902 | 
  903 | 		URLConnection con = rootDirUrl.openConnection();
  904 | 		JarFile jarFile;
  905 | 		boolean closeJarFile;
  906 | 
  907 | 		if (con instanceof JarURLConnection jarCon) {
  908 | 			// Should usually be the case for traditional JAR files.
  909 | 			if (this.useCaches != null) {
  910 | 				jarCon.setUseCaches(this.useCaches);
  911 | 			}
  912 | 			try {
  913 | 				jarFile = jarCon.getJarFile();
  914 | 				jarFileUrl = jarCon.getJarFileURL().toExternalForm();
  915 | 				JarEntry jarEntry = jarCon.getJarEntry();
  916 | 				rootEntryPath = (jarEntry != null ? jarEntry.getName() : "");
  917 | 				closeJarFile = !jarCon.getUseCaches();
  918 | 			}
  919 | 			catch (ZipException | FileNotFoundException | NoSuchFileException ex) {
  920 | 				// Happens in case of a non-jar file or in case of a cached root directory
  921 | 				// without the specific subdirectory present, respectively.
  922 | 				return Collections.emptySet();
  923 | 			}
  924 | 		}
  925 | 		else {
  926 | 			// No JarURLConnection -> need to resort to URL file parsing.
  927 | 			// We'll assume URLs of the format "jar:path!/entry", with the protocol
  928 | 			// being arbitrary as long as following the entry format.
  929 | 			// We'll also handle paths with and without leading "file:" prefix.
  930 | 			try {
  931 | 				if (jarFileUrl != null) {
  932 | 					jarFile = getJarFile(jarFileUrl);
  933 | 				}
  934 | 				else {
  935 | 					jarFile = new JarFile(urlFile);
  936 | 					jarFileUrl = urlFile;
  937 | 					rootEntryPath = "";
  938 | 				}
  939 | 				closeJarFile = true;
  940 | 			}
  941 | 			catch (ZipException ex) {
  942 | 				if (logger.isDebugEnabled()) {
  943 | 					logger.debug("Skipping invalid jar class path entry [" + urlFile + "]");
  944 | 				}
  945 | 				return Collections.emptySet();
  946 | 			}
  947 | 		}
  948 | 
  949 | 		try {
  950 | 			if (logger.isTraceEnabled()) {
  951 | 				logger.trace("Looking for matching resources in jar file [" + jarFileUrl + "]");
  952 | 			}
  953 | 			if (StringUtils.hasLength(rootEntryPath) && !rootEntryPath.endsWith("/")) {
  954 | 				// Root entry path must end with slash to allow for proper matching.
  955 | 				// The Sun JRE does not return a slash here, but BEA JRockit does.
  956 | 				rootEntryPath = rootEntryPath + "/";
  957 | 			}
  958 | 			Set<Resource> result = new LinkedHashSet<>(64);
  959 | 			NavigableSet<String> entriesCache = new TreeSet<>();
  960 | 			for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
  961 | 				entriesCache.add(entries.nextElement().getName());
  962 | 			}
  963 | 			for (String entryPath : entriesCache) {
  964 | 				if (entryPath.startsWith(rootEntryPath)) {
  965 | 					String relativePath = entryPath.substring(rootEntryPath.length());
  966 | 					if (getPathMatcher().match(subPattern, relativePath)) {
  967 | 						result.add(rootDirResource.createRelative(relativePath));
  968 | 					}
  969 | 				}
  970 | 			}
  971 | 			if (this.useCaches == null || this.useCaches) {
  972 | 				// Cache jar entries in TreeSet for efficient searching on re-encounter.
  973 | 				this.jarEntriesCache.put(jarFileUrl, entriesCache);
  974 | 			}
  975 | 			return result;
  976 | 		}
  977 | 		finally {
  978 | 			if (closeJarFile) {
  979 | 				jarFile.close();
  980 | 			}
  981 | 		}
  982 | 	}
  983 | 
  984 | 	/**
  985 | 	 * Resolve the given jar file URL into a JarFile object.
  986 | 	 */
  987 | 	protected JarFile getJarFile(String jarFileUrl) throws IOException {
  988 | 		if (jarFileUrl.startsWith(ResourceUtils.FILE_URL_PREFIX)) {
  989 | 			try {
  990 | 				return new JarFile(ResourceUtils.toURI(jarFileUrl).getSchemeSpecificPart());
  991 | 			}
  992 | 			catch (URISyntaxException ex) {
  993 | 				// Fallback for URLs that are not valid URIs (should hardly ever happen).
  994 | 				return new JarFile(jarFileUrl.substring(ResourceUtils.FILE_URL_PREFIX.length()));
  995 | 			}
  996 | 		}
  997 | 		else {
  998 | 			return new JarFile(jarFileUrl);
  999 | 		}
 1000 | 	}
 1001 | 
 1002 | 	/**
 1003 | 	 * Find all resources in the file system of the supplied root directory that
 1004 | 	 * match the given location sub pattern via the Ant-style {@link #getPathMatcher()
 1005 | 	 * PathMatcher}.
 1006 | 	 * @param rootDirResource the root directory as a Resource
 1007 | 	 * @param subPattern the sub pattern to match (below the root directory)
 1008 | 	 * @return a mutable Set of matching Resource instances
 1009 | 	 * @throws IOException in case of I/O errors
 1010 | 	 * @see org.springframework.util.PathMatcher
 1011 | 	 */
 1012 | 	protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern)
 1013 | 			throws IOException {
 1014 | 
 1015 | 		Set<Resource> result = new LinkedHashSet<>(64);
 1016 | 		URI rootDirUri;
 1017 | 		try {
 1018 | 			rootDirUri = rootDirResource.getURI();
 1019 | 		}
 1020 | 		catch (Exception ex) {
 1021 | 			if (logger.isWarnEnabled()) {
 1022 | 				logger.warn("Failed to resolve directory [%s] as URI: %s".formatted(rootDirResource, ex));
 1023 | 			}
 1024 | 			return result;
 1025 | 		}
 1026 | 
 1027 | 		Path rootPath = null;
 1028 | 		if (rootDirUri.isAbsolute() && !rootDirUri.isOpaque()) {
 1029 | 			// Prefer Path resolution from URI if possible
 1030 | 			try {
 1031 | 				try {
 1032 | 					rootPath = Path.of(rootDirUri);
 1033 | 				}
 1034 | 				catch (FileSystemNotFoundException ex) {
 1035 | 					// If the file system was not found, assume it's a custom file system that needs to be installed.
 1036 | 					FileSystems.newFileSystem(rootDirUri, Map.of(), ClassUtils.getDefaultClassLoader());
 1037 | 					rootPath = Path.of(rootDirUri);
 1038 | 				}
 1039 | 			}
 1040 | 			catch (Exception ex) {
 1041 | 				if (logger.isDebugEnabled()) {
 1042 | 					logger.debug("Failed to resolve %s in file system: %s".formatted(rootDirUri, ex));
 1043 | 				}
 1044 | 				// Fallback via Resource.getFile() below
 1045 | 			}
 1046 | 		}
 1047 | 
 1048 | 		if (rootPath == null) {
 1049 | 			// Resource.getFile() resolution as a fallback -
 1050 | 			// for custom URI formats and custom Resource implementations
 1051 | 			try {
 1052 | 				rootPath = Path.of(rootDirResource.getFile().getAbsolutePath());
 1053 | 			}
 1054 | 			catch (FileNotFoundException ex) {
 1055 | 				if (logger.isDebugEnabled()) {
 1056 | 					logger.debug("Cannot search for matching files underneath " + rootDirResource +
 1057 | 							" in the file system: " + ex.getMessage());
 1058 | 				}
 1059 | 				return result;
 1060 | 			}
 1061 | 			catch (Exception ex) {
 1062 | 				if (logger.isInfoEnabled()) {
 1063 | 					logger.info("Failed to resolve " + rootDirResource + " in the file system: " + ex);
 1064 | 				}
 1065 | 				return result;
 1066 | 			}
 1067 | 		}
 1068 | 
 1069 | 		if (!Files.exists(rootPath)) {
 1070 | 			if (logger.isDebugEnabled()) {
 1071 | 				logger.debug("Skipping search for files matching pattern [%s]: directory [%s] does not exist"
 1072 | 						.formatted(subPattern, rootPath.toAbsolutePath()));
 1073 | 			}
 1074 | 			return result;
 1075 | 		}
 1076 | 
 1077 | 		String rootDir = StringUtils.cleanPath(rootPath.toString());
 1078 | 		if (!rootDir.endsWith("/")) {
 1079 | 			rootDir += "/";
 1080 | 		}
 1081 | 
 1082 | 		Path rootPathForPattern = rootPath;
 1083 | 		String resourcePattern = rootDir + StringUtils.cleanPath(subPattern);
 1084 | 		Predicate<Path> isMatchingFile = path -> (!path.equals(rootPathForPattern) &&
 1085 | 				getPathMatcher().match(resourcePattern, StringUtils.cleanPath(path.toString())));
 1086 | 
 1087 | 		if (logger.isTraceEnabled()) {
 1088 | 			logger.trace("Searching directory [%s] for files matching pattern [%s]"
 1089 | 					.formatted(rootPath.toAbsolutePath(), subPattern));
 1090 | 		}
 1091 | 
 1092 | 		try (Stream<Path> files = Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)) {
 1093 | 			files.filter(isMatchingFile).sorted().map(FileSystemResource::new).forEach(result::add);
 1094 | 		}
 1095 | 		catch (Exception ex) {
 1096 | 			if (logger.isWarnEnabled()) {
 1097 | 				logger.warn("Failed to search in directory [%s] for files matching pattern [%s]: %s"
 1098 | 						.formatted(rootPath.toAbsolutePath(), subPattern, ex));
 1099 | 			}
 1100 | 		}
 1101 | 		return result;
 1102 | 	}
 1103 | 
 1104 | 	/**
 1105 | 	 * Resolve the given location pattern into {@code Resource} objects for all
 1106 | 	 * matching resources found in the module path.
 1107 | 	 * <p>The location pattern may be an explicit resource path such as
 1108 | 	 * {@code "com/example/config.xml"} or a pattern such as
 1109 | 	 * <code>"com/example/**&#47;config-*.xml"</code> to be matched using the
 1110 | 	 * configured {@link #getPathMatcher() PathMatcher}.
 1111 | 	 * <p>The default implementation scans all modules in the {@linkplain ModuleLayer#boot()
 1112 | 	 * boot layer}, excluding {@linkplain ModuleFinder#ofSystem() system modules}.
 1113 | 	 * @param locationPattern the location pattern to resolve
 1114 | 	 * @return a modifiable {@code Set} containing the corresponding {@code Resource}
 1115 | 	 * objects
 1116 | 	 * @throws IOException in case of I/O errors
 1117 | 	 * @since 6.0
 1118 | 	 * @see ModuleLayer#boot()
 1119 | 	 * @see ModuleFinder#ofSystem()
 1120 | 	 * @see ModuleReader
 1121 | 	 * @see PathMatcher#match(String, String)
 1122 | 	 */
 1123 | 	protected Set<Resource> findAllModulePathResources(String locationPattern) throws IOException {
 1124 | 		Set<Resource> result = new LinkedHashSet<>(64);
 1125 | 
 1126 | 		// Skip scanning the module path when running in a native image.
 1127 | 		if (NativeDetector.inNativeImage()) {
 1128 | 			return result;
 1129 | 		}
 1130 | 
 1131 | 		String resourcePattern = stripLeadingSlash(locationPattern);
 1132 | 		Predicate<String> resourcePatternMatches = (getPathMatcher().isPattern(resourcePattern) ?
 1133 | 				path -> getPathMatcher().match(resourcePattern, path) :
 1134 | 				resourcePattern::equals);
 1135 | 
 1136 | 		try {
 1137 | 			ModuleLayer.boot().configuration().modules().stream()
 1138 | 					.filter(isNotSystemModule)
 1139 | 					.forEach(resolvedModule -> {
 1140 | 						// NOTE: a ModuleReader and a Stream returned from ModuleReader.list() must be closed.
 1141 | 						try (ModuleReader moduleReader = resolvedModule.reference().open();
 1142 | 								Stream<String> names = moduleReader.list()) {
 1143 | 							names.filter(resourcePatternMatches)
 1144 | 									.map(name -> findResource(moduleReader, name))
 1145 | 									.filter(Objects::nonNull)
 1146 | 									.forEach(result::add);
 1147 | 						}
 1148 | 						catch (IOException ex) {
 1149 | 							if (logger.isDebugEnabled()) {
 1150 | 								logger.debug("Failed to read contents of module [%s]".formatted(resolvedModule), ex);
 1151 | 							}
 1152 | 							throw new UncheckedIOException(ex);
 1153 | 						}
 1154 | 					});
 1155 | 		}
 1156 | 		catch (UncheckedIOException ex) {
 1157 | 			// Unwrap IOException to conform to this method's contract.
 1158 | 			throw ex.getCause();
 1159 | 		}
 1160 | 
 1161 | 		if (logger.isTraceEnabled()) {
 1162 | 			logger.trace("Resolved module-path location pattern [%s] to resources %s".formatted(resourcePattern, result));
 1163 | 		}
 1164 | 		return result;
 1165 | 	}
 1166 | 
 1167 | 	private @Nullable Resource findResource(ModuleReader moduleReader, String name) {
 1168 | 		try {
 1169 | 			return moduleReader.find(name)
 1170 | 					.map(this::convertModuleSystemURI)
 1171 | 					.orElse(null);
 1172 | 		}
 1173 | 		catch (Exception ex) {
 1174 | 			if (logger.isDebugEnabled()) {
 1175 | 				logger.debug("Failed to find resource [%s] in module path".formatted(name), ex);
 1176 | 			}
 1177 | 			return null;
 1178 | 		}
 1179 | 	}
 1180 | 
 1181 | 	/**
 1182 | 	 * If it's a "file:" URI, use {@link FileSystemResource} to avoid duplicates
 1183 | 	 * for the same path discovered via class path scanning.
 1184 | 	 */
 1185 | 	private Resource convertModuleSystemURI(URI uri) {
 1186 | 		return (ResourceUtils.URL_PROTOCOL_FILE.equals(uri.getScheme()) ?
 1187 | 				new FileSystemResource(uri.getPath()) : UrlResource.from(uri));
 1188 | 	}
 1189 | 
 1190 | 	private static String stripLeadingSlash(String path) {
 1191 | 		return (path.startsWith("/") ? path.substring(1) : path);
 1192 | 	}
 1193 | 
 1194 | 	private static boolean checkPathWithinPackage(String path) {
 1195 | 		return (path.contains("/") && !path.contains(ResourceUtils.JAR_URL_SEPARATOR));
 1196 | 	}
 1197 | 
 1198 | 
 1199 | 	/**
 1200 | 	 * Inner delegate class, avoiding a hard JBoss VFS API dependency at runtime.
 1201 | 	 */
 1202 | 	private static class VfsResourceMatchingDelegate {
 1203 | 
 1204 | 		public static Set<Resource> findMatchingResources(
 1205 | 				URL rootDirUrl, String locationPattern, PathMatcher pathMatcher) throws IOException {
 1206 | 
 1207 | 			Object root = VfsPatternUtils.findRoot(rootDirUrl);
 1208 | 			PatternVirtualFileVisitor visitor =
 1209 | 					new PatternVirtualFileVisitor(VfsPatternUtils.getPath(root), locationPattern, pathMatcher);
 1210 | 			VfsPatternUtils.visit(root, visitor);
 1211 | 			return visitor.getResources();
 1212 | 		}
 1213 | 	}
 1214 | 
 1215 | 
 1216 | 	/**
 1217 | 	 * VFS visitor for path matching purposes.
 1218 | 	 */
 1219 | 	@SuppressWarnings("unused")
 1220 | 	private static class PatternVirtualFileVisitor implements InvocationHandler {
 1221 | 
 1222 | 		private final String subPattern;
 1223 | 
 1224 | 		private final PathMatcher pathMatcher;
 1225 | 
 1226 | 		private final String rootPath;
 1227 | 
 1228 | 		private final Set<Resource> resources = new LinkedHashSet<>(64);
 1229 | 
 1230 | 		public PatternVirtualFileVisitor(String rootPath, String subPattern, PathMatcher pathMatcher) {
 1231 | 			this.subPattern = subPattern;
 1232 | 			this.pathMatcher = pathMatcher;
 1233 | 			this.rootPath = (rootPath.isEmpty() || rootPath.endsWith("/") ? rootPath : rootPath + "/");
 1234 | 		}
 1235 | 
 1236 | 		@Override
 1237 | 		public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
 1238 | 			String methodName = method.getName();
 1239 | 			if (Object.class == method.getDeclaringClass()) {
 1240 | 				switch (methodName) {
 1241 | 					case "equals" -> {
 1242 | 						// Only consider equal when proxies are identical.
 1243 | 						return (proxy == args[0]);
 1244 | 					}
 1245 | 					case "hashCode" -> {
 1246 | 						return System.identityHashCode(proxy);
 1247 | 					}
 1248 | 				}
 1249 | 			}
 1250 | 			return switch (methodName) {
 1251 | 				case "getAttributes" -> getAttributes();
 1252 | 				case "visit" -> {
 1253 | 					visit(args[0]);
 1254 | 					yield null;
 1255 | 				}
 1256 | 				case "toString" -> toString();
 1257 | 				default -> throw new IllegalStateException("Unexpected method invocation: " + method);
 1258 | 			};
 1259 | 		}
 1260 | 
 1261 | 		public void visit(Object vfsResource) {
 1262 | 			if (this.pathMatcher.match(this.subPattern,
 1263 | 					VfsPatternUtils.getPath(vfsResource).substring(this.rootPath.length()))) {
 1264 | 				this.resources.add(new VfsResource(vfsResource));
 1265 | 			}
 1266 | 		}
 1267 | 
 1268 | 		public @Nullable Object getAttributes() {
 1269 | 			return VfsPatternUtils.getVisitorAttributes();
 1270 | 		}
 1271 | 
 1272 | 		public Set<Resource> getResources() {
 1273 | 			return this.resources;
 1274 | 		}
 1275 | 
 1276 | 		public int size() {
 1277 | 			return this.resources.size();
 1278 | 		}
 1279 | 
 1280 | 		@Override
 1281 | 		public String toString() {
 1282 | 			return "sub-pattern: " + this.subPattern + ", resources: " + this.resources;
 1283 | 		}
 1284 | 	}
 1285 | 
 1286 | 
 1287 | 	/**
 1288 | 	 * A single {@code Class-Path} manifest entry.
 1289 | 	 */
 1290 | 	private record ClassPathManifestEntry(Resource resource, @Nullable Resource alternative) {
 1291 | 
 1292 | 		private static final String JARFILE_URL_PREFIX = ResourceUtils.JAR_URL_PREFIX + ResourceUtils.FILE_URL_PREFIX;
 1293 | 
 1294 | 		static ClassPathManifestEntry of(File file, @Nullable Boolean useCaches) throws MalformedURLException {
 1295 | 			String path = fixPath(file.getAbsolutePath());
 1296 | 			Resource resource = asJarFileResource(path, useCaches);
 1297 | 			if (!resource.exists()) {
 1298 | 				String encodedPath = fixPath(StringUtils.replace(file.getAbsolutePath(), "%", "%25"));
 1299 | 				if (!encodedPath.equals(path)) {
 1300 | 					Resource encodedResource = asJarFileResource(encodedPath, useCaches);
 1301 | 					if (encodedResource.exists()) {
 1302 | 						path = encodedPath;
 1303 | 						resource = encodedResource;
 1304 | 					}
 1305 | 				}
 1306 | 			}
 1307 | 			Resource alternative = createAlternative(path, useCaches);
 1308 | 			return new ClassPathManifestEntry(resource, alternative);
 1309 | 		}
 1310 | 
 1311 | 		private static String fixPath(String path) {
 1312 | 			int prefixIndex = path.indexOf(':');
 1313 | 			if (prefixIndex == 1) {
 1314 | 				// Possibly a drive prefix on Windows (for example, "c:"), so we prepend a slash
 1315 | 				// and convert the drive letter to uppercase for consistent duplicate detection.
 1316 | 				path = "/" + StringUtils.capitalize(path);
 1317 | 			}
 1318 | 			// Since '#' can appear in directories/filenames, java.net.URL should not treat it as a fragment
 1319 | 			return StringUtils.replace(path, "#", "%23");
 1320 | 		}
 1321 | 
 1322 | 		/**
 1323 | 		 * Return an alternative form of the resource, i.e. with or without a leading slash.
 1324 | 		 * @param path the file path (with or without a leading slash)
 1325 | 		 * @return the alternative form or {@code null}
 1326 | 		 */
 1327 | 		private static @Nullable Resource createAlternative(String path, @Nullable Boolean useCaches) {
 1328 | 			try {
 1329 | 				String alternativePath = (path.startsWith("/") ? path.substring(1) : "/" + path);
 1330 | 				return asJarFileResource(alternativePath, useCaches);
 1331 | 			}
 1332 | 			catch (MalformedURLException ex) {
 1333 | 				return null;
 1334 | 			}
 1335 | 		}
 1336 | 
 1337 | 		private static Resource asJarFileResource(String path, @Nullable Boolean useCaches) throws MalformedURLException {
 1338 | 			UrlResource resource = new UrlResource(JARFILE_URL_PREFIX + path + ResourceUtils.JAR_URL_SEPARATOR);
 1339 | 			if (useCaches != null) {
 1340 | 				resource.setUseCaches(useCaches);
 1341 | 			}
 1342 | 			return resource;
 1343 | 		}
 1344 | 	}
 1345 | 
 1346 | }
```
