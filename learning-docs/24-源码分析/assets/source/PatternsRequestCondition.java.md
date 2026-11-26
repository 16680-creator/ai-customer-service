<!-- learning-docs source archive（机械抓取原文，禁止手改代码内容） -->
<!-- origin: org.springframework:spring-webmvc:7.1.0 | path: org/springframework/web/servlet/mvc/condition/PatternsRequestCondition.java -->
<!-- 正文引用：[[源码:PatternsRequestCondition.java#L起-L止]] ；代码块头部：(src: PatternsRequestCondition.java#L起-L止) -->

# PatternsRequestCondition.java

| 项 | 值 |
| --- | --- |
| 来源 | `org.springframework:spring-webmvc:7.1.0` |
| 路径 | `org/springframework/web/servlet/mvc/condition/PatternsRequestCondition.java` |
| 行数 | 285 |
| 在线地址 | https://repo1.maven.org/maven2/org/springframework/spring-webmvc/7.1.0/spring-webmvc-7.1.0-sources.jar |
| 抓取时间 | 2026-11-25T14:01:13.998Z |

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
   19 | import java.util.ArrayList;
   20 | import java.util.Collection;
   21 | import java.util.Collections;
   22 | import java.util.Comparator;
   23 | import java.util.HashSet;
   24 | import java.util.Iterator;
   25 | import java.util.LinkedHashSet;
   26 | import java.util.List;
   27 | import java.util.Set;
   28 | 
   29 | import jakarta.servlet.http.HttpServletRequest;
   30 | import org.jspecify.annotations.Nullable;
   31 | 
   32 | import org.springframework.util.AntPathMatcher;
   33 | import org.springframework.util.CollectionUtils;
   34 | import org.springframework.util.ObjectUtils;
   35 | import org.springframework.util.PathMatcher;
   36 | import org.springframework.util.StringUtils;
   37 | import org.springframework.web.util.UrlPathHelper;
   38 | import org.springframework.web.util.pattern.PathPattern;
   39 | import org.springframework.web.util.pattern.PathPatternParser;
   40 | 
   41 | /**
   42 |  * A logical disjunction (' || ') request condition that matches a request
   43 |  * against a set of URL path patterns.
   44 |  *
   45 |  * <p>In contrast to {@link PathPatternsRequestCondition} which uses parsed
   46 |  * {@link PathPattern}s, this condition does String pattern matching via
   47 |  * {@link org.springframework.util.AntPathMatcher AntPathMatcher}.
   48 |  *
   49 |  * @author Rossen Stoyanchev
   50 |  * @since 3.1
   51 |  * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
   52 |  * for use at runtime in web modules in favor of parsed patterns with
   53 |  * {@link PathPatternParser}.
   54 |  */
   55 | @Deprecated(since = "7.0", forRemoval = true)
   56 | public class PatternsRequestCondition extends AbstractRequestCondition<PatternsRequestCondition> {
   57 | 
   58 | 	private static final Set<String> EMPTY_PATH_PATTERN = Collections.singleton("");
   59 | 
   60 | 	private static final String[] ROOT_PATH_PATTERNS = new String[] {"", "/"};
   61 | 
   62 | 
   63 | 	private final Set<String> patterns;
   64 | 
   65 | 	private final PathMatcher pathMatcher;
   66 | 
   67 | 
   68 | 	/**
   69 | 	 * Constructor with URL patterns which are prepended with "/" if necessary.
   70 | 	 * @param patterns 0 or more URL patterns; no patterns results in an empty
   71 | 	 * path {@code ""} mapping which matches all requests.
   72 | 	 */
   73 | 	public PatternsRequestCondition(String... patterns) {
   74 | 		this(patterns, null, null);
   75 | 	}
   76 | 
   77 | 	/**
   78 | 	 * Constructor with URL patterns which are prepended with "/" if necessary.
   79 | 	 * @param patterns 0 or more URL patterns; no patterns results in an empty
   80 | 	 * path {@code ""} mapping which matches all requests.
   81 | 	 * @since 7.0
   82 | 	 */
   83 | 	public PatternsRequestCondition(
   84 | 			String[] patterns, @Nullable UrlPathHelper urlPathHelper, @Nullable PathMatcher pathMatcher) {
   85 | 
   86 | 		this.patterns = initPatterns(patterns);
   87 | 		this.pathMatcher = (pathMatcher != null ? pathMatcher : new AntPathMatcher());
   88 | 	}
   89 | 
   90 | 	private static Set<String> initPatterns(String[] patterns) {
   91 | 		if (!hasPattern(patterns)) {
   92 | 			return EMPTY_PATH_PATTERN;
   93 | 		}
   94 | 		Set<String> result = CollectionUtils.newLinkedHashSet(patterns.length);
   95 | 		for (String pattern : patterns) {
   96 | 			pattern = PathPatternParser.defaultInstance.initFullPathPattern(pattern);
   97 | 			result.add(pattern);
   98 | 		}
   99 | 		return result;
  100 | 	}
  101 | 
  102 | 	private static boolean hasPattern(String[] patterns) {
  103 | 		if (!ObjectUtils.isEmpty(patterns)) {
  104 | 			for (String pattern : patterns) {
  105 | 				if (StringUtils.hasText(pattern)) {
  106 | 					return true;
  107 | 				}
  108 | 			}
  109 | 		}
  110 | 		return false;
  111 | 	}
  112 | 
  113 | 	/**
  114 | 	 * Private constructor for use when combining and matching.
  115 | 	 */
  116 | 	private PatternsRequestCondition(Set<String> patterns, PatternsRequestCondition other) {
  117 | 		this.patterns = patterns;
  118 | 		this.pathMatcher = other.pathMatcher;
  119 | 	}
  120 | 
  121 | 
  122 | 	public Set<String> getPatterns() {
  123 | 		return this.patterns;
  124 | 	}
  125 | 
  126 | 	@Override
  127 | 	protected Collection<String> getContent() {
  128 | 		return this.patterns;
  129 | 	}
  130 | 
  131 | 	@Override
  132 | 	protected String getToStringInfix() {
  133 | 		return " || ";
  134 | 	}
  135 | 
  136 | 	/**
  137 | 	 * Whether the condition is the "" (empty path) mapping.
  138 | 	 */
  139 | 	public boolean isEmptyPathMapping() {
  140 | 		return this.patterns == EMPTY_PATH_PATTERN;
  141 | 	}
  142 | 
  143 | 	/**
  144 | 	 * Return the mapping paths that are not patterns.
  145 | 	 * @since 5.3
  146 | 	 */
  147 | 	public Set<String> getDirectPaths() {
  148 | 		if (isEmptyPathMapping()) {
  149 | 			return EMPTY_PATH_PATTERN;
  150 | 		}
  151 | 		Set<String> result = Collections.emptySet();
  152 | 		for (String pattern : this.patterns) {
  153 | 			if (!this.pathMatcher.isPattern(pattern)) {
  154 | 				result = (result.isEmpty() ? new HashSet<>(1) : result);
  155 | 				result.add(pattern);
  156 | 			}
  157 | 		}
  158 | 		return result;
  159 | 	}
  160 | 
  161 | 	/**
  162 | 	 * Combine the patterns of the current and of the other instances as follows:
  163 | 	 * <ul>
  164 | 	 * <li>If only one instance has patterns, use those.
  165 | 	 * <li>If both have patterns, combine patterns from "this" instance with
  166 | 	 * patterns from the other instance via {@link PathMatcher#combine(String, String)}.
  167 | 	 * <li>If neither has patterns, use {@code ""} and {@code "/"} as root path patterns.
  168 | 	 * </ul>
  169 | 	 */
  170 | 	@Override
  171 | 	public PatternsRequestCondition combine(PatternsRequestCondition other) {
  172 | 		if (isEmptyPathMapping() && other.isEmptyPathMapping()) {
  173 | 			return new PatternsRequestCondition(ROOT_PATH_PATTERNS);
  174 | 		}
  175 | 		else if (other.isEmptyPathMapping()) {
  176 | 			return this;
  177 | 		}
  178 | 		else if (isEmptyPathMapping()) {
  179 | 			return other;
  180 | 		}
  181 | 		Set<String> result = new LinkedHashSet<>();
  182 | 		if (!this.patterns.isEmpty() && !other.patterns.isEmpty()) {
  183 | 			for (String pattern1 : this.patterns) {
  184 | 				for (String pattern2 : other.patterns) {
  185 | 					result.add(this.pathMatcher.combine(pattern1, pattern2));
  186 | 				}
  187 | 			}
  188 | 		}
  189 | 		return new PatternsRequestCondition(result, this);
  190 | 	}
  191 | 
  192 | 	/**
  193 | 	 * Checks if any of the patterns match the given request and returns an instance
  194 | 	 * that is guaranteed to contain matching patterns, sorted via
  195 | 	 * {@link PathMatcher#getPatternComparator(String)}.
  196 | 	 * <p>A matching pattern is obtained by making checks in the following order:
  197 | 	 * <ul>
  198 | 	 * <li>Direct match
  199 | 	 * <li>Pattern match with ".*" appended if the pattern doesn't already contain a "."
  200 | 	 * <li>Pattern match
  201 | 	 * <li>Pattern match with "/" appended if the pattern doesn't already end in "/"
  202 | 	 * </ul>
  203 | 	 * @param request the current request
  204 | 	 * @return the same instance if the condition contains no patterns;
  205 | 	 * or a new condition with sorted matching patterns;
  206 | 	 * or {@code null} if no patterns match.
  207 | 	 */
  208 | 	@Override
  209 | 	public @Nullable PatternsRequestCondition getMatchingCondition(HttpServletRequest request) {
  210 | 		String lookupPath = UrlPathHelper.getResolvedLookupPath(request);
  211 | 		List<String> matches = getMatchingPatterns(lookupPath);
  212 | 		return !matches.isEmpty() ? new PatternsRequestCondition(new LinkedHashSet<>(matches), this) : null;
  213 | 	}
  214 | 
  215 | 	/**
  216 | 	 * Find the patterns matching the given lookup path. Invoking this method should
  217 | 	 * yield results equivalent to those of calling {@link #getMatchingCondition}.
  218 | 	 * This method is provided as an alternative to be used if no request is available
  219 | 	 * (for example, introspection, tooling, etc).
  220 | 	 * @param lookupPath the lookup path to match to existing patterns
  221 | 	 * @return a collection of matching patterns sorted with the closest match at the top
  222 | 	 */
  223 | 	public List<String> getMatchingPatterns(String lookupPath) {
  224 | 		List<String> matches = null;
  225 | 		for (String pattern : this.patterns) {
  226 | 			String match = getMatchingPattern(pattern, lookupPath);
  227 | 			if (match != null) {
  228 | 				matches = (matches != null ? matches : new ArrayList<>());
  229 | 				matches.add(match);
  230 | 			}
  231 | 		}
  232 | 		if (matches == null) {
  233 | 			return Collections.emptyList();
  234 | 		}
  235 | 		if (matches.size() > 1) {
  236 | 			matches.sort(this.pathMatcher.getPatternComparator(lookupPath));
  237 | 		}
  238 | 		return matches;
  239 | 	}
  240 | 
  241 | 	private @Nullable String getMatchingPattern(String pattern, String lookupPath) {
  242 | 		if (pattern.equals(lookupPath)) {
  243 | 			return pattern;
  244 | 		}
  245 | 		if (this.pathMatcher.match(pattern, lookupPath)) {
  246 | 			return pattern;
  247 | 		}
  248 | 		return null;
  249 | 	}
  250 | 
  251 | 	/**
  252 | 	 * Compare the two conditions based on the URL patterns they contain.
  253 | 	 * Patterns are compared one at a time, from top to bottom via
  254 | 	 * {@link PathMatcher#getPatternComparator(String)}. If all compared
  255 | 	 * patterns match equally, but one instance has more patterns, it is
  256 | 	 * considered a closer match.
  257 | 	 * <p>It is assumed that both instances have been obtained via
  258 | 	 * {@link #getMatchingCondition(HttpServletRequest)} to ensure they
  259 | 	 * contain only patterns that match the request and are sorted with
  260 | 	 * the best matches on top.
  261 | 	 */
  262 | 	@Override
  263 | 	public int compareTo(PatternsRequestCondition other, HttpServletRequest request) {
  264 | 		String lookupPath = UrlPathHelper.getResolvedLookupPath(request);
  265 | 		Comparator<String> patternComparator = this.pathMatcher.getPatternComparator(lookupPath);
  266 | 		Iterator<String> iterator = this.patterns.iterator();
  267 | 		Iterator<String> iteratorOther = other.patterns.iterator();
  268 | 		while (iterator.hasNext() && iteratorOther.hasNext()) {
  269 | 			int result = patternComparator.compare(iterator.next(), iteratorOther.next());
  270 | 			if (result != 0) {
  271 | 				return result;
  272 | 			}
  273 | 		}
  274 | 		if (iterator.hasNext()) {
  275 | 			return -1;
  276 | 		}
  277 | 		else if (iteratorOther.hasNext()) {
  278 | 			return 1;
  279 | 		}
  280 | 		else {
  281 | 			return 0;
  282 | 		}
  283 | 	}
  284 | 
  285 | }
```
