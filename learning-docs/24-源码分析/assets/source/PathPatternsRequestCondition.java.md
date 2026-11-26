<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/condition/PathPatternsRequestCondition.java -->
<!-- 正文引用：[[源码:PathPatternsRequestCondition.java#L起-L止]] ；代码块头部：(src: PathPatternsRequestCondition.java#L起-L止) -->

# PathPatternsRequestCondition.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/condition/PathPatternsRequestCondition.java` |
| 行数 | 241 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:13.778Z |

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
   17 | package org.springframework.web.servlet.mvc.condition;
   18 | 
   19 | import java.util.Collection;
   20 | import java.util.Collections;
   21 | import java.util.HashSet;
   22 | import java.util.Iterator;
   23 | import java.util.List;
   24 | import java.util.Set;
   25 | import java.util.SortedSet;
   26 | import java.util.TreeSet;
   27 | import java.util.stream.Collectors;
   28 | 
   29 | import jakarta.servlet.http.HttpServletRequest;
   30 | import org.jspecify.annotations.Nullable;
   31 | 
   32 | import org.springframework.http.server.PathContainer;
   33 | import org.springframework.util.StringUtils;
   34 | import org.springframework.web.util.ServletRequestPathUtils;
   35 | import org.springframework.web.util.pattern.PathPattern;
   36 | import org.springframework.web.util.pattern.PathPatternParser;
   37 | 
   38 | /**
   39 |  * A logical disjunction (' || ') request condition that matches a request
   40 |  * against a set of URL path patterns.
   41 |  *
   42 |  * <p>In contrast to {@link PatternsRequestCondition}, this condition uses
   43 |  * parsed {@link PathPattern}s instead of String pattern matching with
   44 |  * {@link org.springframework.util.AntPathMatcher AntPathMatcher}.
   45 |  *
   46 |  * @author Rossen Stoyanchev
   47 |  * @since 5.3
   48 |  */
   49 | public final class PathPatternsRequestCondition extends AbstractRequestCondition<PathPatternsRequestCondition> {
   50 | 
   51 | 	private static final SortedSet<PathPattern> EMPTY_PATH_PATTERN =
   52 | 			new TreeSet<>(Collections.singleton(new PathPatternParser().parse("")));
   53 | 
   54 | 	private static final Set<String> EMPTY_PATH = Collections.singleton("");
   55 | 
   56 | 	private static final SortedSet<PathPattern> ROOT_PATH_PATTERNS =
   57 | 			new TreeSet<>(List.of(new PathPatternParser().parse(""), new PathPatternParser().parse("/")));
   58 | 
   59 | 
   60 | 	private final SortedSet<PathPattern> patterns;
   61 | 
   62 | 
   63 | 	/**
   64 | 	 * Default constructor resulting in an {@code ""} (empty path) mapping.
   65 | 	 */
   66 | 	public PathPatternsRequestCondition() {
   67 | 		this(EMPTY_PATH_PATTERN);
   68 | 	}
   69 | 
   70 | 	/**
   71 | 	 * Constructor with patterns to use.
   72 | 	 */
   73 | 	public PathPatternsRequestCondition(PathPatternParser parser, String... patterns) {
   74 | 		this(parse(parser, patterns));
   75 | 	}
   76 | 
   77 | 	private static SortedSet<PathPattern> parse(PathPatternParser parser, String... patterns) {
   78 | 		if (patterns.length == 0 || (patterns.length == 1 && !StringUtils.hasText(patterns[0]))) {
   79 | 			return EMPTY_PATH_PATTERN;
   80 | 		}
   81 | 		SortedSet<PathPattern> result = new TreeSet<>();
   82 | 		for (String pattern : patterns) {
   83 | 			pattern = parser.initFullPathPattern(pattern);
   84 | 			result.add(parser.parse(pattern));
   85 | 		}
   86 | 		return result;
   87 | 	}
   88 | 
   89 | 	private PathPatternsRequestCondition(SortedSet<PathPattern> patterns) {
   90 | 		this.patterns = patterns;
   91 | 	}
   92 | 
   93 | 	/**
   94 | 	 * Return the patterns in this condition. If only the first (top) pattern
   95 | 	 * is needed use {@link #getFirstPattern()}.
   96 | 	 */
   97 | 	public Set<PathPattern> getPatterns() {
   98 | 		return this.patterns;
   99 | 	}
  100 | 
  101 | 	@Override
  102 | 	protected Collection<PathPattern> getContent() {
  103 | 		return this.patterns;
  104 | 	}
  105 | 
  106 | 	@Override
  107 | 	protected String getToStringInfix() {
  108 | 		return " || ";
  109 | 	}
  110 | 
  111 | 	/**
  112 | 	 * Return the first pattern.
  113 | 	 */
  114 | 	public PathPattern getFirstPattern() {
  115 | 		return this.patterns.first();
  116 | 	}
  117 | 
  118 | 	/**
  119 | 	 * Whether the condition is the "" (empty path) mapping.
  120 | 	 */
  121 | 	public boolean isEmptyPathMapping() {
  122 | 		return this.patterns == EMPTY_PATH_PATTERN;
  123 | 	}
  124 | 
  125 | 	/**
  126 | 	 * Return the mapping paths that are not patterns.
  127 | 	 */
  128 | 	public Set<String> getDirectPaths() {
  129 | 		if (isEmptyPathMapping()) {
  130 | 			return EMPTY_PATH;
  131 | 		}
  132 | 		Set<String> result = Collections.emptySet();
  133 | 		for (PathPattern pattern : this.patterns) {
  134 | 			if (!pattern.hasPatternSyntax()) {
  135 | 				result = (result.isEmpty() ? new HashSet<>(1) : result);
  136 | 				result.add(pattern.getPatternString());
  137 | 			}
  138 | 		}
  139 | 		return result;
  140 | 	}
  141 | 
  142 | 	/**
  143 | 	 * Return the {@link #getPatterns()} mapped to Strings.
  144 | 	 */
  145 | 	public Set<String> getPatternValues() {
  146 | 		return (isEmptyPathMapping() ? EMPTY_PATH :
  147 | 				getPatterns().stream().map(PathPattern::getPatternString).collect(Collectors.toSet()));
  148 | 	}
  149 | 
  150 | 	/**
  151 | 	 * Combine the patterns of the current and of the other instances as follows:
  152 | 	 * <ul>
  153 | 	 * <li>If only one instance has patterns, use those.
  154 | 	 * <li>If both have patterns, combine patterns from "this" instance with
  155 | 	 * patterns from the other instance via {@link PathPattern#combine(PathPattern)}.
  156 | 	 * <li>If neither has patterns, use {@code ""} and {@code "/"} as root path patterns.
  157 | 	 * </ul>
  158 | 	 */
  159 | 	@Override
  160 | 	public PathPatternsRequestCondition combine(PathPatternsRequestCondition other) {
  161 | 		if (isEmptyPathMapping() && other.isEmptyPathMapping()) {
  162 | 			return new PathPatternsRequestCondition(ROOT_PATH_PATTERNS);
  163 | 		}
  164 | 		else if (other.isEmptyPathMapping()) {
  165 | 			return this;
  166 | 		}
  167 | 		else if (isEmptyPathMapping()) {
  168 | 			return other;
  169 | 		}
  170 | 		else {
  171 | 			SortedSet<PathPattern> combined = new TreeSet<>();
  172 | 			for (PathPattern pattern1 : this.patterns) {
  173 | 				for (PathPattern pattern2 : other.patterns) {
  174 | 					combined.add(pattern1.combine(pattern2));
  175 | 				}
  176 | 			}
  177 | 			return new PathPatternsRequestCondition(combined);
  178 | 		}
  179 | 	}
  180 | 
  181 | 	/**
  182 | 	 * Checks if any of the patterns match the given request and returns an
  183 | 	 * instance that is guaranteed to contain matching patterns, sorted.
  184 | 	 * @param request the current request
  185 | 	 * @return the same instance if the condition contains no patterns;
  186 | 	 * or a new condition with sorted matching patterns;
  187 | 	 * or {@code null} if no patterns match.
  188 | 	 */
  189 | 	@Override
  190 | 	public @Nullable PathPatternsRequestCondition getMatchingCondition(HttpServletRequest request) {
  191 | 		PathContainer path = ServletRequestPathUtils.getParsedRequestPath(request).pathWithinApplication();
  192 | 		if (this.patterns.size() == 1) {
  193 | 			return (this.patterns.first().matches(path) ? this : null);
  194 | 		}
  195 | 		SortedSet<PathPattern> matches = getMatchingPatterns(path);
  196 | 		return (matches != null ? new PathPatternsRequestCondition(matches) : null);
  197 | 	}
  198 | 
  199 | 	private @Nullable SortedSet<PathPattern> getMatchingPatterns(PathContainer path) {
  200 | 		TreeSet<PathPattern> result = null;
  201 | 		for (PathPattern pattern : this.patterns) {
  202 | 			if (pattern.matches(path)) {
  203 | 				result = (result != null ? result : new TreeSet<>());
  204 | 				result.add(pattern);
  205 | 			}
  206 | 		}
  207 | 		return result;
  208 | 	}
  209 | 
  210 | 	/**
  211 | 	 * Compare the two conditions based on the URL patterns they contain.
  212 | 	 * Patterns are compared one at a time, from top to bottom. If all compared
  213 | 	 * patterns match equally, but one instance has more patterns, it is
  214 | 	 * considered a closer match.
  215 | 	 * <p>It is assumed that both instances have been obtained via
  216 | 	 * {@link #getMatchingCondition(HttpServletRequest)} to ensure they
  217 | 	 * contain only patterns that match the request and are sorted with
  218 | 	 * the best matches on top.
  219 | 	 */
  220 | 	@Override
  221 | 	public int compareTo(PathPatternsRequestCondition other, HttpServletRequest request) {
  222 | 		Iterator<PathPattern> iterator = this.patterns.iterator();
  223 | 		Iterator<PathPattern> iteratorOther = other.getPatterns().iterator();
  224 | 		while (iterator.hasNext() && iteratorOther.hasNext()) {
  225 | 			int result = PathPattern.SPECIFICITY_COMPARATOR.compare(iterator.next(), iteratorOther.next());
  226 | 			if (result != 0) {
  227 | 				return result;
  228 | 			}
  229 | 		}
  230 | 		if (iterator.hasNext()) {
  231 | 			return -1;
  232 | 		}
  233 | 		else if (iteratorOther.hasNext()) {
  234 | 			return 1;
  235 | 		}
  236 | 		else {
  237 | 			return 0;
  238 | 		}
  239 | 	}
  240 | 
  241 | }
```
